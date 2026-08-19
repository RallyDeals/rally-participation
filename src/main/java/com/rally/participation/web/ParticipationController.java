package com.rally.participation.web;

import com.rally.participation.dto.*;
import com.rally.participation.service.ParticipationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/deals/{dealId}")
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationResponse join(@PathVariable UUID dealId,
                                       @CurrentUser UUID userId,
                                       @Valid @RequestBody JoinRequest request) {
        return participationService.join(dealId, userId, request.referralCode(), request.paymentMethodId(), request.address());
    }

    @DeleteMapping("/leave")
    public ResponseEntity<Void> leave(@PathVariable UUID dealId, @CurrentUser UUID userId) {
        participationService.leave(dealId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/participants")
    public ParticipantsPageResponse listParticipants(@PathVariable UUID dealId,
                                                       @RequestParam(defaultValue = "true") boolean activeOnly,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return participationService.listParticipants(dealId, activeOnly, PageRequest.of(page, size));
    }

    @GetMapping("/progress")
    public DealProgressResponse getProgress(@PathVariable UUID dealId) {
        return participationService.getProgress(dealId);
    }

    @GetMapping("/activity")
    public List<ActivityEvent> getActivity(@PathVariable UUID dealId) {
        return participationService.getActivity(dealId);
    }
}
