package com.asg.finance.entity;

import com.asg.finance.entity.key.TransactionDetailKey;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_RECURRING_JV_MONTH_DTL")
@IdClass(TransactionDetailKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlRecurringJvMonthDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "JV_POID", length = 25)
    private String jvPoid;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

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

    @Column(name = "STATUS", length = 100)
    private String status;

    @Column(name = "MONTH_WISE_DATE")
    private LocalDate monthWiseDate;

    @Column(name = "DRILLDOWN_LINK_INFO", length = 300)
    private String drilldownLinkInfo;
}
