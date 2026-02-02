package com.asg.finance.entity;


import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_IMCO_CHEQUE_REFUND_DTL")
@IdClass(GlImcoChequeRefundDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlImcoChequeRefundDtl {
    
    @Id
    @AuditIgnore
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @AuditIgnore
    @Column(name = "DET_ROW_ID")
    private Long detRowId;
    
    @Column(name = "CHO_POID")
    @AuditIgnore
    private Long choPoid;
    
    @Column(name = "CHO_DATE")
    private LocalDate choDate;
    
    @Column(name = "REF_DOC_ID")
    private String refDocId;
    
    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;
    
    @Column(name = "PYMT_TYPE")
    @AuditIgnore
    private String pymtType;
    
    @Column(name = "CHQ_CARDNO")
    private String chqCardno;
    
    @Column(name = "CHQ_DATE")
    private LocalDate chqDate;
    
    @Column(name = "BANK_POID")
    private Long bankPoid;
    
    @Column(name = "ADDRESS_POID")
    @AuditIgnore
    private Long addressPoid;
    
    @Column(name = "CHQ_AC_NAME")
    private String chqAcName;
    
    @Column(name = "CHQ_AC_NO")
    private String chqAcNo;
    
    @Column(name = "AMOUNT")
    private BigDecimal amount;
    
    @Column(name = "STATUS")
    @AuditIgnore
    private String status;
    
    @Column(name = "REMARKS")
    private String remarks;
    
    @Column(name = "OLD_RCPVNO")
    @AuditIgnore
    private String oldRcpvno;
    
    @Column(name = "RCP_DATE")
    private LocalDate rcpDate;
    
    @Column(name = "REF_DOC_REF")
    private String refDocRef;
    
    @Column(name = "CHO_DOC_ID")
    @AuditIgnore
    private String choDocId;
    
    @Column(name = "PAYMENT_MAIN_POID")
    @AuditIgnore
    private Long paymentMainPoid;

    @Column(name = "CREATED_BY")
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}