package com.asg.finance.service.impl;

import com.asg.common.lib.dto.StockInfoDto;
import com.asg.finance.entity.StockMasterEntity;
import com.asg.finance.repository.StockMasterRepository;
import com.asg.finance.service.StockMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockMasterServiceImpl implements StockMasterService {
    
    private final StockMasterRepository stockMasterRepository;
    
    @Override
    public StockInfoDto getStockInfo(Long stockPoid) {
        StockMasterEntity entity = stockMasterRepository.findByStockPoid(stockPoid)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockPoid));
        return mapToDto(entity);
    }
    
    @Override
    public List<StockInfoDto> getStockInfoList(List<Long> stockPoids) {
        List<StockMasterEntity> entities = stockMasterRepository.findByStockPoidIn(new HashSet<>(stockPoids));
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    private StockInfoDto mapToDto(StockMasterEntity entity) {
        return StockInfoDto.builder()
                .stockPoid(entity.getStockPoid())
                .stockCode(entity.getStockCode())
                .stockName(entity.getStockName())
                .stockName2(entity.getStockName2())
                .categoryPoid(entity.getCategoryPoid())
                .stockUnitPoid(entity.getStockUnitPoid())
                .stockCost(entity.getStockCost())
                .currencyCode(entity.getCurrencyCode())
                .taxPoid(entity.getTaxPoid())
                .serviceItem(entity.getServiceItem())
                .categoryCode(entity.getCategoryCode())
                .build();
    }
}
