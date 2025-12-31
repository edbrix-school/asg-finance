package com.asg.finance.entity.master;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_CHARGE_GROUP_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipChargeGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHARGE_GROUP_POID", nullable = false)
    private Long chargeGroupPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid = 1L;

    @Column(name = "CHARGE_GROUP_CODE", length = 20, nullable = false)
    private String chargeGroupCode;

    @Column(name = "CHARGE_GROUP_NAME", length = 1000, nullable = false)
    private String chargeGroupName;

    @Column(name = "CHARGE_GROUP_NAME2", length = 200)
    private String chargeGroupName2;

    @Column(name = "CHARGE_GL_PAYABLE")
    private Long chargeGlPayable;

    @Column(name = "LINEWISE_PAYABLE_POSTING", length = 1)
    private String linewisePayablePosting = "Y";

    @Column(name = "CHARGE_GL_SALE")
    private Long chargeGlSale;

    @Column(name = "CHARGE_GL_COST_SALE")
    private Long chargeGlCostSale;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "GL_PREFIX", length = 20)
    private String glPrefix;
}
