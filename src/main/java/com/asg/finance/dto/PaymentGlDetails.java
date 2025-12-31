package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PaymentGlDetails {
    private Long transactionPoid;

    private Long detRowId;
    private String type;
    private Long companyPoid;

    private LovGetListDto companyDet;

    private Long glPoid;

    private LovGetListDto glDet;

    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String partyInvNumber;
    private LocalDate partyInvDate;
    private String remarks;
    private String actionType;
    private List<BillwiseBreakupPopupRequestDto> breakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;
}