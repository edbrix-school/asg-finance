package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankFileDetailProjection {
    private Long debitCompanyPoid;
    private Long debitTransactionPoid;
    private String debitDocRef;
    private String debitCurrencyCode;
    private String onlyApproval;
    private String ttSuppressBalanceCheck;
    private Long detRowId;
}
