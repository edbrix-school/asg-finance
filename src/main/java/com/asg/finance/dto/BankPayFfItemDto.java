package com.asg.finance.dto;

import lombok.Data;

@Data
public class BankPayFfItemDto {
    private Long chargePoid;
    private Double chargeAmount;
    private Double ffAmount;
    private String refDocId;
    private String refDocPoid;
    private String detRowId;
}
