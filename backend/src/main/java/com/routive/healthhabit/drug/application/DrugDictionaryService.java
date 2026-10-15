package com.routive.healthhabit.drug.application;

import com.routive.healthhabit.common.api.PageResponse;
import com.routive.healthhabit.common.error.BusinessException;
import com.routive.healthhabit.common.error.ErrorCode;
import com.routive.healthhabit.drug.api.DrugDetailResponse;
import com.routive.healthhabit.drug.api.DrugSearchItem;
import com.routive.healthhabit.drug.domain.MedicationProduct;
import com.routive.healthhabit.drug.domain.MedicationProductRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DrugDictionaryService {

    private final MedicationProductRepository medicationProductRepository;

    public DrugDictionaryService(MedicationProductRepository medicationProductRepository) {
        this.medicationProductRepository = medicationProductRepository;
    }

    public PageResponse<DrugSearchItem> search(String keyword, int page, int size) {
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name", "id").ascending());
        // JPA offsets are int-based even though Pageable exposes a long offset.
        if (pageRequest.getOffset() > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Page<DrugSearchItem> result = medicationProductRepository
                .search(normalizedKeyword, pageRequest)
                .map(DrugSearchItem::from);
        return PageResponse.from(result);
    }

    public DrugDetailResponse get(UUID drugId) {
        MedicationProduct product = medicationProductRepository.findByIdAndActiveTrue(drugId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRUG_NOT_FOUND));
        return DrugDetailResponse.from(product);
    }
}
