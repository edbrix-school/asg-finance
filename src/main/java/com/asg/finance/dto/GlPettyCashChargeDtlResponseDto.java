package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlPettyCashChargeDtlResponseDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long chargePoid;
    private DetailsDto chargePoidDtl;
    private BigDecimal chargeAmount;
    private String description;
    private String remarks;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private String refDocId;
    private Long refDocPoid;
    private DetailsDto refDocPoidDtl;
    private Long fdaDetRowId;
    private String checkAll;

    private BigDecimal pdaAmount;
    private BigDecimal ffAmount;
    private String chargeFrom;

    private String vatPartyName;
    private String partyInvNumber;
    private LocalDate partyInvDate;
    private Long taxPoid;
    private DetailsDto taxPoidDtl;
}
