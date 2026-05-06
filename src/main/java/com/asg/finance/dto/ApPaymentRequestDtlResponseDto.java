package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestDtlResponseDto {

    private Long transactionPoid;
    private Long detRowId;

    private Long chargePoid;
    private LovGetListDto chargeLov;

    private BigDecimal amount;
    private BigDecimal vatPer;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;

    private String remarks;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
