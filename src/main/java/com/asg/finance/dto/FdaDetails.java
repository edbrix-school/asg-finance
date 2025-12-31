package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FdaDetails {

    private Long fdaDetRowId;       // FDA detail row reference

    private BigDecimal costAmount;  // Base Cost
    private BigDecimal pdaAmount;   // PDA Amount
    private BigDecimal chargeAmount;

    private String vatPartyName;
    private String vatInvoiceNumber;
    private String invoiceDate;

    private Integer sequenceNumber;
    private String checkAll;
    private String refDocId;
    private Long refDocPoid;
}

