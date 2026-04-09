package com.asg.finance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcBankPostingProcRequest {

    private Long transactionPoid;

    private Long payGlPoid;
    private String payingTo;
    private Long bankPoid;
    private Long noOfChqs;
    private Double chequeAmount;
    private String startChequeNo;
    private String startDate;    // VARCHAR2 date

}
