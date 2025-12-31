package com.asg.finance.dto;



import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class BillwiseBreakupDtoCreditNotePopUp {
    private Long id;
    private Long companyPoid;
    private Long billRefPoid;
    private String billRefNo;
    private String billRefType;
    private BigDecimal originalAmount;
    private BigDecimal adjustedAmount;
    private BigDecimal balanceAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Long glDetRowId;     //
    private Long transactionPoid;
    private String remarks;

    public BillwiseBreakupDtoCreditNotePopUp() {}

}

