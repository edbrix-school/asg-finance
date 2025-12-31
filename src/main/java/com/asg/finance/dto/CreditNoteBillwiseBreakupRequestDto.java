package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class CreditNoteBillwiseBreakupRequestDto {

    private Long glDetRowId;
    private String billRefType;
    private String billRef;
    private Date billDueDate;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String remarks;

    private Long groupPoid;
    private Long companyPoid;
    private String docId;
    private Long transactionPoid;
    private Long loginUserPoid;
}
