package com.asg.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodValidationHelperTest {

    private PeriodValidationHelper helper;

    @BeforeEach
    void setUp() {
        helper = new PeriodValidationHelper();
    }

    @Test
    void validatePeriodRules_InvalidPeriodFromNotMonthStart_ReturnsLegacyMessageOnly() {
        LocalDateTime periodFrom = LocalDateTime.of(2026, 5, 31, 0, 0);
        LocalDateTime periodTo = LocalDateTime.of(2025, 5, 31, 0, 0);

        List<String> errors = helper.validatePeriodRules(periodFrom, periodTo, 1);

        assertEquals(1, errors.size());
        assertEquals("Period From date value is not a month start date.", errors.get(0));
    }

    @Test
    void validatePeriodRules_PeriodToBeforePeriodFrom_ReturnsLegacyWarning() {
        LocalDateTime periodFrom = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime periodTo = LocalDateTime.of(2025, 5, 31, 0, 0);

        List<String> errors = helper.validatePeriodRules(periodFrom, periodTo, 1, LocalDate.of(2026, 5, 14));

        assertEquals(1, errors.size());
        assertEquals("WARNING : Period To date value is less than or equal to the From date.", errors.get(0));
    }

    @Test
    void validatePeriodRules_PeriodToBeforePeriodFrom_TakesPriorityOverMonthEndRule() {
        LocalDateTime periodFrom = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime periodTo = LocalDateTime.of(2025, 5, 15, 0, 0);

        List<String> errors = helper.validatePeriodRules(periodFrom, periodTo, 1, LocalDate.of(2026, 5, 14));

        assertEquals("WARNING : Period To date value is less than or equal to the From date.", errors.get(0));
    }

    @Test
    void validatePeriodRules_PeriodToAfterCurrentDate_ReturnsLegacyWarning() {
        LocalDate currentDate = LocalDate.of(2026, 5, 20);
        LocalDateTime periodFrom = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime periodTo = LocalDateTime.of(2027, 5, 31, 0, 0);

        List<String> errors = helper.validatePeriodRules(periodFrom, periodTo, 1, currentDate);

        assertEquals(1, errors.size());
        assertEquals("WARNING : Period To date value is greater than the current date.", errors.get(0));
    }

    @Test
    void validatePeriodRules_PeriodToAfterCurrentDate_TakesPriorityOverMonthEndRule() {
        LocalDate currentDate = LocalDate.of(2026, 5, 20);
        LocalDateTime periodFrom = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime periodTo = LocalDateTime.of(2027, 6, 15, 0, 0);

        List<String> errors = helper.validatePeriodRules(periodFrom, periodTo, 1, currentDate);

        assertEquals("WARNING : Period To date value is greater than the current date.", errors.get(0));
    }

    @Test
    void validatePeriodRules_ValidMonthlyPeriod_ReturnsNoErrors() {
        LocalDateTime periodFrom = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime periodTo = LocalDateTime.of(2026, 5, 31, 0, 0);

        List<String> errors = helper.validatePeriodRules(periodFrom, periodTo, 1, LocalDate.of(2026, 5, 31));

        assertTrue(errors.isEmpty());
    }
}
