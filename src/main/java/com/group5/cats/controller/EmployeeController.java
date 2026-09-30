package com.group5.cats.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EmployeeController {
    @GetMapping ("/employee/home")
    public String showEmployeeHome() {
        return "employee-home";
    }

}
