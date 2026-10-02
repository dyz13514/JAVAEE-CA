package com.group5.cats;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;

@Component 
public class DataLoader implements CommandLineRunner {
private final EmployeeRepository employeeRepository;
public DataLoader(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
     public void run(String... args) throws Exception {
        if (employeeRepository.count() == 0) {
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
