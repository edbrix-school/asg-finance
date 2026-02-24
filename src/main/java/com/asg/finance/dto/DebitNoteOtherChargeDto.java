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
    
    // LOV Details
    private LovGetListDto chargeDetails;
    private LovGetListDto taxDetails;
    private LovGetListDto costCenterDetails;
}