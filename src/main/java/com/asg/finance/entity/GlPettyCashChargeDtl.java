package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "GL_PETTY_CASH_CHARGE_DTL")
@IdClass(GLPettyCashItemDtl.CompositeKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPettyCashChargeDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    // Other columns
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

    @Column(name = "CHARGE_FROM", length = 20)
    private String chargeFrom;

    @Column(name = "VAT_PARTY_NAME", length = 500)
    private String vatPartyName;

    @Column(name = "PARTY_INV_NUMBER", length = 500)
    private String partyInvNumber;

    @Column(name = "PARTY_INV_DATE")
    private LocalDate partyInvDate;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}
