package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.masters.FixedAssetRequestDto;
import com.asg.finance.dto.masters.FixedAssetResponseDto;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.FixedAssetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FixedAssetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FixedAssetService fixedAssetService;

    @InjectMocks
    private FixedAssetController fixedAssetController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FixedAssetRequestDto requestDto;
    private FixedAssetResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fixedAssetController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        requestDto = new FixedAssetRequestDto();
        requestDto.setFaCode("FA001");
        requestDto.setFaDescription("Test Asset");
        requestDto.setAssetType("EQUIPMENT");
        requestDto.setFaCategoryPoid(1L);
        requestDto.setLocationPoid(1L);
        requestDto.setCompanyPoid(1L);
        requestDto.setActive("Y");

        responseDto = new FixedAssetResponseDto();
        responseDto.setFaPoid(1L);
        responseDto.setFaCode("FA001");
        responseDto.setFaDescription("Test Asset");
        responseDto.setAssetType("EQUIPMENT");
        responseDto.setActive("Y");
    }

    @Test
    void createFixedAsset_ShouldCreateSuccessfully() throws Exception {
        when(fixedAssetService.createFixedAsset(any(FixedAssetRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/v1/fixed-asset")
                        .param("documentId", "FA-001")
                        .param("actionRequested", "create")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Fixed Asset created successfully")))
                .andExpect(jsonPath("$.result.data.faPoid", is(1)));

        verify(fixedAssetService).createFixedAsset(any(FixedAssetRequestDto.class));
    }

    @Test
    void createFixedAsset_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
        when(fixedAssetService.createFixedAsset(any()))
                .thenThrow(new ValidationException("FA Description must be unique"));

        mockMvc.perform(post("/v1/fixed-asset")
                        .param("documentId", "FA-001")
                        .param("actionRequested", "create")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("FA Description must be unique")));

        verify(fixedAssetService).createFixedAsset(any());
    }

    @Test
    void createFixedAsset_ShouldReturnInternalServerError_WhenExceptionOccurs() throws Exception {
        when(fixedAssetService.createFixedAsset(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(post("/v1/fixed-asset")
                        .param("documentId", "FA-001")
                        .param("actionRequested", "create")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));

        verify(fixedAssetService).createFixedAsset(any());
    }

    @Test
    void updateFixedAsset_ShouldUpdateSuccessfully() throws Exception {
        when(fixedAssetService.updateFixedAsset(eq(1L), any(FixedAssetRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/v1/fixed-asset/{faPoid}", 1L)
                        .param("documentId", "FA-002")
                        .param("actionRequested", "update")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Fixed Asset updated successfully")))
                .andExpect(jsonPath("$.result.data.faPoid", is(1)));

        verify(fixedAssetService).updateFixedAsset(eq(1L), any(FixedAssetRequestDto.class));
    }

    @Test
    void updateFixedAsset_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        when(fixedAssetService.updateFixedAsset(eq(1L), any(FixedAssetRequestDto.class)))
                .thenThrow(new ValidationException("FA Code must be unique"));

        mockMvc.perform(put("/v1/fixed-asset/{faPoid}", 1L)
                        .param("documentId", "FA-002")
                        .param("actionRequested", "update")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("FA Code must be unique")));

        verify(fixedAssetService).updateFixedAsset(eq(1L), any(FixedAssetRequestDto.class));
    }

    @Test
    void getFixedAssetById_ShouldReturnSuccessfully() throws Exception {
        when(fixedAssetService.getFixedAssetById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/fixed-asset/{faPoid}", 1L)
                        .param("documentId", "FA-003")
                        .param("actionRequested", "view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Fixed Asset fetched successfully")))
                .andExpect(jsonPath("$.result.data.faCode", is("FA001")));

        verify(fixedAssetService).getFixedAssetById(1L);
    }

    @Test
    void softDeleteFixedAsset_ShouldDeleteSuccessfully() throws Exception {
        doNothing().when(fixedAssetService).softDeleteFixedAsset(1L);

        mockMvc.perform(delete("/v1/fixed-asset/{faPoid}", 1L)
                        .param("documentId", "FA-004")
                        .param("actionRequested", "delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Fixed Asset has been soft deleted successfully")));

        verify(fixedAssetService).softDeleteFixedAsset(1L);
    }

    @Test
    void createAssetCopies_ShouldCreateSuccessfully() throws Exception {
        List<Long> newPoidList = List.of(2L, 3L, 4L);
        when(fixedAssetService.createMultipleCopies(1L, 3)).thenReturn(newPoidList);

        mockMvc.perform(post("/v1/fixed-asset/{faPoid}/{noOfCopies}", 1L, 3)
                        .param("documentId", "FA-005")
                        .param("actionRequested", "create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Fixed Asset Copies created successfully")))
                .andExpect(jsonPath("$.result.data[0]", is(2)));

        verify(fixedAssetService).createMultipleCopies(1L, 3);
    }

    @Test
    void createAssetCopies_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
        when(fixedAssetService.createMultipleCopies(1L, 3))
                .thenThrow(new ValidationException("Invalid number of copies"));

        mockMvc.perform(post("/v1/fixed-asset/{faPoid}/{noOfCopies}", 1L, 3)
                        .param("documentId", "FA-005")
                        .param("actionRequested", "create"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid number of copies")));

        verify(fixedAssetService).createMultipleCopies(1L, 3);
    }

    @Test
    void getFixedAssetCategories_ShouldReturnFilteredListSuccessfully() throws Exception {
        // Mock response from service
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("records", List.of(
                Map.of("FA_CATG_CODE", "VEHICLE", "FA_CATG_DESCRIPTION", "Vehicle Asset"),
                Map.of("FA_CATG_CODE", "EQUIPMENT", "FA_CATG_DESCRIPTION", "Equipment Asset")
        ));
        mockResponse.put("totalCount", 2);

        // Mock service behavior
        when(fixedAssetService.listFixedAssetCategories(
                isNull(), any(FilterRequestDto.class), any(Pageable.class)))
                .thenReturn(mockResponse);

        // JSON filter request
        String filterRequestJson = """
    {
      "operator": "AND",
      "isDeleted": "N",
      "filters": [
         { "searchField": "GLOBALSEARCH", "searchValue": "Asset" }
      ]
    }
    """;

        // Perform MockMvc POST request with Pageable params
        mockMvc.perform(post("/v1/fixed-asset/list")
                        .param("documentId", "DOC123")
                        .param("actionRequested", "VIEW")
                        .param("page", "0")        // Pageable page
                        .param("size", "10")       // Pageable size
                        .param("sort", "FA_CATG_CODE,asc") // Optional sort
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestJson))
                .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Fixed Asset Categories fetched successfully")));

        // Verify service call
        verify(fixedAssetService, times(1))
                .listFixedAssetCategories(isNull(), any(FilterRequestDto.class), any(Pageable.class));
    }


}
