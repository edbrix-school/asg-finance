package com.asg.finance.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcChqBatchHdrResponseDto {

    private Long transactionPoid;
    private LocalDateTime transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private Long payGlPoid;
    private String payingTo;
    private String payingType;
    private String divisionCode;
    private Long bankPoid;
    private String chqStartNo;
    private LocalDate chqStartDate;
    private Double chqAmount;
    private Long noOfChqs;
    private Double totalAmount;
    private String narration;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private String billType;
    private String billRef;
    private String costGroup;
    private String costPoid;
    private String prePrinted;
    private String confidentialRemarks;
    private String accountPayee;
    private List<PdcChqBatchDtlResponseDto> chequeDetails;
}
