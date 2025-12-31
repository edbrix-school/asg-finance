package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "GL_AGEING_MASTER_DTL")
@IdClass(GlAgeingMasterDtlEntity.CompositeKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlAgeingMasterDtlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ageingDtlSeq")
    @SequenceGenerator(name = "ageingDtlSeq", sequenceName = "GL_AGEING_MASTER_DTL_SEQ", allocationSize = 1)
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Id
    @Column(name = "AGEING_POID", nullable = false)
    private Long ageingPoid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGEING_POID", nullable = false, insertable = false, updatable = false)
    private GlAgeingMasterEntity ageingMaster;

    @Column(name = "BREAKUP_TITLE", length = 50, nullable = false)
    private String breakupTitle;

    @Column(name = "BREAKUP_FROM", nullable = false)
    private Integer breakupFrom;

    @Column(name = "BREAKUP_TO", nullable = false)
    private Integer breakupTo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long detRowId;
        private Long ageingPoid;
    }
}
