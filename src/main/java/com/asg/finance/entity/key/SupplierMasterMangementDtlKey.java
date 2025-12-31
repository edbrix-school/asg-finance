package com.asg.finance.entity.key;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class SupplierMasterMangementDtlKey {


    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

}
