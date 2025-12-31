package com.asg.finance.entity.master;

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
public class InsurancePicDetail {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private InsuranceMaster insuranceMaster;

    @Column(name = "ROLE_POID", nullable = true)
    private Long rolePoid;

    @Column(name = "CONTACT_TYPE", length = 25)
    private String contactType;

    @Column(name = "PIC_PERSON", length = 25)
    private String picPerson;

    @Column(name = "FROM_DATE")
    private LocalDate fromDate;

    @Column(name = "TO_DATE")
    private LocalDate toDate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}