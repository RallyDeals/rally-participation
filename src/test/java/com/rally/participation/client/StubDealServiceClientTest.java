package com.rally.participation.client;

import com.rally.participation.exception.DealNotJoinableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubDealServiceClientTest {

    @Test
    void reserveSlot_throwsOnceCapacityReached() {
        StubDealServiceClient client = new StubDealServiceClient();
        ReflectionTestUtils.setField(client, "defaultCapacity", 2);
        UUID dealId = UUID.randomUUID();

        assertThatCode(() -> client.reserveSlot(dealId)).doesNotThrowAnyException();
        assertThatCode(() -> client.reserveSlot(dealId)).doesNotThrowAnyException();
        assertThatThrownBy(() -> client.reserveSlot(dealId)).isInstanceOf(DealNotJoinableException.class);
    }

    @Test
    void reserveSlot_capacityIsTrackedPerDeal() {
        StubDealServiceClient client = new StubDealServiceClient();
        ReflectionTestUtils.setField(client, "defaultCapacity", 1);

        UUID dealA = UUID.randomUUID();
        UUID dealB = UUID.randomUUID();

        assertThatCode(() -> client.reserveSlot(dealA)).doesNotThrowAnyException();
        assertThatCode(() -> client.reserveSlot(dealB)).doesNotThrowAnyException();
        assertThatThrownBy(() -> client.reserveSlot(dealA)).isInstanceOf(DealNotJoinableException.class);
    }

    @Test
    void checkLeaveEligible_alwaysSucceeds() {
        StubDealServiceClient client = new StubDealServiceClient();
        assertThatCode(() -> client.checkLeaveEligible(UUID.randomUUID())).doesNotThrowAnyException();
    }
}
