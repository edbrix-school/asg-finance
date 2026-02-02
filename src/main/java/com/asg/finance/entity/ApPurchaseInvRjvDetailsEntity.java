package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.finance.entity.key.ApPurchaseInvRjvDetailsKey;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "AP_PURCHASE_INV_RJV_DETAILS")
public class ApPurchaseInvRjvDetailsEntity {

    @EmbeddedId
    @AuditIgnore
    private ApPurchaseInvRjvDetailsKey id;

    @AuditIgnore
    @Column(name = "DRILLDOWN_LINK_INFO")
    private String drilldownLinkInfo;

    @Column(name = "RJV_POID")
    private Long rjvPoid;

    @AuditIgnore
    @Column(name = "RJV_TRN_DATE")
    private LocalDate rjvTrnDate;

    @Column(name = "RJV_DOC_REF")
    private String rjvDocRef;

    @AuditIgnore
    @Column(name = "RJV_COMPANY_POID")
    private Long rjvCompanyPoid;

    @Column(name = "RJV_REF_TYPE")
    private String rjvRefType;

    @Column(name = "RJV_AMOUNT")
    private Long rjvAmount;

    @Column(name = "RJV_REMARKS")
    private String rjvRemarks;

    @Column(name = "REMARKS")
    private String remarks;

    @AuditIgnore
    @Column(name = "CREATED_BY")
    private String createdBy;

    @AuditIgnore
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}

