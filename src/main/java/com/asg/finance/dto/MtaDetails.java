package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MtaDetails {
    private BigDecimal purchaseQuantity;
    private BigDecimal unitPrice;
}

