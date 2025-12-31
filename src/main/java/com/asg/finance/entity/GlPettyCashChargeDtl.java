package com.asg.finance.entity;

import com.asg.finance.entity.master.ShipChargeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "GL_PETTY_CASH_CHARGE_DTL")
@IdClass(GLPettyCashItemDtl.CompositeKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPettyCashChargeDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    // Column: CHARGE_POID (Foreign Key)
    @ManyToOne
    @JoinColumn(name = "CHARGE_POID", referencedColumnName = "CHARGE_POID", insertable = false, updatable = false)
    private ShipChargeEntity shipChargeMaster;

    // Other columns
    @Column(name = "CHARGE_AMOUNT")
    private BigDecimal chargeAmount;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

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
    private Date partyInvDate;

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
