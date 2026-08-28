package com.rally.participation.dto;

import com.rally.participation.domain.Participation;
import com.rally.participation.domain.ParticipationStatus;

import java.util.UUID;

public record MyParticipationsResponseItem(UUID dealId, ParticipationStatus status) {
    public static MyParticipationsResponseItem from(Participation participation) {
        return new MyParticipationsResponseItem(participation.getDealId(), participation.getStatus());
    }
}