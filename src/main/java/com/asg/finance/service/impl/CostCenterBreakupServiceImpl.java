package com.asg.finance.service.impl;

import com.asg.finance.dto.CostCenterBreakupRequestDto;
import com.asg.finance.dto.GlVoucherCostCenterBreakupResponseDto;
import com.asg.finance.repository.CostCenterBreakupDtlRepository;
import com.asg.finance.service.CostCenterBreakupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostCenterBreakupServiceImpl implements CostCenterBreakupService {

    private final CostCenterBreakupDtlRepository costCenterRepository;

    public GlVoucherCostCenterBreakupResponseDto loadCostCenterData(String docId, Long transactionPoid,Long groupPoid, Long companyPoid,Long userPoid) {

        GlVoucherCostCenterBreakupResponseDto breakupList = costCenterRepository.loadCostCenters(
                groupPoid,
                companyPoid,
                docId,
                transactionPoid
        );


        return breakupList;
    }

    @Override
    public void deleteCostCenterData(String docId, Long transactionPoid, Long groupPoid, Long companyPoid, Long userPoid) {

        costCenterRepository.deleteCostCenters(
                groupPoid,
                companyPoid,
                docId,
                transactionPoid,
                userPoid
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCostCenterBreakups(List<CostCenterBreakupRequestDto> request) {

        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException("No cost center breakup entries provided");
        }
        CostCenterBreakupRequestDto first = request.get(0);
        if (first.getGroupPoid() == null || first.getCompanyPoid() == null ||
                first.getDocId() == null || first.getTransactionPoid() == null) {
            throw new IllegalArgumentException("Missing mandatory fields in breakup data");
        }

        costCenterRepository.insertCostBreakup(request);


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCostCenterBreakups(List<CostCenterBreakupRequestDto> request, Long userPoid) {

        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException("No cost center breakup entries provided");
        }

        CostCenterBreakupRequestDto first = request.get(0);

        if (first.getGroupPoid() == null || first.getCompanyPoid() == null ||
                first.getDocId() == null || first.getTransactionPoid() == null) {
            throw new IllegalArgumentException("Missing mandatory fields in breakup data");
        }

        deleteCostCenterData(
                first.getDocId(),
                first.getTransactionPoid(),
                first.getGroupPoid(),
                first.getCompanyPoid(),
                userPoid
        );

        costCenterRepository.insertCostBreakup(request);
    }



}
