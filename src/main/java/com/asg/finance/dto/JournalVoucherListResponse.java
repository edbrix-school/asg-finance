package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalVoucherListResponse {
    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private String postingNarration;
    private String refType;
    private String currencyCode;
    private BigDecimal amount;
    private BigDecimal bhdAmount;
    private String status;
    private String createdBy;
    private LocalDateTime createdDate;
}
