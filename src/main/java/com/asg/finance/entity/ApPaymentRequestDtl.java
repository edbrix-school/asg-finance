package com.asg.finance.entity;


import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AP_PAYMENT_REQUEST_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestDtl extends BaseEntity {

    @EmbeddedId
    private ApPaymentRequestDtlId id;

    @Column(name = "CHARGE_POID", nullable = false)
    private Long chargePoid;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "VAT_PER")
    private BigDecimal vatPer;

    @Column(name = "VAT_AMOUNT")
    private BigDecimal vatAmount;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

}
