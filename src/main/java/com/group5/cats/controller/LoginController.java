package com.group5.cats.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.group5.cats.service.AuthService;
import com.group5.cats.model.Employee;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {
    private final AuthService authService;

    
    public LoginController(AuthService authService) {
        this.authService = authService;
    }



    @GetMapping("/employee/login")
    public String showEmployeeLogin() {
        return "employee-login";
    }

    @PostMapping("/employee/login")
    public String processEmployeeLogin(@RequestParam String username, @RequestParam String password,HttpSession session,RedirectAttributes redirectAttrs) {
        Optional<Employee> employee = authService.login(username, password);
        if (employee.isPresent()) {
            session.setAttribute("loggedInUser", employee.get());
            return "redirect:/employee/home";
        } else {
            redirectAttrs.addFlashAttribute("errorMessage", "Invalid username or password");
            return "redirect:/employee/login";
        }
        }
}