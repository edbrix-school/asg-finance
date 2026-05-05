package com.asg.finance.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BankPayFfItemDto {
    private Long chargePoid;
    private BigDecimal chargeAmount;
    private BigDecimal ffAmount;
    private String refDocId;
    private String refDocPoid;
    private String detRowId;
}
