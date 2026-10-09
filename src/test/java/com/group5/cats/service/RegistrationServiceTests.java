package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import com.group5.cats.dto.RegistrationForm;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class RegistrationServiceTests {
    private RegistrationRequestRepository registrations;
    private EmployeeRepository employees;
    private EntitlementService entitlements;
    private RegistrationService service;
    private Employee admin;
    private RegistrationForm form;
    private RegistrationRequest request;
    @BeforeEach void setup() {
        registrations = mock(RegistrationRequestRepository.class);
        employees = mock(EmployeeRepository.class);
        entitlements = mock(EntitlementService.class);
        service = new RegistrationService(registrations, employees, entitlements);
        admin = new Employee(); admin.setRole(EmployeeRole.ADMIN);
        form = new RegistrationForm(); form.setName("New Staff"); form.setUsername("new.staff");
        form.setPassword("test-password"); form.setConfirmPassword("test-password");
        request = new RegistrationRequest(); request.setId(1L); request.setUsername("new.staff"); request.setName("New Staff"); request.setPasswordHash(PasswordSupport.encode("test-password"));
        when(registrations.findForReview(1L)).thenReturn(Optional.of(request));
    }
    @Test void submissionStoresOnlyAHashAndCreatesNoEmployee() {
        service.submit(form);
        var captured = org.mockito.ArgumentCaptor.forClass(RegistrationRequest.class);
        verify(registrations).saveAndFlush(captured.capture());
        assertTrue(PasswordSupport.matches(form.getPassword(), captured.getValue().getPasswordHash()));
        assertNotEquals(form.getPassword(), captured.getValue().getPasswordHash());
        verify(employees, never()).saveAndFlush(any());
    }
    @Test void passwordsAndUsernamesAreValidatedBeforeSaving() {
        form.setConfirmPassword("wrong-password"); assertThrows(IllegalArgumentException.class, () -> service.submit(form));
        form.setConfirmPassword("test-password"); form.setUsername("bad username"); assertThrows(IllegalArgumentException.class, () -> service.submit(form));
        form.setUsername("new.staff"); when(employees.findByUsername("new.staff")).thenReturn(Optional.of(admin));
        assertThrows(IllegalArgumentException.class, () -> service.submit(form));
        verify(registrations, never()).saveAndFlush(any());
    }
    @Test void pendingRequestsCannotBeOverwrittenOrReviewedTwice() {
        when(registrations.findByUsername("new.staff")).thenReturn(Optional.of(request));
        assertThrows(IllegalArgumentException.class, () -> service.submit(form));
        request.setStatus("APPROVED");
        assertThrows(IllegalArgumentException.class, () -> service.approve(1L, EmployeeDesignation.PROFESSIONAL, 2L, admin));
        verify(employees, never()).saveAndFlush(any());
    }
    @Test void approvalCreatesStaffAndAllowanceAndRemovesRequestCredentials() {
        Employee manager = new Employee(); manager.setRole(EmployeeRole.MANAGER);
        when(employees.findById(2L)).thenReturn(Optional.of(manager));
        when(employees.saveAndFlush(any())).thenAnswer(invocation -> { Employee employee = invocation.getArgument(0); employee.setId(8L); return employee; });
        service.approve(1L, EmployeeDesignation.PROFESSIONAL, 2L, admin);
        var captured = org.mockito.ArgumentCaptor.forClass(Employee.class); verify(employees).saveAndFlush(captured.capture());
        assertEquals(EmployeeRole.REGULAR_STAFF, captured.getValue().getRole());
        assertSame(manager, captured.getValue().getSupervisor());
        assertTrue(PasswordSupport.matches("test-password", captured.getValue().getPassword()));
        verify(entitlements).createDefaultEntitlement(eq(8L), anyInt());
        assertEquals("APPROVED", request.getStatus()); assertNull(request.getPasswordHash());
    }
    @Test void staffCannotApproveAndNonManagersCannotBeAssigned() {
        Employee staff = new Employee(); staff.setRole(EmployeeRole.REGULAR_STAFF);
        assertThrows(IllegalArgumentException.class, () -> service.approve(1L, EmployeeDesignation.PROFESSIONAL, 2L, staff));
        when(employees.findById(2L)).thenReturn(Optional.of(staff));
        assertThrows(IllegalArgumentException.class, () -> service.approve(1L, EmployeeDesignation.PROFESSIONAL, 2L, admin));
        assertThrows(IllegalArgumentException.class, () -> service.approve(1L, null, 2L, admin));
        verify(employees, never()).saveAndFlush(any()); verifyNoInteractions(entitlements);
    }
    @Test void rejectionRequiresReasonAndAllowsAReplacementRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.reject(1L, " ", admin));
        service.reject(1L, "Confirm staff details", admin);
        assertEquals("REJECTED", request.getStatus()); assertNull(request.getPasswordHash());
        when(registrations.findByUsername("new.staff")).thenReturn(Optional.of(request));
        service.submit(form); assertEquals("PENDING", request.getStatus()); assertNull(request.getReviewReason());
    }
    @Test void hashedCredentialsCannotBeUsedAsTheirOwnPassword() {
        String hash = PasswordSupport.encode("test-password");
        assertFalse(PasswordSupport.matches(hash, hash)); assertFalse(PasswordSupport.matches("wrong", hash));
        assertTrue(PasswordSupport.matches("old-password", "old-password"));
        assertFalse(PasswordSupport.matches("x".repeat(100), hash));
    }
}
