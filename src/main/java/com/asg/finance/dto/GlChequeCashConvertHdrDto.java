package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GlChequeCashConvertHdrDto {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    @NotBlank(message = "Cheque Conversion Type is required")
    private String type;
    @NotBlank(message = "Cheque Conversion Type is required")
    private String postingNarration;
    private Long cash;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    @NotBlank(message = "Cheque Account No is required")
    private String chqAcNo;
    @NotBlank(message = "Cheque Card No is required")
    private String chqCardNo;
    private Long roundingAmt;

    private LovGetListDto groupDet;
    private LovGetListDto companyDet;

    private List<GlChequeCashConvertInDtlDto> inDtls;
    private List<GlChequeCashConvertOutDtlDto> outDtls;
}
