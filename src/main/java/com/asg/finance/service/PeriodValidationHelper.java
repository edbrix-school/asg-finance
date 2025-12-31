package com.asg.finance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PeriodValidationHelper {

    /**
     * Validates period rules:
     * - Period From must be first day of month
     * - Period To must be last day of month
     * - Period duration (P_PERIOD_TO - P_PERIOD_FROM) <= 30 days when VAT_FILING_PERIOD = 1
     * - Period must be within same month when VAT_FILING_PERIOD = 1
     */
    public List<String> validatePeriodRules(Timestamp periodFrom, Timestamp periodTo, Integer vatFilingPeriod) {
        List<String> errors = new ArrayList<>();
        
        if (periodFrom == null || periodTo == null) {
            errors.add("Period From and Period To are required");
            return errors;
        }
        
        LocalDate fromDate = periodFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate toDate = periodTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        // Check if Period From is first day of month
        if (fromDate.getDayOfMonth() != 1) {
            errors.add("Period From must be the first day of the month");
        }
        
        // Check if Period To is last day of month
        LocalDate lastDayOfMonth = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
        if (!toDate.equals(lastDayOfMonth)) {
            errors.add("Period To must be the last day of the month");
        }
        
        // Check duration and same month when VAT_FILING_PERIOD = 1
        if (vatFilingPeriod != null && vatFilingPeriod == 1) {
            // Mirror PL/SQL logic: L_DATE_DIFF := P_PERIOD_TO - P_PERIOD_FROM; IF L_DATE_DIFF > 30 THEN ERROR
            long daysDifference = ChronoUnit.DAYS.between(fromDate, toDate); // non-inclusive difference
            if (daysDifference > 30) {
                // For users this effectively means max 31 calendar days in the period
                errors.add("Period duration must not exceed 31 days when VAT_FILING_PERIOD = 1");
            }
            
            if (fromDate.getMonth() != toDate.getMonth() || fromDate.getYear() != toDate.getYear()) {
                errors.add("Period From and Period To must be in the same month when VAT_FILING_PERIOD = 1");
            }
        }
        
        // Check that Period From <= Period To
        if (fromDate.isAfter(toDate)) {
            errors.add("Period From must be before or equal to Period To");
        }
        
        return errors;
    }
}


