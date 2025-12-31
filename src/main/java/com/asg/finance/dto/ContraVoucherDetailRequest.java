package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraVoucherDetailRequest {

    private Long detRowId; // Required for isUpdated/isDeleted/noChange, null for isCreated (auto-generated)
    private String actionType; // isCreated, isUpdated, noChange/noChanges, isDeleted
    private String type; // Dr or Cr
    private Long companyPoid;
    private Long glPoid;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String remarks;
}

