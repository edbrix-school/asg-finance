package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entity class for GL_FAV_AC_MASTER table
 * Key Favorite Account Master - Configuration master used for grouping critical financial GL accounts
 * for reporting purposes (Bank Cash Position Reports, etc.)
 */
@Entity
@Table(name = "GL_FAV_AC_MASTER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlFavAcMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FAV_AC_POID", nullable = false)
    private Long favAcPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "FAV_AC_CODE", length = 20, nullable = false)
    private String favAcCode;

    @Column(name = "DESCRIPTION", length = 100, nullable = false)
    private String description;

    @Column(name = "DESCRIPTION2", length = 100)
    private String description2;

    @Column(name = "ACTIVE", length = 1, columnDefinition = "VARCHAR2(1) DEFAULT 'Y'")
    private String active;

    @Column(name = "SEQNO", precision = 5, scale = 0)
    private Integer seqNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "DELETED", length = 1, columnDefinition = "VARCHAR2(1) DEFAULT 'N'")
    private String deleted;
}

