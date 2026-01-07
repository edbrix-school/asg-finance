//package com.asg.finance.service;
//
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.utility.ASGHelperUtils;
//import com.asg.finance.dto.PropertyCostCenterRequest;
//import com.asg.finance.dto.PropertyCostCenterResponse;
//import com.asg.finance.dto.PropertyCostCenterTreeNodeDto;
//import com.asg.finance.dto.PropertyCostCenterTreeRequest;
//import com.asg.finance.entity.PropertyCostCenter;
//import com.asg.finance.repository.PropertyCostCenterRepository;
//import com.asg.finance.repository.PropertyCostCenterTreeViewRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PropertyCostCenterServiceImplTest {
//
//    @Mock
//    private PropertyCostCenterRepository repository;
//
//    @Mock
//    private PropertyCostCenterTreeViewRepository treeViewRepository;
//
//    @InjectMocks
//    private PropertyCostCenterServiceImpl service;
//
//    private PropertyCostCenter testEntity;
//    private PropertyCostCenterRequest testRequest;
//
//    @BeforeEach
//    void setUp() {
//        testEntity = new PropertyCostCenter();
//        testEntity.setPropertyCostCenterPoid(1L);
//        testEntity.setPropertyCostCenterCode("PROP001");
//        testEntity.setPropertyCostCenterName("Main Office");
//        testEntity.setPropertyType("MAIN_GROUP");
//        testEntity.setActive("Y");
//        testEntity.setDeleted("N");
//
//        testRequest = new PropertyCostCenterRequest();
//        testRequest.setPropertyCostCenterCode("PROP001");
//        testRequest.setPropertyCostCenterName("Main Office");
//        testRequest.setPropertyType("MAIN_GROUP");
//        testRequest.setActive("Y");
//    }
//
//    @Test
//    void createPropertyCostCenter_WithValidData_ReturnsResponse() {
//        when(repository.findByPropertyCostCenterCode("PROP001")).thenReturn(Optional.empty());
//        when(repository.save(any(PropertyCostCenter.class))).thenReturn(testEntity);
//
//        PropertyCostCenterResponse result = service.createPropertyCostCenter(testRequest);
//
//        assertNotNull(result);
//        assertEquals(1L, result.getPoid());
//        assertEquals("PROP001", result.getPropertyCostCenterCode());
//        assertEquals("Main Office", result.getPropertyCostCenterName());
//        verify(repository).save(any(PropertyCostCenter.class));
//    }
//
//    @Test
//    void createPropertyCostCenter_WithDuplicateCode_ThrowsException() {
//        when(repository.findByPropertyCostCenterCode("PROP001")).thenReturn(Optional.of(testEntity));
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            service.createPropertyCostCenter(testRequest);
//        });
//    }
//
//    @Test
//    void getPropertyCostCenterById_WithValidId_ReturnsResponse() {
//        when(repository.findByPropertyCostCenterPoid(1L)).thenReturn(Optional.of(testEntity));
//
//        PropertyCostCenterResponse result = service.getPropertyCostCenterById(1L);
//
//        assertNotNull(result);
//        assertEquals(1L, result.getPoid());
//        assertEquals("PROP001", result.getPropertyCostCenterCode());
//    }
//
//    @Test
//    void getPropertyCostCenterById_WithInvalidId_ThrowsException() {
//        when(repository.findByPropertyCostCenterPoid(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            service.getPropertyCostCenterById(1L);
//        });
//    }
//
//    @Test
//    void updatePropertyCostCenter_WithValidData_ReturnsUpdatedResponse() {
//        when(repository.findByPropertyCostCenterPoidAndDeleted(1L, "N")).thenReturn(Optional.of(testEntity));
//        when(repository.save(any(PropertyCostCenter.class))).thenReturn(testEntity);
//
//        PropertyCostCenterResponse result = service.updatePropertyCostCenter(1L, testRequest);
//
//        assertNotNull(result);
//        assertEquals("PROP001", result.getPropertyCostCenterCode());
//        verify(repository).save(any(PropertyCostCenter.class));
//    }
//
//    @Test
//    void updatePropertyCostCenter_WithInvalidId_ThrowsException() {
//        when(repository.findByPropertyCostCenterPoidAndDeleted(1L, "N")).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            service.updatePropertyCostCenter(1L, testRequest);
//        });
//    }
//
//    @Test
//    void softDeleteByPoid_WithValidId_DeletesSuccessfully() {
//        try (MockedStatic<ASGHelperUtils> mockedUtils = mockStatic(ASGHelperUtils.class)) {
//            mockedUtils.when(ASGHelperUtils::getCurrentUser).thenReturn("testUser");
//            when(repository.findByPropertyCostCenterPoidAndDeleted(1L, "N")).thenReturn(Optional.of(testEntity));
//
//            service.softDeleteByPoid(1L);
//
//            assertEquals("Y", testEntity.getDeleted());
//            verify(repository).save(testEntity);
//        }
//    }
//
//    @Test
//    void softDeleteByPoid_WithInvalidId_ThrowsException() {
//        when(repository.findByPropertyCostCenterPoidAndDeleted(1L, "N")).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            service.softDeleteByPoid(1L);
//        });
//    }
//
//    @Test
//    void getPropertyCostCenterTree_WithValidData_ReturnsTree() throws Exception {
//        PropertyCostCenterTreeRequest request = PropertyCostCenterTreeRequest.builder()
//                .filterValue("Office")
//                .build();
//
//        List<Map<String, Object>> mockData = createMockTreeData();
//        when(treeViewRepository.callPropertyCostCenterTreeViewProcedure("DOC001", "VIEW", request))
//                .thenReturn(mockData);
//
//        List<PropertyCostCenterTreeNodeDto> result = service.getPropertyCostCenterTree("DOC001", "VIEW", request);
//
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//        assertEquals(1, result.size());
//
//        PropertyCostCenterTreeNodeDto rootNode = result.get(0);
//        assertEquals(1L, rootNode.getPoid());
//        assertEquals("Main Office", rootNode.getPropertyCostCenterName());
//        assertEquals("MAIN_GROUP", rootNode.getPropertyType());
//    }
//
//    @Test
//    void getPropertyCostCenterTree_WithEmptyData_ReturnsEmptyList() throws Exception {
//        PropertyCostCenterTreeRequest request = PropertyCostCenterTreeRequest.builder().build();
//        when(treeViewRepository.callPropertyCostCenterTreeViewProcedure("DOC001", "VIEW", request))
//                .thenReturn(new ArrayList<>());
//
//        List<PropertyCostCenterTreeNodeDto> result = service.getPropertyCostCenterTree("DOC001", "VIEW", request);
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getPropertyCostCenterTree_WithException_ThrowsRuntimeException() throws Exception {
//        PropertyCostCenterTreeRequest request = PropertyCostCenterTreeRequest.builder().build();
//        when(treeViewRepository.callPropertyCostCenterTreeViewProcedure("DOC001", "VIEW", request))
//                .thenThrow(new RuntimeException("Database error"));
//
//        assertThrows(RuntimeException.class, () -> {
//            service.getPropertyCostCenterTree("DOC001", "VIEW", request);
//        });
//    }
//
//    @Test
//    void getPropertyCostCenterList_WithNullParent_ReturnsMainGroups() {
//        List<PropertyCostCenter> mockEntities = Arrays.asList(testEntity);
//        when(repository.findMainGroups(false, null)).thenReturn(mockEntities);
//
//        List<PropertyCostCenterResponse> result = service.getPropertyCostCenterList("DOC001", "VIEW", null);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals("PROP001", result.get(0).getPropertyCostCenterCode());
//        assertEquals(0, result.get(0).getLevel());
//    }
//
//    @Test
//    void getPropertyCostCenterList_WithParentId_ReturnsChildren() {
//        List<PropertyCostCenter> mockEntities = Arrays.asList(testEntity);
//        when(repository.findDirectChildren(1L, false, null)).thenReturn(mockEntities);
//
//        List<PropertyCostCenterResponse> result = service.getPropertyCostCenterList("DOC001", "VIEW", 1L);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    void getPropertyCostCenterList_WithException_ThrowsRuntimeException() {
//        when(repository.findMainGroups(false, null)).thenThrow(new RuntimeException("Database error"));
//
//        assertThrows(RuntimeException.class, () -> {
//            service.getPropertyCostCenterList("DOC001", "VIEW", null);
//        });
//    }
//
//    private List<Map<String, Object>> createMockTreeData() {
//        List<Map<String, Object>> data = new ArrayList<>();
//
//        Map<String, Object> rootNode = new HashMap<>();
//        rootNode.put("POID", 1L);
//        rootNode.put("DESCRIPTION", "Main Office(PROP001)");
//        rootNode.put("PARENT_POID", null);
//        rootNode.put("LVL", 1);
//        rootNode.put("ITEM_TYPE", "GROUP");
//        rootNode.put("GL_TYPE", "MAIN_GROUP");
//        data.add(rootNode);
//
//        return data;
//    }
//}
