//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.finance.dto.SupplierCategoryDto;
//import com.asg.finance.service.SupplierCategoryService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SupplierCategoryControllerTest {
//
//    @Mock
//    private SupplierCategoryService supplierCategoryService;
//
//    @InjectMocks
//    private SupplierCategoryController supplierCategoryController;
//
//    private SupplierCategoryDto testSupplierCategoryDto;
//    private final Long TEST_CATEGORY_ID = 1L;
//    private final String TEST_DOCUMENT_ID = "DOC-123";
//    private final String TEST_ACTION = "view";
//
//    @BeforeEach
//    void setUp() {
//        testSupplierCategoryDto = new SupplierCategoryDto();
//        testSupplierCategoryDto.setSupplierCategoryPoid(TEST_CATEGORY_ID);
//        testSupplierCategoryDto.setSupplierCategoryName("Test Category");
//        testSupplierCategoryDto.setActive("Y");
//    }
//
//    @Test
//    void getSupplierCategoryById_ShouldReturnSupplierCategory_WhenValidIdProvided() {
//
//        when(supplierCategoryService.getSupplierCategoryById(TEST_CATEGORY_ID)).thenReturn(testSupplierCategoryDto);
//
//        ResponseEntity<?> response = supplierCategoryController.getSupplierCategoryById(
//                TEST_CATEGORY_ID, TEST_DOCUMENT_ID, TEST_ACTION);
//
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//
//        verify(supplierCategoryService, times(1)).getSupplierCategoryById(TEST_CATEGORY_ID);
//    }
//
//    @Test
//    void getSupplierCategoryById_ShouldReturn404_WhenCategoryNotFound() {
//
//        when(supplierCategoryService.getSupplierCategoryById(anyLong()))
//                .thenThrow(new RuntimeException("Supplier category not found"));
//
//        assertThrows(RuntimeException.class, () ->
//                supplierCategoryController.getSupplierCategoryById(
//                        999L, TEST_DOCUMENT_ID, TEST_ACTION));
//
//        verify(supplierCategoryService, times(1)).getSupplierCategoryById(999L);
//    }
//
//    @Test
//    void softDeleteSupplierCategory_ShouldSoftDeleteCategory_WhenValidIdProvided() {
//
//        testSupplierCategoryDto.setActive("N");
//        testSupplierCategoryDto.setDeleted("Y");
//
//        when(supplierCategoryService.softDeleteSupplierCategory(TEST_CATEGORY_ID))
//                .thenReturn(testSupplierCategoryDto);
//
//        ResponseEntity<?> response = supplierCategoryController.softDeleteSupplierCategory(
//                TEST_CATEGORY_ID, "DOC-DELETE", "delete");
//
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//
//        verify(supplierCategoryService, times(1)).softDeleteSupplierCategory(TEST_CATEGORY_ID);
//    }
//
//    @Test
//    void softDeleteSupplierCategory_ShouldReturn404_WhenCategoryNotFound() {
//
//        when(supplierCategoryService.softDeleteSupplierCategory(anyLong()))
//                .thenThrow(new RuntimeException("Supplier category not found"));
//
//        assertThrows(RuntimeException.class, () ->
//                supplierCategoryController.softDeleteSupplierCategory(
//                        999L, "DOC-DELETE", "delete"));
//
//        verify(supplierCategoryService, times(1)).softDeleteSupplierCategory(999L);
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldUpdateCategory_WhenValidDataProvided() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setSupplierCategoryName("Updated Category");
//        updateDto.setSupplierCategoryName2("Updated Category 2");
//        updateDto.setSeqNo("10");
//        updateDto.setActive("N");
//
//        SupplierCategoryDto updatedDto = new SupplierCategoryDto();
//        updatedDto.setSupplierCategoryPoid(TEST_CATEGORY_ID);
//        updatedDto.setSupplierCategoryName("Updated Category");
//        updatedDto.setSupplierCategoryName2("Updated Category 2");
//        updatedDto.setSeqNo("10");
//        updatedDto.setActive("N");
//
//        when(supplierCategoryService.updateSupplierCategory(TEST_CATEGORY_ID, updateDto))
//                .thenReturn(updatedDto);
//
//        ResponseEntity<?> response = supplierCategoryController.updateSupplierCategory(
//                TEST_CATEGORY_ID, updateDto, "DOC-UPDATE", "EDIT");
//
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//
//        verify(supplierCategoryService, times(1)).updateSupplierCategory(TEST_CATEGORY_ID, updateDto);
//    }
//
//    @Test
//    void updateSupplierCategory_ShouldReturn404_WhenCategoryNotFound() {
//        SupplierCategoryDto updateDto = new SupplierCategoryDto();
//        updateDto.setSupplierCategoryName("Updated Category");
//
//        when(supplierCategoryService.updateSupplierCategory(anyLong(), any(SupplierCategoryDto.class)))
//                .thenThrow(new RuntimeException("Supplier category not found"));
//
//        assertThrows(RuntimeException.class, () ->
//                supplierCategoryController.updateSupplierCategory(999L, updateDto, "DOC-UPDATE", "EDIT"));
//
//        verify(supplierCategoryService, times(1)).updateSupplierCategory(999L, updateDto);
//    }
//
//    @Test
//    void createSupplierCategory_ShouldReturnCreatedSupplierCategory_WhenValidRequest() {
//
//        SupplierCategoryDto requestDto = new SupplierCategoryDto();
//        requestDto.setSupplierCategoryName("New Category");
//        requestDto.setActive("Y");
//
//        SupplierCategoryDto createdDto = new SupplierCategoryDto();
//        createdDto.setSupplierCategoryPoid(100L);
//        createdDto.setSupplierCategoryName("New Category");
//        createdDto.setActive("Y");
//
//        when(supplierCategoryService.createSupplierCategory(any(SupplierCategoryDto.class)))
//                .thenReturn(createdDto);
//
//
//        ResponseEntity<?> response = supplierCategoryController.createSupplierCategory(
//                requestDto, "DOC-123", "CREATE");
//
//
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//
//        verify(supplierCategoryService, times(1))
//                .createSupplierCategory(any(SupplierCategoryDto.class));
//    }
//
//    @Test
//    void createSupplierCategory_ShouldHandleServiceException() {
//
//        SupplierCategoryDto requestDto = new SupplierCategoryDto();
//        requestDto.setSupplierCategoryName("New Category");
//
//        when(supplierCategoryService.createSupplierCategory(any(SupplierCategoryDto.class)))
//                .thenThrow(new RuntimeException("Database error"));
//
//        assertThrows(RuntimeException.class, () ->
//                supplierCategoryController.createSupplierCategory(
//                        requestDto, "DOC-123", "CREATE"));
//
//        verify(supplierCategoryService, times(1))
//                .createSupplierCategory(any(SupplierCategoryDto.class));
//    }
//
//    @Test
//    void listOfRecordsWithGenericSearch_ShouldReturnSuccessResponse() {
//
//        Pageable pageable = PageRequest.of(0, 10);
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//
//        Map<String, Object> serviceResponse = new HashMap<>();
//        serviceResponse.put("total", 5);
//        serviceResponse.put("data", List.of(new SupplierCategoryDto()));
//        serviceResponse.put("success", true);
//
//        when(supplierCategoryService.listOfRecordsAndGenericSearch(
//                anyString(), any(FilterRequestDto.class), any(Pageable.class)))
//                .thenReturn(serviceResponse);
//
//
//        ResponseEntity<?> response = supplierCategoryController.listOfRecordsWithGenericSearch(
//                pageable, filters, "DOC-001", "VIEW");
//
//        assertNotNull(response);
//        assertEquals(200, response.getStatusCodeValue());
//
//        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
//        assertNotNull(responseBody);
//        assertEquals(200, responseBody.get("statusCode"));
//        assertTrue((Boolean) responseBody.get("success"));
//        assertEquals("Supplier Category list fetched successfully", responseBody.get("message"));
//
//        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
//        assertNotNull(result);
//
//        Map<String, Object> data = (Map<String, Object>) result.get("data");
//        assertNotNull(data);
//        assertEquals(5, data.get("total"));
//        assertTrue(data.get("data") instanceof List);
//    }
//    @Test
//    void listOfRecordsWithGenericSearch_ShouldHandleNullFilters() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 5);
//
//        Map<String, Object> serviceResponse = new HashMap<>();
//        serviceResponse.put("total", 0);
//        serviceResponse.put("data", null);
//        serviceResponse.put("success", true);
//
//        when(supplierCategoryService.listOfRecordsAndGenericSearch(
//                anyString(), isNull(), any(Pageable.class)))
//                .thenReturn(serviceResponse);
//
//        ResponseEntity<?> response = supplierCategoryController.listOfRecordsWithGenericSearch(
//                pageable, null, "DOC-002", "VIEW");
//
//        assertNotNull(response);
//        assertEquals(200, response.getStatusCodeValue());
//
//
//        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
//        assertNotNull(responseBody);
//        assertEquals(200, responseBody.get("statusCode"));
//        assertTrue((Boolean) responseBody.get("success"));
//        assertEquals("Supplier Category list fetched successfully", responseBody.get("message"));
//
//
//        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
//        assertNotNull(result);
//
//        Map<String, Object> data = (Map<String, Object>) result.get("data");
//        assertNotNull(data);
//        assertEquals(0, data.get("total"));
//        assertNull(data.get("data"));
//    }
//}
//
//
