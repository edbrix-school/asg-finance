package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.dto.StockInfoDto;
import com.asg.finance.service.StockMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/stock-master")
@RequiredArgsConstructor
public class StockMasterController {
    
    private final StockMasterService stockMasterService;

    @GetMapping("/{stockPoid}")
    public ResponseEntity<?> getStockByPoid(@PathVariable Long stockPoid) {
        return success("Successfully retrieved stock info", stockMasterService.getStockInfo(stockPoid));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> getStocksByPoids(@RequestBody List<Long> stockPoids) {
        return success("Successfully retrieved stock info", stockMasterService.getStockInfoList(stockPoids));
    }
}
