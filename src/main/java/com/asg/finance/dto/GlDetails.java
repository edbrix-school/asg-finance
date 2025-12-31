package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GlDetails {

    private String type;                // DR / CR
    private Long companyPoid;           // Company field
    private String glCode;              // GL Master code

    private BigDecimal debitAmount;     // DR
    private BigDecimal creditAmount;    // CR

    private String taxCode;             // Tax Slab (LOV)
    private BigDecimal taxPercent;      // Tax %
    private BigDecimal taxAmount;       // Tax amount
    private BigDecimal totalAmount;     // DR/CR + tax

    private String vatSupplier;         // VAT Supplier (Petty Cash)
    private String vatPartyName;        // VAT Party Name
    private String vatInvoiceNumber;    // VAT Invoice Number

    private String invoiceDate;         // Date format yyyy-MM-dd
    private String remarks;             // Remarks per row
}

