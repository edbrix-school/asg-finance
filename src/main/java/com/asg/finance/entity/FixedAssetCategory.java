package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "FIXED_ASSET_CATEGORY_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAssetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FA_CATEGORY_POID", nullable = false)
    private Long faCategoryPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "FA_CATG_CODE", length = 20, updatable = false)
    private String faCategoryCode;

    @Column(name = "FA_CATG_DESCRIPTION", length = 300)
    private String faCategoryDescription;

    @Column(name = "FA_CATG_DESCRIPTION2", length = 300)
    private String faCategoryDescription2;

    @Column(name = "ASSET_TYPE", length = 300)
    private String assetType;

    @Column(name = "FA_GL_ACCOUNT", length = 20)
    private String faGlAccount;

    @Column(name = "FA_ACCUMULATION_AC", length = 20)
    private String faAccumulationAccount;

    @Column(name = "FA_DEPRECIATION_AC", length = 20)
    private String faDepreciationAccount;

    @Column(name = "COST_CENTER", length = 100)
    private String costCenter;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "USER_ROLE_POID", length = 300)
    private String userRolePoid;
}

