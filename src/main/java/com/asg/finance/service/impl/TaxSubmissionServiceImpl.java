package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.client.CompanyServiceClient;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.*;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.entity.GlobalTaxSubmissionDtl;
import com.asg.finance.entity.GlobalTaxSubmissionHdr;
import com.asg.finance.repository.GlobalTaxSubmissionDtlRepository;
import com.asg.finance.repository.GlobalTaxSubmissionHdrRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.PeriodValidationHelper;
import com.asg.finance.service.TaxSubmissionAfterSaveRunner;
import com.asg.finance.service.TaxSubmissionService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.service.TaxSubmissionStoredProcedureHelper;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxSubmissionServiceImpl implements TaxSubmissionService {
    private static final String DOC_ID_TAX_SUBMISSION = "400-118";

    private final GlobalTaxSubmissionHdrRepository hdrRepository;
    private final GlobalTaxSubmissionDtlRepository dtlRepository;
    private final TaxSubmissionStoredProcedureHelper storedProcedureHelper;
    private final PeriodValidationHelper periodValidationHelper;
    private final DocumentSearchService documentService;
    private final CompanyServiceClient companyServiceClient;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final EntityManager entityManager;
    private final TaxSubmissionAfterSaveRunner afterSaveRunner;
    private final PrintService printService;
    private final DataSource dataSource;

    @Override
    @Transactional
    public TaxSubmissionResponse createTaxSubmission(CreateTaxSubmissionRequest request) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        String userId = UserContext.getUserId();
        
        log.info("createTaxSubmission started for groupPoid={} companyPoid={} userId={}", groupPoid, companyPoid, userId);

        // Use session company if not provided (read-only)
        Long finalCompanyId = request.getCompanyId() != null ? request.getCompanyId() : companyPoid;
        if (finalCompanyId == null) {
            throw new ValidationException("Company is required");
        }

        // Validate mandatory fields
        if (request.getPeriodFrom() == null) {
            throw new ValidationException("Period From is required");
        }
        if (request.getPeriodTo() == null) {
            throw new ValidationException("Period To is required");
        }

        // Normalize period dates to midnight to match legacy behavior / PL-SQL expectations
        LocalDateTime normalizedPeriodFrom = normalizeToMidnight(request.getPeriodFrom());
        LocalDateTime normalizedPeriodTo = normalizeToMidnight(request.getPeriodTo());

        // Get VAT filing period parameter
        Integer vatFilingPeriod = getVatFilingPeriod(finalCompanyId);

        // Validate period rules
        List<String> periodErrors = periodValidationHelper.validatePeriodRules(
                normalizedPeriodFrom, normalizedPeriodTo, vatFilingPeriod);
        if (!periodErrors.isEmpty()) {
            throw new ValidationException(periodErrors.get(0));
        }

        // Check for overlapping periods (existing submissions)
        List<GlobalTaxSubmissionHdr> overlapping = hdrRepository.findOverlappingPeriods(
                finalCompanyId, groupPoid, normalizedPeriodFrom, normalizedPeriodTo);
        if (!overlapping.isEmpty()) {
            throw new ValidationException("A tax submission already exists for this period");
        }

        // Call before save validation stored procedure
        String beforeSaveStatus = storedProcedureHelper.validateBeforeSave(
                groupPoid, finalCompanyId, resolveProcedureUserId(), normalizedPeriodFrom, normalizedPeriodTo, null);
        if (beforeSaveStatus != null && (beforeSaveStatus.contains("ERROR") || beforeSaveStatus.contains("WARNING"))) {
            throw new ValidationException(beforeSaveStatus);
        }

        // Create header entity
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setCompanyPoid(finalCompanyId);
        header.setPeriodFrom(normalizedPeriodFrom);
        header.setPeriodTo(normalizedPeriodTo);
        header.setRemarks(request.getRemarks());
        header.setDocRef(request.getDocRef()); // Auto-generate if null
        header.setTransactionDate(DateUtil.getCurrentDateTimeInUserTimeZone());
        header.setGroupPoid(groupPoid);
        header.setStatus("DRAFT");
        header.setApprovalStatus("PENDING");
        header.setDeleted("N");

        GlobalTaxSubmissionHdr savedHeader = afterSaveRunner.persistHeader(header);
        savedHeader = afterSaveRunner.runAfterSaveAndReload(
                savedHeader.getTransactionPoid(), groupPoid, finalCompanyId, resolveProcedureUserPoidForAfterSave());

        log.info("createTaxSubmission persisted header transactionPoid={}", savedHeader.getTransactionPoid());

        // Log the creation first
        String key = savedHeader.getTransactionPoid().toString();
        String docId = UserContext.getDocumentId();
        String docRef = savedHeader.getDocRef();
        loggingService.createLogSummaryEntry(docId, key, String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), docRef));

        // Build response
        TaxSubmissionResponse response = buildResponse(savedHeader, new ArrayList<>());

        log.info("createTaxSubmission completed for transactionPoid={}", savedHeader.getTransactionPoid());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxSubmissionResponse getTaxSubmissionById(Long transactionPoid) {
        log.info("getTaxSubmissionById started for transactionPoid={}", transactionPoid);

        // GROUP_POID is NULL in all records, so use findByTransactionPoid without GROUP_POID filter
        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Submission", "transactionPoid", transactionPoid));

        List<GlobalTaxSubmissionDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);

        TaxSubmissionResponse response = buildResponse(header, details);

        log.info("getTaxSubmissionById completed for transactionPoid={}", transactionPoid);
        return response;
    }

    @Override
    @Transactional
    public TaxSubmissionResponse updateTaxSubmission(Long transactionPoid, UpdateTaxSubmissionRequest request) {
        Long groupPoid = UserContext.getGroupPoid();
        String userId = UserContext.getUserId();
        log.info("updateTaxSubmission started for transactionPoid={} groupPoid={} userId={}", transactionPoid, groupPoid, userId);

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Submission", "transactionPoid", transactionPoid));

        // Create a copy of the existing entity for logging
        GlobalTaxSubmissionHdr oldEntity = new GlobalTaxSubmissionHdr();
        BeanUtils.copyProperties(header ,oldEntity);

        if ("APPROVED".equals(header.getApprovalStatus()) || "POSTED".equals(header.getStatus())) {
            throw new ValidationException("Cannot update tax submission that is already approved or posted");
        }

        // Validate mandatory fields
        if (request.getPeriodFrom() == null) {
            throw new ValidationException("Period From is required");
        }
        if (request.getPeriodTo() == null) {
            throw new ValidationException("Period To is required");
        }

        // Normalize period dates to midnight to match legacy behavior / PL-SQL expectations
        LocalDateTime normalizedPeriodFrom = normalizeToMidnight(request.getPeriodFrom());
        LocalDateTime normalizedPeriodTo = normalizeToMidnight(request.getPeriodTo());

        // Get VAT filing period parameter
        Integer vatFilingPeriod = getVatFilingPeriod(header.getCompanyPoid());

        // Validate period rules
        List<String> periodErrors = periodValidationHelper.validatePeriodRules(
                normalizedPeriodFrom, normalizedPeriodTo, vatFilingPeriod);
        if (!periodErrors.isEmpty()) {
            throw new ValidationException(periodErrors.get(0));
        }

        // Check for overlapping periods (excluding current record)
        List<GlobalTaxSubmissionHdr> overlapping = hdrRepository.findOverlappingPeriodsExcluding(
                header.getCompanyPoid(), groupPoid, normalizedPeriodFrom, normalizedPeriodTo, transactionPoid);
        if (!overlapping.isEmpty()) {
            throw new ValidationException("A tax submission already exists for this period");
        }

        // Call before save validation stored procedure
        String beforeSaveStatus = storedProcedureHelper.validateBeforeSave(
                groupPoid, header.getCompanyPoid(), resolveProcedureUserId(), normalizedPeriodFrom, normalizedPeriodTo, transactionPoid);
        if (beforeSaveStatus != null && (beforeSaveStatus.contains("ERROR") || beforeSaveStatus.contains("WARNING"))) {
            throw new ValidationException(beforeSaveStatus);
        }

        // Check if period changed - if so, clear existing details
        boolean periodChanged = !header.getPeriodFrom().equals(normalizedPeriodFrom) ||
                                !header.getPeriodTo().equals(normalizedPeriodTo);
        if (periodChanged) {
            dtlRepository.deleteByTransactionPoid(transactionPoid);
            log.info("updateTaxSubmission cleared details due to period change for transactionPoid={}", transactionPoid);
        }

        // Update header
        header.setPeriodFrom(normalizedPeriodFrom);
        header.setPeriodTo(normalizedPeriodTo);
        header.setRemarks(request.getRemarks());

        GlobalTaxSubmissionHdr savedHeader = afterSaveRunner.persistHeader(header);
        savedHeader = afterSaveRunner.runAfterSaveAndReload(
                transactionPoid, groupPoid, header.getCompanyPoid(), resolveProcedureUserPoidForAfterSave());

        // Log the update
        String key = savedHeader.getTransactionPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, savedHeader, GlobalTaxSubmissionHdr.class, 
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        // Build response
        List<GlobalTaxSubmissionDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        TaxSubmissionResponse response = buildResponse(savedHeader, details);

        log.info("updateTaxSubmission completed for transactionPoid={}", transactionPoid);
        return response;
    }

    @Override
    @Transactional
    public void deleteTaxSubmission(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        Long groupPoid = UserContext.getGroupPoid();
        log.info("deleteTaxSubmission started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Submission", "transactionPoid", transactionPoid));

        if ("Y".equalsIgnoreCase(header.getDeleted())) {
            throw new ValidationException("Tax submission is already deleted");
        }

        // Remove detail rows first (legacy deletes child data before header soft-delete)
        dtlRepository.deleteByTransactionPoid(transactionPoid);

        header.setDeleted("Y");
        hdrRepository.save(header);

        try {
            String deleteStatus = documentDeleteService.deleteDocument(
                    transactionPoid,
                    "GLOBAL_TAX_SUBMISSION_HDR",
                    "TRANSACTION_POID",
                    deleteReasonDto,
                    resolveTransactionDate(header));
            if (deleteStatus != null
                    && (deleteStatus.toUpperCase().contains("ERROR") || deleteStatus.toUpperCase().contains("WARNING"))) {
                throw new ValidationException(deleteStatus);
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("deleteTaxSubmission failed for transactionPoid={}", transactionPoid, ex);
            throw new ValidationException("Failed to delete tax submission: " + ex.getMessage());
        }

        try {
            loggingService.createLogSummaryEntry(
                    LogDetailsEnum.DELETED, UserContext.getDocumentId(), transactionPoid.toString());
        } catch (Exception ex) {
            log.warn("Failed to write delete log summary for transactionPoid={}", transactionPoid, ex);
        }

        log.info("deleteTaxSubmission completed for transactionPoid={}", transactionPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listTaxSubmission(FilterRequestDto filters, 
                                                 Pageable pageable, LocalDate periodFrom, LocalDate periodTo) {
        String documentId = UserContext.getDocumentId();
        if (documentId == null) {
            documentId = DOC_ID_TAX_SUBMISSION; // Default document ID for tax submission
        }
        
        log.info("listTaxSubmission started for documentId={}", documentId);

        if ((periodFrom == null && periodTo != null) || (periodFrom != null && periodTo == null)) {
            throw new ValidationException("Both periodFrom and periodTo should be specified or both dates should be empty.");
        }
        
        if (periodFrom != null && periodTo != null && periodFrom.isAfter(periodTo)) {
            throw new ValidationException("Period From must not be after Period To");
        }

        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", periodFrom, periodTo);

        // Ensure filterList is mutable (resolveDateFilters may return Collections.emptyList() which is immutable)
        filterList = new ArrayList<>(filterList);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "TRANSACTION_POID", "DOC_REF");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        log.info("listTaxSubmission completed for documentId={} count={}", documentId, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public LoadVatDetailsResponse loadVatDetails(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long userPoid = UserContext.getUserPoid();
        String userId = UserContext.getUserId();
        
        log.info("loadVatDetails started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Submission", "transactionPoid", transactionPoid));

        // Validate header is in editable state
        if (header.getPeriodClosedDate() != null) {
            throw new ValidationException("Cannot load VAT details for closed period");
        }
        if ("APPROVED".equals(header.getApprovalStatus()) || "POSTED".equals(header.getStatus())) {
            throw new ValidationException("Cannot load VAT details for approved or posted submission");
        }

        // Validate period is set
        if (header.getPeriodFrom() == null || header.getPeriodTo() == null) {
            throw new ValidationException("Period From and Period To must be set before loading VAT details");
        }

        // Validate period is 1 month duration
        long daysBetween = ChronoUnit.DAYS.between(header.getPeriodFrom(), header.getPeriodTo()) + 1;
        if (daysBetween > 31) {
            throw new ValidationException("Period must be 1 month duration (30 days max) to load VAT details");
        }

        // Call stored procedure to load VAT details
        List<Map<String, Object>> loadedDetails = storedProcedureHelper.loadVatDetails(
                groupPoid, header.getCompanyPoid(), userPoid, transactionPoid,
                header.getPeriodFrom(), header.getPeriodTo());

        // Delete existing details
        dtlRepository.deleteByTransactionPoid(transactionPoid);

        // Save loaded details
        List<GlobalTaxSubmissionDtl> detailEntities = new ArrayList<>();
        for (Map<String, Object> detailMap : loadedDetails) {
            GlobalTaxSubmissionDtl detail = new GlobalTaxSubmissionDtl();
            detail.setTransactionPoid(transactionPoid);
            detail.setDetRowId(((Number) detailMap.get("DET_ROW_ID")).longValue());
            detail.setTaxType((String) detailMap.get("TAX_TYPE"));
            detail.setTaxPoid(detailMap.get("TAX_POID") != null ? 
                    ((Number) detailMap.get("TAX_POID")).longValue() : null);
            detail.setTaxCode((String) detailMap.get("TAX_CODE"));
            detail.setTaxDescription((String) detailMap.get("TAX_DESCRIPTION"));
            Object taxPercentage = detailMap.get("TAX_PERCENTAGE");
            if (taxPercentage != null) {
                detail.setTaxPercentage(taxPercentage.toString());
            }
            detail.setTaxBaseAmount(detailMap.get("TAX_BASE_AMOUNT") != null ? 
                    (BigDecimal) detailMap.get("TAX_BASE_AMOUNT") : null);
            detail.setTaxAmount(detailMap.get("TAX_AMOUNT") != null ? 
                    (BigDecimal) detailMap.get("TAX_AMOUNT") : null);
            detail.setTotalAmount(detailMap.get("TOTAL_AMOUNT") != null ? 
                    (BigDecimal) detailMap.get("TOTAL_AMOUNT") : null);
            detail.setRemarks((String) detailMap.get("REMARKS"));
            detailEntities.add(detail);
        }

        dtlRepository.saveAll(detailEntities);

        // Build response
        LoadVatDetailsResponse response = new LoadVatDetailsResponse();
        response.setStatus("SUCCESS");
        response.setMessage("VAT details loaded successfully");
        response.setDetails(detailEntities.stream()
                .map(this::convertDetailToResponse)
                .collect(Collectors.toList()));

        log.info("loadVatDetails completed for transactionPoid={} loadedCount={}", transactionPoid, detailEntities.size());
        return response;
    }

    @Override
    @Transactional
    public SubmitTaxSubmissionResponse submitTaxSubmission(Long transactionPoid, SubmitTaxSubmissionRequest request) {
        log.info("submitTaxSubmission started for transactionPoid={} action={}", transactionPoid, request.getAction());

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Submission", "transactionPoid", transactionPoid));

        // Validate VAT details are loaded
        List<GlobalTaxSubmissionDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        if (details == null || details.isEmpty()) {
            throw new ValidationException("VAT details must be loaded before submitting");
        }

        SubmitTaxSubmissionResponse response = new SubmitTaxSubmissionResponse();

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            // Approve
            // TODO: Integrate with approval service
            header.setApprovalStatus("APPROVED");
            header.setStatus("APPROVED");
            response.setStatus("APPROVED");
            response.setMessage("Tax submission approved successfully");
        } else if ("SUBMIT".equalsIgnoreCase(request.getAction())) {
            // Submit for approval
            // TODO: Integrate with approval service (one level - account manager)
            header.setApprovalStatus("SUBMITTED");
            response.setStatus("SUBMITTED");
            response.setMessage("Tax submission submitted for approval");
        }

        hdrRepository.save(header);
        response.setApprovalStatus(header.getApprovalStatus());

        log.info("submitTaxSubmission completed for transactionPoid={}", transactionPoid);
        return response;
    }

    @Override
    @Transactional
    public TaxSubmissionResponse runAfterSave(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        String userId = UserContext.getUserId();

        log.info("runAfterSave started for transactionPoid={} groupPoid={} userId={}", transactionPoid, groupPoid, userId);

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Submission", "transactionPoid", transactionPoid));

        GlobalTaxSubmissionHdr reloadedHeader = afterSaveRunner.runAfterSaveAndReload(
                transactionPoid, groupPoid, header.getCompanyPoid(), resolveProcedureUserPoidForAfterSave());
        List<GlobalTaxSubmissionDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);

        TaxSubmissionResponse response = buildResponse(reloadedHeader, details);

        log.info("runAfterSave completed for transactionPoid={}", transactionPoid);
        return response;
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, DOC_ID_TAX_SUBMISSION);
        params.put("SUB_HEADER", printService.load("Templates/DocHeaderSubReport.jrxml"));
        params.put("SUB_FOOTER_ISO", printService.load("Templates/DocFooterSubReport-ISO.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/TaxSubmissionReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidatePeriodResponse validatePeriod(ValidatePeriodRequest request) {
        Long groupPoid = UserContext.getGroupPoid();
        log.info("validatePeriod started for companyId={} periodFrom={} periodTo={}", 
                request.getCompanyId(), request.getPeriodFrom(), request.getPeriodTo());

        ValidatePeriodResponse response = new ValidatePeriodResponse();
        List<String> errors = new ArrayList<>();

        // Normalize period dates to midnight to match legacy behavior / PL-SQL expectations
        LocalDateTime normalizedPeriodFrom = normalizeToMidnight(request.getPeriodFrom());
        LocalDateTime normalizedPeriodTo = normalizeToMidnight(request.getPeriodTo());

        // Get VAT filing period parameter
        Integer vatFilingPeriod = getVatFilingPeriod(request.getCompanyId());

        // Validate period rules
        List<String> periodErrors = periodValidationHelper.validatePeriodRules(
                normalizedPeriodFrom, normalizedPeriodTo, vatFilingPeriod);
        errors.addAll(periodErrors);

        // Check for overlapping periods
        List<GlobalTaxSubmissionHdr> overlapping = hdrRepository.findOverlappingPeriods(
                request.getCompanyId(), groupPoid, normalizedPeriodFrom, normalizedPeriodTo);
        if (!overlapping.isEmpty()) {
            errors.add("A tax submission already exists for this period");
        }

        // TODO: Check for gap days between periods

        response.setValid(errors.isEmpty());
        response.setErrors(errors);
        if (errors.isEmpty()) {
            response.setMessage("Period is valid");
        } else {
            response.setMessage(errors.get(0));
        }

        log.info("validatePeriod completed valid={} errorCount={}", response.getValid(), errors.size());
        return response;
    }

    private TaxSubmissionResponse buildResponse(GlobalTaxSubmissionHdr header, List<GlobalTaxSubmissionDtl> details) {
        TaxSubmissionResponse response = new TaxSubmissionResponse();
        BeanUtils.copyProperties(header, response);
        response.setCompanyId(header.getCompanyPoid());
        response.setPeriodClosedBy(resolvePeriodClosedByDisplay(header.getPeriodClosedBy()));
        // TODO: Set companyName from lookup

        // Convert details
        List<TaxSubmissionDetailResponse> detailResponses = details.stream()
                .map(this::convertDetailToResponse)
                .collect(Collectors.toList());
        response.setDetails(detailResponses);

        return response;
    }

    private TaxSubmissionDetailResponse convertDetailToResponse(GlobalTaxSubmissionDtl detail) {
        TaxSubmissionDetailResponse response = new TaxSubmissionDetailResponse();
        response.setDetRowId(detail.getDetRowId());
        response.setTaxType(detail.getTaxType());
        response.setTaxPoid(detail.getTaxPoid());
        response.setTaxCode(detail.getTaxCode());
        response.setTaxDescription(detail.getTaxDescription());
        response.setTaxPercentage(detail.getTaxPercentage());
        response.setTaxBaseAmount(detail.getTaxBaseAmount());
        response.setTaxAmount(detail.getTaxAmount());
        response.setTotalAmount(detail.getTotalAmount());
        response.setRemarks(detail.getRemarks());

        return response;
    }

    private Integer getVatFilingPeriod(Long companyPoid) {
        if (companyPoid == null) {
            return null;
        }

        CompanyDto company = companyServiceClient.findById(companyPoid);
        if (company == null || company.getVatFilingPeriod() == null || company.getVatFilingPeriod().isBlank()) {
            return null;
        }
        
        try {
            return Integer.parseInt(company.getVatFilingPeriod());
        } catch (NumberFormatException e) {
            log.warn("Invalid vatFilingPeriod value for companyPoid={}: {}", companyPoid, company.getVatFilingPeriod());
            return null;
        }
    }

    /**
     * Normalize a {@link LocalDateTime} to midnight (00:00:00) of its date component.
     * This mirrors the legacy ADF behavior where only the date part was sent to
     * PL/SQL procedures expecting DATE values that are compared using TRUNC.
     */
    private LocalDateTime normalizeToMidnight(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atStartOfDay();
    }

    private LocalDate resolveTransactionDate(GlobalTaxSubmissionHdr header) {
        if (header.getTransactionDate() != null) {
            return header.getTransactionDate().toLocalDate();
        }
        return DateUtil.getCurrentDateInUserTimeZone();
    }

    
    private String resolveProcedureUserId() {
        String userId = UserContext.getUserId();
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        Long userPoid = UserContext.getUserPoid();
        return userPoid != null ? String.valueOf(userPoid) : userId;
    }

    private String resolveProcedureUserPoidForAfterSave() {
        Long userPoid = UserContext.getUserPoid();
        if (userPoid != null) {
            return String.valueOf(userPoid);
        }
        return resolveProcedureUserId();
    }

    private String resolvePeriodClosedByDisplay(String periodClosedBy) {
        if (periodClosedBy == null || periodClosedBy.isBlank() || !periodClosedBy.matches("\\d+")) {
            return periodClosedBy;
        }
        Long userPoid = UserContext.getUserPoid();
        if (userPoid != null && periodClosedBy.equals(String.valueOf(userPoid))) {
            String userName = UserContext.getUserName();
            if (userName != null && !userName.isBlank()) {
                return userName;
            }
        }
        return periodClosedBy;
    }
}

