package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "GL_BANK_MASTER_COMMISSION_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankCommissionDtlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "commissionDtlSeq")
    @SequenceGenerator(name = "commissionDtlSeq", sequenceName = "GL_BANK_MASTER_COMMISSION_DTL_SEQ", allocationSize = 1)
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "BANK_POID", nullable = false)
    private Long bankPoid;

    @Column(name = "PERIOD_FROM")
    @Temporal(TemporalType.DATE)
    private Date periodFrom;

    @Column(name = "PERIOD_TO")
    @Temporal(TemporalType.DATE)
    private Date periodTo;

    @Column(name = "COMMISSION_GL_POID")
    private Long commissionGlPoid;

    @Column(name = "COMMISSION_PERCENT", precision = 19, scale = 2)
    private BigDecimal commissionPercent;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE", precision = 19, scale = 2)
    private BigDecimal taxPercentage;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CREATED_BY", length = 200)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 200)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "CARD_TYPE", length = 500)
    private String cardType;

}

