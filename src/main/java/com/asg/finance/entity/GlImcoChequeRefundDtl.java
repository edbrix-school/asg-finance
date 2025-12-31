package com.asg.finance.entity;


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
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;
    
    @Column(name = "CHO_POID")
    private Long choPoid;
    
    @Column(name = "CHO_DATE")
    private LocalDate choDate;
    
    @Column(name = "REF_DOC_ID")
    private String refDocId;
    
    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;
    
    @Column(name = "PYMT_TYPE")
    private String pymtType;
    
    @Column(name = "CHQ_CARDNO")
    private String chqCardno;
    
    @Column(name = "CHQ_DATE")
    private LocalDate chqDate;
    
    @Column(name = "BANK_POID")
    private Long bankPoid;
    
    @Column(name = "ADDRESS_POID")
    private Long addressPoid;
    
    @Column(name = "CHQ_AC_NAME")
    private String chqAcName;
    
    @Column(name = "CHQ_AC_NO")
    private String chqAcNo;
    
    @Column(name = "AMOUNT")
    private BigDecimal amount;
    
    @Column(name = "STATUS")
    private String status;
    
    @Column(name = "REMARKS")
    private String remarks;
    
    @Column(name = "OLD_RCPVNO")
    private String oldRcpvno;
    
    @Column(name = "RCP_DATE")
    private LocalDate rcpDate;
    
    @Column(name = "REF_DOC_REF")
    private String refDocRef;
    
    @Column(name = "CHO_DOC_ID")
    private String choDocId;
    
    @Column(name = "PAYMENT_MAIN_POID")
    private Long paymentMainPoid;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}