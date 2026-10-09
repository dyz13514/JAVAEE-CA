package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.group5.cats.dto.ApplicationSearch;
import com.group5.cats.dto.ApplicationPagination;
import com.group5.cats.model.CourseApplication;
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
    public String showApprovals(@ModelAttribute("search") ApplicationSearch search,
            HttpSession session, Model model) {
        Employee manager = (Employee) session.getAttribute("loggedInUser");
        if (manager == null) {
            return "redirect:/employee/login";

        }
        if (manager.getRole() != EmployeeRole.MANAGER) {
            return "redirect:/employee/home";
        }
        Page<CourseApplication> result = courseApplicationService.searchTeamApplications(manager, search);
        model.addAttribute("applications", result.getContent());
        model.addAttribute("pagination", new ApplicationPagination(result));
        model.addAttribute("listUrl", "/manager/approvals");
        return "manager-approvals";
    }

    @PostMapping("/manager/review/{id}")
    public String reviewApplication(@PathVariable Long id, @RequestParam String decision, @RequestParam String comment,
            @ModelAttribute("search") ApplicationSearch search,
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
        redirectAttrs.addAttribute("page", search.getPage());
        redirectAttrs.addAttribute("size", search.getSize());
        redirectAttrs.addAttribute("keyword", search.getKeyword());
        return "redirect:/manager/approvals";
    }
}
