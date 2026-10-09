package com.group5.cats.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.group5.cats.model.Course;
import com.group5.cats.repository.PublicHolidayRepository;

@Service
public class CourseScheduleServiceImpl implements CourseScheduleService {
    private final PublicHolidayRepository holidayRepository;

    public CourseScheduleServiceImpl(PublicHolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Override
    public Map<LocalDate, LocalDate> findAvailableDates(Course course) {
        Map<LocalDate, LocalDate> dates = new LinkedHashMap<>();
        if (course.getDurationDays() == null) return dates;
        for (LocalDate start : course.getStartDates()) {
            if (start.isAfter(LocalDate.now()) && isWorkingDay(start)) {
                dates.put(start, calculateEndDate(start, course.getDurationDays()));
            }
        }
        return dates;
    }

    @Override
    public LocalDate calculateEndDate(LocalDate start, int trainingDays) {
        if (start == null || trainingDays < 1 || trainingDays > 30 || !isWorkingDay(start)) {
            throw new IllegalArgumentException("Please select a valid course schedule.");
        }
        LocalDate end = start;
        int remaining = trainingDays - 1;
        while (remaining > 0) {
            end = end.plusDays(1);
            if (isWorkingDay(end)) remaining--;
        }
        return end;
    }

    private boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && holidayRepository.findByHolidayDate(date).isEmpty();
    }
}
