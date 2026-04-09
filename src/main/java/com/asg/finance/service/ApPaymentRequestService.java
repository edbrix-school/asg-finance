package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.ApPaymentRequestDetailResponse;
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

    ApPaymentRequestDetailResponse findDetailsByRefId(Long transactionPoid, String refType);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listPaymentRequest(String documentId, FilterRequestDto filters, Pageable pageable, LocalDate periodFrom, LocalDate periodTo);

    Map<String, Object> createFromPo(String poPoid);

    Map<String, Object> createFromFf(String ffPoid);


    Map<String, Object> createFromFda(String fdaPoid);

    Map<String, Object> createFromMta(String poPoid);
}
