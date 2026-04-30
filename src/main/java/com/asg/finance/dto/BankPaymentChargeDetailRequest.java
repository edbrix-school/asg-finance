package com.asg.finance.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BankPaymentChargeDetailRequest {

    private Long detRowId;
    private Long chargePoid;
    private BigDecimal chargeAmount;
    private String description;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
    private Boolean selected;
    private BigDecimal pdaAmount;
    private BigDecimal ffAmount;
    private String actionType;          // "isCreated", "isUpdated", "isDeleted", "noChanges"
}