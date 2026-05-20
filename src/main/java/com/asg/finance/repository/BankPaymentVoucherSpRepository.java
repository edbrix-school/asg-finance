package com.asg.finance.repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

public interface BankPaymentVoucherSpRepository {
    
    void validateBeforeSave(Long transactionPoid, Long groupPoid, Long companyPoid, String userCode, String suppressBalanceCheck);
    
    String validateJob(Long groupPoid, Long userPoid, Long companyPoid, String docId, String refType, String refPoid);

    Map<String, BigDecimal> getBankBalance(String docId,
                                           Long docKeyPoid,
                                           LocalDate docDate,
                                           Long bankPoid);
    
    String getNextChequeNumber(Long bankPoid);
    
    void updateFdaCost(Long groupPoid, Long companyPoid, Long userPoid, String fdaPoid, Long piPoid);
    
    void updateFfCost(Long groupPoid, Long companyPoid, Long userPoid, String ffPoid, Long piPoid);
    
    void updateMtaCost(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid, String rfqPoid);
    
    Map<String, String> validateBeforeChequePrint(Long groupPoid, Long loginUserPoid, Long companyPoid, Long bankPoid, String chqSignType, Long transactionPoid, String suppressBalanceCheck);
    
    void afterChequePrint(Long groupPoid, String loginUser, Long companyPoid, Long transactionPoid, Long bankPoid, String chqSignType, Long userPoid);
    
    void releaseCheque(Long groupPoid, String loginUser, Long companyPoid, Long transactionPoid, String releasedTo, String contact);
    
    void unReleaseCheque(Long groupPoid, Long loginUser, Long companyPoid, Long transactionPoid);
    
    void resetChequeStatus(Long groupPoid, Long companyPoid, Long userPoid, Long docKeyPoid);
    
    String revertReconciliation(Long groupPoid, Long companyPoid, Long userPoid, String docId, String transactionPoid, String mailAlert, String comments);
    
    void releaseOldJobValues(Long groupPoid, Long userPoid, Long companyPoid, String docId, String transactionPoid);
    
    String validateVoucherStatus(Long groupPoid, Long userPoid, Long companyPoid, String docId, String refType, String refPoid);
}
