package com.routive.healthhabit.meal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.routive.healthhabit.meal.domain.*;
import java.math.BigDecimal;
import java.util.*;

public record NutritionSummary(Values totals,Values knownTotals,Map<String,Long> missingItemCounts) {
    private static final List<String> KEYS=List.of("caloriesKcal","carbohydrateG","proteinG","fatG","sugarG","caffeineMg");
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Values(BigDecimal caloriesKcal,BigDecimal carbohydrateG,BigDecimal proteinG,
            BigDecimal fatG,BigDecimal sugarG,BigDecimal caffeineMg) {
        static Values of(List<BigDecimal> v){return new Values(v.get(0),v.get(1),v.get(2),v.get(3),v.get(4),v.get(5));}
        public static Values from(NutrientValues v) {
            return of(v==null?Arrays.asList(null,null,null,null,null,null):v.values());
        }
    }
    public static NutritionSummary items(List<MealItem> items) {
        var sums=new ArrayList<>(Collections.nCopies(6,BigDecimal.ZERO));
        long[] missing=new long[6];
        for(var item:items) {
            var v=item.getNutrients()==null?Arrays.<BigDecimal>asList(null,null,null,null,null,null):item.getNutrients().values();
            for(int i=0;i<6;i++) if(v.get(i)==null)missing[i]++;else sums.set(i,sums.get(i).add(v.get(i)));
        }
        return summary(sums,missing);
    }
    public static NutritionSummary aggregate(MealItemRepository.Aggregate a) {
        var sums=Arrays.asList(a.getCalories(),a.getCarbohydrate(),a.getProtein(),a.getFat(),a.getSugar(),a.getCaffeine());
        long n=a.getItemCount();
        return summary(sums,new long[]{n-a.getCaloriesCount(),n-a.getCarbohydrateCount(),n-a.getProteinCount(),
                n-a.getFatCount(),n-a.getSugarCount(),n-a.getCaffeineCount()});
    }
    private static NutritionSummary summary(List<BigDecimal> sums,long[] missing) {
        var known=new ArrayList<BigDecimal>();var complete=new ArrayList<BigDecimal>();
        Map<String,Long> counts=new LinkedHashMap<>();
        for(int i=0;i<6;i++){
            BigDecimal sum=sums.get(i)==null?BigDecimal.ZERO:sums.get(i);
            known.add(sum);complete.add(missing[i]==0?sum:null);counts.put(KEYS.get(i),missing[i]);
        }
        return new NutritionSummary(Values.of(complete),Values.of(known),counts);
    }
}
