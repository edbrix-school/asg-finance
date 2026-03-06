package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import com.asg.finance.entity.key.SupplierMasterQstnDtlKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_MASTER_QSTN_DTL")
public class SupplierMasterQstnDtlEntity extends BaseEntity {
    @EmbeddedId
    @AuditIgnore
    private SupplierMasterQstnDtlKey id;

    @Column(name = "QSTN")
    private String questionaries;

    @Column(name = "ANSWERS")
    private String answers;

    @Column(name = "REMARKS")
    private String remarks;
}
