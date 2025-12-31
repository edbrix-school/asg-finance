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
public class AdvancePettyCashHdrResponseDTO {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private Long pettyCashGlPoid;
    private GlLedgerDTO pettyCashGlPoidDet;
    private String payingTo;
    private BigDecimal iouAmount;
    private BigDecimal settledAmount;
    private BigDecimal balanceAmount;
    private String narration;
    private String status;
    private String closedReason;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private List<AdvancePettyCashDtlResponseDTO> details;
}