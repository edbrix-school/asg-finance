package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entity class for GL_FAV_AC_MASTER_USER_ROLE_DTL table
 * Detail table to store user role mappings for Key Favorite Account Master
 * Links favorite account groups to specific user roles for access control
 */
@Entity
@Table(name = "GL_FAV_AC_MASTER_USER_ROLE_DTL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlFavAcMasterUserRoleDtl {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gl_fav_ac_user_role_dtl_seq")
    @SequenceGenerator(name = "gl_fav_ac_user_role_dtl_seq", sequenceName = "GL_FAV_AC_MASTER_USER_ROLE_DTL_SEQ", allocationSize = 1)
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "FAV_AC_POID", nullable = false)
    private Long favAcPoid;

    @Column(name = "USER_ROLE_POID", nullable = false)
    private Long userRolePoid;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;
}

