package com.group5.cats.service;
import java.util.List;
import java.util.Optional;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;

public interface CourseApplicationService {
String submitApplication(CourseApplication application, Employee applicant);
List<CourseApplication> findApplicationsByEmployee(Employee employee);
Optional<CourseApplication> findApplicationById(Long id);
String withdrawApplication(Long id, Employee employee);
String updateApplication(Long id, CourseApplication updatedData, Employee employee);
String cancelApplication(Long id, Employee employee);
String completeApplication(Long id, Employee employee, String experienceComments);
List<CourseApplication> findSubordinateApplications(Employee manager);
String reviewApplication(Long id, Employee manager, String decision, String comment);

}
