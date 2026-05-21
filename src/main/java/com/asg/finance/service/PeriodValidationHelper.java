package com.asg.finance.service;

import com.asg.common.lib.utility.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
public class PeriodValidationHelper {

    static final String PERIOD_FROM_NOT_MONTH_START =
            "Period From date value is not a month start date.";
    static final String PERIOD_TO_AFTER_CURRENT_DATE =
            "WARNING : Period To date value is greater than the current date.";
    static final String PERIOD_TO_NOT_MONTH_END =
            "Period To must be the last day of the month";
    static final String PERIOD_TO_NOT_AFTER_FROM =
            "WARNING : Period To date value is less than or equal to the From date.";

    /**
     * Validates period rules (legacy order; returns at most one error):
     * - Period From must be first day of month
     * - Period To must not be after the current date (legacy warning)
     * - Period To must be after Period From (legacy warning)
     * - Period To must be last day of month
     * - Period duration (P_PERIOD_TO - P_PERIOD_FROM) <= 30 days when VAT_FILING_PERIOD = 1
     * - Period must be within same month when VAT_FILING_PERIOD = 1
     */
    public List<String> validatePeriodRules(LocalDateTime periodFrom, LocalDateTime periodTo, Integer vatFilingPeriod) {
        return validatePeriodRules(periodFrom, periodTo, vatFilingPeriod, DateUtil.getCurrentDateInUserTimeZone());
    }

    List<String> validatePeriodRules(LocalDateTime periodFrom, LocalDateTime periodTo,
                                     Integer vatFilingPeriod, LocalDate currentDate) {
        if (periodFrom == null || periodTo == null) {
            return List.of("Period From and Period To are required");
        }

        LocalDate fromDate = periodFrom.toLocalDate();
        LocalDate toDate = periodTo.toLocalDate();

        if (fromDate.getDayOfMonth() != 1) {
            return List.of(PERIOD_FROM_NOT_MONTH_START);
        }

        if (currentDate != null && toDate.isAfter(currentDate)) {
            return List.of(PERIOD_TO_AFTER_CURRENT_DATE);
        }

        if (!toDate.isAfter(fromDate)) {
            return List.of(PERIOD_TO_NOT_AFTER_FROM);
        }

        LocalDate lastDayOfToMonth = toDate.withDayOfMonth(toDate.lengthOfMonth());
        if (!toDate.equals(lastDayOfToMonth)) {
            return List.of(PERIOD_TO_NOT_MONTH_END);
        }

        if (vatFilingPeriod != null && vatFilingPeriod == 1) {
            long daysDifference = ChronoUnit.DAYS.between(fromDate, toDate);
            if (daysDifference > 30) {
                return List.of("Period duration must not exceed 31 days when VAT_FILING_PERIOD = 1");
            }

            if (fromDate.getMonth() != toDate.getMonth() || fromDate.getYear() != toDate.getYear()) {
                return List.of("Period From and Period To must be in the same month when VAT_FILING_PERIOD = 1");
            }
        }

        return List.of();
    }
}


