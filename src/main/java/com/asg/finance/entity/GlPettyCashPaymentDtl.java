package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_PETTY_CASH_PAYMENT_DTL")
@IdClass(GlPettyCashPaymentDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlPettyCashPaymentDtl extends BaseEntity {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "TYPE", length = 20)
    private String type;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "DR_AMT")
    private BigDecimal drAmt;

    @Column(name = "CR_AMT")
    private BigDecimal crAmt;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "VAT_AMOUNT")
    private BigDecimal vatAmount;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "VAT_SUPPLIER")
    private Long vatSupplier;

    @Column(name = "INPUT_VAT_NUMBER", length = 300)
    private String inputVatNumber;

    @Column(name = "SUPPLIER_INV_DATE")
    private LocalDate supplierInvDate;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "VAT_PARTY_NAME", length = 500)
    private String vatPartyName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}