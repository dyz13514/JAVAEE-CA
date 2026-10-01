package com.group5.cats.service;
import org.springframework.stereotype.Service;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.repository.CourseApplicationRepository;

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
}

