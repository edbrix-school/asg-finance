package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelexFileGenerateResponseDto {
    private Long transactionPoid;
    private LocalDate transactionDate;
    private Long groupPoid;
    private LovGetListDto groupDet;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private String docRef;
    private Long bankPoid;
    private LovGetListDto bankDet;
    private String remarks;
    private String bankList;
    private boolean approvalOnly;
    private boolean suppressBalanceCheck;
    private String fileName;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private List<TelexFileDtlDto> details;
}
