package com.asg.finance.entity;
import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "GL_CHEQUE_RETURN_GL_DTL")
public class ChequeReturnGlDetail extends BaseEntity {

    @EmbeddedId
    @AuditIgnore
    private ChequeReturnGlDetailId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("transactionPoid") // 👈 same reason — reuse embedded id
    @JoinColumn(name = "TRANSACTION_POID", nullable = false)
    @AuditIgnore
    private ChequeReturn chequeReturn;

    @Column(name = "TYPE")
    private String type; // DR/CR

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DR_AMT")
    private Double drAmt;

    @Column(name = "CR_AMT")
    private Double crAmt;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

}
