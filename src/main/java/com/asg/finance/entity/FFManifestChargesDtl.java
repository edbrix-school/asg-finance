package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "FF_MANIFEST_CHARGES_DTL")
@IdClass(FFManifestChargesDtlId.class)
@Data
public class FFManifestChargesDtl {

    @AuditIgnore
    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @NotNull
    private Long transactionPoid;

    @AuditIgnore
    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    @NotNull
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private BigDecimal chargePoid;

    @Column(name = "CURRENCY_EXCHANGE")
    private BigDecimal currencyExchange;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "BUYING_PERCHARGE")
    private BigDecimal buyingPercharge;

    @Column(name = "BILLING_PERCHARGE")
    private BigDecimal billingPrecharge;

    @Column(name = "PAID_AT_PORT_POID")
    private BigDecimal paidAtPortPoid;

    @AuditIgnore
    @Column(name = "CREATED_BY")
    @Size(max = 20)
    private String createdBy;

    @AuditIgnore
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_BY")
    @Size(max = 20)
    private String lastModifiedBy;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "CURRENCY_CODE")
    @Size(max = 10)
    private String currencyCode;

    @Column(name = "PAY_MODE")
    @Size(max = 1)
    private String payMode;

    @Column(name = "RCPT_NO_OLD")
    @Size(max = 100)
    private String rcptNoOld;

    @Column(name = "CHARGE_CODE_OLD")
    @Size(max = 20)
    private String chargeCideOld;

    @Column(name = "RCPT_DAE_OLD")
    private LocalDateTime rcptDaeOld;

    @Column(name = "COST_INV_OLD")
    @Size(max = 100)
    private String costInvOld;

    @Column(name = "EQUIPMENT_POID")
    private BigDecimal equipmentPoid;

    @Column(name = "TOTAL_BUYING_CHARGE")
    private BigDecimal totalBuyingCharge;

    @Column(name = "TOTAL_SELLING_CHARGE")
    private BigDecimal totalSellingCharge;

    @Column(name = "COST_INV_DT_OLD")
    private LocalDateTime costInvDtOld;

    @Column(name = "RCPT_INV_POID")
    @Size(max = 100)
    private String rcptIvPoid;

    @Column(name = "TOTAL_COST_BOOKED")
    private BigDecimal totalCostBooked;

    @Column(name = "DATA_ROWID")
    @Size(max = 10)
    private String dataRowId;

    @Column(name = "COST_CURRENCY")
    @Size(max = 10)
    private String costCurrency;

    @Column(name = "COST_CURRENCY_RATE")
    private BigDecimal costCurrencyRate;

    @Column(name = "COST_BOOK_REF")
    @Size(max = 100)
    private String costBookRef;

    @Column(name = "PRINT_GROUP")
    @Size(max = 50)
    private String printGroup;

    @Column(name = "REMARKS")
    @Size(max = 200)
    private String remarks;

    @Column(name = "SH_CHARGE_INV")
    @Size(max = 25)
    private String shChargeInv;

    @Column(name = "UNIT_TYPE", length = 25)
    private String unitType;

    @Column(name = "TAX_POID")
    private BigDecimal taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "TAX_INPUT_AMOUNT")
    private BigDecimal taxInputAmount;

    @Column(name = "CN_REF_DOC_ID")
    @Size(max = 100)
    private String cnRefDocId;

    @Column(name = "CN_REF_DOC_POID")
    @Size(max = 300)
    private String cnRefDocPoid;

    @Column(name = "CN_REF_DET_ROW_ID")
    @Size(max = 300)
    private String cnRefDetRowId;

    @Column(name = "CN_ISSUE_INVOICE")
    @Size(max = 100)
    private String cnIssueInvoice;

    @Column(name = "HOUSE_BL_POID")
    private BigDecimal houseBlPoid;

    @Column(name = "SUPPLIER_POID")
    private BigDecimal supplierPoid;

    @Column(name = "CHARGE_BASIS")
    @Size(max = 20)
    private String chargeBasis;

    @Column(name = "ENTRY_LOCATION")
    @Size(max = 25)
    private String enteryLocation;

}
