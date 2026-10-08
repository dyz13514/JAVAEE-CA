package com.group5.cats.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.model.EntitlementSummary;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.EntitlementService;

import jakarta.servlet.http.HttpSession;

@Controller
public class EntitlementController {
	
	private final EntitlementService entitlementService;
	private final EmployeeRepository employeeRepository;
	
	public EntitlementController (
			EntitlementService entitlementService,
			EmployeeRepository employeeRepository) {
		this.entitlementService = entitlementService;
		this.employeeRepository = employeeRepository;
	}
	
	private Employee getCurrentUser(HttpSession session) {
		
		Employee sessionEmployee = (Employee) session.getAttribute("loggedInUser");
		
		if (sessionEmployee == null) {
			return null;
		}
		
		return employeeRepository.findById(sessionEmployee.getId()).orElse(null);
	}
	
	private List<Employee> getQueryableEmployees(Employee currentUser) {
	
		return entitlementService.getQueryableEmployees(currentUser);
	}

	
	@GetMapping("/entitlements")
	public String showEntitlement(
			@RequestParam(required = false) Long employeeId,
			@RequestParam(required = false) Integer entitlementYear,
			HttpSession session,
			Model model)
	{
		
		Employee currentUser = getCurrentUser(session);
		
		if(currentUser == null) {
			return "redirect:/employee/login";
		}
		
		List<Employee> toBeViewedEmployees = getQueryableEmployees(currentUser);
		
		Long selectedId;
		if (employeeId == null) {
			selectedId = currentUser.getId();
		} else {
			selectedId = employeeId;
		}
		
		Integer selectedYear;
		if (entitlementYear == null) {
			selectedYear = LocalDate.now().getYear();
		} else {
			selectedYear = entitlementYear;
		}
		
		

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("toBeViewedEmployees", toBeViewedEmployees);
		model.addAttribute("selectedId", selectedId);
		model.addAttribute("selectedYear", selectedYear);
		model.addAttribute("canEdit", currentUser.getRole() == EmployeeRole.ADMIN);
		
		Employee selectedEmployee = null;
		
		for(Employee employee : toBeViewedEmployees) {
			if (employee.getId().equals(selectedId)) {
				selectedEmployee = employee;
				break;
			}
		}
		
		if(selectedEmployee == null) {
			model.addAttribute("message", "You are not authorised.");
			return "entitlement";
		}
		
		
		model.addAttribute("selectedEmployee", selectedEmployee);
		
		try{
		Optional<EntitlementSummary> result = entitlementService.getEntitlementSummary(selectedId, selectedYear);
		
			if(result.isPresent()) {
				
				EntitlementSummary summary = result.get();
		

			model.addAttribute("entitlement", summary);
			
			model.addAttribute("occupiedDays", summary.getOccupiedDays());
			model.addAttribute("occupiedBudget", summary.getOccupiedBudget());
			model.addAttribute("remainingDays", summary.getRemainingDays());
			model.addAttribute("remainingBudget", summary.getRemainingBudget());
			
		} else {
			model.addAttribute("message", "No entitlement record for this employee in this year.");
		}
		}
	catch (IllegalArgumentException exception) {
		model.addAttribute("message", exception.getMessage());
	}
		
		
		return "entitlement";
		 
		
	}
	
	@PostMapping("/admin/entitlements")
	public String setEntitlement(
			@RequestParam Long employeeId,
			@RequestParam Integer entitlementYear,
			@RequestParam double trainingDaysLimit,
			@RequestParam double trainingBudget,
			HttpSession session,
			RedirectAttributes redirectAttributes
			) {
		
		Employee currentUser = getCurrentUser(session);
		if (currentUser == null) {
			return "redirect:/employee/login";
		}
		
		if(currentUser.getRole() != EmployeeRole.ADMIN) {
			redirectAttributes.addFlashAttribute("saveMessage", 
					"Only administrators can change entitlements.");
			return "redirect:/entitlements";
		}
		
		try{
			entitlementService.setEntitlement(employeeId, entitlementYear, trainingDaysLimit, trainingBudget);
			redirectAttributes.addFlashAttribute("saveMessage", "Entitlement saved.");
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("saveMessage", exception.getMessage());
		}
		
		
		redirectAttributes.addAttribute("employeeId",employeeId);
		redirectAttributes.addAttribute("entitlementYear",entitlementYear);
		
		return "redirect:/entitlements";
		
	}
	
	@PostMapping("/admin/entitlements/delete")
	public String deleteEntitlement (
			@RequestParam Long employeeId,
			@RequestParam Integer entitlementYear,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		
		Employee currentUser = getCurrentUser(session);
		if (currentUser == null) {
			return "redirect:/employee/login";
		}
		
		if(currentUser.getRole() != EmployeeRole.ADMIN) {
			redirectAttributes.addFlashAttribute("saveMessage", 
					"Only administrators can delete entitlements.");
			return "redirect:/entitlements";
		}
		
		try{
			entitlementService.deleteEntitlement(employeeId, entitlementYear);
			redirectAttributes.addFlashAttribute("saveMessage", "Entitlement deleted.");
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("saveMessage", exception.getMessage());
		}
		
		redirectAttributes.addAttribute("employeeId",employeeId);
		redirectAttributes.addAttribute("entitlementYear",entitlementYear);
		
		return "redirect:/entitlements";

		
	}
	
	@GetMapping("/entitlements/search")
	public String searchEmployees(
			@RequestParam(required = false) Long queryEmployeeId,
			@RequestParam(required = false) String queryEmployeeName,
			HttpSession session,
			Model model) {
		
		Employee currentUser = getCurrentUser(session);
		if (currentUser == null) {
			return "redirect:/employee/login";
		}
		
		List<Employee> canBeSearchedEmployees = getQueryableEmployees(currentUser);
		List<Employee> searchedEmployees = new ArrayList<>();
		
		
		if(queryEmployeeId != null) {
			for(Employee employee : canBeSearchedEmployees) {
				if(employee.getId().equals(queryEmployeeId)) {
					searchedEmployees.add(employee);
				}
			}
		} else if (queryEmployeeName != null && !queryEmployeeName.trim().isEmpty()) {
			String keyword = queryEmployeeName.trim().toLowerCase();
			
			for(Employee employee : canBeSearchedEmployees) {
				if(employee.getName() != null && employee.getName().toLowerCase().contains(keyword)) {
					searchedEmployees.add(employee);
				}
			}
		} 
		
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("toBeViewedEmployees", searchedEmployees);
		model.addAttribute("selectedId", null);
		model.addAttribute("selectedYear", LocalDate.now().getYear());
		model.addAttribute("canEdit", currentUser.getRole() == EmployeeRole.ADMIN);
		
		if(searchedEmployees.isEmpty()) {
			model.addAttribute("message", "No matching employees.");
		} else {
			model.addAttribute("message", "Search successful, found " + searchedEmployees.size() + " employee(s). Use the Employee bar to select.");
		}
		
		return "entitlement";
			
		}

		
		
	}
	
	
			
	
	

	

	


