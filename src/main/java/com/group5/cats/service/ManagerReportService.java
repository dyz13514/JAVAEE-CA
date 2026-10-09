package com.group5.cats.service;

import java.time.LocalDate;
import java.util.List;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.model.Employee;

/**
 * Reporting over a manager's direct reports.
 *
 * <p>Every method scopes the query to the manager's own direct reports; a
 * selection outside that team raises {@link SecurityException} rather than
 * returning data.
 */
public interface ManagerReportService {

    /** Direct reports the manager is allowed to report on, ordered by name. */
    List<Employee> findReportableEmployees(Employee manager);

    /**
     * Confirmed attendance (approved and completed applications) whose training
     * period overlaps the reporting period.
     *
     * @param employeeId one direct report, or null for every direct report
     * @param category   one of {@link com.group5.cats.dto.CourseCategory} filter values
     */
    AttendanceReport findAttendanceReport(
            Employee manager, Long employeeId, LocalDate fromDate, LocalDate toDate, String category);

    /**
     * Course fee claims and budget utilisation for one reporting year, using the
     * annual entitlement calculation.
     *
     * @param employeeId one direct report, or null for every direct report
     */
    BudgetReport findBudgetReport(Employee manager, Long employeeId, Integer year);
}
