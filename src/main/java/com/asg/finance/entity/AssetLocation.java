package com.asg.finance.entity;

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
    private Long locationPoid;

    @Column(name = "LOCATION_CODE", nullable = false, unique = true)
    private String locationCode;

    @Column(name = "DESCRIPTION", nullable = false, unique = true)
    private String description;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "ACTIVE", nullable = false,length = 1)
    private String active;



}