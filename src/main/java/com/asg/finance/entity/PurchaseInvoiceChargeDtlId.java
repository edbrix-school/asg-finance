package com.asg.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PurchaseInvoiceChargeDtlId implements Serializable {
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;
}
