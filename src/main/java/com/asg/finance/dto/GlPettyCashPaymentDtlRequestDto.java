package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPettyCashPaymentDtlRequestDto {

    private Long detRowId;

    private String type;
    private Long companyPoid;


    private Long glPoid;
    private Long chargePoid;

    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;

    private Long vatSupplier;
    private String inputVatNumber;
    private LocalDate supplierInvDate;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private String vatPartyName;


    private String remarks;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"

    private List<BillwiseBreakupPopupRequestDto> billwiseBreakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterBreakupList;
}
