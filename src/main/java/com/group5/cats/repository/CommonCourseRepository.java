package com.group5.cats.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.group5.cats.model.CommonCourse;

public interface CommonCourseRepository extends JpaRepository<CommonCourse, Long> {
    List<CommonCourse> findAllByOrderByCourse_TitleAsc();
    boolean existsByCourse_Id(Long courseId);
    boolean existsByCourse_IdAndIdNot(Long courseId, Long id);
}
