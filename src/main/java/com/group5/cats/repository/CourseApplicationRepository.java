package com.group5.cats.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;

public interface CourseApplicationRepository
        extends JpaRepository<CourseApplication, Long> {

    boolean existsByCourse_Id(Long courseId);

    List<CourseApplication> findByEmployee(Employee employee);

    List<CourseApplication> findByEmployeeIn(List<Employee> employees);

    // LOCATE treats % and _ as ordinary search characters, not SQL wildcards.
    @Query("SELECT a FROM CourseApplication a WHERE a.employee.id = :employeeId "
            + "AND (:currentYear = false OR (a.fromDate >= :yearStart AND a.fromDate < :nextYear)) "
            + "AND (:keyword = '' OR LOCATE(LOWER(:keyword), LOWER(a.courseTitle)) > 0 "
            + "OR LOCATE(LOWER(:keyword), LOWER(a.category)) > 0 "
            + "OR LOCATE(LOWER(:keyword), LOWER(a.provider)) > 0 "
            + "OR LOCATE(LOWER(:keyword), LOWER(a.status)) > 0)")
    Page<CourseApplication> searchEmployeeApplications(
            @Param("employeeId") Long employeeId, @Param("currentYear") boolean currentYear,
            @Param("yearStart") LocalDate yearStart, @Param("nextYear") LocalDate nextYear,
            @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT a FROM CourseApplication a WHERE a.employee.supervisor.id = :managerId "
            + "AND (:keyword = '' OR LOCATE(LOWER(:keyword), LOWER(a.employee.name)) > 0 "
            + "OR LOCATE(LOWER(:keyword), LOWER(a.courseTitle)) > 0 "
            + "OR LOCATE(LOWER(:keyword), LOWER(a.category)) > 0 "
            + "OR LOCATE(LOWER(:keyword), LOWER(a.provider)) > 0 "
            + "OR LOCATE(LOWER(:keyword), LOWER(a.status)) > 0)")
    Page<CourseApplication> searchTeamApplications(@Param("managerId") Long managerId,
            @Param("keyword") String keyword, Pageable pageable);

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
