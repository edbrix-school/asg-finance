package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BankPaymentItemDetailRequest {

    private Long detRowId;
    private Long stockPoid;
    private Long stockUnitPoid;
    private Double poQty;
    private Double dnQty;
    private Double qtyReceived;
    private Double price;
    private Double discount;
    private Double total;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
    private String actionType;          // "isCreated", "isUpdated", "isDeleted", "noChanges"
}