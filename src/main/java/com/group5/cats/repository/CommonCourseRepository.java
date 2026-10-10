package com.group5.cats.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.CommonCourse;

public interface CommonCourseRepository
        extends JpaRepository<CommonCourse, Long> {

    List<CommonCourse> findAllByOrderByTitleAsc();

    boolean existsByTitleIgnoreCaseAndCategory_IdAndProvider_Id(
            String title,
            Long categoryId,
            Long providerId);

    boolean existsByTitleIgnoreCaseAndCategory_IdAndProvider_IdAndIdNot(
            String title,
            Long categoryId,
            Long providerId,
            Long id);

    boolean existsByCategory_Id(Long categoryId);

    boolean existsByProvider_Id(Long providerId);
}