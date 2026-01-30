package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.Company;
import com.asg.common.lib.entity.CurrencyEntity;
import com.asg.common.lib.entity.GroupEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "AP_PURCHASE_ORDER_HDR")
public class PurchaseOrder {

    @Id
    @AuditIgnore
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_hdr_seq_gen")
    @SequenceGenerator(
            name = "po_hdr_seq_gen",
            sequenceName = "PO_HDR_SEQ",
            allocationSize = 1
    )
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    @AuditIgnore
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GROUP_POID", referencedColumnName = "GROUP_POID",
            insertable = false, updatable = false)
    @AuditIgnore
    private GroupEntity group;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "COMPANY_POID")
    @AuditIgnore
    private Long companyPoid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_POID", referencedColumnName = "COMPANY_POID",
            insertable = false, updatable = false)
    @AuditIgnore
    private Company company;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CURRENCY_CODE", referencedColumnName = "CURRENCY_CODE",
            insertable = false, updatable = false)
    @AuditIgnore
    private CurrencyEntity currency;

    @Column(name = "CURRENCY_RATE")
    private Double currencyRate;

    @Column(name = "EXPECTED_DATE")
    private LocalDate expectedDate;

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUPPLIER_POID", referencedColumnName = "SUPPLIER_POID",
            insertable = false, updatable = false)
    @AuditIgnore
    private SupplierMasterEntity supplier;

    @Column(name = "PAYMENT_TERMS", length = 100)
    private String paymentTerms;

    @Column(name = "MODE_OF_TRANSPORT", length = 100)
    private String modeOfTransport;

    @Column(name = "DELIVERY_TERMS", length = 100)
    private String deliveryTerms;

    @Column(name = "FREIGHT_FORWARDER", length = 100)
    @AuditIgnore
    private String freightForwarder;

    @Column(name = "SHIPPING_MARK", length = 100)
    @AuditIgnore
    private String shippingMark;

    @Column(name = "BILLING_ADDRESS_POID")
    @AuditIgnore
    private Long billingAddressPoid;

    @Column(name = "DELIVERY_ADDRESS_POID")
    private Long deliveryAddressPoid;

    @Column(name = "SUB_TOTAL")
    @AuditIgnore
    private Double subTotal;

    @Column(name = "DISCOUNT")
    @AuditIgnore
    private Double discount;

    @Column(name = "EXPENSE_BY_SUPPLIER")
    @AuditIgnore
    private Double expenseBySupplier;

    @Column(name = "GRAND_TOTAL")
    private Double grandTotal;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "RFQ_POID")
    @AuditIgnore
    private Long rfqPoid;

    @Column(name = "PO_STATUS", length = 20)
    private String poStatus;

    @Column(name = "ITEM_TOTAL")
    @AuditIgnore
    private Double itemTotal;

    @Column(name = "CHARGE_TOTAL")
    @AuditIgnore
    private Double chargeTotal;

    @Column(name = "TYPE", length = 20)
    private String type;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "DELIVERY_METHOD", length = 50)
    private String deliveryMethod;

    @Column(name = "DELIVERY_ADDRESS", length = 1000)
    private String deliveryAddress;

    @Column(name = "SALES_QTN_POID")
    @AuditIgnore
    private Long salesQtnPoid;

    @Column(name = "DESCRIPTION_PRINT_YN", length = 1)
    private String descriptionPrintYn;

    @Column(name = "SALES_INV_POID")
    @AuditIgnore
    private Long salesInvPoid;

    @Column(name = "SALES_INV_DOC_REF", length = 50)
    @AuditIgnore
    private String salesInvDocRef;

    @Column(name = "REF_TYPE", length = 50)
    private String refType;

    @Column(name = "MULTI_COMPANY", length = 1)
    @AuditIgnore
    private String multiCompany;

    @Column(name = "VOUCHER_NARRATION", length = 1000)
    private String voucherNarration;

    @Column(name = "PJ_POID")
    @AuditIgnore
    private Long pjPoid;

    @Column(name = "VALIDITY_DATE")
    private LocalDate validityDate;

    @Column(name = "TERMS_POID")
    private Long termsPoid;

    @Column(name = "PURCHASE_REQUEST_POID")
    @AuditIgnore
    private Long purchaseRequestPoid;

    @Column(name = "PRINT_DIV_POID")
    private Long printDivPoid;

    @Column(name = "GRN_POID")
    @AuditIgnore
    private Long grnPoid;

    @Column(name = "GRN_REF", length = 100)
    @AuditIgnore
    private String grnRef;

    @Column(name = "GRN_DATE")
    @AuditIgnore
    private LocalDate grnDate;

    @Column(name = "SHIPMENT_MONTH")
    @AuditIgnore
    private LocalDate shipmentMonth;

    @Column(name = "DISCOUNT_PERCENTAGE")
    @AuditIgnore
    private Double discountPercentage;

    @Column(name = "ITEM_DISCOUNT_TOTAL")
    @AuditIgnore
    private Double itemDiscountTotal;

    @Column(name = "ITEM_DISCOUNT_TOTAL_PERCENTAGE")
    @AuditIgnore
    private Double itemDiscountTotalPercentage;
}
