package com.asg.finance.entity;

import com.asg.finance.entity.master.ShipChargeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AP_PURCHASE_INVOICE_CHARGE_DTL")
public class PurchaseInvoiceChargeDtl {

    @EmbeddedId
    private PurchaseInvoiceChargeDtlId id;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @ManyToOne
    @JoinColumn(name = "CHARGE_POID", referencedColumnName = "CHARGE_POID",
            insertable = false, updatable = false)
    private ShipChargeEntity shipChargeMaster;

    @Column(name = "CHARGE_AMOUNT")
    private Double chargeAmount;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "REF_DOC_ID")
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "FDA_DET_ROW_ID")
    private Long fdaDetRowId;

    @Column(name = "CHECK_ALL")
    private String checkAll;

    @Column(name = "PDA_AMOUNT")
    private Double pdaAmount;

    @Column(name = "FF_AMOUNT")
    private Double ffAmount;

    @Column(name = "CHARGE_FROM")
    private String chargeFrom;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private Double taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private Double taxAmount;

    @Column(name = "CHARGE_BASE_AMOUNT")
    private Double chargeBaseAmount;

    @Column(name = "SUPPLIER_POID_FF")
    private Long supplierPoidFf;
}
