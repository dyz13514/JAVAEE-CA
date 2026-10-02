package com.group5.cats;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.EntitlementService;

@Component 
@Order(1)
public class DataLoader implements CommandLineRunner {
	
private final EmployeeRepository employeeRepository;
private final EntitlementService entitlementService;


public DataLoader(EmployeeRepository employeeRepository,
		EntitlementService entitlementService) {
        this.employeeRepository = employeeRepository;
        this.entitlementService = entitlementService;
    }

    @Override
     public void run(String... args) throws Exception {
        if (employeeRepository.count() == 0) {
            employeeRepository.saveAll(List.of(
                new Employee("admin", "admin123", "System Admin", EmployeeRole.ADMIN),
                new Employee("manager1", "manager123", "Alice Wong", EmployeeRole.MANAGER),
                new Employee("emp1", "emp123", "Ben Tan", EmployeeRole.REGULAR_STAFF),
                new Employee("emp2", "emp123", "Cathy Lim", EmployeeRole.REGULAR_STAFF)
            ));
        }
    }

}
