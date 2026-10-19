package com.routive.healthhabit.medication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import com.jayway.jsonpath.JsonPath;
import com.routive.healthhabit.auth.application.TokenService;
import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.medication.domain.*;
import com.routive.healthhabit.user.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE) @Transactional
class MedicationScheduleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserConsentRepository consents;
    @Autowired TokenService tokens;
    @Autowired FixedMedicationScheduleRepository schedules;
    @Autowired MedicationOccurrenceRepository occurrences;
    @Autowired com.routive.healthhabit.user.application.UserAccountService accounts;
    @MockitoBean AccountMailSender mail;
    String token,id;User user;
    LocalDate today=LocalDate.now(ZoneId.of("Asia/Seoul"));
    @BeforeEach void setup()throws Exception{
        user=users.saveAndFlush(User.createSocial(UUID.randomUUID()+"@example.test",Instant.now()));
        agree(user); token=tokens.issue(user,Instant.now()).accessToken();
        String body="{\"customName\":\"일정 약\",\"medicationType\":\"OTC\",\"startDate\":\""+today+"\"}";
        id=JsonPath.read(mvc.perform(post("/api/v1/medications").header("Authorization","Bearer "+token)
            .header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),"$.data.medicationId");
    }
    void agree(User u){
        for(String type:List.of("TERMS_OF_SERVICE","PRIVACY","SENSITIVE_HEALTH_DATA"))
            consents.save(UserConsent.agreed(u,type,"v1",Instant.now()));
        consents.flush();
    }
    String request(int version,LocalDate date){
        return "{\"medicationVersion\":"+version+",\"effectiveFrom\":\""+date+"\",\"timezone\":\"Asia/Seoul\","
            +"\"daysOfWeek\":[1,2,3,4,5,6,7],\"times\":[{\"sequence\":1,\"fixedTime\":\"08:00\","
            +"\"doseAmount\":1.5,\"doseUnitCode\":\"TABLET\",\"mealRelation\":\"NONE\"}]}";
    }
    ResultActions save(String body)throws Exception{
        return mvc.perform(post("/api/v1/medications/"+id+"/schedules").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    ResultActions generate(LocalDate date)throws Exception{
        return mvc.perform(post("/api/v1/medications/"+id+"/occurrences").param("date",date.toString())
            .header("Authorization","Bearer "+token));
    }
    ResultActions list(LocalDate date)throws Exception{
        return mvc.perform(get("/api/v1/medication-occurrences").param("date",date.toString())
            .header("Authorization","Bearer "+token));
    }
    @Test void generatesStableOccurrencesAndConvertsTimezone()throws Exception{
        save(request(0,today)).andExpect(status().isOk()).andExpect(jsonPath("$.data.medicationVersion").value(1));
        LocalDate date=today.plusDays(1);
        list(date).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        generate(date).andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(1));
        String first=JsonPath.read(list(date).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].scheduledAt")
            .value(date.atTime(8,0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
            .andReturn().getResponse().getContentAsString(),"$.data.items[0].occurrenceId");
        generate(date).andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(0));
        list(date).andExpect(jsonPath("$.data.items.length()").value(1)).andExpect(jsonPath("$.data.items[0].occurrenceId").value(first));
    }
    @Test void weekdaysAndMedicationDatesRestrictGeneration()throws Exception{
        int other=today.plusDays(1).getDayOfWeek().getValue()%7+1;
        save(request(0,today).replace("[1,2,3,4,5,6,7]","["+other+"]")).andExpect(status().isOk());
        generate(today.plusDays(1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(0));
    }
    @Test void revisionCancelsOnlyReplacedFutureAndPreservesHistory()throws Exception{
        save(request(0,today)).andExpect(status().isOk());
        LocalDate retained=today.plusDays(1),changed=today.plusDays(2);
        generate(retained).andExpect(status().isOk());
        generate(changed).andExpect(status().isOk());
        save(request(1,changed).replace("08:00","09:00")).andExpect(status().isOk());
        list(retained).andExpect(jsonPath("$.data.items[0].status").value("SCHEDULED"));
        list(changed).andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"));
        generate(changed).andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(1));
        list(changed).andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[1].status").value("SCHEDULED"));
        mvc.perform(get("/api/v1/medications/"+id+"/schedules").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].effectiveUntil").value(changed.toString()));
    }
    @Test void staleVersionAndBackdatedRevisionAreRejected()throws Exception{
        save(request(0,today)).andExpect(status().isOk());
        save(request(0,today.plusDays(1))).andExpect(status().isConflict());
        save(request(1,today)).andExpect(status().isBadRequest());
        save(request(1,today.plusDays(1)).replace("Asia/Seoul","UTC")).andExpect(status().isBadRequest());
    }
    @Test void invalidDaysTimesDoseAndMealScheduleReturn400()throws Exception{
        for(String body:List.of(request(0,today).replace("[1,2,3,4,5,6,7]","[1,1]"),
            request(0,today).replace("08:00","25:00"),request(0,today).replace("1.5","0"),
            request(0,today).replace("NONE","AFTER_MEAL"),request(0,today).replace("Asia/Seoul","Invalid/Zone"),
            request(0,today).replace("\"sequence\":1","\"sequence\":2")))
            save(body).andExpect(status().isBadRequest());
    }
    @Test void stoppedMedicationCancelsFutureAndCannotGenerate()throws Exception{
        save(request(0,today)).andExpect(status().isOk());generate(today.plusDays(1)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/medications/"+id+"/stop").header("Authorization","Bearer "+token))
            .andExpect(status().isOk());
        list(today.plusDays(1)).andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"));
        generate(today.plusDays(2)).andExpect(status().isConflict());
    }
    @Test void otherUserCannotManageOrReadOccurrences()throws Exception{
        save(request(0,today)).andExpect(status().isOk());generate(today.plusDays(1)).andExpect(status().isOk());
        User other=users.saveAndFlush(User.createSocial(UUID.randomUUID()+"@example.test",Instant.now()));
        agree(other);token=tokens.issue(other,Instant.now()).accessToken();
        save(request(1,today.plusDays(1))).andExpect(status().isNotFound());
        generate(today.plusDays(1)).andExpect(status().isNotFound());
        list(today.plusDays(1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }
    @Test void generationBoundsAndMissingScheduleAreRejected()throws Exception{
        generate(today).andExpect(status().isBadRequest());
        save(request(0,today)).andExpect(status().isOk());
        generate(today.minusDays(1)).andExpect(status().isBadRequest());
        generate(today.plusDays(91)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/medication-occurrences").header("Authorization","Bearer "+token))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/medication-occurrences").param("date",today.toString())).andExpect(status().isUnauthorized());
    }
    @Test void datesCannotChangeAfterScheduleAndSnapshotDoesNotFollowRename()throws Exception{
        save(request(0,today)).andExpect(status().isOk());generate(today.plusDays(1)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"startDate\":\""+today.plusDays(1)+"\"}"))
            .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"customName\":\"새 이름\"}"))
            .andExpect(status().isOk());
        list(today.plusDays(1)).andExpect(jsonPath("$.data.items[0].medicationName").value("일정 약"));
    }
    @Test void withdrawalDeletesOccurrencesAndScheduleChildren()throws Exception{
        save(request(0,today)).andExpect(status().isOk());generate(today.plusDays(1)).andExpect(status().isOk());
        accounts.withdraw(user.getId());
        assertTrue(schedules.findAllByMedicationIdOrderByEffectiveFromAsc(UUID.fromString(id)).isEmpty());
        assertTrue(occurrences.findAllByUserIdAndScheduledDateOrderByScheduledAtAscIdAsc(user.getId(),today.plusDays(1)).isEmpty());
    }
}
