package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.GlBankPaymentChargeDtlId;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "GL_BANK_PAYMENT_CHARGE_DTL")
@IdClass(GlBankPaymentChargeDtlId.class)
public class GlBankPaymentChargeDtlEntity extends BaseEntity implements Serializable {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "CHARGE_AMOUNT")
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

    @Column(name = "PDA_AMOUNT")
    private BigDecimal pdaAmount;

    @Column(name = "FF_AMOUNT")
    private BigDecimal ffAmount;

}