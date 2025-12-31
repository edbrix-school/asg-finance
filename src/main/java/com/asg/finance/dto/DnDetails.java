package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DnDetails {

    private BigDecimal costAmount;      // Cost Amount (Base)
    private BigDecimal chargeAmount;    // Charge Amount
    private Integer sequenceNumber;
    private BigDecimal basicAmount;
    private String refDocId;
    private Long refDocPoid;

}

