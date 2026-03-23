package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.SupplierMasterServiceDtlKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_MASTER_SERVCS_DTL")
public class SupplierMasterServiceDtlEntity extends BaseEntity {
    @EmbeddedId
    @AuditIgnore
    private SupplierMasterServiceDtlKey id;

    @Column(name = "SERVICE_POID")
    private Long servicePoid;

    @Column(name = "REMARKS")
    private String remarks;
}