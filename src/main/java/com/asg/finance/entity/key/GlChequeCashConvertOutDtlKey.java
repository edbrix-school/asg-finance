package com.asg.finance.entity.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Data;

@Data
@Embeddable
public class GlChequeCashConvertOutDtlKey implements Serializable {

    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;
}
