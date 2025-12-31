package com.asg.finance.dto;

import lombok.Data;

@Data
public class BankPayItemDto {
    private Long stockPoid;
    private Long stockUnitPoid;
    private Double poQty;
    private Double price;
    private Double total;
    private String remarks;
    private String refDocId;
    private String refDocPoid;
    private String refDetRowId;
}
