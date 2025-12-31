package com.asg.finance.entity.key;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class SupplierMasterQstnDtlKey {

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;
}
