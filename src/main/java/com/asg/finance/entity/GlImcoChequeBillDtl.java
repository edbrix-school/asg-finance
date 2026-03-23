package com.asg.finance.entity;


import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_IMCO_CHEQUE_BILL_DTL")
@IdClass(GlImcoChequeRefundDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlImcoChequeBillDtl extends BaseEntity {
    
    @Id
    @AuditIgnore
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    @AuditIgnore
    private Long detRowId;

    @Column(name = "BILL_REF")
    private String billRef;
    
    @Column(name = "BILL_AMOUNT")
    private BigDecimal billAmount;
    
    @Column(name = "REMARKS")
    private String remarks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}