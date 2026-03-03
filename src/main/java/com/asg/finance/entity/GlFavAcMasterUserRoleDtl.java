package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
public class GlFavAcMasterUserRoleDtl extends BaseEntity {

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long detRowId;
        private Long favAcPoid;
    }
}

