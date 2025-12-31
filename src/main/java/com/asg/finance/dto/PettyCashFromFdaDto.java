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
public class PettyCashFromFdaDto {
    private Long chargePoid;
    private BigDecimal pdaAmount;
    private Long taxPoid;
    private String remarks;
    private String refDocId;
    private String refDocPoid;
    private Long fdaDetRowId;
}
