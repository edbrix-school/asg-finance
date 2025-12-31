package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_SERVICES_MASTER")
public class SupplierServicesMasterEntity {
    @Id
    @Column(name = "SERVICE_POID", nullable = false)
    private Long servicePoid;

    @Column(name = "SERVICE_NAME", length = 1000)
    private String serviceName;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 20)
    private String deleted = "N";
}

