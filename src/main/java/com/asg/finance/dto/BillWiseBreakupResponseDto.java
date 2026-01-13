package com.asg.finance.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillWiseBreakupResponseDto {

    private Long mainDetRowId;
    private Long billDetRowId;
    private Long glPoid;
    private String billRefType;
    private String billRef;
    private Date billDueDate;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String billRemarks;
    private String type;
}
