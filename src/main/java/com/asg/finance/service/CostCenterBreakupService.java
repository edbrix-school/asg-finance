package com.asg.finance.service;

import com.asg.finance.dto.CostCenterBreakupRequestDto;
import com.asg.finance.dto.GlVoucherCostCenterBreakupResponseDto;

import java.util.List;

public interface CostCenterBreakupService {
    GlVoucherCostCenterBreakupResponseDto loadCostCenterData(String docId, Long transactionPoid,Long groupPoid, Long companyPoid,Long userPoid);
    void deleteCostCenterData(String docId, Long transactionPoid, Long groupPoid, Long companyPoid, Long userPoid);
    void saveCostCenterBreakups(List<CostCenterBreakupRequestDto> request);
    void updateCostCenterBreakups(List<CostCenterBreakupRequestDto> request, Long userPoid);
}
