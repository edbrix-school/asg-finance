package com.asg.finance.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
@Embeddable
public class ChequeReturnDetailId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}

