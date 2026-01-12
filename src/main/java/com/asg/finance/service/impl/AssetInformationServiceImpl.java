package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.AssetInformationMasterRequest;
import com.asg.finance.dto.AssetInformationMasterResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.entity.AssetInformationMasterEntity;
import com.asg.finance.repository.reports.AssetInformationRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.service.AssetInformationService;
import com.asg.finance.utility.DatabaseErrorHandler;
import com.asg.common.lib.utility.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for Information Asset Master operations
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AssetInformationServiceImpl implements AssetInformationService {

    private final AssetInformationRepository repository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    public AssetInformationMasterResponse createAssetInformation(AssetInformationMasterRequest request) {
        // Validate uniqueness of IA Code (only if provided)
        if (request.getIaCode() != null && repository.findByIaCode(request.getIaCode()).isPresent()) {
            throw new ValidationException("IA Code already exists: " + request.getIaCode());
        }

        // Validate uniqueness of IA Name
        if (repository.findByIaName(request.getIaName()).isPresent()) {
            throw new ValidationException("IA Name already exists: " + request.getIaName());
        }

        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        // Create entity
        AssetInformationMasterEntity entity = AssetInformationMasterEntity.builder()
                .iaCode(request.getIaCode())
                .iaName(request.getIaName())
                .groupPoid(UserContext.getGroupPoid())
                .companyPoid(UserContext.getCompanyPoid())
                .iaDescription(request.getIaDescription())
                .operatingUnit(request.getOperatingUnit())
                .typeOfInformationAsset(request.getTypeOfInformationAsset())
                .personalData(request.getPersonalData())
                .personalSensitiveData(request.getPersonalSensitiveData())
                .sensitiveCustomerData(request.getSensitiveCustomerData())
                .assetClassification(request.getAssetClassification())
                .integrity(request.getIntegrity())
                .availability(request.getAvailability())
                .dataRetentionPeriod(request.getDataRetentionPeriod())
                .assetCustodian(request.getAssetCustodian())
                .protectionLevelOrigin(request.getAtOrigin())
                .protectionLevelMoved(request.getInformationisMoved())
                .processName(request.getProcessName())
                .processOwner(request.getProcessOwner())
                .seqNo(request.getSeqNo())
                .active(request.getActive())
                .deleted("N")
                .createdBy(currentUser)
                .createdDate(now)
                .lastModifiedBy(currentUser)
                .lastModifiedDate(now)
                .build();

        try {
            AssetInformationMasterEntity savedEntity = repository.save(entity);
            return mapToResponse(savedEntity);
        } catch (DataAccessException ex) {
            log.error("Database error while creating asset information", ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        } catch (Exception ex) {
            log.error("Unexpected error while creating asset information", ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        }
    }

    @Override
    public AssetInformationMasterResponse updateAssetInformation(Long iaPoid, AssetInformationMasterRequest request) {
        AssetInformationMasterEntity existing = repository.findByIaPoid(iaPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Information Asset Master", "iaPoid", iaPoid));

        // Only validate uniqueness if the values have changed
        if (request.getIaCode() != null && !request.getIaCode().equals(existing.getIaCode()) &&
                repository.existsByIaCodeAndIaPoidNot(request.getIaCode(), iaPoid)) {
            throw new ValidationException("IA Code already exists: " + request.getIaCode());
        }

        if (!request.getIaName().equals(existing.getIaName()) &&
                repository.existsByIaNameAndIaPoidNot(request.getIaName(), iaPoid)) {
            throw new ValidationException("IA Name already exists: " + request.getIaName());
        }

        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        // Update entity
        existing.setIaCode(request.getIaCode());
        existing.setIaName(request.getIaName());
        existing.setGroupPoid(UserContext.getGroupPoid());
        existing.setCompanyPoid(UserContext.getCompanyPoid());
        existing.setIaDescription(request.getIaDescription());
        existing.setOperatingUnit(request.getOperatingUnit());
        existing.setTypeOfInformationAsset(request.getTypeOfInformationAsset());
        existing.setPersonalData(request.getPersonalData());
        existing.setPersonalSensitiveData(request.getPersonalSensitiveData());
        existing.setSensitiveCustomerData(request.getSensitiveCustomerData());
        existing.setAssetClassification(request.getAssetClassification());
        existing.setIntegrity(request.getIntegrity());
        existing.setAvailability(request.getAvailability());
        existing.setDataRetentionPeriod(request.getDataRetentionPeriod());
        existing.setAssetCustodian(request.getAssetCustodian());
        existing.setSeqNo(request.getSeqNo());
        existing.setProtectionLevelOrigin(request.getAtOrigin());
        existing.setProtectionLevelMoved(request.getInformationisMoved());
        existing.setProcessName(request.getProcessName());
        existing.setProcessOwner(request.getProcessOwner());
        existing.setActive(request.getActive());
        existing.setLastModifiedBy(currentUser);
        existing.setLastModifiedDate(now);

        try {
            // No need to call save() as the entity is already managed by JPA
            // The changes will be persisted automatically at the end of the transaction
            return mapToResponse(existing);
        } catch (DataAccessException ex) {
            log.error("Database error while updating asset information", ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        } catch (Exception ex) {
            log.error("Unexpected error while updating asset information", ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listAssetInformation(String documentId, FilterRequestDto request, Pageable pageable) {
        try {
            String operator = documentService.resolveOperator(request);
            String isDeleted = documentService.resolveIsDeleted(request);
            List<FilterDto> filters = documentService.resolveFilters(request);

            RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                    "IA_NAME",   // label
                    "IA_POID");    // value

            Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

            return PaginationUtil.wrapPage(page, raw.displayFields());
        } catch (DataAccessException ex) {
            log.error("Database error while listing asset information", ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        } catch (Exception ex) {
            log.error("Unexpected error while listing asset information", ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        }
    }

    /**
     * Get current user from security context
     */
    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    /**
     * Map entity to response DTO
     */
    private AssetInformationMasterResponse mapToResponse(AssetInformationMasterEntity entity) {

        if (entity == null) {
            return null;
        }

        AssetInformationMasterResponse response = new AssetInformationMasterResponse();

        response.setIaPoid(entity.getIaPoid());
        response.setGroupPoid(entity.getGroupPoid());
        response.setCompanyPoid(entity.getCompanyPoid());

        response.setIaCode(entity.getIaCode());
        response.setIaName(entity.getIaName());
        response.setIaDescription(entity.getIaDescription());
        response.setOperatingUnit(entity.getOperatingUnit());

        response.setTypeOfInformationAsset(entity.getTypeOfInformationAsset());
        response.setAssetCustodian(entity.getAssetCustodian());
        response.setAssetClassification(entity.getAssetClassification());

        response.setIntegrity(entity.getIntegrity());
        response.setAvailability(entity.getAvailability());
        response.setDataRetentionPeriod(entity.getDataRetentionPeriod());

        response.setPersonalData(entity.getPersonalData());
        response.setPersonalSensitiveData(entity.getPersonalSensitiveData());
        response.setSensitiveCustomerData(entity.getSensitiveCustomerData());

        response.setActive(entity.getActive());
        response.setDeleted(entity.getDeleted());
        response.setSeqNo(entity.getSeqNo());

        response.setProcessName(entity.getProcessName());
        response.setProcessOwner(entity.getProcessOwner());

        response.setAtOrigin(entity.getProtectionLevelOrigin());
        response.setInformationisMoved(entity.getProtectionLevelMoved());

        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastModifiedBy(entity.getLastModifiedBy());
        response.setLastModifiedDate(entity.getLastModifiedDate());

        return response;
    }

    @Override
    public AssetInformationMasterResponse getAssetInformationByPoidId(Long iaPoid) {
        try {
            // Commented out deleted filter to allow retrieval of deleted records
            // AssetInformationMasterEntity entity = repository.findByIaPoidAndDeleted(iaPoid, "N")
            //         .orElseThrow(() -> new ResourceNotFoundException("Asset Information", "iaPoid", iaPoid));
            AssetInformationMasterEntity entity = repository.findByIaPoid(iaPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Asset Information", "iaPoid", iaPoid));

            return mapToResponse(entity);
        } catch (ResourceNotFoundException ex) {
            // Re-throw ResourceNotFoundException as-is (handled by GlobalExceptionHandler)
            throw ex;
        } catch (DataAccessException ex) {
            log.error("Database error while fetching asset information by ID: {}", iaPoid, ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        } catch (Exception ex) {
            log.error("Unexpected error while fetching asset information by ID: {}", iaPoid, ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
            return null; // Never reached, but needed for compilation
        }
    }

    @Override
    public void softDeleteAssetInformationByPoidId(Long iaPoid, DeleteReasonDto deleteReasonDto) {
        try {
            AssetInformationMasterEntity entity = repository.findByIaPoidAndDeleted(iaPoid, "N")
                    .orElseThrow(() -> new ResourceNotFoundException("Asset Information", "iaPoid", iaPoid));

            documentDeleteService.deleteDocument(
                    iaPoid,
                    "ASSET_INFORMATION_MASTER",
                    "IA_POID",
                    deleteReasonDto,
                    null
            );
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            log.error("Database error while soft deleting asset information: {}", iaPoid, ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
        } catch (Exception ex) {
            log.error("Unexpected error while soft deleting asset information: {}", iaPoid, ex);
            DatabaseErrorHandler.handleDatabaseException(ex);
        }
    }
}
