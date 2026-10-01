package com.group5.cats.service;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;

public interface CourseApplicationService {
void submitApplication(CourseApplication application, Employee applicant);

}
