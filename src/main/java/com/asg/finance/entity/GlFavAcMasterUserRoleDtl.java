package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity class for GL_FAV_AC_MASTER_USER_ROLE_DTL table
 * Detail table to store user role mappings for Key Favorite Account Master
 * Links favorite account groups to specific user roles for access control
 */
@Entity
@Table(name = "GL_FAV_AC_MASTER_USER_ROLE_DTL")
@IdClass(GlFavAcMasterUserRoleDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlFavAcMasterUserRoleDtl {

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    @AuditIgnore
    private Long detRowId;

    @Id
    @Column(name = "FAV_AC_POID", nullable = false)
    @AuditIgnore
    private Long favAcPoid;

    @Column(name = "USER_ROLE_POID", nullable = false)
    private Long userRolePoid;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long detRowId;
        private Long favAcPoid;
    }
}

