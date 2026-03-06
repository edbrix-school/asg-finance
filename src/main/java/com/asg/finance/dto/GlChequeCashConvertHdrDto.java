package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
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
    private BigDecimal cash;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;

    private String chqAcNo;

    private String chqCardNo;
    private BigDecimal roundingAmt;

    private LovGetListDto groupDet;
    private LovGetListDto companyDet;

    private List<GlChequeCashConvertInDtlDto> inDtls;
    private List<GlChequeCashConvertOutDtlDto> outDtls;
}
