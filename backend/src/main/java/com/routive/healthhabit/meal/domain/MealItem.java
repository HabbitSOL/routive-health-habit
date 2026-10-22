package com.routive.healthhabit.meal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="meal_items",uniqueConstraints=@UniqueConstraint(columnNames={"meal_id","item_no"}))
public class MealItem {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="meal_id",nullable=false) private Meal meal;
    @Column(name="item_no",nullable=false) private int itemNo;
    @Column(name="food_name",nullable=false,length=255) private String foodName;
    @Column(nullable=false,precision=12,scale=3) private BigDecimal amount;
    @Column(name="unit_code",nullable=false,length=16) private String unitCode;
    @Embedded private NutrientValues nutrients;
    protected MealItem() {}
    public MealItem(Meal meal,int itemNo,Meal.NewItem value) {
        id=UUID.randomUUID();this.meal=meal;this.itemNo=itemNo;foodName=value.foodName();
        amount=value.amount();unitCode=value.unitCode();nutrients=value.nutrients();
    }
    public UUID getId(){return id;} public int getItemNo(){return itemNo;}
    public String getFoodName(){return foodName;} public BigDecimal getAmount(){return amount;}
    public String getUnitCode(){return unitCode;} public NutrientValues getNutrients(){return nutrients;}
}
