package com.asg.finance.entity;

import com.asg.finance.entity.key.SupplierMasterQstnDtlKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_MASTER_QSTN_DTL")
public class SupplierMasterQstnDtlEntity {
    @EmbeddedId
    private SupplierMasterQstnDtlKey id;

    @Column(name = "QSTN")
    private String questionaries;

    @Column(name = "ANSWERS")
    private String answers;

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
