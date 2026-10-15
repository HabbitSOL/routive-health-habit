package com.routive.healthhabit.drug.importer;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class MfdsDrugClientTest {
    private static final String TEST_KEY = "unit+/key==";
    private MockRestServiceServer server;
    private MfdsDrugClient client;
    @BeforeEach void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MfdsDrugClient(props(TEST_KEY), JsonMapper.builder().build(), builder.build());
    }
    @AfterEach void verify() { server.verify(); }
    private MfdsImportProperties props(String key) { return new MfdsImportProperties(false, key, 1, 10, 100); }
    private String response(String items, int total, int page) {
        return "{\"header\":{\"resultCode\":\"00\"},\"body\":{\"pageNo\":" + page
                + ",\"totalCount\":" + total + ",\"items\":" + items + "}}";
    }
    private void returns(String body) {
        server.expect(request -> {}).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
    private void invalid(String body) {
        returns(body);
        assertThatThrownBy(() -> client.fetch(1)).isInstanceOf(DrugImportException.class)
                .hasMessage("RESPONSE_INVALID").hasNoCause();
    }

    @Test void parsesJsonAndEncodesRawKeyExactlyOnce() {
        server.expect(request -> {
            assertThat(request.getURI().getRawQuery()).contains("serviceKey=unit%2B%2Fkey%3D%3D")
                    .contains("pageNo=1", "numOfRows=100", "type=json").doesNotContain("%252B");
        }).andRespond(withSuccess(response("[{\"itemSeq\":\"990001\",\"itemName\":\"테스트약\"}]", 1, 1),
                MediaType.APPLICATION_JSON));
        var page = client.fetch(1);
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.records().getFirst().itemName()).isEqualTo("테스트약");
    }
    @Test void missingKeyDoesNotCallProvider() {
        RestClient.Builder builder = RestClient.builder();
        client = new MfdsDrugClient(props(""), JsonMapper.builder().build(), builder.build());
        assertThatThrownBy(() -> client.fetch(1)).hasMessage("NOT_CONFIGURED").hasNoCause();
    }
    @Test void rejectsProviderErrorWithoutEchoingMessageOrKey() {
        returns("{\"header\":{\"resultCode\":\"30\",\"resultMsg\":\"" + TEST_KEY + "\"}}");
        assertThatThrownBy(() -> client.fetch(1)).hasMessage("UPSTREAM_REJECTED").hasNoCause();
    }
    @Test void httpErrorsDoNotRetainUrlOrKey() {
        server.expect(request -> {}).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body(TEST_KEY));
        assertThatThrownBy(() -> client.fetch(1)).hasMessage("HTTP_ERROR").hasNoCause();
    }
    @Test void transportErrorsDoNotRetainUrlOrKey() {
        server.expect(request -> {}).andRespond(request -> { throw new java.io.IOException(TEST_KEY); });
        assertThatThrownBy(() -> client.fetch(1)).hasMessage("HTTP_ERROR").hasNoCause();
    }
    @Test void malformedBodyIsNotAcceptedAsAnEmptyCatalog() { invalid("<error>no key</error>"); }
    @Test void missingItemsIsNotAcceptedForNonemptyCatalog() { invalid(response("null", 1, 1)); }
    @Test void rejectsWrongPageAndUnexpectedEmptyPage() {
        returns(response("[]", 1, 2));
        returns(response("[]", 1, 1));
        assertThatThrownBy(() -> client.fetch(1)).hasMessage("RESPONSE_INVALID");
        assertThatThrownBy(() -> client.fetch(1)).hasMessage("RESPONSE_INVALID");
    }
    @Test void zeroResultsMayOmitItems() {
        returns("{\"header\":{\"resultCode\":\"00\"},\"body\":{\"pageNo\":1,\"totalCount\":0}}");
        assertThat(client.fetch(1).records()).isEmpty();
    }
    @Test void truncatedPageCannotSilentlySkipCatalogEntries() {
        invalid(response("[{\"itemSeq\":\"990001\",\"itemName\":\"약\"}]", 100, 1));
    }
    @Test void normalizesTextAndSourceDateWithoutInventingIngredients() {
        var data = MfdsDrugData.from(new MfdsDrugRecord("990001", "<p>테스트약</p>", "제약사", "<p>A<br>B &amp; C</p>",
                null, "경고", "주의", "상호작용", null, null, "https://example.com/image.png", "20261001"));
        assertThat(data.name()).isEqualTo("테스트약");
        assertThat(data.efficacy()).isEqualTo("A\nB & C");
        assertThat(data.precautions()).isEqualTo("경고\n\n주의");
        assertThat(data.sourceUpdatedAt()).isEqualTo(java.time.Instant.parse("2026-09-30T15:00:00Z"));
        assertThat(props(TEST_KEY).toString()).doesNotContain(TEST_KEY);
        assertThat(data.toProductData().kind()).isEqualTo("OTC");
    }
    @Test void rejectsInvalidMetadataAndOversizedDatabaseText() {
        for (MfdsDrugRecord row : java.util.List.of(
                new MfdsDrugRecord("bad-code", "약", null, null, null, null, null, null, null, null, null, null),
                new MfdsDrugRecord("990001", "약", null, null, null, null, null, null, null, null, null, "bad-date"),
                new MfdsDrugRecord("990001", "약", null, null, null, null, null, null, null, null, "javascript:alert(1)", null),
                new MfdsDrugRecord("990001", "약", null, "한".repeat(22000), null, null, null, null, null, null, null, null))) {
            assertThatThrownBy(() -> MfdsDrugData.from(row)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat("한".repeat(22000).getBytes(StandardCharsets.UTF_8)).hasSize(66000);
    }
}
