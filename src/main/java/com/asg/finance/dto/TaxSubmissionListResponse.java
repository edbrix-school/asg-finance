package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxSubmissionListResponse {

    private Long transactionPoid;
    private Timestamp transactionDate;
    private String docRef;
    private Timestamp periodFrom;
    private Timestamp periodTo;
    private Long companyId;
    private String companyName;
    private String status;
    private String approvalStatus;
    private String periodClosedBy;
    private Timestamp periodClosedDate;
    private String createdBy;
    private Timestamp createdDate;
}


