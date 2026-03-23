package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.entity.GLMaster;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.PettyCashUserRoleRequestDto;
import com.asg.finance.dto.PettyCashUserroleResponseDto;
import com.asg.finance.entity.PettyCashUserroleMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.PettyCashUserroleMasterRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.PettyCashUserRoleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PettyCashUserRoleServiceImpl implements PettyCashUserRoleService {

    private final PettyCashUserroleMasterRepository repository;
    private final RoleServiceClient roleServiceClient;
    private final GLMasterRepository glMasterRepository;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    public PettyCashUserroleResponseDto createPettyCashUserRole(PettyCashUserRoleRequestDto request) {
        PettyCashUserroleMaster entity = covertFromGlPettyDtoToGlPettyEntity(request);
        PettyCashUserroleMaster pettyCashUserroleMaster = repository.save(entity);

        // Log the creation
        String key = pettyCashUserroleMaster.getRefTypePoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);

        return covertFromGlPettyEntityToGlPettyDto(pettyCashUserroleMaster);
    }

    private PettyCashUserroleMaster covertFromGlPettyDtoToGlPettyEntity(PettyCashUserRoleRequestDto dto) {
        PettyCashUserroleMaster entity = new PettyCashUserroleMaster();
        entity.setRefTypePoid(dto.getRefTypePoid());
        entity.setRefType(dto.getRefType());
        entity.setDescription(dto.getDescription());
        entity.setUserRolePoid(ASGHelperUtils.convertListToString(dto.getUserRolePoid()));
        entity.setGlPoid(ASGHelperUtils.convertListToString(dto.getGlPoid()));
        entity.setValidUntil(LocalDate.now());
        entity.setActive(StringUtils.isBlank(dto.getActive()) ? "Y" : dto.getActive());
        entity.setDeleted("N");
        entity.setSeqNo(dto.getSeqno());
      /*  entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        entity.setCreatedBy(getCurrentUser());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());*/
        return entity;
    }

    private PettyCashUserroleResponseDto covertFromGlPettyEntityToGlPettyDto(PettyCashUserroleMaster entity){
        PettyCashUserroleResponseDto requestDto = new PettyCashUserroleResponseDto();
        requestDto.setRefTypePoid(entity.getRefTypePoid());
        requestDto.setRefType(entity.getRefType());
        requestDto.setDescription(entity.getDescription());
        List<UserRoleDto> userRoleDtos = new ArrayList<>();
        if (entity.getUserRolePoid() != null && !entity.getUserRolePoid().isBlank()) {
            String[] userRoles = entity.getUserRolePoid().split(";");
            for (String userRoleString : userRoles) {
                if (userRoleString != null && !userRoleString.isBlank()) {
                    RoleDto roleDtoResponse = roleServiceClient.findById(Long.valueOf(userRoleString.trim()));
                    if (roleDtoResponse != null) {
                        UserRoleDto roleDto = UserRoleDto.builder()
                                .userRolePoId(roleDtoResponse.getUserRolePoid())
                                .userRoleId(roleDtoResponse.getUserRoleId())
                                .active(roleDtoResponse.getActive())
                                .userRoleName(roleDtoResponse.getUserRoleName())
                                .build();
                        userRoleDtos.add(roleDto);
                    }
                }
            }
        }

        List<DetailsDto> glPoidDet = new ArrayList<>();
        if(entity.getGlPoid() != null && !entity.getGlPoid().isBlank()) {
            String[] glPoid = entity.getGlPoid().split(";");
            for (String glPoidString : glPoid) {
                if (glPoidString != null && !glPoidString.isBlank()) {
                    GLMaster glMaster = glMasterRepository.findByGlPoid(Long.valueOf(glPoidString.trim())).orElseThrow(
                            () -> new ResourceNotFoundException("GL Master not found with ID: ", "glPoid", glPoidString));
                    if (glMaster != null) {
                        DetailsDto detailsDto = new DetailsDto(
                                glMaster.getGlPoid(),
                                glMaster.getGlCode(),
                                glMaster.getGlDescription(),
                                glMaster.getGlPoid(),
                                glMaster.getGlDescription(),
                                glMaster.getSeqno());
                        glPoidDet.add(detailsDto);
                    }
                }
            }
        }

        requestDto.setUserRolePoid(ASGHelperUtils.convertFromStringToList(entity.getUserRolePoid()));
        requestDto.setUserRolesPoidDet(userRoleDtos);
        requestDto.setGlPoid(ASGHelperUtils.convertFromStringToList(entity.getGlPoid()));
        requestDto.setPettyCashGlPoidDet(glPoidDet);
        requestDto.setActive(entity.getActive());
        requestDto.setSeqno(entity.getSeqNo());
        requestDto.setCreatedBy(entity.getCreatedBy());
        requestDto.setCreatedDate(entity.getCreatedDate());
        requestDto.setLastModifiedBy(entity.getLastModifiedBy());
        requestDto.setLastModifiedDate(entity.getLastModifiedDate());
        return requestDto;
    }

    public PettyCashUserroleResponseDto updatePettyCashUserRole(Long refTypePoid, PettyCashUserRoleRequestDto requestDto) {
        PettyCashUserroleMaster existingEntity = repository.findById(refTypePoid)
                .orElseThrow(() -> new ResourceNotFoundException("Petty cash user role not found with ID: ", "refTypePoid",refTypePoid));

        // Create a copy of the existing entity for logging
        PettyCashUserroleMaster oldEntity = new PettyCashUserroleMaster();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        existingEntity.setRefType(requestDto.getRefType());
        existingEntity.setDescription(requestDto.getDescription());
        existingEntity.setUserRolePoid(ASGHelperUtils.convertListToString(requestDto.getUserRolePoid()));
        existingEntity.setGlPoid(ASGHelperUtils.convertListToString(requestDto.getGlPoid()));
        existingEntity.setValidUntil(LocalDate.now());
        existingEntity.setActive(StringUtils.isBlank(requestDto.getActive()) ? "Y" : requestDto.getActive());
        existingEntity.setSeqNo(requestDto.getSeqno());
        PettyCashUserroleMaster updatedEntity = repository.save(existingEntity);
        
        // Log the update
        String key = updatedEntity.getRefTypePoid().toString();
        loggingService.logChanges(oldEntity, updatedEntity, PettyCashUserroleMaster.class, 
                UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, "REF_TYPE_POID");
        
        return covertFromGlPettyEntityToGlPettyDto(updatedEntity);
    }

    public PettyCashUserroleResponseDto getPettyCashUserRole(Long refTypePoid) {
        PettyCashUserroleMaster entity = repository.findById(refTypePoid)
                .orElseThrow(() -> new ResourceNotFoundException("Petty cash user role not found with ID: ", "refTypePoid", refTypePoid));
        return covertFromGlPettyEntityToGlPettyDto(entity);
    }

    @Transactional
    public void softDeletePettyCashUserRole(Long refTypePoid, DeleteReasonDto deleteReasonDto) {
        PettyCashUserroleMaster existingEntity = repository.findById(refTypePoid)
                .orElseThrow(() -> new ResourceNotFoundException("Petty cash user role not found with ID: ", "refTypePoid", refTypePoid));
        
        documentDeleteService.deleteDocument(
                refTypePoid,
                "GL_PETTY_CASH_USERROLE_MASTER",
                "REF_TYPE_POID",
                deleteReasonDto,
                null
        );
    }

    @Override
    public Map<String, Object> listPettyCashUserRole(String documentId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "REF_TYPE",   // label
                "REF_TYPE_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }


}
