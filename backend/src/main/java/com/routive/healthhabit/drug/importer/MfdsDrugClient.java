package com.routive.healthhabit.drug.importer;

import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

@Component
public class MfdsDrugClient {
    static final String ENDPOINT = "https://apis.data.go.kr/1471000/DrbEasyDrugInfoService/getDrbEasyDrugList";
    private static final int MAX_RESPONSE_BYTES = 10 * 1024 * 1024;
    private final MfdsImportProperties properties;
    private final RestClient client;
    private final ObjectMapper mapper;

    @Autowired
    public MfdsDrugClient(MfdsImportProperties properties, ObjectMapper mapper) {
        this(properties, mapper, defaultClient());
    }

    MfdsDrugClient(MfdsImportProperties properties, ObjectMapper mapper, RestClient client) {
        this.properties = properties; this.mapper = mapper; this.client = client;
    }

    private static RestClient defaultClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    public Page fetch(int pageNo) {
        if (properties.serviceKey() == null || properties.serviceKey().isBlank()) {
            throw new DrugImportException(DrugImportException.Code.NOT_CONFIGURED);
        }
        if (pageNo < 1) throw new DrugImportException(DrugImportException.Code.RESPONSE_INVALID);
        URI uri = UriComponentsBuilder.fromUriString(ENDPOINT)
                .queryParam("serviceKey", "{key}").queryParam("pageNo", pageNo)
                .queryParam("numOfRows", properties.pageSize()).queryParam("type", "json")
                .encode().buildAndExpand(properties.serviceKey()).toUri();
        try {
            return client.get().uri(uri).exchange((request, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new DrugImportException(DrugImportException.Code.HTTP_ERROR);
                }
                byte[] data = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                if (data.length > MAX_RESPONSE_BYTES) throw invalid();
                return parse(data, pageNo);
            });
        } catch (DrugImportException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DrugImportException(DrugImportException.Code.HTTP_ERROR);
        }
    }

    private Page parse(byte[] bytes, int requestedPage) {
        try {
            Map<?, ?> root = mapper.readValue(bytes, Map.class);
            if (!(root.get("header") instanceof Map<?, ?> header)) throw invalid();
            if (!"00".equals(String.valueOf(header.get("resultCode")))) {
                throw new DrugImportException(DrugImportException.Code.UPSTREAM_REJECTED);
            }
            if (!(root.get("body") instanceof Map<?, ?> body)) throw invalid();
            int page = number(body.get("pageNo")), total = number(body.get("totalCount"));
            if (page != requestedPage) throw invalid();
            Object raw = body.get("items");
            List<?> items = raw == null && total == 0 ? List.of() : raw instanceof List<?> list ? list : null;
            int expected = (int) Math.max(0, Math.min(properties.pageSize(),
                    (long) total - (long) (page - 1) * properties.pageSize()));
            if (items == null || items.size() != expected) throw invalid();
            List<MfdsDrugRecord> records = new ArrayList<>();
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> map)) throw invalid();
                records.add(MfdsDrugRecord.from(map));
            }
            return new Page(page, total, List.copyOf(records));
        } catch (DrugImportException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // Do not surface response bodies or credentials in parser failures.
            throw invalid();
        }
    }

    private int number(Object value) {
        int number = Integer.parseInt(String.valueOf(value));
        if (number < 0) throw invalid();
        return number;
    }

    private DrugImportException invalid() { return new DrugImportException(DrugImportException.Code.RESPONSE_INVALID); }
    public record Page(int pageNo, int totalCount, List<MfdsDrugRecord> records) {}
}
