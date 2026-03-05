package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
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
public class AdvancePettyCashDtl extends BaseEntity {
    
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

}