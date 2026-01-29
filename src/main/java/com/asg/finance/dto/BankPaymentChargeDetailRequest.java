package com.asg.finance.dto;

import lombok.Data;

@Data
public class BankPaymentChargeDetailRequest {

    private Long detRowId;
    private Long chargePoid;
    private Long chargeAmount;
    private String description;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
    private Boolean selected;
    private Long pdaAmount;
    private Long ffAmount;
    private String actionType;          // "isCreated", "isUpdated", "isDeleted", "noChanges"
}