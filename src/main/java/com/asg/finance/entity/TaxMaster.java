package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_TAX_MASTER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_CODE",nullable = false, unique = true, length = 50)
    private String taxCode;

    @Column(name = "TAX_NAME", nullable = false, length = 100)
    private String taxName;

    @Column(name = "TAX_NAME2")
    private String taxName2;

    @Column(name = "PERCENTAGE")
    private Double percentage;

    @Column(name = "TAX_TYPE",nullable = false, length = 20)
    private String taxType;

    @Column(name = "GL_CREDIT_DEBIT",nullable = false, length = 20)
    private String glType;

    @Column(name = "GL_POID",nullable = false, length = 50)
    private Long glLedgerPoid;

    @Column(name = "TAX_CATEGORY",nullable = false, length = 50)
    private String taxCategory;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "ACTIVE",length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted="N";

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


}
