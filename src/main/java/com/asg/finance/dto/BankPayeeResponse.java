package com.asg.finance.dto;

import lombok.Data;

@Data
public class BankPayeeResponse {

    private Long poid;
    private String payingName;
    private String payingName2;
    private String remarks;
    private String active;
    private Integer seqNo;
    // Getters and Setters
}
