package com.group5.cats.service;
import org.springframework.stereotype.Service;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.repository.CourseApplicationRepository;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
@Service
public class CourseApplicationServiceImpl implements CourseApplicationService {
    private final CourseApplicationRepository courseApplicationRepository;

    public CourseApplicationServiceImpl(CourseApplicationRepository courseApplicationRepository) {
        this.courseApplicationRepository = courseApplicationRepository;
    }

    @Override
    public void submitApplication (CourseApplication application, Employee employee) {
        application.setEmployee(employee);
        application.setStatus("APPLIED");
        courseApplicationRepository.save(application);
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
    public Optional<CourseApplication> findApplicationById(Long id){
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
        if (!"APPROVED".equals(application.getStatus())){
            return "Only approved applications can be cancelled";
        }
        application.setStatus("CANCELLED");
        courseApplicationRepository.save(application);
         return "Course application cancelled successfully";
    }
}

