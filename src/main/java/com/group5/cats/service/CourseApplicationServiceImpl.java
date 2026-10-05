package com.group5.cats.service;

import org.springframework.stereotype.Service;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.time.DayOfWeek;

@Service
public class CourseApplicationServiceImpl implements CourseApplicationService {
    private final CourseApplicationRepository courseApplicationRepository;
    private final EmployeeRepository employeeRepository;

    public CourseApplicationServiceImpl(CourseApplicationRepository courseApplicationRepository,
            EmployeeRepository employeeRepository) {
        this.courseApplicationRepository = courseApplicationRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public String submitApplication(CourseApplication application, Employee employee) {
        String error = validateBasicRules(application);
        if (error != null) {
            return error;
        }

        application.setEmployee(employee);
        application.setStatus("APPLIED");
        courseApplicationRepository.save(application);
        return null;
    }

    private String validateBasicRules(CourseApplication application) {
        LocalDate from = application.getFromDate();
        LocalDate to = application.getToDate();
        if (from == null || to == null) {
            return "Course start and end dates must be provided";
    }
        if (!from.isBefore(to)) {
            return "Course start date cannot be after end date";
        }
        if (!from.isAfter(LocalDate.now())) {
            return "From date must start in the future";
        }
        if (from.getDayOfWeek()==DayOfWeek.SATURDAY || from.getDayOfWeek()==DayOfWeek.SUNDAY) {
            return "'From' date must be a working day (Monday to Friday).";
        }
        if (to.getDayOfWeek()==DayOfWeek.SATURDAY || to.getDayOfWeek()==DayOfWeek.SUNDAY) {
            return "'To' date must be a working day (Monday to Friday).";
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
        application.setCourseTitle(updatedData.getCourseTitle());
        application.setCategory(updatedData.getCategory());
        application.setFromDate(updatedData.getFromDate());
        application.setProvider(updatedData.getProvider());
        application.setFromDate(updatedData.getFromDate());
        application.setToDate(updatedData.getToDate());
        application.setFee(updatedData.getFee());
        application.setJustification(updatedData.getJustification());
        application.setDissemination(updatedData.getDissemination());
        application.setStatus("UPDATED");
        courseApplicationRepository.save(application);
        return "Course application updated successfully";
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
        application.setStatus("COMPLETED");
        application.setExperienceComments(experienceComments);
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
    public String reviewApplication(Long id, Employee manager, String decision, String comment) {
        if (comment == null || comment.isBlank()) {
            return "Comment cannot be empty";
        }
        Optional<CourseApplication> result = courseApplicationRepository.findById(id);
        if (result.isEmpty()) {
            return "Application not found";
        }
        CourseApplication application = result.get();
        Employee owner = application.getEmployee();
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
        application.setManagerComment(comment);
        courseApplicationRepository.save(application);
        return "APPROVE".equals(decision) ? "Application approved" : "Application rejected";
    }
}
