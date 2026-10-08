package com.group5.cats.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.model.PublicHoliday;
import com.group5.cats.service.PublicHolidayService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminPublicHolidayController {

    private final PublicHolidayService publicHolidayService;

    public AdminPublicHolidayController(
            PublicHolidayService publicHolidayService) {

        this.publicHolidayService = publicHolidayService;
    }

    @GetMapping("/admin/public-holidays")
    public String showPublicHolidays(
            @RequestParam(value = "year", required = false) Integer year,
            HttpSession session,
            Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        Integer selectedYear;

        if (year == null) {
            selectedYear = LocalDate.now().getYear();
        } else {
            selectedYear = year;
        }

        List<PublicHoliday> holidays = new ArrayList<>();

        try {
            holidays = publicHolidayService.findHolidaysByYear(
                    selectedYear
            );
        } catch (IllegalArgumentException exception) {
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("holidays", holidays);

        return "admin/public-holidays";
    }

    @GetMapping("/admin/public-holidays/new")
    public String showNewHolidayForm(
            HttpSession session,
            Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        model.addAttribute("holidayDate", "");
        model.addAttribute("name", "");

        return "admin/public-holiday-form";
    }

    @PostMapping("/admin/public-holidays/save")
    public String saveHoliday(
            @RequestParam("holidayDate") String holidayDate,
            @RequestParam("name") String name,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        model.addAttribute("holidayDate", holidayDate);
        model.addAttribute("name", name);

        LocalDate date;

        try {
            date = LocalDate.parse(holidayDate);
        } catch (DateTimeParseException exception) {
            model.addAttribute(
                    "errorMessage",
                    "Please enter a valid holiday date."
            );

            return "admin/public-holiday-form";
        }

        String error = publicHolidayService.createHoliday(date, name);

        if (error != null) {
            model.addAttribute("errorMessage", error);

            return "admin/public-holiday-form";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Public holiday created successfully."
        );

        redirectAttributes.addAttribute("year", date.getYear());

        return "redirect:/admin/public-holidays";
    }

    @GetMapping("/admin/public-holidays/{id}/edit")
    public String showEditHolidayForm(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        PublicHoliday holiday =
                publicHolidayService.findHolidayById(id);

        if (holiday == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Public holiday not found."
            );

            return "redirect:/admin/public-holidays";
        }

        model.addAttribute("holidayId", holiday.getId());
        model.addAttribute(
                "holidayDate",
                holiday.getHolidayDate().toString()
        );
        model.addAttribute("name", holiday.getName());
        model.addAttribute(
                "selectedYear",
                holiday.getHolidayDate().getYear()
        );

        return "admin/public-holiday-edit";
    }

    @PostMapping("/admin/public-holidays/{id}/update")
    public String updateHoliday(
            @PathVariable("id") Long id,
            @RequestParam("holidayDate") String holidayDate,
            @RequestParam("name") String name,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        PublicHoliday holiday =
                publicHolidayService.findHolidayById(id);

        if (holiday == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Public holiday not found."
            );

            return "redirect:/admin/public-holidays";
        }

        model.addAttribute("holidayId", id);
        model.addAttribute("holidayDate", holidayDate);
        model.addAttribute("name", name);
        model.addAttribute(
                "selectedYear",
                holiday.getHolidayDate().getYear()
        );

        LocalDate date;

        try {
            date = LocalDate.parse(holidayDate);
        } catch (DateTimeParseException exception) {
            model.addAttribute(
                    "errorMessage",
                    "Please enter a valid holiday date."
            );

            return "admin/public-holiday-edit";
        }

        String error = publicHolidayService.updateHoliday(
                id,
                date,
                name
        );

        if (error != null) {
            model.addAttribute("errorMessage", error);

            return "admin/public-holiday-edit";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Public holiday updated successfully."
        );

        redirectAttributes.addAttribute("year", date.getYear());

        return "redirect:/admin/public-holidays";
    }

    @PostMapping("/admin/public-holidays/{id}/delete")
    public String deleteHoliday(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        PublicHoliday holiday =
                publicHolidayService.findHolidayById(id);

        if (holiday == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Public holiday not found."
            );

            return "redirect:/admin/public-holidays";
        }

        int year = holiday.getHolidayDate().getYear();

        String error = publicHolidayService.deleteHoliday(id);

        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Public holiday deleted successfully."
            );
        }

        redirectAttributes.addAttribute("year", year);

        return "redirect:/admin/public-holidays";
    }
}