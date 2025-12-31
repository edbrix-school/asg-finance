package com.asg.finance.service;

import com.asg.finance.dto.AssetLocationMasterRequestDto;
import com.asg.finance.dto.AssetLocationMasterResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;
import java.util.Map;

public interface AssetLocationMasterService {
    AssetLocationMasterResponseDto createAssetLocationMaster(AssetLocationMasterRequestDto request);

    AssetLocationMasterResponseDto updateAssetLocationMaster(Long assetPoid, AssetLocationMasterRequestDto request);

    AssetLocationMasterResponseDto getAssetLocationMasterById(Long assetPoid);

    void softDeleteAssetLocationMaster(Long assetPoid);

    Map<String, Object> listAssetLocations(String documentId, FilterRequestDto filters, Pageable pageable);

}

