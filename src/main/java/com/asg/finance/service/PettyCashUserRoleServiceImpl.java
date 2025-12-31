package com.asg.finance.service;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.entity.GLMaster;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.PettyCashUserRoleRequestDto;
import com.asg.finance.dto.PettyCashUserroleResponseDto;
import com.asg.finance.entity.PettyCashUserroleMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.PettyCashUserroleMasterRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
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

    public PettyCashUserroleResponseDto createPettyCashUserRole(PettyCashUserRoleRequestDto request) {
        PettyCashUserroleMaster entity = covertFromGlPettyDtoToGlPettyEntity(request);
        PettyCashUserroleMaster pettyCashUserroleMaster = repository.save(entity);
        return covertFromGlPettyEntityToGlPettyDto(pettyCashUserroleMaster);
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
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
        entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        entity.setCreatedBy(getCurrentUser());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
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
        requestDto.setLastModifiedDate(entity.getLastModifiedDate() != null ? java.sql.Timestamp.valueOf(entity.getLastModifiedDate()) : null);
        return requestDto;
    }

    public PettyCashUserroleResponseDto updatePettyCashUserRole(Long refTypePoid, PettyCashUserRoleRequestDto requestDto) {
        PettyCashUserroleMaster existingEntity = repository.findById(refTypePoid)
                .orElseThrow(() -> new ResourceNotFoundException("Petty cash user role not found with ID: ", "refTypePoid",refTypePoid));

        existingEntity.setRefType(requestDto.getRefType());
        existingEntity.setDescription(requestDto.getDescription());
        existingEntity.setUserRolePoid(ASGHelperUtils.convertListToString(requestDto.getUserRolePoid()));
        existingEntity.setGlPoid(ASGHelperUtils.convertListToString(requestDto.getGlPoid()));
        existingEntity.setValidUntil(LocalDate.now());
        existingEntity.setActive(StringUtils.isBlank(requestDto.getActive()) ? "Y" : requestDto.getActive());
        existingEntity.setSeqNo(requestDto.getSeqno());
        existingEntity.setLastModifiedBy(getCurrentUser());
        existingEntity.setLastModifiedDate(LocalDateTime.now());
        PettyCashUserroleMaster updatedEntity = repository.save(existingEntity);
        return covertFromGlPettyEntityToGlPettyDto(updatedEntity);
    }

    public PettyCashUserroleResponseDto getPettyCashUserRole(Long refTypePoid) {
        PettyCashUserroleMaster entity = repository.findById(refTypePoid)
                .orElseThrow(() -> new ResourceNotFoundException("Petty cash user role not found with ID: ", "refTypePoid", refTypePoid));
        return covertFromGlPettyEntityToGlPettyDto(entity);
    }

    @Transactional
    public void softDeletePettyCashUserRole(Long refTypePoid) {
        PettyCashUserroleMaster existingEntity = repository.findById(refTypePoid)
                .orElseThrow(() -> new ResourceNotFoundException("Petty cash user role not found with ID: ", "refTypePoid",refTypePoid));
        existingEntity.setDeleted("Y");
        existingEntity.setActive("N");
        existingEntity.setLastModifiedDate(LocalDateTime.now());
        existingEntity.setLastModifiedBy(getCurrentUser());
        repository.save(existingEntity);
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
