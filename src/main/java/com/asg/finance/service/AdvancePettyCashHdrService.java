package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.AdvancePettyCashHdrRequestDTO;
import com.asg.finance.dto.AdvancePettyCashHdrResponseDTO;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface AdvancePettyCashHdrService {
    AdvancePettyCashHdrResponseDTO createAdvancePettyCash(AdvancePettyCashHdrRequestDTO request);
    AdvancePettyCashHdrResponseDTO updateAdvancePettyCash(Long transactionPoid, AdvancePettyCashHdrRequestDTO request);
    AdvancePettyCashHdrResponseDTO getAdvancePettyCashById(Long transactionPoid);
    void softDeleteAdvancePettyCash(Long transactionPoid, DeleteReasonDto deleteReasonDto);
    Map<String, Object> listAdvancePettyCash(String documentId, FilterRequestDto filters, Pageable pageable, LocalDate periodFrom, LocalDate periodTo);
}