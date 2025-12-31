package com.asg.finance.dto;

import lombok.Data;

@Data
public class HsbcApiTransactionDto {
    private String accountNumber;
    private String bookingDatetime;
    private String valueDatetime;
    private String statementReference;
    private String transactionReference;
    private String transInfo;
    private Double transactionAmountDr;
    private Double transactionAmountCr;
    private String uploadedDatetime;
    private String newTransaction;
}
