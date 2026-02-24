package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.finance.entity.key.GlChequeCashConvertOutDtlKey;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_CHEQUE_CASH_CONVERT_OUT_DTL")
@Data
public class GlChequeCashConvertOutDtlEntity {

    @EmbeddedId
    @AuditIgnore
    private GlChequeCashConvertOutDtlKey id;

    @Column(name = "PAYMENT_MAIN_POID")
    private Long paymentMainPoid;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

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

    @Column(name = "SELECTED")
    private String selected;

    @Column(name = "VOUCHER_TYPE")
    private String voucherType;

    @Column(name = "LINE_TYPE")
    private String lineType;
}
