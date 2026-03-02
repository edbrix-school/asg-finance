package com.asg.finance.entity.master;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Entity
@Table(name = "GLOBAL_INSURANCE_OTHERS_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(InsuranceDetailId.class)
public class InsuranceDetail extends BaseEntity {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private InsuranceMaster insuranceMaster;

    @Column(name = "DETAILS", length = 50)
    private String details;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "REMARKS", length = 200)
    private String remarks;
}