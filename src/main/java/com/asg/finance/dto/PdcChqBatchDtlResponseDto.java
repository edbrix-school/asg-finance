package com.asg.finance.dto;

import lombok.*;

import java.sql.Date;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcChqBatchDtlResponseDto {

    private Long transactionPoid;
    private Long detRowId;

    private Date pdcChqDate;
    private String chqNumber;
    private Double chqAmount;
    private String remarks;

    private String bankPaymentPoid;
    private String bankPaymentRef;
    private String narration;
    private String billRef;

    private Long drGlPoid1;
    private Double drAmt1;
    private Long drGlPoid2;
    private Double drAmt2;
    private Long drGlPoid3;
    private Double drAmt3;

    private Long crGlPoid;
    private Double crAmt;

    private String costPoid;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
