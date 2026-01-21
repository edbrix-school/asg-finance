package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_TAX_PERIOD_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxPeriodHdr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    @AuditIgnore
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    @AuditIgnore
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    @AuditIgnore
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25)
    @AuditIgnore
    private String docRef;

    @Column(name = "DESCRIPTION", nullable = false, length = 200)
    private String description;

    @Column(name = "PERIOD_FROM", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO", nullable = false)
    private LocalDate periodTo;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted;
}
