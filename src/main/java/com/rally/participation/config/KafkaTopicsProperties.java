package com.rally.participation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rally.kafka.topics")
public class KafkaTopicsProperties {

    private String participation;
    private String orderDealOrderCancelled;

    public String getParticipation() {
        return participation;
    }

    public void setParticipation(String participation) {
        this.participation = participation;
    }

    public String getOrderDealOrderCancelled() {
        return orderDealOrderCancelled;
    }

    public void setOrderDealOrderCancelled(String orderDealOrderCancelled) {
        this.orderDealOrderCancelled = orderDealOrderCancelled;
    }
}
