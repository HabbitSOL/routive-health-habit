package com.routive.healthhabit.medication.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="medication_schedules")
public class FixedMedicationSchedule {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="medication_id",nullable=false,columnDefinition="BINARY(16)") private UUID medicationId;
    @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
    @Column(name="effective_until") private LocalDate effectiveUntil;
    @Column(nullable=false,length=64) private String timezone;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @ElementCollection @CollectionTable(name="medication_schedule_days",joinColumns=@JoinColumn(name="schedule_id"))
    @Column(name="day_of_week",nullable=false) private Set<Integer> days=new HashSet<>();
    @ElementCollection @CollectionTable(name="medication_schedule_times",joinColumns=@JoinColumn(name="schedule_id"))
    @OrderColumn(name="slot_no") private List<Slot> slots=new ArrayList<>();
    protected FixedMedicationSchedule(){}
    public FixedMedicationSchedule(UUID medication,LocalDate from,String zone,Set<Integer> days,List<Slot> slots){
        id=UUID.randomUUID();medicationId=medication;effectiveFrom=from;timezone=zone;
        createdAt=Instant.now();this.days.addAll(days);this.slots.addAll(slots);
    }
    public void endAt(LocalDate date){effectiveUntil=date;}
    public boolean covers(LocalDate date){return !date.isBefore(effectiveFrom)&&(effectiveUntil==null||date.isBefore(effectiveUntil));}
    public UUID getId(){return id;} public LocalDate getEffectiveFrom(){return effectiveFrom;}
    public LocalDate getEffectiveUntil(){return effectiveUntil;} public String getTimezone(){return timezone;}
    public Set<Integer> getDays(){return Set.copyOf(days);} public List<Slot> getSlots(){return List.copyOf(slots);}
    @Embeddable
    public static class Slot {
        @Column(name="fixed_time",nullable=false) private LocalTime fixedTime;
        @Column(name="dose_amount",nullable=false,precision=12,scale=3) private BigDecimal doseAmount;
        @Column(name="dose_unit_code",nullable=false,length=16) private String doseUnitCode;
        protected Slot(){}
        public Slot(LocalTime time,BigDecimal dose,String unit){fixedTime=time;doseAmount=dose;doseUnitCode=unit;}
        public LocalTime getFixedTime(){return fixedTime;} public BigDecimal getDoseAmount(){return doseAmount;}
        public String getDoseUnitCode(){return doseUnitCode;}
    }
}
