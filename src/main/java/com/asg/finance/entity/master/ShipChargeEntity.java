package com.asg.finance.entity.master;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_CHARGE_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipChargeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHARGE_POID", nullable = false)
    private Long chargePoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid = 1L;

    @Column(name = "CHARGE_CODE", nullable = false, length = 20)
    private String chargeCode;

    @Column(name = "CHARGE_NAME", nullable = false, length = 1000)
    private String chargeName;

    @Column(name = "CHARGE_NAME2", length = 200)
    private String chargeName2;

    @Column(name = "CHARGE_REVENUE_TYPE", length = 50)
    private String chargeRevenueType = "BOTH";

    @Column(name = "CHARGE_TYPE", length = 50)
    private String chargeType = "BOTH";

    @Column(name = "CHARGE_GL_REVENUE")
    private Long chargeGlRevenue;

    @Column(name = "CHARGE_GL_COST")
    private Long chargeGlCost;

    @Column(name = "CHARGE_GL_WIP")
    private Long chargeGlWip;

    @Column(name = "ACTIVE", length = 1)
    private String active;

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

    @Column(name = "CHARGE_APPLICABLE_TYPE", length = 25)
    private String chargeApplicableType = "PERQUANTITY";

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CHARGE_PAYABLE_GL")
    private Long chargePayableGl;

    @Column(name = "DIVISION_CODE", nullable = false, length = 25)
    private String divisionCode = "SH";

    @Column(name = "FDA_GL_REVENUE")
    private Long fdaGlRevenue;

    @Column(name = "FDA_GL_COST")
    private Long fdaGlCost;

    @Column(name = "SH_FF_CHARGE_MAP")
    private Long shFfChargeMap;

    @Column(name = "SH_FF_CHARGE_GL_POID")
    private Long shFfChargeGlPoid;

    @Column(name = "SH_FF_CHARGE_GL_POID_REV")
    private Long shFfChargeGlPoidRev;

    @Column(name = "VISIBLE_IN_FF", length = 1)
    private String visibleInFf = "Y";

    // 🔗 Foreign key to GLOBAL_TAX_MASTER
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "TAX_POID", nullable = false, foreignKey = @ForeignKey(name = "SHIP_CHARGE_MASTER_FK1"))
    //private GlobalTaxMasterEntity taxMaster;

    @Column(name = "INPUT_TAX_POID")
    private Long inputTaxPoid;

    @Column(name = "OLD_CHARGE_GL_REVENUE")
    private Long oldChargeGlRevenue;

    @Column(name = "OLD_CHARGE_GL_COST")
    private Long oldChargeGlCost;

    @Column(name = "DIRECT_REVENUE_GL")
    private Long directRevenueGl;

    @Column(name = "CHARGE_GROUP_POID")
    private Long chargeGroupPoid;

    @Column(name = "DIRECT_COST_OF_SALE_GL")
    private Long directCostOfSaleGl;

    @Column(name = "DIRECT_PAYABLE_GL")
    private Long directPayableGl;
}
