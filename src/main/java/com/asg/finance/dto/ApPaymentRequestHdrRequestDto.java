package com.asg.finance.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestHdrRequestDto {


    private Long groupPoid;
    private Long companyPoid;

    private LocalDate transactionDate;

    private String docRef;
    private String refType;
    private Long docReferencePoid;

    private String currencyCode;
    private BigDecimal currencyRate;

    private Long payeePoid;
    private String requestedBy;
    private String remarks;

    private String accResponseCategory;
    private String accResponse;

    private BigDecimal totalAmount;

    /* Detail lines */
    private List<ApPaymentRequestDtlRequestDto> details;
    private List<ApPaymentRequestStockDtlRequest> stockDetails;
}
