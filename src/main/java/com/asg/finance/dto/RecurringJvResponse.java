package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringJvResponse {

    private Long transactionPoid;
    private String narration;
    private LocalDate startDate;
    private BigDecimal totalAmount;
    private Integer noOfMonths;
    private Long monthWiseAmt;
    private String refType;
    private Long employeeId;
    private Long assetId;
    private LovGetListDto employeeDet;
    private LovGetListDto assetDet;
    private LovGetListDto companyDet;

    private String policyNumber;
    private String remarks;
    private String docRef;
    private LocalDate transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private Boolean billWiseCapable;

    private Boolean glPosting;
    private BigDecimal drTotal;
    private BigDecimal crTotal;

    private List<RecurringJvDetailResponse> details;

    private List<RecurringJvScheduleDetailResponse> scheduleDetails;
}
