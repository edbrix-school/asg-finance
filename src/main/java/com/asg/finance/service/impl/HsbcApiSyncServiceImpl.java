package com.asg.finance.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.asg.common.lib.exception.AsgException;
import com.asg.finance.service.HsbcApiSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.finance.dto.HsbcApiSyncResponseDto;
import com.asg.finance.repository.HsbcApiSyncRepository;
import com.asg.finance.utility.HsbcApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HsbcApiSyncServiceImpl implements HsbcApiSyncService {

    private final HsbcApiSyncRepository repository;
    private final HsbcApiClient hsbcApiClient;

    @Override
    @Transactional(readOnly = true)
    public HsbcApiSyncResponseDto refreshHsbcData(String accountNumber, LocalDate date) {
        validateInput(accountNumber, date);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")).toUpperCase();
        return repository.loadHsbcApiData(accountNumber, formattedDate);
    }
    
    @Override
    @Transactional
    public String syncHsbcApiData(String accountNumber, LocalDate date) {
        validateInput(accountNumber, date);
        try {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")).toUpperCase();
            hsbcApiClient.syncHsbcData(accountNumber, formattedDate);
            return "HSBC data synced successfully";
        } catch (Exception e) {
            log.error("Error syncing HSBC data: {}", e.getMessage(), e);
            throw new AsgException(e.getMessage());
        }
    }
    
    private void validateInput(String accountNumber, LocalDate date) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new AsgException("Bank account must be selected", 400);
        }
        if (date == null) {
            throw new AsgException("Date must be selected", 400);
        }
        if (date.isBefore(LocalDate.now().minusMonths(3))) {
            throw new AsgException("Date range must be within the last 3 months", 400);
        }
    }
}
