package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxSubmissionResponse {

    private Long transactionPoid;
    private Long companyId;
    private String companyName;
    private Timestamp periodFrom;
    private Timestamp periodTo;
    private String remarks;
    private String periodClosedBy;
    private Timestamp periodClosedDate;
    private String docRef;
    private Timestamp transactionDate;
    private String approvalStatus;
    private String status;
    private Long groupPoid;
    private String createdBy;
    private Timestamp createdDate;
    private String lastmodifiedBy;
    private Timestamp lastmodifiedDate;

    // Details
    private List<TaxSubmissionDetailResponse> details;
}


