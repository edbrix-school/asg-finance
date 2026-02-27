package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.ArCreditNoteChargeDtlKey;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AR_CREDIT_NOTE_CHARGE_DTL")
@IdClass(ArCreditNoteChargeDtlKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArCreditNoteChargeDtl extends BaseEntity {
    
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;
    
    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;
    
    @Column(name = "CHARGE_POID")
    private Long chargePoid;
    
    @Column(name = "CHARGE_AMOUNT", precision = 19, scale = 2)
    private BigDecimal chargeAmount;
    
    @Column(name = "DESCRIPTION", length = 500)
    private String description;
    
    @Column(name = "REMARKS", length = 500)
    private String remarks;
    
    @Column(name = "REF_DOC_ID", length = 50)
    private String refDocId;
    
    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;
    
    @Column(name = "FDA_DET_ROW_ID")
    private Long fdaDetRowId;
    
    @Column(name = "CHECK_ALL", length = 1)
    private String checkAll;
    
    @Column(name = "PDA_AMOUNT", precision = 19, scale = 2)
    private BigDecimal pdaAmount;
    
    @Column(name = "FF_AMOUNT", precision = 19, scale = 2)
    private BigDecimal ffAmount;
    
    @Column(name = "TAX_POID")
    private Long taxPoid;
    
    @Column(name = "TAX_PERCENTAGE", precision = 5, scale = 2)
    private BigDecimal taxPercentage;
    
    @Column(name = "TAX_AMOUNT", precision = 19, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(name = "TOTAL_AMOUNT", precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "ISSUE_INVOICE", length = 1)
    private String issueInvoice;
    
    @Column(name = "CHARGE_COST_AMOUNT", precision = 19, scale = 2)
    private BigDecimal chargeCostAmount;

 /*   @Column(name = "BASIC_AMOUNT", precision = 19, scale = 2)
    private BigDecimal basicAmount;*/
}