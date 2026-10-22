package com.routive.healthhabit.meal.api;

import com.routive.healthhabit.meal.domain.Meal;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record MealResponse(UUID mealId,String mealType,Instant eatenAt,Instant recordedAt,
        LocalDate mealDate,String timezone,String status,String memo,Instant cancelledAt,Long version,
        List<Item> items,NutritionSummary.Values totals,NutritionSummary.Values knownTotals,Map<String,Long> missingItemCounts) {
    public record Item(UUID itemId,int sequence,String foodName,BigDecimal amount,String unitCode,
            NutritionSummary.Values nutrients,String source) {}
    public static MealResponse from(Meal m) {
        var items=m.getItems();
        var summary=NutritionSummary.items(items);
        return new MealResponse(m.getId(),m.getMealType(),m.getEatenAt(),m.getRecordedAt(),m.getMealDate(),
                m.getTimezone(),m.getStatus(),m.getMemo(),m.getCancelledAt(),m.getVersion(),
                items.stream().map(i->new Item(i.getId(),i.getItemNo()+1,i.getFoodName(),i.getAmount(),
                        i.getUnitCode(),NutritionSummary.Values.from(i.getNutrients()),"USER_ENTERED")).toList(),
                summary.totals(),summary.knownTotals(),summary.missingItemCounts());
    }
}
