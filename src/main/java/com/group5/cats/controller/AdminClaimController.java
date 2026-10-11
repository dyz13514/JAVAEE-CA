package com.group5.cats.controller;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.CourseFeeClaimService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminClaimController {

    private final CourseFeeClaimService claimService;
    private final EmployeeRepository employeeRepository;

    public AdminClaimController(CourseFeeClaimService claimService, EmployeeRepository employeeRepository) {
        this.claimService = claimService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/admin/claims")
    public String showClaims(HttpSession session, Model model) {
        Employee administrator = getCurrentUser(session);
        if (administrator == null) {
            return "redirect:/admin/login";
        }
        if (administrator.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }
        model.addAttribute("claims", claimService.findAllClaims(administrator));
        model.addAttribute("pageTitle", "Reimbursements");
        model.addAttribute("activeNavigation", "reimbursements");
        model.addAttribute("canSubmit", false);
        return "claims/list";
    }

    @PostMapping("/admin/claims/{id}/pay")
    public String recordPayment(@PathVariable Long id, @RequestParam Long version,
            @RequestParam String paymentDate, @RequestParam String reference,
            @RequestParam(required = false) String note,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Employee administrator = getCurrentUser(session);
        if (administrator == null) {
            return "redirect:/admin/login";
        }
        if (administrator.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }
        String error;
        try {
            LocalDate date = LocalDate.parse(paymentDate);
            error = claimService.reimburseClaim(id, version, date, reference, note, administrator);
        } catch (java.time.format.DateTimeParseException exception) {
            error = "Please enter a valid payment date.";
        } catch (OptimisticLockingFailureException exception) {
            error = "This claim has changed. Please reload it before recording payment.";
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        }
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "The reimbursement payment has been recorded.");
        }
        return "redirect:/admin/claims";
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
