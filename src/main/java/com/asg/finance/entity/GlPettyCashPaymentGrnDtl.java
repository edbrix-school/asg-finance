package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_PETTY_CASH_PAYMENT_GRN_DTL")
@IdClass(GlPettyCashPaymentGrnDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlPettyCashPaymentGrnDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "GRN_POID")
    private Long grnPoid;

    @Column(name = "CHECK_ALL", length = 1)
    private String checkAll;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "REF_DOC_ID", length = 100)
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "REF_DET_ROW_ID")
    private Long refDetRowId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}
