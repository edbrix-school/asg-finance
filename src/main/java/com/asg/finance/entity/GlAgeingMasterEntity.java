package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
public class GlAgeingMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AGEING_POID")
    @AuditIgnore
    private Long ageingPoid;

    @Column(name = "GROUP_POID", nullable = false)
    @AuditIgnore
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
    @AuditIgnore
    private String deleted;

    @OneToMany(
            mappedBy = "ageingMaster",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<GlAgeingMasterDtlEntity> details = new ArrayList<>();
}
