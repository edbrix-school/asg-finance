package com.asg.finance.service.impl;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.dto.AssetLocationMasterRequestDto;
import com.asg.finance.dto.AssetLocationMasterResponseDto;
import com.asg.finance.entity.AssetLocation;
import com.asg.finance.repository.AssetLocationMasterRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.AssetLocationMasterService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AssetLocationMasterServiceImpl implements AssetLocationMasterService {

    @Autowired
    private AssetLocationMasterRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DocumentSearchService documentService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private DocumentDeleteService documentDeleteService;


    @Transactional
    public AssetLocationMasterResponseDto createAssetLocationMaster(AssetLocationMasterRequestDto dto) {
        if (repository.existsByLocationCode(dto.getLocationCode())) {
            throw new IllegalArgumentException("Location Code already exists");
        }

        if (repository.existsByDescription(dto.getDescription())) {
            throw new IllegalArgumentException("Description already exists");
        }

        String currentUser = getCurrentUser();

        AssetLocation entity = new AssetLocation();
        entity.setLocationCode(dto.getLocationCode());
        entity.setDescription(dto.getDescription());
        entity.setSeqNo(dto.getSeqNo());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setDeleted("N");
        entity.setActive(String.valueOf(dto.getActive()));
        entity.setCreatedBy(currentUser);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(currentUser);
        entity.setLastModifiedDate(LocalDateTime.now());

        entity = repository.save(entity);
        
        // Log the creation
        String key = entity.getLocationPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);
        
        return convertEntityToResponseDto(entity);
    }

    private AssetLocationMasterResponseDto convertEntityToResponseDto(AssetLocation entity) {
        AssetLocationMasterResponseDto assetLocationMasterResponseDto = new AssetLocationMasterResponseDto();
        BeanUtils.copyProperties(entity, assetLocationMasterResponseDto);
        return assetLocationMasterResponseDto;
    }

    @Override
    @Transactional
    public AssetLocationMasterResponseDto updateAssetLocationMaster(Long locationPoid, AssetLocationMasterRequestDto dto) {
        AssetLocation entity = repository.findById(locationPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Asset Location not found for Location POID: ", "locationPoid", locationPoid));

        // Create a copy of the existing entity for logging

        AssetLocation oldEntity = new AssetLocation();
        BeanUtils.copyProperties(entity, oldEntity);

        // Uniqueness checks (excluding current record)
        if (repository.existsByLocationCodeAndLocationPoidNot(dto.getLocationCode(), locationPoid)) {
            throw new IllegalArgumentException("Location Code already exists");
        }

        if (repository.existsByDescriptionAndLocationPoidNot(dto.getDescription(), locationPoid)) {
            throw new IllegalArgumentException("Description already exists");
        }

        entity.setLocationCode(dto.getLocationCode());
        entity.setDescription(dto.getDescription());
        entity.setSeqNo(dto.getSeqNo());
        entity.setDeleted("N");
        entity.setActive(String.valueOf(dto.getActive()));
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        AssetLocation savedEntity = repository.save(entity);
        
        // Log the update
        String key = savedEntity.getLocationPoid().toString();
        loggingService.logChanges(oldEntity, savedEntity, AssetLocation.class, 
                UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, "LOCATION_POID");
        
        return convertEntityToResponseDto(savedEntity);
    }

    @Override
    @Transactional
    public void softDeleteAssetLocationMaster(Long locationPoid, DeleteReasonDto deleteReasonDto) {
        AssetLocation entity = repository.findById(locationPoid)
                .orElseThrow(() -> new IllegalArgumentException("Asset Location not found for POID: " + locationPoid));

        documentDeleteService.deleteDocument(
                locationPoid,
                "FIXED_ASSET_LOCN_MASTER",
                "LOCATION_POID",
                deleteReasonDto,
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AssetLocationMasterResponseDto getAssetLocationMasterById(Long locationPoid) {
        AssetLocation assetLocation = repository.findById(locationPoid)
                .orElseThrow(() -> new IllegalArgumentException("Asset Location not found for POID: " + locationPoid));
        return convertEntityToResponseDto(assetLocation);
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }


    @Override
    public Map<String, Object> listAssetLocations(String documentId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "LOCATION_CODE",   // label (was DIVISION_NAME)
                "LOCATION_POID");  // value (was DIVISION_POID)

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }
}
