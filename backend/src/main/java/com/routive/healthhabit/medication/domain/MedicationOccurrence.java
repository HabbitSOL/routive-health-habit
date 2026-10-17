package com.routive.healthhabit.medication.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="medication_occurrences",uniqueConstraints=@UniqueConstraint(columnNames={"schedule_id","scheduled_date","slot_no"}))
public class MedicationOccurrence {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="user_id",nullable=false,columnDefinition="BINARY(16)") private UUID userId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="medication_id",nullable=false,columnDefinition="BINARY(16)") private UUID medicationId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="schedule_id",nullable=false,columnDefinition="BINARY(16)") private UUID scheduleId;
    @Column(name="scheduled_date",nullable=false) private LocalDate scheduledDate;
    @Column(name="slot_no",nullable=false) private int slotNo;
    @Column(name="scheduled_at",nullable=false) private Instant scheduledAt;
    @Column(nullable=false,length=64) private String timezone;
    @Column(name="medication_name",nullable=false,length=255) private String medicationName;
    @Column(name="dose_amount",nullable=false,precision=12,scale=3) private BigDecimal doseAmount;
    @Column(name="dose_unit_code",nullable=false,length=16) private String doseUnitCode;
    @Column(nullable=false,length=16) private String status;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected MedicationOccurrence(){}
    public MedicationOccurrence(UUID user,PersonalMedication medication,FixedMedicationSchedule schedule,
        LocalDate date,int slot,Instant at,FixedMedicationSchedule.Slot value){
        id=UUID.randomUUID();userId=user;medicationId=medication.getId();scheduleId=schedule.getId();
        scheduledDate=date;slotNo=slot;scheduledAt=at;timezone=schedule.getTimezone();
        medicationName=medication.getDisplayName();doseAmount=value.getDoseAmount();doseUnitCode=value.getDoseUnitCode();
        status="SCHEDULED";createdAt=Instant.now();
    }
    public void cancel(){if("SCHEDULED".equals(status))status="CANCELLED";}
    public UUID getId(){return id;} public UUID getMedicationId(){return medicationId;}
    public UUID getScheduleId(){return scheduleId;} public LocalDate getScheduledDate(){return scheduledDate;}
    public Instant getScheduledAt(){return scheduledAt;} public String getTimezone(){return timezone;}
    public String getMedicationName(){return medicationName;} public BigDecimal getDoseAmount(){return doseAmount;}
    public String getDoseUnitCode(){return doseUnitCode;} public String getStatus(){return status;}
}
