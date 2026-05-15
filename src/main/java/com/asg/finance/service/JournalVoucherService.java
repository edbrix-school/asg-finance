package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Map;

public interface JournalVoucherService {
    JournalVoucherResponse createJournalVoucher(JournalVoucherRequest request,String docId);

    JournalVoucherResponse updateJournalVoucher(Long transactionPoid, JournalVoucherRequest request,String docId);

    JournalVoucherDetailResponse getJournalVoucherById(Long transactionPoid);

    void deleteJournalVoucher(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listJournalVouchers(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    JournalVoucherAssetDetailDto getAssetDepreciationDetails(Long faPoid);

    JournalVoucherAssetCapitalizationResponseDto getAssetCapitalizationDetails(Long faPoid);

    byte[] print(Long transactionPoid) throws Exception;
}
