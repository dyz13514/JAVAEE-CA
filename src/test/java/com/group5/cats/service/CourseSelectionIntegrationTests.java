package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.springframework.mock.web.MockMultipartFile;
import com.group5.cats.dto.ApplicationSearch;
import com.group5.cats.dto.CourseFeeClaimForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:course-selection;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "cats.mail.enabled=false",
    "cats.claims.upload-directory=${java.io.tmpdir}/cats-course-selection-audit-claims"
})
@Transactional
class CourseSelectionIntegrationTests {
    @Autowired CourseRepository courses;
    @Autowired CommonCourseRepository catalogue;
    @Autowired CourseApplicationRepository applications;
    @Autowired EmployeeRepository employees;
    @Autowired CourseApplicationService applicationService;
    @Autowired CommonCourseService catalogueService;
    @Autowired CourseService courseService;
    @Autowired EntitlementService entitlementService;
    @Autowired CourseFeeClaimService claimService;
    @Autowired ManagerReportService reportService;
    @Autowired TrainingCalendarService calendarService;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private Course javaCourse(String category) {
        return courses.findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc().stream()
                .filter(course -> course.getTitle().equals("Java") && course.getCategory().getName().equals(category))
                .findFirst().orElseThrow();
    }

    private CourseApplication request(Course course) {
        CourseApplication request = new CourseApplication();
        request.setCourseId(course.getId());
        request.setJustification("Use the skills in our Java project.");
        LocalDate from = LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        request.setFromDate(from);
        request.setToDate(from.plusDays(1));
        return request;
    }

    private Employee employeeWithBudget(double budget) {
        Employee employee = employees.findByUsername("emp1").orElseThrow();
        int year = LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).getYear();
        entitlementService.setEntitlement(employee.getId(), year, 10, budget);
        return employee;
    }

    @Test
    void seededVariantsAndCatalogueReferToTheSameCourses() {
        assertEquals(18, courseService.findAvailableCourses().size());
        assertEquals(3, courseService.findAvailableCourses().stream().filter(option -> option.title().equals("Java")).count());
        assertEquals("AWS", javaCourse("EXTERNAL").getProvider().getName());
        assertEquals("ISS", javaCourse("INTERNAL").getProvider().getName());
        assertEquals(0, javaCourse("INTERNAL").getFee());
        assertEquals(0, catalogue.count());
    }

    @Test
    void fixedPriceCannotBeBypassedBySubmittedFieldsOrBudget() {
        Course selected = javaCourse("EXTERNAL");
        selected.setFee(500);
        Employee employee = employeeWithBudget(200);
        CourseApplication request = request(selected);
        request.setFee(1);
        request.setCourseTitle("Forged title");
        request.setCategory(javaCourse("INTERNAL").getCategory());
        request.setProvider(new TrainingProvider("Forged provider"));
        assertTrue(applicationService.submitApplication(request, employee).contains("Training budget exceeded"));
        assertEquals(500, request.getFee());
        assertEquals("EXTERNAL", request.getCategoryName());
        assertEquals("Java", request.getCourseTitle());
        assertEquals("AWS", request.getProviderName());
        assertNull(request.getId());
    }

    @Test
    void submittedSnapshotsSurviveCataloguePriceAndNameChangesAndRemoval() {
        Course selected = javaCourse("EXTERNAL");
        selected.setFee(300);
        Employee employee = employeeWithBudget(1000);
        CourseApplication request = request(selected);
        assertNull(applicationService.submitApplication(request, employee));
        Long applicationId = request.getId();
        selected.setTitle("Java Advanced");
        selected.setFee(500);
        selected.getProvider().setName("AWS renamed");
        assertNull(catalogueService.addCommonCourse(selected.getId()));
        CommonCourse entry = catalogue.findAll().stream()
                .filter(row -> row.getCourse().getId().equals(selected.getId())).findFirst().orElseThrow();
        assertNull(catalogueService.deleteCommonCourse(entry.getId()));
        entityManager.flush();
        entityManager.clear();
        CourseApplication saved = applications.findById(applicationId).orElseThrow();
        assertEquals("Java", saved.getCourseTitle());
        assertEquals("AWS renamed", saved.getProviderName());
        assertEquals(300, saved.getFee());
        assertTrue(saved.getCourse().isActive());
        assertNotNull(applicationService.submitApplication(request(selected), employee));
        assertEquals(18, courseService.findAvailableCourses().size());
    }

    @Test
    void internalHalfDayAndEditingToAnotherVariantUseTheSelectedCourse() {
        Employee employee = employeeWithBudget(1000);
        CourseApplication internal = request(javaCourse("INTERNAL"));
        internal.setToDate(internal.getFromDate());
        internal.setHalfDay(true);
        internal.setFee(999);
        assertNull(applicationService.submitApplication(internal, employee));
        assertEquals(0, internal.getFee());
        assertEquals(0.5, internal.getTrainingDays());
        Course external = javaCourse("EXTERNAL");
        CourseApplication updated = request(external);
        assertNull(applicationService.updateApplication(internal.getId(), updated, employee));
        assertEquals(external.getId(), internal.getCourseId());
        assertEquals("AWS", internal.getProviderName());
        assertEquals(external.getFee(), internal.getFee());
        assertEquals(2, internal.getTrainingDays());
        assertEquals("UPDATED", internal.getStatus());
    }

    @Test
    void categoryNameSnapshotDoesNotChangeWhenCustomCategoryIsRenamed() {
        Course selected = javaCourse("EXTERNAL");
        Category category = new Category("CUSTOM", "A custom course category", false);
        entityManager.persist(category);
        selected.setCategory(category);
        CourseApplication request = request(selected);
        assertNull(applicationService.submitApplication(request, employeeWithBudget(1000)));
        Long id = request.getId();
        category.setName("RENAMED");
        entityManager.flush(); entityManager.clear();
        assertEquals("CUSTOM", applications.findById(id).orElseThrow().getCategoryName());
    }

    @Test
    void removingAndReaddingCommonMembershipLeavesTheCourseAndItsPriceUnchanged() {
        Course selected = javaCourse("EXTERNAL");
        Long courseId = selected.getId();
        double fee = selected.getFee();
        assertNull(catalogueService.addCommonCourse(courseId));
        CommonCourse entry = catalogue.findAll().get(0);
        assertNull(catalogueService.deleteCommonCourse(entry.getId()));
        entityManager.flush();
        assertNull(catalogueService.addCommonCourse(courseId));
        assertNotNull(catalogueService.addCommonCourse(courseId));
        entityManager.flush(); entityManager.clear();
        assertEquals(18, courses.count());
        assertEquals(1, catalogue.count());
        assertEquals(18, courseService.findAvailableCourses().size());
        assertEquals(courseId, javaCourse("EXTERNAL").getId());
        assertEquals(fee, javaCourse("EXTERNAL").getFee());
    }

    @Test
    void renamedCategoryRemainsSearchableInHistoryAndSelectableInReports() {
        Course selected = javaCourse("EXTERNAL");
        Category category = new Category("CUSTOM", "Custom training", false);
        entityManager.persist(category);
        selected.setCategory(category);
        Employee employee = employeeWithBudget(1000);
        Employee manager = employee.getSupervisor();
        CourseApplication application = request(selected);
        assertNull(applicationService.submitApplication(application, employee));
        assertEquals("Application approved", applicationService.reviewApplication(
                application.getId(), manager, "APPROVE", "Relevant training."));
        LocalDate from = application.getFromDate();
        LocalDate to = application.getToDate();
        category.setName("RENAMED");
        entityManager.flush(); entityManager.clear();
        ApplicationSearch search = new ApplicationSearch();
        search.setKeyword("CUSTOM");
        assertAll(
                () -> assertEquals(1, applicationService.searchEmployeeApplications(employee, false, search).getTotalElements()),
                () -> assertEquals(1, applicationService.searchTeamApplications(manager, search).getTotalElements()),
                () -> assertEquals(1, reportService.findAttendanceReport(manager, employee.getId(), from, to, "RENAMED").getRecordCount()));
    }

    @Test
    void fixedCourseSupportsCompletionClaimResubmissionPaymentAndReports() throws Exception {
        Employee employee = employeeWithBudget(1000);
        Employee manager = employee.getSupervisor();
        Employee admin = employees.findByUsername("admin").orElseThrow();
        Course selected = javaCourse("EXTERNAL");
        selected.setFee(500);
        CourseApplication application = request(selected);
        assertNull(applicationService.submitApplication(application, employee));
        assertEquals("Application approved", applicationService.reviewApplication(
                application.getId(), manager, "APPROVE", "Useful for the project."));
        LocalDate from = application.getFromDate();
        assertEquals(1, calendarService.findApprovedApplicationsByMonth(manager, employee.getId(),
                from.getYear(), from.getMonthValue()).size());
        assertEquals(500, entitlementService.getEntitlementSummary(employee.getId(), from.getYear()).orElseThrow().getOccupiedBudget());
        assertTrue(claimService.findEligibleApplications(employee).isEmpty());

        // Advance the test fixture to after the approved training has ended.
        application.setFromDate(LocalDate.now().minusDays(3));
        application.setToDate(LocalDate.now().minusDays(1));
        entitlementService.setEntitlement(employee.getId(), LocalDate.now().getYear(), 10, 1000);
        assertEquals("Course application completed successfully", applicationService.completeApplication(
                application.getId(), employee, "Applied these skills to our project."));
        assertNull(catalogueService.addCommonCourse(selected.getId()));
        CommonCourse entry = catalogue.findAll().stream()
                .filter(row -> row.getCourse().getId().equals(selected.getId())).findFirst().orElseThrow();
        selected.setFee(200);
        selected.setTitle("Java revised");
        assertNull(catalogueService.deleteCommonCourse(entry.getId()));
        entityManager.flush(); entityManager.clear();
        application = applications.findById(application.getId()).orElseThrow();
        assertEquals(500, application.getFee());
        assertEquals(List.of(application.getId()), claimService.findEligibleApplications(employee).stream().map(CourseApplication::getId).toList());

        CourseFeeClaimForm form = new CourseFeeClaimForm();
        form.setApplicationId(application.getId());
        form.setAmount(new BigDecimal("500.00"));
        form.setPaidPersonally(true);
        MockMultipartFile receipt = new MockMultipartFile("receipt", "receipt.pdf", "application/pdf",
                "%PDF-1.4\nReceipt".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile certificate = new MockMultipartFile("certificate", "certificate.pdf", "application/pdf",
                "%PDF-1.4\nCertificate".getBytes(StandardCharsets.UTF_8));
        assertNull(claimService.submitClaim(null, form, receipt, certificate, employee));
        CourseFeeClaim claim = claimService.findMyClaims(employee).get(0);
        Long receiptId = claim.getReceipt().getId();
        assertEquals(ClaimStatus.PENDING, claim.getStatus());
        assertEquals(1, claimService.findTeamClaims(manager).size());
        assertNotNull(claimService.reviewClaim(claim.getId(), claim.getVersion(), "APPROVE", "Approved", employee));
        assertNull(claimService.reviewClaim(claim.getId(), claim.getVersion(), "REJECT", "Please clarify the receipt.", manager));
        form.setVersion(claim.getVersion());
        assertNull(claimService.submitClaim(claim.getId(), form, null, null, employee));
        assertNull(claimService.reviewClaim(claim.getId(), claim.getVersion(), "APPROVE", "Receipt verified.", manager));
        assertNull(claimService.reimburseClaim(claim.getId(), claim.getVersion(), LocalDate.now(), "AUDIT-PAYMENT", "Paid", admin));
        assertEquals(ClaimStatus.REIMBURSED, claim.getStatus());
        assertEquals(5, claimService.findHistory(claim.getId(), employee).size());
        assertEquals(receiptId, claimService.findViewableAttachment(claim.getId(), receiptId, manager).getId());

        int year = LocalDate.now().getYear();
        var ledger = claimService.getTrainingLedger(employee.getId(), year, employee);
        assertEquals(500, ledger.getOccupiedBudget());
        assertEquals(new BigDecimal("500.00"), ledger.getReimbursedAmount());
        assertEquals(500, ledger.getRemainingBudget());
        assertTrue(TrainingLedgerCsv.export(ledger).contains("AUDIT-PAYMENT"));
        assertEquals(500, reportService.findBudgetReport(manager, employee.getId(), year).getRows().get(0).getClaimedFees());
        var attendance = reportService.findAttendanceReport(manager, employee.getId(),
                application.getFromDate(), application.getToDate(), "EXTERNAL");
        assertEquals("Java", attendance.getRows().get(0).getCourseTitle());
        assertEquals(500, attendance.getRows().get(0).getFee());
    }

    @Test
    void internalCompletionRemainsFreeAndCannotBeClaimed() {
        Employee employee = employeeWithBudget(1000);
        CourseApplication application = request(javaCourse("INTERNAL"));
        assertNull(applicationService.submitApplication(application, employee));
        assertEquals("Application approved", applicationService.reviewApplication(application.getId(),
                employee.getSupervisor(), "APPROVE", "Approved internal training."));
        application.setFromDate(LocalDate.now().minusDays(3));
        application.setToDate(LocalDate.now().minusDays(1));
        assertEquals("Course application completed successfully", applicationService.completeApplication(
                application.getId(), employee, "Useful internal training."));
        assertEquals(0, application.getFee());
        assertTrue(claimService.findEligibleApplications(employee).isEmpty());
        CourseFeeClaimForm form = new CourseFeeClaimForm();
        form.setApplicationId(application.getId());
        form.setAmount(new BigDecimal("200.00"));
        form.setPaidPersonally(true);
        assertEquals("Only External Course and Professional Certification fees can be claimed.",
                claimService.submitClaim(null, form, null, null, employee));
    }

    @Test
    void withdrawalRejectionAndCancellationReleaseTheFixedFeeReservation() {
        Employee employee = employeeWithBudget(1000);
        Employee manager = employee.getSupervisor();
        Course selected = javaCourse("EXTERNAL");
        CourseApplication withdrawn = request(selected);
        int year = withdrawn.getFromDate().getYear();
        assertNull(applicationService.submitApplication(withdrawn, employee));
        assertEquals("Course application withdrawn successfully", applicationService.withdrawApplication(withdrawn.getId(), employee));
        CourseApplication rejected = request(selected);
        assertNull(applicationService.submitApplication(rejected, employee));
        assertEquals("Application rejected", applicationService.reviewApplication(rejected.getId(), manager, "REJECT", "Not required."));
        CourseApplication cancelled = request(selected);
        assertNull(applicationService.submitApplication(cancelled, employee));
        assertEquals("Application approved", applicationService.reviewApplication(cancelled.getId(), manager, "APPROVE", "Approved."));
        assertEquals("Course application cancelled successfully", applicationService.cancelApplication(cancelled.getId(), employee));
        assertEquals(0, entitlementService.getEntitlementSummary(employee.getId(), year).orElseThrow().getOccupiedBudget());
        assertEquals(3, applicationService.findApplicationsByEmployee(employee).size());
        assertTrue(calendarService.findApprovedApplicationsByMonth(manager, employee.getId(), year,
                cancelled.getFromDate().getMonthValue()).isEmpty());
    }
}
