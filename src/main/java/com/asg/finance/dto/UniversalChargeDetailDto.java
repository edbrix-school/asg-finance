package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UniversalChargeDetailDto {
    private Long transactionPoid;
    private Long detRowId;
    /**
     * LOVs for ChargePoid
     * If Ref Type FF_INVOICE -> CHARGE_MASTER_IN_CN_FOR_FF
     * If Ref Type SH_INVOICE -> CHARGE_MASTER_IN_CN_FOR_SH
     * If Ref Type DN_INVOICE -> CHARGE_MASTER_IN_CN_FOR_DN
     */
    private Long chargePoid;
    private LovGetListDto chargeDet;
    private BigDecimal chargeAmount;
    private BigDecimal chargeCostAmount;
    private BigDecimal pdaAmount;
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remarks;
    private String issueInvoice;   // "Y" or "N"
    private String selected;
    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;

    private String actionType; 
}


