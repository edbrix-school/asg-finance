package com.asg.finance.service;

import com.asg.finance.dto.CreditNoteHeaderDto;
import com.asg.finance.dto.DefaultCreditValuesDto;
import com.asg.finance.dto.UniversalChargeDetailDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CreditNoteService {
    
    CreditNoteHeaderDto createCreditNote(CreditNoteHeaderDto creditNoteDto);
    
    CreditNoteHeaderDto getCreditNoteById(Long transactionPoid);
    
    Map<String, Object> listCreditNotes(String docId, FilterRequestDto filters, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable);
    
    CreditNoteHeaderDto updateCreditNote(Long transactionPoid, CreditNoteHeaderDto creditNoteDto);
    
    void deleteCreditNote(Long transactionPoid);
    
    List<UniversalChargeDetailDto> getFFInvoiceCharges(Long refNo, Long partyPoid);
    
    List<UniversalChargeDetailDto> getSHInvoiceCharges(Long refNo, Long partyPoid);
    
    List<UniversalChargeDetailDto> getDNInvoiceCharges(Long refNo, Long partyPoid);
    
    List<UniversalChargeDetailDto> getFDADetails(Long fdaRef, Long partyPoid);
    
    DefaultCreditValuesDto getDefaultCreditValues(Long partyPoid, String partyType);
    
    Long getPartyGLPoid(Long partyPoid, String partyType);

    byte[] print(Long transactionPoid) throws Exception;

}