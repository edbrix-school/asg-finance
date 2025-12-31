package com.asg.finance.service;

public interface LedgerPostingService {
    void postToLedger(Long transactionPoid, String docId);
}
