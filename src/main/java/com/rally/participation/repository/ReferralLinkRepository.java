package com.rally.participation.repository;

import com.rally.participation.domain.ReferralLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralLinkRepository extends JpaRepository<ReferralLink, String> {
}
