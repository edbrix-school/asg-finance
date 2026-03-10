package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.GlBankDebitChargeDtlId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "GL_BANK_DEBIT_CHARGE_DTL",
        uniqueConstraints = {
                @UniqueConstraint(name = "GL_BANK_DEBIT_CHARGE_PK", columnNames = {"TRANSACTION_POID", "DET_ROW_ID"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankDebitChargeDtl extends BaseEntity {

    @EmbeddedId
    private GlBankDebitChargeDtlId id;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "CHARGE_AMOUNT", precision = 15, scale = 2)
    private BigDecimal chargeAmount;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "REF_DOC_ID", length = 100)
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "FDA_DET_ROW_ID")
    private Long fdaDetRowId;

    @Column(name = "CHECK_ALL", length = 1)
    private String checkAll;

    @Column(name = "PDA_AMOUNT", precision = 15, scale = 2)
    private BigDecimal pdaAmount;

    @Column(name = "FF_AMOUNT", precision = 15, scale = 2)
    private BigDecimal ffAmount;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE", precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "CHARGE_BASE_AMOUNT", precision = 15, scale = 2)
    private BigDecimal chargeBaseAmount;
}
