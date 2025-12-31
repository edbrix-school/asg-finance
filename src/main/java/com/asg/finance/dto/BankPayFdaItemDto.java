package com.asg.finance.dto;

import lombok.Data;

@Data
public class BankPayFdaItemDto {
    private Long chargePoid;
    private Double pdaAmount;
    private String remarks;
    private String refDocId;
    private String refDocPoid;
    private String detRowId;
}
