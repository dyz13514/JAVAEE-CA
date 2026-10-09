package com.group5.cats.service;

import java.util.List;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;

public interface TrainingCalendarService {

    List<Employee> findEmployeesForCalendar(Employee currentUser);

    Employee findCalendarEmployee(Employee currentUser, Long employeeId);

    List<CourseApplication> findApprovedApplicationsByMonth(
            Employee currentUser,
            Long employeeId,
            Integer year,
            Integer month);
}
