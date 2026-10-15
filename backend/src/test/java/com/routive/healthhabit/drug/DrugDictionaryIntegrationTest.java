package com.routive.healthhabit.drug;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.routive.healthhabit.auth.mail.AccountMailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@Sql("/drug-dictionary-fixtures.sql")
class DrugDictionaryIntegrationTest {
    private static final String ALPHA = "11111111-1111-4111-8111-111111111111";
    private static final String BETA = "22222222-2222-4222-8222-222222222222";
    private static final String HIDDEN = "55555555-5555-4555-8555-555555555555";
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountMailSender mailSender;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void setSourceTimestamp() {
        // Use JPA's UTC Instant binding for both H2 and MariaDB datetime types.
        entityManager.createQuery("update MedicationProduct p set p.sourceUpdatedAt = :time where p.id = :id")
                .setParameter("time", java.time.Instant.parse("2026-10-01T00:00:00Z"))
                .setParameter("id", java.util.UUID.fromString(ALPHA)).executeUpdate();
    }

    @Test
    void publicNameSearchNormalizesCaseAndSurroundingSpaces() throws Exception {
        mvc.perform(get("/api/v1/drugs").param("keyword", "  qA aLpHa  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].drugId").value(ALPHA))
                .andExpect(jsonPath("$.data.items[0].dosageForm").value("테스트 정제"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void ingredientSearchExcludesInactiveProducts() throws Exception {
        mvc.perform(get("/api/v1/drugs").param("keyword", "테스트성분A"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].drugId").value(ALPHA));
    }

    @Test
    void manufacturerSearchHasCorrectTotalsAndNextPage() throws Exception {
        mvc.perform(get("/api/v1/drugs").param("keyword", "QA Pharma").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        mvc.perform(get("/api/v1/drugs").param("keyword", "QA Pharma").param("page", "2").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void equalNamesHaveStableIdOrderAcrossPages() throws Exception {
        mvc.perform(get("/api/v1/drugs").param("keyword", "QA Twin").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].drugId").value("33333333-3333-4333-8333-333333333333"));
        mvc.perform(get("/api/v1/drugs").param("keyword", "QA Twin").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].drugId").value("44444444-4444-4444-8444-444444444444"));
    }

    @Test
    void noResultsAndOutOfRangePageReturnEmptyItems() throws Exception {
        mvc.perform(get("/api/v1/drugs").param("keyword", "존재하지않는검색어"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        mvc.perform(get("/api/v1/drugs").param("keyword", "QA Pharma").param("page", "9").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(5));
    }

    @Test
    void searchTreatsWildcardCharactersAsLiteralText() throws Exception {
        for (String keyword : java.util.List.of("%", "_", "!")) {
            mvc.perform(get("/api/v1/drugs").param("keyword", keyword))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        }
        mvc.perform(get("/api/v1/drugs").param("keyword", "' OR 1=1 --"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void invalidSearchParametersReturnJson400() throws Exception {
        mvc.perform(get("/api/v1/drugs")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        for (String keyword : java.util.List.of("", "   ", "a".repeat(101))) {
            mvc.perform(get("/api/v1/drugs").param("keyword", keyword)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        }
        for (String page : java.util.List.of("-1", "abc", "2147483648", "2147483647")) {
            mvc.perform(get("/api/v1/drugs").param("keyword", "QA").param("page", page))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        }
        for (String size : java.util.List.of("0", "101", "abc")) {
            mvc.perform(get("/api/v1/drugs").param("keyword", "QA").param("size", size))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        }
    }

    @Test
    void maximumAllowedPageSizeIsAccepted() throws Exception {
        mvc.perform(get("/api/v1/drugs").param("keyword", "QA Pharma").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.totalElements").value(5));
    }

    @Test
    void publicDetailReturnsAppearanceOrderedIngredientsAndSource() throws Exception {
        mvc.perform(get("/api/v1/drugs/" + ALPHA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("QA Alpha50% Formula"))
                .andExpect(jsonPath("$.data.medicationType").value("OTC"))
                .andExpect(jsonPath("$.data.efficacy").value("테스트 효능"))
                .andExpect(jsonPath("$.data.usage").value("테스트 사용법"))
                .andExpect(jsonPath("$.data.precautions").value("테스트 주의사항"))
                .andExpect(jsonPath("$.data.sideEffects").value("테스트 부작용"))
                .andExpect(jsonPath("$.data.storage").value("테스트 보관법"))
                .andExpect(jsonPath("$.data.appearance.dosageForm").value("테스트 정제"))
                .andExpect(jsonPath("$.data.ingredients[0].name").value("첫번째성분"))
                .andExpect(jsonPath("$.data.ingredients[0].amount").value(500))
                .andExpect(jsonPath("$.data.ingredients[1].name").value("두번째성분"))
                .andExpect(jsonPath("$.data.sourceName").value("자동 테스트 전용"))
                .andExpect(jsonPath("$.data.sourceUpdatedAt").value("2026-10-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    void detailAllowsMissingOptionalData() throws Exception {
        mvc.perform(get("/api/v1/drugs/" + BETA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ingredients").isEmpty())
                .andExpect(jsonPath("$.data.appearance.dosageForm").isEmpty());
    }

    @Test
    void missingAndInactiveProductsReturn404() throws Exception {
        for (String id : java.util.List.of(HIDDEN, "77777777-7777-4777-8777-777777777777")) {
            mvc.perform(get("/api/v1/drugs/" + id)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("DRUG_NOT_FOUND"));
        }
    }

    @Test
    void malformedProductIdReturnsJson400() throws Exception {
        mvc.perform(get("/api/v1/drugs/not-a-uuid")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}
