package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PdcBatchCreationExcelProcRequest {

    private Long transactionPoid;
    private Integer noOfCheques;
    private BigDecimal chequeAmount;
    private String startChequeNo;
    private LocalDate startDate;
    private String prePrinted;
    private String narration;
    private String billRef;
}
