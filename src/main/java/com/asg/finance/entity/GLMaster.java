package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "GL_MASTER")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gl_master_seq")
    @SequenceGenerator(name = "gl_master_seq", sequenceName = "GL_MASTER_SEQ", allocationSize = 1)
    @Column(name = "GL_POID", nullable = false)
    private Long glPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "GL_CODE", length = 20)
    private String glCode;

    @Column(name = "GL_DESCRIPTION", length = 100)
    private String glDescription;

    @Column(name = "GL_DESCRIPTION2", length = 100)
    private String glDescription2;

    @Column(name = "GL_TYPE", length = 20)
    private String glType;

    @Column(name = "GROUP_GL_POID")
    private Long groupGlPoid;

    @Column(name = "CONTROL_AC_NATURE", length = 20)
    private String controlAcNature;

    @Column(name = "COST_GROUP", length = 20)
    private String costGroup;

    @Column(name = "BILLWISE", length = 1, columnDefinition = "CHAR DEFAULT 'N'")
    private String billwise;

    @Column(name = "PREPAYMENT_LEDGER", length = 1, columnDefinition = "CHAR DEFAULT 'N'")
    private String prepaymentLedger;

    @Column(name = "INTER_COMPANY_AC", length = 1, columnDefinition = "CHAR DEFAULT 'N'")
    private String interCompanyAc;

    @Column(name = "INTER_COMPANY_POID")
    private Long interCompanyPoid;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "SEQNO", precision = 5)
    private Integer seqno;

    @Column(name = "ACTIVE", length = 1, columnDefinition = "CHAR DEFAULT 'Y'")
    private String active;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "GROUP_CODE_OLD", length = 20)
    private String groupCodeOld;

    @Column(name = "GL_AC_TYPE", length = 20, nullable = false)
    private String glAcType;

    @Column(name = "DELETED", length = 1, columnDefinition = "CHAR DEFAULT 'N'")
    private String deleted;

    @Column(name = "AMOUNT_LIMIT")
    private Double amountLimit;

    @Column(name = "AMOUNT_ROL")
    private Double amountRol;

    @Column(name = "OLD_ORGCODE", length = 50)
    private String oldOrgCode;

    @Column(name = "OLD_ORIGINAL_CODE", length = 20)
    private String oldOriginalCode;

    @Column(name = "OLD_MOD_CODE", length = 20)
    private String oldModCode;
}
