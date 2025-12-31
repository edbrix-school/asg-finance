package com.asg.finance.repository;

import com.asg.finance.dto.CostCenterBreakupRequestDto;
import com.asg.finance.dto.GlVoucherCostCenterBreakupResponseDto;

import java.util.List;


public interface CostCenterBreakupDtlRepository {
    GlVoucherCostCenterBreakupResponseDto loadCostCenters(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long transactionPoid);

    void deleteCostCenters(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long transactionPoid,
            Long userPoid);

    void insertCostBreakup(List<CostCenterBreakupRequestDto> breakup);
}
