package com.rally.participation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rally.kafka.topics")
public class KafkaTopicsProperties {

    private String participantJoined;
    private String participantLeft;
    private String orderDealOrderCancelled;

    public String getParticipantJoined() {
        return participantJoined;
    }

    public void setParticipantJoined(String participantJoined) {
        this.participantJoined = participantJoined;
    }

    public String getParticipantLeft() {
        return participantLeft;
    }

    public void setParticipantLeft(String participantLeft) {
        this.participantLeft = participantLeft;
    }

    public String getOrderDealOrderCancelled() {
        return orderDealOrderCancelled;
    }

    public void setOrderDealOrderCancelled(String orderDealOrderCancelled) {
        this.orderDealOrderCancelled = orderDealOrderCancelled;
    }
}
