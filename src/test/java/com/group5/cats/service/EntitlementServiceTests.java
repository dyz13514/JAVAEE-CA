package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.group5.cats.model.Employee;
import com.group5.cats.repository.*;

class EntitlementServiceTests {
    private AnnualEntitlementRepository repository;
    private EmployeeRepository employees;
    private EntitlementServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AnnualEntitlementRepository.class);
        employees = mock(EmployeeRepository.class);
        service = new EntitlementServiceImpl(repository, employees, mock(CourseApplicationRepository.class));
        Employee employee = new Employee();
        employee.setId(1L);
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
    }

    static Stream<Double> invalidAmounts() {
        return Stream.of(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void invalidLimitsNeverReachPersistence(double amount) {
        assertThrows(IllegalArgumentException.class, () -> service.setEntitlement(1L, 2026, amount, 2000));
        assertThrows(IllegalArgumentException.class, () -> service.setEntitlement(1L, 2026, 10, amount));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 10000})
    void yearsOutsideSupportedCalendarAreRejected(int year) {
        assertThrows(IllegalArgumentException.class, () -> service.findEntitlement(1L, year));
        verifyNoInteractions(repository);
    }

    @Test
    void validZeroLimitsRemainAllowed() {
        Employee employee = new Employee();
        employee.setId(1L);
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(repository.findByEmployeeIdAndEntitlementYear(1L, 2026)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.setEntitlement(1L, 2026, 0, 0));
        verify(repository).save(any());
    }
}
