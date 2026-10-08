package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminHomeController {

    @GetMapping("/admin/home")
    public String showAdminHome(HttpSession session, Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        model.addAttribute("currentUser", loggedInUser);

        return "admin/home";
    }
}