package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "STOCK_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMasterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STOCK_POID", nullable = false)
    private Long stockPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "STOCK_CODE", length = 20, unique = true)
    private String stockCode;

    @Column(name = "STOCK_NAME", length = 100, unique = true)
    private String stockName;

    @Column(name = "STOCK_NAME2", length = 100)
    private String stockName2;

    @Column(name = "CATEGORY_POID")
    private Long categoryPoid;

    @Column(name = "STOCK_UNIT_POID")
    private Long stockUnitPoid;

    @Column(name = "STOCK_COST")
    private BigDecimal stockCost;

    @Column(name = "TAG_PRICE")
    private BigDecimal tagPrice;

    @Column(name = "RETAIL_PRICE")
    private BigDecimal retailPrice;

    @Column(name = "WHOLESALE_PRICE")
    private BigDecimal wholesalePrice;

    @Column(name = "BARCODE", length = 50)
    private String barcode;

    @Column(name = "SERIAL_NO", length = 50)
    private String serialNo;

    @Column(name = "MAIN_SUPPLIER_POID")
    private Long mainSupplierPoid;

    @Column(name = "OTHER_SUPPLIER_DETAIL", length = 100)
    private String otherSupplierDetail;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "STOCK_GL_POID")
    private Long stockGlPoid;

    @Column(name = "COST_OF_SALES_GL_POID")
    private Long costOfSalesGlPoid;

    @Column(name = "SALES_GL_POID")
    private Long salesGlPoid;

    @Column(name = "PRICE1")
    private BigDecimal price1;

    @Column(name = "PRICE2")
    private BigDecimal price2;

    @Column(name = "PRICE3")
    private BigDecimal price3;

    @Column(name = "ITEM_SIZE", length = 15)
    private String itemSize;

    @Column(name = "ORIGIN", length = 45)
    private String origin;

    @Column(name = "COMPOSITION", length = 30)
    private String composition;

    @Column(name = "FABRIC_TYPE", length = 2)
    private String fabricType;

    @Column(name = "SEASON_CODE", length = 5)
    private String seasonCode;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "INPUT_TAX_POID")
    private Long inputTaxPoid;

    @Column(name = "IS_GIFT_CARD", length = 1)
    private String isGiftCard;

    @Column(name = "SERIAL_NO_TRACKING", length = 1)
    private String serialNoTracking;

    @Column(name = "SERVICE_ITEM", length = 1)
    private String serviceItem = "N";

    @Column(name = "CATEGORY_CODE", length = 50)
    private String categoryCode;

    @Column(name = "WASTAGE_PERCENTAGE")
    private BigDecimal wastagePercentage;

    @Column(name = "PO_POID")
    private Long poPoid;

    @Column(name = "PO_DET_ROW_ID")
    private Long poDetRowId;

    @Column(name = "PRODUCT_TAGS", length = 200)
    private String productTags;

    @Column(name = "STOCK_DTLD_NARRATION", length = 2000)
    private String stockDtldNarration;

    @Column(name = "ONLINE_STOCK", length = 1)
    private String onlineStock;

    @Column(name = "SUPPLIER_BARCODE", length = 100)
    private String supplierBarcode;

    @Column(name = "STOCK_CARE_INSTRUCTIONS", length = 2500)
    private String stockCareInstructions;

    @Column(name = "STOCK_COLOR", length = 500)
    private String stockColor;

    @Column(name = "STOCK_BRAND", length = 200)
    private String stockBrand;

    @Column(name = "STOCK_DESCRIPTION", length = 500)
    private String stockDescription;

    @Column(name = "PURCHASE_SALES_CONVERSION")
    private BigDecimal purchaseSalesConversion;

    @Column(name = "ONLINE_CATEGORY_NAME", length = 500)
    private String onlineCategoryName;

    @Column(name = "PURCHASE_STOCK_UNIT_POID")
    private Long purchaseStockUnitPoid;

    @Column(name = "FOOD_ITEM", length = 1)
    private String foodItem;

    @Column(name = "PRINT_LABEL", length = 1)
    private String printLabel;

    @Column(name = "IS_CONSUMABLES", length = 1)
    private String isConsumables;

    @Column(name = "SUB_CATEGORY_NAME_2", length = 400)
    private String subCategoryName2;

    @Column(name = "SUB_CATEGORY_NAME_1", length = 400)
    private String subCategoryName1;

    @Column(name = "STOCK_GROUP_CODE", length = 250)
    private String stockGroupCode;

    @Column(name = "IMAGES", length = 8000)
    private String images;

    @Column(name = "IMAGE_URL", length = 8000)
    private String imageUrl;

    @Column(name = "WEIGHT")
    private BigDecimal weight;

    @Column(name = "MINIMUM_REQUIRED_QTY")
    private BigDecimal minimumRequiredQty;

    @Column(name = "CONSUMPTION_UNIT_POID")
    private Long consumptionUnitPoid;

    @Column(name = "CONSUMPTION_QTY")
    private BigDecimal consumptionQty;

    @Column(name = "SUPPLIER_STOCK_CODE", length = 200)
    private String supplierStockCode;

    @Column(name = "SPECIES", length = 200)
    private String species;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GRN_REF_POID")
    private Long grnRefPoid;

    @Column(name = "LASTMODIFIED_GRN_POID")
    private Long lastModifiedGrnPoid;

    @Column(name = "TAX_PERCENT", precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "EXPIRY_TRACKING", length = 1)
    private String expiryTracking;
}
