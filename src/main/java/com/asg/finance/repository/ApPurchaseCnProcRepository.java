package com.asg.finance.repository;

import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public interface ApPurchaseCnProcRepository {
    Map<String, Object> getPjRefDetails(Long pjPoid);
    Map<String, Object> getPartyDetails(String partyType, Long partyPoid);
    void beforeSaveValidation(String partyType, Long partyPoid, String refType, Long pjPoid);
}
