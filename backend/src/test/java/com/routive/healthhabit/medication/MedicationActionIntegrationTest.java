package com.routive.healthhabit.medication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import com.jayway.jsonpath.JsonPath;
import com.routive.healthhabit.auth.application.TokenService;
import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.common.error.BusinessException;
import com.routive.healthhabit.medication.api.*;
import com.routive.healthhabit.medication.application.MedicationActionService;
import com.routive.healthhabit.medication.domain.*;
import com.routive.healthhabit.user.domain.*;
import com.routive.healthhabit.user.application.UserAccountService;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class MedicationActionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserConsentRepository consents;
    @Autowired TokenService tokens;
    @Autowired PersonalMedicationRepository medications;
    @Autowired FixedMedicationScheduleRepository schedules;
    @Autowired MedicationOccurrenceRepository occurrences;
    @Autowired MedicationActionRepository actions;
    @Autowired MedicationActionService service;
    @Autowired UserAccountService accounts;
    @MockitoBean AccountMailSender mail;
    User user;
    String token;
    UUID medicationId, occurrenceId;
    Instant actualAt;
    final ZoneId zone = ZoneId.of("Asia/Seoul");

    @BeforeEach void setup() {
        actualAt = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MICROS);
        LocalDate date = actualAt.atZone(zone).toLocalDate();
        user = users.saveAndFlush(User.createSocial(UUID.randomUUID() + "@example.invalid", Instant.now()));
        for (String type : List.of("TERMS_OF_SERVICE", "PRIVACY", "SENSITIVE_HEALTH_DATA"))
            consents.saveAndFlush(UserConsent.agreed(user, type, "v1", Instant.now()));
        token = tokens.issue(user, Instant.now()).accessToken();
        var medication = medications.saveAndFlush(new PersonalMedication(user.getId(), null, "가상 약", "가상 약", "OTC",
                null, date, null, null, UUID.randomUUID().toString(), "fixture"));
        medicationId = medication.getId();
        var slot = new FixedMedicationSchedule.Slot(LocalTime.MIDNIGHT, new BigDecimal("1.500"), "TABLET");
        var schedule = schedules.saveAndFlush(new FixedMedicationSchedule(medicationId, date, zone.getId(), Set.of(1,2,3,4,5,6,7), List.of(slot)));
        occurrenceId = occurrences.saveAndFlush(new MedicationOccurrence(user.getId(), medication, schedule, date, 0,
                date.atStartOfDay(zone).toInstant(), slot)).getId();
    }
    String body(String action) {
        return action.equals("TAKE") ? "{\"action\":\"TAKE\",\"actualAt\":\"" + actualAt + "\",\"actualDoseAmount\":1,\"doseUnitCode\":\"TABLET\",\"memo\":\" 예시 메모 \"}"
                : "{\"action\":\"" + action + "\",\"memo\":\"예시\"}";
    }
    String path() { return "/api/v1/medication-occurrences/" + occurrenceId + "/actions"; }
    ResultActions record(UUID key, String body) throws Exception {
        return mvc.perform(post(path()).header("Authorization", "Bearer " + token).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    ResultActions list() throws Exception { return mvc.perform(get(path()).header("Authorization", "Bearer " + token)); }

    @Test void takeStoresSeparateTimesActualDoseAndOccurrenceStatus() throws Exception {
        list().andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        String json = record(UUID.randomUUID(), body("TAKE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("TAKE")).andExpect(jsonPath("$.data.status").value("TAKEN"))
                .andExpect(jsonPath("$.data.actualAt").value(actualAt.toString()))
                .andExpect(jsonPath("$.data.actualDoseAmount").value(1)).andExpect(jsonPath("$.data.memo").value("예시 메모"))
                .andReturn().getResponse().getContentAsString();
        assertThat(Instant.parse(JsonPath.read(json, "$.data.recordedAt"))).isAfter(actualAt);
        list().andExpect(jsonPath("$.data.items.length()").value(1));
        mvc.perform(get("/api/v1/medication-occurrences").param("date", actualAt.atZone(zone).toLocalDate().toString())
                .header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.data.items[0].status").value("TAKEN"));
        assertThat(occurrences.findById(occurrenceId).orElseThrow().getDoseAmount()).isEqualByComparingTo("1.5");
    }
    @Test void sameKeyReplayIsStableAndCanonicalizesQuantityAndMemo() throws Exception {
        UUID key = UUID.randomUUID();
        String first = record(key, body("TAKE")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String replay = record(key, body("TAKE").replace(":1,", ":1.000,").replace(" 예시 메모 ", "예시 메모"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(replay, "$.data.actionId")).isEqualTo(JsonPath.read(first, "$.data.actionId"));
        assertThat((String) JsonPath.read(replay, "$.data.recordedAt")).isEqualTo(JsonPath.read(first, "$.data.recordedAt"));
        record(key, body("SKIP")).andExpect(status().isConflict());
        record(UUID.randomUUID(), body("TAKE")).andExpect(status().isConflict());
    }
    @Test void skipDoesNotFabricateActualTimeOrDose() throws Exception {
        record(UUID.randomUUID(), body("SKIP")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SKIPPED"))
                .andExpect(jsonPath("$.data.actualAt").doesNotExist()).andExpect(jsonPath("$.data.actualDoseAmount").doesNotExist());
    }
    @Test void noStockIsAUserReportedState() throws Exception {
        record(UUID.randomUUID(), body("NO_STOCK")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NO_STOCK")).andExpect(jsonPath("$.data.recordedAt").exists());
    }
    @Test void invalidFieldsAreRejectedWithoutPartialWrites() throws Exception {
        for (String value : List.of("{}", "{\"action\":\"TAKE\"}", body("TAKE").replace(":1,", ":0,"),
                body("TAKE").replace(":1,", ":1.0001,"), body("TAKE").replace("TABLET", "ML"),
                body("TAKE").replace("TAKE", "SKIP"), body("TAKE").replace(actualAt.toString(), Instant.now().plusSeconds(60).toString()),
                body("TAKE").replace(actualAt.toString(), actualAt.atZone(zone).toLocalDate().minusDays(1).atStartOfDay(zone).toInstant().toString()),
                body("TAKE").replace(" 예시 메모 ", "x".repeat(1001)))) {
            record(UUID.randomUUID(), value).andExpect(status().isBadRequest());
        }
        list().andExpect(jsonPath("$.data.items").isEmpty());
        assertThat(occurrences.findById(occurrenceId).orElseThrow().getStatus()).isEqualTo("SCHEDULED");
    }
    @Test void missingKeyMalformedIdAndUnauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(post(path()).contentType(MediaType.APPLICATION_JSON).content(body("TAKE"))).andExpect(status().isUnauthorized());
        mvc.perform(post(path()).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body("TAKE")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path()).header("Authorization", "Bearer " + token).header("Idempotency-Key", "invalid")
                .contentType(MediaType.APPLICATION_JSON).content(body("TAKE"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/medication-occurrences/invalid/actions").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
    @Test void otherUserAndNonexistentOccurrenceReturnSame404() throws Exception {
        User other = users.saveAndFlush(User.createSocial(UUID.randomUUID() + "@example.invalid", Instant.now()));
        for (String type : List.of("TERMS_OF_SERVICE", "PRIVACY", "SENSITIVE_HEALTH_DATA"))
            consents.saveAndFlush(UserConsent.agreed(other, type, "v1", Instant.now()));
        token = tokens.issue(other, Instant.now()).accessToken();
        list().andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MEDICATION_OCCURRENCE_NOT_FOUND"));
        record(UUID.randomUUID(), body("TAKE")).andExpect(status().isNotFound());
        occurrenceId = UUID.randomUUID();
        list().andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MEDICATION_OCCURRENCE_NOT_FOUND"));
    }
    @Test void consentWithdrawalBlocksReadWriteAndIdempotentReplay() throws Exception {
        UUID key = UUID.randomUUID(); record(key, body("TAKE")).andExpect(status().isOk());
        consents.findAllByUserIdOrderByConsentedAtDesc(user.getId()).stream()
                .filter(c -> c.getConsentType().equals("SENSITIVE_HEALTH_DATA")).forEach(c -> c.withdraw(Instant.now()));
        consents.flush();
        list().andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("REQUIRED_CONSENT_MISSING"));
        record(key, body("TAKE")).andExpect(status().isBadRequest());
    }
    @Test void cancelledOccurrenceCannotBeRecorded() throws Exception {
        occurrences.findById(occurrenceId).orElseThrow().cancel(); occurrences.flush();
        record(UUID.randomUUID(), body("SKIP")).andExpect(status().isConflict());
        list().andExpect(jsonPath("$.data.items").isEmpty());
    }
    @Test void futureOccurrenceCannotBeRecordedAndKeyCannotMoveBetweenOccurrences() throws Exception {
        UUID key = UUID.randomUUID(); record(key, body("TAKE")).andExpect(status().isOk());
        var medication = medications.findById(medicationId).orElseThrow();
        var schedule = schedules.findAllByMedicationIdOrderByEffectiveFromAsc(medicationId).getFirst();
        LocalDate tomorrow = LocalDate.now(zone).plusDays(1);
        occurrenceId = occurrences.saveAndFlush(new MedicationOccurrence(user.getId(), medication, schedule, tomorrow, 0,
                tomorrow.atStartOfDay(zone).toInstant(), schedule.getSlots().getFirst())).getId();
        record(key, body("TAKE")).andExpect(status().isConflict());
        record(UUID.randomUUID(), body("SKIP")).andExpect(status().isBadRequest());
    }
    @Test void stopPreservesRecordedActionAndReplay() throws Exception {
        UUID key = UUID.randomUUID(); record(key, body("TAKE")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/medications/" + medicationId + "/stop").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        record(key, body("TAKE")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("TAKEN"));
        list().andExpect(jsonPath("$.data.items.length()").value(1));
    }
    @Test void stoppedMedicationAllowsOnlyPastActualTime() throws Exception {
        var medication = medications.findById(medicationId).orElseThrow(); medication.close("STOPPED"); medications.flush();
        Instant closed = medication.getClosedAt();
        // The timestamp just after closure is in the past by the time the request is evaluated.
        while (!Instant.now().isAfter(closed.plusMillis(1))) Thread.onSpinWait();
        record(UUID.randomUUID(), body("TAKE").replace(actualAt.toString(), closed.plusMillis(1).toString()))
                .andExpect(status().isBadRequest());
        record(UUID.randomUUID(), body("TAKE")).andExpect(status().isOk());
    }
    @Test void withdrawalDeletesActionAndChildrenAndRevokesAccess() throws Exception {
        record(UUID.randomUUID(), body("TAKE")).andExpect(status().isOk());
        accounts.withdraw(user.getId());
        assertThat(actions.findByOccurrenceIdAndUserId(occurrenceId, user.getId())).isEmpty();
        assertThat(occurrences.findById(occurrenceId)).isEmpty();
        assertThat(schedules.findAllByMedicationIdOrderByEffectiveFromAsc(medicationId)).isEmpty();
        list().andExpect(status().isUnauthorized());
    }
    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSameKeyCreatesOneAction() throws Exception { concurrent(true); }
    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDifferentKeysHaveOneWinner() throws Exception { concurrent(false); }

    private void concurrent(boolean sameKey) throws Exception {
        UUID key = UUID.randomUUID();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var request = new MedicationActionRequest("TAKE", actualAt, BigDecimal.ONE, "TABLET", null);
            Callable<String> first = () -> { start.await(); return invoke(key, request); };
            Callable<String> second = () -> { start.await(); return invoke(sameKey ? key : UUID.randomUUID(), request); };
            Future<String> a = executor.submit(first), b = executor.submit(second);
            start.countDown();
            String left = a.get(10, TimeUnit.SECONDS), right = b.get(10, TimeUnit.SECONDS);
            if (sameKey) assertThat(left).isEqualTo(right).isNotEqualTo("DATA_CONFLICT");
            else assertThat(List.of(left, right)).filteredOn("DATA_CONFLICT"::equals).hasSize(1);
            assertThat(service.list(user.getId(), occurrenceId)).hasSize(1);
        } finally { accounts.withdraw(user.getId()); }
    }
    private String invoke(UUID key, MedicationActionRequest request) {
        try { return service.record(user.getId(), occurrenceId, key, request).actionId().toString(); }
        catch (BusinessException exception) { return exception.errorCode().name(); }
    }
}
