package com.asg.finance.entity.master;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_INSURANCE_PIC_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(InsuranceDetailId.class)
public class InsurancePicDetail extends BaseEntity {
    @Id
    @AuditIgnore
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @AuditIgnore
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    @AuditIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private InsuranceMaster insuranceMaster;

    @Column(name = "ROLE_POID", nullable = true)
    private Long rolePoid;

    @Column(name = "CONTACT_TYPE", length = 25)
    private String contactType;

    @Column(name = "PIC_PERSON")
    private Long picPersonPoid;

    @Column(name = "FROM_DATE")
    private LocalDate fromDate;

    @Column(name = "TO_DATE")
    private LocalDate toDate;

}