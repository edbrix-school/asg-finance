package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "BANK_PURPOSE_CODE_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "BANK_PURPOSE_CODE_MASTER_UK", columnNames = "BANK_PURPOSE_CODE")
        }
)
public class BankPurposeCodeMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bp_code_master_seq")
    @SequenceGenerator(
            name = "bp_code_master_seq",
            sequenceName = "BANK_PURPOSE_CODE_MASTER_SEQ",
            allocationSize = 1
    )
    @Column(name = "BANK_PURPOSE_POID")
    private Long bankPurposePoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "BANK_PURPOSE_CODE", length = 100)
    private String bankPurposeCode;

    @Column(name = "BANK_PURPOSE_NAME", length = 500)
    private String bankPurposeName;

    @Column(name = "PURPOSE_TYPE", length = 500)
    private String purposeType;

    @Column(name = "PURPOSE_DETAILS", length = 1000)
    private String purposeDetails;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

}
