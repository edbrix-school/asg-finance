package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraVoucherResponse {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private String postingNarration;
    private String createdBy;
    private String deleted;
    private LocalDateTime createdDate;
}

