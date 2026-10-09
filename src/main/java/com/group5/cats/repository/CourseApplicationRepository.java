package com.group5.cats.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;

public interface CourseApplicationRepository
        extends JpaRepository<CourseApplication, Long> {

    boolean existsByCourse_Id(Long courseId);

    List<CourseApplication> findByEmployee(Employee employee);

    List<CourseApplication> findByEmployeeIn(List<Employee> employees);

    @Query("SELECT application FROM CourseApplication application "
            + "WHERE application.status = 'APPROVED' "
            + "AND application.employee.id = :employeeId "
            + "AND application.fromDate <= :monthEnd "
            + "AND application.toDate >= :monthStart "
            + "ORDER BY application.fromDate ASC, "
            + "application.employee.name ASC, application.id ASC")
    List<CourseApplication> findApprovedApplicationsForCalendar(
            @Param("employeeId") Long employeeId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);
}
