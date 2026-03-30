package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ItemDetailDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long stockPoid;
    private LovGetListDto stockDet;
    private Long stockUnitPoid;
    private LovGetListDto stockUnitDet;
    private BigDecimal poQty;
    private BigDecimal dnQty;
    private BigDecimal qtyReceived;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal total;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
    private String actionType;
}
