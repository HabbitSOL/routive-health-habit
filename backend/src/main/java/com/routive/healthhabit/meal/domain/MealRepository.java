package com.routive.healthhabit.meal.domain;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface MealRepository extends JpaRepository<Meal,UUID> {
    Optional<Meal> findByUserIdAndRequestKey(UUID userId,String requestKey);
    Optional<Meal> findByIdAndUserId(UUID id,UUID userId);
    Page<Meal> findAllByUserIdAndMealDateBetweenAndStatus(UUID userId,LocalDate from,LocalDate to,String status,Pageable pageable);
    long countByUserIdAndMealDateBetweenAndStatus(UUID userId,LocalDate from,LocalDate to,String status);
    @Modifying(flushAutomatically=true)
    @Query("delete from Meal m where m.userId=:userId")
    void deleteForUser(UUID userId);
}
