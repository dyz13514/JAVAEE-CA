package com.group5.cats.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group5.cats.model.Employee;
import com.group5.cats.repository.EmployeeRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;

    public AuthServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Optional<Employee> login(String username, String password) {
        return employeeRepository.findByUsername(username)
                .filter(employee -> employee.getPassword().equals(password));
    }
}
