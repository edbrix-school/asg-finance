package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.ArCreditNoteDtlKey;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "AR_CREDIT_NOTE_DTL")
@IdClass(ArCreditNoteDtlKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArCreditNoteDtl extends BaseEntity {
    
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;
    
    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;
    
    @Column(name = "TYPE", length = 10)
    private String type;
    
    @Column(name = "COMPANY_POID")
    private Long companyPoid;
    
    @Column(name = "GL_POID")
    private Long glPoid;
    
    @Column(name = "CHARGE_POID")
    private Long chargePoid;
    
    @Column(name = "DR_AMT", precision = 19, scale = 2)
    private BigDecimal drAmt;
    
    @Column(name = "CR_AMT", precision = 19, scale = 2)
    private BigDecimal crAmt;
    
    @Column(name = "REMARKS", length = 500)
    private String remarks;
    
    @Column(name = "TAX_POID")
    private Long taxPoid;
    
    @Column(name = "TAX_PERCENTAGE", precision = 5, scale = 2)
    private BigDecimal taxPercentage;
    
    @Column(name = "TAX_AMOUNT", precision = 19, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(name = "TOTAL_AMOUNT", precision = 19, scale = 2)
    private BigDecimal totalAmount;
}