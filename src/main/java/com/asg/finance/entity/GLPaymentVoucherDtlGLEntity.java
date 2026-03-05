package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "GL_BANK_PAYMENT_DTL_GL")
@IdClass(GLPaymentVoucherDtlGLEntity.CompositeKey.class)
public class GLPaymentVoucherDtlGLEntity extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "TYPE", length = 2)
    private String type; // DR / CR

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "FF_PDA_TRANSACTION_POID")
    private Long ffPdaTransactionPoid;

    @Column(name = "FF_PDA_CHARGE_POID")
    private Long ffPdaChargePoid;

    @Column(name = "FF_PDA_CHARGE_DET_ROW_ID")
    private Long ffPdaChargeDetRowId;

    @Column(name = "DR_AMT")
    private Double drAmt;

    @Column(name = "CR_AMT")
    private Double crAmt;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "PDC_CHQ_TRN_POID")
    private Long pdcChqTrnPoid;

    @Column(name = "BANK_GL_IND", length = 1)
    private String bankGlInd; // Y/N flag

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private Double taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private Double taxAmount;

    @Column(name = "TOTAL_AMOUNT")
    private Double totalAmount;

    @Column(name = "PARTY_INV_NUMBER", length = 50)
    private String partyInvNumber;

    @Column(name = "PARTY_INV_DATE")
    private LocalDate partyInvDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements java.io.Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}
