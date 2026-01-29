package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal drAmt;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal crAmt;

    /**
     * LOV - CR_TAX_MASTER
     */
    @NotNull(message = "Tax Poid mandatory")
    private Long taxPoid;
    private LovGetListDto taxDet;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal taxPercentage;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal taxAmount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal totalAmount;
    private String remarks;

    private List<BillwiseBreakupPopupRequestDto> breakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;
}