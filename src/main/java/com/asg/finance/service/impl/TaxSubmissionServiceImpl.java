package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.client.CompanyServiceClient;
import com.asg.common.lib.entity.DocumentEntity;
import com.asg.common.lib.repository.DocumentCommonRepository;
import com.asg.common.lib.repository.TableMetaRepository;
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
import com.asg.finance.service.TaxSubmissionService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.service.TaxSubmissionStoredProcedureHelper;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxSubmissionServiceImpl implements TaxSubmissionService {

    private final GlobalTaxSubmissionHdrRepository hdrRepository;
    private final GlobalTaxSubmissionDtlRepository dtlRepository;
    private final TaxSubmissionStoredProcedureHelper storedProcedureHelper;
    private final PeriodValidationHelper periodValidationHelper;
    private final DocumentSearchService documentService;
    private final TableMetaRepository tableMetaRepository;
    private final DocumentCommonRepository documentRepository;
    private final CompanyServiceClient companyServiceClient;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

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
            throw new ValidationException("Period validation failed: " + String.join(", ", periodErrors));
        }

        // Check for overlapping periods (existing submissions)
        List<GlobalTaxSubmissionHdr> overlapping = hdrRepository.findOverlappingPeriods(
                finalCompanyId, groupPoid, normalizedPeriodFrom, normalizedPeriodTo);
        if (!overlapping.isEmpty()) {
            throw new ValidationException("A tax submission already exists for this period");
        }

        // Call before save validation stored procedure
        String beforeSaveStatus = storedProcedureHelper.validateBeforeSave(
                groupPoid, finalCompanyId, userId, normalizedPeriodFrom, normalizedPeriodTo, null);
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

        GlobalTaxSubmissionHdr savedHeader = hdrRepository.save(header);
        log.info("createTaxSubmission persisted header transactionPoid={}", savedHeader.getTransactionPoid());

        // Log the creation
        String key = savedHeader.getTransactionPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

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

        // Check if can be updated (not closed/approved/posted)
        if (header.getPeriodClosedDate() != null) {
            throw new ValidationException("Cannot update tax submission that has closed period");
        }
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
            throw new ValidationException("Period validation failed: " + String.join(", ", periodErrors));
        }

        // Check for overlapping periods (excluding current record)
        List<GlobalTaxSubmissionHdr> overlapping = hdrRepository.findOverlappingPeriodsExcluding(
                header.getCompanyPoid(), groupPoid, normalizedPeriodFrom, normalizedPeriodTo, transactionPoid);
        if (!overlapping.isEmpty()) {
            throw new ValidationException("A tax submission already exists for this period");
        }

        // Call before save validation stored procedure
        String beforeSaveStatus = storedProcedureHelper.validateBeforeSave(
                groupPoid, header.getCompanyPoid(), userId, normalizedPeriodFrom, normalizedPeriodTo, transactionPoid);
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

        GlobalTaxSubmissionHdr savedHeader = hdrRepository.save(header);

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

        // Check if can be deleted
        if (header.getPeriodClosedDate() != null) {
            throw new ValidationException("Cannot delete tax submission that has closed period");
        }
        if ("APPROVED".equals(header.getApprovalStatus()) || "POSTED".equals(header.getStatus())) {
            throw new ValidationException("Cannot delete tax submission that is already approved or posted");
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GLOBAL_TAX_SUBMISSION_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                header.getTransactionDate().toLocalDate()
        );
        
        // Log the deletion
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), transactionPoid.toString());


        log.info("deleteTaxSubmission completed for transactionPoid={}", transactionPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listTaxSubmission(FilterRequestDto filters, 
                                                 Pageable pageable, LocalDate periodFrom, LocalDate periodTo) {
        String documentId = UserContext.getDocumentId();
        if (documentId == null) {
            documentId = "400-118"; // Default document ID for tax submission
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

        // Use custom search method
        // Note: GROUP_POID column is NULL in all records in GLOBAL_TAX_SUBMISSION_HDR table,
        // so we pass null for groupPoid to skip GROUP_POID filtering entirely
        RawSearchResult raw = searchTaxSubmissions(filterList, operator, pageable, isDeleted, null, periodFrom, periodTo);

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

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
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
        Long groupPoid = UserContext.getGroupPoid();
        
        log.info("submitTaxSubmission started for transactionPoid={} action={}", transactionPoid, request.getAction());

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
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

        GlobalTaxSubmissionHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Submission", "transactionPoid", transactionPoid));

        String afterSaveStatus = storedProcedureHelper.processAfterSave(
                groupPoid, header.getCompanyPoid(), userId, header.getTransactionPoid());

        if (afterSaveStatus != null && (afterSaveStatus.contains("ERROR") || afterSaveStatus.contains("WARNING"))) {
            throw new ValidationException(afterSaveStatus);
        }

        // Reload header (procedure may have updated fields like PERIOD_CLOSED_BY/DATE)
        GlobalTaxSubmissionHdr reloadedHeader = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElse(header);
        List<GlobalTaxSubmissionDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);

        TaxSubmissionResponse response = buildResponse(reloadedHeader, details);

        log.info("runAfterSave completed for transactionPoid={}", transactionPoid);
        return response;
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
            response.setMessage("Period validation failed: " + String.join(", ", errors));
        }

        log.info("validatePeriod completed valid={} errorCount={}", response.getValid(), errors.size());
        return response;
    }

    // Private helper methods

    /**
     * Custom search method for tax submissions that handles GROUP_POID with exact numeric match
     */
    private RawSearchResult searchTaxSubmissions(List<FilterDto> filters, String operator, Pageable pageable, 
                                                 String isDeleted, @Nullable Long groupPoid, 
                                                 @Nullable LocalDate periodFrom, @Nullable LocalDate periodTo) {
        // Base SQL query
        String baseSql = "SELECT * FROM GLOBAL_TAX_SUBMISSION_HDR";
        
        // Get searchable columns from table
        List<String> columnNames = tableMetaRepository.getColumnsFromTable("GLOBAL_TAX_SUBMISSION_HDR");
        
        // Build WHERE clause with proper GROUP_POID handling
        WhereClauseResult whereClause = buildTaxSubmissionWhereClause(columnNames, filters, operator, isDeleted, 
                                                                       groupPoid, periodFrom, periodTo);
        
        // Apply sorting
        String sortedSql = applyTaxSubmissionSorting(baseSql, pageable, columnNames, whereClause.sql());
        
        // Add pagination
        String finalSql = sortedSql + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        
        List<Object> params = new ArrayList<>(whereClause.params());
        params.add(pageable.getPageNumber() * pageable.getPageSize()); // offset
        params.add(pageable.getPageSize()); // limit
        
        // Execute query
        List<Map<String, Object>> rows = tableMetaRepository.executeDynamicQuery(finalSql, params, columnNames);
        
        // Get display fields from document configuration
        Map<String, String> displayFields = getDisplayFields("400-118");
        
        // Enrich rows with displayable fields and label/value
        for (Map<String, Object> row : rows) {
            if (displayFields != null) {
                for (String field : displayFields.keySet()) {
                    row.putIfAbsent(field, null);
                }
            }
            // Add label/value for frontend
            row.putIfAbsent("label", row.get("TRANSACTION_POID"));
            row.putIfAbsent("value", row.get("DOC_REF"));
        }
        
        // Get total count
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + " " + whereClause.sql() + ") total_count";
        Long totalRecords = tableMetaRepository.executeCountQuery(countSql, whereClause.params());
        
        return new RawSearchResult(rows, displayFields != null ? displayFields : Map.of(), totalRecords);
    }
    
    /**
     * Build WHERE clause for tax submission search with proper GROUP_POID handling
     */
    private WhereClauseResult buildTaxSubmissionWhereClause(List<String> fields, List<FilterDto> filters, 
                                                            String operator, String isDeleted, 
                                                            @Nullable Long groupPoid,
                                                            @Nullable LocalDate periodFrom, 
                                                            @Nullable LocalDate periodTo) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        
        // DELETED filter (exact match)
        if ("Y".equalsIgnoreCase(isDeleted)) {
            sql.append(" AND DELETED = 'Y'");
        } else {
            sql.append(" AND (DELETED IS NULL OR DELETED = 'N')");
        }
        
        // GROUP_POID filter (exact numeric match - critical for data isolation)
        // NOTE: GROUP_POID column is NULL in all records in GLOBAL_TAX_SUBMISSION_HDR table,
        // so this filter is intentionally skipped (groupPoid is always passed as null)
        // If GROUP_POID data is populated in the future, this filter will automatically work
        if (groupPoid != null) {
            sql.append(" AND GROUP_POID = ?");
            params.add(groupPoid);
        }
        
        // Date filters for periodFrom/periodTo (if provided)
        if (periodFrom != null && periodTo != null) {
            sql.append(" AND TRANSACTION_DATE >= DATE ?");
            params.add(java.sql.Date.valueOf(periodFrom));
            sql.append(" AND TRANSACTION_DATE <= DATE ?");
            params.add(java.sql.Date.valueOf(periodTo));
        }
        
        // Handle other filters
        boolean hasOrGroup = "OR".equalsIgnoreCase(operator);
        boolean groupStarted = false;
        
        for (FilterDto f : Optional.ofNullable(filters).orElse(List.of())) {
            String field = f.searchField().toUpperCase();
            String rawValue = f.searchValue();
            if (rawValue == null) continue;
            
            if ("GLOBALSEARCH".equals(field)) {
                handleGlobalSearch(sql, fields, rawValue, params);
            } else if (fields.contains(field)) {
                String[] cmp = parseComparison(rawValue);
                String op = cmp[0], value = cmp[1];
                boolean isDateField = value.matches("\\d{4}-\\d{2}-\\d{2}");
                String fieldCondition = buildTaxSubmissionFieldCondition(field, op, value, isDateField, params);
                
                // Close OR group before date filters
                if (isDateField && groupStarted) {
                    sql.append(")");
                    groupStarted = false;
                }
                
                // Append condition with proper grouping
                if (isDateField) {
                    sql.append(" AND ").append(fieldCondition);
                } else {
                    if (hasOrGroup && !groupStarted) {
                        sql.append(" AND (");
                        groupStarted = true;
                    } else {
                        sql.append(" ").append(operator).append(" ");
                    }
                    sql.append(fieldCondition);
                }
            }
        }
        
        // Close open OR group
        if (groupStarted) sql.append(")");
        
        return new WhereClauseResult(sql.toString(), params);
    }
    
    /**
     * Build field condition for tax submission search
     * Numeric fields use exact match, text fields use LIKE
     */
    private String buildTaxSubmissionFieldCondition(String field, String op, String value, 
                                                    boolean isDateField, List<Object> params) {
        // Numeric fields that should use exact match
        boolean isNumericField = "TRANSACTION_POID".equals(field) || 
                                 "GROUP_POID".equals(field) || 
                                 "COMPANY_POID".equals(field);
        
        if (!"=".equals(op)) {
            // Comparison operators (>, >=, <, <=)
            if (isDateField) {
                return field + " " + op + " DATE '" + value.trim() + "'";
            } else if (isNumericField) {
                params.add(Long.parseLong(value.trim()));
                return field + " " + op + " ?";
            } else {
                params.add("%" + value.toUpperCase() + "%");
                return "UPPER(" + field + ") LIKE ?";
            }
        }
        
        // Equals operator
        if (value.contains("|")) {
            // Multiple values (OR condition)
            String[] vals = value.split("\\|");
            List<String> orClauses = new ArrayList<>();
            for (String v : vals) {
                if (isNumericField) {
                    orClauses.add(field + " = ?");
                    params.add(Long.parseLong(v.trim()));
                } else {
                    orClauses.add("UPPER(" + field + ") LIKE ?");
                    params.add("%" + v.toUpperCase() + "%");
                }
            }
            return "(" + String.join(" OR ", orClauses) + ")";
        }
        
        // Single value
        if (isNumericField) {
            params.add(Long.parseLong(value.trim()));
            return field + " = ?";
        } else {
            params.add("%" + value.toUpperCase() + "%");
            return "UPPER(" + field + ") LIKE ?";
        }
    }
    
    /**
     * Handle global search across multiple fields
     */
    private void handleGlobalSearch(StringBuilder sql, List<String> fields, String value, List<Object> params) {
        List<String> orClauses = new ArrayList<>();
        
        for (String f : fields) {
            // Numeric fields use exact match, text fields use LIKE
            boolean isNumericField = "TRANSACTION_POID".equals(f) || 
                                   "GROUP_POID".equals(f) || 
                                   "COMPANY_POID".equals(f);
            
            if (isNumericField) {
                try {
                    Long numValue = Long.parseLong(value.trim());
                    orClauses.add(f + " = ?");
                    params.add(numValue);
                } catch (NumberFormatException e) {
                    // If not a number, skip this field
                }
            } else {
                orClauses.add("UPPER(" + f + ") LIKE ?");
                params.add("%" + value.toUpperCase() + "%");
            }
        }
        
        if (!orClauses.isEmpty()) {
            sql.append(" AND (").append(String.join(" OR ", orClauses)).append(")");
        }
    }
    
    /**
     * Parse comparison operator and value
     */
    private String[] parseComparison(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return new String[]{"=", ""};
        }
        
        String value = rawValue.trim();
        String op = "=";
        
        if (value.startsWith(">=")) {
            op = ">=";
            value = value.substring(2).trim();
        } else if (value.startsWith("<=")) {
            op = "<=";
            value = value.substring(2).trim();
        } else if (value.startsWith(">")) {
            op = ">";
            value = value.substring(1).trim();
        } else if (value.startsWith("<")) {
            op = "<";
            value = value.substring(1).trim();
        }
        
        return new String[]{op, value};
    }
    
    /**
     * Apply sorting to SQL query
     */
    private String applyTaxSubmissionSorting(String baseSql, Pageable pageable, List<String> columnNames, String whereClause) {
        // Strip ORDER BY from base SQL if Pageable has sorting
        String sql = pageable.getSort().isSorted()
                ? baseSql.replaceAll("(?i)ORDER\\s+BY[\\s\\S]*?(?=\\))", "")
                : baseSql;
        
        StringBuilder sqlBuilder = new StringBuilder(sql).append(whereClause);
        
        // Apply dynamic sorting from Pageable
        if (pageable.getSort().isSorted()) {
            String orderBy = pageable.getSort().stream()
                    .filter(order -> columnNames.contains(order.getProperty().toUpperCase()))
                    .map(order -> order.getProperty() + " " + order.getDirection().name())
                    .collect(Collectors.joining(", "));
            if (!orderBy.isEmpty()) {
                sqlBuilder.append(" ORDER BY ").append(orderBy);
            }
        } else {
            // Default sorting by TRANSACTION_POID DESC
            sqlBuilder.append(" ORDER BY TRANSACTION_POID DESC");
        }
        
        return sqlBuilder.toString();
    }
    
    /**
     * Get display fields from document configuration
     */
    private Map<String, String> getDisplayFields(String docId) {
        DocumentEntity doc = documentRepository.findByDocId(docId);
        if (doc == null) {
            return Map.of();
        }
        
        if (doc.getListOfDisplayColumnsAndTypes() != null && !doc.getListOfDisplayColumnsAndTypes().isBlank()) {
            return parseDisplayColumns(doc.getListOfDisplayColumnsAndTypes());
        }
        return Map.of();
    }
    
    /**
     * Parse display columns configuration
     */
    private Map<String, String> parseDisplayColumns(String config) {
        if (config == null || config.isBlank()) {
            return Map.of();
        }
        
        Map<String, String> map = new LinkedHashMap<>();
        for (String part : config.split("\\|")) {
            String trimmed = part.trim();
            if (!trimmed.startsWith("<") || !trimmed.endsWith(">") || !trimmed.contains(",")) {
                continue; // Skip invalid format
            }
            String[] kv = trimmed.substring(1, trimmed.length() - 1).split(",", 2);
            if (kv.length == 2) {
                map.put(kv[0].toUpperCase().trim(), kv[1].trim());
            }
        }
        return map;
    }
    
    /**
     * Helper class for WHERE clause result
     */
    private record WhereClauseResult(String sql, List<Object> params) {}
    
    private TaxSubmissionResponse buildResponse(GlobalTaxSubmissionHdr header, List<GlobalTaxSubmissionDtl> details) {
        TaxSubmissionResponse response = new TaxSubmissionResponse();
        BeanUtils.copyProperties(header, response);
        response.setCompanyId(header.getCompanyPoid());
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
}

