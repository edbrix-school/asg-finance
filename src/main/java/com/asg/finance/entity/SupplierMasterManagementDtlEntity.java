package com.asg.finance.entity;

import com.asg.finance.entity.key.SupplierMasterMangementDtlKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_MASTER_MGMNT_DTL")
public class SupplierMasterManagementDtlEntity {
    @EmbeddedId
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

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "MGMNT_TELEPHONE1")
    private String telephone1;

    @Column(name = "MGMNT_TELEPHONE")
    private Long telephone;

    @Column(name = "MANAGMENT_MOBILE")
    private String managementMobile;

    @Column(name = "MANAGMENT_TELEPHONE")
    private String managementTelephone;
}