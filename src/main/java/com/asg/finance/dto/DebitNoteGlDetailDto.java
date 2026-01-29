package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DebitNoteGlDetailDto {
    
    private Long transactionPoid;
    
    private Long detRowId;
    
    @NotNull
    private String type; // ACC_TYPE_SHORT
    
    private Long companyPoid; // For multi-company
    
    @NotNull
    private Long glId; // GL_MASTER_LEDGERS_A_L
    
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal debitAmount;
    
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal creditAmount;
    
    private Long taxId; // DR_TAX_MASTER
    
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal taxPercentage;
    
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal taxAmount;
    
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal totalAmount;
    
    private String remarks;

    private List<BillwiseBreakupPopupRequestDto> breakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;

}