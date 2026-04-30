package com.asg.finance.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BankPayFdaItemDto {
    private Long chargePoid;
    private BigDecimal pdaAmount;
    private String remarks;
    private String refDocId;
    private String refDocPoid;
    private String detRowId;
}
