package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.ApPaymentRequestHdrRequestDto;
import com.asg.finance.dto.ApPaymentRequestHdrResponseDto;
import com.asg.finance.dto.ApPaymentRequestMapper;
import com.asg.finance.entity.ApPaymentRequestDtl;
import com.asg.finance.entity.ApPaymentRequestHdr;
import com.asg.finance.entity.ApPaymentRequestStockDtl;
import com.asg.finance.repository.ApPaymentRequestCustomRepository;
import com.asg.finance.repository.ApPaymentRequestDtlRepository;
import com.asg.finance.repository.ApPaymentRequestHdrRepository;
import com.asg.finance.repository.ApPaymentRequestStockDtlRepository;
import com.asg.finance.service.ApPaymentRequestService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Transactional
public class ApPaymentRequestServiceImpl implements ApPaymentRequestService {

    private final ApPaymentRequestHdrRepository hdrRepository;
    private final ApPaymentRequestDtlRepository dtlRepository;
    private final ApPaymentRequestStockDtlRepository stockDtlRepository;
    private final DocumentSearchService documentService;
    private final ApPaymentRequestCustomRepository aapPaymentRequestCustomRepository;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    public ApPaymentRequestHdrResponseDto create(ApPaymentRequestHdrRequestDto requestDto) {
        ApPaymentRequestHdr hdr =
                ApPaymentRequestMapper.toEntity(requestDto, null);

        hdr.setCreatedBy(getCurrentUser());
        hdr.setCreatedDate(LocalDateTime.now());

        // 🔑 ID GENERATED HERE
        hdr = hdrRepository.save(hdr);

        Long transactionPoid = hdr.getTransactionPoid();

        // Auto-generate detRowId for new records
        List<ApPaymentRequestDtl> details = new java.util.ArrayList<>();
        long detRowId = 1;
        
        for (var detailDto : requestDto.getDetails()) {
            detailDto.setDetRowId(detRowId++); // Auto-generate detRowId
            ApPaymentRequestDtl detail = ApPaymentRequestMapper.toDtlEntity(transactionPoid, detailDto);
            details.add(detail);
        }

        dtlRepository.saveAll(details);

        // Save stock details
        List<ApPaymentRequestStockDtl> stockDetails = new java.util.ArrayList<>();
        if (requestDto.getStockDetails() != null) {
            long stockRowId = 1;
            for (var stockDto : requestDto.getStockDetails()) {
                stockDto.setDetRowId(stockRowId++);
                stockDetails.add(ApPaymentRequestMapper.toStockDtlEntity(transactionPoid, stockDto));
            }
            stockDtlRepository.saveAll(stockDetails);
        }

        String docId = UserContext.getDocumentId();
        String key = transactionPoid.toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        return ApPaymentRequestMapper.toResponse(hdr, details, stockDetails);
    }

    @Override
    public ApPaymentRequestHdrResponseDto update(
            Long transactionPoid,
            ApPaymentRequestHdrRequestDto requestDto
    ) {
        ApPaymentRequestHdr hdr = hdrRepository
                .findById(transactionPoid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment request not found","transactionPoid",transactionPoid));

        // Create a copy of the existing entity for logging

        ApPaymentRequestHdr oldEntity = new ApPaymentRequestHdr();
        BeanUtils.copyProperties(hdr, oldEntity);

        hdr.setDocRef(requestDto.getDocRef());
        hdr.setRefType(requestDto.getRefType());
        hdr.setCurrencyCode(requestDto.getCurrencyCode());
        hdr.setCurrencyRate(requestDto.getCurrencyRate());
        hdr.setPayeePoid(requestDto.getPayeePoid());
        hdr.setRequestedBy(requestDto.getRequestedBy());
        hdr.setRemarks(requestDto.getRemarks());
        hdr.setAccResponse(requestDto.getAccResponse());
        hdr.setAccResponseCategory(requestDto.getAccResponseCategory());
        hdr.setTotalAmount(requestDto.getTotalAmount());
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());


        ApPaymentRequestHdr savedEntity = hdrRepository.save(hdr);

        // Process details based on actionType
        processDetails(transactionPoid, requestDto.getDetails());
        processStockDetails(transactionPoid, requestDto.getStockDetails());

        // Log the update
        String key = savedEntity.getTransactionPoid().toString();
        loggingService.logChanges(oldEntity, savedEntity, ApPaymentRequestHdr.class,
                UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        List<ApPaymentRequestDtl> details = dtlRepository.findByIdTransactionPoid(transactionPoid);
        List<ApPaymentRequestStockDtl> stockDetails = stockDtlRepository.findByIdTransactionPoid(transactionPoid);
        return ApPaymentRequestMapper.toResponse(hdr, details, stockDetails);
    }

    @Override
    @Transactional
    public ApPaymentRequestHdrResponseDto findById(Long transactionPoid) {

        ApPaymentRequestHdr hdr = hdrRepository.findById(transactionPoid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment request not found","transactionPoid",transactionPoid));

        List<ApPaymentRequestDtl> details = dtlRepository.findByIdTransactionPoid(transactionPoid);
        List<ApPaymentRequestStockDtl> stockDetails = stockDtlRepository.findByIdTransactionPoid(transactionPoid);

        return ApPaymentRequestMapper.toResponse(hdr, details, stockDetails);
    }

    @Override
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {

        ApPaymentRequestHdr hdr = hdrRepository
                .findById(transactionPoid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment request not found","transactionPoid",transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "AP_PAYMENT_REQUEST_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                hdr.getTransactionDate()
        );

    }

    @Override
    public Map<String, Object> listPaymentRequest(String documentId, FilterRequestDto request, Pageable pageable, LocalDate periodFrom, LocalDate periodTo) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request,"TRANSACTION_DATE", periodFrom, periodTo);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "DOC_REF",
                "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public Map<String, Object> createFromPo(String poPoid) {

        return aapPaymentRequestCustomRepository.createFromPo(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                poPoid
        );
    }

    @Override
    public Map<String, Object> createFromFf(String ffPoid) {

        return aapPaymentRequestCustomRepository.createFromFf(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                ffPoid
        );
    }

    @Override
    public Map<String, Object> createFromFda(String fdaPoid) {

        return aapPaymentRequestCustomRepository.createFromFda(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                fdaPoid
        );
    }

    @Override
    public Map<String, Object> createFromMta(String poPoid) {

        return aapPaymentRequestCustomRepository.createFromMta(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                poPoid
        );

    }
    
    private void processStockDetails(Long transactionPoid, List<com.asg.finance.dto.ApPaymentRequestStockDtlRequest> stockDetails) {
        if (stockDetails == null || stockDetails.isEmpty()) return;

        String docId = UserContext.getDocumentId();
        String key = transactionPoid.toString();
        List<LogRequestDto<ApPaymentRequestStockDtl>> logRequests = new java.util.ArrayList<>();

        Long maxDetRowId = stockDtlRepository.findByIdTransactionPoid(transactionPoid).stream()
                .mapToLong(d -> d.getId().getDetRowId())
                .max().orElse(0L);

        for (com.asg.finance.dto.ApPaymentRequestStockDtlRequest stockDetail : stockDetails) {
            String actionType = stockDetail.getActionType() != null ? stockDetail.getActionType().toUpperCase() : "ISCREATED";
            switch (actionType) {
                case "ISCREATED" -> {
                    stockDetail.setDetRowId(++maxDetRowId);
                    ApPaymentRequestStockDtl saved = stockDtlRepository.save(ApPaymentRequestMapper.toStockDtlEntity(transactionPoid, stockDetail));
                    loggingService.createLogSummaryEntry(docId, key, String.format("Row Created on AP Payment Request Stock Detail with detRowId: %s", saved.getId().getDetRowId()));
                }
                case "ISUPDATED" -> {
                    ApPaymentRequestStockDtl existing = stockDtlRepository
                            .findByIdTransactionPoidAndIdDetRowId(transactionPoid, stockDetail.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("AP Payment Request Stock Detail", "detRowId", stockDetail.getDetRowId()));
                    ApPaymentRequestStockDtl oldStock = new ApPaymentRequestStockDtl();
                    BeanUtils.copyProperties(existing, oldStock);
                    existing.setStockPoid(stockDetail.getStockPoid());
                    existing.setQuantity(stockDetail.getQuantity());
                    existing.setPrice(stockDetail.getPrice());
                    existing.setDiscount(stockDetail.getDiscount());
                    existing.setBaseAmount(stockDetail.getBaseAmount());
                    existing.setTaxPoid(stockDetail.getTaxPoid());
                    existing.setTaxPercent(stockDetail.getTaxPercent());
                    existing.setTaxAmount(stockDetail.getTaxAmount());
                    existing.setNetSales(stockDetail.getNetSales());
                    stockDtlRepository.save(existing);
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, stockDetail.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldStock, existing, ApPaymentRequestStockDtl.class, docId, key, logDetail));
                }
                case "ISDELETED" -> stockDtlRepository
                        .findByIdTransactionPoidAndIdDetRowId(transactionPoid, stockDetail.getDetRowId())
                        .ifPresent(entity -> {
                            stockDtlRepository.delete(entity);
                            loggingService.logDelete(stockDetail, docId, key);
                        });
                case "NOCHANGE" -> {
                    ApPaymentRequestStockDtl existing = stockDtlRepository
                            .findByIdTransactionPoidAndIdDetRowId(transactionPoid, stockDetail.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("AP Payment Request Stock Detail", "detRowId", stockDetail.getDetRowId()));
                    stockDtlRepository.save(existing);
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void processDetails(Long transactionPoid, List<com.asg.finance.dto.ApPaymentRequestDtlRequestDto> details) {
        List<LogRequestDto<ApPaymentRequestDtl>> logRequests = new java.util.ArrayList<>();
        String docId = UserContext.getDocumentId();
        
        // Auto-generate detRowId for new records
        Long maxDetRowId = dtlRepository.findByIdTransactionPoid(transactionPoid).stream()
                .mapToLong(d -> d.getId().getDetRowId())
                .max().orElse(0L);
        
        for (com.asg.finance.dto.ApPaymentRequestDtlRequestDto detail : details) {
            String actionType = detail.getActionType() != null ? detail.getActionType().toUpperCase() : "ISCREATED";
            
            switch (actionType) {
                case "ISCREATED" -> {
                    // Auto-generate detRowId for new records
                    detail.setDetRowId(++maxDetRowId);
                    ApPaymentRequestDtl entity = ApPaymentRequestMapper.toDtlEntity(transactionPoid, detail);
                    dtlRepository.save(entity);
                    String logDetail = String.format("Row Created on AP Payment Request Detail with detRowId: %s", entity.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
                }
                case "ISUPDATED" -> {
                    ApPaymentRequestDtl existing = dtlRepository.findByIdTransactionPoidAndIdDetRowId(transactionPoid, detail.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("AP Payment Request Detail", "detRowId", detail.getDetRowId()));
                    
                    ApPaymentRequestDtl oldEntity = new ApPaymentRequestDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    
                    existing.setChargePoid(detail.getChargePoid());
                    existing.setAmount(detail.getAmount());
                    existing.setVatPer(detail.getVatPer());
                    existing.setVatAmount(detail.getVatAmount());
                    existing.setTotalAmount(detail.getTotalAmount());
                    existing.setRemarks(detail.getRemarks());
                    dtlRepository.save(existing);
                    
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detail.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ApPaymentRequestDtl.class, docId, transactionPoid.toString(), logDetailForUpdate));
                }
                case "ISDELETED" -> {
                    dtlRepository.findByIdTransactionPoidAndIdDetRowId(transactionPoid, detail.getDetRowId())
                            .ifPresent(entity -> {
                                dtlRepository.delete(entity);
                                loggingService.logDelete(detail, docId, transactionPoid.toString());
                            });
                }
                case "NOCHANGE" -> {
                    ApPaymentRequestDtl existing = dtlRepository.findByIdTransactionPoidAndIdDetRowId(transactionPoid, detail.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("AP Payment Request Detail", "detRowId", detail.getDetRowId()));
                    dtlRepository.save(existing);
                }
            }
        }
        
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }


}
