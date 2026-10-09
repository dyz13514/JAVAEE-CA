package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;

class TrainingCalendarServiceTests {

    private CourseApplicationRepository applications;
    private EmployeeRepository employees;
    private TrainingCalendarService service;
    private Employee staff;
    private Employee manager;
    private Employee admin;
    private Employee unrelated;

    @BeforeEach
    void setUp() {
        applications = mock(CourseApplicationRepository.class);
        employees = mock(EmployeeRepository.class);
        service = new TrainingCalendarServiceImpl(applications, employees);

        manager = employee(1L, EmployeeRole.MANAGER);
        staff = employee(2L, EmployeeRole.REGULAR_STAFF);
        admin = employee(3L, EmployeeRole.ADMIN);
        unrelated = employee(4L, EmployeeRole.REGULAR_STAFF);
        staff.setSupervisor(manager);
    }

    @ParameterizedTest
    @EnumSource(EmployeeRole.class)
    void everyRoleCanQueryOnlyItsOwnCoursesByDefault(EmployeeRole role) {
        staff.setRole(role);
        CourseApplication course = new CourseApplication();
        course.setEmployee(staff);
        when(applications.findApprovedApplicationsForCalendar(
                staff.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(course));

        List<CourseApplication> result = service.findApprovedApplicationsByMonth(
                staff, staff.getId(), 2026, 10);

        assertEquals(List.of(course), result);
        verify(applications).findApprovedApplicationsForCalendar(
                staff.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @Test
    void employeeCannotListPeopleOrQueryAnotherEmployee() {
        assertThrows(SecurityException.class,
                () -> service.findEmployeesForCalendar(staff));
        assertThrows(SecurityException.class,
                () -> service.findApprovedApplicationsByMonth(staff, unrelated.getId(), 2026, 10));
        verifyNoInteractions(applications);
        verify(employees, never()).findAll();
    }

    @Test
    void managerListUsesOnlyDirectReports() {
        when(employees.findBySupervisor_Id(manager.getId())).thenReturn(List.of(staff));

        assertEquals(List.of(staff), service.findEmployeesForCalendar(manager));
        verify(employees, never()).findAll();
    }

    @Test
    void managerCanViewOwnCalendarAndDirectReport() {
        assertSame(manager, service.findCalendarEmployee(manager, manager.getId()));
        assertSame(staff, service.findCalendarEmployee(manager, staff.getId()));
        service.findApprovedApplicationsByMonth(manager, staff.getId(), 2026, 10);
        verify(applications).findApprovedApplicationsForCalendar(
                staff.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @Test
    void managerCannotViewUnrelatedOrIndirectEmployees() {
        assertThrows(SecurityException.class,
                () -> service.findCalendarEmployee(manager, unrelated.getId()));

        unrelated.setSupervisor(staff);
        assertThrows(SecurityException.class,
                () -> service.findApprovedApplicationsByMonth(manager, unrelated.getId(), 2026, 10));
        verifyNoInteractions(applications);
    }

    @Test
    void reassignedEmployeeIsNoLongerVisibleToOldManager() {
        assertSame(staff, service.findCalendarEmployee(manager, staff.getId()));
        staff.setSupervisor(admin);
        assertThrows(SecurityException.class,
                () -> service.findApprovedApplicationsByMonth(manager, staff.getId(), 2026, 10));
        verifyNoInteractions(applications);
    }

    @Test
    void adminCanListAllRolesAndViewAnyEmployee() {
        List<Employee> allEmployees = List.of(staff, manager, admin, unrelated);
        when(employees.findAll()).thenReturn(allEmployees);

        assertEquals(allEmployees, service.findEmployeesForCalendar(admin));
        for (Employee employee : allEmployees) {
            assertSame(employee, service.findCalendarEmployee(admin, employee.getId()));
        }
    }

    @Test
    void missingEmployeeDoesNotReturnAnyCourseInformation() {
        assertThrows(SecurityException.class,
                () -> service.findApprovedApplicationsByMonth(admin, 999L, 2026, 10));
        verifyNoInteractions(applications);
    }

    @ParameterizedTest
    @CsvSource({"2024,2,29", "2026,2,28", "2026,4,30", "9999,12,31"})
    void queryUsesCorrectMonthBoundaries(int year, int month, int lastDay) {
        service.findApprovedApplicationsByMonth(staff, staff.getId(), year, month);
        verify(applications).findApprovedApplicationsForCalendar(
                staff.getId(), LocalDate.of(year, month, 1), LocalDate.of(year, month, lastDay));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, 10000})
    void invalidYearDoesNotQueryCourses(Integer year) {
        assertThrows(IllegalArgumentException.class,
                () -> service.findApprovedApplicationsByMonth(staff, staff.getId(), year, 10));
        verifyNoInteractions(applications);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, 13})
    void invalidMonthDoesNotQueryCourses(Integer month) {
        assertThrows(IllegalArgumentException.class,
                () -> service.findApprovedApplicationsByMonth(staff, staff.getId(), 2026, month));
        verifyNoInteractions(applications);
    }

    private Employee employee(Long id, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setRole(role);
        when(employees.findById(id)).thenReturn(Optional.of(employee));
        return employee;
    }
}
