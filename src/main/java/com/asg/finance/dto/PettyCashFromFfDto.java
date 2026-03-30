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
public class PettyCashFromFfDto {
    private Long chargePoid;
    private DetailsDto chargePoidDtl;
    private BigDecimal chargeAmount;
    private BigDecimal ffAmount;
    private Long taxPoid;
    private DetailsDto taxPoidDtl;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
}
