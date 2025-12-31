package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChequeDetailDto {
    private Long paymentMainPoid;
    private BigDecimal amount;
    private Long choPoid;
    private LocalDate choDate;
    private String pymtType;
    private String chqCardno;
    private LocalDate chqDate;
    private Long bankPoid;
    private Long addressPoid;
    private String chqAcName;
    private LocalDate rcpDate;
    private String chqAcNo;
    private String remarks;
    private String refDocRef;
    private String refDocId;
    private Long refDocPoid;
}