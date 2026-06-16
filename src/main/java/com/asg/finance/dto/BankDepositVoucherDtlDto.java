package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDepositVoucherDtlDto {
    private Long detRowId;
    private String actionType;

    @NotNull(message = "Bank POID is required")
    private Long bankPoid;

    private LovGetListDto bankDet;

    @NotNull(message = "Payment type is required")
    private String pymtType;
    private DrilldownLinkInfoDto drilldownLinkInfo;
    private Long refDocPoid;
    private String refDocRef;
    private LocalDate rcpDate;
    private String chqAcName;
    private String chqAcNo;
    private String chqCardNo;
    private LocalDate chqDate;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String remarks;
    private String selected;
    @NotBlank(message = "Cheque sequence number is required")
    @Pattern(regexp = "\\d+", message = "Cheque sequence number must contain only numbers")
    private String chqSeqNum;
    private Long paymentMainPoid;
    private String refDocId;
}
