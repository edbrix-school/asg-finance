package com.asg.finance.service;


import com.asg.common.lib.dto.LovGetListDto;

public interface ChargeLovService {
    LovGetListDto getChargeDet(Long chargePoid, String refType);
}

