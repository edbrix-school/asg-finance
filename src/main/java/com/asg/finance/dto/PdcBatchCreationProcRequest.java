package com.asg.finance.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcBatchCreationProcRequest {

   // private Long companyPoid;
    private Long transactionPoid;
    private Long noOfCheques;
    private Double chequeAmount;
    private String startChequeNo;
    private LocalDate startDate;       // expected format: dd-MMM-yyyy or yyyy-MM-dd
    private String prePrinted;      // Y or N
    //private String loginUser;
    private String narration;
    private String billRef;
}
