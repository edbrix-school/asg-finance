package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitNoteOtherChargeDto {
    
    private Long id;
    
    private Long chargeId; // DEBIT_NOTE_OTHER_CHARGES
    
    private BigDecimal saleAmount;
    
    private Long taxId; // DR_TAX_MASTER
    
    private BigDecimal taxPercentage;
    
    private BigDecimal taxAmount;
    
    private BigDecimal totalAmount;
    
    private BigDecimal costAmount;
    
    private Long costCenterPoid; // DN_GL_COST_CENTRE
    
    private String remarks;

    private Integer seqNo;

    private Boolean selected = false;

    private Long fdaDetRowId;

    private String refDocId;

    private Long refDocPoid;
    
    // LOV Details
    private LovGetListDto chargeDetails;
    private LovGetListDto taxDetails;
    private LovGetListDto costCenterDetails;

    public boolean isEmpty() {
        return id == null && chargeId == null && saleAmount == null && taxId == null &&
               taxPercentage == null && taxAmount == null && totalAmount == null && costAmount == null &&
               costCenterPoid == null && remarks == null && seqNo == null && !selected &&
               chargeDetails == null && taxDetails == null && costCenterDetails == null;
    }
}