package com.group5.cats.service;

import java.time.LocalDate;
import java.util.List;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.model.Employee;

public interface ManagerReportService {

    List<Employee> findReportableEmployees(Employee manager);

    AttendanceReport findAttendanceReport(
            Employee manager, Long employeeId, LocalDate fromDate, LocalDate toDate, String category);

    BudgetReport findBudgetReport(Employee manager, Long employeeId, Integer year);
}
