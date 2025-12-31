package com.asg.finance.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ChequeReturnDataDto {

    private Long paymentMainPoid;
    private Double amount;
    private Long choPoid;
    private LocalDate choDate;
    private String pymtType;
    private String chqCardNo;
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
