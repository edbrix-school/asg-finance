package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
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
@Table(name = "GL_IMCO_CHEQUE_REFUND_HDR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlImcoChequeRefundHdr extends BaseEntity {

    @Id
    @AuditIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    @AuditIgnore
    private LocalDate transactionDate;
    
    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;
    
    @Column(name = "COMPANY_POID")
    @AuditIgnore
    private Long companyPoid;
    
    @Column(name = "DOC_REF")
    private String docRef;
    
    @Column(name = "REMARKS")
    private String remarks;
    
    @Column(name = "GRAND_TOTAL")
    @AuditIgnore
    private BigDecimal grandTotal;
    
    @Column(name = "BL_NUMBER")
    private String blNumber;
    
    @Column(name = "RECEIPT_NUM")
    private String receiptNum;
    
    @Column(name = "PAYING_TO")
    private String payingTo;
    
    @Column(name = "DELETED", length = 1)
    private String deleted = "N";

}