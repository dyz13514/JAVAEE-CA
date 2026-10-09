package com.group5.cats.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import com.group5.cats.model.DeliveryStatus;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.NotificationHistoryService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AdminNotificationController {
    private final NotificationHistoryService historyService;

    public AdminNotificationController(NotificationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/admin/notifications")
    public String showNotifications(@RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/admin/login";
        if (user.getRole() != EmployeeRole.ADMIN) return "redirect:/employee/home";
        if (page < 0 || page > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page.");
        DeliveryStatus selected = null;
        if (!status.isBlank()) {
            try {
                selected = DeliveryStatus.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid delivery status.");
            }
        }
        model.addAttribute("notifications", historyService.findNotifications(selected, page));
        model.addAttribute("statuses", DeliveryStatus.values());
        model.addAttribute("selectedStatus", selected == null ? "" : selected.name());
        return "admin/notifications";
    }
}
