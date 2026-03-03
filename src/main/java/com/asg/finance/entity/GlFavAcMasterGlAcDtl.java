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
 * Entity class for GL_FAV_AC_MASTER_GL_AC_DTL table
 * Detail table to store GL account mappings for Key Favorite Account Master
 * Includes company and view category for report filtering
 */
@Entity
@Table(name = "GL_FAV_AC_MASTER_GL_AC_DTL")
@IdClass(GlFavAcMasterGlAcDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlFavAcMasterGlAcDtl extends BaseEntity {

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    @AuditIgnore
    private Long detRowId;

    @Id
    @Column(name = "FAV_AC_POID", nullable = false)
    @AuditIgnore
    private Long favAcPoid;

    @Column(name = "GL_POID", nullable = false)
    private Long glPoid;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "COMPANY")
    private Long company;

    @Column(name = "VIEW_CATEGORY", length = 50)
    private String viewCategory;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long detRowId;
        private Long favAcPoid;
    }
}

