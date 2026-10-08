package com.group5.cats.service;

import java.util.List;

import com.group5.cats.dto.EmployeeForm;
import com.group5.cats.model.Employee;

public interface EmployeeService {

    List<Employee> findAllEmployees();

    List<Employee> findAllManagers();

    Employee findEmployeeById(Long id);

    String createEmployee(EmployeeForm employeeForm);

    String updateEmployee(Long id, EmployeeForm employeeForm);

    String deleteEmployee(Long id, Long loggedInUserId);
}