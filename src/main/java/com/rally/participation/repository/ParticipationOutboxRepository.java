package com.rally.participation.repository;

import com.rally.participation.domain.ParticipationOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationOutboxRepository extends JpaRepository<ParticipationOutbox, Long> {

    List<ParticipationOutbox> findByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
