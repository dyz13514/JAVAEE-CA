package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.dto.RegistrationForm;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

@SpringBootTest
@Transactional
class RegistrationIntegrationTests {
    @Autowired RegistrationService service;
    @Autowired EmployeeRepository employees;
    @Autowired RegistrationRequestRepository registrations;
    @Autowired AuthService auth;
    @Autowired EntitlementService entitlements;
    @Autowired EmployeeService employeeService;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test void removingAReviewerKeepsRegistrationHistory() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Employee reviewer = employees.saveAndFlush(new Employee("reviewer_" + suffix,
                PasswordSupport.encode("reviewer-test-password"), "Review Test", null,
                EmployeeDesignation.PROFESSIONAL, EmployeeRole.ADMIN));
        Employee administrator = employees.findByRole(EmployeeRole.ADMIN).stream()
                .filter(employee -> !employee.getId().equals(reviewer.getId())).findFirst().orElseThrow();
        RegistrationForm form = new RegistrationForm();
        form.setUsername("reject_" + suffix); form.setName("Rejected Test");
        form.setPassword("registration-test-password"); form.setConfirmPassword(form.getPassword());
        service.submit(form);
        RegistrationRequest request = registrations.findByUsername(form.getUsername()).orElseThrow();
        service.reject(request.getId(), "Staff record could not be verified.", reviewer);
        assertNull(employeeService.deleteEmployee(reviewer.getId(), administrator.getId()));
        entityManager.flush();
        entityManager.clear();
        assertTrue(employees.findById(reviewer.getId()).isEmpty());
        RegistrationRequest history = registrations.findById(request.getId()).orElseThrow();
        assertEquals("REJECTED", history.getStatus());
        assertEquals("Staff record could not be verified.", history.getReviewReason());
        assertNull(history.getReviewedBy());
    }

    @Test void approvedRegistrationCanLoginAndHasTheCorrectSupervisorAndAllowance() {
        RegistrationForm form = new RegistrationForm();
        form.setUsername("regtest_" + UUID.randomUUID().toString().substring(0, 8));
        form.setName("Registration Test"); form.setPassword("registration-test-password"); form.setConfirmPassword(form.getPassword());
        service.submit(form);
        assertTrue(auth.login(form.getUsername(), form.getPassword()).isEmpty());
        RegistrationRequest request = registrations.findByUsername(form.getUsername()).orElseThrow();
        Employee manager = employees.findByRole(EmployeeRole.MANAGER).get(0);
        service.approve(request.getId(), EmployeeDesignation.PROFESSIONAL, manager.getId(), employees.findByRole(EmployeeRole.ADMIN).get(0));
        Employee employee = auth.login(form.getUsername(), form.getPassword()).orElseThrow();
        assertEquals(EmployeeRole.REGULAR_STAFF, employee.getRole());
        assertEquals(manager.getId(), employee.getSupervisor().getId());
        assertTrue(entitlements.findEntitlement(employee.getId(), LocalDate.now().getYear()).isPresent());
        assertNull(registrations.findByUsername(form.getUsername()).orElseThrow().getPasswordHash());
    }
}
