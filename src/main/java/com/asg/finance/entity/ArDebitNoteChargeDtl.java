package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AR_DEBIT_NOTE_CHARGE_DTL")
@IdClass(ArDebitNoteChargeDtlId.class)
@Data
public class ArDebitNoteChargeDtl {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ArDebitNoteHdr debitNoteHdr;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "CHARGE_AMOUNT")
    private BigDecimal chargeAmount;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "REMARKS", length = 1250)
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

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "CN_REF_DOC_ID", length = 100)
    private String cnRefDocId;

    @Column(name = "CN_REF_DOC_POID", length = 300)
    private String cnRefDocPoid;

    @Column(name = "CN_REF_DET_ROW_ID", length = 300)
    private String cnRefDetRowId;

    @Column(name = "COST_AMOUNT")
    private BigDecimal costAmount;

    @Column(name = "COST_POID", length = 1000)
    private String costPoid;

    @Column(name = "COST_GROUP", length = 1000)
    private String costGroup;

    @Column(name = "PRINT_SEQ_NO")
    private Integer printSeqNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}