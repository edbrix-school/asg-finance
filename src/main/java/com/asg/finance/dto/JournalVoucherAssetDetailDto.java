package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Asset poid is required")
    private Long faPoid;

    @NotBlank(message = "Process is required")
    private String process;
    private Long detRowId;
    private String actionType;
    private Integer lifeYear;
    private LocalDate purchaseDate;
    private LocalDate depreciationStartDate;
    private BigDecimal assetValue;
    private BigDecimal depreciatedAmt;
    private BigDecimal wdvValue;
    private LocalDate scrapSoldDate;
    private BigDecimal scrapSoldValue;
    private String remarks;

}
