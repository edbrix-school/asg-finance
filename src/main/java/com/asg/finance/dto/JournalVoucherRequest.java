package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalVoucherRequest {

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotBlank(message = "Ref type is required")
    private String refType;

    private String currencyCode;
    private BigDecimal currencyRate;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private BigDecimal bhdAmount;

    @NotBlank(message = "Posting narration is required")
    private String postingNarration;

    private Long wdvAccountGl;
    private Boolean multiCompany;
    private String remarks;
    @NotBlank(message = "Confidential remarks is required")
    private String confidentialRemarks;

    private List<JournalVoucherGlDetailDto> glDetails;
    private List<JournalVoucherAssetDetailDto> assetDetails;
    private List<JournalVoucherCapitalizationDto> assetCapitalization;
}
