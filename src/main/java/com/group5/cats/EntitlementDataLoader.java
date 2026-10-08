package com.group5.cats;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.EntitlementService;

@Component
@Order(2)
public class EntitlementDataLoader implements CommandLineRunner {
	
	private final EmployeeRepository employeeRepository;
	private final EntitlementService entitlementService;
	
	public EntitlementDataLoader(
			EmployeeRepository employeeRepository,
			EntitlementService entitlementService) {
		this.employeeRepository = employeeRepository;
		this.entitlementService = entitlementService;
	}
	
	public void run(String... args) throws Exception {
		
		if (employeeRepository.findByUsername("entitlememt_test").isEmpty()) {
		
		Employee employee = new Employee("entitlememt_test", "entitlememt_test_password", "entitlememt_test", null, EmployeeDesignation.ADMINISTRATIVE, EmployeeRole.REGULAR_STAFF  );
		
		employee = employeeRepository.save(employee);
		
		entitlementService.createDefaultEntitlement(
				employee.getId(),
				LocalDate.now().getYear());
	}
	}


}
