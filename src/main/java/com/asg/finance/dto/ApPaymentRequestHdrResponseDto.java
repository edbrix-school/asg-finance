package com.asg.finance.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestHdrResponseDto {

    private Long transactionPoid;
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

    private String deleted;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    /* 🔽 Detail lines */
    private List<ApPaymentRequestDtlResponseDto> details;
    private List<ApPaymentRequestStockDtlResponse> stockDetails;

}
