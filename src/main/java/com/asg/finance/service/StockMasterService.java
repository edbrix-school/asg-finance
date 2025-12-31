package com.asg.finance.service;

import com.asg.common.lib.dto.StockInfoDto;

import java.util.List;

public interface StockMasterService {
    StockInfoDto getStockInfo(Long stockPoid);
    List<StockInfoDto> getStockInfoList(List<Long> stockPoids);
}
