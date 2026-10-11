package com.group5.cats.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.group5.cats.dto.CourseCategory;
import com.group5.cats.dto.CourseFeeClaimForm;
import com.group5.cats.dto.TrainingLedger;
import com.group5.cats.model.ClaimAttachment;
import com.group5.cats.model.ClaimHistory;
import com.group5.cats.model.ClaimStatus;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.CourseFeeClaim;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.model.EntitlementSummary;
import com.group5.cats.repository.ClaimHistoryRepository;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.CourseFeeClaimRepository;
import com.group5.cats.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class CourseFeeClaimServiceImpl implements CourseFeeClaimService {

    private final CourseFeeClaimRepository claimRepository;
    private final ClaimHistoryRepository historyRepository;
    private final CourseApplicationRepository applicationRepository;
    private final EmployeeRepository employeeRepository;
    private final ClaimAttachmentService attachmentService;
    private final EntitlementService entitlementService;

    public CourseFeeClaimServiceImpl(CourseFeeClaimRepository claimRepository,
            ClaimHistoryRepository historyRepository, CourseApplicationRepository applicationRepository,
            EmployeeRepository employeeRepository, ClaimAttachmentService attachmentService,
            EntitlementService entitlementService) {
        this.claimRepository = claimRepository;
        this.historyRepository = historyRepository;
        this.applicationRepository = applicationRepository;
        this.employeeRepository = employeeRepository;
        this.attachmentService = attachmentService;
        this.entitlementService = entitlementService;
    }

    @Override
    public List<CourseFeeClaim> findMyClaims(Employee currentUser) {
        Employee employee = getCurrentEmployee(currentUser);
        if (employee.getRole() == EmployeeRole.ADMIN) {
            throw new SecurityException("Administrators cannot submit employee claims.");
        }
        return claimRepository.findByApplication_Employee_IdOrderBySubmittedAtDescIdDesc(employee.getId());
    }

    @Override
    public List<CourseFeeClaim> findTeamClaims(Employee currentUser) {
        Employee manager = getCurrentEmployee(currentUser);
        if (manager.getRole() != EmployeeRole.MANAGER) {
            throw new SecurityException("Only managers can view team claims.");
        }
        List<CourseFeeClaim> teamClaims = new ArrayList<>();
        List<CourseFeeClaim> claims = claimRepository
                .findByApplication_Employee_Supervisor_IdOrderBySubmittedAtDescIdDesc(manager.getId());
        for (CourseFeeClaim claim : claims) {
            if (!claim.getApplication().getEmployee().getId().equals(manager.getId())) {
                teamClaims.add(claim);
            }
        }
        return teamClaims;
    }

    @Override
    public List<CourseFeeClaim> findAllClaims(Employee currentUser) {
        Employee administrator = getCurrentEmployee(currentUser);
        if (administrator.getRole() != EmployeeRole.ADMIN) {
            throw new SecurityException("Only administrators can view all claims.");
        }
        return claimRepository.findAllByOrderBySubmittedAtDescIdDesc();
    }

    @Override
    public List<CourseApplication> findEligibleApplications(Employee currentUser) {
        Employee employee = getCurrentEmployee(currentUser);
        List<CourseApplication> eligibleApplications = new ArrayList<>();
        if (employee.getRole() == EmployeeRole.ADMIN) {
            return eligibleApplications;
        }
        List<CourseApplication> applications = applicationRepository.findByEmployee(employee);
        for (CourseApplication application : applications) {
            if (validateCourse(application, employee) == null
                    && claimRepository.findByApplication_Id(application.getId()).isEmpty()) {
                eligibleApplications.add(application);
            }
        }
        return eligibleApplications;
    }

    @Override
    public CourseFeeClaim findViewableClaim(Long claimId, Employee currentUser) {
        Employee employee = getCurrentEmployee(currentUser);
        CourseFeeClaim claim = getClaim(claimId);
        Employee owner = claim.getApplication().getEmployee();
        if (owner.getId().equals(employee.getId()) || employee.getRole() == EmployeeRole.ADMIN) {
            return claim;
        }
        if (canReview(owner, employee)) {
            return claim;
        }
        throw new SecurityException("You are not authorised to view this claim.");
    }

    @Override
    public List<ClaimHistory> findHistory(Long claimId, Employee currentUser) {
        findViewableClaim(claimId, currentUser);
        return historyRepository.findByClaim_IdOrderByCreatedAtAscIdAsc(claimId);
    }

    @Override
    public ClaimAttachment findViewableAttachment(Long claimId, Long attachmentId, Employee currentUser) {
        CourseFeeClaim claim = findViewableClaim(claimId, currentUser);
        if (claim.getReceipt().getId().equals(attachmentId)) {
            return claim.getReceipt();
        }
        if (claim.getCertificate().getId().equals(attachmentId)) {
            return claim.getCertificate();
        }
        List<ClaimHistory> history = historyRepository.findByClaim_IdOrderByCreatedAtAscIdAsc(claimId);
        for (ClaimHistory entry : history) {
            if (entry.getReceipt().getId().equals(attachmentId)) {
                return entry.getReceipt();
            }
            if (entry.getCertificate().getId().equals(attachmentId)) {
                return entry.getCertificate();
            }
        }
        throw new IllegalArgumentException("The attachment does not belong to this claim.");
    }

    @Override
    @Transactional
    public String submitClaim(Long claimId, CourseFeeClaimForm form, MultipartFile receipt,
            MultipartFile certificate, Employee currentUser) {
        Employee employee = getCurrentEmployee(currentUser);
        if (employee.getRole() == EmployeeRole.ADMIN) {
            return "Administrators cannot submit employee claims.";
        }
        if (form.getApplicationId() == null) {
            return "Please select a completed course.";
        }
        Optional<CourseApplication> applicationResult = applicationRepository.findById(form.getApplicationId());
        if (applicationResult.isEmpty()) {
            return "The course application was not found.";
        }
        CourseApplication application = applicationResult.get();
        String error = validateCourse(application, employee);
        if (error != null) {
            return error;
        }

        BigDecimal amount = form.getAmount();
        if (amount == null || amount.signum() <= 0) {
            return "The claim amount must be greater than zero.";
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            return "The claim amount can have at most two decimal places.";
        }
        if (amount.compareTo(new BigDecimal("9999999999.99")) > 0
                || amount.compareTo(BigDecimal.valueOf(application.getFee())) > 0) {
            return "The claim amount must not exceed the course fee.";
        }
        if (!form.isPaidPersonally()) {
            return "Please confirm that you paid the course fee yourself.";
        }

        CourseFeeClaim claim;
        if (claimId == null) {
            if (claimRepository.findByApplication_Id(application.getId()).isPresent()) {
                return "A claim already exists for this course. Please open it from My claims.";
            }
            claim = new CourseFeeClaim();
            claim.setApplication(application);
        } else {
            claim = getClaim(claimId);
            if (!claim.getApplication().getEmployee().getId().equals(employee.getId())) {
                return "You can only resubmit your own claim.";
            }
            if (!claim.getApplication().getId().equals(application.getId())) {
                return "The course cannot be changed when resubmitting a claim.";
            }
            if (claim.getStatus() != ClaimStatus.REJECTED) {
                return "Only rejected claims can be resubmitted.";
            }
            if (!matchesVersion(claim, form.getVersion())) {
                return "This claim has changed. Please reload the page before resubmitting.";
            }
        }

        error = attachmentService.validateUpload(receipt, claimId == null);
        if (error != null) {
            return "Receipt: " + error;
        }
        error = attachmentService.validateUpload(certificate, claimId == null);
        if (error != null) {
            return "Certificate of completion: " + error;
        }

        // Validate all business rules before writing either attachment.
        if (receipt != null && !receipt.isEmpty()) {
            claim.setReceipt(attachmentService.store(receipt));
        }
        if (certificate != null && !certificate.isEmpty()) {
            claim.setCertificate(attachmentService.store(certificate));
        }
        claim.setAmount(amount.setScale(2));
        claim.setPaidPersonally(true);
        claim.setStatus(ClaimStatus.PENDING);
        claim.setSubmittedAt(LocalDateTime.now());
        claim.setReviewedAt(null);
        claim.setReviewedById(null);
        claim.setReviewedByName(null);
        claim.setManagerReason(null);
        claimRepository.saveAndFlush(claim);

        String comment;
        if (claimId == null) {
            comment = "Claim submitted with receipt and certificate of completion.";
        } else {
            comment = "Rejected claim revised and resubmitted for manager approval.";
        }
        saveHistory(claim, employee, comment);
        return null;
    }

    @Override
    @Transactional
    public String reviewClaim(Long claimId, Long version, String decision, String reason, Employee currentUser) {
        Employee manager = getCurrentEmployee(currentUser);
        CourseFeeClaim claim = getClaim(claimId);
        if (!canReview(claim.getApplication().getEmployee(), manager)) {
            return "Only the employee's assigned manager can review this claim.";
        }
        if (!matchesVersion(claim, version)) {
            return "This claim has changed. Please reload the page before reviewing.";
        }
        if (claim.getStatus() != ClaimStatus.PENDING) {
            return "Only pending claims can be approved or rejected.";
        }
        if (reason == null || reason.trim().isEmpty() || reason.trim().length() > 1000) {
            return "Please provide an approval or rejection reason of no more than 1000 characters.";
        }
        ClaimStatus nextStatus;
        if ("APPROVE".equals(decision)) {
            nextStatus = ClaimStatus.APPROVED;
        } else if ("REJECT".equals(decision)) {
            nextStatus = ClaimStatus.REJECTED;
        } else {
            return "Please choose Approve or Reject.";
        }

        claim.setStatus(nextStatus);
        claim.setManagerReason(reason.trim());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewedById(manager.getId());
        claim.setReviewedByName(snapshotName(manager));
        claimRepository.saveAndFlush(claim);
        saveHistory(claim, manager, reason.trim());
        return null;
    }

    @Override
    @Transactional
    public String reimburseClaim(Long claimId, Long version, LocalDate paymentDate,
            String reference, String note, Employee currentUser) {
        Employee administrator = getCurrentEmployee(currentUser);
        if (administrator.getRole() != EmployeeRole.ADMIN) {
            return "Only administrators can record reimbursement payments.";
        }
        CourseFeeClaim claim = getClaim(claimId);
        if (!matchesVersion(claim, version)) {
            return "This claim has changed. Please reload the page before recording payment.";
        }
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            return "Only approved, unpaid claims can be marked as reimbursed.";
        }
        if (paymentDate == null || paymentDate.isAfter(LocalDate.now())
                || claim.getReviewedAt() == null || paymentDate.isBefore(claim.getReviewedAt().toLocalDate())) {
            return "The payment date must be between the manager approval date and today.";
        }
        if (reference == null || reference.trim().isEmpty() || reference.trim().length() > 100) {
            return "Please provide a payment reference of no more than 100 characters.";
        }
        if (note != null && note.trim().length() > 1000) {
            return "The payment note must not exceed 1000 characters.";
        }

        claim.setStatus(ClaimStatus.REIMBURSED);
        claim.setPaymentDate(paymentDate);
        claim.setPaymentReference(reference.trim());
        if (note != null) {
            claim.setPaymentNote(note.trim());
        }
        claim.setPaidAt(LocalDateTime.now());
        claim.setPaidById(administrator.getId());
        claim.setPaidByName(snapshotName(administrator));
        claimRepository.saveAndFlush(claim);
        saveHistory(claim, administrator, "Reimbursement recorded for " + paymentDate
                + ". Payment reference: " + reference.trim());
        return null;
    }

    @Override
    public TrainingLedger getTrainingLedger(Long employeeId, Integer year, Employee currentUser) {
        Employee viewer = getCurrentEmployee(currentUser);
        if (employeeId == null || year == null || year < 1 || year > 9999) {
            throw new IllegalArgumentException("Please select an employee and a valid year.");
        }
        Employee selectedEmployee = null;
        List<Employee> employees = entitlementService.getQueryableEmployees(viewer);
        for (Employee employee : employees) {
            if (employee.getId().equals(employeeId)) {
                selectedEmployee = employee;
                break;
            }
        }
        if (selectedEmployee == null) {
            throw new SecurityException("You are not authorised to view this employee's ledger.");
        }

        TrainingLedger ledger = new TrainingLedger();
        ledger.setEmployee(selectedEmployee);
        ledger.setYear(year);
        Optional<EntitlementSummary> summaryResult = entitlementService.getEntitlementSummary(employeeId, year);
        if (summaryResult.isPresent()) {
            EntitlementSummary summary = summaryResult.get();
            ledger.setHasEntitlement(true);
            ledger.setTrainingBudget(summary.getTrainingBudget());
            ledger.setOccupiedBudget(summary.getOccupiedBudget());
            ledger.setRemainingBudget(summary.getRemainingBudget());
        }

        List<CourseApplication> applications = applicationRepository.findByEmployee(selectedEmployee);
        for (CourseApplication application : applications) {
            if (application.getFromDate() == null || application.getFromDate().getYear() != year) {
                continue;
            }
            if ("APPROVED".equals(application.getStatus()) || "COMPLETED".equals(application.getStatus())) {
                ledger.getApplications().add(application);
                if (CourseCategory.isBudgetRelevant(application.getCategoryName())
                        && Double.isFinite(application.getFee())) {
                    BigDecimal courseFee = BigDecimal.valueOf(application.getFee()).setScale(2, RoundingMode.HALF_UP);
                    ledger.setApprovedCourseFees(ledger.getApprovedCourseFees().add(courseFee));
                }
            }
        }
        List<CourseFeeClaim> claims = claimRepository
                .findByApplication_Employee_IdOrderBySubmittedAtDescIdDesc(employeeId);
        for (CourseFeeClaim claim : claims) {
            LocalDate courseDate = claim.getApplication().getFromDate();
            if (courseDate == null || courseDate.getYear() != year) {
                continue;
            }
            ledger.getClaims().add(claim);
            if (claim.getStatus() == ClaimStatus.PENDING) {
                ledger.setPendingClaimAmount(ledger.getPendingClaimAmount().add(claim.getAmount()));
            } else if (claim.getStatus() == ClaimStatus.APPROVED) {
                ledger.setApprovedClaimAmount(ledger.getApprovedClaimAmount().add(claim.getAmount()));
            } else if (claim.getStatus() == ClaimStatus.REIMBURSED) {
                ledger.setReimbursedAmount(ledger.getReimbursedAmount().add(claim.getAmount()));
            }
        }
        // Reimbursement records settle existing course fees; they do not reserve budget again.
        return ledger;
    }

    private Employee getCurrentEmployee(Employee currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new SecurityException("Please sign in again.");
        }
        Optional<Employee> employeeResult = employeeRepository.findById(currentUser.getId());
        if (employeeResult.isEmpty()) {
            throw new SecurityException("Please sign in again.");
        }
        return employeeResult.get();
    }

    private CourseFeeClaim getClaim(Long claimId) {
        if (claimId == null) {
            throw new IllegalArgumentException("The claim was not found.");
        }
        Optional<CourseFeeClaim> claimResult = claimRepository.findById(claimId);
        if (claimResult.isEmpty()) {
            throw new IllegalArgumentException("The claim was not found.");
        }
        return claimResult.get();
    }

    private String validateCourse(CourseApplication application, Employee employee) {
        if (application.getEmployee() == null || !application.getEmployee().getId().equals(employee.getId())) {
            return "You can only claim fees for your own course application.";
        }
        if (!"COMPLETED".equals(application.getStatus())) {
            return "The approved course must be completed before claiming its fee.";
        }
        if (!CourseCategory.EXTERNAL.equals(application.getCategoryName())
                && !CourseCategory.CERTIFICATION.equals(application.getCategoryName())) {
            return "Only External Course and Professional Certification fees can be claimed.";
        }
        if (!Double.isFinite(application.getFee()) || application.getFee() <= 0) {
            return "The course must have a valid, positive fee.";
        }
        if (application.getFromDate() == null || application.getToDate() == null
                || application.getToDate().isAfter(LocalDate.now())) {
            return "The course must have ended before claiming its fee.";
        }
        Employee supervisor = employee.getSupervisor();
        if (supervisor == null || supervisor.getRole() != EmployeeRole.MANAGER
                || supervisor.getId().equals(employee.getId())) {
            return "Please ask an administrator to assign a manager before submitting a claim.";
        }
        return null;
    }

    private boolean canReview(Employee owner, Employee reviewer) {
        if (reviewer.getRole() != EmployeeRole.MANAGER || owner.getId().equals(reviewer.getId())) {
            return false;
        }
        Employee supervisor = owner.getSupervisor();
        if (supervisor == null) {
            return false;
        }
        return supervisor.getId().equals(reviewer.getId());
    }

    private boolean matchesVersion(CourseFeeClaim claim, Long version) {
        if (version == null || claim.getVersion() == null) {
            return false;
        }
        return claim.getVersion().equals(version);
    }

    private String snapshotName(Employee employee) {
        String name = employee.getName();
        if (name == null || name.isBlank()) {
            name = employee.getUsername();
        }
        if (name == null) {
            name = "Employee #" + employee.getId();
        }
        if (name.length() > 180) {
            name = name.substring(0, 180);
        }
        return name;
    }

    private void saveHistory(CourseFeeClaim claim, Employee actor, String comment) {
        ClaimHistory history = new ClaimHistory();
        history.setClaim(claim);
        history.setStatus(claim.getStatus());
        history.setAmount(claim.getAmount());
        history.setActorId(actor.getId());
        history.setActorName(snapshotName(actor));
        history.setComment(comment);
        history.setCreatedAt(LocalDateTime.now());
        history.setReceipt(claim.getReceipt());
        history.setCertificate(claim.getCertificate());
        historyRepository.save(history);
    }
}
