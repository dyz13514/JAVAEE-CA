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
}

