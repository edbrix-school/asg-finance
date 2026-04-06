package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxSubmissionResponse {

    private Long transactionPoid;
    private Long companyId;
    private String companyName;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
    private String remarks;
    private String periodClosedBy;
    private LocalDateTime periodClosedDate;
    private String docRef;
    private LocalDateTime transactionDate;
    private String approvalStatus;
    private String status;
    private Long groupPoid;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastmodifiedBy;
    private LocalDateTime lastmodifiedDate;

    // Details
    private List<TaxSubmissionDetailResponse> details;
}


