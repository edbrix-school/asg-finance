package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ApPurchaseCnGlDtlDto {
    private Long detRowId;
    private String type; // DR or CR
    private Long companyPoid; // When multiCompany = true
    private Long glPoid;
    private String glDescription;
    private BigDecimal drAmount;
    private BigDecimal crAmount;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remarks;
    private String description;
    private String refDocId;
    private Long refDocPoid;
    
    // Action type field
    private String actionType; // isCreated, isUpdated, isDeleted

    private List<BillwiseBreakupPopupRequestDto> breakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;
    
    // LOV details
    private LovGetListDto companyDet;
    private LovGetListDto glDet;
    private LovGetListDto typeDet;
    private LovGetListDto taxDet;
}
