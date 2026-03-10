package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "GL_PDC_CHQ_BATCH_DTL")
@IdClass(PdcChqBatchDtlId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcChqBatchDtlEntity extends BaseEntity {
    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "PDC_CHQ_DATE")
    private LocalDate pdcChqDate;

    @Column(name = "CHQ_NUMBER", length = 20)
    private String chqNumber;

    @Column(name = "CHQ_AMOUNT")
    private Double chqAmount;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "BANK_PAYMENT_POID", length = 50)
    private String bankPaymentPoid;

    @Column(name = "BANK_PAYMENT_REF", length = 50)
    private String bankPaymentRef;

    @Column(name = "NARRATION", length = 300)
    private String narration;

    @Column(name = "BILL_REF", length = 100)
    private String billRef;

    @Column(name = "DR_GL_POID1")
    private Long drGlPoid1;

    @Column(name = "DR_AMT1")
    private Double drAmt1;

    @Column(name = "DR_GL_POID2")
    private Long drGlPoid2;

    @Column(name = "DR_AMT2")
    private Double drAmt2;

    @Column(name = "DR_GL_POID3")
    private Long drGlPoid3;

    @Column(name = "DR_AMT3")
    private Double drAmt3;

    @Column(name = "CR_GL_POID")
    private Long crGlPoid;

    @Column(name = "CR_AMT")
    private Double crAmt;

    @Column(name = "COST_POID", length = 100)
    private String costPoid;


    // OPTIONAL Mapping to Header (Read-only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "TRANSACTION_POID",
            referencedColumnName = "TRANSACTION_POID",
            insertable = false,
            updatable = false
    )
    private PdcChqBatchHdrEntity header;
}
