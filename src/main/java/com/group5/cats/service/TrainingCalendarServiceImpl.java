package com.group5.cats.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;

@Service
public class TrainingCalendarServiceImpl
        implements TrainingCalendarService {

    private final CourseApplicationRepository courseApplicationRepository;
    private final EmployeeRepository employeeRepository;

    public TrainingCalendarServiceImpl(
            CourseApplicationRepository courseApplicationRepository,
            EmployeeRepository employeeRepository) {

        this.courseApplicationRepository = courseApplicationRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<Employee> findEmployeesForCalendar(Employee currentUser) {

        if (currentUser.getRole() == EmployeeRole.ADMIN) {
            return employeeRepository.findAll();
        }

        if (currentUser.getRole() == EmployeeRole.MANAGER) {
            return employeeRepository.findBySupervisor_Id(currentUser.getId());
        }

        throw new SecurityException(
                "You are not allowed to view other employees' calendars."
        );
    }

    @Override
    public Employee findCalendarEmployee(
            Employee currentUser,
            Long employeeId) {

        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Please select a valid employee.");
        }

        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        // Use the same message for a missing employee and an inaccessible one.
        if (employee == null) {
            throw new SecurityException("You are not allowed to view this calendar.");
        }

        if (currentUser.getId().equals(employee.getId())) {
            return employee;
        }

        if (currentUser.getRole() == EmployeeRole.ADMIN) {
            return employee;
        }

        if (currentUser.getRole() == EmployeeRole.MANAGER
                && employee.getSupervisor() != null
                && currentUser.getId().equals(employee.getSupervisor().getId())) {
            return employee;
        }

        throw new SecurityException("You are not allowed to view this calendar.");
    }

    @Override
    public List<CourseApplication> findApprovedApplicationsByMonth(
            Employee currentUser,
            Long employeeId,
            Integer year,
            Integer month) {

        // Check access here too, before querying any course information.
        Employee employee = findCalendarEmployee(currentUser, employeeId);

        if (year == null || year < 1 || year > 9999) {
            throw new IllegalArgumentException(
                    "Year must be between 1 and 9999."
            );
        }

        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Month must be between 1 and 12."
            );
        }

        LocalDate monthStart = LocalDate.of(year, month, 1);

        LocalDate monthEnd = monthStart.withDayOfMonth(
                monthStart.lengthOfMonth()
        );

        return courseApplicationRepository
                .findApprovedApplicationsForCalendar(
                        employee.getId(),
                        monthStart,
                        monthEnd
                );
    }
}
