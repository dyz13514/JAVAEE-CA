package com.group5.cats.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.CourseFeeClaim;

public interface CourseFeeClaimRepository extends JpaRepository<CourseFeeClaim, Long> {

    Optional<CourseFeeClaim> findByApplication_Id(Long applicationId);

    List<CourseFeeClaim> findByApplication_Employee_IdOrderBySubmittedAtDescIdDesc(Long employeeId);

    List<CourseFeeClaim> findByApplication_Employee_Supervisor_IdOrderBySubmittedAtDescIdDesc(Long managerId);

    List<CourseFeeClaim> findAllByOrderBySubmittedAtDescIdDesc();
}
