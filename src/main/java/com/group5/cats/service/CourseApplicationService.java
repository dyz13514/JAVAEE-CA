package com.group5.cats.service;
import java.util.List;
import java.util.Optional;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;

public interface CourseApplicationService {
void submitApplication(CourseApplication application, Employee applicant);
List<CourseApplication> findApplicationsByEmployee(Employee employee);
Optional<CourseApplication> findApplicationById(Long id);


}
