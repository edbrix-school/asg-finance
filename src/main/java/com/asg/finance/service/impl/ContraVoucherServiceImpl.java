package com.asg.finance.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.ContraVoucherDetailRequest;
import com.asg.finance.dto.ContraVoucherRequest;
import com.asg.finance.dto.ContraVoucherDetailResponse;
import com.asg.finance.dto.ContraVoucherFullResponse;
import com.asg.finance.dto.ContraVoucherResponse;
import com.asg.finance.dto.BillwiseBreakupPopupRequestDto;
import com.asg.finance.dto.CostCenterBreakupResponseDto;
import com.asg.finance.dto.CostCenterBreakupPopupRequestDto;
import com.asg.finance.dto.CostCenterBreakupRequestDto;
import com.asg.finance.dto.GlVoucherCostCenterBreakupResponseDto;
import com.asg.finance.dto.PoidDetailsDto;
import com.asg.finance.entity.GlContraVoucherDtl;
import com.asg.finance.entity.GlContraVoucherHdr;
import com.asg.finance.repository.GlContraVoucherDtlRepository;
import com.asg.finance.repository.GlContraVoucherHdrRepository;
import com.asg.common.lib.security.util.UserContext;

import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.ContraVoucherService;
import com.asg.finance.service.CostCenterBreakupService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContraVoucherServiceImpl implements ContraVoucherService {
    private static final String DEFAULT_DOC_ID = "400-103";
    private static final long DEFAULT_CONTEXT_POID = 1L;
    private static final String BILLWISE_TYPE_DR = "DR";
    private static final String BILLWISE_TYPE_CR = "CR";
    private static final String DEFAULT_COST_GROUP_LOV = "COST_CENTRE";

    private final GlContraVoucherHdrRepository hdrRepository;
    private final GlContraVoucherDtlRepository dtlRepository;
    private final GLMasterRepository glMasterRepository;
    private final DataSource dataSource;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final LovDataService lovService;
    private final PrintService printService;
    private final LoggingService loggingService;
    private final EntityManager entityManager;
    private final BillwiseBreakupService billwiseBreakupService;
    private final CostCenterBreakupService costCenterBreakupService;

    @Override
    public Map<String, Object> listContraVouchers(String docId, FilterRequestDto request, Pageable pageable, LocalDate periodFrom, LocalDate periodTo) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        
        FilterRequestDto safeRequest = request;
        if (request != null && request.filters() == null) {
            safeRequest = new FilterRequestDto(
                request.operator(),
                request.isDeleted(),
                new ArrayList<>()
            );
        }
        
        List<FilterDto> filters = documentService.resolveDateFilters(safeRequest, "TRANSACTION_DATE", periodFrom, periodTo);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "TRANSACTION_DATE",
                "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private boolean applyFilters(GlContraVoucherHdr header, List<FilterDto> filters, String operator) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        List<Boolean> results = new ArrayList<>();
        
        for (FilterDto filter : filters) {
            String searchField = filter.searchField() != null ? filter.searchField().toUpperCase() : "";
            String searchValue = filter.searchValue();
            
            if (searchValue == null || searchValue.trim().isEmpty()) {
                continue;
            }

            boolean matches = false;
            
            switch (searchField) {
                case "DOCREF":
                case "DOC_REF":
                    matches = header.getDocRef() != null && 
                             header.getDocRef().toUpperCase().contains(searchValue.toUpperCase());
                    break;
                    
                case "NARRATION":
                case "POSTING_NARRATION":
                    matches = header.getPostingNarration() != null && 
                             header.getPostingNarration().toUpperCase().contains(searchValue.toUpperCase());
                    break;
                    
                case "COMPANY_ID":
                case "COMPANY_POID":
                    try {
                        Long companyId = Long.parseLong(searchValue);
                        matches = header.getCompanyPoid() != null && header.getCompanyPoid().equals(companyId);
                    } catch (NumberFormatException e) {
                        matches = false;
                    }
                    break;
                    
                case "CREDIT_GL":
                    try {
                        Long creditGl = Long.parseLong(searchValue);
                        matches = header.getCreditGl() != null && header.getCreditGl().equals(creditGl);
                    } catch (NumberFormatException e) {
                        matches = false;
                    }
                    break;
                    
                case "DEBIT_GL":
                    try {
                        Long debitGl = Long.parseLong(searchValue);
                        matches = header.getDebitGl() != null && header.getDebitGl().equals(debitGl);
                    } catch (NumberFormatException e) {
                        matches = false;
                    }
                    break;
                    
                case "CURRENCY_CODE":
                case "CURRENCY":
                    matches = header.getCurrencyCode() != null && 
                             header.getCurrencyCode().equalsIgnoreCase(searchValue);
                    break;
                    
                case "GLOBALSEARCH":
                    // Search across multiple fields
                    String searchUpper = searchValue.toUpperCase();
                    matches = (header.getDocRef() != null && header.getDocRef().toUpperCase().contains(searchUpper)) ||
                             (header.getPostingNarration() != null && header.getPostingNarration().toUpperCase().contains(searchUpper)) ||
                             (header.getCurrencyCode() != null && header.getCurrencyCode().toUpperCase().contains(searchUpper)) ||
                             (header.getCreatedBy() != null && header.getCreatedBy().toUpperCase().contains(searchUpper));
                    break;
                    
                default:
                    matches = false;
                    break;
            }
            
            results.add(matches);
        }
        
        if (results.isEmpty()) {
            return true;
        }
        
        // Apply operator
        if ("OR".equalsIgnoreCase(operator)) {
            return results.stream().anyMatch(b -> b);
        } else {
            // AND (default)
            return results.stream().allMatch(b -> b);
        }
    }

    @Override
    @Transactional
    @PerformGlPosting
    public ContraVoucherFullResponse createContraVoucher(ContraVoucherRequest request) {
        log.info("createContraVoucher started");

        // Create header
        GlContraVoucherHdr header = new GlContraVoucherHdr();
        header.setCreditGl(request.getCreditGl());
        header.setDebitGl(request.getDebitGl());
        header.setCurrencyCode(request.getCurrencyCode());
        header.setCurrencyRate(request.getCurrencyRate());
        header.setAmount(request.getAmount());
        header.setBhdAmount(request.getBhdAmount());
        header.setPostingNarration(request.getPostingNarration());
        header.setChequeNo(request.getChequeNo());
        header.setManual(request.getManual());
        header.setChequeDate(request.getChequeDate());
        header.setMultiCompany(request.getMultiCompany());
        
        // Auto-generate docRef if not provided
        header.setGroupPoid(request.getGroupPoid() != null ? request.getGroupPoid() : ASGHelperUtils.getGroupId());
        if (request.getDocRef() == null || request.getDocRef().trim().isEmpty()) {
            Long maxDocRefNum = hdrRepository.findMaxDocRefNumber(header.getGroupPoid());
            Long nextDocRefNum = (maxDocRefNum != null ? maxDocRefNum : 10000L) + 1;
            header.setDocRef("ASG" + nextDocRefNum);
            log.info("Auto-generated docRef: {}", header.getDocRef());
        } else {
            header.setDocRef(request.getDocRef());
        }
        
        // Set transactionDate to LocalDate.now()
        header.setTransactionDate(DateUtil.getCurrentDateInUserTimeZone());
        header.setDeleted(request.getDeleted() != null ? request.getDeleted() : "N");
        header.setOldJvno(request.getOldJvno());
        header.setRemarks(request.getRemarks());
        header.setCompanyPoid(request.getCompanyPoid());
        header.setDrTotal(request.getDrTotal());
        header.setCrTotal(request.getCrTotal());

        GlContraVoucherHdr savedHeader = hdrRepository.save(header);
        entityManager.flush();
        entityManager.refresh(header);

        // Log the creation
        String key = savedHeader.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), key, String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedHeader.getDocRef()));

        // Process details from request
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            Long nextDetRowId = 1L;
            for (ContraVoucherDetailRequest detailRequest : request.getDetails()) {
                if ("isCreated".equalsIgnoreCase(detailRequest.getActionType())) {
                    GlContraVoucherDtl detail = new GlContraVoucherDtl();
                    detail.setTransactionPoid(savedHeader.getTransactionPoid());
                    // Always auto-generate sequential detRowId starting from 1 during create
                    detail.setDetRowId(nextDetRowId++);
                    detail.setType(detailRequest.getType());
                    detail.setCompanyPoid(detailRequest.getCompanyPoid() != null ? detailRequest.getCompanyPoid() : savedHeader.getCompanyPoid());
                    detail.setGlPoid(detailRequest.getGlPoid());
                    detail.setDrAmt(detailRequest.getDrAmt());
                    detail.setCrAmt(detailRequest.getCrAmt());
                    detail.setRemarks(detailRequest.getRemarks());
                    dtlRepository.save(detail);
                    detailRequest.setDetRowId(detail.getDetRowId());
                    
                    // Log child record creation
                    String logDetail = String.format("Row Created on Contra Voucher Detail with detRowId: %s", detail.getDetRowId());
                    loggingService.createLogSummaryEntry(UserContext.getDocumentId(), savedHeader.getTransactionPoid().toString(), logDetail);
                }
            }
            saveBillwiseForDetails(savedHeader.getTransactionPoid(), request.getDetails(), false);
            saveCostCenterForDetails(savedHeader.getTransactionPoid(), request.getDetails(), false);
        }

        log.info("createContraVoucher completed for transactionPoid={}", savedHeader.getTransactionPoid());
        return convertToFullResponse(savedHeader);
    }

    @Override
    @Transactional
    public ContraVoucherFullResponse updateContraVoucher(ContraVoucherRequest request) {
        log.info("updateContraVoucher started for transactionPoid={}", request.getTransactionPoid());

        if (request.getTransactionPoid() == null) {
            throw new IllegalArgumentException("transactionPoid is required for update operation");
        }

        // Update header
        GlContraVoucherHdr header = hdrRepository.findByTransactionPoid(request.getTransactionPoid())
                .orElseThrow(() -> new RuntimeException("Contra voucher not found with transactionPoid: " + request.getTransactionPoid()));

        // Create a copy of the existing entity for logging
        GlContraVoucherHdr oldEntity = new GlContraVoucherHdr();
        BeanUtils.copyProperties(header, oldEntity);

        header.setCreditGl(request.getCreditGl());
        header.setDebitGl(request.getDebitGl());
        header.setCurrencyCode(request.getCurrencyCode());
        header.setCurrencyRate(request.getCurrencyRate());
        header.setAmount(request.getAmount());
        header.setBhdAmount(request.getBhdAmount());
        header.setPostingNarration(request.getPostingNarration());
        header.setChequeNo(request.getChequeNo());
        header.setManual(request.getManual());
        header.setChequeDate(request.getChequeDate());
        header.setMultiCompany(request.getMultiCompany());
        header.setDocRef(request.getDocRef());
        if (request.getTransactionDate() != null) {
            header.setTransactionDate(request.getTransactionDate());
        }
        if (request.getDeleted() != null) {
            header.setDeleted(request.getDeleted());
        }
        header.setOldJvno(request.getOldJvno());
        header.setRemarks(request.getRemarks());
        header.setCompanyPoid(request.getCompanyPoid());
        header.setDrTotal(request.getDrTotal());
        header.setCrTotal(request.getCrTotal());

        GlContraVoucherHdr savedHeader = hdrRepository.save(header);

        // Log the update
        String key = savedHeader.getTransactionPoid().toString();
        loggingService.logChanges(oldEntity, savedHeader, GlContraVoucherHdr.class, 
                UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        // Process details based on actionType
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            List<GlContraVoucherDtl> existingDetails = dtlRepository.findByTransactionPoid(savedHeader.getTransactionPoid());
            Long maxDetRowId = existingDetails.stream()
                    .map(GlContraVoucherDtl::getDetRowId)
                    .max(Long::compareTo)
                    .orElse(0L);

            Long nextDetRowId = maxDetRowId + 1;
            List<LogRequestDto<GlContraVoucherDtl>> logRequests = new ArrayList<>();

            for (ContraVoucherDetailRequest detailRequest : request.getDetails()) {
                String actionType = detailRequest.getActionType();
                
                if ("isCreated".equalsIgnoreCase(actionType)) {
                    // Create new detail - always auto-generate sequential detRowId starting from next available
                    GlContraVoucherDtl detail = new GlContraVoucherDtl();
                    detail.setTransactionPoid(savedHeader.getTransactionPoid());
                    detail.setDetRowId(nextDetRowId++);
                    detail.setType(detailRequest.getType());
                    detail.setCompanyPoid(detailRequest.getCompanyPoid() != null ? detailRequest.getCompanyPoid() : savedHeader.getCompanyPoid());
                    detail.setGlPoid(detailRequest.getGlPoid());
                    detail.setDrAmt(detailRequest.getDrAmt());
                    detail.setCrAmt(detailRequest.getCrAmt());
                    detail.setRemarks(detailRequest.getRemarks());
                    dtlRepository.save(detail);
                    detailRequest.setDetRowId(detail.getDetRowId());
                    
                    // Log child record creation
                    String logDetail = String.format("Row Created on Contra Voucher Detail with detRowId: %s", detail.getDetRowId());
                    loggingService.createLogSummaryEntry(UserContext.getDocumentId(), savedHeader.getTransactionPoid().toString(), logDetail);
                    
                } else if ("isUpdated".equalsIgnoreCase(actionType)) {
                    // Update existing detail with request data
                    if (detailRequest.getDetRowId() == null) {
                        throw new IllegalArgumentException("detRowId is required for update operation");
                    }
                    GlContraVoucherDtl detail = dtlRepository
                            .findByTransactionPoidAndDetRowId(savedHeader.getTransactionPoid(), detailRequest.getDetRowId())
                            .orElseThrow(() -> new RuntimeException("Detail not found with detRowId: " + detailRequest.getDetRowId()));
                    
                    // Create a copy of the existing detail for logging
                    GlContraVoucherDtl oldDetail = new GlContraVoucherDtl();
                    BeanUtils.copyProperties(detail, oldDetail);
                    
                    // Update with request values
                    if (detailRequest.getType() != null) {
                        detail.setType(detailRequest.getType());
                    }
                    if (detailRequest.getCompanyPoid() != null) {
                        detail.setCompanyPoid(detailRequest.getCompanyPoid());
                    }
                    if (detailRequest.getGlPoid() != null) {
                        detail.setGlPoid(detailRequest.getGlPoid());
                    }
                    if (detailRequest.getDrAmt() != null) {
                        detail.setDrAmt(detailRequest.getDrAmt());
                    }
                    if (detailRequest.getCrAmt() != null) {
                        detail.setCrAmt(detailRequest.getCrAmt());
                    }
                    if (detailRequest.getRemarks() != null) {
                        detail.setRemarks(detailRequest.getRemarks());
                    }
                    dtlRepository.save(detail);
                    
                    // Collect log request for batch processing
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", detail.getTransactionPoid(), detail.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldDetail, detail, GlContraVoucherDtl.class, 
                            UserContext.getDocumentId(), savedHeader.getTransactionPoid().toString(), logDetailForUpdate));
                    
                } else if ("isDeleted".equalsIgnoreCase(actionType)) {
                    // Delete detail
                    if (detailRequest.getDetRowId() == null) {
                        throw new IllegalArgumentException("detRowId is required for delete operation");
                    }
                    dtlRepository.findByTransactionPoidAndDetRowId(savedHeader.getTransactionPoid(), detailRequest.getDetRowId())
                            .ifPresent(detail -> {
                                dtlRepository.delete(detail);
                                // Log child record deletion
                                loggingService.logDelete(detailRequest, UserContext.getDocumentId(), savedHeader.getTransactionPoid().toString());
                            });
                }
                // noChange/noChanges - do nothing
            }
            
            // Batch process all update logs
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }

            List<ContraVoucherDetailRequest> activeDetails = request.getDetails().stream()
                    .filter(detail -> detail != null && detail.getDetRowId() != null)
                    .filter(detail -> !"isDeleted".equalsIgnoreCase(detail.getActionType()))
                    .toList();
            saveBillwiseForDetails(savedHeader.getTransactionPoid(), activeDetails, true);
            saveCostCenterForDetails(savedHeader.getTransactionPoid(), activeDetails, true);
        }

        log.info("updateContraVoucher completed for transactionPoid={}", savedHeader.getTransactionPoid());
        return convertToFullResponse(savedHeader);
    }

    @Override
    @Transactional(readOnly = true)
    public ContraVoucherFullResponse getContraVoucherById(Long transactionPoid) {
        log.info("getContraVoucherById started for transactionPoid={}", transactionPoid);

        GlContraVoucherHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Contra voucher not found with transactionPoid: " + transactionPoid));

        log.info("getContraVoucherById completed for transactionPoid={}", transactionPoid);
        return convertToFullResponse(header);
    }

    @Override
    @Transactional
    public void deleteContraVoucher(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("deleteContraVoucher started for transactionPoid={}", transactionPoid);

        GlContraVoucherHdr existing = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Contra voucher not found with transactionPoid: " + transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_CONTRA_VOUCHER_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                existing.getTransactionDate()
        );

        log.info("deleteContraVoucher completed for transactionPoid={}", transactionPoid);
    }

    @Override
    public String checkGlNature(Long creditGlId) {
        log.info("checkGlNature started for creditGlId={}", creditGlId);

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        String sqlProc = "BEGIN PROC_GL_CONTRA_CHK_GL_NATURE(?,?,?,?,?); END;";
        
        CallableStatement statement = null;
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            statement = conn.prepareCall(sqlProc);

            statement.setLong(1, groupPoid);
            statement.setLong(2, companyPoid);
            statement.setLong(3, userPoid);
            statement.setLong(4, creditGlId);
            statement.registerOutParameter(5, Types.VARCHAR);

            statement.executeUpdate();

            String result = statement.getString(5);
            log.info("PROC_GL_CONTRA_CHK_GL_NATURE returned: {} for creditGlId={}", result, creditGlId);

            String finalResult = result != null ? result : "FALSE";
            log.info("checkGlNature completed for creditGlId={}, result={}", creditGlId, finalResult);
            return finalResult;

        } catch (SQLException e) {
            log.error("Error calling PROC_GL_CONTRA_CHK_GL_NATURE for creditGlId={}", creditGlId, e);
            throw new RuntimeException("Error checking GL nature: " + e.getMessage(), e);
        } finally {
            try {
                if (statement != null) statement.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                log.error("Error closing connection/statement", e);
            }
        }
    }


    private ContraVoucherResponse convertToResponse(GlContraVoucherHdr header) {
        ContraVoucherResponse response = new ContraVoucherResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setTransactionDate(header.getTransactionDate());
        response.setDocRef(header.getDocRef());
        response.setPostingNarration(header.getPostingNarration());
        response.setCreatedBy(header.getCreatedBy());
        response.setDeleted(header.getDeleted());
        response.setCreatedDate(header.getCreatedDate());
        return response;
    }

    private ContraVoucherFullResponse convertToFullResponse(GlContraVoucherHdr header) {
        ContraVoucherFullResponse response = new ContraVoucherFullResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setTransactionDate(header.getTransactionDate());
        response.setDocRef(header.getDocRef());
        response.setPostingNarration(header.getPostingNarration());
        response.setDeleted(header.getDeleted());
        response.setCompanyPoid(header.getCompanyPoid());
        response.setCurrencyCode(header.getCurrencyCode());
        response.setCurrencyRate(header.getCurrencyRate());
        response.setAmount(scaleToThreeDecimals(header.getAmount()));
        response.setBhdAmount(scaleToThreeDecimals(header.getBhdAmount()));
        response.setDrTotal(scaleToThreeDecimals(header.getDrTotal()));
        response.setCrTotal(scaleToThreeDecimals(header.getCrTotal()));
        response.setApprovalStatus(null); // ApprovalStatus column doesn't exist in database
        response.setCreatedBy(header.getCreatedBy());
        response.setCreatedDate(header.getCreatedDate());
        response.setCreditGl(header.getCreditGl());
        response.setCreditGlDetails(getGlDetails(header.getCreditGl()));
        response.setDebitGl(header.getDebitGl());
        response.setDebitGlDetails(getGlDetails(header.getDebitGl()));
        response.setCompanyPoidDetails(getCompanyDetails(header.getCompanyPoid()));
        response.setChequeNo(header.getChequeNo());
        response.setManual(header.getManual());
        response.setChequeDate(header.getChequeDate());
        response.setMultiCompany(header.getMultiCompany());
        response.setRemarks(header.getRemarks());

        // Fetch and convert details
        List<GlContraVoucherDtl> details = dtlRepository.findByTransactionPoid(header.getTransactionPoid());
        if (details != null && !details.isEmpty()) {
            List<ContraVoucherDetailResponse> detailResponses = details.stream()
                    .map(this::convertDetailToResponse)
                    .collect(Collectors.toList());
            response.setDetails(detailResponses);
        }

        return response;
    }

    private ContraVoucherDetailResponse convertDetailToResponse(GlContraVoucherDtl detail) {
        ContraVoucherDetailResponse response = new ContraVoucherDetailResponse();
        response.setDetRowId(detail.getDetRowId());
        response.setType(detail.getType());
        response.setCompanyPoid(detail.getCompanyPoid());
        response.setCompanyPoidDetails(getCompanyDetails(detail.getCompanyPoid()));
        response.setGlPoid(detail.getGlPoid());
        response.setGlPoidDetails(getGlDetails(detail.getGlPoid()));
        response.setDrAmt(scaleToThreeDecimals(detail.getDrAmt()));
        response.setCrAmt(scaleToThreeDecimals(detail.getCrAmt()));
        response.setRemarks(detail.getRemarks());
        response.setBreakupList(loadBillwisePopupList(detail.getTransactionPoid(), detail.getDetRowId()));
        response.setCostCenterList(loadCostCenterPopupList(detail.getTransactionPoid(), detail.getDetRowId()));
        return response;
    }

    private void saveBillwiseForDetails(Long transactionPoid,
                                        List<ContraVoucherDetailRequest> details,
                                        boolean isUpdate) {
        if (details == null || details.isEmpty()) {
            return;
        }

        String docId = getCurrentDocId();
        Long groupPoid = getCurrentGroupPoid();
        Long companyPoid = getCurrentCompanyPoid();
        Long userPoid = getCurrentUserPoid();
        List<BillwiseBreakupRequestDto> breakupRequests = new ArrayList<>();

        for (ContraVoucherDetailRequest detail : details) {
            if (detail == null || detail.getDetRowId() == null || detail.getBreakupList() == null) {
                continue;
            }
            for (BillwiseBreakupPopupRequestDto popup : detail.getBreakupList()) {
                if (popup == null) {
                    continue;
                }
                BillwiseBreakupRequestDto req = new BillwiseBreakupRequestDto();
                req.setGroupPoid(groupPoid);
                req.setCompanyPoid(companyPoid);
                req.setDocId(docId);
                req.setTransactionPoid(transactionPoid);
                if (popup.getBillDetRowId() == null) {
                    throw new IllegalArgumentException("billDetRowId is required in breakupList");
                }
                req.setBillDetRowId(popup.getBillDetRowId());
                req.setBillRefType(popup.getBillRefType());
                req.setBillRef(popup.getBillRef());
                req.setBillDueDate(popup.getBillDueDate());
                req.setBillOriginalAmount(popup.getBillOriginalAmount());
                if (BILLWISE_TYPE_DR.equalsIgnoreCase(popup.getType())) {
                    req.setDrAmt(popup.getAmount());
                    req.setCrAmt(BigDecimal.ZERO);
                } else {
                    req.setDrAmt(BigDecimal.ZERO);
                    req.setCrAmt(popup.getAmount());
                }
                req.setBillRemarks(popup.getBillRemarks());
                req.setLoginUserPoid(userPoid);
                req.setMainDetRowId(detail.getDetRowId());
                req.setGlCompanyPoid(detail.getCompanyPoid() != null ? detail.getCompanyPoid() : companyPoid);
                req.setGlPoid(detail.getGlPoid());
                breakupRequests.add(req);
            }
        }

        if (breakupRequests.isEmpty()) {
            return;
        }

        if (isUpdate) {
            billwiseBreakupService.updateBillwiseBreakups(breakupRequests, userPoid);
        } else {
            billwiseBreakupService.insertBillwiseBreakup(breakupRequests);
        }
    }

    private void saveCostCenterForDetails(Long transactionPoid,
                                          List<ContraVoucherDetailRequest> details,
                                          boolean isUpdate) {
        if (details == null || details.isEmpty()) {
            return;
        }

        String docId = getCurrentDocId();
        Long groupPoid = getCurrentGroupPoid();
        Long companyPoid = getCurrentCompanyPoid();
        Long userPoid = getCurrentUserPoid();
        List<CostCenterBreakupRequestDto> costCenterRequests = new ArrayList<>();

        for (ContraVoucherDetailRequest detail : details) {
            if (detail == null || detail.getDetRowId() == null || detail.getCostCenterList() == null) {
                continue;
            }
            for (CostCenterBreakupPopupRequestDto popup : detail.getCostCenterList()) {
                if (popup == null) {
                    continue;
                }
                CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
                dto.setGroupPoid(groupPoid);
                dto.setCompanyPoid(companyPoid);
                dto.setDocId(docId);
                dto.setTransactionPoid(transactionPoid);
                dto.setMainDetRowId(detail.getDetRowId());
                dto.setGlPoid(detail.getGlPoid());
                if (popup.getCostDetRowId() == null) {
                    throw new IllegalArgumentException("costDetRowId is required in costCenterList");
                }
                dto.setCostDetRowId(popup.getCostDetRowId());
                dto.setCostGroup(popup.getCostGroup());
                dto.setCostPoid(popup.getCostPoid());
                dto.setAmount(popup.getAmount());
                dto.setLoginUserPoid(userPoid);
                costCenterRequests.add(dto);
            }
        }

        if (costCenterRequests.isEmpty()) {
            return;
        }

        if (isUpdate) {
            costCenterBreakupService.updateCostCenterBreakups(costCenterRequests, userPoid);
        } else {
            costCenterBreakupService.saveCostCenterBreakups(costCenterRequests);
        }
    }

    private List<BillwiseBreakupPopupRequestDto> loadBillwisePopupList(Long transactionPoid, Long detRowId) {
        GlVoucherLoadBillwiseBreakupResponseDto response = billwiseBreakupService.loadBillwiseBreakup(
                getCurrentGroupPoid(),
                getCurrentCompanyPoid(),
                getCurrentDocId(),
                transactionPoid
        );
        if (response == null || response.getLoadBillwiseBreakupResponseDtoList() == null) {
            return Collections.emptyList();
        }
        return response.getLoadBillwiseBreakupResponseDtoList().stream()
                .filter(b -> detRowId.equals(b.getMainDetRowId()))
                .map(this::mapBillwisePopup)
                .toList();
    }

    private BillwiseBreakupPopupRequestDto mapBillwisePopup(
            com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto billwiseDto) {
        BillwiseBreakupPopupRequestDto popup = new BillwiseBreakupPopupRequestDto();
        popup.setBillDetRowId(billwiseDto.getBillDetRowId());
        popup.setBillRefType(billwiseDto.getBillRefType());
        popup.setBillRef(billwiseDto.getBillRef());
        popup.setBillDueDate(billwiseDto.getBillDueDate());
        popup.setBillOriginalAmount(billwiseDto.getBillOriginalAmount() != null
                ? billwiseDto.getBillOriginalAmount()
                : (billwiseDto.getDrAmt() != null && billwiseDto.getDrAmt().compareTo(BigDecimal.ZERO) > 0
                ? billwiseDto.getDrAmt()
                : billwiseDto.getCrAmt()));
        BigDecimal drAmt = billwiseDto.getDrAmt() != null ? billwiseDto.getDrAmt() : BigDecimal.ZERO;
        BigDecimal crAmt = billwiseDto.getCrAmt() != null ? billwiseDto.getCrAmt() : BigDecimal.ZERO;
        popup.setType(drAmt.compareTo(BigDecimal.ZERO) > 0 ? BILLWISE_TYPE_DR : BILLWISE_TYPE_CR);
        popup.setAmount(drAmt.compareTo(BigDecimal.ZERO) > 0 ? drAmt : crAmt);
        popup.setBillRemarks(billwiseDto.getBillRemarks());
        return popup;
    }

    private List<CostCenterBreakupPopupRequestDto> loadCostCenterPopupList(Long transactionPoid, Long detRowId) {
        GlVoucherCostCenterBreakupResponseDto response = costCenterBreakupService.loadCostCenterData(
                getCurrentDocId(),
                transactionPoid,
                getCurrentGroupPoid(),
                getCurrentCompanyPoid(),
                getCurrentUserPoid()
        );
        if (response == null || response.getCostBreakupList() == null) {
            return Collections.emptyList();
        }
        return response.getCostBreakupList().stream()
                .filter(c -> detRowId.equals(c.getMainDetRowId()))
                .map(this::mapCostCenterPopup)
                .toList();
    }

    private CostCenterBreakupPopupRequestDto mapCostCenterPopup(CostCenterBreakupResponseDto costCenterDto) {
        CostCenterBreakupPopupRequestDto popup = new CostCenterBreakupPopupRequestDto();
        popup.setCostDetRowId(costCenterDto.getCostDetRowId());
        popup.setCostGroup(costCenterDto.getCostGroup());
        popup.setCostPoid(costCenterDto.getCostPoid());
        popup.setAmount(costCenterDto.getAmount());
        if (costCenterDto.getCostPoid() != null && costCenterDto.getCostGroup() != null) {
            popup.setCostCenterDetails(resolveCostCenterDetails(costCenterDto));
        }
        return popup;
    }

    private String normalizeCostGroupLovName(String costGroup) {
        if (costGroup == null) {
            return DEFAULT_COST_GROUP_LOV;
        }
        String normalized = costGroup.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return DEFAULT_COST_GROUP_LOV;
        }
        return getCustomLovNameForCostGroup(normalized);
    }

    
    private String getCustomLovNameForCostGroup(String costGroup) {
        return switch (costGroup) {
            case "GL_SH_BLS" -> "GL_SH_BLS";
            case "GL_FDA_JOBS" -> "GL_FDA_JOBS";
            case "GL_FF_JOBS" -> "GL_FF_JOBS";
            case "GL_FFP_JOBS" -> "GL_FFP_JOBS";
            case "ANOOD_MANSION" -> "ANOOD_MANSION";
            case "NAJOOD_MANSION" -> "NAJOOD_MANSION";
            case "PROPERTIES" -> "DN_PROPERTIES";
            case "GL_COST_CENTRE", "GL_COST_CENTER" -> "GL_COST_CENTRE";
            case "COST_CENTER", "COST_CENTRE", "1", "TELEPHONES" -> DEFAULT_COST_GROUP_LOV;
            case "FIXED_ASSET" -> "FIXED_ASSET";
            case "OASIS" -> "DN_OASIS";
            case "SALESMAN" -> "SALESMAN";
            case "COMPANY" -> "COMPANY";
            case "VOYAGE_COST_GROUP" -> "VOYAGE_COST_GROUP";
            case "EMPLOYEE_NAME" -> "EMPLOYEE_NAME";
            default -> DEFAULT_COST_GROUP_LOV;
        };
    }

    private LovGetListDto resolveCostCenterDetails(CostCenterBreakupResponseDto costCenterDto) {
        String lovName = normalizeCostGroupLovName(costCenterDto.getCostGroup());
        String costPoid = costCenterDto.getCostPoid();

        LovGetListDto details = null;
        try {
            Long numericPoid = Long.parseLong(costPoid);
            details = lovService.getDetailsByPoidAndLovName(numericPoid, lovName);
        } catch (NumberFormatException ignored) {
         
        }

        if (!isLovResolved(details)) {
            details = lovService.getDetailsByCodeAndLovName(costPoid, lovName);
        }

        if (!isLovResolved(details) && !DEFAULT_COST_GROUP_LOV.equalsIgnoreCase(lovName)) {
            try {
                Long numericPoid = Long.parseLong(costPoid);
                details = lovService.getDetailsByPoidAndLovName(numericPoid, DEFAULT_COST_GROUP_LOV);
            } catch (NumberFormatException ignored) {
           
            }
            if (!isLovResolved(details)) {
                details = lovService.getDetailsByCodeAndLovName(costPoid, DEFAULT_COST_GROUP_LOV);
            }
        }

        if (!isLovResolved(details)) {
            LovGetListDto fallback = new LovGetListDto();
            fallback.setPoid(parseLongSafely(costPoid));
            fallback.setCode(costPoid);
            fallback.setDescription(
                    costCenterDto.getDescription() != null ? costCenterDto.getDescription() : costPoid
            );
            return fallback;
        }

        return details;
    }

    private boolean isLovResolved(LovGetListDto details) {
        return details != null
                && ((details.getCode() != null && !details.getCode().isBlank())
                || (details.getDescription() != null && !details.getDescription().isBlank()));
    }

    private Long parseLongSafely(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String getCurrentDocId() {
        return UserContext.getDocumentId() != null ? UserContext.getDocumentId() : DEFAULT_DOC_ID;
    }

    private Long getCurrentGroupPoid() {
        return UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : DEFAULT_CONTEXT_POID;
    }

    private Long getCurrentCompanyPoid() {
        return UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : DEFAULT_CONTEXT_POID;
    }

    private Long getCurrentUserPoid() {
        return UserContext.getUserPoid() != null ? UserContext.getUserPoid() : DEFAULT_CONTEXT_POID;
    }

    /**
     * Fetches GL Master details (POID, CODE, DESCRIPTION) for a given GL POID
     */
    private PoidDetailsDto getGlDetails(Long glPoid) {
        if (glPoid == null) {
            return null;
        }
        try {
            Optional<GLMaster> glMaster = glMasterRepository.findByGlPoid(glPoid);
            if (glMaster.isPresent()) {
                GLMaster gl = glMaster.get();
                PoidDetailsDto details = new PoidDetailsDto();
                details.setPoid(gl.getGlPoid());
                details.setCode(gl.getGlCode());
                details.setDescription(gl.getGlDescription());
                return details;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GL Master details for glPoid={}: {}", glPoid, e.getMessage());
        }
        return null;
    }

    /**
     * Fetches Company details (POID, CODE, DESCRIPTION) for a given Company POID
     */
    private PoidDetailsDto getCompanyDetails(Long companyPoid) {
        if (companyPoid == null) {
            return null;
        }
        try {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovNameFast(companyPoid, "COMPANY");
            if (lovGetListDto != null) {
                PoidDetailsDto details = new PoidDetailsDto();
                details.setPoid(lovGetListDto.getPoid());
                details.setCode(lovGetListDto.getCode());
                details.setDescription(lovGetListDto.getDescription());
                return details;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Company details for companyPoid={}: {}", companyPoid, e.getMessage());
        }
        return null;
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, DEFAULT_DOC_ID);
        params.put("SUB_DETAIL", printService.load("Finance/GL/ContraVoucherReportDtlSubreport1.jrxml"));
        params.put("SUB_FOOTER_ISO", printService.load("Templates/DocFooterSubReport-ISO.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/ContraVoucherReport1.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private BigDecimal scaleToThreeDecimals(BigDecimal value) {
        return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
    }

}
