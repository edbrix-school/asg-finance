package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.dto.TelexFileGenerateRequestDto;
import com.asg.finance.dto.TelexFileGenerateResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TelexFileGenerateService {
    TelexFileGenerateResponseDto createTelexFile(TelexFileGenerateRequestDto request);
    TelexFileGenerateResponseDto updateTelexFile(Long transactionPoid, TelexFileGenerateRequestDto request);
    TelexFileGenerateResponseDto getTelexFileById(Long transactionPoid);
    void softDeleteTelexFile(Long transactionPoid, DeleteReasonDto reasonDto);
    Map<String, Object> listTelexFiles(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);
    List<TelexFileDtlDto> loadTelexTransferData(String bankList);
    String regenerateTelexFile(Long telexTransactionPoid, Long debitVoucherPoid);
    String regenerateTelexFile(Long debitVoucherPoid);
    String generateBankFileButton(Long transactionPoid);
    String checkBankBalance(Long transactionPoid);
}
