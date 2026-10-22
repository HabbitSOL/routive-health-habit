package com.routive.healthhabit.medication.application;

import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.medication.api.*;
import com.routive.healthhabit.medication.domain.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class MedicationActionService {
    private final PersonalMedicationService access;
    private final MedicationOccurrenceRepository occurrences;
    private final MedicationActionRepository actions;
    private final PersonalMedicationRepository medications;

    public MedicationActionService(PersonalMedicationService access, MedicationOccurrenceRepository occurrences,
            MedicationActionRepository actions, PersonalMedicationRepository medications) {
        this.access = access; this.occurrences = occurrences; this.actions = actions; this.medications = medications;
    }

    public MedicationActionResponse record(UUID user, UUID occurrenceId, UUID key, MedicationActionRequest request) {
        // Same user lock order as schedule changes, account withdrawal and consent changes.
        access.authorizeRecords(user, true);
        MedicationOccurrence occurrence = owned(user, occurrenceId);
        Instant actualAt = request.actualAt() == null ? null : request.actualAt().truncatedTo(ChronoUnit.MICROS);
        String memo = request.memo() == null || request.memo().isBlank() ? null : request.memo().strip();
        BigDecimal amount = request.actualDoseAmount();
        String hash = hash(occurrenceId, request.action(), actualAt, amount, request.doseUnitCode(), memo);
        var previous = actions.findByUserIdAndRequestKey(user, key.toString());
        if (previous.isPresent()) {
            if (!previous.get().getRequestHash().equals(hash)) conflict();
            return MedicationActionResponse.from(previous.get());
        }
        if (!"SCHEDULED".equals(occurrence.getStatus())) conflict();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        ZoneId zone = ZoneId.of(occurrence.getTimezone());
        if (occurrence.getScheduledDate().isAfter(now.atZone(zone).toLocalDate())) invalid();
        if ("TAKE".equals(request.action())) {
            if (actualAt == null || amount == null || !occurrence.getDoseUnitCode().equals(request.doseUnitCode())) invalid();
            if (actualAt.isAfter(now) || actualAt.isBefore(occurrence.getScheduledDate().atStartOfDay(zone).toInstant())) invalid();
            var medication = medications.findByIdAndUserId(occurrence.getMedicationId(), user)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_NOT_FOUND));
            if (medication.getClosedAt() != null && actualAt.isAfter(medication.getClosedAt())) invalid();
        } else if (!Set.of("SKIP", "NO_STOCK").contains(request.action())
                || actualAt != null || amount != null || request.doseUnitCode() != null) {
            invalid();
        }
        var action = new MedicationAction(user, occurrenceId, request.action(), actualAt, amount,
                request.doseUnitCode(), memo, now, key, hash);
        occurrence.resolve(action.status());
        actions.saveAndFlush(action);
        return MedicationActionResponse.from(action);
    }

    @Transactional(readOnly = true)
    public List<MedicationActionResponse> list(UUID user, UUID occurrenceId) {
        access.authorizeRecords(user, false);
        owned(user, occurrenceId);
        return actions.findByOccurrenceIdAndUserId(occurrenceId, user).stream()
                .map(MedicationActionResponse::from).toList();
    }

    private MedicationOccurrence owned(UUID user, UUID id) {
        return occurrences.findByIdAndUserId(id, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_OCCURRENCE_NOT_FOUND));
    }
    private static String hash(UUID id, String action, Instant at, BigDecimal amount, String unit, String memo) {
        StringBuilder canonical = new StringBuilder();
        for (Object value : Arrays.asList(id, action, at, amount == null ? null : amount.stripTrailingZeros().toPlainString(), unit, memo)) {
            String text = value == null ? null : value.toString();
            canonical.append(text == null ? "-1:" : text.length() + ":" + text);
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    private static void invalid() { throw new BusinessException(ErrorCode.INVALID_REQUEST); }
    private static void conflict() { throw new BusinessException(ErrorCode.DATA_CONFLICT); }
}
