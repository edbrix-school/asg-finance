package com.asg.finance.service;

public interface BankFileAubService {
    String createBankFileBatchAub(Long transactionPoid, Long userPoid, Long companyPoid);
}
