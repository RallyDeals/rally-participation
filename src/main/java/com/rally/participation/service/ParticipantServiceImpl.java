package com.rally.participation.service;

import com.rally.participation.domain.Participation;
import com.rally.participation.domain.ParticipationStatus;
import com.rally.participation.dto.MyParticipationsPageResponse;
import com.rally.participation.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {
    private final ParticipationRepository participationRepository;

    @Override
    public MyParticipationsPageResponse getParticipantDeals(UUID callerId, ParticipationStatus status, Pageable pageable) {
        Page<Participation> myParticipations;
        if(status != null) {
            myParticipations = participationRepository.findAllByUserIdAndStatus(callerId, status, pageable);
        }else {
            myParticipations = participationRepository.findAllByUserId(callerId, pageable);
        }
        return MyParticipationsPageResponse.of(myParticipations.getContent(), pageable.getPageNumber(), pageable.getPageSize(), myParticipations.getTotalElements());
    }
}
