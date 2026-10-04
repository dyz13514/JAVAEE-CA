package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CourseApplicationService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ManagerController {
    private final CourseApplicationService courseApplicationService;

    public ManagerController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @GetMapping("/manager/approvals")
    public String showApprovals(HttpSession session, Model model) {
        Employee manager = (Employee) session.getAttribute("loggedInUser");
        if (manager == null) {
            return "redirect:/employee/login";

        }
        if (manager.getRole() != EmployeeRole.MANAGER) {
            return "redirect:/employee/home";
        }
        model.addAttribute("applications", courseApplicationService.findSubordinateApplications(manager));
        return "manager-approvals";
    }

    @PostMapping("/manager/review/{id}")
    public String reviewApplication(@PathVariable Long id, @RequestParam String decision, @RequestParam String comment,
            HttpSession session, RedirectAttributes redirectAttrs) {
        Employee manager = (Employee) session.getAttribute("loggedInUser");
        if (manager == null) {
            return "redirect:/employee/login";
        }
        if (manager.getRole() != EmployeeRole.MANAGER) {
            return "redirect:/employee/home";
        }
        String message = courseApplicationService.reviewApplication(id, manager, decision, comment);
        redirectAttrs.addFlashAttribute("message", message);
        return "redirect:/manager/approvals";
    }
}