package com.asg.finance.service;


import com.asg.common.lib.dto.LovGetListDto;

public interface ChargeLovService {
    Long getChargePoid(String chargeCode);
    LovGetListDto getChargeDet(Long chargePoid, String refType);
}

