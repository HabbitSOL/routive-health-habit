package com.routive.healthhabit.meal.application;

import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.meal.api.*;
import com.routive.healthhabit.meal.domain.*;
import com.routive.healthhabit.user.application.HealthRecordAccessService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class MealService {
    private final HealthRecordAccessService access;
    private final MealRepository meals;
    private final MealItemRepository items;
    public MealService(HealthRecordAccessService access,MealRepository meals,MealItemRepository items) {
        this.access=access;this.meals=meals;this.items=items;
    }
    public MealResponse create(UUID user,UUID key,MealWriteRequest r) {
        var owner=access.require(user,true);
        if(r.imageFileKey()!=null||r.items().stream().anyMatch(i->i.foodId()!=null))invalid();
        Instant eaten=r.eatenAt().truncatedTo(ChronoUnit.MICROS);
        String memo=clean(r.memo());
        var snapshots=r.items().stream().map(i->new Meal.NewItem(clean(i.foodName()),i.amount(),i.unitCode(),values(i.nutrients()))).toList();
        if(snapshots.stream().anyMatch(i->i.foodName()==null))invalid();
        String hash=hash(r.mealType(),eaten,memo,snapshots);
        var old=meals.findByUserIdAndRequestKey(user,key.toString());
        if(old.isPresent()) {
            if(!hash.equals(old.get().getRequestHash()))conflict();
            return MealResponse.from(old.get());
        }
        Instant now=Instant.now().truncatedTo(ChronoUnit.MICROS);
        if(eaten.isBefore(Instant.EPOCH)||eaten.isAfter(now))invalid();
        var meal=new Meal(user,r.mealType(),eaten,owner.getTimezone(),memo,key,hash,now,snapshots);
        return MealResponse.from(meals.saveAndFlush(meal));
    }
    @Transactional(readOnly=true)
    public MealResponse get(UUID user,UUID id) {access.require(user,false);return MealResponse.from(owned(user,id));}
    @Transactional(readOnly=true)
    public Map<String,Object> list(UUID user,LocalDate from,LocalDate to,String status,int page,int size) {
        access.require(user,false);range(from,to);
        if(!Set.of("COMPLETED","CANCELLED").contains(status)||page<0||size<1||size>100||(long)page*size>100_000)invalid();
        var result=meals.findAllByUserIdAndMealDateBetweenAndStatus(user,from,to,status,
                PageRequest.of(page,size,Sort.by(Sort.Order.desc("eatenAt"),Sort.Order.asc("id"))));
        return Map.of("items",result.getContent().stream().map(MealResponse::from).toList(),"page",page,"size",size,
                "totalElements",result.getTotalElements(),"totalPages",result.getTotalPages(),"hasNext",result.hasNext());
    }
    public MealResponse cancel(UUID user,UUID id,long version) {
        access.require(user,true);if(version<0)invalid();
        var meal=owned(user,id);
        if("CANCELLED".equals(meal.getStatus()))return MealResponse.from(meal);
        if(!meal.getVersion().equals(version))conflict();
        meal.cancel(Instant.now().truncatedTo(ChronoUnit.MICROS));meals.flush();return MealResponse.from(meal);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> statistics(UUID user,LocalDate from,LocalDate to) {
        access.require(user,false);range(from,to);
        var aggregate=items.aggregate(user,from,to);var summary=NutritionSummary.aggregate(aggregate);
        return Map.of("from",from,"to",to,"mealCount",meals.countByUserIdAndMealDateBetweenAndStatus(user,from,to,"COMPLETED"),
                "itemCount",aggregate.getItemCount(),"totals",summary.totals(),"knownTotals",summary.knownTotals(),
                "missingItemCounts",summary.missingItemCounts());
    }
    private Meal owned(UUID user,UUID id) {
        return meals.findByIdAndUserId(id,user).orElseThrow(()->new BusinessException(ErrorCode.MEAL_NOT_FOUND));
    }
    private static void range(LocalDate from,LocalDate to) {
        if(from==null||to==null||from.isBefore(LocalDate.of(1970,1,1))||to.isAfter(LocalDate.of(9999,12,31))||to.isBefore(from)
                ||ChronoUnit.DAYS.between(from,to)>365)invalid();
    }
    private static NutrientValues values(MealWriteRequest.Nutrients n) {
        return n==null?new NutrientValues(null,null,null,null,null,null)
                :new NutrientValues(n.caloriesKcal(),n.carbohydrateG(),n.proteinG(),n.fatG(),n.sugarG(),n.caffeineMg());
    }
    private static String clean(String v) {return v==null||v.isBlank()?null:v.strip();}
    private static String hash(String type,Instant eaten,String memo,List<Meal.NewItem> items) {
        var values=new ArrayList<Object>(Arrays.asList(type,eaten,memo,items.size()));
        for(var item:items) {
            values.add(item.foodName());values.add(item.amount());values.add(item.unitCode());values.addAll(item.nutrients().values());
        }
        var canonical=new StringBuilder();
        for(Object value:values) {
            String s=value==null?null:value instanceof BigDecimal n?n.stripTrailingZeros().toPlainString():value.toString();
            canonical.append(s==null?"-1:":s.length()+":"+s);
        }
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException("SHA-256 unavailable");}
    }
    private static void invalid(){throw new BusinessException(ErrorCode.INVALID_REQUEST);}
    private static void conflict(){throw new BusinessException(ErrorCode.DATA_CONFLICT);}
}
