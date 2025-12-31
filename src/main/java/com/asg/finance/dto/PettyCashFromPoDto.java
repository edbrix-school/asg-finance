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
public class PettyCashFromPoDto {
    private Long stockPoid;
    private Long stockUnitPoid;
    private BigDecimal poQty;
    private BigDecimal price;
    private Long taxPoid;
    private String remarks;
    private String refDocId;
    private String refDocPoid;
    private Long refDetRowId;
}
