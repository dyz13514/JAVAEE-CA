package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import com.group5.cats.model.Employee;

@Controller
public class EmployeeController {

    @GetMapping("/employee/home")
    public String home(HttpSession session, Model model) {
        Employee employee = (Employee) session.getAttribute("loggerInUser");
        if (employee == null) {
            return "redirect:/employee/login";
        }
        model.addAttribute("employee", employee);
        return "employee-home";
    }
}
