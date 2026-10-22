package com.routive.healthhabit.meal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;
import com.routive.healthhabit.auth.application.TokenService;
import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.meal.api.MealWriteRequest;
import com.routive.healthhabit.meal.application.MealService;
import com.routive.healthhabit.meal.domain.*;
import com.routive.healthhabit.user.application.UserAccountService;
import com.routive.healthhabit.user.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE) @Transactional
class MealIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserConsentRepository consents;
    @Autowired TokenService tokens;
    @Autowired MealRepository meals;
    @Autowired MealItemRepository items;
    @Autowired MealService service;
    @Autowired UserAccountService accounts;
    @Autowired ObjectMapper mapper;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    @MockitoBean AccountMailSender mail;
    User user;String token;
    Instant eaten;
    LocalDate day;
    private final List<UUID> cleanup=new ArrayList<>();
    @BeforeEach void setup() {
        eaten=Instant.now().minusSeconds(3600).truncatedTo(ChronoUnit.MICROS);
        day=eaten.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        user=newUser();token=tokens.issue(user,Instant.now()).accessToken();
    }
    User newUser() {
        var u=users.saveAndFlush(User.createSocial(UUID.randomUUID()+"@example.invalid",Instant.now()));
        cleanup.add(u.getId());
        for(String type:List.of("TERMS_OF_SERVICE","PRIVACY","SENSITIVE_HEALTH_DATA"))
            consents.saveAndFlush(UserConsent.agreed(u,type,"v1",Instant.now()));
        return u;
    }
    @AfterEach void cleanup() {cleanup.forEach(accounts::withdraw);}
    String nutrients() {
        return "{\"caloriesKcal\":165,\"carbohydrateG\":0,\"proteinG\":31,\"fatG\":3.6,\"sugarG\":0,\"caffeineMg\":0}";
    }
    String item(String name,String amount,String nutrients) {
        return "{\"foodName\":\""+name+"\",\"amount\":"+amount+",\"unitCode\":\"G\",\"nutrients\":"+nutrients+"}";
    }
    String body(String... entries) {
        return "{\"mealType\":\"LUNCH\",\"eatenAt\":\""+eaten+"\",\"status\":\"COMPLETED\",\"items\":["+String.join(",",entries)+"]}";
    }
    String normal() {return body(item("닭가슴살","150",nutrients()));}
    ResultActions create(String body,String key)throws Exception {
        return mvc.perform(post("/api/v1/meals").header("Authorization","Bearer "+token)
                .header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    String createId()throws Exception {
        return JsonPath.read(create(normal(),UUID.randomUUID().toString()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),"$.data.mealId");
    }
    ResultActions getMeal(String id)throws Exception {
        return mvc.perform(get("/api/v1/meals/"+id).header("Authorization","Bearer "+token));
    }
    ResultActions cancel(String id,String version)throws Exception {
        return mvc.perform(delete("/api/v1/meals/"+id).param("version",version).header("Authorization","Bearer "+token));
    }
    ResultActions list(String status,int page,int size)throws Exception {
        return mvc.perform(get("/api/v1/meals").param("from",day.toString()).param("to",day.toString())
                .param("status",status).param("page",""+page).param("size",""+size).header("Authorization","Bearer "+token));
    }
    ResultActions stats()throws Exception {
        return mvc.perform(get("/api/v1/statistics/meals").param("from",day.toString()).param("to",day.toString())
                .header("Authorization","Bearer "+token));
    }

    @Test void storesActualTimeSeparateFromRecordedTimeAndPortionNutritionSnapshot()throws Exception {
        String id=createId();
        String json=getMeal(id).andExpect(status().isOk()).andExpect(jsonPath("$.data.eatenAt").value(eaten.toString()))
                .andExpect(jsonPath("$.data.mealDate").value(day.toString()))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.items[0].amount").value(150))
                .andExpect(jsonPath("$.data.items[0].source").value("USER_ENTERED"))
                .andExpect(jsonPath("$.data.totals.caloriesKcal").value(165))
                .andExpect(jsonPath("$.data.version").value(0)).andReturn().getResponse().getContentAsString();
        assertTrue(Instant.parse(JsonPath.read(json,"$.data.recordedAt")).isAfter(eaten));
    }
    @Test void addsSixNutrientsExactlyWithoutMultiplyingPortionAmount()throws Exception {
        create(body(item("A","150",nutrients()),item("B","2","{\"caloriesKcal\":0.125,\"carbohydrateG\":0.1,\"proteinG\":0.2,\"fatG\":0.3,\"sugarG\":0.4,\"caffeineMg\":20.005}")),
                UUID.randomUUID().toString()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totals.caloriesKcal").value(165.125))
                .andExpect(jsonPath("$.data.totals.carbohydrateG").value(0.1))
                .andExpect(jsonPath("$.data.totals.proteinG").value(31.2))
                .andExpect(jsonPath("$.data.totals.fatG").value(3.9))
                .andExpect(jsonPath("$.data.totals.sugarG").value(0.4))
                .andExpect(jsonPath("$.data.totals.caffeineMg").value(20.005));
        stats().andExpect(jsonPath("$.data.mealCount").value(1)).andExpect(jsonPath("$.data.itemCount").value(2))
                .andExpect(jsonPath("$.data.totals.caloriesKcal").value(165.125));
    }
    @Test void unknownNutrientIsNullAndKnownSubtotalIsExplicit()throws Exception {
        create(body(item("A","1",nutrients()),item("B","1","{\"caloriesKcal\":20}")),UUID.randomUUID().toString())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totals.caloriesKcal").value(185))
                .andExpect(jsonPath("$.data.totals.proteinG").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.knownTotals.proteinG").value(31))
                .andExpect(jsonPath("$.data.missingItemCounts.proteinG").value(1));
        stats().andExpect(status().isOk()).andExpect(jsonPath("$.data.totals.proteinG").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.knownTotals.proteinG").value(31))
                .andExpect(jsonPath("$.data.missingItemCounts.proteinG").value(1));
    }
    @Test void entirelyUnknownEmbeddedValuesRemainUnknownAfterReload()throws Exception {
        String id=JsonPath.read(create(body(item("unknown","1","null")),UUID.randomUUID().toString()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),"$.data.mealId");
        entityManager.flush();entityManager.clear();
        getMeal(id).andExpect(jsonPath("$.data.items[0].nutrients.caloriesKcal").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.missingItemCounts.caffeineMg").value(1));
        stats().andExpect(jsonPath("$.data.totals.caloriesKcal").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.knownTotals.caloriesKcal").value(0));
    }
    @Test void replayNormalizesNumericPrecisionWhitespaceAndTimestampOffset()throws Exception {
        String key=UUID.randomUUID().toString();
        String json=create(normal(),key).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id=JsonPath.read(json,"$.data.mealId"),recorded=JsonPath.read(json,"$.data.recordedAt");
        String equivalent=body(item("  닭가슴살  ","150.000",nutrients())).replace(eaten.toString(),eaten.atOffset(ZoneOffset.ofHours(9)).toString());
        create(equivalent,key).andExpect(status().isOk()).andExpect(jsonPath("$.data.mealId").value(id))
                .andExpect(jsonPath("$.data.recordedAt").value(recorded));
        assertEquals(1,meals.count());
    }
    @Test void reusedKeyWithDifferentFoodOrNutrientsReturnsConflict()throws Exception {
        String key=UUID.randomUUID().toString();create(normal(),key).andExpect(status().isOk());
        create(normal().replace("닭가슴살","두부"),key).andExpect(status().isConflict());
        create(normal().replace("165","166"),key).andExpect(status().isConflict());
        assertEquals(1,items.count());
    }
    @Test void cancellationPreservesOriginalAndExcludesItFromStatistics()throws Exception {
        String key=UUID.randomUUID().toString();
        String id=JsonPath.read(create(normal(),key).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),"$.data.mealId");
        String first=cancel(id,"0").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.version").value(1)).andReturn().getResponse().getContentAsString();
        cancel(id,"0").andExpect(status().isOk()).andExpect(jsonPath("$.data.cancelledAt").value((String)JsonPath.read(first,"$.data.cancelledAt")));
        getMeal(id).andExpect(jsonPath("$.data.items[0].foodName").value("닭가슴살"));
        create(normal(),key).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        stats().andExpect(jsonPath("$.data.mealCount").value(0)).andExpect(jsonPath("$.data.itemCount").value(0))
                .andExpect(jsonPath("$.data.totals.caloriesKcal").value(0));
        list("COMPLETED",0,20).andExpect(jsonPath("$.data.totalElements").value(0));
        list("CANCELLED",0,20).andExpect(jsonPath("$.data.totalElements").value(1));
    }
    @Test void cancellationChecksVersionAndInput()throws Exception {
        String id=createId();
        cancel(id,"1").andExpect(status().isConflict());cancel(id,"-1").andExpect(status().isBadRequest());
        cancel(id,"bad").andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/meals/"+id).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        getMeal(id).andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
    @Test void otherUserCannotReadCancelListOrAggregateMyMeals()throws Exception {
        String id=createId();var other=newUser();token=tokens.issue(other,Instant.now()).accessToken();
        getMeal(id).andExpect(status().isNotFound());cancel(id,"0").andExpect(status().isNotFound());
        getMeal(UUID.randomUUID().toString()).andExpect(status().isNotFound());
        list("COMPLETED",0,20).andExpect(jsonPath("$.data.totalElements").value(0));
        stats().andExpect(jsonPath("$.data.mealCount").value(0)).andExpect(jsonPath("$.data.knownTotals.proteinG").value(0));
    }
    @Test void consentWithdrawalBlocksReadWriteCancelStatisticsAndReplay()throws Exception {
        String key=UUID.randomUUID().toString();
        String id=JsonPath.read(create(normal(),key).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),"$.data.mealId");
        consents.findAllByUserIdOrderByConsentedAtDesc(user.getId()).forEach(c->c.withdraw(Instant.now()));consents.flush();
        getMeal(id).andExpect(status().isBadRequest());cancel(id,"0").andExpect(status().isBadRequest());
        create(normal(),key).andExpect(status().isBadRequest());list("COMPLETED",0,20).andExpect(status().isBadRequest());
        stats().andExpect(status().isBadRequest());
    }
    @Test void allMealEndpointsRequireAuthentication()throws Exception {
        String id=createId();
        mvc.perform(get("/api/v1/meals/"+id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/meals").header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(normal()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/meals/"+id).param("version","0")).andExpect(status().isUnauthorized());
        for(String path:List.of("/api/v1/meals","/api/v1/statistics/meals"))
            mvc.perform(get(path).param("from",day.toString()).param("to",day.toString())).andExpect(status().isUnauthorized());
    }
    @Test void malformedIdentifiersAndMissingKeysReturn400()throws Exception {
        create(normal(),"invalid").andExpect(status().isBadRequest());getMeal("invalid").andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/meals").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(normal()))
                .andExpect(status().isBadRequest());assertEquals(0,meals.count());
    }
    @Test void invalidQuantityNutrientsEnumsAndPrecisionDoNotPartiallySave()throws Exception {
        for(String json:List.of(normal().replace("\"amount\":150","\"amount\":0"),
                normal().replace("\"amount\":150","\"amount\":1.0001"),
                normal().replace("165","-1"),normal().replace("165","1000000000"),
                normal().replace("LUNCH","INVALID"),normal().replace("COMPLETED","SKIPPED"),
                normal().replace("\"unitCode\":\"G\"","\"unitCode\":\"KG\""),
                normal().replace("닭가슴살","   "),body("null")))
            create(json,UUID.randomUUID().toString()).andExpect(status().isBadRequest());
        assertEquals(0,meals.count());assertEquals(0,items.count());
    }
    @Test void requestCollectionsAndTextAreBounded()throws Exception {
        for(String json:List.of(body(),normal().replace("닭가슴살","a".repeat(256)),
                normal().replace("\"items\":","\"memo\":\""+"m".repeat(1001)+"\",\"items\":"),
                body(Collections.nCopies(31,item("x","1",nutrients())).toArray(String[]::new))))
            create(json,UUID.randomUUID().toString()).andExpect(status().isBadRequest());
        assertEquals(0,meals.count());
    }
    @Test void unimplementedFoodDictionaryAndImagesAreRejected()throws Exception {
        create(normal().replace("\"foodName\":","\"foodId\":\""+UUID.randomUUID()+"\",\"foodName\":"),UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());
        create(normal().replace("\"items\":","\"imageFileKey\":\"untrusted-key\",\"items\":"),UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());assertEquals(0,meals.count());
    }
    @Test void futureAndUnsupportedPastTimesAreRejected()throws Exception {
        create(normal().replace(eaten.toString(),Instant.now().plusSeconds(3600).toString()),UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());
        create(normal().replace(eaten.toString(),"1969-12-31T23:59:59Z"),UUID.randomUUID().toString()).andExpect(status().isBadRequest());
        create(normal().replace("\"eatenAt\":\""+eaten+"\"","\"eatenAt\":null"),UUID.randomUUID().toString()).andExpect(status().isBadRequest());
    }
    @Test void listUsesStablePaginationAndSnapshotsLocalDate()throws Exception {
        createId();
        create(normal().replace(eaten.toString(),eaten.minusSeconds(60).toString()),UUID.randomUUID().toString()).andExpect(status().isOk());
        list("COMPLETED",0,1).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true)).andExpect(jsonPath("$.data.items[0].eatenAt").value(eaten.toString()));
        list("COMPLETED",1,1).andExpect(jsonPath("$.data.hasNext").value(false));
        list("COMPLETED",2,1).andExpect(jsonPath("$.data.items").isEmpty());
    }
    @Test void invalidRangesAndPaginationAreRejected()throws Exception {
        for(String status:List.of("ALL","INVALID"))list(status,0,20).andExpect(status().isBadRequest());
        list("COMPLETED",-1,20).andExpect(status().isBadRequest());list("COMPLETED",0,101).andExpect(status().isBadRequest());
        list("COMPLETED",100001,1).andExpect(status().isBadRequest());
        for(String path:List.of("/api/v1/meals","/api/v1/statistics/meals")){
            mvc.perform(get(path).param("from",day.toString()).param("to",day.minusDays(1).toString()).header("Authorization","Bearer "+token))
                    .andExpect(status().isBadRequest());
            mvc.perform(get(path).param("from",day.minusDays(366).toString()).param("to",day.toString()).header("Authorization","Bearer "+token))
                    .andExpect(status().isBadRequest());
            mvc.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
            mvc.perform(get(path).param("from","9999-12-31").param("to","+10000-01-01").header("Authorization","Bearer "+token))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test void emptyAndDateLimitedStatisticsAreExplicit()throws Exception {
        stats().andExpect(status().isOk()).andExpect(jsonPath("$.data.mealCount").value(0))
                .andExpect(jsonPath("$.data.totals.caloriesKcal").value(0)).andExpect(jsonPath("$.data.missingItemCounts.caloriesKcal").value(0));
        createId();
        create(normal().replace(eaten.toString(),eaten.minusSeconds(86400).toString()),UUID.randomUUID().toString()).andExpect(status().isOk());
        stats().andExpect(jsonPath("$.data.mealCount").value(1)).andExpect(jsonPath("$.data.totals.caloriesKcal").value(165));
    }
    @Test void withdrawalDeletesOnlyMyFoodSnapshotsIncludingCancelledRecords()throws Exception {
        String id=createId();cancel(id,"0").andExpect(status().isOk());
        var other=newUser();String original=token;token=tokens.issue(other,Instant.now()).accessToken();String retained=createId();token=original;
        accounts.withdraw(user.getId());cleanup.remove(user.getId());
        entityManager.flush();entityManager.clear();
        assertEquals(1,meals.count());assertEquals(1,items.count());assertTrue(meals.findById(UUID.fromString(id)).isEmpty());
        assertTrue(meals.findById(UUID.fromString(retained)).isPresent());getMeal(id).andExpect(status().isUnauthorized());
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentSameKeyCreatesOneMealAndSameId()throws Exception {
        var request=mapper.readValue(normal(),MealWriteRequest.class);UUID key=UUID.randomUUID();
        var results=race(()->service.create(user.getId(),key,request).mealId().toString(),
                ()->service.create(user.getId(),key,request).mealId().toString());
        assertEquals(results.get(0),results.get(1));
        assertEquals(1,meals.countByUserIdAndMealDateBetweenAndStatus(user.getId(),day,day,"COMPLETED"));
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentSameKeyWithDifferentPayloadHasOneWinner()throws Exception {
        UUID key=UUID.randomUUID();
        var a=mapper.readValue(normal(),MealWriteRequest.class);
        var b=mapper.readValue(normal().replace("닭가슴살","두부"),MealWriteRequest.class);
        var results=race(()->tryCreate(key,a),()->tryCreate(key,b));
        assertEquals(1,results.stream().filter("DATA_CONFLICT"::equals).count());
        assertEquals(1,meals.countByUserIdAndMealDateBetweenAndStatus(user.getId(),day,day,"COMPLETED"));
    }
    private String tryCreate(UUID key,MealWriteRequest request){
        try{return service.create(user.getId(),key,request).mealId().toString();}
        catch(BusinessException e){return e.errorCode().name();}
    }
    private List<String> race(Callable<String> a,Callable<String> b)throws Exception {
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try{
            var x=executor.submit(()->{ready.countDown();start.await();return a.call();});
            var y=executor.submit(()->{ready.countDown();start.await();return b.call();});
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            return List.of(x.get(10,TimeUnit.SECONDS),y.get(10,TimeUnit.SECONDS));
        }finally{start.countDown();executor.shutdownNow();}
    }
}
