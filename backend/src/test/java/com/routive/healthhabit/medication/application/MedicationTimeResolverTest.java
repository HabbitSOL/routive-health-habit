package com.routive.healthhabit.medication.application;
import static org.junit.jupiter.api.Assertions.*;
import com.routive.healthhabit.common.error.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;
class MedicationTimeResolverTest {
    @Test void convertsSeoulWithoutChangingLocalDate(){
        assertEquals(Instant.parse("2026-10-16T23:00:00Z"),
            MedicationTimeResolver.resolve(LocalDate.parse("2026-10-17"),LocalTime.of(8,0),ZoneId.of("Asia/Seoul")));
    }
    @Test void rejectsNonexistentSpringClockTime(){
        assertThrows(BusinessException.class,()->MedicationTimeResolver.resolve(
            LocalDate.parse("2026-03-08"),LocalTime.of(2,30),ZoneId.of("America/New_York")));
    }
    @Test void rejectsAmbiguousFallClockTime(){
        assertThrows(BusinessException.class,()->MedicationTimeResolver.resolve(
            LocalDate.parse("2026-11-01"),LocalTime.of(1,30),ZoneId.of("America/New_York")));
    }
}
