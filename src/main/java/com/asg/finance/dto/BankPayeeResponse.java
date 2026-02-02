package com.asg.finance.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BankPayeeResponse {

    private Long poid;
    private String payingName;
    private String payingName2;
    private String remarks;
    private String active;
    private Integer seqNo;
    private String createdBy;
    private LocalDateTime createdDate;

    // Getters and Setters
}
