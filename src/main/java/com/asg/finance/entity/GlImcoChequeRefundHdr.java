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
@Table(name = "GL_IMCO_CHEQUE_REFUND_HDR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlImcoChequeRefundHdr {

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
    
    @Column(name = "DOC_REF")
    private String docRef;
    
    @Column(name = "REMARKS")
    private String remarks;
    
    @Column(name = "GRAND_TOTAL")
    private BigDecimal grandTotal;
    
    @Column(name = "BL_NUMBER")
    private String blNumber;
    
    @Column(name = "RECEIPT_NUM")
    private String receiptNum;
    
    @Column(name = "PAYING_TO")
    private String payingTo;
    
    @Column(name = "DELETED", length = 1)
    private String deleted = "N";
    
    @Column(name = "CREATED_BY")
    private String createdBy;
    
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;
    
    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;
    
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}