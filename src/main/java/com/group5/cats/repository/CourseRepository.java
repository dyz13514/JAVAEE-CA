package com.group5.cats.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.group5.cats.model.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByOrderByTitleAsc();
    boolean existsByProvider_Id(Long providerId);

    boolean existsByTitleIgnoreCaseAndCategoryAndProvider_Id(
            String title, String category, Long providerId);

    boolean existsByTitleIgnoreCaseAndCategoryAndProvider_IdAndIdNot(
            String title, String category, Long providerId, Long id);
}
