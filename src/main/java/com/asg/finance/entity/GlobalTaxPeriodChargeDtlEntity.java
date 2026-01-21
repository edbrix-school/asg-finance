package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_TAX_PERIOD_CHARGE_DTL")
@IdClass(GlobalTaxPeriodChargeDtlEntity.CompositeKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalTaxPeriodChargeDtlEntity {

    @Id
    @Column(name = "TRANSACTION_POID")
    @AuditIgnore
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    @AuditIgnore
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "CHARGE_CAT_POID")
    private Long chargeCatPoid;

    @Column(name = "INPUT_TAX_POID")
    private Long inputTaxPoid;

    @Column(name = "TAX_POID")
    private Long outputTaxPoid;

    @Column(name = "REMARKS", length = 50)
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}
