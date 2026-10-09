package com.group5.cats.controller;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import com.group5.cats.dto.RegistrationForm;
import com.group5.cats.model.*;
import com.group5.cats.service.*;

@Controller
public class RegistrationController {
    private final RegistrationService registrationService;
    private final EmployeeService employeeService;
    public RegistrationController(RegistrationService registrationService, EmployeeService employeeService) {
        this.registrationService = registrationService;
        this.employeeService = employeeService;
    }

    @InitBinder("registrationForm")
    void registrationFields(WebDataBinder binder) { binder.setAllowedFields("name", "username", "password", "confirmPassword"); }

    static String token(HttpSession session) {
        String token = (String) session.getAttribute("registrationToken");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("registrationToken", token);
        }
        return token;
    }

    private void verifyToken(HttpSession session, String submitted) {
        if (submitted == null || !submitted.equals(session.getAttribute("registrationToken")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reload the page and try again.");
    }

    @GetMapping("/register")
    public String showRegistration(HttpSession session, Model model) {
        model.addAttribute("accountMode", "register");
        model.addAttribute("registrationForm", new RegistrationForm());
        model.addAttribute("registrationToken", token(session));
        return "login";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registrationForm") RegistrationForm registrationForm,
            @RequestParam(required = false) String registrationToken, HttpSession session,
            Model model, RedirectAttributes redirectAttributes) {
        verifyToken(session, registrationToken);
        try {
            registrationService.submit(registrationForm);
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
            registrationForm.setPassword(null);
            registrationForm.setConfirmPassword(null);
            model.addAttribute("accountMode", "register");
            model.addAttribute("registrationError", exception instanceof DataIntegrityViolationException
                    ? "This username is already in use. Choose another username." : exception.getMessage());
            model.addAttribute("registrationToken", token(session));
            return "login";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Registration submitted. You can sign in after your administrator approves your account.");
        return "redirect:/employee/login";
    }

    @GetMapping("/admin/registrations")
    public String showRequests(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/admin/login";
        if (user.getRole() != EmployeeRole.ADMIN) return "redirect:/employee/home";
        model.addAttribute("registrations", registrationService.pending());
        model.addAttribute("registrationHistory", registrationService.recentReviews());
        model.addAttribute("pendingRegistrationCount", registrationService.pendingCount());
        model.addAttribute("designations", EmployeeDesignation.values());
        model.addAttribute("managers", employeeService.findAllManagers());
        model.addAttribute("registrationToken", token(session));
        return "admin/registrations";
    }

    @PostMapping("/admin/registrations/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) EmployeeDesignation designation,
            @RequestParam(required = false) Long supervisorId, @RequestParam(required = false) String registrationToken,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/admin/login";
        if (user.getRole() != EmployeeRole.ADMIN) return "redirect:/employee/home";
        verifyToken(session, registrationToken);
        try {
            registrationService.approve(id, designation, supervisorId, user);
            redirectAttributes.addFlashAttribute("successMessage", "Account approved. The employee and this year's training allowance are ready.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/registrations";
    }

    @PostMapping("/admin/registrations/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam String reason,
            @RequestParam(required = false) String registrationToken, HttpSession session, RedirectAttributes redirectAttributes) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/admin/login";
        if (user.getRole() != EmployeeRole.ADMIN) return "redirect:/employee/home";
        verifyToken(session, registrationToken);
        try {
            registrationService.reject(id, reason, user);
            redirectAttributes.addFlashAttribute("successMessage", "Registration rejected.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/registrations";
    }
}
