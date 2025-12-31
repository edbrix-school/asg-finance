package com.asg.finance.repository;

import java.util.Map;

public interface DebitNoteCustomRepository {

    Map<String, Object> loadFdaCharges(Long fdaPoid);

    Map<String, Object> getTaxPercentage(Long chargeId, String partyType, Long partyPoid);

    void updateCostAmount(Long transactionPoid);

    Map<String, Object> checkSailDate(Long fdaPoid);

    Map<String, Object> getPartyDefaults(Long partyPoid, String partyType);

    void validateDebitNote(String refType, String partyType,
                           Long partyPoid, Long fdaRefPoid, String poRef);
}
