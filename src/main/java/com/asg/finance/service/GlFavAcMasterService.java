package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.GlFavAcMasterRequest;
import com.asg.finance.dto.GlFavAcMasterResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service interface for Key Favorite Account Master operations
 */
public interface GlFavAcMasterService {

    GlFavAcMasterResponse createFavoriteAccount(GlFavAcMasterRequest request);

    GlFavAcMasterResponse updateFavoriteAccount(Long favAcPoid, GlFavAcMasterRequest request);

    GlFavAcMasterResponse getFavoriteAccountById(Long favAcPoid);

    void softDeleteFavoriteAccount(Long favAcPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable);
}

