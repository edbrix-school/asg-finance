package com.asg.finance.repository;

import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.DrilldownLinkInfoDto;

import java.util.List;

public interface BankDepositVoucherProcRepository {
    void callBeforeSaveValidation(Long companyPoid, Long bankPoid);
    void callChequeStatusValidation(String refDocRef, Long refDocPoid);
    List<BankDepositVoucherDtlDto> loadPendingPayments(Long bankPoid, String type, String bankFilter);
    void markPaymentsCompleted(Long transactionPoid, String paymentType);
    void callApprovalProcedure(Long companyPoid, Long userPoid, Long transactionPoid, String docRef, java.time.LocalDate transactionDate);
    DrilldownLinkInfoDto buildDrilldownLinkInfo(String refDocId, Long refDocPoid, Long companyPoid);
}
