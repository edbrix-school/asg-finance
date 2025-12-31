package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class CreditNoteListResponseDto {
    private Long transactionPoid;
    private String docRef;
    private Timestamp transactionDate;
    private String partyType;
    private String partyName;
    private String refType;
    private String currencyCode;
    private BigDecimal grandTotal;
    private String companyName;
}