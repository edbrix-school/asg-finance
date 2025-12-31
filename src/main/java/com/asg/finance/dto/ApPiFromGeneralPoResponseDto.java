package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApPiFromGeneralPoResponseDto {
    private String type;
    private Long companyPoid;
    private Long glPoid;
    private BigDecimal drAmount;
    private BigDecimal crAmount;
    private String remarks;

    private String refDocId;
    private String refDocPoid;
}
