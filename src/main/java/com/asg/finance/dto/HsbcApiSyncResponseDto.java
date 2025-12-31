package com.asg.finance.dto;

import java.util.List;

import lombok.Data;

@Data
public class HsbcApiSyncResponseDto {
    private List<HsbcApiBalanceDto> balances;
    private List<HsbcApiTransactionDto> transactions;
}
