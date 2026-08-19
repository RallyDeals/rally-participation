package com.rally.participation.client;

import com.rally.participation.exception.DealNotFoundException;
import com.rally.participation.exception.DealNotJoinableException;
import com.rally.participation.exception.DealServiceUnavailableException;
import com.rally.participation.exception.LeaveNotEligibleException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RealDealServiceClientTest {

    private MockWebServer server;
    private RealDealServiceClient client;
    private final UUID dealId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        // small timeout/backoff so failure-path tests don't hang the suite
        client = new RealDealServiceClient(webClient, 1000, 1, 50);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void reserveSlot_success_doesNotThrow() {
        server.enqueue(new MockResponse().setResponseCode(200));
        assertThatCode(() -> client.reserveSlot(dealId)).doesNotThrowAnyException();
    }

    @Test
    void reserveSlot_409_throwsDealNotJoinable() {
        server.enqueue(new MockResponse().setResponseCode(409));
        assertThatThrownBy(() -> client.reserveSlot(dealId)).isInstanceOf(DealNotJoinableException.class);
    }

    @Test
    void reserveSlot_404_throwsDealNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertThatThrownBy(() -> client.reserveSlot(dealId)).isInstanceOf(DealNotFoundException.class);
    }

    @Test
    void reserveSlot_serverErrorExhaustsRetries_throwsUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500)); // 1 retry attempt configured
        assertThatThrownBy(() -> client.reserveSlot(dealId)).isInstanceOf(DealServiceUnavailableException.class);
    }

    @Test
    void checkLeaveEligible_notEligible_throwsLeaveNotEligible() {
        String json = """
            {
              "eligible": false,
              "dealId": "%s",
              "reason": "TOO_CLOSE_TO_END_TIME"
            }
            """.formatted(dealId);
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json));
        assertThatThrownBy(() -> client.checkLeaveEligible(dealId)).isInstanceOf(LeaveNotEligibleException.class);
    }

    @Test
    void checkLeaveEligible_eligible_doesNotThrow() {
        String json = """
            {
              "eligible": true,
              "dealId": "%s",
              "reason": null
            }
            """.formatted(dealId);
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json));
        assertThatCode(() -> client.checkLeaveEligible(dealId)).doesNotThrowAnyException();
    }

    @Test
    void getDealSummary_success_parsesBody() {
        String json = """
            {
              "id": "%s",
              "productId": "00000000-0000-0000-0000-000000000001",
              "sellerId": "00000000-0000-0000-0000-000000000002",
              "originalPrice": 100.00,
              "dealPrice": 75.00,
              "dealStock": 100,
              "currentParticipants": 42,
              "authorizedCount": 0,
              "minParticipants": 10,
              "status": "ACTIVE",
              "startTime": null,
              "durationMinutes": 1440,
              "endTime": "2026-12-01T00:00:00Z",
              "timeRemainingSeconds": 86400,
              "createdAt": null
            }
            """.formatted(dealId);
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json));

        DealSummaryResponse summary = client.getDealSummary(dealId);

        org.assertj.core.api.Assertions.assertThat(summary.dealId()).isEqualTo(dealId);
        org.assertj.core.api.Assertions.assertThat(summary.minParticipants()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(summary.stockCap()).isEqualTo(100);
        org.assertj.core.api.Assertions.assertThat(summary.reservedCount()).isEqualTo(42);
        org.assertj.core.api.Assertions.assertThat(summary.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getDealSummary_404_throwsDealNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertThatThrownBy(() -> client.getDealSummary(dealId)).isInstanceOf(DealNotFoundException.class);
    }
}
