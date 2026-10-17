package com.routive.healthhabit.medication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import com.jayway.jsonpath.JsonPath;
import com.routive.healthhabit.auth.application.TokenService;
import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.user.domain.*;
import java.time.Instant;
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

@SpringBootTest
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Transactional
@Sql("/drug-dictionary-fixtures.sql")
class PersonalMedicationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserConsentRepository consents;
    @Autowired TokenService tokens;
    @Autowired com.routive.healthhabit.medication.domain.PersonalMedicationRepository medications;
    @Autowired com.routive.healthhabit.user.application.UserAccountService accounts;
    @MockitoBean AccountMailSender mail;
    String token;
    User user;
    private static final String CUSTOM="{\"customName\":\"나의 보충제\",\"medicationType\":\"SUPPLEMENT\",\"startDate\":\"2026-10-17\"}";
    @BeforeEach void setup(){
        user=users.saveAndFlush(User.createSocial(UUID.randomUUID()+"@example.test",Instant.now()));
        agree(user); token=tokens.issue(user,Instant.now()).accessToken();
    }
    void agree(User u){
        for(String type:List.of("TERMS_OF_SERVICE","PRIVACY","SENSITIVE_HEALTH_DATA"))
            consents.save(UserConsent.agreed(u,type,"v1",Instant.now()));
        consents.flush();
    }
    ResultActions create(String body,String key)throws Exception{
        return mvc.perform(post("/api/v1/medications").header("Authorization","Bearer "+token)
            .header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    String createId()throws Exception{
        return JsonPath.read(create(CUSTOM,UUID.randomUUID().toString()).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(),"$.data.medicationId");
    }
    @Test void createsAndReadsOwnCustomMedication()throws Exception{
        String id=createId();
        mvc.perform(get("/api/v1/medications/"+id).header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("나의 보충제"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE")).andExpect(jsonPath("$.data.version").value(0));
        mvc.perform(get("/api/v1/medications").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
    }
    @Test void createsFromActiveDictionaryAndRejectsInactiveOrWrongType()throws Exception{
        String body="{\"drugId\":\"11111111-1111-4111-8111-111111111111\",\"medicationType\":\"OTC\",\"startDate\":\"2026-10-17\"}";
        create(body,UUID.randomUUID().toString()).andExpect(status().isOk()).andExpect(jsonPath("$.data.drugId").isNotEmpty());
        create(body.replace("OTC","SUPPLEMENT"),UUID.randomUUID().toString()).andExpect(status().isBadRequest());
        create(body.replace("11111111-1111-4111-8111-111111111111","55555555-5555-4555-8555-555555555555"),
            UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }
    @Test void repeatedKeyReplaysAndConflictingBodyReturns409()throws Exception{
        String key=UUID.randomUUID().toString();
        String a=JsonPath.read(create(CUSTOM,key).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),"$.data.medicationId");
        create(CUSTOM,key).andExpect(status().isOk()).andExpect(jsonPath("$.data.medicationId").value(a));
        create(CUSTOM.replace("나의 보충제","다른 보충제"),key).andExpect(status().isConflict());
    }
    @Test void otherUserCannotReadEditOrStopAndHasEmptyList()throws Exception{
        String id=createId();
        User other=users.saveAndFlush(User.createSocial(UUID.randomUUID()+"@example.test",Instant.now()));
        agree(other); token=tokens.issue(other,Instant.now()).accessToken();
        mvc.perform(get("/api/v1/medications/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"memo\":\"변경\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/medications/"+id+"/stop").header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/medications").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }
    @Test void patchChecksVersionAndExplicitlyClearsOptionalFields()throws Exception{
        String id=createId();
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"memo\":\"메모\",\"endDate\":\"2026-10-20\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"memo\":\"오래된 수정\"}")).andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"clearFields\":[\"memo\",\"endDate\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.memo").doesNotExist())
            .andExpect(jsonPath("$.data.endDate").doesNotExist());
    }
    @Test void stopAndCompleteAreTerminalAndIdempotent()throws Exception{
        String id=createId();
        for(int i=0;i<2;i++)mvc.perform(post("/api/v1/medications/"+id+"/stop").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("STOPPED"));
        mvc.perform(post("/api/v1/medications/"+id+"/complete").header("Authorization","Bearer "+token)).andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"memo\":\"수정\"}")).andExpect(status().isConflict());
        String second=createId();
        mvc.perform(post("/api/v1/medications/"+second+"/complete").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mvc.perform(get("/api/v1/medications").param("status","STOPPED").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
    }
    @Test void invalidCreateFieldsAndUnsupportedScheduleReturn400()throws Exception{
        for(String body:List.of("{}",CUSTOM.replace("나의 보충제","  "),CUSTOM.replace("SUPPLEMENT","INVALID"),
            CUSTOM.replace("\"startDate\":\"2026-10-17\"","\"startDate\":\"2026-10-17\",\"endDate\":\"2026-10-16\""),
            CUSTOM.replace("\"startDate\":\"2026-10-17\"","\"startDate\":\"2026-10-17\",\"schedule\":{}"),
            CUSTOM.replace("\"customName\"","\"drugId\":\"11111111-1111-4111-8111-111111111111\",\"customName\"")))
            create(body,UUID.randomUUID().toString()).andExpect(status().isBadRequest());
    }
    @Test void missingOrMalformedKeyAndMissingAuthenticationAreRejected()throws Exception{
        mvc.perform(post("/api/v1/medications").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(CUSTOM)).andExpect(status().isBadRequest());
        create(CUSTOM,"invalid").andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/medications")).andExpect(status().isUnauthorized());
    }
    @Test void withdrawnConsentBlocksHealthData()throws Exception{
        String id=createId();
        consents.findAllByUserIdOrderByConsentedAtDesc(user.getId()).stream()
            .filter(c->c.getConsentType().equals("SENSITIVE_HEALTH_DATA")).forEach(c->c.withdraw(Instant.now()));
        consents.flush();
        mvc.perform(get("/api/v1/medications/"+id).header("Authorization","Bearer "+token))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("REQUIRED_CONSENT_MISSING"));
        create(CUSTOM,UUID.randomUUID().toString()).andExpect(status().isBadRequest());
    }
    @Test void invalidPaginationAndPatchCannotChangeProductIdentity()throws Exception{
        String id=createId();
        mvc.perform(get("/api/v1/medications").param("size","101").header("Authorization","Bearer "+token))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/medications").param("status","INVALID").header("Authorization","Bearer "+token))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/medications/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"medicationType\":\"OTC\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void withdrawalDeletesPersonalHealthRecord()throws Exception{
        UUID id=UUID.fromString(createId());
        accounts.withdraw(user.getId());
        medications.flush();
        assertFalse(medications.existsById(id));
        mvc.perform(get("/api/v1/medications").header("Authorization","Bearer "+token))
            .andExpect(status().isUnauthorized());
    }
}
