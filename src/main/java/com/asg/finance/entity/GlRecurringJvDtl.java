package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.TransactionDetailKey;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_RECURRING_JV_DTL")
@IdClass(TransactionDetailKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlRecurringJvDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "TYPE", length = 20)
    private String type;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DR_AMT")
    private BigDecimal drAmt;

    @Column(name = "CR_AMT")
    private BigDecimal crAmt;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

}
