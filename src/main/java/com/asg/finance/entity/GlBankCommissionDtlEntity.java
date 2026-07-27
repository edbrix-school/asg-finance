package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import java.time.LocalDate;


@Entity
@Table(name = "GL_BANK_MASTER_COMMISSION_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankCommissionDtlEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "commissionDtlSeq")
    @SequenceGenerator(name = "commissionDtlSeq", sequenceName = "GL_BANK_MASTER_COMMISSION_DTL_SEQ", allocationSize = 1)
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "BANK_POID", nullable = false)
    private Long bankPoid;

    @Column(name = "PERIOD_FROM")
    //@Temporal(TemporalType.DATE)
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO")
    //@Temporal(TemporalType.DATE)
    private LocalDate periodTo;

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

    @Column(name = "CARD_TYPE", length = 500)
    private String cardType;

}

