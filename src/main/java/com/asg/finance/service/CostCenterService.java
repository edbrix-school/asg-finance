package com.asg.finance.service;

import com.asg.finance.dto.CostCenterListResponseDto;
import com.asg.finance.dto.CostCenterRequestDTO;
import com.asg.finance.dto.CostCenterTreeResponseDto;
import com.asg.finance.dto.CostCenterTreeRequest;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CostCenterService {
    Long createCostCenter(CostCenterRequestDTO dto);

    void softDeleteCountry(Long costCenterPoid);

    Long updateCostCenter(Long id, CostCenterRequestDTO dto);

    CostCenterRequestDTO getCostCenterById(Long costCenterPoid);

    Map<String, Object> listCostCenter(String documentId, FilterRequestDto filters, Pageable pageable);

    // Tree functionality
    List<CostCenterTreeResponseDto> getCostCenterTree(String documentId, String actionRequested, CostCenterTreeRequest request);

    // List functionality
    List<CostCenterListResponseDto> getCostCenterList(String documentId, String actionRequested, Long parentPoid);

}
