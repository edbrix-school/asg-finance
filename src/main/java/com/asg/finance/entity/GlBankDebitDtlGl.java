package com.asg.finance.entity;

import com.asg.finance.entity.key.GlBankDebitDtlGlId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "GL_BANK_DEBIT_DTL_GL",
        uniqueConstraints = {
                @UniqueConstraint(name = "GL_BANK_DEBIT_DTL_GL_PK", columnNames = {"TRANSACTION_POID", "DET_ROW_ID"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankDebitDtlGl {

    @EmbeddedId
    private GlBankDebitDtlGlId id;

    @Column(name = "TYPE", length = 20)
    private String type;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DR_AMT", precision = 15, scale = 2)
    private BigDecimal drAmt;

    @Column(name = "CR_AMT", precision = 15, scale = 2)
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

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE", precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "PARTY_INV_NUMBER", length = 500)
    private String partyInvNumber;

    @Column(name = "PARTY_INV_DATE")
    private LocalDate partyInvDate;
}
