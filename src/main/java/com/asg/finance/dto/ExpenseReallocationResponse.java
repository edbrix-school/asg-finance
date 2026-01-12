package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationResponse {

    private Long transactionPoid;
    private Timestamp transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private String companyName;
    private String docRef;
    private String narration;
    private Long expenseGroupGlId;
    private String expenseGroupGlName;
    private Long fromCompanyId;
    private String fromCompanyName;
    private Long jvPoid;
    private String jvRef;
    private String remarks;
    private Timestamp fromDate;
    private Timestamp toDate;
    private String costPoid;
    private String reportGeneration;
    private String allocationType;
    private String createdBy;
    private Timestamp createdDate;
    private String lastmodifiedBy;
    private Timestamp lastmodifiedDate;
    private String deleted;

    // Computed fields for UI
    private Boolean glPosting; // false - no direct GL posting
    private String reportName; // To be confirmed from SRS

    // Detail lines
    private List<ExpenseReallocationDetailResponse> details;

    // Detail totals
    private DetailTotals detailTotals;

    // Excel detail lines (optional)
    private List<ExpenseReallocationXlDetailResponse> xlDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailTotals {
        private BigDecimal totalSh;
        private BigDecimal totalFf;
        private BigDecimal totalFfs;
        private BigDecimal totalFfp;
        private BigDecimal totalProperties;
        private BigDecimal totalMta;
        private BigDecimal totalPda;
        private BigDecimal totalAdmin;
        private BigDecimal grandTotal;
    }
}

