package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.ApPurchaseCnHdrDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Map;

public interface ApPurchaseCnService {
    ApPurchaseCnHdrDto create(ApPurchaseCnHdrDto dto);
    ApPurchaseCnHdrDto getById(Long transactionPoid);
    ApPurchaseCnHdrDto update(Long transactionPoid, ApPurchaseCnHdrDto dto);
    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);
    Map<String, Object> list(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);
    Map<String, Object> getPjRefDetails(Long pjPoid);
    Map<String, Object> getPartyDetails(String partyType, Long partyPoid);
    byte[] print(Long transactionPoid);
}
