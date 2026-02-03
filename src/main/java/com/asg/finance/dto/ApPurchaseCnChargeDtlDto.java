package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ApPurchaseCnChargeDtlDto {
    private Long detRowId;
    private Long chargePoid;
    private BigDecimal chargeAmount;
    private String description;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
    private String checkAll;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal chargeBaseAmount;
    private String chargeFrom;
    private Long supplierPoidFf;
    
    // Action type field
    private String actionType; // isCreated, isUpdated, isDeleted
    
    // LOV details
    private LovGetListDto chargeDet;
    private LovGetListDto taxDet;
    private LovGetListDto refDocDet;
}
