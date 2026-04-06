package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcChqBatchHdrRequestDto {
    private LocalDateTime transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    @NotNull(message = "payGlPoid is mandatory")
    private Long payGlPoid;
    @NotBlank(message = "payingTo is mandatory")
    private String payingTo;
    private String payingType;
    private String divisionCode;
    @NotNull(message = "bankPoid is mandatory")
    private Long bankPoid;
    private String chqStartNo;
    @NotNull(message = "chqStartDate is mandatory")
    private LocalDate chqStartDate;
    @NotNull(message = "chqAmount is mandatory")
    private Double chqAmount;
    @NotNull(message = "noOfChqs is mandatory")
    private Long noOfChqs;
    private Double totalAmount;
    @NotBlank(message = "narration is mandatory")
    private String narration;
    private String billType;
    private String billRef;
    private String costGroup;
    private String costPoid;
    private String prePrinted;
    private String accountPayee;
    private String confidentialRemarks;

    private List<PdcChqBatchDtlRequestDto> chequeDetails;
}
