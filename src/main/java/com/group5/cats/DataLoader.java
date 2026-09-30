package com.group5.cats;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Employee;
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
            employeeRepository.saveAll(List.of(
                new Employee("admin", "admin123", "System Admin", "ADMIN"),
                new Employee("manager1", "manager123", "Alice Wong", "MANAGER"),
                new Employee("emp1", "emp123", "Ben Tan", "EMPLOYEE"),
                new Employee("emp2", "emp123", "Cathy Lim", "EMPLOYEE")
            ));
        }
    }

}
