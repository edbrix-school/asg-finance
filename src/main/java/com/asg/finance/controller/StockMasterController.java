package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.security.util.UserContext;

import com.asg.common.lib.dto.StockInfoDto;
import com.asg.finance.service.StockMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/api/stock-master")
@RequiredArgsConstructor
public class StockMasterController {
    
    private final StockMasterService stockMasterService;
    private final LoggingService loggingService;

    @GetMapping("/{stockPoid}")
    public ResponseEntity<?> getStockByPoid(@PathVariable Long stockPoid) {
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), stockPoid.toString());
        return success("Successfully retrieved stock info", stockMasterService.getStockInfo(stockPoid));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> getStocksByPoids(@RequestBody List<Long> stockPoids) {
        return success("Successfully retrieved stock info", stockMasterService.getStockInfoList(stockPoids));
    }
}
