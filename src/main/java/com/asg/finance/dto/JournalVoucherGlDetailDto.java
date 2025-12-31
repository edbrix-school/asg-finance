package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalVoucherGlDetailDto {
    private Long detRowId;
    private String actionType;
    private String type;
    private Long companyPoid;
    private Long glPoid;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String remarks;
    private List<CostCenterBreakupPopupRequestDto> costCenterBreakup;
    private List<BillwiseBreakupPopupRequestDto> billWiseBreakup;
}
