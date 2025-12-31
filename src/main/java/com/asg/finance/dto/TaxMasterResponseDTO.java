package com.asg.finance.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxMasterResponseDTO {

    private Long taxPoid;

    private String taxCode;

    private String taxName;

    private String taxName2;

    private Double percentage;

    private String taxType;

    private String glType;

    private Long glLedgerPoid;

    private GlLedgerDTO glLedger;

    private String taxCategory;

    private String active;

    private Integer seqNo;

    private Long groupPoid;
}
