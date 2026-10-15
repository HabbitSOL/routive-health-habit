package com.routive.healthhabit.drug.importer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.drug.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class DrugImportIntegrationTest {
    @Autowired private DrugImportWriter writer;
    @Autowired private DrugImportRunRepository runs;
    @Autowired private MedicationProductRepository products;
    @Autowired private ApplicationContext context;
    @Autowired private MockMvc mvc;
    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean private MfdsDrugClient client;
    @MockitoBean private AccountMailSender mailSender;

    @BeforeEach void clean() {
        reset(client);
        runs.deleteAll();
        for (String code : List.of("990001", "990002", "990003")) {
            products.findByExternalCode("MFDS-EDRUG:" + code).ifPresent(products::delete);
        }
    }
    private DrugImportService service(int maxPages, int pageSize) {
        return new DrugImportService(new MfdsImportProperties(false, "test-key", 1, maxPages, pageSize), client, writer);
    }
    private MfdsDrugRecord row(String code, String name, String date) {
        return new MfdsDrugRecord(code, name, "시험제약", "효능", "사용법", "경고", "주의", "주의할 음식",
                "부작용", "보관", "https://example.com/drug.png", date);
    }
    private void onePage(MfdsDrugRecord... rows) {
        when(client.fetch(1)).thenReturn(new MfdsDrugClient.Page(1, rows.length, List.of(rows)));
    }
    private MedicationProduct product(String code) { return products.findByExternalCode("MFDS-EDRUG:" + code).orElseThrow(); }

    @Test void importsMultiplePagesAndMakesDetailsSearchable() throws Exception {
        when(client.fetch(1)).thenReturn(new MfdsDrugClient.Page(1, 2, List.of(row("990001", "실데이터형약A", "20261001"))));
        when(client.fetch(2)).thenReturn(new MfdsDrugClient.Page(2, 2, List.of(row("990002", "실데이터형약B", "20261001"))));
        var result = service(10, 1).importCatalog();
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.created()).isEqualTo(2); assertThat(result.lastCompletedPage()).isEqualTo(2);
        mvc.perform(get("/api/v1/drugs").param("keyword", "실데이터형약")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mvc.perform(get("/api/v1/drugs/" + product("990001").getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interactions").value("주의할 음식"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/drug.png"))
                .andExpect(jsonPath("$.data.ingredients").isEmpty());
    }
    @Test void repeatedImportDoesNotDuplicateProductsOrChangeIds() {
        onePage(row("990001", "약A", "20261001"));
        var first = service(10, 100).importCatalog();
        UUID id = product("990001").getId();
        var second = service(10, 100).importCatalog();
        assertThat(first.created()).isEqualTo(1); assertThat(second.created()).isZero();
        assertThat(second.unchanged()).isEqualTo(1); assertThat(product("990001").getId()).isEqualTo(id);
    }
    @Test void newerRecordUpdatesSameProductAndOlderRecordCannotOverwriteIt() {
        onePage(row("990001", "최초약", "20261002")); service(10, 100).importCatalog();
        UUID id = product("990001").getId();
        onePage(row("990001", "수정약", "20261003"));
        assertThat(service(10, 100).importCatalog().updated()).isEqualTo(1);
        onePage(row("990001", "오래된약", "20261001"));
        assertThat(service(10, 100).importCatalog().unchanged()).isEqualTo(1);
        assertThat(product("990001").getName()).isEqualTo("수정약");
        assertThat(product("990001").getId()).isEqualTo(id);
    }
    @Test void pageLimitMarksPartialAndExposesResumePage() {
        when(client.fetch(1)).thenReturn(new MfdsDrugClient.Page(1, 2, List.of(row("990001", "약A", "20261001"))));
        var result = service(1, 1).importCatalog();
        assertThat(result.status()).isEqualTo("PARTIAL"); assertThat(result.nextPage()).isEqualTo(2);
        verify(client, never()).fetch(2);
    }
    @Test void laterNetworkFailurePreservesCommittedPagesAndExistingProducts() {
        when(client.fetch(1)).thenReturn(new MfdsDrugClient.Page(1, 2, List.of(row("990001", "약A", "20261001"))));
        when(client.fetch(2)).thenThrow(new DrugImportException(DrugImportException.Code.HTTP_ERROR));
        var result = service(10, 1).importCatalog();
        assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.errorCode()).isEqualTo("HTTP_ERROR");
        assertThat(result.lastCompletedPage()).isEqualTo(1); assertThat(product("990001").getName()).isEqualTo("약A");
        assertThat(runs.findById(result.runId()).orElseThrow().summary().created()).isEqualTo(1);
    }
    @Test void pageWriteFailureRollsBackProductsAndCountersTogether() {
        products.save(MedicationProduct.fromPublicData(new MedicationProductData("MFDS-EDRUG:990002", "다른 출처",
                "OTC", null, null, null, null, null, null, null, null, "다른 데이터 출처", null)));
        onePage(row("990001", "새약", "20261001"), row("990002", "충돌", "20261001"));
        var result = service(10, 100).importCatalog();
        assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.errorCode()).isEqualTo("WRITE_FAILED");
        assertThat(result.created()).isZero(); assertThat(result.lastCompletedPage()).isZero();
        assertThat(products.findByExternalCode("MFDS-EDRUG:990001")).isEmpty();
        assertThat(product("990002").getName()).isEqualTo("다른 출처");
    }
    @Test void invalidRowsAreCountedAndEmptyCatalogDoesNotDeletePreviousData() {
        onePage(row("990001", "보존약", "20261001"), row("990002", "잘못된날짜", "not-a-date"));
        var first = service(10, 100).importCatalog();
        assertThat(first.created()).isEqualTo(1); assertThat(first.rejected()).isEqualTo(1);
        assertThat(first.status()).isEqualTo("COMPLETED_WITH_REJECTIONS");
        onePage();
        assertThat(service(10, 100).importCatalog().status()).isEqualTo("COMPLETED");
        assertThat(product("990001").getName()).isEqualTo("보존약");
    }
    @Test void defaultStartupDoesNotEnableImportRunner() {
        assertThat(context.getBeansOfType(DrugImportRunner.class)).isEmpty();
        verifyNoInteractions(client);
    }
    @Test void concurrentStaleUpdateCannotOverwriteCommittedNewerData() {
        onePage(row("990001", "최초약", "20261001")); service(10, 100).importCatalog();
        UUID id = product("990001").getId();
        var first = entityManagerFactory.createEntityManager();
        var second = entityManagerFactory.createEntityManager();
        try {
            first.getTransaction().begin(); second.getTransaction().begin();
            var newer = first.find(MedicationProduct.class, id);
            var stale = second.find(MedicationProduct.class, id);
            newer.applyPublicData(MfdsDrugData.from(row("990001", "최신약", "20261003")).toProductData());
            first.getTransaction().commit();
            stale.applyPublicData(MfdsDrugData.from(row("990001", "중간약", "20261002")).toProductData());
            assertThatThrownBy(() -> second.getTransaction().commit())
                    .isInstanceOf(jakarta.persistence.RollbackException.class);
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close(); second.close();
        }
        assertThat(product("990001").getName()).isEqualTo("최신약");
    }
    @Test void refreshingDoesNotReactivateInactiveProducts() {
        onePage(row("990001", "최초약", "20261001")); service(10, 100).importCatalog();
        UUID id = product("990001").getId();
        jdbc.update("update medication_products set active = 0 where external_code = ?", "MFDS-EDRUG:990001");
        onePage(row("990001", "최신약", "20261002")); service(10, 100).importCatalog();
        assertThat(products.findByIdAndActiveTrue(id)).isEmpty();
        assertThat(product("990001").getName()).isEqualTo("최신약");
    }
    @Test void unknownSourceDateCannotOverwriteDatedData() {
        onePage(row("990001", "시각확인약", "20261001")); service(10, 100).importCatalog();
        onePage(row("990001", "시각미상약", null));
        assertThat(service(10, 100).importCatalog().unchanged()).isEqualTo(1);
        assertThat(product("990001").getName()).isEqualTo("시각확인약");
    }
}
