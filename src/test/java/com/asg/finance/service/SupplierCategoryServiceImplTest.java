//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceAlreadyExistsException;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.finance.dto.SupplierCategoryDto;
//import com.asg.finance.entity.SupplierCategoryEntity;
//import com.asg.finance.repository.SupplierCategoryRepository;
//import jakarta.persistence.EntityManager;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.lang.reflect.Field;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SupplierCategoryServiceImplTest {
//
//    @Mock
//    private SupplierCategoryRepository supplierCategoriesRepository;
//
//    @Mock
//    private DocumentService documentService;
//
//    @Mock
//    private EntityManager entityManager;
//
//    @InjectMocks
//    private SupplierCategoryServiceImpl supplierCategoryService;
//
//    private SupplierCategoryEntity testEntity;
//    private final Long TEST_CATEGORY_ID = 1L;
//    private final String TEST_CATEGORY_NAME = "Test Category";
//    private final String TEST_CATEGORY_CODE = "TEST";
//
//    @BeforeEach
//    void setUp() {
//        supplierCategoryService = new SupplierCategoryServiceImpl(supplierCategoriesRepository, documentService);
//        testEntity = new SupplierCategoryEntity();
//        testEntity.setSupplierCategoryPoid(TEST_CATEGORY_ID);
//        testEntity.setSupplierCategoryName(TEST_CATEGORY_NAME);
//        testEntity.setSupplierCategoryCode(TEST_CATEGORY_CODE);
//        testEntity.setActive("Y");
//        testEntity.setDeleted("N");
//        testEntity.setSequenceNumber(1);
//    }
//
//    @Test
//    void getSupplierCategoryById_ShouldReturnSupplierCategory_WhenValidIdProvided() {
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//
//        SupplierCategoryDto result = supplierCategoryService.getSupplierCategoryById(TEST_CATEGORY_ID);
//
//        assertNotNull(result);
//        assertEquals(TEST_CATEGORY_ID, result.getSupplierCategoryPoid());
//        assertEquals(TEST_CATEGORY_NAME, result.getSupplierCategoryName());
//        assertEquals(TEST_CATEGORY_CODE, result.getSupplierCategoryCode());
//
//        verify(supplierCategoriesRepository).findById(TEST_CATEGORY_ID);
//    }
//
//    @Test
//    void getSupplierCategoryById_ShouldThrowResourceNotFound_WhenCategoryNotFound() {
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () ->
//                supplierCategoryService.getSupplierCategoryById(TEST_CATEGORY_ID));
//
//        verify(supplierCategoriesRepository).findById(TEST_CATEGORY_ID);
//    }
//
//    @Test
//    void softDeleteSupplierCategory_ShouldUpdateStatus_WhenValidIdProvided() {
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        SupplierCategoryDto result = supplierCategoryService.softDeleteSupplierCategory(TEST_CATEGORY_ID);
//
//        assertNotNull(result);
//        assertEquals("N", result.getActive());
//        assertEquals("Y", result.getDeleted());
//
//        verify(supplierCategoriesRepository).findById(TEST_CATEGORY_ID);
//        verify(supplierCategoriesRepository).save(any(SupplierCategoryEntity.class));
//    }
//
//    @Test
//    void softDeleteSupplierCategory_ShouldThrowResourceNotFound_WhenCategoryNotFound() {
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () ->
//                supplierCategoryService.softDeleteSupplierCategory(TEST_CATEGORY_ID));
//
//        verify(supplierCategoriesRepository).findById(TEST_CATEGORY_ID);
//        verify(supplierCategoriesRepository, never()).save(any(SupplierCategoryEntity.class));
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldUpdateCategory_WhenNameUnchanged() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setSupplierCategoryName(TEST_CATEGORY_NAME);
//        updateDto.setSupplierCategoryName2("Updated Category 2");
//        updateDto.setSeqNo("10");
//        updateDto.setActive("N");
//
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        SupplierCategoryDto result = supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, updateDto);
//
//        assertNotNull(result);
//        assertEquals(TEST_CATEGORY_NAME, result.getSupplierCategoryName());
//        assertEquals("Updated Category 2", result.getSupplierCategoryName2());
//        assertEquals("10", result.getSeqNo());
//        assertEquals("N", result.getActive());
//
//        verify(supplierCategoriesRepository).findById(TEST_CATEGORY_ID);
//        verify(supplierCategoriesRepository).save(any(SupplierCategoryEntity.class));
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldThrowResourceNotFound_WhenCategoryNotFound() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setSupplierCategoryName("Updated Category");
//
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () ->
//                supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, updateDto));
//
//        verify(supplierCategoriesRepository).findById(TEST_CATEGORY_ID);
//        verify(supplierCategoriesRepository, never()).save(any(SupplierCategoryEntity.class));
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldThrowResourceAlreadyExists_WhenNameAlreadyExists() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setSupplierCategoryName("Existing Category");
//
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//        when(supplierCategoriesRepository.existsBySupplierCategoryNameAndSupplierCategoryPoidNot(
//                "Existing Category", TEST_CATEGORY_ID))
//                .thenReturn(true);
//
//        assertThrows(ResourceAlreadyExistsException.class, () ->
//                supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, updateDto));
//
//        verify(supplierCategoriesRepository).findById(TEST_CATEGORY_ID);
//        verify(supplierCategoriesRepository, never()).save(any(SupplierCategoryEntity.class));
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldSetDefaultActiveWhenNull() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setActive(null);
//
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        SupplierCategoryDto result = supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, updateDto);
//
//        assertNotNull(result);
//        assertEquals("Y", result.getActive());
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldSetDefaultActiveWhenBlank() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setActive("");
//
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        SupplierCategoryDto result = supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, updateDto);
//
//        assertNotNull(result);
//        assertEquals("Y", result.getActive());
//    }
//
//    @Test
//    void mapToDto_ShouldHandleNullEntity() {
//        when(supplierCategoriesRepository.findById(999L))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () ->
//                supplierCategoryService.getSupplierCategoryById(999L));
//    }
//
//    @Test
//    void mapToDto_ShouldHandleNullGroupPoid() {
//        testEntity.setGroupPoid(null);
//
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//
//        SupplierCategoryDto result = supplierCategoryService.getSupplierCategoryById(TEST_CATEGORY_ID);
//
//        assertNotNull(result);
//        assertNull(result.getGroupPoid());
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldHandleNullDto() {
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        SupplierCategoryDto result = supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, new SupplierCategoryDto());
//
//        assertNotNull(result);
//        assertEquals("Y", result.getActive());
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldConvertSeqNoToInteger() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setSeqNo("25");
//
//        when(supplierCategoriesRepository.findById(TEST_CATEGORY_ID))
//                .thenReturn(Optional.of(testEntity));
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenAnswer(invocation -> {
//                    SupplierCategoryEntity saved = invocation.getArgument(0);
//                    assertEquals(25, saved.getSequenceNumber());
//                    return saved;
//                });
//
//        SupplierCategoryDto result = supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, updateDto);
//
//        assertNotNull(result);
//        assertEquals("25", result.getSeqNo());
//    }
//
//    @Test
//    void getSupplierCategoryById_ShouldThrowException_WhenNullId() {
//        assertThrows(ResourceNotFoundException.class, () ->
//                supplierCategoryService.getSupplierCategoryById(null));
//    }
//
//    @Test
//    void softDeleteSupplierCategory_ShouldThrowException_WhenNullId() {
//        assertThrows(ResourceNotFoundException.class, () ->
//                supplierCategoryService.softDeleteSupplierCategory(null));
//    }
//
//    @Test
//    void createSupplierCategory_ShouldCreateNewCategory_WhenValidDataProvided() throws Exception {
//        // Arrange
//        SupplierCategoryDto dto = new SupplierCategoryDto();
//        dto.setSupplierCategoryName("Electronics");
//        dto.setSupplierCategoryName2("Electronics 2");
//        dto.setSupplierCategoryCode("EL001");
//        dto.setSeqNo("5");
//        dto.setActive("Y");
//        dto.setGroupPoid(10L);
//
//        SupplierCategoryEntity savedEntity = new SupplierCategoryEntity();
//        savedEntity.setSupplierCategoryPoid(1L);
//        savedEntity.setSupplierCategoryName(dto.getSupplierCategoryName());
//        savedEntity.setSupplierCategoryName2(dto.getSupplierCategoryName2());
//        savedEntity.setSupplierCategoryCode(dto.getSupplierCategoryCode());
//        savedEntity.setGroupPoid(dto.getGroupPoid());
//        savedEntity.setActive("Y");
//        savedEntity.setDeleted("N");
//        savedEntity.setSequenceNumber(5);
//
//
//        when(supplierCategoriesRepository.existsBySupplierCategoryCode(dto.getSupplierCategoryCode()))
//                .thenReturn(false);
//        when(supplierCategoriesRepository.existsBySupplierCategoryName(dto.getSupplierCategoryName()))
//                .thenReturn(false);
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenReturn(savedEntity);
//
//
//        EntityManager entityManager = mock(EntityManager.class);
//        doNothing().when(entityManager).flush();
//        doNothing().when(entityManager).refresh(any(SupplierCategoryEntity.class));
//
//
//        Field entityManagerField = SupplierCategoryServiceImpl.class.getDeclaredField("entityManager");
//        entityManagerField.setAccessible(true);
//        entityManagerField.set(supplierCategoryService, entityManager);
//
//        SupplierCategoryDto result = supplierCategoryService.createSupplierCategory(dto);
//
//        assertNotNull(result);
//        assertEquals("Electronics", result.getSupplierCategoryName());
//        assertEquals("EL001", result.getSupplierCategoryCode());
//        assertEquals("Y", result.getActive());
//        assertEquals("N", result.getDeleted());
//
//        verify(supplierCategoriesRepository).save(any(SupplierCategoryEntity.class));
//        verify(entityManager).flush();
//        verify(entityManager).refresh(any(SupplierCategoryEntity.class));
//    }
//    @Test
//    void createSupplierCategory_ShouldThrowException_WhenDuplicateCodeExists() {
//        SupplierCategoryDto dto = new SupplierCategoryDto();
//        dto.setSupplierCategoryCode("EL001");
//        dto.setSupplierCategoryName("Electronics");
//
//        when(supplierCategoriesRepository.existsBySupplierCategoryCode("EL001"))
//                .thenReturn(true);
//
//        assertThrows(ResourceAlreadyExistsException.class, () ->
//                supplierCategoryService.createSupplierCategory(dto));
//
//        verify(supplierCategoriesRepository, never()).save(any());
//    }
//
//    @Test
//    void createSupplierCategory_ShouldThrowException_WhenDuplicateNameExists() {
//        SupplierCategoryDto dto = new SupplierCategoryDto();
//        dto.setSupplierCategoryCode("EL001");
//        dto.setSupplierCategoryName("Electronics");
//
//        when(supplierCategoriesRepository.existsBySupplierCategoryCode("EL001"))
//                .thenReturn(false);
//        when(supplierCategoriesRepository.existsBySupplierCategoryName("Electronics"))
//                .thenReturn(true);
//
//        assertThrows(ResourceAlreadyExistsException.class, () ->
//                supplierCategoryService.createSupplierCategory(dto));
//
//        verify(supplierCategoriesRepository, never()).save(any());
//    }
//
//    @Test
//    void createSupplierCategory_ShouldSetDefaultActive_WhenActiveIsNull() throws Exception {
//        SupplierCategoryDto dto = new SupplierCategoryDto();
//        dto.setSupplierCategoryCode("EL002");
//        dto.setSupplierCategoryName("Furniture");
//        dto.setActive(null);
//
//        SupplierCategoryEntity savedEntity = new SupplierCategoryEntity();
//        savedEntity.setSupplierCategoryPoid(2L);
//        savedEntity.setSupplierCategoryName("Furniture");
//        savedEntity.setSupplierCategoryCode("EL002");
//        savedEntity.setActive("Y");
//        savedEntity.setDeleted("N");
//
//
//        when(supplierCategoriesRepository.existsBySupplierCategoryCode("EL002"))
//                .thenReturn(false);
//        when(supplierCategoriesRepository.existsBySupplierCategoryName("Furniture"))
//                .thenReturn(false);
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenReturn(savedEntity);
//
//
//        EntityManager entityManager = mock(EntityManager.class);
//        doNothing().when(entityManager).flush();
//        doNothing().when(entityManager).refresh(any(SupplierCategoryEntity.class));
//
//
//        Field entityManagerField = SupplierCategoryServiceImpl.class.getDeclaredField("entityManager");
//        entityManagerField.setAccessible(true);
//        entityManagerField.set(supplierCategoryService, entityManager);
//
//
//        SupplierCategoryDto result = supplierCategoryService.createSupplierCategory(dto);
//
//
//        assertNotNull(result);
//        assertEquals("Y", result.getActive());
//        verify(supplierCategoriesRepository).save(any(SupplierCategoryEntity.class));
//        verify(entityManager).flush();
//        verify(entityManager).refresh(any(SupplierCategoryEntity.class));
//    }
//
//    @Test
//    void createSupplierCategory_ShouldHandleNullSequenceNumber() {
//
//        SupplierCategoryDto dto = new SupplierCategoryDto();
//        dto.setSupplierCategoryCode("EL003");
//        dto.setSupplierCategoryName("Groceries");
//        dto.setSeqNo(null);
//
//        SupplierCategoryEntity savedEntity = new SupplierCategoryEntity();
//        savedEntity.setSupplierCategoryPoid(3L);
//        savedEntity.setSupplierCategoryName("Groceries");
//        savedEntity.setSupplierCategoryCode("EL003");
//        savedEntity.setSequenceNumber(null);
//        savedEntity.setActive("Y");
//        savedEntity.setDeleted("N");
//
//        when(supplierCategoriesRepository.existsBySupplierCategoryCode("EL003"))
//                .thenReturn(false);
//        when(supplierCategoriesRepository.existsBySupplierCategoryName("Groceries"))
//                .thenReturn(false);
//        when(supplierCategoriesRepository.save(any(SupplierCategoryEntity.class)))
//                .thenReturn(savedEntity);
//
//
//        EntityManager entityManager = mock(EntityManager.class);
//        doNothing().when(entityManager).flush();
//        doNothing().when(entityManager).refresh(any(SupplierCategoryEntity.class));
//
//
//        try {
//            Field entityManagerField = SupplierCategoryServiceImpl.class.getDeclaredField("entityManager");
//            entityManagerField.setAccessible(true);
//            entityManagerField.set(supplierCategoryService, entityManager);
//        } catch (Exception e) {
//            fail("Failed to inject entityManager mock", e);
//        }
//
//
//        SupplierCategoryDto result = supplierCategoryService.createSupplierCategory(dto);
//
//        assertNotNull(result);
//        assertEquals("Groceries", result.getSupplierCategoryName());
//        assertEquals("EL003", result.getSupplierCategoryCode());
//        verify(supplierCategoriesRepository).save(any(SupplierCategoryEntity.class));
//        verify(entityManager).flush();
//        verify(entityManager).refresh(any(SupplierCategoryEntity.class));
//    }
//
//    @Test
//    void listOfRecordsAndGenericSearch_ShouldReturnPaginatedResults() {
//        // Arrange
//        String docId = "test-doc";
//        FilterRequestDto request = new FilterRequestDto("AND", "N", List.of());
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Mock DocumentService responses
//        when(documentService.resolveOperator(any(FilterRequestDto.class))).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any(FilterRequestDto.class))).thenReturn("N");
//        when(documentService.resolveFilters(any(FilterRequestDto.class))).thenReturn(new ArrayList<>());
//
//        // Mock search results
//        List<Map<String, Object>> records = new ArrayList<>();
//        Map<String, Object> record = new HashMap<>();
//        record.put("SUPPLIER_CATEGORY_POID", 1L);
//        record.put("SUPPLIER_CATEGORY_NAME", "Test Category");
//        record.put("SUPPLIER_CATEGORY_CODE", "TEST");
//        records.add(record);
//
//        Map<String, String> displayFields = new HashMap<>();
//        displayFields.put("SUPPLIER_CATEGORY_NAME", "Supplier Category Name");
//        displayFields.put("SUPPLIER_CATEGORY_CODE", "Supplier Category Code");
//
//        RawSearchResult searchResult = new RawSearchResult(records, displayFields, 1L);
//
//        when(documentService.search(
//                eq(docId),
//                anyList(),
//                eq("AND"),
//                eq(pageable),
//                eq("N"),
//                eq("SUPPLIER_CATEGORY_NAME"),
//                eq("SUPPLIER_CATEGORY_POID")
//        )).thenReturn(searchResult);
//
//        // Act
//        Map<String, Object> result = supplierCategoryService.listOfRecordsAndGenericSearch(docId, request, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.containsKey("content"));
//        assertTrue(result.containsKey("totalElements"));
//        assertEquals(1L, result.get("totalElements"));
//
//        @SuppressWarnings("unchecked")
//        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
//        assertEquals(1, content.size());
//        assertEquals(1L, content.get(0).get("SUPPLIER_CATEGORY_POID"));
//        assertEquals("Test Category", content.get(0).get("SUPPLIER_CATEGORY_NAME"));
//
//        verify(documentService).resolveOperator(request);
//        verify(documentService).resolveIsDeleted(request);
//        verify(documentService).resolveFilters(request);
//
//        verify(documentService).search(
//                eq(docId),
//                anyList(),
//                eq("AND"),
//                eq(pageable),
//                eq("N"),
//                eq("SUPPLIER_CATEGORY_NAME"),
//                eq("SUPPLIER_CATEGORY_POID")
//        );
//    }
//
//    @Test
//    void listOfRecordsAndGenericSearch_ShouldHandleEmptyResults() {
//        // Arrange
//        String docId = "test-doc";
//        FilterRequestDto request = new FilterRequestDto("AND", "N", new ArrayList<>());
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(documentService.resolveOperator(any(FilterRequestDto.class))).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any(FilterRequestDto.class))).thenReturn("N");
//        when(documentService.resolveFilters(any(FilterRequestDto.class))).thenReturn(new ArrayList<>());
//
//        // Return empty results
//        RawSearchResult rawResult = new RawSearchResult(new ArrayList<>(), new HashMap<>(), 0L);
//
//        when(documentService.search(
//                anyString(),
//                anyList(),
//                anyString(),
//                any(Pageable.class),
//                anyString(),
//                anyString(),
//                anyString()
//        )).thenReturn(rawResult);
//
//        // Act
//        Map<String, Object> result = supplierCategoryService.listOfRecordsAndGenericSearch(docId, request, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.containsKey("content"));
//        assertTrue(result.containsKey("totalElements"));
//        assertEquals(0L, result.get("totalElements"));
//
//        @SuppressWarnings("unchecked")
//        List<?> content = (List<?>) result.get("content");
//        assertTrue(content.isEmpty());
//    }
//
//}
