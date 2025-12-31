package com.asg.finance.service;


import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.masters.FixedAssetRequestDto;
import com.asg.finance.dto.masters.FixedAssetResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FixedAssetService {
    FixedAssetResponseDto createFixedAsset(FixedAssetRequestDto request);
    FixedAssetResponseDto updateFixedAsset(Long faPoid, FixedAssetRequestDto request);
    FixedAssetResponseDto getFixedAssetById(Long faPoid);
    void softDeleteFixedAsset(Long faPoid);
    List<Long> createMultipleCopies(Long faPoid, int noOfCopies);
    Map<String, Object> listFixedAssetCategories(String documentId, FilterRequestDto filters, Pageable pageable);
}

