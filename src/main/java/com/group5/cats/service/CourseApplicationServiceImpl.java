package com.group5.cats.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.group5.cats.dto.ApplicationSearch;
import com.group5.cats.dto.ManagerDecisionAid;
import com.group5.cats.model.EmployeeRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.NotificationType;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Category;
import com.group5.cats.model.Course;
import com.group5.cats.dto.CourseCategory;
import com.group5.cats.repository.CourseRepository;
import com.group5.cats.model.Employee;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.repository.PublicHolidayRepository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.time.DayOfWeek;
import com.group5.cats.model.AnnualEntitlement;

@Service
public class CourseApplicationServiceImpl implements CourseApplicationService {
    // String columns currently use the JPA default maximum length.
    private static final int TEXT_LIMIT = 255;
    private final CourseApplicationRepository courseApplicationRepository;
    private final EmployeeRepository employeeRepository;
    private final EntitlementService entitlementService;
    private final PublicHolidayRepository publicHolidayRepository;
    private final NotificationService notificationService;
    private final CourseRepository courseRepository;

    public CourseApplicationServiceImpl(CourseApplicationRepository courseApplicationRepository,
            EmployeeRepository employeeRepository, EntitlementService entitlementService,
            PublicHolidayRepository publicHolidayRepository, NotificationService notificationService,
            CourseRepository courseRepository) {
        this.courseApplicationRepository = courseApplicationRepository;
        this.employeeRepository = employeeRepository;
        this.entitlementService = entitlementService;
        this.publicHolidayRepository = publicHolidayRepository;
        this.notificationService = notificationService;
        this.courseRepository = courseRepository;
    }

    @Override
    public Page<CourseApplication> searchEmployeeApplications(Employee employee,
            boolean currentYear, ApplicationSearch search) {
        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        Sort order = Sort.by(Sort.Direction.DESC, "id");
        PageRequest request = PageRequest.of(search.getPage() - 1, search.getSize(), order);
        Page<CourseApplication> result = courseApplicationRepository.searchEmployeeApplications(
                employee.getId(), currentYear, yearStart, yearStart.plusYears(1), search.getKeyword(), request);
        // A bookmarked page may no longer exist after records or filters change.
        if (search.getPage() > Math.max(1, result.getTotalPages())) {
            search.setPage(Math.max(1, result.getTotalPages()));
            request = PageRequest.of(search.getPage() - 1, search.getSize(), order);
            result = courseApplicationRepository.searchEmployeeApplications(employee.getId(),
                    currentYear, yearStart, yearStart.plusYears(1), search.getKeyword(), request);
        }
        return result;
    }

    @Override
    public Page<CourseApplication> searchTeamApplications(Employee manager, ApplicationSearch search) {
        if (manager.getRole() != EmployeeRole.MANAGER) {
            throw new SecurityException("Only managers can view team applications.");
        }
        Sort order = Sort.by("employee.name", "employee.id").and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest request = PageRequest.of(search.getPage() - 1, search.getSize(), order);
        Page<CourseApplication> result = searchTeamPage(manager, search, request);
        if (search.getPage() > Math.max(1, result.getTotalPages())) {
            search.setPage(Math.max(1, result.getTotalPages()));
            request = PageRequest.of(search.getPage() - 1, search.getSize(), order);
            result = searchTeamPage(manager, search, request);
        }
        return result;
    }

    private Page<CourseApplication> searchTeamPage(Employee manager, ApplicationSearch search, PageRequest request) {
        if ("all".equals(search.getReviewState())) {
            return courseApplicationRepository.searchTeamApplications(manager.getId(), search.getKeyword(), request);
        }
        return courseApplicationRepository.searchTeamApplicationsByReviewState(
                manager.getId(), search.getKeyword(), search.getReviewState(), request);
    }

    @Override
    @Transactional
    public String submitApplication(CourseApplication application, Employee employee) {
        if (application.getId() != null) {
            return "New applications must not contain an existing application ID.";
        }
        // Session objects may predate changes to the employee or supervisor email address.
        application.setEmployee(employeeRepository.findById(employee.getId()).orElse(employee));
        if (application.getEmployee().getRole() == EmployeeRole.ADMIN) {
            return "Administrators manage courses and cannot submit course applications.";
        }
        String supervisorError = validateSupervisor(application.getEmployee());
        if (supervisorError != null) return supervisorError;
        String error = selectCourse(application);
        if (error != null) return error;
        error = validateBasicRules(application);
        if (error != null) {
            return error;
        }
        String overlapError = validateOverlap(application, employee, null);
        if (overlapError != null) {
            return overlapError;
        }

        application.setTrainingDays(computeTrainingDays(application));

        String entitlementError = validateEntitlement(application, employee, null);
        if (entitlementError != null) {
            return entitlementError;
        }
        application.setStatus("APPLIED");
        courseApplicationRepository.save(application);
        notificationService.createNotification(application, NotificationType.APPLICATION_SUBMITTED);
        return null;
    }

    private String selectCourse(CourseApplication application) {
        Long courseId = application.getCourseId();
        if (courseId == null || courseId <= 0) {
            return "Please select a course, category and training provider.";
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || !course.isActive()) {
            return "Selected course is no longer available. Please select another course.";
        }
        if ("INTERNAL".equals(course.getCategory().getName()) && course.getFee() != 0) {
            return "Internal training must have a zero course fee. Please contact the administrator.";
        }
        application.setCourse(course);
        application.setCourseTitle(course.getTitle());
        application.setCategory(course.getCategory());
        application.setCategoryNameSnapshot(course.getCategory().getName());
        application.setProvider(course.getProvider());
        application.setProviderId(course.getProvider().getId());
        application.setFee(course.getFee());
        return null;
    }

    private String validateBasicRules(CourseApplication application) {
        String error = validateText(application.getCourseTitle(), "Course title", true);
        if (error != null) return error;
        error = validateText(application.getJustification(), "Justification", true);
        if (error != null) return error;
        error = validateText(application.getDissemination(), "Work dissemination", false);
        if (error != null) return error;

        Category category = application.getCategory();
        double fee = application.getFee();
        if (!Double.isFinite(fee) || fee < 0) {
            return "Course fee must be a valid number greater than or equal to zero.";
        }

        LocalDate from = application.getFromDate();
        LocalDate to = application.getToDate();
        if (from == null || to == null) {
            return "Course start and end dates must be provided";
        }
        if (from.getYear() < 1 || from.getYear() > 9999 || to.getYear() < 1 || to.getYear() > 9999) {
            return "Course dates must be within years 1 to 9999.";
        }
        if (Boolean.TRUE.equals(application.getHalfDay())) {
            if (!category.isHalfDayAllowed()) {
                return "This category does not allow half-day sessions.";
            }
            if (to.isBefore(from)) {
                return "Course end date must not be before start date.";
            }
        } else if (!from.isBefore(to)) {
            return "Course end date must be after start date for a full-day course.";
        }

        if (!from.isAfter(LocalDate.now())) {
            return "From date must start in the future";
        }
        if (from.getDayOfWeek() == DayOfWeek.SATURDAY || from.getDayOfWeek() == DayOfWeek.SUNDAY || isPublicHoliDays(from)) {
            return "'From' date must be a working day.";
        }
        if (to.getDayOfWeek() == DayOfWeek.SATURDAY || to.getDayOfWeek() == DayOfWeek.SUNDAY || isPublicHoliDays(to)) {
            return "'To' date must be a working day.";
        }
        application.setCourseTitle(application.getCourseTitle().strip());
        application.setJustification(application.getJustification().strip());
        if (application.getDissemination() != null) application.setDissemination(application.getDissemination().strip());
        return null;
    }

    private String validateText(String value, String label, boolean required) {
        if (required && (value == null || value.isBlank())) {
            return label + " is required.";
        }
        if (value != null && value.strip().length() > TEXT_LIMIT) {
            return label + " must not exceed " + TEXT_LIMIT + " characters.";
        }
        return null;
    }

    private String validateEntitlement(CourseApplication application, Employee employee, Long excludeId) {
        int year = application.getFromDate().getYear();
        Optional<AnnualEntitlement> entitlement = entitlementService.findEntitlement(employee.getId(), year);
        if (entitlement.isEmpty()) {
            return "No training entitlement configured for " + year + ". Please contact the administrator.";
        }
        double limit = entitlement.get().getTrainingDaysLimit();
        double budget = entitlement.get().getTrainingBudget();
        if (!Double.isFinite(limit) || limit < 0 || !Double.isFinite(budget) || budget < 0) {
            return "Your training entitlement is invalid. Please contact the administrator.";
        }
        double usedDays = 0;
        double usedFees = 0;
        for (CourseApplication existing : courseApplicationRepository.findByEmployee(employee)) {
            if (excludeId != null && excludeId.equals(existing.getId())) {
                continue;
            }
            String status = existing.getStatus();
            if (existing.getFromDate() != null && existing.getFromDate().getYear() == year && ("APPROVED".equals(status)
                    || "COMPLETED".equals(status) || "APPLIED".equals(status) || "UPDATED".equals(status))) {
                usedDays += existing.getTrainingDays();

                if (CourseCategory.isBudgetRelevant(existing.getCategoryName())) {
                    usedFees += existing.getFee();
                }
            }

        }
        if (usedDays + application.getTrainingDays() > limit) {
            return "Training days quota exceeded for " + year + ": used " + usedDays
                    + " + requested " + application.getTrainingDays()
                    + " exceeds your limit of " + limit + " days.";
        }
        if (CourseCategory.isBudgetRelevant(application.getCategoryName())
                && usedFees + application.getFee() > budget) {
            return "Training budget exceeded for " + year + ": used $" + usedFees
                    + " + requested $" + application.getFee()
                    + " exceeds your annual budget of $" + budget + ".";
        }
        return null;
    }

    private String validateOverlap(CourseApplication application, Employee employee, Long excludeId) {
        LocalDate from = application.getFromDate();
        LocalDate to = application.getToDate();
        for (CourseApplication existing : courseApplicationRepository.findByEmployee(employee)) {
            if (excludeId != null && excludeId.equals(existing.getId())) {
                continue;
            }
            String status = existing.getStatus();
            if (!"APPLIED".equals(status) && !"UPDATED".equals(status) && !"APPROVED".equals(status)) {
                continue;
            }
            if (existing.getFromDate() == null || existing.getToDate() == null) {
                continue;
            }
            if (!from.isAfter(existing.getToDate()) && !existing.getFromDate().isAfter(to)) {
                return "Course period overlaps with an existing application: "
                        + existing.getCourseTitle() + " (" + existing.getFromDate()
                        + " to " + existing.getToDate() + ").";
            }
        }
        return null;
    }

    @Override
    public List<CourseApplication> findApplicationsByEmployee(Employee employee) {
        int currentYear = LocalDate.now().getYear();
        return courseApplicationRepository.findByEmployee(employee).stream()
                .filter(course -> course.getFromDate() != null
                        && course.getFromDate().getYear() == currentYear)
                .toList();
    }

    @Override
    public Optional<CourseApplication> findApplicationById(Long id) {
        return courseApplicationRepository.findById(id);
    }

    @Override
    public String withdrawApplication(Long id, Employee employee) {
        Optional<CourseApplication> result = courseApplicationRepository.findById(id);
        if (result.isEmpty()) {
            return "Application not found.";
        }
        CourseApplication application = result.get();
        if (!application.getEmployee().getId().equals(employee.getId())) {
            return "You can only withdraw your own application";
        }
        if (!"APPLIED".equals(application.getStatus()) && !"UPDATED".equals(application.getStatus())) {
            return "Only pending applications can be withdrawn";
        }
        application.setStatus("DELETED");
        courseApplicationRepository.save(application);
        return "Course application withdrawn successfully";

    }

    @Override
    @Transactional
    public String updateApplication(Long id, CourseApplication updatedData, Employee employee) {
        Optional<CourseApplication> result = courseApplicationRepository.findById(id);
        if (result.isEmpty()) {
            return "Application not found.";
        }
        CourseApplication application = result.get();
        if (!application.getEmployee().getId().equals(employee.getId())) {
            return "You can only update your own application";
        }
        if (!"APPLIED".equals(application.getStatus()) && !"UPDATED".equals(application.getStatus())) {
            return "Only pending applications can be updated";
        }
        Employee currentEmployee = employeeRepository.findById(employee.getId()).orElse(employee);
        if (currentEmployee.getRole() == EmployeeRole.ADMIN) {
            return "Administrators cannot update personal course applications.";
        }
        String supervisorError = validateSupervisor(currentEmployee);
        if (supervisorError != null) return supervisorError;
        String error = selectCourse(updatedData);
        if (error != null) return error;
        error = validateBasicRules(updatedData);
        if (error != null) {
            return error;
        }
        updatedData.setTrainingDays(computeTrainingDays(updatedData));
        String entitlementError = validateEntitlement(updatedData, employee, id);
        if (entitlementError != null) {
            return entitlementError;
        }
        String overlapError = validateOverlap(updatedData, employee, id);
        if (overlapError != null) {
            return overlapError;
        }
        application.setCourse(updatedData.getCourse());
        application.setCategoryNameSnapshot(updatedData.getCategoryName());
        application.setCourseTitle(updatedData.getCourseTitle());
        application.setCategory(updatedData.getCategory());
        application.setFromDate(updatedData.getFromDate());
        application.setToDate(updatedData.getToDate());
        application.setProvider(updatedData.getProvider());
        application.setFee(updatedData.getFee());
        application.setJustification(updatedData.getJustification());
        application.setDissemination(updatedData.getDissemination());
        application.setHalfDay(updatedData.getHalfDay());
        application.setTrainingDays(updatedData.getTrainingDays());
        application.setStatus("UPDATED");
        courseApplicationRepository.save(application);
        return null;

    }

    @Override
    public String cancelApplication(Long id, Employee employee) {
        Optional<CourseApplication> result = courseApplicationRepository.findById(id);
        if (result.isEmpty()) {
            return "Application not found.";
        }
        CourseApplication application = result.get();
        if (!application.getEmployee().getId().equals(employee.getId())) {
            return "You can only cancel your own application";
        }
        if (!"APPROVED".equals(application.getStatus())) {
            return "Only approved applications can be cancelled";
        }
        application.setStatus("CANCELLED");
        courseApplicationRepository.save(application);
        return "Course application cancelled successfully";
    }

    @Override
    public String completeApplication(Long id, Employee employee, String experienceComments) {
        Optional<CourseApplication> result = courseApplicationRepository.findById(id);
        if (result.isEmpty()) {
            return "Application not found.";
        }
        CourseApplication application = result.get();
        if (!application.getEmployee().getId().equals(employee.getId())) {
            return "You can only complete your own application";
        }
        if (!"APPROVED".equals(application.getStatus())) {
            return "Only approved applications can be completed";
        }
        if (application.getToDate() == null || !application.getToDate().isBefore(LocalDate.now())) {
            return "Course has not ended yet";
        }
        String error = validateText(experienceComments, "Experience comments", true);
        if (error != null) return error;
        application.setStatus("COMPLETED");
        application.setExperienceComments(experienceComments.strip());
        courseApplicationRepository.save(application);
        return "Course application completed successfully";
    }

    @Override
    public List<CourseApplication> findSubordinateApplications(Employee manager) {
        List<Employee> subordinates = employeeRepository.findBySupervisor_Id(manager.getId());
        if (subordinates.isEmpty()) {
            return List.of();
        }
        return courseApplicationRepository.findByEmployeeIn(subordinates);
    }

    @Override
    public ManagerDecisionAid buildDecisionAid(CourseApplication application, Employee manager) {
        Employee applicant = application.getEmployee();
        int year = application.getFromDate() != null
                ? application.getFromDate().getYear() : LocalDate.now().getYear();
        Optional<AnnualEntitlement> entitlement =
                entitlementService.findEntitlement(applicant.getId(), year);
        boolean entitlementPresent = entitlement.isPresent();
        double limitDays = entitlementPresent ? entitlement.get().getTrainingDaysLimit() : 0;
        double budgetLimit = entitlementPresent ? entitlement.get().getTrainingBudget() : 0;
        double usedDays = entitlementService.getOccupiedTrainingDays(applicant.getId(), year);
        double usedBudget = entitlementService.getOccupiedTrainingBudget(applicant.getId(), year);

        List<CourseApplication> overlappingApproved = List.of();
        if (application.getFromDate() != null && application.getToDate() != null) {
            List<Employee> subordinates = employeeRepository.findBySupervisor_Id(manager.getId());
            if (!subordinates.isEmpty()) {
                overlappingApproved = courseApplicationRepository.findByEmployeeIn(subordinates).stream()
                        .filter(existing -> "APPROVED".equals(existing.getStatus()))
                        .filter(existing -> existing.getFromDate() != null && existing.getToDate() != null)
                        .filter(existing -> !existing.getEmployee().getId().equals(applicant.getId()))
                        .filter(existing -> !existing.getFromDate().isAfter(application.getToDate())
                                && !application.getFromDate().isAfter(existing.getToDate()))
                        .toList();
            }
        }
        return new ManagerDecisionAid(entitlementPresent, usedDays, limitDays,
                usedBudget, budgetLimit, overlappingApproved);
    }

    @Override
    @Transactional
    public String reviewApplication(Long id, Employee manager, String decision, String comment) {
        String error = validateText(comment, "Manager comment", true);
        if (error != null) return error;
        Optional<CourseApplication> result = courseApplicationRepository.findById(id);
        if (result.isEmpty()) {
            return "Application not found";
        }
        CourseApplication application = result.get();
        Employee owner = application.getEmployee();
        Employee currentManager = employeeRepository.findById(manager.getId()).orElse(null);
        if (currentManager == null || currentManager.getRole() != EmployeeRole.MANAGER
                || owner.getId().equals(manager.getId())) {
            return "Only the assigned manager may review this application. Self-approval is not allowed.";
        }
        if (owner.getSupervisor() == null
                || !owner.getSupervisor().getId().equals(manager.getId())) {
            return "You can only review your own subordinates' applications";
        }
        String status = application.getStatus();
        boolean pending = "APPLIED".equals(status) || "UPDATED".equals(status);
        if (!pending) {
            return "Only pending applications can be reviewed";
        }
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) {
            return "Invalid decision";
        }
        application.setStatus("APPROVE".equals(decision) ? "APPROVED" : "REJECTED");
        application.setManagerComment(comment.strip());
        courseApplicationRepository.save(application);
        notificationService.createNotification(application, "APPROVE".equals(decision)
                ? NotificationType.APPLICATION_APPROVED : NotificationType.APPLICATION_REJECTED);
        return "APPROVE".equals(decision) ? "Application approved" : "Application rejected";
    }

    private double computeTrainingDays(CourseApplication application) {

        double days = countTrainingDays(
                application.getFromDate(),
                application.getToDate());

        if (Boolean.TRUE.equals(application.getHalfDay())) {
            days = days - 0.5;
        }

        return days;
    }
    
    private boolean isPublicHoliDays(LocalDate date) { 
    	return publicHolidayRepository.findByHolidayDate(date).isPresent();
    	
    }

    private double countTrainingDays(LocalDate from, LocalDate to) {
        double days = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !isPublicHoliDays(date)) {
                days += 1;
            }
        }
        return days;
    }
    private String validateSupervisor(Employee employee) {
        Employee supervisor = employee.getSupervisor();
        if (supervisor == null || supervisor.getRole() != EmployeeRole.MANAGER
                || employee.getId().equals(supervisor.getId())) {
            return "Please ask an administrator to assign your manager before applying.";
        }
        return null;
    }

}
