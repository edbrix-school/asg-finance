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
public class UpdateAssetDetailRequest {
    private Long faPoid;
    private String process;
    private BigDecimal scrapSoldValue;
    private LocalDate scrapSoldDate;
    private String remarks;
}
