package com.group5.cats.controller;

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
public class ManagerClaimController {

    private final CourseFeeClaimService claimService;
    private final EmployeeRepository employeeRepository;

    public ManagerClaimController(CourseFeeClaimService claimService, EmployeeRepository employeeRepository) {
        this.claimService = claimService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/manager/claims")
    public String showTeamClaims(HttpSession session, Model model) {
        Employee manager = getCurrentUser(session);
        if (manager == null) {
            return "redirect:/employee/login";
        }
        if (manager.getRole() != EmployeeRole.MANAGER) {
            return "redirect:/employee/home";
        }
        model.addAttribute("claims", claimService.findTeamClaims(manager));
        model.addAttribute("pageTitle", "Team claims");
        model.addAttribute("activeNavigation", "team-claims");
        model.addAttribute("canSubmit", false);
        return "claims/list";
    }

    @PostMapping("/manager/claims/{id}/review")
    public String reviewClaim(@PathVariable Long id, @RequestParam Long version,
            @RequestParam String decision, @RequestParam String reason,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Employee manager = getCurrentUser(session);
        if (manager == null) {
            return "redirect:/employee/login";
        }
        if (manager.getRole() != EmployeeRole.MANAGER) {
            return "redirect:/employee/home";
        }
        String error;
        try {
            error = claimService.reviewClaim(id, version, decision, reason, manager);
        } catch (OptimisticLockingFailureException exception) {
            error = "This claim has changed. Please reload it before reviewing.";
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        }
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "The claim review has been recorded.");
        }
        return "redirect:/manager/claims";
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
