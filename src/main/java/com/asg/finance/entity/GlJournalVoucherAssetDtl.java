package com.asg.finance.entity;

import com.asg.finance.entity.key.TransactionDetailKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_JOURNAL_VOUCHER_ASSET_DTL")
@IdClass(TransactionDetailKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlJournalVoucherAssetDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "FA_POID", unique = true)
    private Long faPoid;

    @Column(name = "FA_DESCRIPTION", length = 300)
    private String faDescription;

    @Column(name = "FA_CATEGORY", length = 100)
    private String faCategory;

    @Column(name = "ASSET_TYPE", length = 100)
    private String assetType;

    @Column(name = "LIFE_YEAR")
    private Integer lifeYear;

    @Column(name = "PURCHASE_DATE")
    private LocalDate purchaseDate;

    @Column(name = "DEPRECIATION_START_DATE")
    private LocalDate depreciationStartDate;

    @Column(name = "SCRAP_SOLD_DATE")
    private LocalDate scrapSoldDate;

    @Column(name = "ASSET_VALUE")
    private BigDecimal assetValue;

    @Column(name = "DEPRECIATED_AMT")
    private BigDecimal depreciatedAmt;

    @Column(name = "WDV_VALUE")
    private BigDecimal wdvValue;

    @Column(name = "PROCESS", length = 100)
    private String process;

    @Column(name = "SCRAP_SOLD_VALUE")
    private BigDecimal scrapSoldValue;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 50)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
