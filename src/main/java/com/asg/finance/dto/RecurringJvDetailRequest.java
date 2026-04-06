package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecurringJvDetailRequest {
    @NotBlank(message = "Type is mandatory")
    private String type;

    private Long companyPoid;

    @NotNull(message = "GL Poid is mandatory")
    private Long glPoid;

    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String remarks;
    private String actionType;
    private Long detRowId;

    private List<CostCenterBreakupPopupRequestDto> costCenter;
    private List<BillwiseBreakupPopupRequestDto> billWiseBreakup;
}
