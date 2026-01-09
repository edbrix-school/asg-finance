package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.FixedAssetCategoryRequestDto;
import com.asg.finance.dto.FixedAssetCategoryResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface FixedAssetCategoryService {
    FixedAssetCategoryResponseDto createFixedAssetCategory(FixedAssetCategoryRequestDto request);
    FixedAssetCategoryResponseDto updateFixedAssetCategory(Long faCategoryPoid, FixedAssetCategoryRequestDto requestDto);
    FixedAssetCategoryResponseDto getFixedAssetCategory(Long faCategoryPoid);
    void softDeleteFixedAssetCategory(Long faCategoryPoid, DeleteReasonDto deleteReasonDto);
    Map<String, Object> listFixedAssetCategories(String documentId, FilterRequestDto filters, Pageable pageable);
}
