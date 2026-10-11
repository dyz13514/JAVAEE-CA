package com.group5.cats;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;

@Profile("!demo")
@Component 
@Order(1)
public class DataLoader implements CommandLineRunner {
	
private final EmployeeRepository employeeRepository;



public DataLoader(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
     public void run(String... args) throws Exception {
        boolean freshDemoDatabase = employeeRepository.count() == 0;
        if (freshDemoDatabase) {
           Employee manager = employeeRepository.save(
                new Employee("manager1", "manager123", "Alice Wong",
                        null, EmployeeDesignation.PROFESSIONAL, EmployeeRole.MANAGER));

            employeeRepository.saveAll(List.of(
                new Employee("admin", "admin123", "System Admin",
                        null, EmployeeDesignation.ADMINISTRATIVE, EmployeeRole.ADMIN),
                new Employee("emp1", "emp123", "Ben Tan",
                        manager, EmployeeDesignation.PROFESSIONAL, EmployeeRole.REGULAR_STAFF),
                new Employee("emp2", "emp123", "Cathy Lim",
                        manager, EmployeeDesignation.ADMINISTRATIVE, EmployeeRole.REGULAR_STAFF)
            ));
        }
    }
}
