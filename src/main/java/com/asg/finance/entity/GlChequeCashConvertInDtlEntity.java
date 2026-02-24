package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.finance.entity.key.GlChequeCashConvertInDtlKey;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_CHEQUE_CASH_CONVERT_IN_DTL")
@Data
public class GlChequeCashConvertInDtlEntity {

    @EmbeddedId
    @AuditIgnore
    private GlChequeCashConvertInDtlKey id;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "CHQ_AC_NAME")
    private String chqAcName;

    @Column(name = "CHQ_AC_NO")
    private String chqAcNo;

    @Column(name = "CHQ_CARDNO")
    private String chqCardNo;

    @Column(name = "CHQ_DATE")
    private LocalDate chqDate;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "VOUCHER_TYPE")
    private String voucherType;

    @Column(name = "CHEQUE_COMPANY_POID")
    private Long chequeCompanyPoid;

    @Column(name = "PAYMENT_MAIN_POID")
    private Long paymentMainPoid;

    @Column(name = "LINE_TYPE")
    private String lineType;

    @Column(name = "PYMT_TYPE")
    private String pymtType;

    @Column(name = "TT_BANK_POID")
    private Long ttBankPoid;

    @Column(name = "TT_REF")
    private String ttRef;

    @Column(name = "CARD_POID")
    private Long cardPoid;

    @Column(name = "CARD_TYPE")
    private String cardType;

    @Column(name = "CREDIT_CARD_REF")
    private String creditCardRef;
}
