package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_BANK_DEPOSIT_VOUCHER_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankDepositVoucherHdr extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 100, insertable = false, updatable = false)
    @Generated
    private String docRef;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "POSTING_NARRATION", length = 500)
    private String postingNarration;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "GRAND_TOTAL", precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "REF_TYPE", length = 50)
    private String refType;

    @Column(name = "BANK_FILTER", length = 50)
    private String bankFilter;

    @Column(name = "GROUP_POSTING", length = 1)
    private String groupPosting;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}
