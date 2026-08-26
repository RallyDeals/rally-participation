package com.rally.participation.web;

import com.rally.participation.domain.ParticipationStatus;
import com.rally.participation.dto.MyParticipationsPageResponse;
import com.rally.participation.repository.ParticipationRepository;
import com.rally.participation.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @GetMapping("/participations")
    public MyParticipationsPageResponse getParticipantDeals(@RequestHeader("X-User-Id") UUID callerId,
                                                            @RequestParam(required = false) ParticipationStatus status,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "3") int size) {
        int requestedPage = Math.max(page, 1);
        MyParticipationsPageResponse result = participantService.getParticipantDeals(
                callerId, status, PageRequest.of(requestedPage - 1, size));
        return new MyParticipationsPageResponse(result.participations(), requestedPage, result.size(), result.totalElements());
    }
}

