package com.asg.finance.service.impl;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.service.ChargeLovService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
@Slf4j
public class ChargeLovServiceImpl implements ChargeLovService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LovDataService lovService;

    public LovGetListDto getChargeDet(Long chargePoid, String refType) {
        String lovName = detectLovNameForCharge(refType);
        return lovService.getDetailsByPoidAndLovName(chargePoid, lovName);
    }

    private String detectLovNameForCharge(String refType) {
        // DN Charges
        if (refType.startsWith("DN")) {
            return "CHARGE_MASTER_IN_CN_FOR_DN";
        }
        // FF Charges
        if (refType.startsWith("FF")) {
            return "CHARGE_MASTER_IN_CN_FOR_DN";
        }
        // Shipping Charges
        if (refType.startsWith("SH")) {
            return "CHARGE_MASTER_IN_CN_FOR_SH";
        }
        // Default (safe fallback)
        return "CHARGE_MASTER_IN_CN_FOR_SH";
    }
}
