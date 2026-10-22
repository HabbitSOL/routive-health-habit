package com.routive.healthhabit.medication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.routive.healthhabit.medication.application.*;
import com.routive.healthhabit.medication.domain.PersonalMedicationRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.*;

class MedicationGenerationJobTest {
    @Test @ExtendWith(OutputCaptureExtension.class)
    void schedulerRetriesAfterFailureWithoutLoggingExceptionPayload(CapturedOutput output) {
        var job=mock(MedicationGenerationJob.class);
        when(job.runBatch()).thenThrow(new IllegalStateException("fake-confidential-value"))
                .thenReturn(new MedicationGenerationJob.Result(1,1,0,0,true));
        var scheduler=new MedicationGenerationScheduler(job);
        scheduler.tick();scheduler.tick();verify(job,times(2)).runBatch();
        assertTrue(output.getAll().contains("retry on next tick"));
        assertFalse(output.getAll().contains("fake-confidential-value"));
    }
    @Test void unexpectedItemFailureDoesNotStopBatchAndIsRetriedOnNextScan() {
        var repo=mock(PersonalMedicationRepository.class);var service=mock(MedicationScheduleService.class);
        var bad=candidate();var good=candidate();
        when(repo.findGenerationCandidates(isNull(),any())).thenReturn(new SliceImpl<>(List.of(bad,good)));
        when(service.generateToday(bad.getUserId(),bad.getId())).thenThrow(new IllegalStateException("fake confidential payload"));
        when(service.generateToday(good.getUserId(),good.getId())).thenReturn(1);
        var job=new MedicationGenerationJob(repo,service);
        var result=job.runBatch();assertEquals(1,result.failed());assertEquals(1,result.created());assertTrue(result.scanCompleted());
        job.runBatch();verify(service,times(2)).generateToday(bad.getUserId(),bad.getId());
    }
    @Test void queryFailureDoesNotAdvanceCursor() {
        var repo=mock(PersonalMedicationRepository.class);var service=mock(MedicationScheduleService.class);
        when(repo.findGenerationCandidates(isNull(),any())).thenThrow(new IllegalStateException()).thenReturn(new SliceImpl<>(List.of()));
        var job=new MedicationGenerationJob(repo,service);
        assertThrows(IllegalStateException.class,job::runBatch);assertEquals(0,job.runBatch().processed());
        verify(repo,times(2)).findGenerationCandidates(isNull(),any());
    }
    @Test void schedulingIsOptInAndRegistersOnlyWhenEnabled() {
        var runner=new ApplicationContextRunner().withUserConfiguration(MedicationGenerationScheduler.class)
                .withBean(MedicationGenerationJob.class,()->mock(MedicationGenerationJob.class));
        runner.run(c->assertFalse(c.containsBean("medicationGenerationScheduler")));
        runner.withPropertyValues("medication-generation.enabled=true").run(c->{
            assertEquals(1,c.getBeansOfType(MedicationGenerationScheduler.class).size());
            assertEquals(1,c.getBeansOfType(org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class).size());
        });
    }
    private PersonalMedicationRepository.GenerationCandidate candidate() {
        var c=mock(PersonalMedicationRepository.GenerationCandidate.class);
        when(c.getId()).thenReturn(UUID.randomUUID());when(c.getUserId()).thenReturn(UUID.randomUUID());return c;
    }
}
