package com.asg.finance.entity.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class ArDebitNoteDtlKey implements Serializable {

    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;
}