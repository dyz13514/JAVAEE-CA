package com.group5.cats.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EntitlementSummary;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.EntitlementService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/entitlements")
public class EntitlementRestController {

	private final EntitlementService entitlementService;
	private final EmployeeRepository employeeRepository;

	public EntitlementRestController(EntitlementService entitlementService, EmployeeRepository employeeRepository) {
		this.entitlementService = entitlementService;
		this.employeeRepository = employeeRepository;
	}

	@GetMapping("/summary")
	public ResponseEntity<?> getSummary(
			@RequestParam(required = false) Long employeeId, 
			@RequestParam(required = false) Integer entitlementYear, 
			HttpSession session) {
		
		Employee currentUser = getCurrentUser(session);
		if(currentUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "Please log in."));
		}
		
		
		if(employeeId == null || employeeId <= 0) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid employeeID."));
		}
		if(entitlementYear == null || entitlementYear <= 0) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid entitlementYear."));
		}
		
		List<Employee> employees = entitlementService.getQueryableEmployees(currentUser);
		
		boolean canQuery = false;
		
		for(Employee employee : employees) {
			if (employee.getId().equals(employeeId)) {
				canQuery = true;
				break;
			}
		}
		
		if(!canQuery) {
			return ResponseEntity.status(403).body(Map.of("message", "You are not authorised."));
		}
		
		
		try{
			Optional<EntitlementSummary> result = entitlementService.getEntitlementSummary(employeeId, entitlementYear);
		
		
		if (result.isEmpty()) {
			return ResponseEntity.status(404).body(Map.of("message", "No entitlement record for this employee in this year."));
		}
		
		return ResponseEntity.ok(result.get());
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
				
		
	}

	private Employee getCurrentUser(HttpSession session) {

		Employee sessionEmployee = (Employee) session.getAttribute("loggedInUser");

		if (sessionEmployee == null) {
			return null;
		}

		return employeeRepository.findById(sessionEmployee.getId()).orElse(null);
	}
}
