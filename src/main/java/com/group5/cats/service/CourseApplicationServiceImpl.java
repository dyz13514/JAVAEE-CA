package com.group5.cats.service;

import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.group5.cats.dto.ApplicationSearch;
import com.group5.cats.model.EmployeeRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.NotificationType;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Course;
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
    private final CourseScheduleService scheduleService;

    public CourseApplicationServiceImpl(CourseApplicationRepository courseApplicationRepository,
            EmployeeRepository employeeRepository, EntitlementService entitlementService,
            PublicHolidayRepository publicHolidayRepository, NotificationService notificationService,
            CourseRepository courseRepository, CourseScheduleService scheduleService) {
        this.courseApplicationRepository = courseApplicationRepository;
        this.employeeRepository = employeeRepository;
        this.entitlementService = entitlementService;
        this.publicHolidayRepository = publicHolidayRepository;
        this.notificationService = notificationService;
        this.courseRepository = courseRepository;
        this.scheduleService = scheduleService;
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
        Page<CourseApplication> result = courseApplicationRepository.searchTeamApplications(
                manager.getId(), search.getKeyword(), request);
        if (search.getPage() > Math.max(1, result.getTotalPages())) {
            search.setPage(Math.max(1, result.getTotalPages()));
            request = PageRequest.of(search.getPage() - 1, search.getSize(), order);
            result = courseApplicationRepository.searchTeamApplications(manager.getId(), search.getKeyword(), request);
        }
        return result;
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
        String error = selectCourse(application, null);
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

    private String selectCourse(CourseApplication data, CourseApplication existing) {
        Long courseId = data.getCourseId();
        if (courseId == null) {
            if (existing != null && existing.getCourse() != null) {
                return "Please select a recorded course and one of its available start dates.";
            }
            data.setCourse(null);
            return null; // Legacy applications without a course link keep their original workflow.
        }
        if (courseId <= 0) {
            return "Please select a valid course.";
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return "Selected course no longer exists.";
        }
        boolean sameSchedule = existing != null && existing.getCourse() != null
                && courseId.equals(existing.getCourse().getId())
                && Objects.equals(data.getFromDate(), existing.getFromDate());
        if (sameSchedule) {
            // Published changes must not move an existing applicant to different dates.
            data.setToDate(existing.getToDate());
            data.setHalfDay(existing.getHalfDay());
        } else {
            if (course.getDurationDays() == null) {
                return "This course has no published schedule. Please contact an administrator.";
            }
            LocalDate end = scheduleService.findAvailableDates(course).get(data.getFromDate());
            if (end == null) return "Please choose one of this course's available start dates.";
            data.setToDate(end);
            data.setHalfDay(false);
        }
        data.setCourse(course);
        if (existing != null && existing.getCourse() != null
                && courseId.equals(existing.getCourse().getId())) {
            // Editing dates or reasons should keep the original course snapshot.
            data.setCourseTitle(existing.getCourseTitle());
            data.setCategory(existing.getCategory());
            data.setProvider(existing.getProvider());
        } else {
            data.setCourseTitle(course.getTitle());
            data.setCategory(course.getCategory());
            data.setProvider(course.getProvider().getName());
        }
        // Fee is the actual fee requested by the employee, not the reference fee.
        return null;
    }

    private String validateBasicRules(CourseApplication application) {
        String error = validateText(application.getCourseTitle(), "Course title", true);
        if (error != null) return error;
        error = validateText(application.getJustification(), "Justification", true);
        if (error != null) return error;
        error = validateText(application.getProvider(), "Training provider", false);
        if (error != null) return error;
        error = validateText(application.getDissemination(), "Work dissemination", false);
        if (error != null) return error;

        String category = application.getCategory();
        if (!"INTERNAL".equals(category) && !"EXTERNAL".equals(category)
                && !"CERTIFICATION".equals(category)) {
            return "Please select a valid course category.";
        }
        double fee = application.getFee();
        if (!Double.isFinite(fee) || fee < 0) {
            return "Course fee must be a valid number greater than or equal to zero.";
        }
        if ("INTERNAL".equals(category) && fee != 0) {
            return "Internal training must have a fee of zero.";
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
            if (!"INTERNAL".equals(application.getCategory())) {
                return "Half-day sessions are allowed for Internal Training only.";
            }
            if (!from.equals(to)) {
                return "Half-day session must start and end on the same date.";
            }
        } else if (from.isAfter(to) || (from.equals(to) && application.getCourse() == null)) {
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
        if (application.getProvider() != null) application.setProvider(application.getProvider().strip());
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

                if ("EXTERNAL".equals(existing.getCategory()) || "CERTIFICATION".equals(existing.getCategory())) {
                    usedFees += existing.getFee();
                }
            }

        }
        if (usedDays + application.getTrainingDays() > limit) {
            return "Training days quota exceeded for " + year + ": used " + usedDays
                    + " + requested " + application.getTrainingDays()
                    + " exceeds your limit of " + limit + " days.";
        }
        if (("EXTERNAL".equals(application.getCategory()) || "CERTIFICATION".equals(application.getCategory()))
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
        String error = selectCourse(updatedData, application);
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
        application.setCourseId(updatedData.getCourseId());
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
        if (Boolean.TRUE.equals(application.getHalfDay())) {
            return 0.5;
        }
        return countTrainingDays(application.getFromDate(), application.getToDate());
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
