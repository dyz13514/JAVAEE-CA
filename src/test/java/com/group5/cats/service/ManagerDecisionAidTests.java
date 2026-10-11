package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.group5.cats.dto.ManagerDecisionAid;
import com.group5.cats.model.AnnualEntitlement;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.CategoryRepository;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.repository.PublicHolidayRepository;
import com.group5.cats.repository.TrainingProviderRepository;

class ManagerDecisionAidTests {

    private CourseApplicationServiceImpl service;
    private EntitlementService entitlementService;
    private CourseApplicationRepository applications;
    private EmployeeRepository employees;

    private Employee manager;
    private Employee applicant;
    private Employee colleague;

    @BeforeEach
    void setUp() {
        applications = mock(CourseApplicationRepository.class);
        employees = mock(EmployeeRepository.class);
        entitlementService = mock(EntitlementService.class);
        service = new CourseApplicationServiceImpl(applications, employees,
                entitlementService, mock(PublicHolidayRepository.class),
                mock(NotificationService.class), mock(CategoryRepository.class),
                mock(TrainingProviderRepository.class));

        manager = employee(1L, EmployeeRole.MANAGER);
        applicant = employee(2L, EmployeeRole.REGULAR_STAFF);
        applicant.setSupervisor(manager);
        colleague = employee(3L, EmployeeRole.REGULAR_STAFF);
        colleague.setSupervisor(manager);
    }

    @Test
    void aidSummarisesTheApplicantsAllowanceUsageForTheYear() {
        CourseApplication pending = application(applicant, "APPLIED",
                LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21));
        AnnualEntitlement entitlement = new AnnualEntitlement();
        entitlement.setTrainingDaysLimit(15);
        entitlement.setTrainingBudget(2000);
        when(entitlementService.findEntitlement(2L, 2026)).thenReturn(Optional.of(entitlement));
        when(entitlementService.getOccupiedTrainingDays(2L, 2026)).thenReturn(3.5);
        when(entitlementService.getOccupiedTrainingBudget(2L, 2026)).thenReturn(800.0);

        ManagerDecisionAid aid = service.buildDecisionAid(pending, manager);

        assertTrue(aid.isEntitlementPresent());
        assertEquals(3.5, aid.getUsedDays());
        assertEquals(15.0, aid.getLimitDays());
        assertEquals(800.0, aid.getUsedBudget());
        assertEquals(2000.0, aid.getBudgetLimit());
        assertTrue(aid.getOverlappingApproved().isEmpty());
    }

    @Test
    void missingEntitlementIsFlaggedInsteadOfGuessed() {
        CourseApplication pending = application(applicant, "APPLIED",
                LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21));
        when(entitlementService.findEntitlement(2L, 2026)).thenReturn(Optional.empty());

        ManagerDecisionAid aid = service.buildDecisionAid(pending, manager);

        assertFalse(aid.isEntitlementPresent());
        assertEquals(0.0, aid.getLimitDays());
    }

    @Test
    void aidListsOnlyOtherSubordinatesApprovedCoursesInTheSamePeriod() {
        LocalDate from = LocalDate.of(2026, 10, 19);
        LocalDate to = LocalDate.of(2026, 10, 21);
        CourseApplication pending = application(applicant, "APPLIED", from, to);
        when(entitlementService.findEntitlement(2L, 2026)).thenReturn(Optional.empty());
        CourseApplication approvedOverlap = application(colleague, "APPROVED",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23));
        CourseApplication approvedOutside = application(colleague, "APPROVED",
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4));
        CourseApplication pendingOverlap = application(colleague, "APPLIED",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23));
        CourseApplication ownApproved = application(applicant, "APPROVED", from, to);
        List<Employee> team = List.of(applicant, colleague);
        when(employees.findBySupervisor_Id(1L)).thenReturn(team);
        when(applications.findByEmployeeIn(team))
                .thenReturn(List.of(approvedOverlap, approvedOutside, pendingOverlap, ownApproved));

        ManagerDecisionAid aid = service.buildDecisionAid(pending, manager);

        assertEquals(List.of(approvedOverlap), aid.getOverlappingApproved());
    }

    private CourseApplication application(Employee owner, String status, LocalDate from, LocalDate to) {
        CourseApplication application = new CourseApplication();
        application.setEmployee(owner);
        application.setStatus(status);
        application.setFromDate(from);
        application.setToDate(to);
        return application;
    }

    private Employee employee(Long id, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName("Employee " + id);
        employee.setRole(role);
        return employee;
    }
}
