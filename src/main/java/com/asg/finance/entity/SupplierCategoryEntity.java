package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "AP_SUPPLIER_CATEGORY_MASTER")
@Getter
@Setter
public class SupplierCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SUPPLIER_CATEGORY_POID")
    private Long supplierCategoryPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "SUPPLIER_CATEGORY_CODE", length = 50, nullable = false, unique = true)
    private String supplierCategoryCode;

    @Column(name = "SUPPLIER_CATEGORY_NAME", length = 100, nullable = false)
    private String supplierCategoryName;

    @Column(name = "SUPPLIER_CATEGORY_NAME2", length = 100)
    private String supplierCategoryName2;

    @Column(name = "ACTIVE", length = 1)
    private String active = "Y";

    @Column(name = "SEQNO")
    private Integer sequenceNumber;

    @Column(name = "CREATED_BY", length = 50, updatable = false)
    private String createdBy;

    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 50)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "GENERAL_REMARKS", length = 500)
    private String generalRemarks;

    @Column(name = "DELETED", nullable = false, length = 1)
    private String deleted = "N";
}


