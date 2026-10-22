package com.routive.healthhabit.meal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface MealItemRepository extends JpaRepository<MealItem,UUID> {
    @Modifying(flushAutomatically=true)
    @Query("delete from MealItem i where i.meal.userId=:userId")
    void deleteForUser(UUID userId);
    interface Aggregate {
        long getItemCount();
        BigDecimal getCalories(); long getCaloriesCount();
        BigDecimal getCarbohydrate(); long getCarbohydrateCount();
        BigDecimal getProtein(); long getProteinCount();
        BigDecimal getFat(); long getFatCount();
        BigDecimal getSugar(); long getSugarCount();
        BigDecimal getCaffeine(); long getCaffeineCount();
    }
    @Query("""
        select count(i) as itemCount,
        sum(i.nutrients.caloriesKcal) as calories, count(i.nutrients.caloriesKcal) as caloriesCount,
        sum(i.nutrients.carbohydrateG) as carbohydrate, count(i.nutrients.carbohydrateG) as carbohydrateCount,
        sum(i.nutrients.proteinG) as protein, count(i.nutrients.proteinG) as proteinCount,
        sum(i.nutrients.fatG) as fat, count(i.nutrients.fatG) as fatCount,
        sum(i.nutrients.sugarG) as sugar, count(i.nutrients.sugarG) as sugarCount,
        sum(i.nutrients.caffeineMg) as caffeine, count(i.nutrients.caffeineMg) as caffeineCount
        from MealItem i where i.meal.userId=:userId and i.meal.status='COMPLETED'
        and i.meal.mealDate between :from and :to
        """)
    Aggregate aggregate(UUID userId,LocalDate from,LocalDate to);
}
