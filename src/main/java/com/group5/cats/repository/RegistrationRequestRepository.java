package com.group5.cats.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.group5.cats.model.RegistrationRequest;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
    Optional<RegistrationRequest> findByUsername(String username);
    List<RegistrationRequest> findByStatusOrderBySubmittedAtAsc(String status);
    long countByStatus(String status);
    List<RegistrationRequest> findTop20ByStatusNotOrderByReviewedAtDesc(String status);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RegistrationRequest r set r.reviewedBy = null where r.reviewedBy.id = :employeeId")
    void detachReviewer(@Param("employeeId") Long employeeId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RegistrationRequest r where r.id = :id")
    Optional<RegistrationRequest> findForReview(@Param("id") Long id);
}
