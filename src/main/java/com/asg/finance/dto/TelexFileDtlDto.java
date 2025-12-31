package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
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
public class TelexFileDtlDto {
    private Long detRowId;
    private Long debitTransactionPoid;
    private LocalDate debitTransactionDate;
    private Long debitCompanyPoid;
    private LovGetListDto debitCompanyDet;
    private String debitDocRef;
    private String debitPayingToName;
    private String debitPayingType;
    private String debitLongNarration;
    private LocalDate debitTtDate;
    private String debitCurrencyCode;
    private BigDecimal debitCurrencyRate;
    private BigDecimal debitCurrencyAmt;
    private BigDecimal debitAmount;
    private String deleted;
    private String selected;
    private String drilldownLinkInfo;
    private String debitTtChargeType;
}
