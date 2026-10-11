package com.group5.cats.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.group5.cats.model.ClaimAttachment;
import com.group5.cats.model.ClaimStatus;
import com.group5.cats.model.CourseFeeClaim;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.ClaimAttachmentService;
import com.group5.cats.service.CourseFeeClaimService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CourseFeeClaimController {

    private final CourseFeeClaimService claimService;
    private final ClaimAttachmentService attachmentService;
    private final EmployeeRepository employeeRepository;

    public CourseFeeClaimController(CourseFeeClaimService claimService,
            ClaimAttachmentService attachmentService, EmployeeRepository employeeRepository) {
        this.claimService = claimService;
        this.attachmentService = attachmentService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/claims/{id}")
    public String showClaim(@PathVariable Long id, HttpSession session, Model model) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return "redirect:/employee/login";
        }
        CourseFeeClaim claim;
        try {
            claim = claimService.findViewableClaim(id, employee);
            model.addAttribute("history", claimService.findHistory(id, employee));
        } catch (SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Employee owner = claim.getApplication().getEmployee();
        boolean isOwner = owner.getId().equals(employee.getId());
        boolean isAssignedManager = false;
        if (employee.getRole() == EmployeeRole.MANAGER && !isOwner && owner.getSupervisor() != null) {
            isAssignedManager = owner.getSupervisor().getId().equals(employee.getId());
        }
        model.addAttribute("claim", claim);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canReview", isAssignedManager && claim.getStatus() == ClaimStatus.PENDING);
        model.addAttribute("canPay", employee.getRole() == EmployeeRole.ADMIN
                && claim.getStatus() == ClaimStatus.APPROVED);
        model.addAttribute("today", LocalDate.now());
        if (employee.getRole() == EmployeeRole.ADMIN) {
            model.addAttribute("listUrl", "/admin/claims");
            model.addAttribute("activeNavigation", "reimbursements");
        } else if (isAssignedManager) {
            model.addAttribute("listUrl", "/manager/claims");
            model.addAttribute("activeNavigation", "team-claims");
        } else {
            model.addAttribute("listUrl", "/employee/claims");
            model.addAttribute("activeNavigation", "claims");
        }
        return "claims/detail";
    }

    @GetMapping("/claims/{claimId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long claimId,
            @PathVariable Long attachmentId, HttpSession session) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ClaimAttachment attachment;
        Resource resource;
        try {
            attachment = claimService.findViewableAttachment(claimId, attachmentId, employee);
            resource = attachmentService.load(attachment);
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
        String disposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8).build().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private Employee getCurrentUser(HttpSession session) {
        Employee sessionEmployee = (Employee) session.getAttribute("loggedInUser");
        if (sessionEmployee == null || sessionEmployee.getId() == null) {
            return null;
        }
        Optional<Employee> employeeResult = employeeRepository.findById(sessionEmployee.getId());
        if (employeeResult.isEmpty()) {
            return null;
        }
        return employeeResult.get();
    }
}
