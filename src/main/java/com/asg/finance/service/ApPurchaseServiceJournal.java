package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ApPurchaseServiceJournal {

    ApPurchaseInvoiceHdrDto fetchApPurchaseInvoiceHdr(Long transactionPoid);

    ApPurchaseInvoiceHdrDto createApPurchaseInvoice(ApPurchaseInvoiceHdrDto dto, String documentId);

    ApPurchaseInvoiceHdrDto updateApPurchaseInvoice(Long transactionPoid, ApPurchaseInvoiceHdrDto dto);

    ApPurchaseInvoiceHdrDto softDeleteApPurchaseInvoice(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<ApPurchaseJournalResponseDto> createFromFf(String ffPoidList, StringBuilder result);

    String updateFdaCost(String fdaPoid, Long piPoid);

    String updateFfCost(String ffPoid, Long piPoid);

    List<ApPurchaseJournalResponseDto> createFromFda(String fdaPoid, StringBuilder result);

    boolean validateVoucher(String docId, String refType, String refPoid);

    String validateBeforeSave(String docId, String refType, String refPoid);

    String updateMtaPoBookingDetails(String poPoid, Long bookPoid);

    List<ApPiFromPoResponseDto> createPiFromPo(String poPoid);

    List<ApPiFromGeneralPoResponseDto> createPiFromGeneralPo(String poPoid);

    List<ApPiFaDefaultDetailsDto> getFaDefaultDetails(String faPoid);

    String validateOutstandingPo(Long supplierPoid);

    String supplierPoidFromPo(String poPoid);

    List<ApPurchaseInvRjvDefaultDto> getRjvDefaultDetails(String rjvPoid);

    byte[] print(Long transactionPoid) throws Exception;

    String validateDuplicateInvoice(ApPurchaseInvoiceHdrDto dto, Long transactionPoid);

}
