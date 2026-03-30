package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPettyCashPaymentGrnDtlResponseDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long grnPoid;
    private DetailsDto grnPoidDtl;
    private String checkAll;
    private BigDecimal amount;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    // Display fields from GRN source document (populated via GRN lookup)
    private LocalDate grnTrnDate;
    private String grnRef;
    private Long grnCompanyPoid;
    private Long supplierPoid;
    private DetailsDto supplierPoidDtl;
    private Long locationPoid;
    private DetailsDto locationPoidDtl;
    private String drilldownLinkInfo;
}
