package com.asg.finance.entity;


import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "GLOBAL_TAX_PERIOD_STOCK_DTL")
@IdClass(GlobalTaxPeriodStockDtlEntity.CompositeKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalTaxPeriodStockDtlEntity extends BaseEntity {
    @Id
    @Column(name = "TRANSACTION_POID")
    @AuditIgnore
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    @AuditIgnore
    private Long detRowId;

    @Column(name = "STOCK_POID")
    private Long stockPoid;

    @Column(name = "STOCK_CAT_POID")
    private Long stockCatPoid;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "REMARKS", length = 50)
    private String remarks;

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
