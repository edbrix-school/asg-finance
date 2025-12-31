package com.asg.finance.repository;

import com.asg.finance.dto.HsbcApiSyncResponseDto;

public interface HsbcApiSyncRepository {
    HsbcApiSyncResponseDto loadHsbcApiData(String accountNumber, String date);
}
