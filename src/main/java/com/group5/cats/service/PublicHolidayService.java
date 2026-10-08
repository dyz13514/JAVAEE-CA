package com.group5.cats.service;

import java.time.LocalDate;
import java.util.List;

import com.group5.cats.model.PublicHoliday;

public interface PublicHolidayService {

    List<PublicHoliday> findHolidaysByYear(Integer year);

    PublicHoliday findHolidayById(Long id);

    String createHoliday(LocalDate holidayDate, String name);

    String updateHoliday(
            Long id,
            LocalDate holidayDate,
            String name);

    String deleteHoliday(Long id);
}