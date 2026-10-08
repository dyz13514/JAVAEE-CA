package com.group5.cats.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.CommonCourse;

public interface CommonCourseRepository
        extends JpaRepository<CommonCourse, Long> {

    List<CommonCourse> findAllByOrderByTitleAsc();

    boolean existsByProvider_Id(Long providerId);
}