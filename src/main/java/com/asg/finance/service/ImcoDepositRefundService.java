package com.asg.finance.service;


import com.asg.common.lib.dto.response.GlPostingViewResponseDto;
import com.asg.finance.dto.ImcoDepositRefundRequestDTO;
import com.asg.finance.dto.ImcoDepositRefundResponseDTO;

import com.asg.finance.dto.ImcoRefundLoadResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

public interface ImcoDepositRefundService {
    ImcoDepositRefundResponseDTO createImcoDepositRefund(ImcoDepositRefundRequestDTO request);
    ImcoDepositRefundResponseDTO getImcoDepositRefundById(Long transactionPoid);
    void softDeleteImcoDepositRefund(Long transactionPoid, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto);
    Map<String, Object> listImcoDepositRefund(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);
    ImcoRefundLoadResponseDto getChequeDetails(Long receiptPoid, String receiptNumber) throws SQLException;
    GlPostingViewResponseDto getGlPostingDetails(String docId, Long transactionPoid);
    byte[] print(Long transactionPoid) throws Exception;
}