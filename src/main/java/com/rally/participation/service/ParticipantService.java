package com.rally.participation.service;

import com.rally.participation.domain.ParticipationStatus;
import com.rally.participation.dto.MyParticipationsPageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ParticipantService {
    MyParticipationsPageResponse getParticipantDeals(UUID callerId, ParticipationStatus status, Pageable pageable);
}
