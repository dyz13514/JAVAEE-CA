package com.group5.cats.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;

public interface CourseApplicationRepository extends JpaRepository<CourseApplication, Long> {
    List<CourseApplication> findByEmployee(Employee employee);

    List<CourseApplication> findByEmployeeIn(List<Employee> employees);
}
