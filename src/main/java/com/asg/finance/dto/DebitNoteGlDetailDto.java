package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DebitNoteGlDetailDto {
    
    private Long transactionPoid;
    
    private Long detRowId;
    
    @NotNull
    private String type; // ACC_TYPE_SHORT
    
    private Long companyPoid; // For multi-company
    
    @NotNull
    private Long glId; // GL_MASTER_LEDGERS_A_L
    
    private BigDecimal debitAmount;
    
    private BigDecimal creditAmount;
    
    private Long taxId; // DR_TAX_MASTER
    
    private BigDecimal taxPercentage;
    
    private BigDecimal taxAmount;
    
    private BigDecimal totalAmount;
    
    private String remarks;

    private String actionType; 

    private List<BillwiseBreakupPopupRequestDto> breakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;

    // LOV Details
    private LovGetListDto typeDetails;
    private LovGetListDto glDetails;
    private LovGetListDto taxDetails;
    private LovGetListDto companyDetails;

    public boolean isEmpty() {
        return transactionPoid == null && detRowId == null && type == null && companyPoid == null &&
               glId == null && debitAmount == null && creditAmount == null && taxId == null &&
               taxPercentage == null && taxAmount == null && totalAmount == null && remarks == null && breakupList == null && costCenterList == null &&
               typeDetails == null && glDetails == null && taxDetails == null && companyDetails == null;
    }
}