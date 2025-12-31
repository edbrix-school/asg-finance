package com.asg.finance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "GL_COST_CENTER_MASTER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COST_CENTER_POID")
    private Long costCenterPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COST_CENTER_CODE", length = 50)
    private String costCenterCode;

    @Column(name = "COST_CENTER_DESCRIPTION", length = 100)
    private String costCenterDescription;

    @Column(name = "COST_CENTER_GROUP_TYPE")
    private Long costCenterGroupTypePoid;

    @Column(name = "COST_CENTER_GROUP", length = 50)
    private String costGroupType;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "MIS_GROUP", length = 100)
    private String misGroup;

    @Column(name = "COST_CENTER_DESCRIPTION2", length = 100)
    private String costCenterDescription2;

    @Column(name = "REMARKS",length = 100)
    private String remarks;

    @Column(name = "COST_CENTER_CHILD", length = 1)
    private String costCenterChild;

    @Column(name = "ACTIVE",length = 1)
    private String active;

    @Column(name = "DELETED",length = 1)
    private String deleted;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "COST_CENTER_TYPE", length = 20)
    private String costCenterType;

    @Column(name = "PARENT_COST_CENTER_POID")
    private Long parentCostCenterPoid;
}
