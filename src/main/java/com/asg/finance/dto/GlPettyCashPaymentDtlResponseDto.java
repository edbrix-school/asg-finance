package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
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
public class GlPettyCashPaymentDtlResponseDto {
    private Long transactionPoid;
    private Long detRowId;
    private String type;
    private Long companyPoid;
    private DetailsDto companyPoidDtl;
    private Long glPoid;
    private DetailsDto glPoidDtl;
    private Long chargePoid;
    private DetailsDto chargePoidDtl;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String remarks;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private Long vatSupplier;
    private DetailsDto vatSupplierDtl;
    private String inputVatNumber;
    private LocalDate supplierInvDate;
    private Long taxPoid;
    private DetailsDto taxPoidDtl;
    private BigDecimal taxPercentage;
    private String vatPartyName;

    private List<BillwiseBreakupPopupRequestDto> billwiseBreakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterBreakupList;
}
