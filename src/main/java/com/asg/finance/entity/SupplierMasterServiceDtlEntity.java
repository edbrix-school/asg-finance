package com.asg.finance.entity;

import com.asg.finance.entity.key.SupplierMasterServiceDtlKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_MASTER_SERVCS_DTL")
public class SupplierMasterServiceDtlEntity {
    @EmbeddedId
    private SupplierMasterServiceDtlKey id;

    @Column(name = "SERVICE_POID")
    private Long servicePoid;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}