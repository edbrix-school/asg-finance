package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.ApPaymentRequestHdrRequestDto;
import com.asg.finance.dto.ApPaymentRequestHdrResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ApPaymentRequestService {

    ApPaymentRequestHdrResponseDto create(ApPaymentRequestHdrRequestDto requestDto);

    ApPaymentRequestHdrResponseDto update(Long transactionPoid,
                                          ApPaymentRequestHdrRequestDto requestDto);

    ApPaymentRequestHdrResponseDto findById(Long transactionPoid);

    void delete(Long transactionPoid);

    Map<String, Object> listPaymentRequest(String documentId, FilterRequestDto filters, Pageable pageable, LocalDate periodFrom, LocalDate periodTo);
}
