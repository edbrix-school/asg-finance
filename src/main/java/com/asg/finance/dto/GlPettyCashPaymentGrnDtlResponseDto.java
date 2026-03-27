package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPettyCashPaymentGrnDtlResponseDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long grnPoid;
    private String checkAll;
    private BigDecimal amount;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
