package com.routive.healthhabit.meal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="meals",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_key"}))
public class Meal {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="user_id",nullable=false,columnDefinition="BINARY(16)") private UUID userId;
    @Column(name="meal_type",nullable=false,length=16) private String mealType;
    @Column(name="eaten_at",nullable=false) private Instant eatenAt;
    @Column(name="meal_date",nullable=false) private LocalDate mealDate;
    @Column(nullable=false,length=64) private String timezone;
    @Column(nullable=false,length=16) private String status;
    @Column(length=1000) private String memo;
    @Column(name="recorded_at",nullable=false) private Instant recordedAt;
    @Column(name="cancelled_at") private Instant cancelledAt;
    @Column(name="request_key",nullable=false,length=36) private String requestKey;
    @Column(name="request_hash",nullable=false,length=64) private String requestHash;
    @Version @Column(nullable=false) private Long version;
    @OneToMany(mappedBy="meal",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("itemNo ASC") private List<MealItem> items=new ArrayList<>();
    protected Meal() {}
    public Meal(UUID user, String type, Instant eaten, String zone, String memo, UUID key,
            String hash, Instant recorded, List<NewItem> values) {
        id=UUID.randomUUID();userId=user;mealType=type;eatenAt=eaten;timezone=zone;
        mealDate=eaten.atZone(ZoneId.of(zone)).toLocalDate();status="COMPLETED";
        this.memo=memo;requestKey=key.toString();requestHash=hash;recordedAt=recorded;
        for(int i=0;i<values.size();i++) items.add(new MealItem(this,i,values.get(i)));
    }
    public record NewItem(String foodName,BigDecimal amount,String unitCode,NutrientValues nutrients) {}
    public void cancel(Instant at) {status="CANCELLED";cancelledAt=at;}
    public UUID getId(){return id;} public String getMealType(){return mealType;}
    public Instant getEatenAt(){return eatenAt;} public LocalDate getMealDate(){return mealDate;}
    public String getTimezone(){return timezone;} public String getStatus(){return status;}
    public String getMemo(){return memo;} public Instant getRecordedAt(){return recordedAt;}
    public Instant getCancelledAt(){return cancelledAt;} public String getRequestHash(){return requestHash;}
    public Long getVersion(){return version;} public List<MealItem> getItems(){return List.copyOf(items);}
}
