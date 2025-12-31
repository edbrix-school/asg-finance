package com.asg.finance.dto;

import lombok.Data;

@Data
public class HsbcApiBalanceDto {
    private String accountNumber;
    private String balanceDatetime;
    private String balanceType;
    private Double balanceAmount;
    private String balanceCurrency;
    private Double creditAmount;
    private String uploadedDatetime;
}
