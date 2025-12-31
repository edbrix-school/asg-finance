package com.asg.finance.entity.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "STOCK_CATEGORY_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCategoryMasterEntity {
    @Id
    @Column(name = "CATEGORY_POID", nullable = false)
    private Long categoryPoid;

    @Column(name = "CATEGORY_CODE", length = 20, nullable = false, unique = true)
    private String categoryCode;

    @Column(name = "CATEGORY_NAME", length = 100, unique = true)
    private String categoryName;

    @Column(name = "CATEGORY_NAME2", length = 100)
    private String categoryName2;

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

    @Column(name = "CATEGORY_TYPE", length = 30)
    private String categoryType;

    // ===================== RELATIONSHIPS =====================

    // Many categories can belong to one group
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "GROUP_POID", referencedColumnName = "GROUP_POID", foreignKey = @ForeignKey(name = "STOCK_CATEGORY_MASTER_FK1"))
    //private GlobalGroupMasterEntity group;

    // Parent category (self-reference)
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "PARENT_CATEGORY_POID", referencedColumnName = "CATEGORY_POID")
    //private StockCategoryMasterEntity parentCategory;

    // GL references (disabled in DB but we can define nullable relations)
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "STOCK_GL_POID", referencedColumnName = "GL_POID")
    //private GlMasterEntity stockGl;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "COST_OF_SALES_GL_POID", referencedColumnName = "GL_POID")
    //private GlMasterEntity costOfSalesGl;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "SALES_GL_POID", referencedColumnName = "GL_POID")
    //private GlMasterEntity salesGl;

    // Cost center reference
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "COST_CENTER_POID", referencedColumnName = "COST_CENTER_POID")
    //private GlCostCenterMasterEntity costCenter;

    // Tax references
    @Column(name = "OUTPUT_TAX_POID")
    private Long outputTaxPoid;

    @Column(name = "INPUT_TAX_POID")
    private Long inputTaxPoid;
}
