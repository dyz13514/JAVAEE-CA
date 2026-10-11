package com.group5.cats.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group5.cats.dto.TrainingLedger;
import com.group5.cats.model.Employee;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.CourseFeeClaimService;
import com.group5.cats.service.EntitlementService;
import com.group5.cats.service.TrainingLedgerCsv;

import jakarta.servlet.http.HttpSession;

@Controller
public class TrainingLedgerController {

    private final CourseFeeClaimService claimService;
    private final EntitlementService entitlementService;
    private final EmployeeRepository employeeRepository;

    public TrainingLedgerController(CourseFeeClaimService claimService,
            EntitlementService entitlementService, EmployeeRepository employeeRepository) {
        this.claimService = claimService;
        this.entitlementService = entitlementService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/training-ledger")
    public String showLedger(@RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year, HttpSession session, Model model) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return "redirect:/employee/login";
        }
        Long selectedId = employeeId;
        if (selectedId == null) {
            selectedId = employee.getId();
        }
        Integer selectedYear = year;
        if (selectedYear == null) {
            selectedYear = LocalDate.now().getYear();
        }
        model.addAttribute("employees", entitlementService.getQueryableEmployees(employee));
        model.addAttribute("selectedId", selectedId);
        model.addAttribute("selectedYear", selectedYear);
        try {
            TrainingLedger ledger = claimService.getTrainingLedger(selectedId, selectedYear, employee);
            model.addAttribute("ledger", ledger);
        } catch (SecurityException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
        }
        return "claims/ledger";
    }

    @GetMapping("/training-ledger/export.csv")
    public ResponseEntity<byte[]> exportLedger(@RequestParam Long employeeId,
            @RequestParam Integer year, HttpSession session) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        TrainingLedger ledger;
        try {
            ledger = claimService.getTrainingLedger(employeeId, year, employee);
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
        byte[] csv = TrainingLedgerCsv.export(ledger).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=training-ledger-"
                        + employeeId + "-" + year + ".csv")
                .body(csv);
    }

    private Employee getCurrentUser(HttpSession session) {
        Employee sessionEmployee = (Employee) session.getAttribute("loggedInUser");
        if (sessionEmployee == null || sessionEmployee.getId() == null) {
            return null;
        }
        Optional<Employee> employeeResult = employeeRepository.findById(sessionEmployee.getId());
        if (employeeResult.isEmpty()) {
            return null;
        }
        return employeeResult.get();
    }
}
