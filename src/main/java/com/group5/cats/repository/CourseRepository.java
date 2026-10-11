package com.group5.cats.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.group5.cats.model.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc();
    Optional<Course> findByTitleIgnoreCaseAndCategory_IdAndProvider_Id(String title, Long categoryId, Long providerId);
    boolean existsByTitleIgnoreCaseAndCategory_IdAndProvider_IdAndIdNot(String title, Long categoryId, Long providerId, Long id);
    boolean existsByCategory_Id(Long categoryId);
    boolean existsByProvider_Id(Long providerId);
}
