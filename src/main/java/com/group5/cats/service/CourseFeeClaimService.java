package com.group5.cats.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import com.group5.cats.dto.CourseFeeClaimForm;
import com.group5.cats.dto.TrainingLedger;
import com.group5.cats.model.ClaimAttachment;
import com.group5.cats.model.ClaimHistory;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.CourseFeeClaim;
import com.group5.cats.model.Employee;

public interface CourseFeeClaimService {

    List<CourseFeeClaim> findMyClaims(Employee currentUser);

    List<CourseFeeClaim> findTeamClaims(Employee currentUser);

    List<CourseFeeClaim> findAllClaims(Employee currentUser);

    List<CourseApplication> findEligibleApplications(Employee currentUser);

    CourseFeeClaim findViewableClaim(Long claimId, Employee currentUser);

    List<ClaimHistory> findHistory(Long claimId, Employee currentUser);

    ClaimAttachment findViewableAttachment(Long claimId, Long attachmentId, Employee currentUser);

    String submitClaim(Long claimId, CourseFeeClaimForm form, MultipartFile receipt,
            MultipartFile certificate, Employee currentUser);

    String reviewClaim(Long claimId, Long version, String decision, String reason, Employee currentUser);

    String reimburseClaim(Long claimId, Long version, LocalDate paymentDate,
            String reference, String note, Employee currentUser);

    TrainingLedger getTrainingLedger(Long employeeId, Integer year, Employee currentUser);
}
