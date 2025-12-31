package com.asg.finance.service;

import java.time.LocalDate;

import com.asg.finance.dto.HsbcApiSyncResponseDto;

public interface HsbcApiSyncService {
    HsbcApiSyncResponseDto refreshHsbcData(String accountNumber, LocalDate date);
    String syncHsbcApiData(String accountNumber, LocalDate date);
}
