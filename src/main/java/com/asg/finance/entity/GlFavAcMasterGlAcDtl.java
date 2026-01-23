package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entity class for GL_FAV_AC_MASTER_GL_AC_DTL table
 * Detail table to store GL account mappings for Key Favorite Account Master
 * Includes company and view category for report filtering
 */
@Entity
@Table(name = "GL_FAV_AC_MASTER_GL_AC_DTL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlFavAcMasterGlAcDtl {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gl_fav_ac_gl_ac_dtl_seq")
    @SequenceGenerator(name = "gl_fav_ac_gl_ac_dtl_seq", sequenceName = "GL_FAV_AC_MASTER_GL_AC_DTL_SEQ", allocationSize = 1)
    @Column(name = "DET_ROW_ID", nullable = false)
    @AuditIgnore
    private Long detRowId;

    @Column(name = "FAV_AC_POID", nullable = false)
    @AuditIgnore
    private Long favAcPoid;

    @Column(name = "GL_POID", nullable = false)
    private Long glPoid;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private Timestamp lastModifiedDate;

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "COMPANY")
    private Long company;

    @Column(name = "VIEW_CATEGORY", length = 50)
    private String viewCategory;
}

