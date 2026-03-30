package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PettyCashFromFfDto {
    private Long chargePoid;
    private BigDecimal chargeAmount;
    private BigDecimal ffAmount;
    private Long taxPoid;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
}
