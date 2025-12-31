package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ChargeDetailDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long chargePoid;
    private LovGetListDto chargeDet;
    private BigDecimal chargeAmount;
    private String description;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
    private String checkAll;
    private BigDecimal pdaAmount;
    private BigDecimal ffAmount;
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal chargeBaseAmount;
    private String actionType;
}