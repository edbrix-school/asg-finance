package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "GL_MASTER_COMPANY_DTL")
@IdClass(GLMasterCompanyDtlEntity.CompositeKey.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GLMasterCompanyDtlEntity {

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long id;

    @Id
    @Column(name = "GL_POID", nullable = false)
    private Long glPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "REMARKS", length = 250)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GL_POID", nullable = false, insertable = false, updatable = false)
    private GLMasterEntity glMaster;

    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long id;      // Position 1 (DET_ROW_ID) - matches database constraint order
        private Long glPoid;  // Position 2
    }
}
