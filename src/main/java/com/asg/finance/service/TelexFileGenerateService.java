package com.asg.finance.service;

import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.dto.TelexFileGenerateRequestDto;
import com.asg.finance.dto.TelexFileGenerateResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface TelexFileGenerateService {
    TelexFileGenerateResponseDto createTelexFile(TelexFileGenerateRequestDto request);
    TelexFileGenerateResponseDto updateTelexFile(Long transactionPoid, TelexFileGenerateRequestDto request);
    TelexFileGenerateResponseDto getTelexFileById(Long transactionPoid);
    void softDeleteTelexFile(Long transactionPoid);
    Map<String, Object> listTelexFiles(String documentId, FilterRequestDto filters, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    List<TelexFileDtlDto> loadTelexTransferData(String bankList);
    String regenerateTelexFile(Long debitVoucherPoid);
    String validateAndMarkDeleted(Long transactionPoid, List<Long> detRowIds);
}
