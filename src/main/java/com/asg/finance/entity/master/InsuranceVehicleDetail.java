package com.asg.finance.entity.master;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_INSURANCE_VEHICLE_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(InsuranceDetailId.class)
public class InsuranceVehicleDetail {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private InsuranceMaster insuranceMaster;

    @Column(name = "FIXED_ASSET_POID")
    private Long fixedAssetPoid;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "REMARKS", length = 200)
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