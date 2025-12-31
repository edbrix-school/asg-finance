package com.asg.finance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcBatchCreationProcRequest {

   // private Long companyPoid;
    private Long transactionPoid;
    private Integer noOfCheques;
    private Double chequeAmount;
    private String startChequeNo;
    private String startDate;       // expected format: dd-MMM-yyyy or yyyy-MM-dd
    private String prePrinted;      // Y or N
    //private String loginUser;
    private String narration;
    private String billRef;
}
