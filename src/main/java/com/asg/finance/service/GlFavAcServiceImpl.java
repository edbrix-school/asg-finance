package com.asg.finance.service;


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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlFavAcServiceImpl implements GlFavAcService {


    private final GlFavAcRepository glFavAcRepository;

    private final GlFavAcMasterGlAcDtlRepository glFavAcMasterGlAcDtlRepository;

    private final GlFavAcMasterUserRoleDtlRepository glFavAcMasterUserRoleDtlRepository;

    @Override
    public GlFavAcDto fetchGlFavAcDetails(Long favAcPoid) {
        GlFavAcEntity glFavAcEntity = glFavAcRepository.findByFavAcPoid(favAcPoid);
        if(glFavAcEntity == null){
            throw new ResourceNotFoundException("Gl Fav Ac Master","favAcPoid",favAcPoid);
        }
        GlFavAcDto glFavAcDto = new GlFavAcDto();
        glFavAcDto.setFavAcPoid(glFavAcEntity.getFavAcPoid());
        glFavAcDto.setGroupPoid(glFavAcEntity.getGroupPoid());
        glFavAcDto.setFavAcCode(glFavAcEntity.getFavAcCode());
        glFavAcDto.setDescription(glFavAcEntity.getDescription());
        glFavAcDto.setDescription2(glFavAcEntity.getDescription2());
        glFavAcDto.setActive(glFavAcEntity.getActive());
        glFavAcDto.setCreatedBy(glFavAcEntity.getCreatedBy());
        glFavAcDto.setSeqNo(glFavAcEntity.getSeqNo());
        glFavAcDto.setCreatedDate(glFavAcEntity.getCreatedDate());
        glFavAcDto.setLastModifiedDate(glFavAcEntity.getLastModifiedDate());
        glFavAcDto.setDeleted(glFavAcEntity.getDeleted());
//        glFavAcDto.setGlFavAcMasterGlAcDtlDtoList(glFavAcMasterGlAcDtlRepository.findByFavAcPoid(favAcPoid).stream().map(this::convertToGlFavAcMasterGlAcDtlDto).collect(Collectors.toList()));
//        glFavAcDto.setGlFavAcMasterUserRoleDtlDtoList(glFavAcMasterUserRoleDtlRepository.findByFavAcPoid(favAcPoid).stream().map(this::convertToGlFavAcMasterUserRoleDtlDto).collect(Collectors.toList()));
        return glFavAcDto;
    }

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
