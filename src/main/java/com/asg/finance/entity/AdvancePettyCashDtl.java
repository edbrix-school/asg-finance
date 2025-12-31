package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_ADVANCE_PETTY_CASH_DTL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AdvancePettyCashDtlId.class)
public class AdvancePettyCashDtl {
    
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;
    
    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;
    
    @Column(name = "DRILLDOWN_LINK_INFO", length = 500)
    private String drilldownLinkInfo;
    
    @Column(name = "PETTY_CASH_POID")
    private Long pettyCashPoid;
    
    @Column(name = "PETTY_CASH_TRN_DATE")
    private LocalDate pettyCashTrnDate;
    
    @Column(name = "PETTY_CASH_REF", length = 50)
    private String pettyCashRef;
    
    @Column(name = "PETTY_CASH_COMPANY_POID")
    private Long pettyCashCompanyPoid;
    
    @Column(name = "AMOUNT")
    private BigDecimal amount;
    
    @Column(name = "PETTY_CASH_REMARKS", length = 500)
    private String pettyCashRemarks;
    
    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;
    
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;
    
    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;
    
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}