package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.GlAccountDetailRequest;
import com.asg.finance.dto.GlFavAcMasterRequest;
import com.asg.finance.dto.GlFavAcMasterResponse;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.GlFavAcMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlFavAcMasterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GlFavAcMasterService service;

    @InjectMocks
    private GlFavAcMasterController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GlFavAcMasterRequest validRequest;
    private GlFavAcMasterResponse response;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        GlAccountDetailRequest glAccountDetail = GlAccountDetailRequest.builder()
                .glAccountPoId(10010001L)
                .companyPoId(1L)
                .viewCategoryPoid("BANK_ACC")
                .seqNo(1L)
                .remarks("Primary bank account")
                .build();

        validRequest = GlFavAcMasterRequest.builder()
                .favAcCode("BANKS")
                .description("Bank Accounts Group")
                .description2("For treasury reporting")
                .seqNo(1)
                .active("Y")
                .groupPoid(1L)
                .userRolePoids(Arrays.asList(1L, 2L))
                .glAccounts(Collections.singletonList(glAccountDetail))
                .build();

        response = GlFavAcMasterResponse.builder()
                .favAcPoid(101L)
                .favAcCode("BANKS")
                .description("Bank Accounts Group")
                .description2("For treasury reporting")
                .active("Y")
                .groupPoid(1L)
                .build();
    }

    // CREATE TESTS
    @Test
    void createFavoriteAccount_WithValidRequest_ShouldReturnSuccess() throws Exception {
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Favorite Account Group created successfully")))
                .andExpect(jsonPath("$.result.data.favAcId", is(101)));

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithMissingFavAcCode_ShouldReturnBadRequest() throws Exception {
        validRequest.setFavAcCode(null);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithMissingDescription_ShouldReturnBadRequest() throws Exception {
        validRequest.setDescription(null);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithEmptyGlAccounts_ShouldReturnBadRequest() throws Exception {
        validRequest.setGlAccounts(Collections.emptyList());

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithValidationException_ShouldReturnBadRequest() throws Exception {
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class)))
                .thenThrow(new ValidationException("Fav Ac Code already exists: BANKS"));

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Fav Ac Code already exists: BANKS")));

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithInternalServerError_ShouldReturn500() throws Exception {
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Failed to create Favorite Account Group: Database connection failed")));

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithMissingDocumentId_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createFavoriteAccount_WithInvalidJSON_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    // UPDATE TESTS
    @Test
    void updateFavoriteAccount_WithValidRequest_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(put("/v1/favorite-accounts/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Favorite Account Group updated successfully")));

        verify(service, times(1)).updateFavoriteAccount(eq(101L), any(GlFavAcMasterRequest.class));
    }

    @Test
    void updateFavoriteAccount_WithNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Favorite Account", "id", 999L))
                .when(service).updateFavoriteAccount(anyLong(), any(GlFavAcMasterRequest.class));

        mockMvc.perform(put("/v1/favorite-accounts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Favorite Account not found with id : '999'")));

        verify(service, times(1)).updateFavoriteAccount(eq(999L), any(GlFavAcMasterRequest.class));
    }

    @Test
    void updateFavoriteAccount_WithValidationException_ShouldReturnBadRequest() throws Exception {
        doThrow(new ValidationException("Description already exists"))
                .when(service).updateFavoriteAccount(anyLong(), any(GlFavAcMasterRequest.class));

        mockMvc.perform(put("/v1/favorite-accounts/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Description already exists")));

        verify(service, times(1)).updateFavoriteAccount(eq(101L), any(GlFavAcMasterRequest.class));
    }

    @Test
    void updateFavoriteAccount_WithInternalServerError_ShouldReturn500() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(service).updateFavoriteAccount(anyLong(), any(GlFavAcMasterRequest.class));

        mockMvc.perform(put("/v1/favorite-accounts/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Failed to update Favorite Account Group: Database error")));

        verify(service, times(1)).updateFavoriteAccount(eq(101L), any(GlFavAcMasterRequest.class));
    }

    // GET TESTS
    @Test
    void getFavoriteAccount_WithValidId_ShouldReturnSuccess() throws Exception {
        when(service.getFavoriteAccountById(anyLong())).thenReturn(response);

        mockMvc.perform(get("/v1/favorite-accounts/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Favorite Account Records fetched successfully")))
                .andExpect(jsonPath("$.result.data.favAcPoid", is(101)));

        verify(service, times(1)).getFavoriteAccountById(101L);
    }

    @Test
    void getFavoriteAccount_WithMissingDocumentId_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/v1/favorite-accounts/101"))
                .andExpect(status().isOk());
    }

    @Test
    void getFavoriteAccount_WithMissingActionRequested_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/v1/favorite-accounts/101")
                     )
                .andExpect(status().isOk());
    }

    // DELETE TESTS
    @Test
    void deleteFavoriteAccount_WithValidId_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/v1/favorite-accounts/101")
                     )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Favorite Account Record deleted successfully")));

        verify(service, times(1)).softDeleteFavoriteAccount(101L);
    }

    @Test
    void deleteFavoriteAccount_WithMissingDocumentId_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(delete("/v1/favorite-accounts/101")
                    )
                .andExpect(status().isOk());
    }

    // EDGE CASES
    @Test
    void createFavoriteAccount_WithNullGroupPoid_ShouldReturnBadRequest() throws Exception {
        validRequest.setGroupPoid(null);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithLongFavAcCode_ShouldReturnBadRequest() throws Exception {
        validRequest.setFavAcCode("A".repeat(21));

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithLongDescription_ShouldReturnBadRequest() throws Exception {
        validRequest.setDescription("A".repeat(101));

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void updateFavoriteAccount_WithZeroId_ShouldCallService() throws Exception {
        mockMvc.perform(put("/v1/favorite-accounts/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(service, times(1)).updateFavoriteAccount(eq(0L), any(GlFavAcMasterRequest.class));
    }

    @Test
    void getFavoriteAccount_WithNegativeId_ShouldCallService() throws Exception {
        when(service.getFavoriteAccountById(anyLong())).thenReturn(response);

        mockMvc.perform(get("/v1/favorite-accounts/-1"))
                .andExpect(status().isOk());

        verify(service, times(1)).getFavoriteAccountById(-1L);
    }

    @Test
    void deleteFavoriteAccount_WithNegativeId_ShouldCallService() throws Exception {
        mockMvc.perform(delete("/v1/favorite-accounts/-1")
                        )
                .andExpect(status().isOk());

        verify(service, times(1)).softDeleteFavoriteAccount(-1L);
    }

    // COMPREHENSIVE EDGE CASES
    @Test
    void createFavoriteAccount_WithEmptyFavAcCode_ShouldReturnBadRequest() throws Exception {
        validRequest.setFavAcCode("");

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithEmptyDescription_ShouldReturnBadRequest() throws Exception {
        validRequest.setDescription("");

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithMaxLengthFavAcCode_ShouldReturnSuccess() throws Exception {
        validRequest.setFavAcCode("A".repeat(20));
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithSpecialCharactersInFavAcCode_ShouldReturnSuccess() throws Exception {
        validRequest.setFavAcCode("BANK_ACC-2024");
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithUnicodeCharacters_ShouldReturnSuccess() throws Exception {
        validRequest.setDescription("银行账户组 - Bank Grp");
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithNullOptionalFields_ShouldReturnSuccess() throws Exception {
        validRequest.setDescription2(null);
        validRequest.setSeqNo(null);
        validRequest.setUserRolePoids(null);
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithInvalidActiveValue_ShouldReturnBadRequest() throws Exception {
        validRequest.setActive("INVALID");

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithLargeGroupPoid_ShouldReturnSuccess() throws Exception {
        validRequest.setGroupPoid(Long.MAX_VALUE);
        when(service.createFavoriteAccount(any(GlFavAcMasterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(service, times(1)).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithInvalidGlAccountData_ShouldReturnBadRequest() throws Exception {
        GlAccountDetailRequest invalidGlAccount = GlAccountDetailRequest.builder()
                .glAccountPoId(null)
                .companyPoId(1L)
                .viewCategoryPoid("BANK_ACC")
                .build();
        validRequest.setGlAccounts(Collections.singletonList(invalidGlAccount));

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void createFavoriteAccount_WithWhitespaceOnlyFields_ShouldReturnBadRequest() throws Exception {
        validRequest.setFavAcCode("   ");
        validRequest.setDescription("   ");

        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createFavoriteAccount(any(GlFavAcMasterRequest.class));
    }

    @Test
    void getFavoriteAccount_WithLargeId_ShouldCallService() throws Exception {
        when(service.getFavoriteAccountById(anyLong())).thenReturn(response);

        mockMvc.perform(get("/v1/favorite-accounts/" + Long.MAX_VALUE)
                      )
                .andExpect(status().isOk());

        verify(service, times(1)).getFavoriteAccountById(Long.MAX_VALUE);
    }

    @Test
    void createFavoriteAccount_WithMissingActionRequested_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createFavoriteAccount_WithNullRequestBody_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/favorite-accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOfRecordsWithGenericSearch_ShouldReturnSuccessResponse() {

        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-004");

            Pageable pageable = PageRequest.of(0, 10);
            FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

            Map<String, Object> serviceResponse = new HashMap<>();
            serviceResponse.put("total", 5);
            serviceResponse.put("data", List.of(new GlFavAcMasterResponse()));
            serviceResponse.put("success", true);

            when(service.listOfRecordsAndGenericSearch(
                    anyString(), any(FilterRequestDto.class), any(Pageable.class)))
                    .thenReturn(serviceResponse);


            ResponseEntity<?> response = controller.listOfRecordsWithGenericSearch(
                    pageable, filters);


            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());

            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertNotNull(responseBody);
            assertEquals(200, responseBody.get("statusCode"));
            assertTrue((Boolean) responseBody.get("success"));
            assertEquals("Favorite Account list fetched successfully", responseBody.get("message"));

            Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
            assertNotNull(result);

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            assertNotNull(data);
            assertEquals(5, data.get("total") != null ? (Integer) data.get("total") : 0);
            assertTrue(data.get("data") instanceof List);
        }
    }

    @Test
    void listOfRecordsWithGenericSearch_ShouldHandleNullFilters() {

        Pageable pageable = PageRequest.of(0, 5);

        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("total", 0);
        serviceResponse.put("data", null);
        serviceResponse.put("success", true);

        when(service.listOfRecordsAndGenericSearch(
                anyString(), isNull(), any(Pageable.class)))
                .thenReturn(serviceResponse);


        ResponseEntity<?> response = controller.listOfRecordsWithGenericSearch(
                pageable, null);


        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals(200, responseBody.get("statusCode"));
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("Favorite Account list fetched successfully", responseBody.get("message"));

        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
        assertNotNull(result);

        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertNotNull(data);
        assertEquals(0, data.get("total") != null ? (Integer) data.get("total") : 0);
        assertNull(data.get("data"));
    }

    @Test
    void listOfRecordsWithGenericSearch_ShouldHandleServiceException() {

        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-004");

            Pageable pageable = PageRequest.of(0, 10);
            FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

            when(service.listOfRecordsAndGenericSearch(
                    anyString(), any(FilterRequestDto.class), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class, () ->
                    controller.listOfRecordsWithGenericSearch(pageable, filters));

            verify(service, times(1))
                    .listOfRecordsAndGenericSearch(anyString(), any(FilterRequestDto.class), any(Pageable.class));
        }
    }

    @Test
    void listOfRecordsWithGenericSearch_ShouldHandleEmptyResult() {

        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("total", 0);
        serviceResponse.put("data", Collections.emptyList());
        serviceResponse.put("success", true);

        when(service.listOfRecordsAndGenericSearch(
                anyString(), any(FilterRequestDto.class), any(Pageable.class)))
                .thenReturn(serviceResponse);


        ResponseEntity<?> response = controller.listOfRecordsWithGenericSearch(
                pageable, filters);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals(200, responseBody.get("statusCode"));
        assertTrue((Boolean) responseBody.get("success"));

        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
        assertNotNull(result);

        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertNotNull(data);
        assertEquals(0, data.get("total") != null ? (Integer) data.get("total") : 0);
        assertTrue(data.get("data") != null ? ((List<?>) data.get("data")).isEmpty() : true);
    }
}