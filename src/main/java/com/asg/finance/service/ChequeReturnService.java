package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.ChequeReturnEditRequest;
import com.asg.finance.dto.ChequeReturnRequest;
import com.asg.finance.dto.ChequeReturnResponse;
import com.asg.finance.dto.ChequeReturnLoadResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ChequeReturnService {
    ChequeReturnResponse createChequeReturn(ChequeReturnRequest request);
    ChequeReturnResponse updateChequeReturnMinimal(Long transactionPoid, ChequeReturnEditRequest request);
    ChequeReturnResponse updateChequeReturnV2(Long transactionPoid, ChequeReturnRequest request);
    ChequeReturnResponse getChequeReturn(Long transactionPoid);
    void softDeleteChequeReturn(Long transactionPoid, DeleteReasonDto deleteReasonDto);
    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable);
    ChequeReturnLoadResponseDto loadChequeData(String chequeNumber, String receiptNo);
}
