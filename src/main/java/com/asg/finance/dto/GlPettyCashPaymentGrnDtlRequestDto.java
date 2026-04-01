package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPettyCashPaymentGrnDtlRequestDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long grnPoid;
    private String checkAll;
    private BigDecimal amount;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
    private String actionType; // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
