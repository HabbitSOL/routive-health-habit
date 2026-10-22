package com.routive.healthhabit.medication.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "medication-generation.enabled", havingValue = "true")
public class MedicationGenerationScheduler {
    private static final Logger log = LoggerFactory.getLogger(MedicationGenerationScheduler.class);
    private final MedicationGenerationJob job;

    public MedicationGenerationScheduler(MedicationGenerationJob job) { this.job = job; }

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void tick() {
        try {
            var result = job.runBatch();
            if (result.rejected() > 0 || result.failed() > 0) {
                log.warn("Medication generation: processed={}, created={}, rejected={}, failed={}, scanCompleted={}",
                        result.processed(), result.created(), result.rejected(), result.failed(), result.scanCompleted());
            } else if (result.created() > 0) {
                log.info("Medication generation: processed={}, created={}, scanCompleted={}",
                        result.processed(), result.created(), result.scanCompleted());
            }
        } catch (RuntimeException unavailable) {
            log.warn("Medication generation batch unavailable; retry on next tick");
        }
    }
}
