package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalVoucherDetailResponse {
    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private String refType;
    private String currencyCode;
    private BigDecimal currencyRate;
    private BigDecimal amount;
    private BigDecimal bhdAmount;
    private String postingNarration;
    private Long wdvAccountGl;
    private Boolean multiCompany;
    private String remarks;
    private String confidentialRemarks;
    private String status;
    private List<GlDetailResponse> glDetails;
    private List<JournalVoucherAssetDetailDto> assetDetails;
    private List<JournalVoucherCapitalizationDto> assetCapitalization;
    private BigDecimal drTotal;
    private BigDecimal crTotal;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlDetailResponse {
        private Long detRowId;
        private String type;
        private String companyPoid;
        private String glPoid;
        private String glCode;
        private String glDescription;
        private BigDecimal drAmt;
        private BigDecimal crAmt;
        private String remarks;
        private List<CostCenterBreakupPopupRequestDto> costCenterBreakup;
        private List<BillwiseBreakupPopupRequestDto> billWiseBreakup;
    }
}
