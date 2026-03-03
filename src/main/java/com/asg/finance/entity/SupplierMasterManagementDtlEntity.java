package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.SupplierMasterMangementDtlKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_MASTER_MGMNT_DTL")
public class SupplierMasterManagementDtlEntity extends BaseEntity {
    @EmbeddedId
    @AuditIgnore
    private SupplierMasterMangementDtlKey id;

    @Column(name = "MGMNT_NAME")
    private String name;

    @Column(name = "MGMNT_DESGTN")
    private String designation;

    @Column(name = "MGMNT_MOBILE")
    private Long mobile;

    @Column(name = "MGMNT_EMAIL")
    private String email;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "MGMNT_TELEPHONE1")
    @AuditIgnore
    private String telephone1;

    @Column(name = "MGMNT_TELEPHONE")
    private Long telephone;

    @Column(name = "MANAGMENT_MOBILE")
    private String managementMobile;

    @Column(name = "MANAGMENT_TELEPHONE")
    @AuditIgnore
    private String managementTelephone;
}