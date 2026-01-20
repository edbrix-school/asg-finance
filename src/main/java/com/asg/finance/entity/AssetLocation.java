package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "FIXED_ASSET_LOCN_MASTER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOCATION_POID")
    @AuditIgnore
    private Long locationPoid;

    @Column(name = "LOCATION_CODE", nullable = false, unique = true)
    @AuditIgnore
    private String locationCode;

    @Column(name = "DESCRIPTION", nullable = false, unique = true)
    private String description;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "GROUP_POID", nullable = false)
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "CREATED_BY")
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED")
    @AuditIgnore
    private String deleted;

    @Column(name = "ACTIVE", nullable = false,length = 1)
    @AuditIgnore
    private String active;

}