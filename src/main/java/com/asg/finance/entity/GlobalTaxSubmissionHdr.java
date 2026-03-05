package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_TAX_SUBMISSION_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalTaxSubmissionHdr extends BaseEntity {

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
    private LocalDateTime transactionDate;

    @Transient
    private String approvalStatus;

    @Transient
    private String status;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}


