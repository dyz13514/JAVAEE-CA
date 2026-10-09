package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.group5.cats.dto.EmployeeForm;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class EmployeeEmailTests {
    @Test
    void storesTrimmedEmailRejectsInvalidAddressAndPreservesLegacyBlankAccounts() {
        var employees = mock(EmployeeRepository.class);
        var service = new EmployeeServiceImpl(employees, mock(CourseApplicationRepository.class),
                mock(AnnualEntitlementRepository.class), mock(NotificationOutboxRepository.class));
        EmployeeForm form = new EmployeeForm();
        form.setName("Ben");
        form.setUsername("ben");
        form.setPassword("test");
        form.setRole(EmployeeRole.REGULAR_STAFF);
        form.setDesignation(EmployeeDesignation.PROFESSIONAL);
        when(employees.findByUsername("ben")).thenReturn(Optional.empty());
        form.setEmail(" ben@example.test ");
        assertNull(service.createEmployee(form));
        var employee = ArgumentCaptor.forClass(Employee.class);
        verify(employees).save(employee.capture());
        assertEquals("ben@example.test", employee.getValue().getEmail());
        clearInvocations(employees);
        for (String address : new String[] {"bad", "bad @example.test", "bad\r\n@example.test", "a".repeat(255) + "@example.test"}) {
            form.setEmail(address);
            assertNotNull(service.createEmployee(form));
        }
        verify(employees, never()).save(any());
        form.setEmail("");
        assertNull(service.createEmployee(form));
    }
}
