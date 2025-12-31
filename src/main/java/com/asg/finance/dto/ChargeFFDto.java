package com.asg.finance.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ChargeFFDto {

    private Long chargePoid;
    private BigDecimal chargeAmount;
    private BigDecimal ffAmount;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
}
