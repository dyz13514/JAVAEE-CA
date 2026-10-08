package com.group5.cats.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.cats.dto.EmployeeForm;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.AnnualEntitlementRepository;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CourseApplicationRepository courseApplicationRepository;
    private final AnnualEntitlementRepository annualEntitlementRepository;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            CourseApplicationRepository courseApplicationRepository,
            AnnualEntitlementRepository annualEntitlementRepository) {

        this.employeeRepository = employeeRepository;
        this.courseApplicationRepository = courseApplicationRepository;
        this.annualEntitlementRepository = annualEntitlementRepository;
    }

    @Override
    public List<Employee> findAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public List<Employee> findAllManagers() {
        return employeeRepository.findByRole(EmployeeRole.MANAGER);
    }

    @Override
    public Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id).orElse(null);
    }

    @Override
    public String createEmployee(EmployeeForm employeeForm) {

        if (employeeForm.getUsername() == null
                || employeeForm.getUsername().isBlank()) {
            return "Username is required.";
        }

        if (employeeForm.getName() == null
                || employeeForm.getName().isBlank()) {
            return "Name is required.";
        }

        if (employeeForm.getPassword() == null
                || employeeForm.getPassword().isBlank()) {
            return "Password is required.";
        }

        if (employeeForm.getRole() == null) {
            return "Role is required.";
        }

        if (employeeForm.getDesignation() == null) {
            return "Designation is required.";
        }

        String username = employeeForm.getUsername().trim();

        if (employeeRepository.findByUsername(username).isPresent()) {
            return "Username already exists.";
        }

        Employee supervisor = null;

        if (employeeForm.getSupervisorId() != null) {
            Optional<Employee> result = employeeRepository.findById(
                    employeeForm.getSupervisorId()
            );

            if (result.isEmpty()) {
                return "Selected supervisor does not exist.";
            }

            supervisor = result.get();

            if (supervisor.getRole() != EmployeeRole.MANAGER) {
                return "Supervisor must be a manager.";
            }
        }

        Employee employee = new Employee();

        employee.setUsername(username);
        employee.setName(employeeForm.getName().trim());
        employee.setPassword(employeeForm.getPassword());
        employee.setRole(employeeForm.getRole());
        employee.setDesignation(employeeForm.getDesignation());
        employee.setSupervisor(supervisor);

        employeeRepository.save(employee);

        return null;
    }

    @Override
    @Transactional
    public String updateEmployee(Long id, EmployeeForm employeeForm) {

        Employee employee = employeeRepository.findById(id).orElse(null);

        if (employee == null) {
            return "Employee not found.";
        }

        if (employeeForm.getName() == null
                || employeeForm.getName().isBlank()) {
            return "Name is required.";
        }

        if (employeeForm.getRole() == null) {
            return "Role is required.";
        }

        if (employeeForm.getDesignation() == null) {
            return "Designation is required.";
        }

        if (employee.getRole() == EmployeeRole.ADMIN
                && employeeForm.getRole() != EmployeeRole.ADMIN) {

            List<Employee> admins =
                    employeeRepository.findByRole(EmployeeRole.ADMIN);

            if (admins.size() <= 1) {
                return "The last administrator must keep the ADMIN role.";
            }
        }

        if (employee.getRole() == EmployeeRole.MANAGER
                && employeeForm.getRole() != EmployeeRole.MANAGER) {

            List<Employee> subordinates =
                    employeeRepository.findBySupervisor_Id(id);

            if (!subordinates.isEmpty()) {
                return "Please reassign this manager's subordinates before changing the role.";
            }
        }

        Employee supervisor = null;

        if (employeeForm.getSupervisorId() != null) {

            if (employeeForm.getSupervisorId().equals(id)) {
                return "An employee cannot be their own supervisor.";
            }

            supervisor = employeeRepository.findById(
                    employeeForm.getSupervisorId()
            ).orElse(null);

            if (supervisor == null) {
                return "Selected supervisor does not exist.";
            }

            if (supervisor.getRole() != EmployeeRole.MANAGER) {
                return "Supervisor must be a manager.";
            }

            Set<Long> visitedIds = new HashSet<>();
            Employee currentSupervisor = supervisor;

            while (currentSupervisor != null) {

                if (currentSupervisor.getId().equals(id)) {
                    return "This supervisor would create a circular reporting relationship.";
                }

                if (!visitedIds.add(currentSupervisor.getId())) {
                    return "The selected supervisor's reporting relationship contains a cycle.";
                }

                currentSupervisor = currentSupervisor.getSupervisor();
            }
        }

        employee.setName(employeeForm.getName().trim());
        employee.setRole(employeeForm.getRole());
        employee.setDesignation(employeeForm.getDesignation());
        employee.setSupervisor(supervisor);

        if (employeeForm.getPassword() != null
                && !employeeForm.getPassword().isBlank()) {
            employee.setPassword(employeeForm.getPassword());
        }

        employeeRepository.save(employee);

        return null;
    }

    @Override
    @Transactional
    public String deleteEmployee(Long id, Long loggedInUserId) {

        Employee employee = employeeRepository.findById(id).orElse(null);

        if (employee == null) {
            return "Employee not found.";
        }

        if (id.equals(loggedInUserId)) {
            return "You cannot delete your own account.";
        }

        if (employee.getRole() == EmployeeRole.ADMIN) {

            List<Employee> admins =
                    employeeRepository.findByRole(EmployeeRole.ADMIN);

            if (admins.size() <= 1) {
                return "The last administrator cannot be deleted.";
            }
        }

        List<Employee> subordinates =
                employeeRepository.findBySupervisor_Id(id);

        if (!subordinates.isEmpty()) {
            return "Please reassign this employee's subordinates before deleting the employee.";
        }

        if (!courseApplicationRepository.findByEmployee(employee).isEmpty()) {
            return "This employee has course application records and cannot be deleted.";
        }

        annualEntitlementRepository.deleteByEmployeeId(id);

        annualEntitlementRepository.flush();

        employeeRepository.delete(employee);

        return null;
    }
}