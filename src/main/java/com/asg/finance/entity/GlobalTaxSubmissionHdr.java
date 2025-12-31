package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "GLOBAL_TAX_SUBMISSION_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalTaxSubmissionHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "PERIOD_FROM", nullable = false)
    private Timestamp periodFrom;

    @Column(name = "PERIOD_TO", nullable = false)
    private Timestamp periodTo;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "PERIOD_CLOSED_BY", length = 300)
    private String periodClosedBy;

    @Column(name = "PERIOD_CLOSED_DATE")
    private Timestamp periodClosedDate;

    @Column(name = "DOC_REF", length = 100)
    private String docRef;

    @Column(name = "TRANSACTION_DATE")
    private Timestamp transactionDate;

    @Transient
    private String approvalStatus;

    @Transient
    private String status;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "CREATED_BY", length = 30)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 30)
    private String lastmodifiedBy;

    @UpdateTimestamp
    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastmodifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}


