package com.routive.healthhabit.medication.application;

import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.drug.domain.MedicationProductRepository;
import com.routive.healthhabit.medication.api.*;
import com.routive.healthhabit.medication.domain.*;
import com.routive.healthhabit.user.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class PersonalMedicationService {
    private final PersonalMedicationRepository medications;
    private final com.routive.healthhabit.user.application.HealthRecordAccessService healthAccess;
    private final MedicationProductRepository products;
    private final FixedMedicationScheduleRepository schedules;
    private final MedicationOccurrenceRepository occurrences;
    public PersonalMedicationService(PersonalMedicationRepository m,
            com.routive.healthhabit.user.application.HealthRecordAccessService healthAccess, MedicationProductRepository p,
            FixedMedicationScheduleRepository s, MedicationOccurrenceRepository o) {
        medications=m; this.healthAccess=healthAccess; products=p;
        schedules=s;occurrences=o;
    }
    private void authorize(UUID user, boolean write) {
        healthAccess.require(user,write);
    }
    public void authorizeRecords(UUID user,boolean write){authorize(user,write);}
    public PersonalMedication requireOwned(UUID user,UUID id,boolean write){
        authorize(user,write);return owned(user,id);
    }
    public MedicationResponse create(UUID user, UUID key, MedicationWriteRequest r) {
        authorize(user,true); noSchedule(r);
        if(r.version()!=null || (r.clearFields()!=null&&!r.clearFields().isEmpty())
            || (r.drugId()==null)==(r.customName()==null) || r.medicationType()==null || r.startDate()==null)
            invalid();
        String custom=clean(r.customName());
        if(custom!=null&&custom.isEmpty()) invalid();
        dates(r.startDate(),r.endDate());
        String hash=hash(r);
        var previous=medications.findByUserIdAndRequestKey(user,key.toString());
        if(previous.isPresent()) {
            if(!previous.get().getRequestHash().equals(hash)) conflict();
            return MedicationResponse.from(previous.get());
        }
        String name=custom;
        if(r.drugId()!=null) {
            var product=products.findById(r.drugId()).filter(p->p.isActive())
                .orElseThrow(()->new BusinessException(ErrorCode.DRUG_NOT_FOUND));
            if(!product.getKind().equals(r.medicationType())) invalid();
            name=product.getName();
        }
        var m=new PersonalMedication(user,r.drugId(),name,custom,r.medicationType(),clean(r.purpose()),
            r.startDate(),r.endDate(),clean(r.memo()),key.toString(),hash);
        return MedicationResponse.from(medications.saveAndFlush(m));
    }
    @Transactional(readOnly=true)
    public Map<String,Object> list(UUID user,String status,int page,int size) {
        authorize(user,false);
        if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE
            ||(status!=null&&!Set.of("ACTIVE","STOPPED","COMPLETED").contains(status))) invalid();
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.asc("id")));
        var result=status==null?medications.findAllByUserId(user,pageable)
            :medications.findAllByUserIdAndStatus(user,status,pageable);
        return Map.of("items",result.getContent().stream().map(MedicationResponse::from).toList(),
            "page",page,"size",size,"totalElements",result.getTotalElements(),
            "totalPages",result.getTotalPages(),"hasNext",result.hasNext());
    }
    @Transactional(readOnly=true)
    public MedicationResponse get(UUID user,UUID id) {authorize(user,false);return MedicationResponse.from(owned(user,id));}
    public MedicationResponse patch(UUID user,UUID id,MedicationWriteRequest r) {
        authorize(user,true); noSchedule(r);
        var m=owned(user,id);
        if(!"ACTIVE".equals(m.getStatus())) conflict();
        if(r.version()==null||!r.version().equals(m.getVersion())) conflict();
        if(r.drugId()!=null||r.medicationType()!=null) invalid();
        Set<String> clear=r.clearFields()==null?Set.of():r.clearFields();
        if(schedules.existsByMedicationId(id)&&(r.startDate()!=null||r.endDate()!=null||clear.contains("endDate")))
            conflict();
        if((clear.contains("purpose")&&r.purpose()!=null)||(clear.contains("endDate")&&r.endDate()!=null)
            ||(clear.contains("memo")&&r.memo()!=null)) invalid();
        String custom=r.customName()==null?m.getCustomName():clean(r.customName());
        if(r.customName()!=null&&(m.getDrugId()!=null||custom.isEmpty())) invalid();
        LocalDate start=r.startDate()==null?m.getStartDate():r.startDate();
        LocalDate end=clear.contains("endDate")?null:r.endDate()==null?m.getEndDate():r.endDate();
        dates(start,end);
        m.update(custom,clear.contains("purpose")?null:r.purpose()==null?m.getPurpose():clean(r.purpose()),
            start,end,clear.contains("memo")?null:r.memo()==null?m.getMemo():clean(r.memo()));
        medications.flush(); return MedicationResponse.from(m);
    }
    public MedicationResponse close(UUID user,UUID id,String target) {
        authorize(user,true); var m=owned(user,id);
        if(m.getStatus().equals(target)) return MedicationResponse.from(m);
        if(!m.getStatus().equals("ACTIVE")) conflict();
        m.close(target);
        occurrences.findAllByMedicationIdAndScheduledAtGreaterThanEqual(id,java.time.Instant.now())
            .forEach(MedicationOccurrence::cancel);
        medications.flush();return MedicationResponse.from(m);
    }
    private PersonalMedication owned(UUID user,UUID id) {
        return medications.findByIdAndUserId(id,user)
            .orElseThrow(()->new BusinessException(ErrorCode.MEDICATION_NOT_FOUND));
    }
    private static void noSchedule(MedicationWriteRequest r){if(r.schedule()!=null)invalid();}
    private static String clean(String v){return v==null?null:v.strip();}
    private static void dates(LocalDate start,LocalDate end){if(end!=null&&end.isBefore(start))invalid();}
    private static String hash(MedicationWriteRequest r) {
        // Length-prefixed fields avoid ambiguous concatenation; no health text is stored in the key.
        var values=Arrays.asList(r.drugId(),clean(r.customName()),r.medicationType(),clean(r.purpose()),
            r.startDate(),r.endDate(),clean(r.memo()));
        StringBuilder canonical=new StringBuilder();
        values.forEach(v->{String s=v==null?null:v.toString();canonical.append(s==null?"-1:":s.length()+":"+s);});
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable");}
    }
    private static void invalid(){throw new BusinessException(ErrorCode.INVALID_REQUEST);}
    private static void conflict(){throw new BusinessException(ErrorCode.DATA_CONFLICT);}
}
