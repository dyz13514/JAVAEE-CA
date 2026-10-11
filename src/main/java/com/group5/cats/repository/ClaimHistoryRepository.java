package com.group5.cats.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.ClaimHistory;

public interface ClaimHistoryRepository extends JpaRepository<ClaimHistory, Long> {

    List<ClaimHistory> findByClaim_IdOrderByCreatedAtAscIdAsc(Long claimId);
}
