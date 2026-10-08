package com.group5.cats.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.cats.model.PublicHoliday;
import com.group5.cats.repository.PublicHolidayRepository;

@Service
public class PublicHolidayServiceImpl implements PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;

    public PublicHolidayServiceImpl(
            PublicHolidayRepository publicHolidayRepository) {

        this.publicHolidayRepository = publicHolidayRepository;
    }

    @Override
    public List<PublicHoliday> findHolidaysByYear(Integer year) {

        if (year == null || year < 1 || year > 9999) {
            throw new IllegalArgumentException(
                    "Year must be between 1 and 9999."
            );
        }

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        return publicHolidayRepository
                .findByHolidayDateBetweenOrderByHolidayDateAsc(
                        startDate,
                        endDate
                );
    }

    @Override
    public PublicHoliday findHolidayById(Long id) {
        return publicHolidayRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public String createHoliday(LocalDate holidayDate, String name) {

        String error = validateHoliday(holidayDate, name);

        if (error != null) {
            return error;
        }

        if (publicHolidayRepository
                .findByHolidayDate(holidayDate).isPresent()) {
            return "A public holiday already exists on this date.";
        }

        PublicHoliday holiday = new PublicHoliday(
                holidayDate,
                name.trim()
        );

        publicHolidayRepository.save(holiday);

        return null;
    }

    @Override
    @Transactional
    public String updateHoliday(
            Long id,
            LocalDate holidayDate,
            String name) {

        PublicHoliday holiday =
                publicHolidayRepository.findById(id).orElse(null);

        if (holiday == null) {
            return "Public holiday not found.";
        }

        String error = validateHoliday(holidayDate, name);

        if (error != null) {
            return error;
        }

        Optional<PublicHoliday> existing =
                publicHolidayRepository.findByHolidayDate(holidayDate);

        if (existing.isPresent()
                && !existing.get().getId().equals(id)) {
            return "A public holiday already exists on this date.";
        }

        holiday.setHolidayDate(holidayDate);
        holiday.setName(name.trim());

        publicHolidayRepository.save(holiday);

        return null;
    }

    @Override
    @Transactional
    public String deleteHoliday(Long id) {

        PublicHoliday holiday =
                publicHolidayRepository.findById(id).orElse(null);

        if (holiday == null) {
            return "Public holiday not found.";
        }

        publicHolidayRepository.delete(holiday);

        return null;
    }

    private String validateHoliday(
            LocalDate holidayDate,
            String name) {

        if (holidayDate == null) {
            return "Holiday date is required.";
        }

        if (holidayDate.getYear() < 1
                || holidayDate.getYear() > 9999) {
            return "Holiday year must be between 1 and 9999.";
        }

        if (name == null || name.isBlank()) {
            return "Holiday name is required.";
        }

        if (name.trim().length() > 255) {
            return "Holiday name must not exceed 255 characters.";
        }

        return null;
    }
}