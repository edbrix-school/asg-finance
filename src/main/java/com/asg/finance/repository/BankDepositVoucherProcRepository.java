package com.asg.finance.repository;

import com.asg.finance.dto.BankDepositVoucherDtlDto;

import java.util.List;

public interface BankDepositVoucherProcRepository {
    void callBeforeSaveValidation(Long companyPoid, Long bankPoid);
    void callChequeStatusValidation(String refDocRef, Long refDocPoid);
    List<BankDepositVoucherDtlDto> loadPendingPayments(Long bankPoid, String type, String bankFilter);
    void markPaymentsCompleted(Long transactionPoid, String paymentType);
}
