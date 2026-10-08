package com.group5.cats.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.PublicHoliday;

public interface PublicHolidayRepository
        extends JpaRepository<PublicHoliday, Long> {

    List<PublicHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(
            LocalDate startDate, LocalDate endDate);

    Optional<PublicHoliday> findByHolidayDate(LocalDate holidayDate);
}