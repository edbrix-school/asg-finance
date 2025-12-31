package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "AP_REQUEST_FOR_QTN_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "AP_REQUEST_FOR_QTN_HDR_UK1", columnNames = {"DOC_REF"})
        }
)
public class APRequestForQtnHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ap_request_for_qtn_hdr_seq")
    @SequenceGenerator(
            name = "ap_request_for_qtn_hdr_seq",
            sequenceName = "AP_REQUEST_FOR_QTN_HDR_SEQ",
            allocationSize = 1
    )
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "DOC_REF", nullable = false, length = 25)
    private String docRef;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private Double currencyRate;

    @Column(name = "EXPECTED_DATE")
    private LocalDate expectedDate;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "TYPE", length = 20)
    private String type;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "STATUS", length = 100)
    private String status;

    @Column(name = "SALES_QTN_POID")
    private Long salesQtnPoid;

    @Column(name = "DIVISION_POID")
    private Long divisionPoid;

    @Column(name = "DESCRIPTION_PRINT_YN", length = 1)
    private String descriptionPrintYn;

    @Column(name = "SALES_INV_POID")
    private Long salesInvPoid;

    @Column(name = "SALES_INV_DOC_REF", length = 50)
    private String salesInvDocRef;
}

