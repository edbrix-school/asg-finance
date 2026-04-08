package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "GL poid is required")
    private Long glPoid;

    @NotBlank(message = "Type is required")
    private String type;

    @NotNull(message = "Debit amount is required")
    private BigDecimal drAmt;

    @NotNull(message = "Credit amount is required")
    private BigDecimal crAmt;

    private Long detRowId;
    private String actionType;
    private Long companyPoid;
    private String remarks;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;
    private List<BillwiseBreakupPopupRequestDto> breakupList;
}
