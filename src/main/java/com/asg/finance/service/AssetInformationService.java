package com.asg.finance.service;

import com.asg.finance.dto.AssetInformationMasterRequest;
import com.asg.finance.dto.AssetInformationMasterResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service interface for Information Asset Master operations
 */
public interface AssetInformationService {

    /**
     * Create a new Information Asset Master
     */
    AssetInformationMasterResponse createAssetInformation(AssetInformationMasterRequest request);

    /**
     * Update an existing Information Asset Master
     */
    AssetInformationMasterResponse updateAssetInformation(Long iaPoid, AssetInformationMasterRequest request);

    /**
     * List Asset Information with search and pagination
     */
    Map<String, Object> listAssetInformation(String documentId, FilterRequestDto filters, Pageable pageable);
    /**
     * Get Asset Information by ID
     */
    AssetInformationMasterResponse getAssetInformationByPoidId(Long iaPoid);

    /**
     * Soft delete an Asset Information record (mark deleted = 'Y')
     */
    void softDeleteAssetInformationByPoidId(Long iaPoid);

}
