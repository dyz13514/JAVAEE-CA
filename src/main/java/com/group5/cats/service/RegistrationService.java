package com.group5.cats.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.dto.RegistrationForm;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

@Service
public class RegistrationService {
    private final RegistrationRequestRepository registrations;
    private final EmployeeRepository employees;
    private final EntitlementService entitlements;
    public RegistrationService(RegistrationRequestRepository registrations, EmployeeRepository employees, EntitlementService entitlements) {
        this.registrations = registrations;
        this.employees = employees;
        this.entitlements = entitlements;
    }

    @Transactional
    public void submit(RegistrationForm form) {
        String username = form.getUsername() == null ? "" : form.getUsername().trim();
        String name = form.getName() == null ? "" : form.getName().trim();
        if (!username.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,39}"))
            throw new IllegalArgumentException("Use 3–40 letters, numbers, dots, underscores or hyphens for your username.");
        if (name.isBlank() || name.length() > 100) throw new IllegalArgumentException("Enter your full name (up to 100 characters).");
        String password = form.getPassword();
        if (password == null || password.isBlank() || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Use a password of at least 8 characters and no more than 72 bytes.");
        if (!password.equals(form.getConfirmPassword())) throw new IllegalArgumentException("Passwords do not match.");
        if (employees.findByUsername(username).isPresent()) throw new IllegalArgumentException("This username is already in use. Sign in or choose another username.");
        RegistrationRequest request = registrations.findByUsername(username).orElseGet(RegistrationRequest::new);
        if (request.getId() != null && !"REJECTED".equals(request.getStatus()))
            throw new IllegalArgumentException("An application for this username has already been submitted. Contact your administrator.");
        request.setUsername(username);
        request.setName(name);
        request.setPasswordHash(PasswordSupport.encode(password));
        request.setStatus("PENDING");
        request.setSubmittedAt(LocalDateTime.now());
        request.setReviewedAt(null);
        request.setReviewedBy(null);
        request.setReviewReason(null);
        registrations.saveAndFlush(request);
    }

    public List<RegistrationRequest> pending() { return registrations.findByStatusOrderBySubmittedAtAsc("PENDING"); }
    public long pendingCount() { return registrations.countByStatus("PENDING"); }
    public List<RegistrationRequest> recentReviews() { return registrations.findTop20ByStatusNotOrderByReviewedAtDesc("PENDING"); }

    @Transactional
    public void approve(Long id, EmployeeDesignation designation, Long supervisorId, Employee reviewer) {
        requireAdmin(reviewer);
        RegistrationRequest request = pendingRequest(id);
        if (designation == null) throw new IllegalArgumentException("Choose a designation before approving.");
        if (employees.findByUsername(request.getUsername()).isPresent()) throw new IllegalArgumentException("This username is already in use. Reject this request and ask the applicant to choose another.");
        if (supervisorId == null) throw new IllegalArgumentException("Choose a supervisor before approving.");
        Employee supervisor = employees.findById(supervisorId)
                .filter(employee -> employee.getRole() == EmployeeRole.MANAGER)
                .orElseThrow(() -> new IllegalArgumentException("Choose an existing manager as supervisor."));
        Employee employee = new Employee(request.getUsername(), request.getPasswordHash(), request.getName(), supervisor, designation, EmployeeRole.REGULAR_STAFF);
        employees.saveAndFlush(employee);
        entitlements.createDefaultEntitlement(employee.getId(), LocalDate.now().getYear());
        reviewed(request, "APPROVED", reviewer, null);
    }

    @Transactional
    public void reject(Long id, String reason, Employee reviewer) {
        requireAdmin(reviewer);
        if (reason == null || reason.isBlank() || reason.trim().length() > 300)
            throw new IllegalArgumentException("Enter a rejection reason (up to 300 characters).");
        reviewed(pendingRequest(id), "REJECTED", reviewer, reason.trim());
    }

    private void requireAdmin(Employee reviewer) {
        if (reviewer == null || reviewer.getRole() != EmployeeRole.ADMIN) throw new IllegalArgumentException("Only administrators can review registrations.");
    }

    private RegistrationRequest pendingRequest(Long id) {
        RegistrationRequest request = registrations.findForReview(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found."));
        if (!"PENDING".equals(request.getStatus())) throw new IllegalArgumentException("This request has already been reviewed.");
        return request;
    }

    private void reviewed(RegistrationRequest request, String status, Employee reviewer, String reason) {
        request.setStatus(status);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(reviewer);
        request.setReviewReason(reason);
        request.setPasswordHash(null);
        registrations.save(request);
    }
}
