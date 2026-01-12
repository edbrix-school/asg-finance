package com.asg.finance.service;

import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.PurchaseOrderItemRequestDto;
import com.asg.finance.dto.PurchaseOrderRequest;
import com.asg.finance.dto.PurchaseOrderResponse;
import com.asg.finance.entity.PurchaseOrder;
import com.asg.finance.entity.PurchaseOrderItem;
import com.asg.finance.repository.PurchaseOrderItemRepository;
import com.asg.finance.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImplTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private LovDataService lovService;

//    @Mock
//    private GlobalTermsConditionRepository globalTermsConditionRepository;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    private PurchaseOrderRequest request;
    private PurchaseOrder purchaseOrder;
    private PurchaseOrderItem purchaseOrderItem;

    @BeforeEach
    void setUp() {
        request = createPurchaseOrderRequest();
        purchaseOrder = createPurchaseOrder();
        purchaseOrderItem = createPurchaseOrderItem();
    }

    @Test
    void createGeneralPurchaseOrder_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getUserId).thenReturn("123");

            when(purchaseOrderRepository.save(any())).thenReturn(purchaseOrder);
            when(purchaseOrderItemRepository.saveAll(anyList())).thenReturn(List.of(purchaseOrderItem));

            PurchaseOrderResponse response =
                    purchaseOrderService.createGeneralPurchaseOrder("PO", request);

            assertNotNull(response);
            assertEquals(1L, response.getTransactionPoid());
        }
    }

    @Test
    void createGeneralPurchaseOrder_InvalidRefType() {
        request.setRefType("INVALID");

        assertThrows(RuntimeException.class, () ->
                purchaseOrderService.createGeneralPurchaseOrder("PO", request));
    }

    @Test
    void updatePurchaseOrder_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getUserId).thenReturn("123");

            when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
            when(purchaseOrderItemRepository.findByTransactionPoid(1L))
                    .thenReturn(List.of(purchaseOrderItem));

            when(purchaseOrderRepository.save(any())).thenReturn(purchaseOrder);
            when(purchaseOrderItemRepository.saveAll(anyList())).thenReturn(List.of(purchaseOrderItem));

            PurchaseOrderResponse response =
                    purchaseOrderService.updatePurchaseOrder("PO", 1L, request);

            assertNotNull(response);
        }
    }

    @Test
    void updatePurchaseOrder_NotFound() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                purchaseOrderService.updatePurchaseOrder("PO", 1L, request));
    }

    @Test
    void findById_Success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderItemRepository.findByTransactionPoid(1L)).thenReturn(List.of(purchaseOrderItem));

        PurchaseOrderResponse response = purchaseOrderService.findById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getTransactionPoid());
        verify(purchaseOrderRepository).findById(1L);
        verify(purchaseOrderItemRepository).findByTransactionPoid(1L);
    }

    @Test
    void findById_NotFound() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                purchaseOrderService.findById(1L));
    }

    @Test
    void deletePurchaseOrder_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");

            when(purchaseOrderRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(purchaseOrder));
            when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

            purchaseOrderService.deletePurchaseOrder(1L);

            verify(purchaseOrderRepository).findByTransactionPoid(1L);
            verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
        }
    }

    @Test
    void deletePurchaseOrder_NotFound() {
        when(purchaseOrderRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                purchaseOrderService.deletePurchaseOrder(1L));
    }



    private PurchaseOrderRequest createPurchaseOrderRequest() {
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setTransactionDate(LocalDate.now());
        request.setGroupPoid(1L);
        request.setCompanyPoid(1L);
        request.setSupplierPoid(1L);
        request.setRefType("GENERAL");
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

    private PurchaseOrder createPurchaseOrder() {
        PurchaseOrder po = new PurchaseOrder();
        po.setTransactionPoid(1L);
        po.setTransactionDate(LocalDate.now());
        po.setGroupPoid(1L);
        po.setCompanyPoid(1L);
        po.setSupplierPoid(1L);
        po.setRefType("GENERAL");
        po.setGrandTotal(10.00);
        return po;
    }

    private PurchaseOrderItem createPurchaseOrderItem() {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setTransactionPoid(1L);
        item.setDetRowId(1L);
        item.setStockPoid(1L);
        item.setQty(10.00);
        item.setPrice(10.00);
        item.setTotal(10.00);
        item.setCreatedDate(LocalDateTime.now());
        return item;
    }

    private GlobalTermsInsertRequestDto createGlobalTermsRequest() {
        GlobalTermsInsertRequestDto request = new GlobalTermsInsertRequestDto();
        request.setCompanyPoid(1L);
        request.setDocId("DOC_ID");
        request.setDocKeyPoid(1L);
        request.setDetRowId(1L);
        return request;
    }
}
