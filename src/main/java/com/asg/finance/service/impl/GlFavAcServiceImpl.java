package com.asg.finance.service.impl;


import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.dto.GlFavAcDto;
import com.asg.finance.dto.GlFavAcMasterGlAcDtlDto;
import com.asg.finance.dto.GlFavAcMasterUserRoleDtlDto;
import com.asg.finance.entity.GlFavAcEntity;
import com.asg.finance.entity.GlFavAcMasterGlAcDtlEntity;
import com.asg.finance.entity.GlFavAcMasterUserRoleDtlEntity;
import com.asg.finance.repository.GlFavAcMasterGlAcDtlRepository;
import com.asg.finance.repository.GlFavAcMasterUserRoleDtlRepository;
import com.asg.finance.repository.GlFavAcRepository;
import com.asg.finance.service.GlFavAcService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlFavAcServiceImpl implements GlFavAcService {


    private final GlFavAcRepository glFavAcRepository;

    private final GlFavAcMasterGlAcDtlRepository glFavAcMasterGlAcDtlRepository;

    private final GlFavAcMasterUserRoleDtlRepository glFavAcMasterUserRoleDtlRepository;

    private GlFavAcMasterUserRoleDtlDto convertToGlFavAcMasterUserRoleDtlDto(GlFavAcMasterUserRoleDtlEntity glFavAcMasterUserRoleDtlEntity){
        GlFavAcMasterUserRoleDtlDto glFavAcMasterUserRoleDtlDto = new GlFavAcMasterUserRoleDtlDto();
        glFavAcMasterUserRoleDtlDto.setDetRowId(glFavAcMasterUserRoleDtlEntity.getDetRowId());
        glFavAcMasterUserRoleDtlDto.setFavAcPoid(glFavAcMasterUserRoleDtlEntity.getFavAcPoid());
        glFavAcMasterUserRoleDtlDto.setUserRolePoid(glFavAcMasterUserRoleDtlEntity.getUserRolePoid());
        glFavAcMasterUserRoleDtlDto.setRemarks(glFavAcMasterUserRoleDtlEntity.getRemarks());
        glFavAcMasterUserRoleDtlDto.setCreatedBy(glFavAcMasterUserRoleDtlEntity.getCreatedBy());
        glFavAcMasterUserRoleDtlDto.setCreatedDate(glFavAcMasterUserRoleDtlEntity.getCreatedDate());
        glFavAcMasterUserRoleDtlDto.setLastModifiedDate(glFavAcMasterUserRoleDtlEntity.getLastModifiedDate());
        return glFavAcMasterUserRoleDtlDto;
    }

    private GlFavAcMasterGlAcDtlDto convertToGlFavAcMasterGlAcDtlDto(GlFavAcMasterGlAcDtlEntity glFavAcMasterGlAcDtlEntity){
        GlFavAcMasterGlAcDtlDto glFavAcMasterGlAcDtlDto = new GlFavAcMasterGlAcDtlDto();
        glFavAcMasterGlAcDtlDto.setDetRowId(glFavAcMasterGlAcDtlEntity.getDetRowId());
        glFavAcMasterGlAcDtlDto.setFavAcPoid(glFavAcMasterGlAcDtlEntity.getFavAcPoid());
        glFavAcMasterGlAcDtlDto.setGlPoid(glFavAcMasterGlAcDtlEntity.getGlPoid());
        glFavAcMasterGlAcDtlDto.setRemarks(glFavAcMasterGlAcDtlEntity.getRemarks());
        glFavAcMasterGlAcDtlDto.setCreatedBy(glFavAcMasterGlAcDtlEntity.getCreatedBy());
        glFavAcMasterGlAcDtlDto.setCreatedDate(glFavAcMasterGlAcDtlEntity.getCreatedDate());
        glFavAcMasterGlAcDtlDto.setLastModifiedDate(glFavAcMasterGlAcDtlEntity.getLastModifiedDate());
        glFavAcMasterGlAcDtlDto.setSeqNo(glFavAcMasterGlAcDtlEntity.getSeqNo());
        glFavAcMasterGlAcDtlDto.setCompany(glFavAcMasterGlAcDtlEntity.getCompany());
        glFavAcMasterGlAcDtlDto.setViewCategory(glFavAcMasterGlAcDtlEntity.getViewCategory());
        return glFavAcMasterGlAcDtlDto;
    }

}
