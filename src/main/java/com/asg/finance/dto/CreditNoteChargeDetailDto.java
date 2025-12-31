package com.asg.finance.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class CreditNoteChargeDetailDto {
    private Long detRowId;
    private String refType;
    private String chargeCode; // Ticket field (instead of chargePoid)
    private String chargeDescription;
    private BigDecimal costAmount;
    private BigDecimal chargeAmount;
    @NotBlank(message = "Tax mandatory")
    private String taxCode; // Ticket field (instead of taxPoid)
    private BigDecimal taxPercent; // Ticket field (instead of taxPercentage)
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remarks;
    private String issueInvoice;
    private Boolean select;
    
    // Keep existing fields for backward compatibility
    private Long chargePoid;
    private String description;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
    private String checkAll;
    private BigDecimal pdaAmount;
    private BigDecimal ffAmount;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal chargeCostAmount;

}