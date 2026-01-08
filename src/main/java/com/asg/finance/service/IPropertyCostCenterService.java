package com.asg.finance.service;

import com.asg.finance.dto.PropertyCostCenterRequest;
import com.asg.finance.dto.PropertyCostCenterResponse;
import com.asg.finance.dto.PropertyCostCenterTreeNodeDto;
import com.asg.finance.dto.PropertyCostCenterTreeRequest;

import java.util.List;

public interface IPropertyCostCenterService {

    PropertyCostCenterResponse createPropertyCostCenter(PropertyCostCenterRequest request);

    PropertyCostCenterResponse getPropertyCostCenterById(Long costCenterPoid);

    PropertyCostCenterResponse updatePropertyCostCenter(Long costCenterPoid, PropertyCostCenterRequest request);

    void softDeleteByPoid(Long costCenterPoid, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto);
    
    /**
     * Get Property Cost Center tree view (hierarchical structure)
     */
    List<PropertyCostCenterTreeNodeDto> getPropertyCostCenterTree(String documentId, String actionRequested, PropertyCostCenterTreeRequest request);
    
    /**
     * Get Property Cost Center list view (flat structure with parent-child info)
     */
    List<PropertyCostCenterResponse> getPropertyCostCenterList(String documentId, String actionRequested, Long parentPoid);
}
