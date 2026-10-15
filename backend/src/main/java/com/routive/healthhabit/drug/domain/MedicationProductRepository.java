package com.routive.healthhabit.drug.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationProductRepository extends JpaRepository<MedicationProduct, UUID> {

    Optional<MedicationProduct> findByExternalCode(String externalCode);

    @Query("""
            select product
            from MedicationProduct product
            left join product.dosageForm dosageForm
            where product.active = true
              and (
                lower(product.normalizedName) like lower(concat('%', :keyword, '%')) escape '!'
                or lower(coalesce(product.ingredientText, '')) like lower(concat('%', :keyword, '%')) escape '!'
                or lower(coalesce(product.manufacturer, '')) like lower(concat('%', :keyword, '%')) escape '!'
              )
            """)
    Page<MedicationProduct> search(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"dosageForm", "ingredients"})
    Optional<MedicationProduct> findByIdAndActiveTrue(UUID id);
}
