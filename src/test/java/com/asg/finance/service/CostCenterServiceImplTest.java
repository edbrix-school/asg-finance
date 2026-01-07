//package com.asg.finance.service;
//
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.finance.dto.CostCenterListResponseDto;
//import com.asg.finance.dto.CostCenterRequestDTO;
//import com.asg.finance.dto.CostCenterTreeRequest;
//import com.asg.finance.dto.CostCenterTreeResponseDto;
//import com.asg.finance.entity.CostCenter;
//import com.asg.finance.repository.CostCenterRepository;
//import com.asg.finance.repository.CostCenterTreeViewRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.sql.SQLException;
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class CostCenterServiceImplTest {
//
//    @Mock
//    private CostCenterRepository repository;
//
//    @Mock
//    private CostCenterTreeViewRepository costCenterTreeViewRepository;
//
//    @Mock
//    private DocumentSearchService documentService;
//
//    @InjectMocks
//    private CostCenterServiceImpl costCenterServiceImpl;
//
//    private CostCenterRequestDTO costCenterRequestDTO;
//    private CostCenter costCenter;
//
//    @BeforeEach
//    void setUp() {
//        // Set up mock cost center request DTO
//        costCenterRequestDTO = new CostCenterRequestDTO();
//        costCenterRequestDTO.setCostCenterCode("CC1001");
//        costCenterRequestDTO.setCostCenterDescription("Description 1");
//        /*costCenterRequestDTO.setMisGroup("MIS1");
//        costCenterRequestDTO.setCostCenterGroupTypePoid(1L);
//        costCenterRequestDTO.setCostGroupType("Test");*/
//        costCenterRequestDTO.setCostCenterType("MAIN_GROUP");
//
//        // Set up mock cost center entity
//        costCenter = new CostCenter();
//        costCenter.setCostCenterPoid(12345L);
//        costCenter.setCostCenterCode("CC1001");
//        costCenter.setCostCenterDescription("Description 1");
//        costCenter.setMisGroup("MIS1");
//    }
//
//    @Test
//    void createCostCenter_ShouldCreateCostCenterSuccessfully() {
//        // Given
//        when(repository.existsByCostCenterCode(any())).thenReturn(false);
//        when(repository.existsByCostCenterDescription(any())).thenReturn(false);
//        when(repository.save(any(CostCenter.class))).thenReturn(costCenter);
//
//        // When
//        Long result = costCenterServiceImpl.createCostCenter(costCenterRequestDTO);
//
//        // Then
//        assertEquals(12345L, result);
//        verify(repository, times(1)).save(any(CostCenter.class));
//    }
//
//    @Test
//    void createCostCenter_ShouldThrowValidationException_WhenCostCenterCodeExists() {
//        // Given
//        when(repository.existsByCostCenterCode(any())).thenReturn(true);
//
//        // When & Then
//        ValidationException exception = assertThrows(ValidationException.class, () -> {
//            costCenterServiceImpl.createCostCenter(costCenterRequestDTO);
//        });
//        assertEquals("Cost Center Code must be unique", exception.getMessage());
//        verify(repository, never()).save(any(CostCenter.class));
//    }
//
//    @Test
//    void createCostCenter_ShouldThrowValidationException_WhenCostCenterDescriptionExists() {
//        // Given
//        when(repository.existsByCostCenterCode(any())).thenReturn(false);
//        when(repository.existsByCostCenterDescription(any())).thenReturn(true);
//
//        // When & Then
//        ValidationException exception = assertThrows(ValidationException.class, () -> {
//            costCenterServiceImpl.createCostCenter(costCenterRequestDTO);
//        });
//        assertEquals("Cost Center Description must be unique", exception.getMessage());
//        verify(repository, never()).save(any(CostCenter.class));
//    }
//
//    @Test
//    void createCostCenter_ShouldThrowValidationException_WhenInvalidCostCenterType() {
//        // Given
//        costCenterRequestDTO.setCostCenterType("INVALID_TYPE");
//        when(repository.existsByCostCenterCode(any())).thenReturn(false);
//        when(repository.existsByCostCenterDescription(any())).thenReturn(false);
//
//        // When & Then
//        ValidationException exception = assertThrows(ValidationException.class, () -> {
//            costCenterServiceImpl.createCostCenter(costCenterRequestDTO);
//        });
//        assertEquals("Invalid Cost Center Type. Allowed: MAIN_GROUP, SUB_GROUP, CHILD", exception.getMessage());
//        verify(repository, never()).save(any(CostCenter.class));
//    }
//
//    @Test
//    void createCostCenter_ShouldThrowValidationException_WhenSubGroupWithoutParent() {
//        // Given
//        costCenterRequestDTO.setCostCenterType("SUB_GROUP");
//        costCenterRequestDTO.setParentCostCenterPoid(null);
//        when(repository.existsByCostCenterCode(any())).thenReturn(false);
//        when(repository.existsByCostCenterDescription(any())).thenReturn(false);
//
//        // When & Then
//        ValidationException exception = assertThrows(ValidationException.class, () -> {
//            costCenterServiceImpl.createCostCenter(costCenterRequestDTO);
//        });
//        assertEquals("Parent Cost Center POID is mandatory for Cost Center Type as SUB_GROUP or CHILD", exception.getMessage());
//        verify(repository, never()).save(any(CostCenter.class));
//    }
//
//    @Test
//    void createCostCenter_ShouldCreateSuccessfully_WhenSubGroupWithParent() {
//        // Given
//        costCenterRequestDTO.setCostCenterType("SUB_GROUP");
//        costCenterRequestDTO.setParentCostCenterPoid(100L);
//        when(repository.existsByCostCenterCode(any())).thenReturn(false);
//        when(repository.existsByCostCenterDescription(any())).thenReturn(false);
//        when(repository.save(any(CostCenter.class))).thenReturn(costCenter);
//
//        // When
//        Long result = costCenterServiceImpl.createCostCenter(costCenterRequestDTO);
//
//        // Then
//        assertEquals(12345L, result);
//        verify(repository, times(1)).save(any(CostCenter.class));
//    }
//
//    @Test
//    void softDeleteCountry_ShouldSoftDeleteSuccessfully() {
//        // Given
//        when(repository.findById(any())).thenReturn(Optional.of(costCenter));
//        when(repository.save(any(CostCenter.class))).thenReturn(costCenter);
//
//        // When
//        costCenterServiceImpl.softDeleteCountry(12345L);
//
//        // Then
//        verify(repository, times(1)).save(any(CostCenter.class));
//    }
//
//    @Test
//    void softDeleteCountry_ShouldThrowResourceNotFoundException_WhenCostCenterNotFound() {
//        // Given
//        when(repository.findById(any())).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
//            costCenterServiceImpl.softDeleteCountry(12345L);
//        });
//        assertEquals("CostCenter not found with costCenterPoid : '12345'", exception.getMessage());
//        verify(repository, never()).save(any(CostCenter.class));
//    }
//
//    @Test
//    void updateCostCenter_ShouldUpdateAndReturnUpdatedCostCenter() {
//        // Given
//        when(repository.findById(any())).thenReturn(Optional.of(costCenter));
//        when(repository.existsByCostCenterCodeAndCostCenterPoidNot(any(), any())).thenReturn(false);
//        when(repository.existsByCostCenterDescriptionAndCostCenterPoidNot(any(), any())).thenReturn(false);
//        when(repository.save(any(CostCenter.class))).thenReturn(costCenter);
//
//        // When
//        Long result = costCenterServiceImpl.updateCostCenter(12345L, costCenterRequestDTO);
//
//        // Then
//        assertEquals(12345L, result);
//        verify(repository, times(1)).save(any(CostCenter.class));
//    }
//
//    @Test
//    void updateCostCenter_ShouldThrowResourceNotFoundException_WhenCostCenterNotFound() {
//        // Given
//        when(repository.findById(any())).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
//            costCenterServiceImpl.updateCostCenter(12345L, costCenterRequestDTO);
//        });
//        assertEquals("Cost Center not found with POID:  not found with poid : '12345'", exception.getMessage());
//        verify(repository, never()).save(any(CostCenter.class));
//    }
//
//    @Test
//    void updateCostCenter_ShouldThrowValidationException_WhenCodeNotUnique() {
//        // Given
//        when(repository.findById(any())).thenReturn(Optional.of(costCenter));
//        when(repository.existsByCostCenterCodeAndCostCenterPoidNot(any(), any())).thenReturn(true);
//
//        // When & Then
//        ValidationException exception = assertThrows(ValidationException.class, () -> {
//            costCenterServiceImpl.updateCostCenter(12345L, costCenterRequestDTO);
//        });
//        assertEquals("Cost Center Code must be unique", exception.getMessage());
//        verify(repository, never()).save(any(CostCenter.class));
//    }
//
//    @Test
//    void getCostCenterById_ShouldReturnCostCenterRequestDTO() {
//        // Given
//        when(repository.existsByCostCenterPoid(any())).thenReturn(true);
//        when(repository.findByCostCenterPoid(any())).thenReturn(costCenter);
//
//        // When
//        CostCenterRequestDTO result = costCenterServiceImpl.getCostCenterById(12345L);
//
//        // Then
//        assertNotNull(result);
//        assertEquals("CC1001", result.getCostCenterCode());
//    }
//
//    @Test
//    void getCostCenterById_ShouldThrowResourceNotFoundException_WhenCostCenterNotFound() {
//        // Given
//        when(repository.existsByCostCenterPoid(any())).thenReturn(false);
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
//            costCenterServiceImpl.getCostCenterById(12345L);
//        });
//        assertEquals("CostCenter not found with costCenterPoid : '12345'", exception.getMessage());
//    }
//
//    // ==================== TREE VIEW TESTS ====================
//
//    @Test
//    void testGetCostCenterTree_Success() throws SQLException {
//        // Arrange
//        CostCenterTreeRequest request = CostCenterTreeRequest.builder()
//                .filterValue("Admin")
//                .includeDeleted(false)
//                .build();
//
//        List<Map<String, Object>> procedureResults = createMockProcedureResults();
//
//        when(costCenterTreeViewRepository.callCostCenterTreeViewProcedure(anyString(), anyString(), any(CostCenterTreeRequest.class)))
//                .thenReturn(procedureResults);
//
//        // Act
//        List<CostCenterTreeResponseDto> result = costCenterServiceImpl.getCostCenterTree("400-012", "VIEW", request);
//
//        // Assert
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//        assertEquals(1, result.size()); // Should have 1 root node
//
//        CostCenterTreeResponseDto rootNode = result.get(0);
//        assertEquals(1L, rootNode.getCostCenterPoid());
//        assertEquals("Admin Department", rootNode.getCostCenterDescription()); // Parsed without code
//        assertNull(rootNode.getParentCostCenterPoid());
//        assertEquals(0, rootNode.getLevel()); // 0-based level
//        assertEquals("MAIN_GROUP", rootNode.getCostCenterType());
//        assertTrue(rootNode.getIsRowGroup());
//        assertFalse(rootNode.getIsExpanded());
//        assertEquals("row-1", rootNode.getId());
//
//        // Verify child nodes
//        assertNotNull(rootNode.getChildren());
//        assertEquals(1, rootNode.getChildren().size());
//
//        CostCenterTreeResponseDto childNode = rootNode.getChildren().get(0);
//        assertEquals(2L, childNode.getCostCenterPoid());
//        assertEquals(1L, childNode.getParentCostCenterPoid());
//        assertEquals("HR Department", childNode.getCostCenterDescription());
//
//        verify(costCenterTreeViewRepository, times(1)).callCostCenterTreeViewProcedure(anyString(), anyString(), any());
//    }
//
//    @Test
//    void testGetCostCenterTree_EmptyResults() throws SQLException {
//        // Arrange
//        CostCenterTreeRequest request = CostCenterTreeRequest.builder()
//                .filterValue("NonExistent")
//                .build();
//
//        when(costCenterTreeViewRepository.callCostCenterTreeViewProcedure(anyString(), anyString(), any(CostCenterTreeRequest.class)))
//                .thenReturn(new ArrayList<>());
//
//        // Act
//        List<CostCenterTreeResponseDto> result = costCenterServiceImpl.getCostCenterTree("400-012", "VIEW", request);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(costCenterTreeViewRepository, times(1)).callCostCenterTreeViewProcedure(anyString(), anyString(), any());
//    }
//
//    @Test
//    void testGetCostCenterTree_WithNullValues() throws SQLException {
//        // Arrange - Test null handling
//        CostCenterTreeRequest request = CostCenterTreeRequest.builder().build();
//
//        List<Map<String, Object>> procedureResults = new ArrayList<>();
//        Map<String, Object> record = new HashMap<>();
//        record.put("POID", null); // Null POID
//        record.put("DESCRIPTION", "Test");
//        record.put("PARENT_POID", null);
//        record.put("LVL", null); // Null level
//        record.put("ITEM_TYPE", null); // Null item type
//        record.put("GL_TYPE", null); // Null GL type
//        procedureResults.add(record);
//
//        when(costCenterTreeViewRepository.callCostCenterTreeViewProcedure(anyString(), anyString(), any()))
//                .thenReturn(procedureResults);
//
//        // Act
//        List<CostCenterTreeResponseDto> result = costCenterServiceImpl.getCostCenterTree("400-012", "VIEW", request);
//
//        // Assert - Should handle nulls gracefully with defaults
//        assertNotNull(result);
//        assertEquals(1, result.size());
//
//        CostCenterTreeResponseDto node = result.get(0);
//        assertEquals(0L, node.getCostCenterPoid()); // Default for null POID
//        assertEquals(0, node.getLevel()); // Default for null level with no parent
//        assertEquals("UNKNOWN", node.getCostCenterType()); // Default for null type
//        assertEquals("row-0", node.getId());
//    }
//
//    @Test
//    void testGetCostCenterTree_DescriptionParsing() throws SQLException {
//        // Arrange - Test description parsing with code
//        CostCenterTreeRequest request = CostCenterTreeRequest.builder().build();
//
//        List<Map<String, Object>> procedureResults = new ArrayList<>();
//        Map<String, Object> record = new HashMap<>();
//        record.put("POID", 1L);
//        record.put("DESCRIPTION", "Admin Department(ADMIN001)"); // Description with code
//        record.put("PARENT_POID", null);
//        record.put("LVL", 1);
//        record.put("ITEM_TYPE", "GROUP");
//        record.put("GL_TYPE", "MAIN_GROUP");
//        procedureResults.add(record);
//
//        when(costCenterTreeViewRepository.callCostCenterTreeViewProcedure(anyString(), anyString(), any()))
//                .thenReturn(procedureResults);
//
//        // Act
//        List<CostCenterTreeResponseDto> result = costCenterServiceImpl.getCostCenterTree("400-012", "VIEW", request);
//
//        // Assert - Should parse out the code
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals("Admin Department", result.get(0).getCostCenterDescription()); // Code removed
//        assertEquals("ADMIN001", result.get(0).getCostCenterCode()); // Code extracted
//    }
//
//    @Test
//    void testGetCostCenterTree_SQLException() throws SQLException {
//        // Arrange
//        CostCenterTreeRequest request = CostCenterTreeRequest.builder().build();
//
//        when(costCenterTreeViewRepository.callCostCenterTreeViewProcedure(anyString(), anyString(), any()))
//                .thenThrow(new SQLException("Database error"));
//
//        // Act & Assert
//        RuntimeException exception = assertThrows(RuntimeException.class, () ->
//                costCenterServiceImpl.getCostCenterTree("400-012", "VIEW", request)
//        );
//
//        assertTrue(exception.getMessage().contains("Error fetching Cost Center tree"));
//        verify(costCenterTreeViewRepository, times(1)).callCostCenterTreeViewProcedure(anyString(), anyString(), any());
//    }
//
//    // ==================== LIST VIEW TESTS ====================
//
//    @Test
//    void testGetCostCenterList_MainGroups() {
//        // Arrange
//        List<CostCenter> mainGroups = createMainGroups();
//
//        when(repository.findMainGroups(eq(false), isNull()))
//                .thenReturn(mainGroups);
//
//        // Act
//        List<CostCenterListResponseDto> result = costCenterServiceImpl.getCostCenterList("400-012", "VIEW", null);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.size());
//
//        // Verify first item
//        CostCenterListResponseDto firstItem = result.get(0);
//        assertEquals(1L, firstItem.getCostCenterPoid());
//        assertEquals("CC001", firstItem.getCostCenterCode());
//        assertEquals("Main Office", firstItem.getCostCenterDescription());
//        assertEquals("MAIN_GROUP", firstItem.getCostCenterType());
//        assertNull(firstItem.getParentCostCenterPoid());
//        assertEquals(0, firstItem.getLevel()); // Level 0 for main groups
//
//        verify(repository, times(1)).findMainGroups(eq(false), isNull());
//        verify(repository, never()).findDirectChildren(anyLong(), anyBoolean(), any());
//    }
//
//    @Test
//    void testGetCostCenterList_DirectChildren() {
//        // Arrange
//        Long parentPoid = 1L;
//        List<CostCenter> children = createChildEntities(parentPoid);
//
//        when(repository.findDirectChildren(eq(parentPoid), eq(false), isNull()))
//                .thenReturn(children);
//
//        // Act
//        List<CostCenterListResponseDto> result = costCenterServiceImpl.getCostCenterList("400-012", "VIEW", parentPoid);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.size());
//
//        // Verify child items have parent
//        result.forEach(item -> {
//            assertEquals(parentPoid, item.getParentCostCenterPoid());
//            assertEquals(1, item.getLevel()); // Level 1 for direct children
//        });
//
//        verify(repository, times(1)).findDirectChildren(eq(parentPoid), eq(false), isNull());
//        verify(repository, never()).findMainGroups(anyBoolean(), any());
//    }
//
//    @Test
//    void testGetCostCenterList_EmptyResults() {
//        // Arrange
//        when(repository.findMainGroups(eq(false), isNull()))
//                .thenReturn(new ArrayList<>());
//
//        // Act
//        List<CostCenterListResponseDto> result = costCenterServiceImpl.getCostCenterList("400-012", "VIEW", null);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(repository, times(1)).findMainGroups(eq(false), isNull());
//    }
//
//    @Test
//    void testGetCostCenterList_Sorting() {
//        // Arrange - Create unsorted list
//        List<CostCenter> unsortedEntities = new ArrayList<>();
//
//        CostCenter entity3 = createEntity(3L, "CC003", "Office C", null);
//        CostCenter entity1 = createEntity(1L, "CC001", "Office A", null);
//        CostCenter entity2 = createEntity(2L, "CC002", "Office B", null);
//
//        unsortedEntities.add(entity3);
//        unsortedEntities.add(entity1);
//        unsortedEntities.add(entity2);
//
//        when(repository.findMainGroups(eq(false), isNull()))
//                .thenReturn(unsortedEntities);
//
//        // Act
//        List<CostCenterListResponseDto> result = costCenterServiceImpl.getCostCenterList("400-012", "VIEW", null);
//
//        // Assert - Should be sorted by code
//        assertNotNull(result);
//        assertEquals(3, result.size());
//        assertEquals("CC001", result.get(0).getCostCenterCode());
//        assertEquals("CC002", result.get(1).getCostCenterCode());
//        assertEquals("CC003", result.get(2).getCostCenterCode());
//    }
//
//    @Test
//    void testGetCostCenterList_NumericCodeSorting() {
//        // Arrange - Test numeric sorting
//        List<CostCenter> entities = new ArrayList<>();
//
//        entities.add(createEntity(1L, "100", "Office 100", null));
//        entities.add(createEntity(2L, "20", "Office 20", null));
//        entities.add(createEntity(3L, "5", "Office 5", null));
//
//        when(repository.findMainGroups(eq(false), isNull()))
//                .thenReturn(entities);
//
//        // Act
//        List<CostCenterListResponseDto> result = costCenterServiceImpl.getCostCenterList("400-012", "VIEW", null);
//
//        // Assert - Should be sorted numerically: 5, 20, 100
//        assertNotNull(result);
//        assertEquals(3, result.size());
//        assertEquals("5", result.get(0).getCostCenterCode());
//        assertEquals("20", result.get(1).getCostCenterCode());
//        assertEquals("100", result.get(2).getCostCenterCode());
//    }
//
//    @Test
//    void testGetCostCenterList_WithNullEntity() {
//        // Arrange - Test null entity handling
//        List<CostCenter> entities = new ArrayList<>();
//        entities.add(createEntity(1L, "CC001", "Office A", null));
//        entities.add(null); // Null entity in list
//
//        CostCenter entityWithNullPoid = new CostCenter();
//        entityWithNullPoid.setCostCenterPoid(null); // Null POID
//        entities.add(entityWithNullPoid);
//
//        when(repository.findMainGroups(eq(false), isNull()))
//                .thenReturn(entities);
//
//        // Act
//        List<CostCenterListResponseDto> result = costCenterServiceImpl.getCostCenterList("400-012", "VIEW", null);
//
//        // Assert - Should skip null entities and entities with null POID
//        assertNotNull(result);
//        assertEquals(1, result.size()); // Only valid entity
//        assertEquals(1L, result.get(0).getCostCenterPoid());
//    }
//
//    @Test
//    void testGetCostCenterList_Exception() {
//        // Arrange
//        when(repository.findMainGroups(anyBoolean(), any()))
//                .thenThrow(new RuntimeException("Database error"));
//
//        // Act & Assert
//        RuntimeException exception = assertThrows(RuntimeException.class, () ->
//                costCenterServiceImpl.getCostCenterList("400-012", "VIEW", null)
//        );
//
//        assertTrue(exception.getMessage().contains("Error fetching Cost Center list"));
//    }
//
//    // ==================== HELPER METHODS ====================
//
//    private List<Map<String, Object>> createMockProcedureResults() {
//        List<Map<String, Object>> results = new ArrayList<>();
//
//        // Root node
//        Map<String, Object> root = new HashMap<>();
//        root.put("POID", 1L);
//        root.put("DESCRIPTION", "Admin Department(ADMIN001)");
//        root.put("PARENT_POID", null);
//        root.put("LVL", 1);
//        root.put("ITEM_TYPE", "GROUP");
//        root.put("GL_TYPE", "MAIN_GROUP");
//        results.add(root);
//
//        // Child node
//        Map<String, Object> child = new HashMap<>();
//        child.put("POID", 2L);
//        child.put("DESCRIPTION", "HR Department(HR001)");
//        child.put("PARENT_POID", 1L);
//        child.put("LVL", 2);
//        child.put("ITEM_TYPE", "GROUP");
//        child.put("GL_TYPE", "SUB_GROUP");
//        results.add(child);
//
//        return results;
//    }
//
//    private List<CostCenter> createMainGroups() {
//        List<CostCenter> mainGroups = new ArrayList<>();
//
//        CostCenter group1 = createEntity(1L, "CC001", "Main Office", null);
//        CostCenter group2 = createEntity(2L, "CC002", "Branch Office", null);
//
//        mainGroups.add(group1);
//        mainGroups.add(group2);
//
//        return mainGroups;
//    }
//
//    private List<CostCenter> createChildEntities(Long parentPoid) {
//        List<CostCenter> children = new ArrayList<>();
//
//        CostCenter child1 = createEntity(10L, "CC010", "Regional A", parentPoid);
//        CostCenter child2 = createEntity(11L, "CC011", "Regional B", parentPoid);
//
//        children.add(child1);
//        children.add(child2);
//
//        return children;
//    }
//
//    private CostCenter createEntity(Long poid, String code, String description, Long parentPoid) {
//        CostCenter entity = new CostCenter();
//        entity.setCostCenterPoid(poid);
//        entity.setCostCenterCode(code);
//        entity.setCostCenterDescription(description);
//        entity.setParentCostCenterPoid(parentPoid);
//        entity.setCostCenterType(parentPoid == null ? "MAIN_GROUP" : "SUB_GROUP");
//        entity.setActive("Y");
//        entity.setDeleted("N");
//        entity.setSeqNo(poid.intValue());
//        entity.setCreatedDate(LocalDateTime.now());
//        entity.setLastModifiedDate(LocalDateTime.now());
//        return entity;
//    }
//}
