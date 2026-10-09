package com.group5.cats.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.AttendanceReportRow;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.dto.BudgetReportRow;
import com.group5.cats.dto.CourseCategory;
import com.group5.cats.dto.CourseFeeDetail;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EntitlementSummary;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;

@Service
public class ManagerReportServiceImpl implements ManagerReportService {

    /**
     * A course only counts as confirmed attendance once a manager has approved it.
     * Pending, rejected, withdrawn and cancelled applications are excluded, and a
     * completed course keeps counting because the training took place.
     */
    private static final List<String> CONFIRMED_ATTENDANCE_STATUSES =
            List.of("APPROVED", "COMPLETED");

    private static final Comparator<Employee> BY_NAME = Comparator
            .comparing(Employee::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(Employee::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final EmployeeRepository employeeRepository;
    private final CourseApplicationRepository courseApplicationRepository;
    private final EntitlementService entitlementService;

    public ManagerReportServiceImpl(
            EmployeeRepository employeeRepository,
            CourseApplicationRepository courseApplicationRepository,
            EntitlementService entitlementService) {

        this.employeeRepository = employeeRepository;
        this.courseApplicationRepository = courseApplicationRepository;
        this.entitlementService = entitlementService;
    }

    @Override
    public List<Employee> findReportableEmployees(Employee manager) {
        List<Employee> reports = new ArrayList<>(
                employeeRepository.findBySupervisor_Id(manager.getId()));
        reports.sort(BY_NAME);
        return reports;
    }

    @Override
    public AttendanceReport findAttendanceReport(
            Employee manager,
            Long employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            String category) {

        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException(
                    "Please provide both a start and an end date for the reporting period.");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "The start date must not be after the end date.");
        }
        String selectedCategory = category == null ? CourseCategory.ALL : category;
        if (!CourseCategory.isValidFilter(selectedCategory)) {
            throw new IllegalArgumentException("Please select a valid course category.");
        }

        AttendanceReport report = new AttendanceReport();
        report.setFromDate(fromDate);
        report.setToDate(toDate);
        report.setCategory(selectedCategory);

        List<Employee> employees = resolveEmployees(manager, employeeId);
        if (employees.isEmpty()) {
            return report;
        }

        List<CourseApplication> applications = new ArrayList<>(
                courseApplicationRepository.findByEmployeeIn(employees));
        applications.removeIf(application -> !countsAsConfirmedAttendance(application, fromDate, toDate, selectedCategory));
        applications.sort(attendanceOrder());

        List<AttendanceReportRow> rows = new ArrayList<>();
        for (CourseApplication application : applications) {
            rows.add(toAttendanceRow(application));
        }
        report.setRows(rows);
        return report;
    }

    @Override
    public BudgetReport findBudgetReport(Employee manager, Long employeeId, Integer year) {

        if (year == null || year < 1 || year > 9999) {
            throw new IllegalArgumentException("EntitlementYear must be between 1 and 9999.");
        }

        BudgetReport report = new BudgetReport();
        report.setYear(year);

        List<Employee> employees = resolveEmployees(manager, employeeId);
        List<BudgetReportRow> rows = new ArrayList<>();
        for (Employee employee : employees) {
            rows.add(toBudgetRow(employee, year));
        }
        report.setRows(rows);
        return report;
    }

    /**
     * Reporting always runs over the direct reports. Selecting the manager's own
     * account or anybody else's employee is rejected instead of being ignored.
     */
    private List<Employee> resolveEmployees(Employee manager, Long employeeId) {

        List<Employee> reports = findReportableEmployees(manager);

        if (employeeId == null) {
            return reports;
        }
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Please select a valid employee.");
        }
        for (Employee report : reports) {
            if (employeeId.equals(report.getId())) {
                return List.of(report);
            }
        }
        throw new SecurityException("You are not allowed to report on this employee.");
    }

    /**
     * The reporting period is inclusive and a course is included when its training
     * period overlaps it, so a course that starts before the period or ends after
     * it still appears on every selected period it touches.
     */
    private boolean countsAsConfirmedAttendance(
            CourseApplication application,
            LocalDate fromDate,
            LocalDate toDate,
            String category) {

        if (!CONFIRMED_ATTENDANCE_STATUSES.contains(application.getStatus())) {
            return false;
        }
        if (application.getFromDate() == null || application.getToDate() == null) {
            return false;
        }
        if (application.getFromDate().isAfter(toDate) || application.getToDate().isBefore(fromDate)) {
            return false;
        }
        return CourseCategory.ALL.equals(category) || category.equals(application.getCategory());
    }

    private Comparator<CourseApplication> attendanceOrder() {
        return Comparator
                .comparing(
                        (CourseApplication application) -> application.getEmployee() == null
                                ? null : application.getEmployee().getName(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(CourseApplication::getFromDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CourseApplication::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private AttendanceReportRow toAttendanceRow(CourseApplication application) {
        AttendanceReportRow row = new AttendanceReportRow();
        row.setEmployeeName(application.getEmployee() == null ? null : application.getEmployee().getName());
        row.setCourseTitle(application.getCourseTitle());
        row.setCategory(application.getCategory());
        row.setFromDate(application.getFromDate());
        row.setToDate(application.getToDate());
        row.setTrainingDays(application.getTrainingDays());
        row.setStatus(application.getStatus());
        row.setFee(application.getFee());
        return row;
    }

    /**
     * Budget totals are taken from {@link EntitlementService} so the report cannot
     * drift from the entitlement page. An employee without an entitlement row for
     * the year is reported as not configured instead of failing.
     */
    private BudgetReportRow toBudgetRow(Employee employee, Integer year) {

        BudgetReportRow row = new BudgetReportRow();
        row.setEmployeeId(employee.getId());
        row.setEmployeeName(employee.getName());
        row.setEntitlementYear(year);

        Optional<EntitlementSummary> summary = entitlementService.getEntitlementSummary(employee.getId(), year);
        if (summary.isEmpty()) {
            return row;
        }

        EntitlementSummary entitlement = summary.get();
        row.setHasEntitlement(true);
        row.setTrainingBudget(entitlement.getTrainingBudget());
        row.setClaimedFees(entitlement.getOccupiedBudget());
        row.setRemainingBudget(entitlement.getRemainingBudget());
        row.setUtilisationPercent(utilisationPercent(
                entitlement.getOccupiedBudget(), entitlement.getTrainingBudget()));

        List<CourseFeeDetail> details = new ArrayList<>();
        for (CourseApplication application : entitlementService.findOccupyingApplications(employee.getId(), year)) {
            details.add(toFeeDetail(application));
        }
        details.sort(Comparator
                .comparing(CourseFeeDetail::getFromDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CourseFeeDetail::getCourseTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        row.setDetails(details);
        return row;
    }

    private CourseFeeDetail toFeeDetail(CourseApplication application) {
        CourseFeeDetail detail = new CourseFeeDetail();
        detail.setCourseTitle(application.getCourseTitle());
        detail.setCategory(application.getCategory());
        detail.setFromDate(application.getFromDate());
        detail.setToDate(application.getToDate());
        detail.setStatus(application.getStatus());
        detail.setFee(application.getFee());
        detail.setCountsTowardsBudget(CourseCategory.isBudgetRelevant(application.getCategory()));
        return detail;
    }

    private double utilisationPercent(double claimedFees, double trainingBudget) {
        if (!Double.isFinite(trainingBudget) || trainingBudget <= 0) {
            return 0.0;
        }
        return claimedFees / trainingBudget * 100.0;
    }
}
