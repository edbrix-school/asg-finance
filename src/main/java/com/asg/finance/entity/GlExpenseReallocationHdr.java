package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "GL_EXPENSE_REALLOCATION_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlExpenseReallocationHdr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "NARRATION", length = 1000)
    private String narration;

    @Column(name = "EXPENSE_GROUP_GL", nullable = false)
    private Long expenseGroupGl;

    @Column(name = "FROM_COMPANY", nullable = false)
    private Long fromCompany;

    @Column(name = "JV_POID")
    private Long jvPoid;

    @Column(name = "JV_REF", length = 100)
    private String jvRef;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "COST_POID", length = 100)
    private String costPoid;

    @Column(name = "FROM_DATE")
    private LocalDate fromDate;

    @Column(name = "TO_DATE")
    private LocalDate toDate;

    @Column(name = "ALLOCATION_TYPE", length = 100)
    private String allocationType;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "REPORT_GENERATION", length = 1)
    private String reportGeneration;

}
