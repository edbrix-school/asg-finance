package com.asg.finance.entity;

import com.asg.finance.entity.master.ShipChargeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_PETTY_CASH_PAYMENT_DTL")
@IdClass(GlPettyCashPaymentDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlPettyCashPaymentDtl {
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

    @ManyToOne
    @JoinColumn(name = "GL_POID", referencedColumnName = "GL_POID")
    private GLMaster glMaster;

    @ManyToOne
    @JoinColumn(name = "CHARGE_POID", referencedColumnName = "CHARGE_POID")
    private ShipChargeEntity chargeMaster;

    @Column(name = "DR_AMT")
    private BigDecimal drAmt;

    @Column(name = "CR_AMT")
    private BigDecimal crAmt;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

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