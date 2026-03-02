package com.asg.finance.service.impl;

import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.GlVoucherPendingBillwiseBreakupResponseDto;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.repository.BillwiseBreakupDtlRepository;
import com.asg.finance.service.BillwiseBreakupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillwiseBreakupServiceImpl implements BillwiseBreakupService {

    private final BillwiseBreakupDtlRepository billwiseBreakupDtlRepository;
    private final LoggingService loggingService;

    @Override
    public GlVoucherLoadBillwiseBreakupResponseDto loadBillwiseBreakup(Long groupPoid, Long companyPoid, String docId, Long transactionPoid) {
        GlVoucherLoadBillwiseBreakupResponseDto response = billwiseBreakupDtlRepository.loadBillwiseBreakup(
                groupPoid,
                companyPoid,
                docId,
                transactionPoid);

        return response;
    }

    @Override
    public GlVoucherPendingBillwiseBreakupResponseDto showPendingBillwiseBreakup(Long groupPoid, Long companyPoid, Long glPoid, LocalDate asOnDate) {
        return billwiseBreakupDtlRepository.showPendingBillwiseBreakup(
                groupPoid,
                companyPoid,
                glPoid,
                asOnDate
        );
    }

    @Override
    public GlVoucherPendingBillwiseBreakupResponseDto showAllPendingBillwiseBreakup(Long groupPoid, Long companyPoid, Long glPoid, LocalDate asOnDate) {
        return billwiseBreakupDtlRepository.showAllPendingBillwiseBreakup(
                groupPoid,
                companyPoid,
                glPoid,
                asOnDate
        );
    }

    @Override
    public void insertBillwiseBreakup(List<BillwiseBreakupRequestDto> breakupList) {
        if (breakupList == null || breakupList.isEmpty()) {
            throw new IllegalArgumentException("No billwise breakup entries provided");
        }
        BillwiseBreakupRequestDto first = breakupList.get(0);
        if (first.getGroupPoid() == null || first.getCompanyPoid() == null ||
                first.getDocId() == null || first.getTransactionPoid() == null) {
            throw new IllegalArgumentException("Missing mandatory fields in billwise breakup data");
        }
        billwiseBreakupDtlRepository.insertBillwiseBreakup(breakupList);

        // Log the creation
        String key = first.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, first.getDocId(), key);
    }

    @Override
    public void deleteBillwiseBreakup(Long groupPoid, Long companyPoid, String docId, Long transactionPoid, Long loginUserPoid) {
          billwiseBreakupDtlRepository.deleteBillwiseBreakup(
                  groupPoid,
                  companyPoid,
                  docId,
                  transactionPoid,
                  loginUserPoid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBillwiseBreakups(List<BillwiseBreakupRequestDto> request, Long userPoid) {

        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException("No cost center breakup entries provided");
        }

        BillwiseBreakupRequestDto first = request.get(0);

        if (first.getGroupPoid() == null || first.getCompanyPoid() == null ||
                first.getDocId() == null || first.getTransactionPoid() == null) {
            throw new IllegalArgumentException("Missing mandatory fields in breakup data");
        }

        deleteBillwiseBreakup(
                first.getGroupPoid(),
                first.getCompanyPoid(),
                first.getDocId(),
                first.getTransactionPoid(),
                first.getLoginUserPoid()
        );

        billwiseBreakupDtlRepository.insertBillwiseBreakup(request);
    }
}
