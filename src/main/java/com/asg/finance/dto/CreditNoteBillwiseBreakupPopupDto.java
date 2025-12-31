package com.asg.finance.dto;


import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class CreditNoteBillwiseBreakupPopupDto {

    private Long id;
    private Long glDetRowId;
    private String billNo;
    private Date billDate;
    private Date dueDate;
    private BigDecimal pendingAmount;
    private BigDecimal adjustedAmount;
    private String remarks;
}

