package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.dto.EmployeeForm;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.EmployeeService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    public AdminEmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/admin/employees")
    public String showEmployees(HttpSession session, Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        model.addAttribute(
                "employees",
                employeeService.findAllEmployees()
        );

        return "admin/employees";
    }

    @GetMapping("/admin/employees/new")
    public String showNewEmployeeForm(HttpSession session, Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        EmployeeForm employeeForm = new EmployeeForm();
        employeeForm.setRole(EmployeeRole.REGULAR_STAFF);

        model.addAttribute("employeeForm", employeeForm);
        model.addAttribute("roles", EmployeeRole.values());
        model.addAttribute("designations", EmployeeDesignation.values());
        model.addAttribute("managers", employeeService.findAllManagers());

        return "admin/employee-form";
    }

    @PostMapping("/admin/employees/save")
    public String saveEmployee(
            @ModelAttribute("employeeForm") EmployeeForm employeeForm,
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

        String error = employeeService.createEmployee(employeeForm);

        if (error != null) {
            employeeForm.setPassword("");

            model.addAttribute("errorMessage", error);
            model.addAttribute("roles", EmployeeRole.values());
            model.addAttribute(
                    "designations",
                    EmployeeDesignation.values()
            );
            model.addAttribute(
                    "managers",
                    employeeService.findAllManagers()
            );

            return "admin/employee-form";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Employee created successfully."
        );

        return "redirect:/admin/employees";
    }

    @GetMapping("/admin/employees/{id}/edit")
    public String showEditEmployeeForm(
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

        Employee employee = employeeService.findEmployeeById(id);

        if (employee == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Employee not found."
            );

            return "redirect:/admin/employees";
        }

        EmployeeForm employeeForm = new EmployeeForm();

        employeeForm.setUsername(employee.getUsername());
        employeeForm.setName(employee.getName());
        employeeForm.setEmail(employee.getEmail());
        employeeForm.setRole(employee.getRole());
        employeeForm.setDesignation(employee.getDesignation());

        if (employee.getSupervisor() != null) {
            employeeForm.setSupervisorId(
                    employee.getSupervisor().getId()
            );
        }

        model.addAttribute("employeeId", employee.getId());
        model.addAttribute("employeeForm", employeeForm);
        model.addAttribute("roles", EmployeeRole.values());
        model.addAttribute("designations", EmployeeDesignation.values());
        model.addAttribute("managers", employeeService.findAllManagers());

        return "admin/employee-edit";
    }

    @PostMapping("/admin/employees/{id}/update")
    public String updateEmployee(
            @PathVariable("id") Long id,
            @ModelAttribute("employeeForm") EmployeeForm employeeForm,
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

        Employee employee = employeeService.findEmployeeById(id);

        if (employee == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Employee not found."
            );

            return "redirect:/admin/employees";
        }

        employeeForm.setUsername(employee.getUsername());

        String error = employeeService.updateEmployee(id, employeeForm);

        if (error != null) {
            employeeForm.setPassword("");

            model.addAttribute("errorMessage", error);
            model.addAttribute("employeeId", id);
            model.addAttribute("employeeForm", employeeForm);
            model.addAttribute("roles", EmployeeRole.values());
            model.addAttribute(
                    "designations",
                    EmployeeDesignation.values()
            );
            model.addAttribute(
                    "managers",
                    employeeService.findAllManagers()
            );

            return "admin/employee-edit";
        }

        if (loggedInUser.getId().equals(id)) {
            Employee updatedUser = employeeService.findEmployeeById(id);
            session.setAttribute("loggedInUser", updatedUser);

            if (updatedUser.getRole() != EmployeeRole.ADMIN) {
                return "redirect:/employee/home";
            }
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Employee updated successfully."
        );

        return "redirect:/admin/employees";
    }

    @PostMapping("/admin/employees/{id}/delete")
    public String deleteEmployee(
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

        String error = employeeService.deleteEmployee(
                id,
                loggedInUser.getId()
        );

        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Employee deleted successfully."
            );
        }

        return "redirect:/admin/employees";
    }
}