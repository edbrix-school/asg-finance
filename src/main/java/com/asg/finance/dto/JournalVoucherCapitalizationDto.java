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
public class JournalVoucherCapitalizationDto {
    private Long sn;
    private String actionType;
    private Long faPoid;
    private String faDescription;
    private Long faCategory;
    private String assetType;
    private BigDecimal assetValue;
    private String remarks;

}
