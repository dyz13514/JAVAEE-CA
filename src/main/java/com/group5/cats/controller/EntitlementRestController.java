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

	// 参考 choppee 项目的 package sg.edu.choppee.api 下的 REST Controller Class。
	@GetMapping("/summary")
	public ResponseEntity<?> getSummary(
			@RequestParam(required = false) Long employeeId, 
			@RequestParam(required = false) Integer entitlementYear, 
			HttpSession session) {
		//使用<?>因为成功时是EntitlementSummary，失败时是装文字提示的Map
		
		Employee currentUser = getCurrentUser(session);
		if(currentUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "Please log in."));
		}
		//.body决定响应体返回什么数据
		
		
		if(employeeId == null || employeeId <= 0) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid employeeID."));
		}
		if(entitlementYear == null || entitlementYear <= 0) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid entitlementYear."));
		}
		//这两个if检查employeeId和entitlementYear。所以虽然设置的required = false，但会在这里验证，不合规返回message。
		
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
		//.ok()是快捷写法，即创建状态为 200 的响应。括号里的对象result.get()是响应体内容
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
