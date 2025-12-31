package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLPettyCashItemDtlResponseDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long stockPoid;
    private DetailsDto stockPoidDtl;
    private Long stockUnitPoid;
    private DetailsDto stockUnitPoidDtl;
    private BigDecimal poQty;
    private BigDecimal dnQty;
    private BigDecimal qtyReceived;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal total;
    private String remarks;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private String refDocId;
    private Long refDocPoid;
    private String checkAll;
    private Long refDetRowId;

    private String vatPartyName;
    private String partyInvNumber;
    private LocalDate partyInvDate;
    private Long taxPoid;
}
