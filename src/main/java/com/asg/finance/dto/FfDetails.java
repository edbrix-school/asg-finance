package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FfDetails {

    private String refDocId;        // Reference Document (text)
    private Long refDocPoid;        // Reference POID

    private BigDecimal costAmount;  // Base Amount (Cost)
    private BigDecimal ffAmount;    // FF Amount (if available)
    private BigDecimal chargeAmount; // Total charge amount for this line (if needed separately)

    private String vatPartyName;
    private String vatInvoiceNumber;
    private String invoiceDate;

    private String issueInvoice;  // Yes/No (Credit Note FF Invoice)
    private Integer sequenceNumber;
}



