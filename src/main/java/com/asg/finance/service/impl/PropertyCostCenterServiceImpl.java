package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.PropertyCostCenterRequest;
import com.asg.finance.dto.PropertyCostCenterResponse;
import com.asg.finance.dto.PropertyCostCenterTreeNodeDto;
import com.asg.finance.dto.PropertyCostCenterTreeRequest;
import com.asg.finance.entity.CostCenter;
import com.asg.finance.entity.PropertyCostCenter;
import com.asg.finance.repository.CostCenterRepository;
import com.asg.finance.repository.PropertyCostCenterRepository;
import com.asg.finance.repository.PropertyCostCenterTreeViewRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.IPropertyCostCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@Slf4j
public class PropertyCostCenterServiceImpl implements IPropertyCostCenterService {

    @Autowired
    private PropertyCostCenterRepository repository;
    
    @Autowired
    private PropertyCostCenterTreeViewRepository treeViewRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private LovDataService lovService;

    private void validatePropertyType(PropertyCostCenterRequest request) {
        if (request.getPropertyType() != null && !request.getPropertyType().matches("MAIN_GROUP|SUB_GROUP|CHILD")) {
            throw new ValidationException("Invalid Property Type. Allowed: MAIN_GROUP, SUB_GROUP, CHILD");
        }
        if (("SUB_GROUP".equals(request.getPropertyType()) || "CHILD".equals(request.getPropertyType()))) {
            if (request.getParentPropertyPoid() != null) {
                repository.findByPropertyCostCenterPoidAndDeleted(request.getParentPropertyPoid(), "N")
                        .orElseThrow(() -> new ResourceNotFoundException("Sub of(Parent Property Cost Center)", "POID", request.getParentPropertyPoid()));
            } else {
                throw new ValidationException("Sub of is mandatory for Property Type: SUB_GROUP or CHILD");
            }
        }
    }

    @Transactional
    public PropertyCostCenterResponse createPropertyCostCenter(PropertyCostCenterRequest request) {
        repository.findByPropertyCostCenterCode(request.getPropertyCostCenterCode()).ifPresent(p -> {
            throw new ValidationException("Property Cost Center Code must be unique");
        });
        
        repository.findByPropertyCostCenterNameAndDeleted(request.getPropertyCostCenterName(), "N").ifPresent(p -> {
            throw new ValidationException("Property Cost Center Name must be unique");
        });

        validatePropertyType(request);

        PropertyCostCenter costCenter = new PropertyCostCenter();
        costCenter.setPropertyCostCenterName(request.getPropertyCostCenterName());
        costCenter.setPropertyType(request.getPropertyType());
        costCenter.setPropertyDescription(request.getPropertyDescription());
        costCenter.setCostCenterPoid(request.getCostCenterPoid());
        costCenter.setCompanyPoid(request.getCompanyPoid());
        costCenter.setParentPropertyPoid(request.getParentPropertyPoid());
        costCenter.setRemarks(request.getRemarks());
        costCenter.setSeqNo(request.getSeqNo());
        costCenter.setActive(request.getActive());
        costCenter.setDeleted("N");
        costCenter.setCreatedBy(getCurrentUser());
        costCenter.setCreatedDate(LocalDateTime.now());
        costCenter.setLastModifiedBy(getCurrentUser());
        costCenter.setLastModifiedDate(LocalDateTime.now());

        PropertyCostCenter saved = repository.save(costCenter);
        return convertEntityToResponseDTO(saved);
    }

    private PropertyCostCenterResponse convertEntityToResponseDTO(PropertyCostCenter saved) {
        PropertyCostCenterResponse response = new PropertyCostCenterResponse();
        response.setPoid(saved.getPropertyCostCenterPoid());
        response.setPropertyCostCenterCode(saved.getPropertyCostCenterCode());
        response.setPropertyCostCenterName(saved.getPropertyCostCenterName());
        response.setPropertyDescription(saved.getPropertyDescription());
        response.setPropertyType(saved.getPropertyType());
        response.setActive(saved.getActive());
        response.setParentPropertyPoid(saved.getParentPropertyPoid());
        if(response.getParentPropertyPoid() != null) {
            Optional<PropertyCostCenter> parentPropertyCostCenterOp = repository.findByPropertyCostCenterPoid(response.getParentPropertyPoid());
            if (parentPropertyCostCenterOp.isPresent()) {
                PropertyCostCenter parentPropertyCostCenter = parentPropertyCostCenterOp.get();
                DetailsDto detailsDto = new DetailsDto(parentPropertyCostCenter.getPropertyCostCenterPoid(), parentPropertyCostCenter.getPropertyCostCenterCode(),
                        parentPropertyCostCenter.getPropertyCostCenterName(), parentPropertyCostCenter.getPropertyCostCenterPoid(), parentPropertyCostCenter.getPropertyCostCenterName(), parentPropertyCostCenter.getSeqNo());
                response.setParentPropertyDet(detailsDto);
            }
        };

        response.setCostCenterPoid(saved.getCostCenterPoid());
        if (saved.getCostCenterPoid() != null) {
            CostCenter costCenter = costCenterRepository.findByCostCenterPoid(saved.getCostCenterPoid());
            DetailsDto detailsDto = new DetailsDto(costCenter.getCostCenterPoid(), costCenter.getCostCenterCode(),
                    costCenter.getCostCenterDescription(), costCenter.getCostCenterPoid(), costCenter.getCostCenterDescription(), costCenter.getSeqNo());
            response.setCostCenterDet(detailsDto);
        }

        response.setCompanyPoid(saved.getCompanyPoid());
        if (response.getCompanyPoid() != null) {
            response.setCompanyDet(lovService.getDetailsByPoidAndLovName(saved.getCompanyPoid(), "COMPANY"));
        };

        response.setRemarks(saved.getRemarks());
        response.setDeleted(saved.getDeleted());
        response.setSeqNo(saved.getSeqNo());

        response.setCreatedBy(saved.getCreatedBy());
        response.setLastModifiedBy(saved.getLastModifiedBy());
        if (saved.getCreatedDate() != null) {
            response.setCreatedDate(saved.getCreatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (saved.getLastModifiedDate() != null) {
            response.setLastModifiedDate(saved.getLastModifiedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PropertyCostCenterResponse getPropertyCostCenterById(Long costCenterPoid) {
        PropertyCostCenter costCenter = repository.findByPropertyCostCenterPoid(costCenterPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Property Cost Center", "POID", costCenterPoid));
        return convertEntityToResponseDTO(costCenter);
    }

    @Transactional
    public PropertyCostCenterResponse updatePropertyCostCenter(Long costCenterPoid, PropertyCostCenterRequest request) {
        PropertyCostCenter entity = repository.findByPropertyCostCenterPoidAndDeleted(costCenterPoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Property Cost Center", "POID", costCenterPoid));

        validatePropertyType(request);

        if (request.getPropertyCostCenterName() != null) {
            repository.findByPropertyCostCenterNameAndDeleted(request.getPropertyCostCenterName(), "N").ifPresent(existing -> {
                if (!existing.getPropertyCostCenterPoid().equals(costCenterPoid)) {
                    throw new ValidationException("Property Cost Center Name must be unique");
                }
            });
            entity.setPropertyCostCenterName(request.getPropertyCostCenterName());
        }
        if (request.getPropertyType() != null) {
            entity.setPropertyType(request.getPropertyType());
        }
        entity.setPropertyDescription(request.getPropertyDescription());
        entity.setCostCenterPoid(request.getCostCenterPoid());
        entity.setCompanyPoid(request.getCompanyPoid());
        entity.setParentPropertyPoid(request.getParentPropertyPoid());
        entity.setSeqNo(request.getSeqNo());
        entity.setRemarks(request.getRemarks());
        entity.setActive(request.getActive());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        entity = repository.save(entity);

        return convertEntityToResponseDTO(entity);
    }

    @Override
    @Transactional
    public void softDeleteByPoid(Long costCenterPoid) {
        // Fetch the Property Cost Center by its POID
        PropertyCostCenter entity = repository.findByPropertyCostCenterPoidAndDeleted(costCenterPoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Property Cost Center", "POID", costCenterPoid));

        // Check if this property cost center has active children
        if (hasActiveChildren(costCenterPoid)) {
            throw new ValidationException("This Property Cost Center cannot be deleted as it has related child records.");
        }

        // Mark the record as deleted
        entity.setDeleted("Y");
        entity.setLastModifiedBy(getCurrentUser());  // Assuming a helper method for fetching current user
        entity.setLastModifiedDate(LocalDateTime.now());  // Set the current date/time as last modified date

        // Save the soft-deleted entity
        repository.save(entity);
    }

    private boolean hasActiveChildren(Long parentPoid) {
        return repository.existsByParentPropertyPoidAndDeleted(parentPoid, "N");
    }

    // Tree functionality implementation
    @Override
    public List<PropertyCostCenterTreeNodeDto> getPropertyCostCenterTree(String documentId, String actionRequested, PropertyCostCenterTreeRequest request) {
        try {
            log.info("Fetching Property Cost Center tree for documentId: {}, actionRequested: {}", documentId, actionRequested);

            // Call the stored procedure via repository
            List<Map<String, Object>> treeViewData = treeViewRepository.callPropertyCostCenterTreeViewProcedure(documentId, actionRequested, request);

            if (treeViewData.isEmpty()) {
                log.info("No Property Cost Center tree data found");
                return new ArrayList<>();
            }

            // Build tree structure
            List<PropertyCostCenterTreeNodeDto> treeNodes = buildTreeStructure(treeViewData);

            log.info("Successfully built Property Cost Center tree with {} root nodes", treeNodes.size());
            return treeNodes;

        } catch (Exception e) {
            log.error("Error fetching Property Cost Center tree", e);
            throw new RuntimeException("Error fetching Property Cost Center tree: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build tree structure from flat procedure results
     */
    private List<PropertyCostCenterTreeNodeDto> buildTreeStructure(List<Map<String, Object>> procedureResults) {
        // Convert all records to tree items
        Map<Long, PropertyCostCenterTreeNodeDto> itemMap = new HashMap<>();
        List<PropertyCostCenterTreeNodeDto> rootItems = new ArrayList<>();
        
        for (Map<String, Object> row : procedureResults) {
            PropertyCostCenterTreeNodeDto item = convertToTreeItem(row);
            itemMap.put(item.getPoid(), item);
        }
        
        // Build parent-child relationships
        for (PropertyCostCenterTreeNodeDto item : itemMap.values()) {
            if (item.getParentPropertyPoid() == null) {
                rootItems.add(item);
            } else {
                PropertyCostCenterTreeNodeDto parent = itemMap.get(item.getParentPropertyPoid());
                if (parent != null) {
                    parent.getChildren().add(item);
                }
            }
        }
        
        // Sort root items and their children recursively
        sortTreeNodes(rootItems);
        
        return rootItems;
    }
    
    /**
     * Convert procedure result row to tree item
     */
    private PropertyCostCenterTreeNodeDto convertToTreeItem(Map<String, Object> row) {
        PropertyCostCenterTreeNodeDto item = new PropertyCostCenterTreeNodeDto();
        
        // Get propertyCostCenterCode from procedure result (returns as 'CODE' column)
        String propertyCode = null;
        Object codeValue = row.get("CODE");
        if (codeValue != null) {
            propertyCode = codeValue.toString();
        }
        
        // Get property name from description
        String description = (String) row.get("DESCRIPTION");
        String propertyName = description;
        
        // Safe extraction of numeric fields with defaults
        Integer level = getIntegerValue(row.get("LVL"));
        Long parentPoid = getLongValue(row.get("PARENT_POID"));
        
        if (level == null) {
            // Set default level based on parent
            if (parentPoid == null) {
                level = 1;
            } else {
                level = 2;
            }
            log.warn("LEVEL field is null for POID {}, parentPoid: {}, setting to: {}",
                    getLongValue(row.get("POID")), parentPoid, level);
        }
        
        Long poid = getLongValue(row.get("POID"));
        if (poid == null) {
            poid = 0L;
        }
        
        String propertyType = (String) row.get("GL_TYPE");
        if (propertyType == null) {
            propertyType = "UNKNOWN";
        }
        
        String itemType = (String) row.get("ITEM_TYPE");
        if (itemType == null) {
            itemType = "ITEM";
        }
        
        // Set item properties
        item.setPoid(poid);
        item.setId(String.valueOf(poid));
        item.setPropertyCostCenterCode(propertyCode);  // Set the property cost center code
        item.setPropertyCostCenterName(propertyName);  // Use parsed name
        item.setParentPropertyPoid(parentPoid);
        item.setLevel(level);
        item.setItemType(itemType);
        item.setPropertyType(propertyType);
        item.setActive("Y");  // Default to active
        
        // Set tree display properties
        item.setIsExpanded(false);
        item.setIsRowGroup("GROUP".equals(itemType));
        
        return item;
    }
    
    /**
     * Sort tree nodes recursively
     */
    private void sortTreeNodes(List<PropertyCostCenterTreeNodeDto> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        // Sort by property type (MAIN_GROUP, SUB_GROUP, CHILD)
        nodes.sort((a, b) -> {
            String typeA = a.getPropertyType() != null ? a.getPropertyType() : "";
            String typeB = b.getPropertyType() != null ? b.getPropertyType() : "";
            
            int orderA = getPropertyTypeOrder(typeA);
            int orderB = getPropertyTypeOrder(typeB);
            
            return Integer.compare(orderA, orderB);
        });
        
        // Recursively sort children
        for (PropertyCostCenterTreeNodeDto node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTreeNodes(node.getChildren());
            }
        }
    }
    
    /**
     * Get sort order for property type
     */
    private int getPropertyTypeOrder(String propertyType) {
        switch (propertyType) {
            case "MAIN_GROUP": return 1;
            case "SUB_GROUP": return 2;
            case "CHILD": return 3;
            default: return 4;
        }
    }

    // List functionality implementation
    @Override
    public List<PropertyCostCenterResponse> getPropertyCostCenterList(String documentId, String actionRequested, Long parentPoid) {
        try {
            log.info("Fetching Property Cost Center list for documentId: {}, actionRequested: {}, parentPoid: {}",
                    documentId, actionRequested, parentPoid);

            // Get data directly from database using repository
            List<PropertyCostCenter> entities;

            if (parentPoid == null) {
                // Get main groups (records with no parent)
                entities = repository.findMainGroups(
                    false, // Always exclude deleted records
                    null   // No group filtering needed
                );
            } else {
                // Get direct children of the specified parent
                entities = repository.findDirectChildren(
                    parentPoid,
                    false, // Always exclude deleted records
                    null   // No group filtering needed
                );
            }

            // Convert entities to list items
            List<PropertyCostCenterResponse> listItems = convertEntitiesToListItems(entities);

            log.info("Successfully retrieved Property Cost Center list with {} items for parentPoid: {}", listItems.size(), parentPoid);
            return listItems;

        } catch (Exception e) {
            log.error("Error fetching Property Cost Center list", e);
            throw new RuntimeException("Error fetching Property Cost Center list: " + e.getMessage(), e);
        }
    }

    /**
     * Convert entities to list items
     */
    private List<PropertyCostCenterResponse> convertEntitiesToListItems(List<PropertyCostCenter> entities) {
        List<PropertyCostCenterResponse> listItems = new ArrayList<>();

        for (PropertyCostCenter entity : entities) {
            PropertyCostCenterResponse listItem = convertEntityToListItem(entity);
            if (listItem != null) {
                listItems.add(listItem);
            }
        }

        // Sort the list items (additional sorting beyond repository)
        sortListItems(listItems);
        return listItems;
    }

    /**
     * Convert a single entity to list item
     */
    private PropertyCostCenterResponse convertEntityToListItem(PropertyCostCenter entity) {
        try {
            if (entity == null || entity.getPropertyCostCenterPoid() == null) {
                return null;
            }

            // Determine level based on parent relationship (same as Cost Center)
            Integer level = (entity.getParentPropertyPoid() == null) ? 0 : 1;

            PropertyCostCenterResponse dto = new PropertyCostCenterResponse();
            dto.setPoid(entity.getPropertyCostCenterPoid());
            dto.setPropertyCostCenterCode(entity.getPropertyCostCenterCode());
            dto.setPropertyCostCenterName(entity.getPropertyCostCenterName());
            dto.setPropertyType(entity.getPropertyType());
            dto.setParentPropertyPoid(entity.getParentPropertyPoid());
            dto.setLevel(level);
            
            dto.setCostCenterPoid(entity.getCostCenterPoid());
            dto.setCompanyPoid(entity.getCompanyPoid());
            dto.setPropertyDescription(entity.getPropertyDescription());
            dto.setRemarks(entity.getRemarks());
            dto.setActive(entity.getActive());
            dto.setDeleted(entity.getDeleted());
            dto.setSeqNo(entity.getSeqNo());
            dto.setCreatedBy(entity.getCreatedBy());
            dto.setCreatedDate(entity.getCreatedDate() != null ? entity.getCreatedDate().toString() : null);
            dto.setLastModifiedBy(entity.getLastModifiedBy());
            dto.setLastModifiedDate(entity.getLastModifiedDate() != null ? entity.getLastModifiedDate().toString() : null);

            return dto;

        } catch (Exception e) {
            log.error("Error converting Property Cost Center entity to list item: {}", entity.getPropertyCostCenterPoid(), e);
            return null;
        }
    }

    /**
     * Sort list items by property cost center code (same pattern as Cost Center)
     */
    private void sortListItems(List<PropertyCostCenterResponse> listItems) {
        listItems.sort((a, b) -> {
            // Sort by property cost center code (treat as numbers if possible, otherwise as strings)
            String codeA = a.getPropertyCostCenterCode() != null ? a.getPropertyCostCenterCode() : "";
            String codeB = b.getPropertyCostCenterCode() != null ? b.getPropertyCostCenterCode() : "";

            try {
                // Try to sort as numbers
                Long numA = Long.parseLong(codeA);
                Long numB = Long.parseLong(codeB);
                return numA.compareTo(numB);
            } catch (NumberFormatException e) {
                // Fall back to string sorting
                return codeA.compareTo(codeB);
            }
        });
    }

    /**
     * Safe conversion of Object to Long (matching Cost Center pattern)
     */
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safe conversion of Object to Integer (matching Cost Center pattern)
     */
    private Integer getIntegerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
