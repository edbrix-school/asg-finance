package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.FixedAssetCategoryRequestDto;
import com.asg.finance.dto.FixedAssetCategoryResponseDto;
import com.asg.finance.entity.CostCenter;
import com.asg.finance.entity.FixedAssetCategory;
import com.asg.finance.repository.CostCenterRepository;
import com.asg.finance.repository.FixedAssetCategoryRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.FixedAssetCategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedAssetCategoryServiceImpl implements FixedAssetCategoryService {
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final FixedAssetCategoryRepository fixedAssetCategoryRepository;
    private final LoggingService loggingService;
    private final LovDataService lovDataService;

    @Override
    public FixedAssetCategoryResponseDto createFixedAssetCategory(FixedAssetCategoryRequestDto request) {
        if (fixedAssetCategoryRepository.existsByFaCategoryDescriptionIgnoreCase(request.getFaCategoryDescription())) {
            throw new ValidationException("FA Category Description already exists: " + request.getFaCategoryDescription());
        }
            FixedAssetCategory entity = convertFromFixedAssetDtoToFixedAssetEntity(request);
        FixedAssetCategory fixedAssetCategory = fixedAssetCategoryRepository.save(entity);
        
        // Log the creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), fixedAssetCategory.getFaCategoryPoid().toString());
        
        return convertFromFixedAssetEntityToFixedAssetDto(fixedAssetCategory);
    }

    @Override
    public FixedAssetCategoryResponseDto updateFixedAssetCategory(Long faCategoryPoid, FixedAssetCategoryRequestDto requestDto) {
        FixedAssetCategory fixedAssetCategory = fixedAssetCategoryRepository.findById(faCategoryPoid).orElseThrow(
                () -> new ResourceNotFoundException("Fixed Asset Category  not found with ID: ", "faCategoryPoid",faCategoryPoid));
        
        // Create a copy of the old entity for logging
        FixedAssetCategory oldEntity = new FixedAssetCategory();
        BeanUtils.copyProperties(fixedAssetCategory, oldEntity);
        if (fixedAssetCategoryRepository.existsByFaCategoryDescriptionIgnoreCaseAndFaCategoryPoidNot(
                requestDto.getFaCategoryDescription(), faCategoryPoid)) {
            throw new ValidationException("FA Category Description already exists: " + requestDto.getFaCategoryDescription());
        }

        fixedAssetCategory.setFaCategoryDescription(requestDto.getFaCategoryDescription());
        fixedAssetCategory.setFaCategoryDescription2(requestDto.getFaCategoryDescription2());
        fixedAssetCategory.setAssetType(requestDto.getAssetType());
        fixedAssetCategory.setFaGlAccount(String.valueOf(requestDto.getFaGlAccount()));
        fixedAssetCategory.setFaAccumulationAccount(String.valueOf(requestDto.getFaAccumulationAccount()));
        fixedAssetCategory.setFaDepreciationAccount(String.valueOf(requestDto.getFaDepreciationAccount()));
        fixedAssetCategory.setCostCenter(String.valueOf(requestDto.getCostCenter()));
        fixedAssetCategory.setUserRolePoid(ASGHelperUtils.convertListToString(requestDto.getUserRolePoid()));
        fixedAssetCategory.setActive(StringUtils.isBlank(requestDto.getActive()) ? "Y" : requestDto.getActive());
        fixedAssetCategory.setSeqNo(requestDto.getSeqNo());
        FixedAssetCategory updatedEntity = fixedAssetCategoryRepository.save(fixedAssetCategory);
        
        // Log the update
        loggingService.logChanges(oldEntity, updatedEntity, FixedAssetCategory.class, UserContext.getDocumentId(), faCategoryPoid.toString(), LogDetailsEnum.MODIFIED, "FA_CATEGORY_POID");
        
        return convertFromFixedAssetEntityToFixedAssetDto(updatedEntity);
    }

    public FixedAssetCategoryResponseDto getFixedAssetCategory(Long faCategoryPoid) {
        FixedAssetCategory entity = fixedAssetCategoryRepository.findById(faCategoryPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed Asset Category not found with ID: ", "faCategoryPoid", faCategoryPoid));
        return convertFromFixedAssetEntityToFixedAssetDto(entity);
    }

    @Override
    public void softDeleteFixedAssetCategory(Long faCategoryPoid, DeleteReasonDto deleteReasonDto) {
        FixedAssetCategory existing = fixedAssetCategoryRepository.findById(faCategoryPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed Asset Category not found with ID: ", "faCategoryPoid", faCategoryPoid));
        
        documentDeleteService.deleteDocument(
                faCategoryPoid,
                "FIXED_ASSET_CATEGORY_MASTER",
                "FA_CATEGORY_POID",
                deleteReasonDto,
                null
        );
    }

    private FixedAssetCategory convertFromFixedAssetDtoToFixedAssetEntity(FixedAssetCategoryRequestDto request) {
        FixedAssetCategory fixedAssetCategory = new FixedAssetCategory();
        fixedAssetCategory.setFaCategoryDescription(request.getFaCategoryDescription());
        fixedAssetCategory.setFaCategoryDescription2(request.getFaCategoryDescription2());
        fixedAssetCategory.setAssetType(request.getAssetType());
        fixedAssetCategory.setFaGlAccount(String.valueOf(request.getFaGlAccount()));
        fixedAssetCategory.setFaAccumulationAccount(String.valueOf(request.getFaAccumulationAccount()));
        fixedAssetCategory.setFaDepreciationAccount(String.valueOf(request.getFaDepreciationAccount()));
        fixedAssetCategory.setCostCenter(String.valueOf(request.getCostCenter()));
        fixedAssetCategory.setUserRolePoid(ASGHelperUtils.convertListToString(request.getUserRolePoid()));
        fixedAssetCategory.setActive(StringUtils.isBlank(request.getActive()) ? "Y" : request.getActive());
        fixedAssetCategory.setDeleted("N");
        fixedAssetCategory.setGroupPoid(UserContext.getGroupPoid());
        fixedAssetCategory.setSeqNo(request.getSeqNo());
        return fixedAssetCategory;
    }

    private FixedAssetCategoryResponseDto convertFromFixedAssetEntityToFixedAssetDto(FixedAssetCategory fixedAssetCategory) {
        FixedAssetCategoryResponseDto  fixedAssetCategoryResponseDto = new FixedAssetCategoryResponseDto();
        fixedAssetCategoryResponseDto.setFaCategoryPoid(fixedAssetCategory.getFaCategoryPoid());
        fixedAssetCategoryResponseDto.setFaCategoryCode(fixedAssetCategory.getFaCategoryCode());
        fixedAssetCategoryResponseDto.setFaCategoryDescription(fixedAssetCategory.getFaCategoryDescription());
        fixedAssetCategoryResponseDto.setFaCategoryDescription2(fixedAssetCategory.getFaCategoryDescription2());
        fixedAssetCategoryResponseDto.setAssetType(fixedAssetCategory.getAssetType());
        
        // Get Asset Type details using LOV service (code-based)
        if (fixedAssetCategory.getAssetType() != null && !fixedAssetCategory.getAssetType().isBlank()) {
            LovGetListDto assetTypeDetail = lovDataService.getDetailsByCodeAndLovName(fixedAssetCategory.getAssetType(), "ASSET_TYPE");
            if (assetTypeDetail != null && assetTypeDetail.getCode() != null) {
                DetailsDto assetTypeDetailsDto = new DetailsDto(
                    assetTypeDetail.getPoid(),
                    assetTypeDetail.getCode(),
                    assetTypeDetail.getDescription(),
                    assetTypeDetail.getValue(),
                    assetTypeDetail.getDescription(),
                    null
                );
                fixedAssetCategoryResponseDto.setAssetTypeDet(assetTypeDetailsDto);
            }
        }
        fixedAssetCategoryResponseDto.setFaGlAccount(Long.valueOf(fixedAssetCategory.getFaGlAccount()));
        fixedAssetCategoryResponseDto.setFaAccumulationAccount(Long.valueOf(fixedAssetCategory.getFaAccumulationAccount()));
        fixedAssetCategoryResponseDto.setFaDepreciationAccount(Long.valueOf(fixedAssetCategory.getFaDepreciationAccount()));
        
        Long costCenterPoid = fixedAssetCategory.getCostCenter() == null ? null : Long.valueOf(fixedAssetCategory.getCostCenter());
        if (costCenterPoid != null) {
        	fixedAssetCategoryResponseDto.setCostCenter(costCenterPoid);
            LovGetListDto costCenterDetail = lovDataService.getDetailsByPoidAndLovName(costCenterPoid, "GL_COST_CENTRE");
            if (costCenterDetail != null) {
                DetailsDto costCenterDetailsDto = new DetailsDto(
                    costCenterDetail.getPoid(),
                    costCenterDetail.getCode(),
                    costCenterDetail.getDescription(),
                    costCenterDetail.getValue(),
                    costCenterDetail.getDescription(),
                    null
                );
                fixedAssetCategoryResponseDto.setCostCenterDet(costCenterDetailsDto);
            }
        }
        
        // Use LOV service to get GL Master details
        Set<Long> glPoids = new HashSet<>(Arrays.asList(
                Long.valueOf(fixedAssetCategory.getFaGlAccount()),
                Long.valueOf(fixedAssetCategory.getFaAccumulationAccount()),
                Long.valueOf(fixedAssetCategory.getFaDepreciationAccount())
        ));
        
        Map<Long, LovGetListDto> glMasterDetailsMap = new HashMap<>();
        for (Long glPoid : glPoids) {
            glMasterDetailsMap.put(glPoid, lovDataService.getDetailsByPoidAndLovName(glPoid, "GL_MASTER_LEDGERS"));
        }
        
        // Convert LovGetListDto to DetailsDto for GL accounts
        LovGetListDto faGlDetail = glMasterDetailsMap.get(Long.valueOf(fixedAssetCategory.getFaGlAccount()));
        if (faGlDetail != null) {
            DetailsDto faGlDetailsDto = new DetailsDto(
                faGlDetail.getPoid(),
                faGlDetail.getCode(),
                faGlDetail.getDescription(),
                faGlDetail.getValue(),
                faGlDetail.getDescription(),
                null
            );
            fixedAssetCategoryResponseDto.setFaGlAccountDet(faGlDetailsDto);
        }
        
        LovGetListDto faAccDetail = glMasterDetailsMap.get(Long.valueOf(fixedAssetCategory.getFaAccumulationAccount()));
        if (faAccDetail != null) {
            DetailsDto faAccDetailsDto = new DetailsDto(
                faAccDetail.getPoid(),
                faAccDetail.getCode(),
                faAccDetail.getDescription(),
                faAccDetail.getValue(),
                faAccDetail.getDescription(),
                null
            );
            fixedAssetCategoryResponseDto.setFaAccumulationAccountDet(faAccDetailsDto);
        }
        
        LovGetListDto faDepDetail = glMasterDetailsMap.get(Long.valueOf(fixedAssetCategory.getFaDepreciationAccount()));
        if (faDepDetail != null) {
            DetailsDto faDepDetailsDto = new DetailsDto(
                faDepDetail.getPoid(),
                faDepDetail.getCode(),
                faDepDetail.getDescription(),
                faDepDetail.getValue(),
                faDepDetail.getDescription(),
                null
            );
            fixedAssetCategoryResponseDto.setFaDepreciationAccountDet(faDepDetailsDto);
        }

        // Use LOV service for user roles
        List<UserRoleDto> userRoleDtos = new ArrayList<>();
        if (fixedAssetCategory.getUserRolePoid() != null && !fixedAssetCategory.getUserRolePoid().isBlank()) {
            String[] userRoles = fixedAssetCategory.getUserRolePoid().split(";");
            for (String userRoleString : userRoles) {
                if (userRoleString != null && !userRoleString.isBlank()) {
                    Long userRolePoid = Long.valueOf(userRoleString.trim());
                    LovGetListDto roleDetail = lovDataService.getDetailsByPoidAndLovName(userRolePoid, "USER_ROLES");
                    if (roleDetail != null && roleDetail.getCode() != null) {
                        UserRoleDto roleDto = UserRoleDto.builder()
                                .userRoleId(roleDetail.getCode())
                                .userRolePoId(roleDetail.getPoid())
                                .userRoleName(roleDetail.getDescription())
                                .deleted("N")
                                .actionType(null)
                                .detRowId(null)
                                .build();
                        userRoleDtos.add(roleDto);
                    }
                }
            }
        }
        fixedAssetCategoryResponseDto.setUserRolePoid(ASGHelperUtils.convertFromStringToList(fixedAssetCategory.getUserRolePoid()));
        fixedAssetCategoryResponseDto.setUserRolesPoidDet(userRoleDtos);
        fixedAssetCategoryResponseDto.setActive(fixedAssetCategory.getActive());
        fixedAssetCategoryResponseDto.setSeqNo(fixedAssetCategory.getSeqNo());
        fixedAssetCategoryResponseDto.setCreatedBy(fixedAssetCategory.getCreatedBy());
        fixedAssetCategoryResponseDto.setCreatedDate(fixedAssetCategory.getCreatedDate());
        fixedAssetCategoryResponseDto.setLastModifiedBy(fixedAssetCategory.getLastModifiedBy());
        fixedAssetCategoryResponseDto.setLastModifiedDate(fixedAssetCategory.getLastModifiedDate());
        return fixedAssetCategoryResponseDto;
    }


    @Override
    public Map<String, Object> listFixedAssetCategories(String documentId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "FA_CATG_CODE",
                "FA_CATEGORY_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }


}
