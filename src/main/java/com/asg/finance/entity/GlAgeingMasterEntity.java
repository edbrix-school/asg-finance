package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GL_AGEING_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "GL_AGEING_MASTER_UK_DESC", columnNames = "DESCRIPTION")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlAgeingMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AGEING_POID")
    private Long ageingPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "DESCRIPTION", length = 100, nullable = false)
    private String description;

    @Column(name = "DESCRIPTION2", length = 100)
    private String description2;

    @Column(name = "AGEING_BREAKUP_TYPE", length = 20)
    private String ageingBreakupType;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @OneToMany(
            mappedBy = "ageingMaster",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<GlAgeingMasterDtlEntity> details = new ArrayList<>();
}
