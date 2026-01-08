package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.CostCenterListResponseDto;
import com.asg.finance.dto.CostCenterRequestDTO;
import com.asg.finance.dto.CostCenterTreeRequest;
import com.asg.finance.dto.CostCenterTreeResponseDto;
import com.asg.finance.entity.CostCenter;
import com.asg.finance.repository.CostCenterRepository;
import com.asg.finance.repository.CostCenterTreeViewRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.CostCenterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostCenterServiceImpl implements CostCenterService {
    private final CostCenterRepository repository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final CostCenterTreeViewRepository costCenterTreeViewRepository;
    
    private static final Logger log = LoggerFactory.getLogger(CostCenterServiceImpl.class);

    @Transactional
    public Long createCostCenter(CostCenterRequestDTO dto) {
        // Check uniqueness
        if (repository.existsByCostCenterCode(dto.getCostCenterCode())) {
            throw new ValidationException("Cost Center Code must be unique");
        }
        if (repository.existsByCostCenterDescription(dto.getCostCenterDescription())) {
            throw new ValidationException("Cost Center Description must be unique");
        }

        validateCostCenterType(dto);

        String currentUser = getCurrentUser();
               return repository.save(CostCenter.builder()
                       .costCenterCode(dto.getCostCenterCode())
                       .costCenterDescription(dto.getCostCenterDescription())
                       .costCenterDescription2(dto.getCostCenterDescription2())
                       .groupPoid(UserContext.getGroupPoid())
                       .remarks(dto.getRemarks())
                       .active(dto.getActive())
                       .seqNo(dto.getSeqNo())
                       .costCenterType(dto.getCostCenterType())
                       .parentCostCenterPoid(dto.getParentCostCenterPoid())
                               .costCenterChild(Objects.equals(dto.getCostCenterType(), "MAIN_GROUP") ? "N" : "Y")
                       .createdBy(currentUser)
                       .createdDate(LocalDateTime.now())
                       .lastModifiedBy(currentUser)
                       .lastModifiedDate(LocalDateTime.now())
                       .deleted("N")
                       .build())
                       .getCostCenterPoid();

    }

    @Transactional
    public void softDeleteCountry(Long costCenterPoid, DeleteReasonDto deleteReasonDto) {
        CostCenter existing = repository.findById(costCenterPoid)
                .orElseThrow(() -> new ResourceNotFoundException("CostCenter", "costCenterPoid", costCenterPoid));
        
        // Check if this cost center has active children
        if (hasActiveChildren(costCenterPoid)) {
            throw new ValidationException("This Cost Center Master cannot be deleted as it has related child records.");
        }
        
        documentDeleteService.deleteDocument(
                costCenterPoid,
                "GL_COST_CENTER_MASTER",
                "COST_CENTER_POID",
                deleteReasonDto,
                null
        );
    }

    private boolean hasActiveChildren(Long parentPoid) {
        return repository.existsByParentCostCenterPoidAndDeleted(parentPoid, "N");
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Transactional
    public Long updateCostCenter(Long poid, CostCenterRequestDTO dto) {
        CostCenter existing = repository.findById(poid)
                .orElseThrow(() -> new ResourceNotFoundException("Cost Center not found with POID: ", "poid", poid));
        
        // Only check uniqueness if the code is actually changing
        if (!existing.getCostCenterCode().equals(dto.getCostCenterCode()) && 
            repository.existsByCostCenterCodeAndCostCenterPoidNot(dto.getCostCenterCode(), poid)) {
            throw new ValidationException("Cost Center Code must be unique");
        }
        
        // Only check uniqueness if the description is actually changing
        if (!existing.getCostCenterDescription().equals(dto.getCostCenterDescription()) && 
            repository.existsByCostCenterDescriptionAndCostCenterPoidNot(dto.getCostCenterDescription(), poid)) {
            throw new ValidationException("Cost Center Description must be unique");
        }

        validateCostCenterType(dto);
        String currentUser = getCurrentUser();
        existing.setCostCenterCode(dto.getCostCenterCode());
        existing.setCostCenterDescription(dto.getCostCenterDescription());
        existing.setCostCenterDescription2(dto.getCostCenterDescription2());
        existing.setGroupPoid(UserContext.getGroupPoid());
        existing.setRemarks(dto.getRemarks());
        existing.setActive(dto.getActive());
        existing.setSeqNo(dto.getSeqNo());
        existing.setCostCenterType(dto.getCostCenterType());
        existing.setParentCostCenterPoid(Objects.equals(dto.getCostCenterType(), "MAIN_GROUP") ? null :dto.getParentCostCenterPoid());
        existing.setCostCenterChild(Objects.equals(dto.getCostCenterType(), "MAIN_GROUP") ? "N" : "Y");
        existing.setLastModifiedBy(currentUser);
        existing.setLastModifiedDate(LocalDateTime.now());
        return repository.save(existing).getCostCenterPoid();
    }

    /*
     * map the ParentCostCenter.
     */
    private DetailsDto mapParent(CostCenter costCenterParent) {
        return new DetailsDto(
        		costCenterParent.getCostCenterPoid(),
        		costCenterParent.getCostCenterCode(),
        		costCenterParent.getCostCenterDescription(),
        		costCenterParent.getCostCenterPoid(),
        		costCenterParent.getCostCenterDescription(),
        		costCenterParent.getSeqNo()
        );
    }

    public CostCenterRequestDTO getCostCenterById(Long costCenterPoid) {
    	if (!repository.existsByCostCenterPoid(costCenterPoid)) {
            throw new ResourceNotFoundException("CostCenter", "costCenterPoid", costCenterPoid);
        }
        CostCenter costCenter = repository.findByCostCenterPoid(costCenterPoid);
        CostCenterRequestDTO costCenterDto = new CostCenterRequestDTO();
        costCenterDto.setCostCenterCode(costCenter.getCostCenterCode());
        costCenterDto.setCostCenterDescription(costCenter.getCostCenterDescription());
        costCenterDto.setCostCenterDescription2(costCenter.getCostCenterDescription2());
        costCenterDto.setCostCenterType(costCenter.getCostCenterType());
        costCenterDto.setParentCostCenterPoid(costCenter.getParentCostCenterPoid());
        costCenterDto.setGroupPoid(costCenter.getGroupPoid());
        costCenterDto.setRemarks(costCenter.getRemarks());
        costCenterDto.setActive(costCenter.getActive());
        costCenterDto.setSeqNo(costCenter.getSeqNo());
        
        Optional.ofNullable(costCenter.getParentCostCenterPoid())
        .map(repository::findByCostCenterPoid)
        .ifPresent(parent -> costCenterDto.setParentCostCenterPoidDtl(mapParent(parent)));
        
        return costCenterDto;
    }

    private void validateCostCenterType(CostCenterRequestDTO request) {
        if (request.getCostCenterType() != null && !request.getCostCenterType().matches("MAIN_GROUP|SUB_GROUP|CHILD")) {
            throw new ValidationException("Invalid Cost Center Type. Allowed: MAIN_GROUP, SUB_GROUP, CHILD");
        }
        if (("SUB_GROUP".equals(request.getCostCenterType()) || "CHILD".equals(request.getCostCenterType())) 
            && request.getParentCostCenterPoid() == null) {
            throw new ValidationException("Parent Cost Center POID is mandatory for Cost Center Type as SUB_GROUP or CHILD");
        }
    }

    // Tree functionality implementation
    @Override
    public List<CostCenterTreeResponseDto> getCostCenterTree(String documentId, String actionRequested, CostCenterTreeRequest request) {
        try {
            log.info("Fetching Cost Center tree for documentId: {}, actionRequested: {}", documentId, actionRequested);

            // Call the stored procedure via repository
            List<Map<String, Object>> treeViewData = costCenterTreeViewRepository.callCostCenterTreeViewProcedure(documentId, actionRequested, request);

            if (treeViewData.isEmpty()) {
                log.info("No Cost Center tree data found");
                return new ArrayList<>();
            }

            // Build tree structure
            List<CostCenterTreeResponseDto> treeNodes = buildTreeStructure(treeViewData, request);

            log.info("Successfully built Cost Center tree with {} root nodes", treeNodes.size());
            return treeNodes;

        } catch (Exception e) {
            log.error("Error fetching Cost Center tree", e);
            throw new RuntimeException("Error fetching Cost Center tree: " + e.getMessage(), e);
        }
    }


    private List<CostCenterTreeResponseDto> buildTreeStructure(List<Map<String, Object>> treeViewData, CostCenterTreeRequest request) {
        // Convert records to tree nodes
        List<CostCenterTreeResponseDto> allNodes = treeViewData.stream()
                .map(this::convertToTreeItem)
                .collect(Collectors.toList());

        // Build parent-child relationships
        Map<Long, CostCenterTreeResponseDto> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(
                        node -> getLongValue(node.getCostCenterPoid()),
                        node -> node
                ));

        List<CostCenterTreeResponseDto> rootNodes = new ArrayList<>();

        for (CostCenterTreeResponseDto node : allNodes) {
            Long parentPoid = getLongValue(node.getParentCostCenterPoid());
            
            if (parentPoid == null) {
                // This is a root node
                rootNodes.add(node);
            } else {
                // This is a child node
                CostCenterTreeResponseDto parent = nodeMap.get(parentPoid);
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        // Sort tree nodes
        sortTreeNodes(rootNodes);

        return rootNodes;
    }

    private CostCenterTreeResponseDto convertToTreeItem(Map<String, Object> record) {
        String description = (String) record.get("DESCRIPTION");
        String costCenterCode = "";
        String costCenterDescription = description;

        // Parse description to extract code and description
        if (description != null && description.contains("(") && description.contains(")")) {
            int openParen = description.lastIndexOf("(");
            int closeParen = description.lastIndexOf(")");
            if (openParen > 0 && closeParen > openParen) {
                costCenterCode = description.substring(openParen + 1, closeParen);
                costCenterDescription = description.substring(0, openParen).trim();
            }
        }

        Integer level = getIntegerValue(record.get("LVL"));
        Long parentPoid = getLongValue(record.get("PARENT_POID"));

        if (level == null) {
            if (parentPoid == null) {
                level = 1;
            } else {
                level = 2;
            }
            log.warn("LEVEL field is null for POID {}, parentPoid: {}, setting to: {}",
                    getLongValue(record.get("POID")), parentPoid, level);
        }

        Long poid = getLongValue(record.get("POID"));
        if (poid == null) {
            poid = 0L;
        }

        String costCenterType = (String) record.get("GL_TYPE");
        if (costCenterType == null) {
            costCenterType = "UNKNOWN";
        }

        String itemType = (String) record.get("ITEM_TYPE");
        if (itemType == null) {
            itemType = "ITEM";
        }

        CostCenterTreeResponseDto treeNode = new CostCenterTreeResponseDto();
        treeNode.setCostCenterPoid(poid);
        treeNode.setCostCenterCode(costCenterCode);
        treeNode.setCostCenterDescription(costCenterDescription);
        treeNode.setCostCenterType(costCenterType);
        treeNode.setParentCostCenterPoid(parentPoid);
        treeNode.setLevel(level - 1); // Convert to 0-based level
        treeNode.setDeleted(false); // Cost Center tree doesn't include deleted flag in procedure
        treeNode.setActive("Y"); // Set active flag as String

        treeNode.setId("row-" + poid);
        treeNode.setIsExpanded(false);
        treeNode.setIsRowGroup("GROUP".equals(itemType));
        treeNode.setChildren(new ArrayList<>());

        return treeNode;
    }

    private void sortTreeNodes(List<CostCenterTreeResponseDto> nodes) {
        // Sort by cost center code
        nodes.sort((a, b) -> {
            String codeA = a.getCostCenterCode() != null ? a.getCostCenterCode() : "";
            String codeB = b.getCostCenterCode() != null ? b.getCostCenterCode() : "";
            return codeA.compareTo(codeB);
        });

        // Recursively sort children
        for (CostCenterTreeResponseDto node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortTreeNodes(node.getChildren());
            }
        }
    }

    // Helper methods
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

    // List functionality implementation
    @Override
    public List<CostCenterListResponseDto> getCostCenterList(String documentId, String actionRequested, Long parentPoid) {
        try {
            log.info("Fetching Cost Center list for documentId: {}, actionRequested: {}, parentPoid: {}",
                    documentId, actionRequested, parentPoid);

            // Get data directly from database using repository
            List<CostCenter> entities;

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
            List<CostCenterListResponseDto> listItems = convertEntitiesToListItems(entities);

            log.info("Successfully retrieved Cost Center list with {} items for parentPoid: {}", listItems.size(), parentPoid);
            return listItems;

        } catch (Exception e) {
            log.error("Error fetching Cost Center list", e);
            throw new RuntimeException("Error fetching Cost Center list: " + e.getMessage(), e);
        }
    }

    /**
     * Convert entities to list items
     */
    private List<CostCenterListResponseDto> convertEntitiesToListItems(List<CostCenter> entities) {
        List<CostCenterListResponseDto> listItems = new ArrayList<>();

        for (CostCenter entity : entities) {
            CostCenterListResponseDto listItem = convertEntityToListItem(entity);
            if (listItem != null) {
                listItems.add(listItem);
            }
        }

        // Sort the list items by cost center code (additional sorting beyond repository)
        sortListItems(listItems);
        return listItems;
    }

    /**
     * Convert a single entity to list item
     */
    private CostCenterListResponseDto convertEntityToListItem(CostCenter entity) {
        try {
            if (entity == null || entity.getCostCenterPoid() == null) {
                return null;
            }

            // Determine level based on parent relationship (same as GL Master)
            Integer level = (entity.getParentCostCenterPoid() == null) ? 0 : 1;

            CostCenterListResponseDto dto = new CostCenterListResponseDto();
            dto.setCostCenterPoid(entity.getCostCenterPoid());
            dto.setCostCenterCode(entity.getCostCenterCode());
            dto.setCostCenterDescription(entity.getCostCenterDescription());
            dto.setCostCenterDescription2(entity.getCostCenterDescription2());
            dto.setCostCenterType(entity.getCostCenterType());
            dto.setParentCostCenterPoid(entity.getParentCostCenterPoid());
            dto.setLevel(level);
            
            dto.setCostCenterGroupTypePoid(entity.getCostCenterGroupTypePoid());
            dto.setCostGroupType(entity.getCostGroupType());
            dto.setCompanyPoid(entity.getCompanyPoid());
            dto.setGroupPoid(entity.getGroupPoid());
            dto.setMisGroup(entity.getMisGroup());
            dto.setRemarks(entity.getRemarks());
            dto.setCostCenterChild(entity.getCostCenterChild());
            dto.setActive(entity.getActive());
            dto.setDeleted(entity.getDeleted());
            dto.setSeqNo(entity.getSeqNo());
            
            // Audit fields
            dto.setCreatedBy(entity.getCreatedBy());
            dto.setCreatedDate(entity.getCreatedDate() != null ? entity.getCreatedDate().toString() : null);
            dto.setLastModifiedBy(entity.getLastModifiedBy());
            dto.setLastModifiedDate(entity.getLastModifiedDate() != null ? entity.getLastModifiedDate().toString() : null);

            return dto;

        } catch (Exception e) {
            log.error("Error converting Cost Center entity to list item: {}", entity.getCostCenterPoid(), e);
            return null;
        }
    }

    /**
     * Sort list items by cost center code (same pattern as GL Master)
     */
    private void sortListItems(List<CostCenterListResponseDto> listItems) {
        listItems.sort((a, b) -> {
            // Sort by cost center code (treat as numbers if possible, otherwise as strings)
            String codeA = a.getCostCenterCode() != null ? a.getCostCenterCode() : "";
            String codeB = b.getCostCenterCode() != null ? b.getCostCenterCode() : "";

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

}

