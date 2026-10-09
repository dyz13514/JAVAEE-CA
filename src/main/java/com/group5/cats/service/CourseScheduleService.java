package com.group5.cats.service;

import java.time.LocalDate;
import java.util.Map;
import com.group5.cats.model.Course;

public interface CourseScheduleService {
    Map<LocalDate, LocalDate> findAvailableDates(Course course);
    LocalDate calculateEndDate(LocalDate start, int trainingDays);
}
