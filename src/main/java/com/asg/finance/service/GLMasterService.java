package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.GLMasterDto;
import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import org.springframework.data.domain.Pageable;

import com.asg.finance.dto.GLMasterRequestDto;
import com.asg.finance.dto.GLMasterResponseDto;
import com.asg.finance.dto.GlMasterTreeNodeDto;
import com.asg.finance.dto.GlMasterTreeRequest;


import java.util.List;
import java.util.Map;

public interface GLMasterService {
    GLMasterResponseDto createGLMaster(GLMasterRequestDto req);

    GLMasterResponseDto getGLMaster(Long glPoid);

    GLMasterDto getGLMasterDto(Long glPoid);

    List<GLMasterDto> getGLMasterDtos(List<Long> glPoids);

    GLMasterResponseDto updateGLMaster(Long glPoid, GLMasterRequestDto req);

    void deleteGLMaster(Long glPoid, DeleteReasonDto deleteReasonDto);

    // Tree functionality
    List<GlMasterTreeNodeDto> getGlMasterTree(String documentId, String actionRequested, GlMasterTreeRequest request);

    // List functionality
    List<GLMasterResponseDto> getGlMasterList(String documentId, String actionRequested, Long parentPoid);

    // maybe list / paging etc

    String releaseLock(DocReleaseLockRequestDto request);

    String acquireLock(DocReleaseLockRequestDto request);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable);
}

