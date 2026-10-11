package com.group5.cats.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.group5.cats.model.CommonCourse;

public interface CommonCourseRepository extends JpaRepository<CommonCourse, Long> {
    @Query("select cc from CommonCourse cc order by cc.course.title, cc.course.category.name, cc.course.provider.name")
    List<CommonCourse> findAllByOrderByTitleAsc();
    boolean existsByCourse_Id(Long courseId);
}
