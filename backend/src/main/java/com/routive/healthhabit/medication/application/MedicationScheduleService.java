package com.routive.healthhabit.medication.application;
import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.medication.api.*;
import com.routive.healthhabit.medication.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class MedicationScheduleService {
    private final PersonalMedicationService access;
    private final PersonalMedicationRepository medications;
    private final FixedMedicationScheduleRepository schedules;
    private final MedicationOccurrenceRepository occurrences;
    public MedicationScheduleService(PersonalMedicationService a,PersonalMedicationRepository m,
        FixedMedicationScheduleRepository s,MedicationOccurrenceRepository o){
        access=a;medications=m;schedules=s;occurrences=o;
    }
    public Map<String,Object> save(UUID user,UUID medication,FixedScheduleRequest r){
        var m=access.requireOwned(user,medication,true);
        if(!m.getStatus().equals("ACTIVE")||!r.medicationVersion().equals(m.getVersion()))conflict();
        ZoneId zone=zone(r.timezone());
        LocalDate today=LocalDate.now(zone);
        if(r.effectiveFrom().isBefore(today)||r.effectiveFrom().isBefore(m.getStartDate())
            ||(m.getEndDate()!=null&&r.effectiveFrom().isAfter(m.getEndDate())))invalid();
        if(new HashSet<>(r.daysOfWeek()).size()!=r.daysOfWeek().size())invalid();
        Set<LocalTime> uniqueTimes=new HashSet<>();
        List<FixedMedicationSchedule.Slot> values=new ArrayList<>();
        var sorted=r.times().stream().sorted(Comparator.comparing(FixedScheduleRequest.Time::sequence)).toList();
        for(int i=0;i<sorted.size();i++){
            var t=sorted.get(i);
            if(t.sequence()!=i+1||t.mealType()!=null||t.offsetMinutes()!=null)invalid();
            LocalTime time=LocalTime.parse(t.fixedTime());
            if(!uniqueTimes.add(time))invalid();
            values.add(new FixedMedicationSchedule.Slot(time,t.doseAmount(),t.doseUnitCode()));
        }
        var old=schedules.findAllByMedicationIdOrderByEffectiveFromAsc(medication);
        if(!old.isEmpty()){
            var last=old.getLast();
            if(!last.getTimezone().equals(r.timezone())||!r.effectiveFrom().isAfter(today)
                ||!r.effectiveFrom().isAfter(last.getEffectiveFrom()))invalid();
            // Append-only effective dates preserve historical schedule definitions.
            last.endAt(r.effectiveFrom());
            Instant cutoff=r.effectiveFrom().atStartOfDay(zone).toInstant();
            occurrences.findAllByMedicationIdAndScheduledAtGreaterThanEqual(medication,cutoff).forEach(MedicationOccurrence::cancel);
        }
        var schedule=schedules.save(new FixedMedicationSchedule(medication,r.effectiveFrom(),r.timezone(),
            new HashSet<>(r.daysOfWeek()),values));
        m.touch();medications.flush();
        return response(schedule,m.getVersion());
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> list(UUID user,UUID medication){
        var m=access.requireOwned(user,medication,false);
        return schedules.findAllByMedicationIdOrderByEffectiveFromAsc(medication).stream()
            .map(s->response(s,m.getVersion())).toList();
    }
    public Map<String,Object> generate(UUID user,UUID medication,LocalDate date){
        var m=access.requireOwned(user,medication,true);
        if(!m.getStatus().equals("ACTIVE"))conflict();
        int created=0;
        var available=schedules.findAllByMedicationIdOrderByEffectiveFromAsc(medication);
        if(available.isEmpty())invalid();
        for(var s:available){
            ZoneId zone=zone(s.getTimezone());LocalDate today=LocalDate.now(zone);
            if(date.isBefore(today)||date.isAfter(today.plusDays(90)))invalid();
            if(!s.covers(date)||date.isBefore(m.getStartDate())||(m.getEndDate()!=null&&date.isAfter(m.getEndDate()))
                ||!s.getDays().contains(date.getDayOfWeek().getValue()))continue;
            var slots=s.getSlots();
            for(int i=0;i<slots.size();i++){
                var slot=slots.get(i);
                // Do not silently move a dose across DST gaps or choose an ambiguous overlap.
                Instant at=MedicationTimeResolver.resolve(date,slot.getFixedTime(),zone);
                if(!occurrences.existsByScheduleIdAndScheduledDateAndSlotNo(s.getId(),date,i)){
                    occurrences.save(new MedicationOccurrence(user,m,s,date,i,at,slot));created++;
                }
            }
        }
        occurrences.flush();return Map.of("created",created,"date",date);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> occurrences(UUID user,LocalDate date){
        access.authorizeRecords(user,false);
        return Map.of("items",occurrences.findAllByUserIdAndScheduledDateOrderByScheduledAtAscIdAsc(user,date)
            .stream().map(MedicationOccurrenceResponse::from).toList());
    }
    private Map<String,Object> response(FixedMedicationSchedule s,Long version){
        Map<String,Object> value=new LinkedHashMap<>();
        value.put("scheduleId",s.getId());value.put("medicationVersion",version);value.put("effectiveFrom",s.getEffectiveFrom());
        value.put("effectiveUntil",s.getEffectiveUntil());value.put("timezone",s.getTimezone());
        value.put("daysOfWeek",s.getDays().stream().sorted().toList());
        List<Map<String,Object>> times=new ArrayList<>();
        var slots=s.getSlots();
        for(int i=0;i<slots.size();i++){
            var t=slots.get(i);
            times.add(Map.of("sequence",i+1,"fixedTime",t.getFixedTime().toString(),
                "doseAmount",t.getDoseAmount(),"doseUnitCode",t.getDoseUnitCode(),"mealRelation","NONE"));
        }
        value.put("times",times);return value;
    }
    private static ZoneId zone(String zone){
        try{return ZoneId.of(zone);}catch(DateTimeException e){invalid();return null;}
    }
    private static void invalid(){throw new BusinessException(ErrorCode.INVALID_REQUEST);}
    private static void conflict(){throw new BusinessException(ErrorCode.DATA_CONFLICT);}
}
