package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDepositVoucherResponseDto {
    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private Long bankPoid;
    private String postingNarration;
    private String remarks;
    private BigDecimal grandTotal;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String groupPosting;
    private String refType;
    private String bankFilter;
    private List<BankDepositVoucherDtlDto> details;
}
