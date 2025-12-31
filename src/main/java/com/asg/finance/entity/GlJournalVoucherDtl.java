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
@Table(name = "GL_JOURNAL_VOUCHER_DTL")
@IdClass(TransactionDetailKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlJournalVoucherDtl {

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

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "COST_POID", length = 50)
    private String costPoid;

    @Column(name = "TAX_POID")
    private Long taxPoid;
}
