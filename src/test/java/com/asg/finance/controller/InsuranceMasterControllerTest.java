//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.finance.dto.masters.InsuranceMasterRequestDto;
//import com.asg.finance.dto.masters.InsuranceMasterResponseDto;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.InsuranceMasterService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class InsuranceMasterControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private InsuranceMasterService insuranceMasterService;
//
//    @InjectMocks
//    private InsuranceMasterController insuranceMasterController;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    private InsuranceMasterRequestDto requestDto;
//    private InsuranceMasterResponseDto responseDto;
//
//    @BeforeEach
//    void setUp() {
//        objectMapper.registerModule(new JavaTimeModule());
//
//        mockMvc = MockMvcBuilders.standaloneSetup(insuranceMasterController)
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .build();
//
//        requestDto = InsuranceMasterRequestDto.builder()
//                .insuranceType("Property Insurance")
//                .category("Property")
//                .policyNo("PROP001")
//                .insuranceProvider("ABC Insurance")
//                .fromDate(LocalDate.of(2024, 1, 1))
//                .expiryDate(LocalDate.of(2024, 12, 31))
//                .insuranceAmount(new BigDecimal("100000"))
//                .premiumAmount(new BigDecimal("5000"))
//                .description("Test Insurance")
//                .build();
//
//        responseDto = InsuranceMasterResponseDto.builder()
//                .insurancePoid(1L)
//                .insuranceType("Property Insurance")
//                .insuranceCategory("Property")
//                .policyNo("PROP001")
//                .insuranceProvider("ABC Insurance")
//                .fromDate(LocalDate.of(2024, 1, 1))
//                .expiryDate(LocalDate.of(2024, 12, 31))
//                .status("Active")
//                .insuranceAmount(new BigDecimal("100000"))
//                .premiumAmount(new BigDecimal("5000"))
//                .description("Test Insurance")
//                .createdBy("SYSTEM")
//                .createdDate(LocalDateTime.now())
//                .build();
//    }
//
//    @Test
//    void createInsuranceMaster_ShouldCreateSuccessfully() throws Exception {
//        when(insuranceMasterService.createInsuranceMaster(any(InsuranceMasterRequestDto.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(post("/v1/insurance-master")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Insurance Master created successfully")))
//                .andExpect(jsonPath("$.result.data.insurancePoid", is(1)))
//                .andExpect(jsonPath("$.result.data.policyNo", is("PROP001")));
//
//        verify(insuranceMasterService).createInsuranceMaster(any(InsuranceMasterRequestDto.class));
//    }
//
//    @Test
//    void createInsuranceMaster_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
//        when(insuranceMasterService.createInsuranceMaster(any()))
//                .thenThrow(new ValidationException("Policy Number must be unique per company"));
//
//        mockMvc.perform(post("/v1/insurance-master")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", is("Policy Number must be unique per company")));
//
//        verify(insuranceMasterService).createInsuranceMaster(any());
//    }
//
//    @Test
//    void updateInsuranceMaster_ShouldUpdateSuccessfully() throws Exception {
//        when(insuranceMasterService.updateInsuranceMaster(eq(1L), any(InsuranceMasterRequestDto.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(put("/v1/insurance-master/{insuranceId}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "update")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Insurance Master updated successfully")))
//                .andExpect(jsonPath("$.result.data.insurancePoid", is(1)));
//
//        verify(insuranceMasterService).updateInsuranceMaster(eq(1L), any(InsuranceMasterRequestDto.class));
//    }
//
//    @Test
//    void getInsuranceMasterById_ShouldReturnSuccessfully() throws Exception {
//        when(insuranceMasterService.getInsuranceMasterById(1L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/v1/insurance-master/{insuranceId}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Insurance Master fetched successfully")))
//                .andExpect(jsonPath("$.result.data.policyNo", is("PROP001")))
//                .andExpect(jsonPath("$.result.data.status", is("Active")));
//
//        verify(insuranceMasterService).getInsuranceMasterById(1L);
//    }
//
//    @Test
//    void softDeleteInsuranceMaster_ShouldDeleteSuccessfully() throws Exception {
//        doNothing().when(insuranceMasterService).softDeleteInsuranceMaster(1L);
//
//        mockMvc.perform(delete("/v1/insurance-master/{insuranceId}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Insurance Master has been soft deleted successfully")));
//
//        verify(insuranceMasterService).softDeleteInsuranceMaster(1L);
//    }
//
//    @Test
//    void softDeleteInsuranceMaster_ShouldReturnError_WhenLinkedToPJ() throws Exception {
//        doThrow(new ValidationException("Cannot delete — Insurance linked with Purchase Journal Reference"))
//                .when(insuranceMasterService).softDeleteInsuranceMaster(1L);
//
//        mockMvc.perform(delete("/v1/insurance-master/{insuranceId}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isBadRequest());
//
//        verify(insuranceMasterService).softDeleteInsuranceMaster(1L);
//    }
//
//    @Test
//    void listInsuranceMasters_ShouldReturnFilteredListSuccessfully() throws Exception {
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("records", List.of(
//                Map.of("POLICY_NO", "PROP001", "INSURANCE_PROVIDER", "ABC Insurance", "STATUS", "Active"),
//                Map.of("POLICY_NO", "PROP002", "INSURANCE_PROVIDER", "XYZ Insurance", "STATUS", "Expired")
//        ));
//        mockResponse.put("totalCount", 2);
//
//        when(insuranceMasterService.listInsuranceMasters(
//                eq("600-001"), any(FilterRequestDto.class), any(Pageable.class)))
//                .thenReturn(mockResponse);
//
//        String filterRequestJson = """
//            {
//              "operator": "AND",
//              "isDeleted": "N",
//              "filters": [
//                 { "searchField": "GLOBALSEARCH", "searchValue": "Property" }
//              ]
//            }
//            """;
//
//        mockMvc.perform(post("/v1/insurance-master/list")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "VIEW")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .param("sort", "POLICY_NO,asc")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(filterRequestJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.result.data.totalCount", is(2)))
//                .andExpect(jsonPath("$.result.data.records", hasSize(2)))
//                .andExpect(jsonPath("$.result.data.records[0].POLICY_NO", is("PROP001")));
//
//        verify(insuranceMasterService).listInsuranceMasters(eq("600-001"), any(FilterRequestDto.class), any(Pageable.class));
//    }
//
////    @Test
////    void loadInsuranceMasterData_ShouldReturnLoadDataSuccessfully() throws Exception {
////        Map<String, Object> mockLoadData = new HashMap<>();
////        mockLoadData.put("insuranceTypes", Map.of("data", List.of(
////                Map.of("poid", 1L, "code", "PROPERTY", "label", "Property Insurance")
////        )));
////        mockLoadData.put("categories", Map.of("data", List.of(
////                Map.of("poid", 1L, "code", "MOTOR", "label", "Motor")
////        )));
////        mockLoadData.put("summary", Map.of(
////                "totalInsurances", 10L,
////                "activeInsurances", 8L,
////                "expiringInsurances", List.of()
////        ));
////
////        when(insuranceMasterService.loadInsuranceMasterData()).thenReturn(mockLoadData);
////
////        mockMvc.perform(get("/v1/insurance-master/load")
////                        .param("documentId", "600-001")
////                        .param("actionRequested", "load"))
////                .andExpect(status().isOk())
////                .andExpect(jsonPath("$.success", is(true)))
////                .andExpect(jsonPath("$.message", is("Insurance Master data loaded successfully")))
////                .andExpect(jsonPath("$.result.data.summary.totalInsurances", is(10)))
////                .andExpect(jsonPath("$.result.data.summary.activeInsurances", is(8)));
////
////        verify(insuranceMasterService).loadInsuranceMasterData();
////    }
//
//    @Test
//    void createInsuranceMaster_ShouldReturnBadRequest_WhenMandatoryFieldsMissing() throws Exception {
//        InsuranceMasterRequestDto invalidRequest = InsuranceMasterRequestDto.builder()
//                .policyNo("") // Empty policy number
//                .build();
//
//        mockMvc.perform(post("/v1/insurance-master")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invalidRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void listInsuranceMasters_ShouldReturnError_WhenServiceFails() throws Exception {
//        when(insuranceMasterService.listInsuranceMasters(any(), any(), any()))
//                .thenThrow(new RuntimeException("Database connection failed"));
//
//        mockMvc.perform(post("/v1/insurance-master/list")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)));
//    }
//}