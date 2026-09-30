package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/employee/login")
    public String login() {
        return "employee-login";
    }

}
