package com.group5.cats;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;

@Component
@Order(2)
public class DataLoader implements CommandLineRunner {
    private final EmployeeRepository employeeRepository;

    public DataLoader(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Employee manager = findOrCreateEmployee("manager1", "manager123", "Alice Wong",
                null, EmployeeDesignation.PROFESSIONAL, EmployeeRole.MANAGER);
        findOrCreateEmployee("admin", "admin123", "System Admin",
                null, EmployeeDesignation.ADMINISTRATIVE, EmployeeRole.ADMIN);
        findOrCreateEmployee("emp1", "emp123", "Ben Tan",
                manager, EmployeeDesignation.PROFESSIONAL, EmployeeRole.REGULAR_STAFF);
        findOrCreateEmployee("emp2", "emp123", "Cathy Lim",
                manager, EmployeeDesignation.ADMINISTRATIVE, EmployeeRole.REGULAR_STAFF);
    }

    private Employee findOrCreateEmployee(String username, String password, String name,
            Employee supervisor, EmployeeDesignation designation, EmployeeRole role) {
        Employee employee = employeeRepository.findByUsername(username).orElse(null);
        if (employee == null) {
            employee = employeeRepository.save(
                    new Employee(username, password, name, supervisor, designation, role));
        }
        return employee;
    }
}
