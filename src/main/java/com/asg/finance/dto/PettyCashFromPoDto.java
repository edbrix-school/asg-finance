package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
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
    private DetailsDto stockPoidDtl;
    private Long stockUnitPoid;
    private DetailsDto stockUnitPoidDtl;
    private BigDecimal poQty;
    private BigDecimal price;
    private Long taxPoid;
    private DetailsDto taxPoidDtl;
    private String remarks;
    private String refDocId;
    private String refDocPoid;
    private Long refDetRowId;
}
