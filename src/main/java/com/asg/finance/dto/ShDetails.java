package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShDetails {
    private BigDecimal costAmount;      // Cost Amount
    private String refDocId;
    private Long refDocPoid;

    private String vatPartyName;
    private String vatInvoiceNumber;
    private String invoiceDate;

    private String issueInvoice;        // Yes/No
    private Integer sequenceNumber;
}
