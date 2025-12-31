package com.asg.finance.entity;

import com.asg.finance.entity.key.TransactionDetailKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_JOURNAL_FA_CAPITALIZATION")
@IdClass(TransactionDetailKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlJournalFaCapitalization {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "FA_POID")
    private Long faPoid;

    @Column(name = "FA_DESCRIPTION", length = 500)
    private String faDescription;

    @Column(name = "FA_CATEGORY")
    private Long faCategory;

    @Column(name = "ASSET_TYPE", length = 300)
    private String assetType;

    @Column(name = "ASSET_VALUE")
    private BigDecimal assetValue;

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
}
