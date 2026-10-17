package com.routive.healthhabit.medication.application;
import com.routive.healthhabit.common.error.*;
import java.time.*;
final class MedicationTimeResolver {
    private MedicationTimeResolver(){}
    static Instant resolve(LocalDate date,LocalTime time,ZoneId zone){
        LocalDateTime local=date.atTime(time);
        var offsets=zone.getRules().getValidOffsets(local);
        if(offsets.size()!=1)throw new BusinessException(ErrorCode.INVALID_REQUEST);
        return local.toInstant(offsets.getFirst());
    }
}
