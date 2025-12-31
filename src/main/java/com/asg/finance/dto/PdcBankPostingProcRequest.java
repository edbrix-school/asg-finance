package com.asg.finance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcBankPostingProcRequest {
    //private Long companyPoid;
    private Long transactionPoid;
    //private Long groupPoid;
    private Long payGlPoid;
    private String payingTo;
    private Long bankPoid;
    private Long noOfChqs;
    private Double chequeAmount;
    private String startChequeNo;
    private String startDate;    // VARCHAR2 date
    //private String loginUser;
}
