package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreditNoteGLDetailDto {
    private Long transactionPoid;
    private Long detRowId;
    private String type;

    @NotNull(message = "Company Poid is mandatory")
    private Long companyPoid;

    private Long glPoid;
    private LovGetListDto glDet;
    private BigDecimal drAmt;
    private BigDecimal crAmt;

    /**
     * LOV - CR_TAX_MASTER
     */
    @NotNull(message = "Tax Poid mandatory")
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remarks;

    private String actionType; 

    private List<BillwiseBreakupPopupRequestDto> breakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;
}