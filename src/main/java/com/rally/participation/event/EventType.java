package com.rally.participation.event;

public final class EventType {
    public static final String PARTICIPANT_JOINED = "Participant.Joined";
    public static final String PARTICIPANT_LEFT = "Participant.Left";

    public static final String ORDER_AUTHORIZED = "Order.Authorized";
    public static final String ORDER_DEAL_CANCELLED = "Order.DealCancelled";

    private EventType() {
    }
}
