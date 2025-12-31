package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BankPaymentItemDetailRequest {
    private Long detRowId;
    private Long stockPoid;
    private Long stockUnitPoid;
    private Long poQty;
    private Long dnQty;
    private Long qtyReceived;
    private Long price;
    private Long discount;
    private Long total;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
}