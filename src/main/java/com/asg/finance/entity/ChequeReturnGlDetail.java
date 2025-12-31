package com.asg.finance.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "GL_CHEQUE_RETURN_GL_DTL")
public class ChequeReturnGlDetail {

    @EmbeddedId
    private ChequeReturnGlDetailId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("transactionPoid") // 👈 same reason — reuse embedded id
    @JoinColumn(name = "TRANSACTION_POID", nullable = false)
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

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LASTMODIFIED_DATE")
    private Date lastModifiedDate;
}
