package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalVoucherAssetDetailDto {
    private Long sn;
    private Long faPoid;
    private Integer lifeYear;
    private LocalDate purchaseDate;
    private LocalDate depreciationStartDate;
    private BigDecimal assetValue;
    private BigDecimal depreciatedAmt;
    private BigDecimal wdvValue;
    private String process;
    private LocalDate scrapSoldDate;
    private BigDecimal scrapSoldValue;
    private String remarks;

}
