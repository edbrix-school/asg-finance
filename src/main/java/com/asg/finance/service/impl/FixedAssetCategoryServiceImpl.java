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

import java.time.LocalDateTime;
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
    private final RoleServiceClient roleServiceClient;
    private final GLMasterRepository glMasterRepository;
    private final CostCenterRepository costCenterRepository;
    private final LoggingService loggingService;

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
        fixedAssetCategory.setLastModifiedBy(getCurrentUser());
        fixedAssetCategory.setLastModifiedDate(LocalDateTime.now());
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

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
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
        fixedAssetCategory.setCreatedBy(getCurrentUser());
        fixedAssetCategory.setCreatedDate(LocalDateTime.now());
        fixedAssetCategory.setLastModifiedBy(getCurrentUser());
        fixedAssetCategory.setLastModifiedDate(LocalDateTime.now());
        return fixedAssetCategory;
    }

    private FixedAssetCategoryResponseDto convertFromFixedAssetEntityToFixedAssetDto(FixedAssetCategory fixedAssetCategory) {
        FixedAssetCategoryResponseDto  fixedAssetCategoryResponseDto = new FixedAssetCategoryResponseDto();
        fixedAssetCategoryResponseDto.setFaCategoryPoid(fixedAssetCategory.getFaCategoryPoid());
        fixedAssetCategoryResponseDto.setFaCategoryCode(fixedAssetCategory.getFaCategoryCode());
        fixedAssetCategoryResponseDto.setFaCategoryDescription(fixedAssetCategory.getFaCategoryDescription());
        fixedAssetCategoryResponseDto.setFaCategoryDescription2(fixedAssetCategory.getFaCategoryDescription2());
        fixedAssetCategoryResponseDto.setAssetType(fixedAssetCategory.getAssetType());
        fixedAssetCategoryResponseDto.setFaGlAccount(Long.valueOf(fixedAssetCategory.getFaGlAccount()));
        fixedAssetCategoryResponseDto.setFaAccumulationAccount(Long.valueOf(fixedAssetCategory.getFaAccumulationAccount()));
        fixedAssetCategoryResponseDto.setFaDepreciationAccount(Long.valueOf(fixedAssetCategory.getFaDepreciationAccount()));
        
        Long costCenterPoid = fixedAssetCategory.getCostCenter() == null ? null : Long.valueOf(fixedAssetCategory.getCostCenter());
        if (costCenterPoid != null) {
        	fixedAssetCategoryResponseDto.setCostCenter(costCenterPoid);

            costCenterRepository.findById(costCenterPoid)
                    .ifPresent(cc -> fixedAssetCategoryResponseDto.setCostCenterDet(
                    		new DetailsDto(
                    				cc.getCostCenterPoid(), 
                    				cc.getCostCenterCode(),
                	                cc.getCostCenterDescription(), 
                	                cc.getCostCenterPoid(), 
                	                cc.getCostCenterDescription(), 
                	                cc.getSeqNo()
                	                )));
        }
        
        Set<Long> glPoids = new HashSet<>(Arrays.asList(
                Long.valueOf(fixedAssetCategory.getFaGlAccount()),
                Long.valueOf(fixedAssetCategory.getFaAccumulationAccount()),
                Long.valueOf(fixedAssetCategory.getFaDepreciationAccount())
        ));

        List<GLMaster> glMasters = glMasterRepository.findByGlPoidIn(glPoids);

        Map<Long, DetailsDto> glMasterDetailsMap = glMasters.stream()
                .collect(Collectors.toMap(
                        GLMaster::getGlPoid,
                        glMaster -> new DetailsDto(
                                glMaster.getGlPoid(),
                                glMaster.getGlCode(),
                                glMaster.getGlDescription(),
                                glMaster.getGlPoid(),
                                glMaster.getGlDescription(),
                                glMaster.getSeqno()
                        )
                ));
        fixedAssetCategoryResponseDto.setFaGlAccountDet(glMasterDetailsMap.get(Long.valueOf(fixedAssetCategory.getFaGlAccount())));
        fixedAssetCategoryResponseDto.setFaAccumulationAccountDet(glMasterDetailsMap.get(Long.valueOf(fixedAssetCategory.getFaAccumulationAccount())));
        fixedAssetCategoryResponseDto.setFaDepreciationAccountDet(glMasterDetailsMap.get(Long.valueOf(fixedAssetCategory.getFaDepreciationAccount())));

        List<UserRoleDto> userRoleDtos = new ArrayList<>();
        if (fixedAssetCategory.getUserRolePoid() != null && !fixedAssetCategory.getUserRolePoid().isBlank()) {
            String[] userRoles = fixedAssetCategory.getUserRolePoid().split(";");
            for (String userRoleString : userRoles) {
                if (userRoleString != null && !userRoleString.isBlank()) {

                    RoleDto roleDtoResponse = roleServiceClient.findById(Long.valueOf(userRoleString.trim()));
                    if (roleDtoResponse != null) {
                        UserRoleDto roleDto = UserRoleDto.builder()
                                .userRoleId(roleDtoResponse.getUserRoleId())
                                .userRolePoId(roleDtoResponse.getUserRolePoid())
                                .userRoleName(roleDtoResponse.getUserRoleName())
                                .active(roleDtoResponse.getActive())
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
