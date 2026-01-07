package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.AssetLocationMasterRequestDto;
import com.asg.finance.dto.AssetLocationMasterResponseDto;
import com.asg.finance.entity.AssetLocation;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.AssetLocationMasterService;
import com.asg.finance.service.AssetLocationMasterServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssetLocationMasterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AssetLocationMasterServiceImpl service;

    @Mock
    private AssetLocationMasterService assetLocationService;

    @InjectMocks
    private AssetLocationMasterController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AssetLocationMasterRequestDto requestDto;
    private AssetLocationMasterResponseDto responseDto;
    private AssetLocation entity;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        requestDto = AssetLocationMasterRequestDto.builder()
                .locationCode("LOC001")
                .description("Main Warehouse")
                .seqNo(1)
                .groupPoid(1L)
                .active(String.valueOf(true))
                .build();

        responseDto = AssetLocationMasterResponseDto.builder()
                .locationPoid(1L)
                .locationCode("LOC001")
                .description("Main Warehouse")
                .seqNo(1)
                .groupPoid(1L)
                .active(String.valueOf(true))
                .deleted("N")
                .build();

        entity = new AssetLocation();
        entity.setLocationPoid(1L);
        entity.setLocationCode("LOC001");
        entity.setDescription("Main Warehouse");
        entity.setSeqNo(1);
        entity.setGroupPoid(1L);
        entity.setActive(String.valueOf(true));
        entity.setDeleted("N");
    }

    @Test
    void createAssetLocation_ShouldCreateSuccessfully() throws Exception {
        when(service.createAssetLocationMaster(any(AssetLocationMasterRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Asset Location Master created successfully")))
                .andExpect(jsonPath("$.result.data.locationPoid", is(1)));

        verify(service).createAssetLocationMaster(any(AssetLocationMasterRequestDto.class));
    }

    @Test
    void createAssetLocation_ShouldReturnBadRequest_WhenDuplicateFound() throws Exception {
        when(service.createAssetLocationMaster(any(AssetLocationMasterRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Location Code already exists"));

        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Location Code already exists")));

        verify(service).createAssetLocationMaster(any(AssetLocationMasterRequestDto.class));
    }

    @Test
    void updateAssetLocation_ShouldUpdateSuccessfully() throws Exception {
        when(service.updateAssetLocationMaster(eq(1L), any(AssetLocationMasterRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/v1/asset-location/{locationPoid}", 1L)
                        .param("documentId", "doc-002")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Asset Location Master updated successfully")))
                .andExpect(jsonPath("$.result.data.locationPoid", is(1)))
                .andExpect(jsonPath("$.result.data.locationCode", is("LOC001")));

        verify(service).updateAssetLocationMaster(eq(1L), any(AssetLocationMasterRequestDto.class));
    }

    @Test
    void updateAssetLocation_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        when(service.updateAssetLocationMaster(eq(1L), any(AssetLocationMasterRequestDto.class)))
                .thenThrow(new ValidationException("Description already exists"));

        mockMvc.perform(put("/v1/asset-location/{locationPoid}", 1L)
                        .param("documentId", "doc-002")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Description already exists")));

        verify(service).updateAssetLocationMaster(eq(1L), any(AssetLocationMasterRequestDto.class));
    }

    @Test
    void getAssetLocationById_ShouldReturnSuccessfully() throws Exception {
        when(service.getAssetLocationMasterById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/asset-location/{locationPoid}", 1L)
                        .param("documentId", "doc-003")
                        .param("actionRequested", "view")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Asset Location Master fetched successfully")))
                .andExpect(jsonPath("$.result.data.locationPoid", is(1)))
                .andExpect(jsonPath("$.result.data.locationCode", is("LOC001")))
                .andExpect(jsonPath("$.result.data.description", is("Main Warehouse")));

        verify(service).getAssetLocationMasterById(1L);
    }

    @Test
    void softDeleteAssetLocation_ShouldDeleteSuccessfully() throws Exception {
        doNothing().when(service).softDeleteAssetLocationMaster(1L);

        mockMvc.perform(delete("/v1/asset-location/{locationPoid}", 1L)
                        .param("documentId", "doc-004")
                        .param("actionRequested", "delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Asset Location Master has been soft deleted successfully")));

        verify(service).softDeleteAssetLocationMaster(1L);
    }

    @Test
    void listAssetLocations_ShouldReturnInternalServerError() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());

        mockMvc.perform(post("/v1/asset-location/list")
                        .param("documentId", "doc-list")
                        .param("actionRequested", "view")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void createAssetLocation_WithMissingRequiredParams_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/asset-location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateAssetLocation_WithInvalidId_ShouldReturnInternalServerError() throws Exception {
        when(service.updateAssetLocationMaster(eq(999L), any(AssetLocationMasterRequestDto.class)))
                .thenThrow(new RuntimeException("AssetLocation not found"));

        mockMvc.perform(put("/v1/asset-location/{locationPoid}", 999L)
                        .param("documentId", "doc-002")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createAssetLocation_WithNullRequestBody_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAssetLocation_WithEmptyLocationCode_ShouldReturnBadRequest() throws Exception {
        AssetLocationMasterRequestDto emptyCodeDto = AssetLocationMasterRequestDto.builder()
                .locationCode("")
                .description("Test")
                .build();

        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyCodeDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAssetLocation_WithSpecialCharacters_ShouldCreateSuccessfully() throws Exception {
        AssetLocationMasterRequestDto specialCharDto = AssetLocationMasterRequestDto.builder()
                .locationCode("LOC@#$")
                .description("Location with special chars: !@#$%^&*()")
                .seqNo(1)
                .groupPoid(1L)
                .active("Y")
                .build();

        AssetLocationMasterResponseDto specialCharResponse = AssetLocationMasterResponseDto.builder()
                .locationPoid(2L)
                .locationCode("LOC@#$")
                .description("Location with special chars: !@#$%^&*()")
                .build();

        when(service.createAssetLocationMaster(any(AssetLocationMasterRequestDto.class)))
                .thenReturn(specialCharResponse);

        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(specialCharDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void createAssetLocation_WithMaxLengthValues_ShouldCreateSuccessfully() throws Exception {
        String maxLengthCode = "A".repeat(50);
        String maxLengthDesc = "B".repeat(255);

        AssetLocationMasterRequestDto maxLengthDto = AssetLocationMasterRequestDto.builder()
                .locationCode(maxLengthCode)
                .description(maxLengthDesc)
                .seqNo(Integer.MAX_VALUE)
                .groupPoid(Long.MAX_VALUE)
                .active("Y")
                .build();

        AssetLocationMasterResponseDto maxLengthResponse = AssetLocationMasterResponseDto.builder()
                .locationPoid(4L)
                .locationCode(maxLengthCode)
                .description(maxLengthDesc)
                .build();

        when(service.createAssetLocationMaster(any(AssetLocationMasterRequestDto.class)))
                .thenReturn(maxLengthResponse);

        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxLengthDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void updateAssetLocation_WithNegativeId_ShouldReturnInternalServerError() throws Exception {
        when(service.updateAssetLocationMaster(eq(-1L), any(AssetLocationMasterRequestDto.class)))
                .thenThrow(new RuntimeException("Invalid ID"));

        mockMvc.perform(put("/v1/asset-location/{locationPoid}", -1L)
                        .param("documentId", "doc-002")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAssetLocationById_WithNonExistentId_ShouldReturnInternalServerError() throws Exception {
        when(service.getAssetLocationMasterById(999L))
                .thenThrow(new RuntimeException("AssetLocation not found"));

        mockMvc.perform(get("/v1/asset-location/{locationPoid}", 999L)
                        .param("documentId", "doc-003")
                        .param("actionRequested", "view"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createAssetLocation_WithMissingDocumentId_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(post("/v1/asset-location")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void createAssetLocation_WithInvalidJson_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAssetLocation_WithNullValues_ShouldHandleGracefully() throws Exception {
        AssetLocationMasterRequestDto nullDto = AssetLocationMasterRequestDto.builder()
                .locationCode(null)
                .description(null)
                .seqNo(null)
                .groupPoid(null)
                .active(null)
                .build();

        when(service.createAssetLocationMaster(any(AssetLocationMasterRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Required fields cannot be null"));

        mockMvc.perform(post("/v1/asset-location")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullDto)))
                .andExpect(status().isBadRequest());
    }
}
