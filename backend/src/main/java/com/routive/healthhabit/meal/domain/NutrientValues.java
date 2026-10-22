package com.routive.healthhabit.meal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Embeddable
public class NutrientValues {
    @Column(name="calories_kcal",precision=12,scale=3) private BigDecimal caloriesKcal;
    @Column(name="carbohydrate_g",precision=12,scale=3) private BigDecimal carbohydrateG;
    @Column(name="protein_g",precision=12,scale=3) private BigDecimal proteinG;
    @Column(name="fat_g",precision=12,scale=3) private BigDecimal fatG;
    @Column(name="sugar_g",precision=12,scale=3) private BigDecimal sugarG;
    @Column(name="caffeine_mg",precision=12,scale=3) private BigDecimal caffeineMg;
    protected NutrientValues() {}
    public NutrientValues(BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein,
            BigDecimal fat, BigDecimal sugar, BigDecimal caffeine) {
        caloriesKcal=calories; carbohydrateG=carbohydrate; proteinG=protein;
        fatG=fat; sugarG=sugar; caffeineMg=caffeine;
    }
    public List<BigDecimal> values() {
        return Arrays.asList(caloriesKcal,carbohydrateG,proteinG,fatG,sugarG,caffeineMg);
    }
}
