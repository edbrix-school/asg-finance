package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.client.GlobalTermsServiceClient;
import com.asg.finance.dto.PurchaseOrderItemRequestDto;
import com.asg.finance.dto.PurchaseOrderItemResponseDto;
import com.asg.finance.dto.PurchaseOrderRequest;
import com.asg.finance.dto.PurchaseOrderResponse;
import com.asg.finance.entity.ChequeReturnDetail;
import com.asg.finance.entity.PurchaseOrder;
import com.asg.finance.entity.PurchaseOrderItem;
import com.asg.finance.repository.PurchaseOrderItemRepository;
import com.asg.finance.repository.PurchaseOrderRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.PurchaseOrderService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final DocumentSearchService documentService;
    private final GlobalTermsServiceClient globalTermsServiceClient;
    private final LovDataService lovService;
    private final JdbcTemplate jdbcTemplate;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    @Transactional
    public PurchaseOrderResponse createGeneralPurchaseOrder(String documentId, PurchaseOrderRequest request) {

        try {
            PurchaseOrder purchaseOrder = mapToPurchaseOrder(request);
            PurchaseOrder savedPO = purchaseOrderRepository.save(purchaseOrder);

            Long transactionPoid = savedPO.getTransactionPoid();
            
            // Validate after save to get transaction POID
            validatePurchaseOrder(request, transactionPoid, documentId);

            // Logging for create operation
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, documentId, savedPO.getTransactionPoid().toString());

            List<PurchaseOrderItem> savedItems = new ArrayList<>();

            String refType = request.getRefType();

            switch (refType.toUpperCase()) {

                case "GENERAL" , "OPERATIONS"-> {
                    if (request.getItems() != null && !request.getItems().isEmpty()) {
                        savedItems = purchaseOrderItemRepository.saveAll(
                                mapPurchaseOrderItems(request.getItems(), transactionPoid)
                        );
                        
                        // Log each item creation
                        savedItems.forEach(item -> {
                            String logDetail = String.format("Row Created on Purchase Order Item with detRowId: %s", item.getDetRowId());
                            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
                        });
                    }
                }
                case "MTA" -> {
                    callMtaProcedureInNewTransaction(request.getRfqPoid());
                    callMtaDeleteProcedureInNewTransaction(transactionPoid);
                }

                default ->
                        throw new IllegalArgumentException("Invalid RefType: " + refType);
            }

            return mapToPurchaseOrderResponse(savedPO, savedItems);

        } catch (Exception ex) {
            throw new ValidationException("Error while creating Purchase Order: " + ex.getMessage());
        }

    }

    @Override
    @Transactional
    public PurchaseOrderResponse updatePurchaseOrder(String documentId, Long transactionPoid,
                                                        PurchaseOrderRequest request) {
        try {
            PurchaseOrder existingPO = purchaseOrderRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ValidationException("Purchase Order not found with ID: " + transactionPoid));

            // Create a copy of the existing entity for logging
            PurchaseOrder oldEntity = new PurchaseOrder();
            BeanUtils.copyProperties(existingPO, oldEntity);

            updatePurchaseOrderFields(existingPO, request);

            PurchaseOrder updatedPO = purchaseOrderRepository.save(existingPO);
            
            // Validate after update with transaction POID
            validatePurchaseOrder(request, transactionPoid, documentId);

            // Logging for update operation
            loggingService.logChanges(oldEntity, updatedPO, PurchaseOrder.class, documentId, updatedPO.getTransactionPoid().toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

            List<PurchaseOrderItem> updatedItems = new ArrayList<>();

            String refType = updatedPO.getRefType().toUpperCase();

            switch (refType) {

                case "GENERAL", "OPERATIONS" -> {
                    updatedItems = updateGeneralOrOperationItems(transactionPoid, request);
                }
                case "MTA" -> {
                    updatedItems = updateGeneralOrOperationItems(transactionPoid, request);
                    callMtaProcedureInNewTransaction(request.getRfqPoid());
                    callMtaDeleteProcedureInNewTransaction(transactionPoid);
                }

                default -> throw new ValidationException("Invalid RefType for update: " + refType);
            }

            return mapToPurchaseOrderResponse(updatedPO, updatedItems);

        } catch (Exception e) {
            throw new ValidationException("Error during Purchase Order update: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PurchaseOrderResponse findById(Long transactionPoid) {

        PurchaseOrder po = purchaseOrderRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Purchase Order not found for POID: " + transactionPoid));

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByTransactionPoid(transactionPoid);

        return mapToPurchaseOrderResponse(po, items);
    }

    @Override
    @Transactional
    public void deletePurchaseOrder(Long transactionPoid, DeleteReasonDto deleteReasonDto) {

        PurchaseOrder header = purchaseOrderRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() ->
                        new EntityNotFoundException("Purchase Order not found for TransactionPoid: " + transactionPoid)
                );

        documentDeleteService.deleteDocument(
                transactionPoid,
                "AP_PURCHASE_ORDER_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                header.getTransactionDate()
        );

        log.info("Purchase Order deleted successfully: {}", transactionPoid);
    }

    @Override
    public Map<String, Object> listPurchaseOrder(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters,"TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "REF_TYPE",
                "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public String createPOFromRFQ(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            Long poPoid,
            String supplierPoid,
            String rfqPoid
    ) {
        try {

            String result = globalTermsServiceClient.createPoFromRfq(
                    loginGroupPoid,
                    loginUserPoid,
                    loginCompanyPoid,
                    poPoid,
                    supplierPoid,
                    rfqPoid
            );

            log.info("PO created from RFQ using procedure. Result = {}", result);

            return result;

        } catch (Exception e) {
            log.error("Error calling PROC_AP_PO_CREATE_FROM_RFQ: {}", e.getMessage(), e);
            throw new ValidationException("Failed to execute PO creation from RFQ: " + e.getMessage());
        }
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    private PurchaseOrder mapToPurchaseOrder(PurchaseOrderRequest request) {

        return PurchaseOrder.builder()
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now())
                .groupPoid(UserContext.getGroupPoid())
                .docRef(request.getDocRef())
                .companyPoid(UserContext.getCompanyPoid())
                .currencyCode(request.getCurrencyCode())
                .currencyRate(request.getCurrencyRate())
                .expectedDate(request.getExpectedDate())
                .supplierPoid(request.getSupplierPoid())
                .paymentTerms(request.getPaymentTerms())
                .modeOfTransport(request.getModeOfTransport())
                .deliveryTerms(request.getDeliveryTerms())
                .freightForwarder(request.getFreightForwarder())
                .shippingMark(request.getShippingMark())
                .billingAddressPoid(request.getBillingAddressPoid())
                .deliveryAddressPoid(request.getDeliveryAddressPoid())
                .subTotal(request.getSubTotal())
                .discount(request.getDiscount())
                .expenseBySupplier(request.getExpenseBySupplier())
                .grandTotal(request.getGrandTotal())
                .remarks(request.getRemarks())
                .rfqPoid(request.getRfqPoid())
                .poStatus(request.getPoStatus())
                .itemTotal(request.getItemTotal())
                .chargeTotal(request.getChargeTotal())
                .type(request.getType())
                .description(request.getDescription())
                .deliveryMethod(request.getDeliveryMethod())
                .deliveryAddress(request.getDeliveryAddress())
                .salesQtnPoid(request.getSalesQtnPoid())
                .descriptionPrintYn(request.getDescriptionPrintYn())
                .salesInvPoid(request.getSalesInvPoid())
                .salesInvDocRef(request.getSalesInvDocRef())
                .refType(request.getRefType())
                .multiCompany(request.getMultiCompany())
                .voucherNarration(request.getVoucherNarration())
                .pjPoid(request.getPjPoid())
                .validityDate(request.getValidityDate())
                .termsPoid(request.getTermsPoid())
                .purchaseRequestPoid(request.getPurchaseRequestPoid())
                .printDivPoid(request.getPrintDivPoid())
                .grnPoid(request.getGrnPoid())
                .grnRef(request.getGrnRef())
                .grnDate(request.getGrnDate())
                .shipmentMonth(request.getShipmentMonth())
                .discountPercentage(request.getDiscountPercentage())
                .itemDiscountTotal(request.getItemDiscountTotal())
                .itemDiscountTotalPercentage(request.getItemDiscountTotalPercentage())
                .createdBy(UserContext.getUserName())
                .createdDate(LocalDateTime.now())
                .build();
    }

    private List<PurchaseOrderItem> mapPurchaseOrderItems(
            List<PurchaseOrderItemRequestDto> itemDtos,
            Long transactionPoid
    ) {

        List<PurchaseOrderItem> items = new ArrayList<>();

        if (itemDtos == null || itemDtos.isEmpty()) {
            return items;
        }

        Long intial = 1L;
        for (PurchaseOrderItemRequestDto dto : itemDtos) {

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .transactionPoid(transactionPoid)
                    .detRowId(intial)
                    .stockPoid(dto.getStockPoid())
                    .stockUnitPoid(dto.getStockUnitPoid() != null && dto.getStockUnitPoid() != 0 ? dto.getStockUnitPoid() : null)
                    .qty(dto.getQty())
                    .price(dto.getPrice())
                    .discount(dto.getDiscount())
                    .total(dto.getTotal())
                    .remarks(dto.getRemarks())
                    .rfqDetRowId(dto.getRfqDetRowId())
                    .rfqPoid(dto.getRfqPoid())
                    .purReqDetRowId(dto.getPurReqDetRowId())
                    .purReqPoid(dto.getPurReqPoid())
                    .taxPoid(dto.getTaxPoid())
                    .taxPercentage(dto.getTaxPercentage())
                    .taxAmount(dto.getTaxAmount())
                    .itemDtlReadOnly(dto.getItemDtlReadOnly())
                    .pjDetRowId(dto.getPjDetRowId())
                    .pjPoid(dto.getPjPoid())
                    .baseAmount(dto.getBaseAmount())
                    .poImpDetRowId(dto.getPoImpDetRowId())
                    .discountPercentage(dto.getDiscountPercentage())
                    .lastPurPrice(dto.getLastPurPrice())
                    .convertedQty(dto.getConvertedQty())
                    .convertedUnit(dto.getConvertedUnit())
                    .conversionValue(dto.getConversionValue())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .build();

            items.add(item);
            intial++;
        }

        return items;
    }

    private PurchaseOrderResponse mapToPurchaseOrderResponse(
            PurchaseOrder savedPO,
            List<PurchaseOrderItem> savedItems) {

        List<PurchaseOrderItemResponseDto> itemDtos = savedItems.stream()
                .map(item -> PurchaseOrderItemResponseDto.builder()
                        .transactionPoid(item.getTransactionPoid())
                        .detRowId(item.getDetRowId())

                        .stockPoid(item.getStockPoid())
                        .stockUnitPoid(item.getStockUnitPoid())

                        .qty(item.getQty())
                        .price(item.getPrice())
                        .discount(item.getDiscount())
                        .total(item.getTotal())

                        .remarks(item.getRemarks())

                        .rfqDetRowId(item.getRfqDetRowId())
                        .rfqPoid(item.getRfqPoid())

                        .purReqDetRowId(item.getPurReqDetRowId())
                        .purReqPoid(item.getPurReqPoid())

                        .taxPoid(item.getTaxPoid())
                        .taxPercentage(item.getTaxPercentage())
                        .taxAmount(item.getTaxAmount())

                        .itemDtlReadOnly(item.getItemDtlReadOnly())

                        .pjDetRowId(item.getPjDetRowId())
                        .pjPoid(item.getPjPoid())

                        .baseAmount(item.getBaseAmount())

                        .poImpDetRowId(item.getPoImpDetRowId())

                        .discountPercentage(item.getDiscountPercentage())
                        .lastPurPrice(item.getLastPurPrice())

                        .convertedQty(item.getConvertedQty())
                        .convertedUnit(item.getConvertedUnit())
                        .conversionValue(item.getConversionValue())
                        .build()
                )
                .toList();

        return PurchaseOrderResponse.builder()
                .transactionPoid(savedPO.getTransactionPoid())
                .transactionDate(savedPO.getTransactionDate())
                .groupPoid(savedPO.getGroupPoid())
                .docRef(savedPO.getDocRef())
                .companyPoid(savedPO.getCompanyPoid())
                .currencyCode(savedPO.getCurrencyCode())
                .currencyRate(savedPO.getCurrencyRate())
                .expectedDate(savedPO.getExpectedDate())
                .supplierPoid(savedPO.getSupplierPoid())
                .paymentTerms(savedPO.getPaymentTerms())
                .modeOfTransport(savedPO.getModeOfTransport())
                .deliveryTerms(savedPO.getDeliveryTerms())
                .freightForwarder(savedPO.getFreightForwarder())
                .shippingMark(savedPO.getShippingMark())
                .billingAddressPoid(savedPO.getBillingAddressPoid())
                .deliveryAddressPoid(savedPO.getDeliveryAddressPoid())
                .subTotal(savedPO.getSubTotal())
                .discount(savedPO.getDiscount())
                .expenseBySupplier(savedPO.getExpenseBySupplier())
                .grandTotal(savedPO.getGrandTotal())
                .remarks(savedPO.getRemarks())
                .rfqPoid(savedPO.getRfqPoid())
                .poStatus(savedPO.getPoStatus())
                .itemTotal(savedPO.getItemTotal())
                .chargeTotal(savedPO.getChargeTotal())
                .type(savedPO.getType())
                .description(savedPO.getDescription())
                .deliveryMethod(savedPO.getDeliveryMethod())
                .deliveryAddress(savedPO.getDeliveryAddress())
                .salesQtnPoid(savedPO.getSalesQtnPoid())
                .descriptionPrintYn(savedPO.getDescriptionPrintYn())
                .salesInvPoid(savedPO.getSalesInvPoid())
                .salesInvDocRef(savedPO.getSalesInvDocRef())
                .refType(savedPO.getRefType())
                .refTypeDetails(mapLovDetails(savedPO.getRefType(), "PO_REF_TYPE", false))
                .multiCompany(savedPO.getMultiCompany())
                .voucherNarration(savedPO.getVoucherNarration())
                .pjPoid(savedPO.getPjPoid())
                .validityDate(savedPO.getValidityDate())
                .termsPoid(savedPO.getTermsPoid())
                .purchaseRequestPoid(savedPO.getPurchaseRequestPoid())
                .printDivPoid(savedPO.getPrintDivPoid())
                .printDivDetails(mapLovDetails(savedPO.getPrintDivPoid(), "COMPANY_DIVISION", true))
                .grnPoid(savedPO.getGrnPoid())
                .grnRef(savedPO.getGrnRef())
                .grnDate(savedPO.getGrnDate())
                .shipmentMonth(savedPO.getShipmentMonth())
                .discountPercentage(savedPO.getDiscountPercentage())
                .itemDiscountTotal(savedPO.getItemDiscountTotal())
                .itemDiscountTotalPercentage(savedPO.getItemDiscountTotalPercentage())
                .supplierDetails(mapLovDetails(savedPO.getSupplierPoid(), "SUPPLIER_MASTER", true))
                .paymentTermsDetails(mapLovDetailsWithFallback(savedPO.getPaymentTerms(), "PO_PAYMENT_TYPE"))
                .deliveryMethodDetails(mapLovDetailsWithFallback(savedPO.getDeliveryMethod(), "DELIVERY_METHOD"))
                .createdBy(savedPO.getCreatedBy())
                .createdDate(savedPO.getCreatedDate())
                .items(itemDtos)
                .build();
    }

    private LovGetListDto mapLovDetails(Object value, String lovName, boolean isPoid) {
        if (value == null) return null;
        
        try {
            if (isPoid) {
                Long poid = value instanceof Long ? (Long) value : Long.valueOf(value.toString());
                return lovService.getDetailsByPoidAndLovName(poid, lovName);
            } else {
                String code = value.toString();
                return lovService.getDetailsByCodeAndLovName(code, lovName);
            }
        } catch (Exception e) {
            log.warn("Failed to map LOV details for value: {}, lovName: {}, isPoid: {}", value, lovName, isPoid, e);
            return null;
        }
    }

    private LovGetListDto mapLovDetailsWithFallback(Object value, String lovName) {
        if (value == null) return null;
        
        try {
            Long poid = value instanceof Long ? (Long) value : Long.valueOf(value.toString());
            return lovService.getDetailsByPoidAndLovName(poid, lovName);
        } catch (Exception e) {
            try {
                String code = value.toString();
                return lovService.getDetailsByCodeAndLovName(code, lovName);
            } catch (Exception ex) {
                log.warn("Failed to map LOV details for value: {}, lovName: {}", value, lovName, ex);
                return null;
            }
        }
    }

    private void updatePurchaseOrderFields(PurchaseOrder po, PurchaseOrderRequest request) {

        po.setTransactionDate(po.getTransactionDate());
       // po.setDocRef(request.getDocRef());
        po.setCurrencyCode(request.getCurrencyCode());
        po.setCurrencyRate(request.getCurrencyRate());
        po.setExpectedDate(request.getExpectedDate());
        po.setSupplierPoid(request.getSupplierPoid());
        po.setPaymentTerms(request.getPaymentTerms());
        po.setModeOfTransport(request.getModeOfTransport());
        po.setDeliveryTerms(request.getDeliveryTerms());
        po.setFreightForwarder(request.getFreightForwarder());
        po.setShippingMark(request.getShippingMark());
        po.setBillingAddressPoid(request.getBillingAddressPoid());
        po.setDeliveryAddressPoid(request.getDeliveryAddressPoid());
        po.setSubTotal(request.getSubTotal());
        po.setDiscount(request.getDiscount());
        po.setExpenseBySupplier(request.getExpenseBySupplier());
        po.setGrandTotal(request.getGrandTotal());
        po.setRemarks(request.getRemarks());
        po.setRfqPoid(request.getRfqPoid());
        po.setPoStatus(request.getPoStatus());
        po.setItemTotal(request.getItemTotal());
        po.setChargeTotal(request.getChargeTotal());
        po.setType(request.getType());
        po.setDescription(request.getDescription());
        po.setDeliveryMethod(request.getDeliveryMethod());
        po.setDeliveryAddress(request.getDeliveryAddress());
        po.setSalesQtnPoid(request.getSalesQtnPoid());
        po.setDescriptionPrintYn(request.getDescriptionPrintYn());
        po.setSalesInvPoid(request.getSalesInvPoid());
        po.setSalesInvDocRef(request.getSalesInvDocRef());
        po.setRefType(request.getRefType());
        po.setMultiCompany(request.getMultiCompany());
        po.setVoucherNarration(request.getVoucherNarration());
        po.setPjPoid(request.getPjPoid());
        po.setValidityDate(request.getValidityDate());
        po.setTermsPoid(request.getTermsPoid());
        po.setPurchaseRequestPoid(request.getPurchaseRequestPoid());
        po.setPrintDivPoid(request.getPrintDivPoid());
        po.setGrnPoid(request.getGrnPoid());
        po.setGrnRef(request.getGrnRef());
        po.setGrnDate(request.getGrnDate());
        po.setShipmentMonth(request.getShipmentMonth());
        po.setDiscountPercentage(request.getDiscountPercentage());
        po.setItemDiscountTotal(request.getItemDiscountTotal());
        po.setItemDiscountTotalPercentage(request.getItemDiscountTotalPercentage());
        po.setLastModifiedBy(UserContext.getUserName());
        po.setLastModifiedDate(LocalDateTime.now());
    }

    private List<PurchaseOrderItem> updateGeneralOrOperationItems(
            Long transactionPoid,
            PurchaseOrderRequest request
    ) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<PurchaseOrderItem> toSave = new ArrayList<>();
        List<PurchaseOrderItem> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<PurchaseOrderItem>> logRequests = new ArrayList<>();
        
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return new ArrayList<>();
        }
        
        Long maxDetRowId = purchaseOrderItemRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        
        for (PurchaseOrderItemRequestDto dto : request.getItems()) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : "NOCHANGE";
            switch (action) {
                case "ISCREATED":
                    toSave.add(PurchaseOrderItem.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(dto.getDetRowId() != null ? dto.getDetRowId() : ++maxDetRowId)
                            .stockPoid(dto.getStockPoid())
                            .stockUnitPoid(dto.getStockUnitPoid() != null && dto.getStockUnitPoid() != 0 ? dto.getStockUnitPoid() : null)
                            .qty(dto.getQty())
                            .price(dto.getPrice())
                            .discount(dto.getDiscount())
                            .discountPercentage(dto.getDiscountPercentage())
                            .total(dto.getTotal())
                            .remarks(dto.getRemarks())
                            .rfqDetRowId(dto.getRfqDetRowId())
                            .rfqPoid(dto.getRfqPoid())
                            .purReqDetRowId(dto.getPurReqDetRowId())
                            .purReqPoid(dto.getPurReqPoid())
                            .taxPoid(dto.getTaxPoid())
                            .taxPercentage(dto.getTaxPercentage())
                            .taxAmount(dto.getTaxAmount())
                            .itemDtlReadOnly(dto.getItemDtlReadOnly())
                            .pjDetRowId(dto.getPjDetRowId())
                            .pjPoid(dto.getPjPoid())
                            .baseAmount(dto.getBaseAmount())
                            .poImpDetRowId(dto.getPoImpDetRowId())
                            .lastPurPrice(dto.getLastPurPrice())
                            .convertedQty(dto.getConvertedQty())
                            .convertedUnit(dto.getConvertedUnit())
                            .conversionValue(dto.getConversionValue())
                            .createdBy(currentUser)
                            .createdDate(now)
                            .build());
                    break;
                    
                case "ISUPDATED":
                    PurchaseOrderItem existingItem = purchaseOrderItemRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                            .orElseThrow(() -> new ValidationException("Purchase Order Item not found for detRowId: " + dto.getDetRowId()));
                    
                    PurchaseOrderItem oldItem = new PurchaseOrderItem();
                    BeanUtils.copyProperties(existingItem, oldItem);
                    
                    existingItem.setStockPoid(dto.getStockPoid());
                    existingItem.setStockUnitPoid(dto.getStockUnitPoid() != null && dto.getStockUnitPoid() != 0 ? dto.getStockUnitPoid() : null);
                    existingItem.setQty(dto.getQty());
                    existingItem.setPrice(dto.getPrice());
                    existingItem.setDiscount(dto.getDiscount());
                    existingItem.setDiscountPercentage(dto.getDiscountPercentage());
                    existingItem.setTotal(dto.getTotal());
                    existingItem.setRemarks(dto.getRemarks());
                    existingItem.setRfqDetRowId(dto.getRfqDetRowId());
                    existingItem.setRfqPoid(dto.getRfqPoid());
                    existingItem.setPurReqDetRowId(dto.getPurReqDetRowId());
                    existingItem.setPurReqPoid(dto.getPurReqPoid());
                    existingItem.setTaxPoid(dto.getTaxPoid());
                    existingItem.setTaxPercentage(dto.getTaxPercentage());
                    existingItem.setTaxAmount(dto.getTaxAmount());
                    existingItem.setItemDtlReadOnly(dto.getItemDtlReadOnly());
                    existingItem.setPjDetRowId(dto.getPjDetRowId());
                    existingItem.setPjPoid(dto.getPjPoid());
                    existingItem.setBaseAmount(dto.getBaseAmount());
                    existingItem.setPoImpDetRowId(dto.getPoImpDetRowId());
                    existingItem.setLastPurPrice(dto.getLastPurPrice());
                    existingItem.setConvertedQty(dto.getConvertedQty());
                    existingItem.setConvertedUnit(dto.getConvertedUnit());
                    existingItem.setConversionValue(dto.getConversionValue());
                    existingItem.setLastModifiedBy(currentUser);
                    existingItem.setLastModifiedDate(now);
                    toUpdate.add(existingItem);
                    
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldItem, existingItem, PurchaseOrderItem.class, docId, docKeyPoid, logDetailForUpdate));
                    break;
                    
                case "ISDELETED":
                    toDelete.add(dto.getDetRowId());
                    loggingService.logDelete(dto, docId, docKeyPoid);
                    break;
            }
        }
        
        List<PurchaseOrderItem> allItems = new ArrayList<>();
        
        if (!toSave.isEmpty()) {
            List<PurchaseOrderItem> savedItems = purchaseOrderItemRepository.saveAll(toSave);
            allItems.addAll(savedItems);
            savedItems.forEach(e -> {
                String logDetail = String.format("Row Created on Purchase Order Item with detRowId: %s", e.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            });
        }

        if (!toUpdate.isEmpty()) {
            List<PurchaseOrderItem> updatedItems = purchaseOrderItemRepository.saveAll(toUpdate);
            allItems.addAll(updatedItems);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (!toDelete.isEmpty()) {
            purchaseOrderItemRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
        
        return allItems;
    }

    private void validatePurchaseOrder(PurchaseOrderRequest request, Long transactionPoid, String documentId) {
        // Validate items exist and have totals
        validateItemTotals(request);
        
        // Validate supplier VAT
        if (request.getSupplierPoid() != null) {
            validateSupplierVat(request, transactionPoid, documentId);
        }
        
        // Validate purchase request if exists
        if (request.getPurchaseRequestPoid() != null) {
            validatePurchaseRequest(request, transactionPoid, documentId);
        }
    }

    private void validateItemTotals(PurchaseOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
           return;
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int i = 0; i < request.getItems().size(); i++) {
            PurchaseOrderItemRequestDto item = request.getItems().get(i);
            if (item.getTotal() == null || item.getTotal() == 0.0) {
                throw new ValidationException("Total amount is showing as zero. Please note the row number -" + (i + 1));
            }
            totalAmount = totalAmount.add(BigDecimal.valueOf(item.getTotal()));
        }
        
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("No total amounts found in this transaction.");
        }
    }

    private void validateSupplierVat(PurchaseOrderRequest request, Long transactionPoid, String documentId) {
        try {
            final BigDecimal taxAmount = (request.getItems() != null && !request.getItems().isEmpty()) 
                ? request.getItems().stream()
                    .filter(item -> item.getTaxAmount() != null)
                    .map(item -> BigDecimal.valueOf(item.getTaxAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
            
            String procedure = "BEGIN PROC_AP_SUPPLIER_IN_VAT_VALTN(?,?,?,?,?,?,?,?); END;";
            
            jdbcTemplate.execute(procedure, (CallableStatementCallback<Void>) cs -> {
                cs.setObject(1, UserContext.getGroupPoid());
                cs.setObject(2, UserContext.getCompanyPoid());
                cs.setObject(3, UserContext.getUserPoid());
                cs.setObject(4, documentId);
                cs.setObject(5, transactionPoid);
                cs.setObject(6, request.getSupplierPoid());
                cs.setObject(7, taxAmount);
                cs.registerOutParameter(8, OracleTypes.VARCHAR);
                
                cs.execute();
                
                String status = cs.getString(8);
                if (status != null && (status.contains("ERROR") || status.contains("WARNING"))) {
                    throw new ValidationException(status);
                }
                
                return null;
            });
        } catch (Exception e) {
            throw new ValidationException("Supplier VAT validation failed: " + e.getMessage());
        }
    }

    private void validatePurchaseRequest(PurchaseOrderRequest request, Long transactionPoid, String documentId) {
        try {
            String procedure = "BEGIN PROC_AP_PUR_REQ_SAVE_EDIT_VAL(?,?,?,?,?,?,?); END;";
            
            jdbcTemplate.execute(procedure, (CallableStatementCallback<Void>) cs -> {
                cs.setObject(1, UserContext.getGroupPoid());
                cs.setObject(2, UserContext.getCompanyPoid());
                cs.setObject(3, UserContext.getUserPoid());
                cs.setObject(4, documentId);
                cs.setObject(5, transactionPoid);
                cs.setObject(6, request.getPurchaseRequestPoid());
                cs.registerOutParameter(7, OracleTypes.VARCHAR);
                
                cs.execute();
                
                String status = cs.getString(7);
                if (status != null && (status.contains("ERROR") || status.contains("WARNING"))) {
                    throw new ValidationException(status);
                }
                
                return null;
            });
        } catch (Exception e) {
            throw new ValidationException("Purchase request validation failed: " + e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    private void callMtaProcedureInNewTransaction(Long rfqPoid) {

        if (rfqPoid == null) {
            return;
        }

        try {
            String procedure = "BEGIN PROC_RFQ_UPDATE_PURCHASE_PRICE(?,?,?,?,?); END;";
            
            jdbcTemplate.execute(procedure, (CallableStatementCallback<Void>) cs -> {
                cs.setObject(1, UserContext.getGroupPoid());
                cs.setObject(2, UserContext.getCompanyPoid());
                cs.setObject(3, UserContext.getUserPoid());
                cs.setObject(4, String.valueOf(rfqPoid));
                cs.registerOutParameter(5, OracleTypes.VARCHAR);
                
                cs.execute();
                
                String status = cs.getString(5);
                if (status != null && status.contains("ERROR")) {
                    log.error("MTA procedure error: {}", status);
                    throw new ValidationException("MTA procedure failed: " + status);
                } else {
                    log.info("MTA procedure completed successfully: {}", status);
                }
                
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling MTA procedure for RFQ POID: {}", rfqPoid, e);
            throw new ValidationException("Failed to execute MTA procedure: " + e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    private void callMtaDeleteProcedureInNewTransaction(Long transactionPoid) {
        try {
            String procedure = "BEGIN PROC_PO_UPDATE_DELETED_DTLRFQ(?,?,?,?,?); END;";
            
            jdbcTemplate.execute(procedure, (CallableStatementCallback<Void>) cs -> {
                cs.setObject(1, UserContext.getGroupPoid());
                cs.setObject(2, UserContext.getCompanyPoid());
                cs.setObject(3, UserContext.getUserPoid());
                cs.setObject(4, String.valueOf(transactionPoid));
                cs.registerOutParameter(5, OracleTypes.VARCHAR);
                
                cs.execute();
                
                String status = cs.getString(5);
                if (status != null && status.contains("ERROR")) {
                    log.error("MTA delete procedure error: {}", status);
                    throw new ValidationException("MTA delete procedure failed: " + status);
                } else {
                    log.info("MTA delete procedure completed successfully: {}", status);
                }
                
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling MTA delete procedure for Transaction POID: {}", transactionPoid, e);
            throw new ValidationException("Failed to execute MTA delete procedure: " + e.getMessage());
        }
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "200-101");
        params.put("SUBREPORT_1", printService.load("Finance/AP/PurchaseOrderReport1_subreport1.jrxml"));
        JasperReport mainReport = printService.load("Finance/AP/PurchaseOrderReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}
