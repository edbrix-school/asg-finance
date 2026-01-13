package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

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
    private Long fdaDetRowId;
    private String checkAll;

    private BigDecimal pdaAmount;
    private BigDecimal ffAmount;
    private String chargeFrom;

    private String vatPartyName;
    private String partyInvNumber;
    private Date partyInvDate;
    private Long taxPoid;
    private DetailsDto taxPoidDtl;
}
