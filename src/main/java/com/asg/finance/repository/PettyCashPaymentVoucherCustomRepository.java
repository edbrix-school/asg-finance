package com.asg.finance.repository;

import com.asg.finance.dto.AdvanceDetailDto;

import java.math.BigDecimal;
import java.util.List;

public interface PettyCashPaymentVoucherCustomRepository {

    void validateGlVouchers(Long loginGroupPoid, Long loginUserPoid, Long loginCompanyPoid,
                            String docId, String refType, String refPoid, StringBuilder result);

    String validateVoucherBeforeDelete(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String refPoid
    );

    void updateCostFF(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            Long piPoid,
            StringBuilder result
    );

    void updateCostFDA(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            Long piPoid,
            StringBuilder result
    );

    void updatePurchaseOrderStatus(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            Long bookPoid,
            StringBuilder resultOut
    );

    void validateBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String glRefPoid,
            String glRefPoid2,
            String glRefPoid3,
            String partyType,
            Long partyPoid,
            StringBuilder taxInputGlPoid,
            StringBuilder result
    );

    void getOldJobReferences(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String transactionPoid,
            StringBuilder refTypeOut,
            StringBuilder refPoidOut
    );

    void validateJobBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String refPoid,
            StringBuilder result
    );

    void loadAdvanceDetails(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            BigDecimal amount,
            String advancePoid,
            StringBuilder result,
            List<AdvanceDetailDto> outData
    );

    void updateRfqPurchasePrice(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid,
            StringBuilder resultOut
    );

    void updateSalesGrnStatus(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long bookPoid,
            StringBuilder resultOut
    );

    String getRefTypeWhereClause(Long loginUserPoid);

}
