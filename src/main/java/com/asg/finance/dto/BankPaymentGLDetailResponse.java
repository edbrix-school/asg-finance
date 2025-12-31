package com.asg.finance.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.asg.common.lib.dto.LovGetListDto;

@Data
public class BankPaymentGLDetailResponse {

    private Long detRowId;
    private Long transactionPoid;
    private String type;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private Long glPoid;
    private LovGetListDto glDet;
    private Double drAmt;
    private Double crAmt;
    private Long taxPoid;
    private LovGetListDto taxDet;
    private Double taxPercentage;
    private Double taxAmount;
    private Double totalAmount;
    private String partyInvNumber;
    private LocalDate partyInvDate;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;

    private List<BillwiseBreakupPopupRequestDto> billwiseBreakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterBreakupList;
}
