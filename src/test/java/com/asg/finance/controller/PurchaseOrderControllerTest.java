package com.asg.finance.controller;

import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.PurchaseOrderItemRequestDto;
import com.asg.finance.dto.PurchaseOrderRequest;
import com.asg.finance.dto.PurchaseOrderResponse;
import com.asg.finance.service.PurchaseOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseOrderController purchaseOrderController;

    private ObjectMapper objectMapper;

    private PurchaseOrderRequest request;
    private PurchaseOrderResponse response;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(purchaseOrderController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        request = createPurchaseOrderRequest();
        response = createPurchaseOrderResponse();
    }

    @Test
    void createPurchaseOrder_Success() throws Exception {

        when(purchaseOrderService.createGeneralPurchaseOrder(
                any(), any(PurchaseOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Purchase Order created successfully"
                        )
                ));

        verify(purchaseOrderService)
                .createGeneralPurchaseOrder(any(), any(PurchaseOrderRequest.class));
    }

    @Test
    void createPurchaseOrder_ValidationException() throws Exception {
        when(purchaseOrderService.createGeneralPurchaseOrder(any(), any(PurchaseOrderRequest.class)))
                .thenThrow(new ValidationException("Validation failed"));

        mockMvc.perform(post("/v1/purchase-orders")
                        .header("groupPoid", 1L)
                        .header("companyPoid", 1L)
                        .header("userPoid", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(purchaseOrderService).createGeneralPurchaseOrder(any(), any(PurchaseOrderRequest.class));
    }

    @Test
    void updatePurchaseOrder_Success() throws Exception {

        when(purchaseOrderService.updatePurchaseOrder(
                any(), eq(1L), any(PurchaseOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/v1/purchase-orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Purchase Order updated successfully"
                        )
                ));

        verify(purchaseOrderService)
                .updatePurchaseOrder(any(), eq(1L), any(PurchaseOrderRequest.class));
    }

    @Test
    void findById_Success() throws Exception {

        when(purchaseOrderService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/v1/purchase-orders/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Purchase Order fetched successfully"
                        )
                ));

        verify(purchaseOrderService).findById(1L);
    }


    @Test
    void deletePurchaseOrder_Success() throws Exception {

        doNothing().when(purchaseOrderService).deletePurchaseOrder(1L);

        mockMvc.perform(delete("/v1/purchase-orders/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Purchase Order deleted successfully"
                        )
                ));

        verify(purchaseOrderService).deletePurchaseOrder(1L);
    }

    private PurchaseOrderRequest createPurchaseOrderRequest() {
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setTransactionDate(LocalDate.now());
        request.setGroupPoid(1L);
        request.setCompanyPoid(1L);
        request.setSupplierPoid(1L);
        request.setRefType("GENERAL");
        request.setPaymentTerms("CREATE");
        request.setGrandTotal(10.00);
        request.setItems(List.of(createPurchaseOrderItemRequest()));
        return request;
    }

    private PurchaseOrderItemRequestDto createPurchaseOrderItemRequest() {
        PurchaseOrderItemRequestDto item = new PurchaseOrderItemRequestDto();
        item.setDetRowId(1L);
        item.setStockPoid(1L);
        item.setQty(10.00);
        item.setPrice(10.00);
        item.setTotal(10.00);
        return item;
    }

    private PurchaseOrderResponse createPurchaseOrderResponse() {
        return PurchaseOrderResponse.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .groupPoid(1L)
                .companyPoid(1L)
                .supplierPoid(1L)
                .refType("GENERAL")
                .grandTotal(10.00)
                .items(List.of())
                .build();
    }

    private GlobalTermsInsertRequestDto createGlobalTermsRequest() {
        GlobalTermsInsertRequestDto request = new GlobalTermsInsertRequestDto();
        request.setCompanyPoid(1L);
        request.setDocId("PO-001");
        request.setDocKeyPoid(1L);
        request.setDetRowId(1L);
        return request;
    }
}