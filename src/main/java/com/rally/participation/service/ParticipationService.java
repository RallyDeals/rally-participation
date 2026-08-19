package com.rally.participation.service;

import com.rally.participation.dto.ActivityEvent;
import com.rally.participation.dto.DealProgressResponse;
import com.rally.participation.dto.ParticipantsPageResponse;
import com.rally.participation.dto.ParticipationResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ParticipationService {

    ParticipationResponse join(UUID dealId, UUID userId, String referralCode, String paymentMethodId, String address);

    void leave(UUID dealId, UUID userId);

    ParticipantsPageResponse listParticipants(UUID dealId, boolean activeOnly, Pageable pageable);

    DealProgressResponse getProgress(UUID dealId);

    List<ActivityEvent> getActivity(UUID dealId);
}
