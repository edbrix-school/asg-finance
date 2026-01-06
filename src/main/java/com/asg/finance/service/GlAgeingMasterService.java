package com.asg.finance.service;

import com.asg.finance.dto.GlAgeingMasterDto;
import com.asg.finance.dto.GlAgeingMasterResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface GlAgeingMasterService {
    
    GlAgeingMasterResponseDto createAgeingMaster(GlAgeingMasterDto ageingMasterDto);
    
    GlAgeingMasterDto fetchAgeingMaster(Long ageingPoid);
    
    GlAgeingMasterDto updateAgeingMaster(Long ageingPoid, GlAgeingMasterDto ageingMasterDto);

    void softDeleteAgeingMaster(Long ageingPoid);

    Map<String, Object> listAgeingMasters(String documentId, FilterRequestDto filters, Pageable pageable);
}
