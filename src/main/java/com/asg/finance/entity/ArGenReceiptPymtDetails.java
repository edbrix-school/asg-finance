package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "AR_GEN_RECEIPT_PYMT_DETAILS")
@IdClass(ArGenReceiptPymtDetails.CompositeKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArGenReceiptPymtDetails extends BaseEntity {

    @Id
    @AuditIgnore
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @AuditIgnore
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", nullable = false, insertable = false, updatable = false)
    @AuditIgnore
    private ArGenReceiptHdr receiptHdr;

    @Column(name = "PYMT_TYPE", length = 100)
    private String pymtType;

    @Column(name = "CHQ_CARDNO", length = 50)
    private String chqCardno;

    @Column(name = "CHQ_DATE")
    private LocalDate chqDate;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "ACCOUNT_POID")
    private Long accountPoid;

    @Column(name = "ACCOUNT_NAME", length = 100)
    private String accountName;

    @Column(name = "ACCOUNT_NO", length = 50)
    private String accountNo;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "GL_POID")
    @AuditIgnore
    private Long glPoid;

    @Column(name = "TT_BANK_POID")
    private Long ttBankPoid;

    @Column(name = "LINE_TYPE", length = 30)
    @AuditIgnore
    private String lineType;

    @Column(name = "TT_REF", length = 500)
    @AuditIgnore
    private String ttRef;

    @Column(name = "CREDIT_CARD_REF", length = 500)
    @AuditIgnore
    private String creditCardRef;

    @Column(name = "CARD_TYPE", length = 500)
    @AuditIgnore
    private String cardType;

    @Column(name = "CARD_POID")
    @AuditIgnore
    private Long cardPoid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}

