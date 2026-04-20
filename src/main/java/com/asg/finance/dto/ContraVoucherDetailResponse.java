package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraVoucherDetailResponse {

    private Long detRowId;
    private String type; // Dr or Cr
    private Long companyPoid;
    private PoidDetailsDto companyPoidDetails; // POID, CODE, DESCRIPTION from GLOBAL_COMPANY_MASTER
    private Long glPoid;
    private PoidDetailsDto glPoidDetails; // POID, CODE, DESCRIPTION from GL_MASTER
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String remarks;
    private List<BillwiseBreakupPopupRequestDto> breakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterList;
}
