package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.group5.cats.model.Course;
import com.group5.cats.model.PublicHoliday;
import com.group5.cats.repository.PublicHolidayRepository;

class CourseScheduleServiceTests {
    @Test
    void durationSkipsWeekendAndHolidayAndAllowsOneDay() {
        PublicHolidayRepository holidays = mock(PublicHolidayRepository.class);
        CourseScheduleService service = new CourseScheduleServiceImpl(holidays);
        LocalDate friday = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        LocalDate monday = friday.plusDays(3);
        when(holidays.findByHolidayDate(monday)).thenReturn(Optional.of(new PublicHoliday(monday, "Test holiday")));
        assertEquals(friday.plusDays(4), service.calculateEndDate(friday, 2));
        assertEquals(friday, service.calculateEndDate(friday, 1));
        assertThrows(IllegalArgumentException.class, () -> service.calculateEndDate(friday, 0));
    }

    @Test
    void datesMustBePublishedFutureWorkingDays() {
        PublicHolidayRepository holidays = mock(PublicHolidayRepository.class);
        CourseScheduleService service = new CourseScheduleServiceImpl(holidays);
        LocalDate monday = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Course course = new Course();
        course.setDurationDays(2);
        course.getStartDates().add(LocalDate.now().minusDays(1));
        course.getStartDates().add(monday);
        course.getStartDates().add(monday.plusDays(5));
        assertEquals(1, service.findAvailableDates(course).size());
        assertEquals(monday.plusDays(1), service.findAvailableDates(course).get(monday));
        when(holidays.findByHolidayDate(monday)).thenReturn(Optional.of(new PublicHoliday(monday, "Holiday")));
        assertTrue(service.findAvailableDates(course).isEmpty());
    }
}
