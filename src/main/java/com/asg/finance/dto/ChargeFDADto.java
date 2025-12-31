package com.asg.finance.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ChargeFDADto {

    private Long chargePoid;
    private BigDecimal pdaAmount;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
}
