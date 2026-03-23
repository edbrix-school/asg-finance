package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitNoteChargeDetailDto {
    private Long transactionPoid;
    
    private Long detRowId;
    
    private Long chargeId; // CHARGE_MASTER_IN_DN_FOR_SH
    
    private BigDecimal costAmount;

    private BigDecimal chargeAmount;
    
    private Long taxId; // DR_TAX_MASTER
    
    private BigDecimal taxPercentage;
    
    private BigDecimal taxAmount;
    
    private BigDecimal totalAmount;
    
    private String remarks;
    
    private Integer seqNo;
    
    private Boolean selected = false;

    private String actionType;

    private String checkAll;

    private String costPoid;

    private String costGroup;
    
    // LOV Details
    private LovGetListDto chargeDetails;
    private LovGetListDto taxDetails;
    private LovGetListDto costCenterDetails;

    public boolean isEmpty() {
        return transactionPoid == null && detRowId == null && chargeId == null && costAmount == null &&
               chargeAmount == null && taxId == null && taxPercentage == null && taxAmount == null &&
               remarks == null && seqNo == null && !selected && checkAll == null &&
               costPoid == null && costGroup == null && chargeDetails == null && taxDetails == null &&
               costCenterDetails == null;
    }
}