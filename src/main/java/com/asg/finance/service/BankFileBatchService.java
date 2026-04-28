package com.asg.finance.service;

import com.asg.finance.dto.BankFileBatchResult;

public interface BankFileBatchService {
    BankFileBatchResult createBankFileBatch(Long transactionPoid, Long userPoid);
}
