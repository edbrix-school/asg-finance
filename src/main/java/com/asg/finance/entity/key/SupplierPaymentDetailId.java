package com.asg.finance.entity.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
public class SupplierPaymentDetailId implements Serializable {

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;
}