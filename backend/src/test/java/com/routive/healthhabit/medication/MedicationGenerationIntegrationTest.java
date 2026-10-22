package com.routive.healthhabit.medication;

import static org.junit.jupiter.api.Assertions.*;
import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.common.error.BusinessException;
import com.routive.healthhabit.medication.application.*;
import com.routive.healthhabit.medication.domain.*;
import com.routive.healthhabit.user.application.UserAccountService;
import com.routive.healthhabit.user.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @Transactional
class MedicationGenerationIntegrationTest {
    @Autowired UserRepository users;
    @Autowired UserConsentRepository consents;
    @Autowired PersonalMedicationRepository medications;
    @Autowired FixedMedicationScheduleRepository schedules;
    @Autowired MedicationOccurrenceRepository occurrences;
    @Autowired MedicationScheduleService service;
    @Autowired PersonalMedicationService personal;
    @Autowired UserAccountService accounts;
    @MockitoBean AccountMailSender mail;
    private final List<UUID> cleanup = new ArrayList<>();

    User user() {
        var u = users.saveAndFlush(User.createSocial(UUID.randomUUID()+"@example.invalid", Instant.now()));
        cleanup.add(u.getId());
        for (String type : List.of("TERMS_OF_SERVICE", "PRIVACY", "SENSITIVE_HEALTH_DATA"))
            consents.saveAndFlush(UserConsent.agreed(u, type, "v1", Instant.now()));
        return u;
    }
    PersonalMedication medication(User u, LocalDate start, LocalDate end) {
        return medications.saveAndFlush(new PersonalMedication(u.getId(), null, "fixture", "fixture", "OTC",
                null, start, end, null, UUID.randomUUID().toString(), "fixture"));
    }
    FixedMedicationSchedule schedule(PersonalMedication m, String zone, LocalDate from, Set<Integer> days) {
        return schedules.saveAndFlush(new FixedMedicationSchedule(m.getId(), from, zone, days,
                List.of(new FixedMedicationSchedule.Slot(LocalTime.of(8, 0), BigDecimal.ONE, "TABLET"))));
    }
    Set<Integer> everyDay() { return Set.of(1,2,3,4,5,6,7); }
    LocalDate today(String zone) { return LocalDate.now(ZoneId.of(zone)); }
    List<MedicationOccurrence> rows(User u, LocalDate day) {
        return occurrences.findAllByUserIdAndScheduledDateOrderByScheduledAtAscIdAsc(u.getId(), day);
    }
    @AfterEach void cleanup() { cleanup.forEach(accounts::withdraw); }

    @Test void automaticAndManualGenerationShareIdsAndPreserveResolvedStatus() {
        var u=user(); var day=today("Asia/Seoul"); var m=medication(u,day,null);
        schedule(m,"Asia/Seoul",day,everyDay());
        assertEquals(1,service.generateToday(u.getId(),m.getId()));
        var first=rows(u,day).getFirst(); first.resolve("SKIPPED"); occurrences.flush();
        assertEquals(0,service.generateToday(u.getId(),m.getId()));
        assertEquals(0,service.generate(u.getId(),m.getId(),day).get("created"));
        assertEquals(1,rows(u,day).size());
        assertEquals(first.getId(),rows(u,day).getFirst().getId());
        assertEquals("SKIPPED",rows(u,day).getFirst().getStatus());
    }

    @Test void eachScheduleUsesItsOwnLocalDateNotServerOrProfileTimezone() {
        var u=user();
        for (String zone:List.of("Pacific/Kiritimati","Pacific/Honolulu")) {
            var day=today(zone); var m=medication(u,day.minusDays(1),null);
            schedule(m,zone,day.minusDays(1),everyDay());
            assertEquals(1,service.generateToday(u.getId(),m.getId()));
            var record=rows(u,day).stream().filter(x->x.getMedicationId().equals(m.getId())).findFirst().orElseThrow();
            assertEquals(day.atTime(8,0).atZone(ZoneId.of(zone)).toInstant(),record.getScheduledAt());
        }
    }

    @Test void startEndDatesWeekdaysAndFutureScheduleAreRespected() {
        var u=user();var day=today("UTC");
        var future=medication(u,day.plusDays(1),null);schedule(future,"UTC",day,everyDay());
        var expired=medication(u,day.minusDays(2),day.minusDays(1));schedule(expired,"UTC",day.minusDays(2),everyDay());
        var weekday=medication(u,day,null);schedule(weekday,"UTC",day,Set.of(day.getDayOfWeek().getValue()%7+1));
        var later=medication(u,day,null);schedule(later,"UTC",day.plusDays(1),everyDay());
        for(var m:List.of(future,expired,weekday,later)) assertEquals(0,service.generateToday(u.getId(),m.getId()));
        assertTrue(rows(u,day).isEmpty());
    }

    @Test void onlyEffectiveVersionGeneratesAndCancelledOccurrenceIsNotResurrected() {
        var u=user();var day=today("UTC");var m=medication(u,day.minusDays(1),null);
        var old=schedule(m,"UTC",day.minusDays(1),everyDay());old.endAt(day);schedules.flush();
        var current=schedule(m,"UTC",day,everyDay());
        assertEquals(1,service.generateToday(u.getId(),m.getId()));
        var record=rows(u,day).getFirst();assertEquals(current.getId(),record.getScheduleId());
        record.cancel();occurrences.flush();
        assertEquals(0,service.generateToday(u.getId(),m.getId()));
        assertEquals("CANCELLED",rows(u,day).getFirst().getStatus());
    }

    @Test void consentWithdrawalIsRecheckedForBackgroundWrites() {
        var u=user();var day=today("UTC");var m=medication(u,day,null);schedule(m,"UTC",day,everyDay());
        consents.findAllByUserIdOrderByConsentedAtDesc(u.getId()).forEach(c->c.withdraw(Instant.now()));consents.flush();
        assertThrows(BusinessException.class,()->service.generateToday(u.getId(),m.getId()));
        assertTrue(rows(u,day).isEmpty());
    }

    @Test void stopAndAccountWithdrawalPreventGenerationFromStaleCandidate() {
        var u=user();var day=today("UTC");var m=medication(u,day,null);schedule(m,"UTC",day,everyDay());
        personal.close(u.getId(),m.getId(),"STOPPED");
        assertThrows(BusinessException.class,()->service.generateToday(u.getId(),m.getId()));
        assertTrue(rows(u,day).isEmpty());
        accounts.withdraw(u.getId());cleanup.remove(u.getId());
        assertThrows(BusinessException.class,()->service.generateToday(u.getId(),m.getId()));
    }

    @Test void keysetBatchesVisitAllCandidatesWithoutDuplicatesAndRestartScan() {
        var u=user();var day=today("UTC");
        for(int i=0;i<51;i++){var m=medication(u,day,null);schedule(m,"UTC",day,everyDay());}
        medication(u,day,null); // No schedule: excluded.
        var stopped=medication(u,day,null);schedule(stopped,"UTC",day,everyDay());stopped.close("STOPPED");medications.flush();
        var first=medications.findGenerationCandidates(null,PageRequest.of(0,50));
        assertEquals(50,first.getNumberOfElements());assertTrue(first.hasNext());
        var next=medications.findGenerationCandidates(first.getContent().getLast().getId(),PageRequest.of(0,50));
        assertEquals(1,next.getNumberOfElements());assertFalse(next.hasNext());
        var job=new MedicationGenerationJob(medications,service);
        assertEquals(50,job.runBatch().created());assertEquals(1,job.runBatch().created());
        assertEquals(0,job.runBatch().created());assertEquals(51,rows(u,day).size());
    }

    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void oneRejectedUserDoesNotRollBackAnotherUsersTransaction() {
        var denied=user();var allowed=user();var day=today("UTC");
        for(var u:List.of(denied,allowed)){var m=medication(u,day,null);schedule(m,"UTC",day,everyDay());}
        // Repository save commits each change, so the worker must use its own transaction boundaries.
        var records=consents.findAllByUserIdOrderByConsentedAtDesc(denied.getId());
        records.forEach(c->c.withdraw(Instant.now()));consents.saveAllAndFlush(records);
        var result=new MedicationGenerationJob(medications,service).runBatch();
        assertEquals(2,result.processed());assertEquals(1,result.rejected());assertEquals(0,result.failed());
        assertEquals(1,result.created());assertTrue(rows(denied,day).isEmpty());assertEquals(1,rows(allowed,day).size());
    }

    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void automaticAndManualRequestsRacingCreateOnlyOneOccurrence() throws Exception {
        var u=user();var day=today("UTC");var m=medication(u,day,null);schedule(m,"UTC",day,everyDay());
        var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var automatic=executor.submit(()->{ready.countDown();start.await();return service.generateToday(u.getId(),m.getId());});
            var manual=executor.submit(()->{ready.countDown();start.await();return (int)service.generate(u.getId(),m.getId(),day).get("created");});
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            assertEquals(1,automatic.get(10,TimeUnit.SECONDS)+manual.get(10,TimeUnit.SECONDS));
        } finally {start.countDown();executor.shutdownNow();}
        assertEquals(1,rows(u,day).size());
    }
}
