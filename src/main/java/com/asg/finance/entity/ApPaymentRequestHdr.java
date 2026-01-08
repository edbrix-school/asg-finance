package com.asg.finance.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "AP_PAYMENT_REQUEST_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ap_payment_request_hdr_seq")
    @SequenceGenerator(name = "ap_payment_request_hdr_seq", sequenceName = "AP_PAYMENT_REQUEST_HDR_SEQ", allocationSize = 1)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "REF_TYPE", length = 20)
    private String refType;

    @Column(name = "DOC_REFERENCE_POID")
    private Long docReferencePoid;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "PAYEE_POID")
    private Long payeePoid;

    @Column(name = "REQUESTED_BY", length = 200)
    private String requestedBy;

    @Column(name = "REAMRKS", length = 500)
    private String remarks;

    @Column(name = "ACC_RESPONSE_CATEGORY", length = 200)
    private String accResponseCategory;

    @Column(name = "ACC_RESPONSE", length = 500)
    private String accResponse;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted = "N";
}
