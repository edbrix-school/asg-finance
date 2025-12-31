package com.asg.finance.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_TAX_PERIOD_STOCK_DTL")
@IdClass(GlobalTaxPeriodStockDtlEntity.CompositeKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalTaxPeriodStockDtlEntity {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "STOCK_POID")
    private Long stockPoid;

    @Column(name = "STOCK_CAT_POID")
    private Long stockCatPoid;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "REMARKS", length = 50)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "INPUT_TAX_POID")
    private Long inputTaxPoid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}
