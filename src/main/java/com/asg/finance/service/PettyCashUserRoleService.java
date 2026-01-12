package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.PettyCashUserRoleRequestDto;
import com.asg.finance.dto.PettyCashUserroleResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface PettyCashUserRoleService {
    PettyCashUserroleResponseDto createPettyCashUserRole(PettyCashUserRoleRequestDto request);
    PettyCashUserroleResponseDto updatePettyCashUserRole(Long refTypePoid, PettyCashUserRoleRequestDto requestDto);
    PettyCashUserroleResponseDto getPettyCashUserRole(Long refTypePoid);
    void softDeletePettyCashUserRole(Long refTypePoid, DeleteReasonDto deleteReasonDto);
    Map<String, Object> listPettyCashUserRole(String documentId, FilterRequestDto filters, Pageable pageable);
}
