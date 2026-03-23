package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.DebitNoteHeaderDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface DebitNoteService {
    
    DebitNoteHeaderDto createDebitNote(DebitNoteHeaderDto debitNoteDto);
    
    DebitNoteHeaderDto updateDebitNote(Long transactionPoid, DebitNoteHeaderDto debitNoteDto);
    
    void deleteDebitNote(Long transactionPoid, DeleteReasonDto deleteReasonDto);
    
    DebitNoteHeaderDto getDebitNote(Long transactionPoid);
    
    Map<String, Object> listDebitNotes(FilterRequestDto filterRequest, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable);

    Map<String, Object> loadFdaCharges(Long fdaPoid);

    Map<String, Object> getChargeTax(Long chargeId, String partyType, Long partyPoid);

    void updateCostAmount(Long transactionPoid);

    Map<String, Object> checkSailDate(Long fdaPoid);

    Map<String, Object> getPartyDefaults(Long partyPoid, String partyType);

    byte[] print(Long transactionPoid) throws Exception;

    Map<String, Object> validateEditRequest(Long transactionPoid);

    Map<String, String> getCustomLovList(String costGroup);

}