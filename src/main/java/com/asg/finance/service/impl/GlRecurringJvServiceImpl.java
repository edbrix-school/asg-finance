package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlRecurringJvDtl;
import com.asg.finance.entity.GlRecurringJvHdr;
import com.asg.finance.entity.GlRecurringJvMonthDtl;
import com.asg.finance.entity.key.TransactionDetailKey;
import com.asg.finance.repository.GlRecurringJvDtlRepository;
import com.asg.finance.repository.GlRecurringJvHdrRepository;
import com.asg.finance.repository.GlRecurringJvMonthDtlRepository;
import com.asg.finance.repository.GlRecurringJvProcRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.GlRecurringJvService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static com.asg.common.lib.utility.ASGHelperUtils.*;
import static com.asg.finance.utility.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GlRecurringJvServiceImpl implements GlRecurringJvService {

    private final DocumentSearchService documentService;
    private final GlRecurringJvHdrRepository hdrRepository;
    private final GlRecurringJvDtlRepository dtlRepository;
    private final GlRecurringJvMonthDtlRepository monthDtlRepository;
    private final GLMasterRepository glMasterRepository;
    private final CostCenterBreakupService costCenterBreakupService;
    private final BillwiseBreakupService billwiseBreakupService;
    private final GlRecurringJvProcRepository procRepository;
    private final LovDataService lovService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String RESOURCE_NAME = "Recurring JV";
    private static final String FIELD_TRANSACTION_POID = "transactionPoid";
    private static final String DB_COLUMN_TRANSACTION_POID = "TRANSACTION_POID";
    private static final String DB_COLUMN_TRANSACTION_DATE = "TRANSACTION_DATE";
    private static final String DB_COLUMN_NARRATION = "NARRATION";
    private static final String DOC_ID_RECURRING_JV = "400-102";
    private static final String TABLE_RECURRING_JV_HDR = "GL_RECURRING_JV_HDR";
    private static final String LOV_GL_MASTER_LEDGERS = "GL_MASTER_LEDGERS";
    private static final String LOV_RJV_EMPLOYEE_DTLS = "RJV_EMPLOYEE_DTLS";
    private static final String LOV_RJV_FIXED_ASSET_DTLS = "RJV_FIXED_ASSET_DTLS";
    private static final String LOV_COMPANY = "COMPANY";
    private static final String REPORT_MAIN = "Finance/GL/RecurringJVReport.jrxml";
    private static final String REPORT_SUB_GL = "Finance/GL/RecurringJVGLSubreport1.jrxml";
    private static final String REPORT_SUB_SCHEDULE = "Finance/GL/RecurringJVScheduleWiseSubreport2.jrxml";

    @Override
    public Map<String, Object> listRecurringJvs(String documentId, FilterRequestDto filters, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, DB_COLUMN_TRANSACTION_DATE, startDate,
                endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                DB_COLUMN_NARRATION,
                DB_COLUMN_TRANSACTION_POID);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringJvResponse getRecurringJvById(Long transactionPoid) {

        GlRecurringJvHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(
                        () -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_TRANSACTION_POID, transactionPoid));

        List<GlRecurringJvDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        List<GlRecurringJvMonthDtl> scheduleDetails = monthDtlRepository.findByTransactionPoid(transactionPoid);
        BigDecimal crTotal = calculateCrTotal(transactionPoid);
        BigDecimal drTotal = calculateDrTotal(transactionPoid);

        RecurringJvResponse response = buildResponse(header, details, scheduleDetails, drTotal, crTotal);

        log.info("getRecurringJvById completed for transactionPoid={}", transactionPoid);
        return response;
    }

    private BigDecimal calculateDrTotal(Long transactionPoid) {
        BigDecimal total = dtlRepository.getDrTotalByTransactionPoid(transactionPoid);
        return total != null ? total : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCrTotal(Long transactionPoid) {
        BigDecimal total = dtlRepository.getCrTotalByTransactionPoid(transactionPoid);
        return total != null ? total : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    }

    private RecurringJvResponse buildResponse(
            GlRecurringJvHdr header,
            List<GlRecurringJvDtl> details,
            List<GlRecurringJvMonthDtl> scheduleDetails,
            BigDecimal drTotal,
            BigDecimal crTotal) {

        // Load all cost center and billwise data once
        GlVoucherCostCenterBreakupResponseDto costCenterResponse = loadAllCostCenterData(header.getTransactionPoid());
        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = loadAllBillwiseData(header.getTransactionPoid());

        RecurringJvResponse response = new RecurringJvResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setDocRef(header.getDocRef());
        response.setTransactionDate(header.getTransactionDate() != null ? header.getTransactionDate() : null);
        response.setNarration(header.getNarration());
        response.setStartDate(header.getStartDate() != null ? header.getStartDate() : null);
        response.setTotalAmount(header.getTotalAmount());
        response.setNoOfMonths(header.getNoOfMonths());
        response.setMonthWiseAmt(header.getMonthWiseAmt());
        response.setRefType(header.getRefType());
        response.setEmployeePoid(header.getEmployeePoid());
        response.setAssetPoid(header.getFaPoid());
        response.setPolicyNumber(header.getPolicyNumber());
        response.setRemarks(header.getRemarks());
        response.setGroupPoid(header.getGroupPoid());
        response.setCompanyPoid(header.getCompanyPoid());
        response.setDrTotal(drTotal);
        response.setCrTotal(crTotal);
        response.setGlPosting(false);
        response.setCreatedBy(header.getCreatedBy());
        response.setCreatedDate(header.getCreatedDate());
        response.setLastModifiedBy(header.getLastModifiedBy());
        response.setLastModifiedDate(header.getLastModifiedDate());

        boolean hasBillWiseCapable = details.stream()
                .anyMatch(dtl -> dtl.getGlPoid() != null &&
                        glMasterRepository.findById(dtl.getGlPoid())
                                .map(gl -> FLAG_YES.equalsIgnoreCase(gl.getBillwise()))
                                .orElse(false));
        response.setBillWiseCapable(hasBillWiseCapable);

        if (header.getEmployeePoid() != null) {
            response.setEmployeeDet(
                    lovService.getDetailsByPoidAndLovName(header.getEmployeePoid(), LOV_RJV_EMPLOYEE_DTLS));
        }
        if (header.getFaPoid() != null) {
            response.setAssetDet(lovService.getDetailsByPoidAndLovName(header.getFaPoid(), LOV_RJV_FIXED_ASSET_DTLS));
        }
        if (header.getCompanyPoid() != null) {
            response.setCompanyDet(lovService.getDetailsByPoidAndLovName(header.getCompanyPoid(), LOV_COMPANY));
        }

        List<RecurringJvDetailResponse> detailResponses = details.stream()
                .map(dtl -> convertDetailToResponseWithBreakups(dtl, costCenterResponse, billwiseResponse))
                .toList();
        response.setDetails(detailResponses);

        List<RecurringJvScheduleDetailResponse> scheduleResponses = scheduleDetails.stream()
                .map(this::convertScheduleToResponse)
                .toList();
        response.setScheduleDetails(scheduleResponses);

        return response;
    }

    private GlVoucherCostCenterBreakupResponseDto loadAllCostCenterData(Long transactionPoid) {
        try {
            return costCenterBreakupService.loadCostCenterData(
                    DOC_ID_RECURRING_JV, transactionPoid, getGroupId(), getCompanyId(), getUserPoid());
        } catch (Exception e) {
            log.warn("Failed to load cost center breakup for transaction: {}", transactionPoid, e);
            return null;
        }
    }

    private GlVoucherLoadBillwiseBreakupResponseDto loadAllBillwiseData(Long transactionPoid) {
        try {
            return billwiseBreakupService.loadBillwiseBreakup(
                    getGroupId(), getCompanyId(), DOC_ID_RECURRING_JV, transactionPoid);
        } catch (Exception e) {
            log.warn("Failed to load billwise breakup for transaction: {}", transactionPoid, e);
            return null;
        }
    }

    private RecurringJvDetailResponse convertDetailToResponseWithBreakups(
            GlRecurringJvDtl dtl,
            GlVoucherCostCenterBreakupResponseDto costCenterResponse,
            GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse) {

        RecurringJvDetailResponse response = new RecurringJvDetailResponse();
        response.setDetRowId(dtl.getDetRowId());
        response.setType(dtl.getType());
        response.setCompanyPoid(dtl.getCompanyPoid());
        response.setGlPoid(dtl.getGlPoid());
        response.setDrAmt(dtl.getDrAmt());
        response.setCrAmt(dtl.getCrAmt());
        response.setRemarks(dtl.getRemarks());

        if (dtl.getGlPoid() != null) {
            response.setGlDet(lovService.getDetailsByPoidAndLovName(dtl.getGlPoid(), LOV_GL_MASTER_LEDGERS));
        }

        response.setCostCenter(filterCostCenterBreakup(costCenterResponse, dtl.getDetRowId()));
        response.setBillWiseBreakup(filterBillwiseBreakup(billwiseResponse, dtl.getDetRowId()));

        return response;
    }

    private List<CostCenterBreakupPopupRequestDto> filterCostCenterBreakup(
            GlVoucherCostCenterBreakupResponseDto response, Long detRowId) {

        if (response == null || response.getCostBreakupList() == null) {
            return Collections.emptyList();
        }

            return response.getCostBreakupList().stream()
                    .filter(cc -> Objects.equals(cc.getMainDetRowId(), detRowId))
                    .map(cc -> {
                        CostCenterBreakupPopupRequestDto dto = new CostCenterBreakupPopupRequestDto();

                        dto.setCostDetRowId(cc.getCostDetRowId());
                        dto.setCostGroup(cc.getCostGroup());
                        dto.setCostPoid(cc.getCostPoid() != null ? cc.getCostPoid() : null);
                        dto.setAmount(cc.getAmount() != null ? cc.getAmount() : BigDecimal.ZERO);

                        if (StringUtils.isNotEmpty(cc.getCostPoid()) && StringUtils.isNotEmpty(cc.getCostGroup())) {
                            try {
                                Long poid = Long.parseLong(cc.getCostPoid());
                                dto.setCostCenterDetails(
                                        lovService.getDetailsByPoidAndLovName(poid, cc.getCostGroup())
                                );
                            } catch (NumberFormatException e) {
                                dto.setCostCenterDetails(
                                        lovService.getDetailsByCodeAndLovName(cc.getCostPoid(), cc.getCostGroup())
                                );
                            }
                        }

                        return dto;
                    })
                    .toList();
        }

        private List<BillwiseBreakupPopupRequestDto> filterBillwiseBreakup(
                GlVoucherLoadBillwiseBreakupResponseDto response, Long detRowId) {

            if (response == null || response.getLoadBillwiseBreakupResponseDtoList() == null) {
                return List.of();
            }

            return response.getLoadBillwiseBreakupResponseDtoList().stream()
                    .filter(bw -> bw.getMainDetRowId().equals(detRowId))
                    .map(bw -> {
                        BillwiseBreakupPopupRequestDto dto = new BillwiseBreakupPopupRequestDto();
                        dto.setBillDetRowId(bw.getBillDetRowId());
                        dto.setBillRefType(bw.getBillRefType());
                        dto.setBillRef(bw.getBillRef());
                        dto.setBillDueDate(bw.getBillDueDate());
                        BigDecimal drAmt = bw.getDrAmt() != null ? bw.getDrAmt() : BigDecimal.ZERO;
                        BigDecimal crAmt = bw.getCrAmt() != null ? bw.getCrAmt() : BigDecimal.ZERO;
                        dto.setType(drAmt.compareTo(BigDecimal.ZERO) > 0 ? "DR" : "CR");
                        dto.setAmount(drAmt.compareTo(BigDecimal.ZERO) > 0 ? drAmt : crAmt);
                        dto.setBillRemarks(bw.getBillRemarks());
                        return dto;
                    })
                    .toList();
        }
    private RecurringJvScheduleDetailResponse convertScheduleToResponse(GlRecurringJvMonthDtl schedule) {
        RecurringJvScheduleDetailResponse response = new RecurringJvScheduleDetailResponse();
        response.setDetRowId(schedule.getDetRowId());
        response.setMonthWiseDate(schedule.getMonthWiseDate() != null ? schedule.getMonthWiseDate() : DateUtil.getCurrentDateInUserTimeZone());
        response.setJv(schedule.getJvPoid());
        response.setAmount(schedule.getAmount());
        response.setStatus(schedule.getStatus());
        response.setRemarks(schedule.getRemarks());
        response.setDrilldownLinkInfo(schedule.getDrilldownLinkInfo());
        return response;
    }

    @Override
    @Transactional
    public RecurringJvCreateResponse createRecurringJv(RecurringJvRequest request) {
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            validateRequest(request);
        }
        GlRecurringJvHdr header = GlRecurringJvHdr.builder()
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : DateUtil.getCurrentDateInUserTimeZone())
                .groupPoid(getGroupId())
                .companyPoid(getCompanyId())
                .narration(request.getNarration())
                .startDate(request.getStartDate())
                .totalAmount(request.getTotalAmount())
                .noOfMonths(request.getNoOfMonths())
                .monthWiseAmt(request.getMonthWiseAmount())
                .refType(request.getRefType())
                .employeePoid(request.getEmployeePoid())
                .faPoid(request.getAssetPoid())
                .policyNumber(request.getPolicyNumber())
                .remarks(request.getRemarks())
                .deleted(FLAG_NO)
                .build();

        header = hdrRepository.save(header);

        // Flush and refresh entity to get updated docRef
        entityManager.flush();
        entityManager.refresh(header);

        Long transactionPoid = header.getTransactionPoid();

        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                transactionPoid.toString(),
                String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), header.getDocRef())
        );

        saveDetails(transactionPoid, request.getDetails(), header, true);

        return new RecurringJvCreateResponse(transactionPoid, "Recurring JV created successfully");
    }

    private void validateRequest(RecurringJvRequest request) {

        BigDecimal drTotal = request.getDetails().stream()
                .map(d -> d.getDrAmt() != null ? d.getDrAmt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal crTotal = request.getDetails().stream()
                .map(d -> d.getCrAmt() != null ? d.getCrAmt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (drTotal.compareTo(crTotal) != 0) {
            throw new IllegalArgumentException("DR Total must equal CR Total");
        }

        if (drTotal.setScale(3, RoundingMode.HALF_UP).compareTo(request.getMonthWiseAmount()) != 0) {
            throw new IllegalArgumentException(String.format("Monthly amount (%s) must equal Dr Total (%s)", request.getMonthWiseAmount(), drTotal));
        }

        for (RecurringJvDetailRequest detail : request.getDetails()) {
            validateDetailLine(detail);
        }
    }

    private void validateDetailLine(RecurringJvDetailRequest detail) {
        if (TYPE_DEBIT.equalsIgnoreCase(detail.getType())) {
            validateDebitAmount(detail.getDrAmt());
            if (detail.getCrAmt() != null && detail.getCrAmt().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("CrAmt must be 0 for Type=Dr");
            }
        } else if (TYPE_CREDIT.equalsIgnoreCase(detail.getType())) {
            validateCreditAmount(detail.getCrAmt());
            if (detail.getDrAmt() != null && detail.getDrAmt().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("DrAmt must be 0 for Type=Cr");
            }
        } else {
            throw new IllegalArgumentException("Invalid Type: Must be 'Dr' or 'Cr'");
        }

        if (!glMasterRepository.existsByGlPoid(detail.getGlPoid())) {
            throw new ResourceNotFoundException("GL Master", "glId", detail.getGlPoid());
        }
    }

    private void validateDebitAmount(BigDecimal drAmt) {
        if (drAmt == null || drAmt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("DrAmt must be greater than 0 for Type=Dr");
        }
    }

    private void validateCreditAmount(BigDecimal crAmt) {
        if (crAmt == null || crAmt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("CrAmt must be greater than 0 for Type=Cr");
        }
    }

    private void saveDetails(Long transactionPoid, List<RecurringJvDetailRequest> details, GlRecurringJvHdr header,
            boolean isNewRecord) {
        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();
        List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
        List<LogRequestDto<GlRecurringJvDtl>> logRequests = new ArrayList<>();
        List<GlRecurringJvDtl> detailsToPersist = new ArrayList<>();

        long nextDetRowId = isNewRecord ? 1L : getNextDetRowIdForRecurring(transactionPoid);

        for (RecurringJvDetailRequest detail : details) {
            String action = isNewRecord ? ACTION_ISCREATED : resolveAction(detail.getActionType());
            
            if (ACTION_ISCREATED.equals(action) && detail.getDetRowId() == null) {
                detail.setDetRowId(nextDetRowId++);
            }

            Long companyPoid = detail.getCompanyPoid() != null ? detail.getCompanyPoid() : header.getCompanyPoid();

            if (companyPoid == null) {
                throw new IllegalArgumentException("Company ID is required");
            }

            processDetailAction(transactionPoid, detail, action, companyPoid, costCenterList, billwiseList,
                    logRequests, detailsToPersist);
        }

        if (!detailsToPersist.isEmpty()) {
            dtlRepository.saveAll(detailsToPersist);
        }

        finalizeBatchSaves(logRequests, costCenterList, billwiseList, isNewRecord);
    }

    private String resolveAction(String actionType) {
        String action = (actionType == null || actionType.trim().isEmpty())
                ? ACTION_NOCHANGES
                : actionType.trim().toUpperCase();

        return switch (action) {
            case "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
            case "ISUPDATED", "UPDATED" -> ACTION_ISUPDATED;
            case "ISDELETED", "DELETED" -> ACTION_ISDELETED;
            default -> ACTION_NOCHANGES;
        };
    }

    private void processDetailAction(Long transactionPoid, RecurringJvDetailRequest detail, String action,
            Long companyPoid,
            List<CostCenterBreakupRequestDto> costCenterList, List<BillwiseBreakupRequestDto> billwiseList,
            List<LogRequestDto<GlRecurringJvDtl>> logRequests, List<GlRecurringJvDtl> detailsToPersist) {
        switch (action) {
            case ACTION_ISDELETED -> handleDeletedDetail(transactionPoid, detail);
            case ACTION_ISCREATED ->
                handleCreatedDetail(transactionPoid, detail, companyPoid, costCenterList, billwiseList,
                        detailsToPersist);
            case ACTION_ISUPDATED ->
                handleUpdatedDetail(transactionPoid, detail, companyPoid, costCenterList, billwiseList,
                        logRequests, detailsToPersist);
            default ->
                handleNoChangeDetail(transactionPoid, detail, companyPoid, costCenterList, billwiseList);
        }
    }

    private void handleNoChangeDetail(Long transactionPoid, RecurringJvDetailRequest detail, Long companyPoid,
            List<CostCenterBreakupRequestDto> costCenterList, List<BillwiseBreakupRequestDto> billwiseList) {

        if (detail.getCostCenter() != null && !detail.getCostCenter().isEmpty()) {
            costCenterList.addAll(buildCostCenterBreakups(transactionPoid, detail.getDetRowId(), detail.getGlPoid(),
                    detail.getCostCenter()));
        }
        if (detail.getBillWiseBreakup() != null && !detail.getBillWiseBreakup().isEmpty()) {
            billwiseList.addAll(buildBillwiseBreakups(transactionPoid, detail.getDetRowId(), detail.getGlPoid(),
                    companyPoid, detail.getBillWiseBreakup()));
        }
    }

    private void handleDeletedDetail(Long transactionPoid, RecurringJvDetailRequest detail) {
        if (detail.getDetRowId() != null) {
            dtlRepository.deleteById(new TransactionDetailKey(transactionPoid, detail.getDetRowId()));
            loggingService.logDelete(detail, DOC_ID_RECURRING_JV, transactionPoid.toString());
        }
    }

    private void handleCreatedDetail(Long transactionPoid, RecurringJvDetailRequest detail, Long companyPoid,
            List<CostCenterBreakupRequestDto> costCenterList, List<BillwiseBreakupRequestDto> billwiseList,
            List<GlRecurringJvDtl> detailsToPersist) {
        Long detRowId = detail.getDetRowId();
        GlRecurringJvDtl dtl = new GlRecurringJvDtl();
        mapDetailToEntity(dtl, transactionPoid, detRowId, detail, companyPoid);
        detailsToPersist.add(dtl);

        loggingService.createLogSummaryEntry(DOC_ID_RECURRING_JV, transactionPoid.toString(),
                String.format("Row Created on Recurring JV Detail with detRowId: %s", detRowId));

        if (detail.getCostCenter() != null && !detail.getCostCenter().isEmpty()) {
            costCenterList.addAll(buildCostCenterBreakups(transactionPoid, detRowId, detail.getGlPoid(),
                    detail.getCostCenter()));
        }
        if (detail.getBillWiseBreakup() != null && !detail.getBillWiseBreakup().isEmpty()) {
            billwiseList.addAll(buildBillwiseBreakups(transactionPoid, detRowId, detail.getGlPoid(),
                    companyPoid, detail.getBillWiseBreakup()));
        }
    }

    private void handleUpdatedDetail(Long transactionPoid, RecurringJvDetailRequest detail, Long companyPoid,
            List<CostCenterBreakupRequestDto> costCenterList, List<BillwiseBreakupRequestDto> billwiseList,
            List<LogRequestDto<GlRecurringJvDtl>> logRequests, List<GlRecurringJvDtl> detailsToPersist) {
        if (!glMasterRepository.existsByGlPoid(detail.getGlPoid())) {
            throw new ResourceNotFoundException("GL Master", "glId", detail.getGlPoid());
        }

        GlRecurringJvDtl dtl = dtlRepository.findById(new TransactionDetailKey(transactionPoid, detail.getDetRowId()))
                .orElseThrow(() -> new ResourceNotFoundException("Recurring JV Detail", "detRowId", detail.getDetRowId()));

        GlRecurringJvDtl oldDetail = new GlRecurringJvDtl();
        BeanUtils.copyProperties(dtl, oldDetail);

        mapDetailToEntity(dtl, transactionPoid, detail.getDetRowId(), detail, companyPoid);
        detailsToPersist.add(dtl);

        logRequests.add(new LogRequestDto<>(oldDetail, dtl, GlRecurringJvDtl.class,DOC_ID_RECURRING_JV , transactionPoid.toString(),
                String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detail.getDetRowId())));

        if (detail.getCostCenter() != null && !detail.getCostCenter().isEmpty()) {
            costCenterList.addAll(buildCostCenterBreakups(transactionPoid, detail.getDetRowId(), detail.getGlPoid(),
                    detail.getCostCenter()));
        }
        if (detail.getBillWiseBreakup() != null && !detail.getBillWiseBreakup().isEmpty()) {
            billwiseList.addAll(buildBillwiseBreakups(transactionPoid, detail.getDetRowId(), detail.getGlPoid(),
                    companyPoid, detail.getBillWiseBreakup()));
        }
    }

    private void mapDetailToEntity(GlRecurringJvDtl dtl, Long transactionPoid, Long detRowId,
            RecurringJvDetailRequest detail, Long companyPoid) {
        dtl.setTransactionPoid(transactionPoid);
        dtl.setDetRowId(detRowId);
        dtl.setType(detail.getType());
        dtl.setCompanyPoid(companyPoid);
        dtl.setGlPoid(detail.getGlPoid());
        dtl.setDrAmt(detail.getDrAmt());
        dtl.setCrAmt(detail.getCrAmt());
        dtl.setRemarks(detail.getRemarks());
    }

    private void finalizeBatchSaves(List<LogRequestDto<GlRecurringJvDtl>> logRequests,
            List<CostCenterBreakupRequestDto> costCenterList,
            List<BillwiseBreakupRequestDto> billwiseList, boolean isNewRecord) {
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        if (!costCenterList.isEmpty()) {
            if(isNewRecord){
                 costCenterBreakupService.saveCostCenterBreakups(costCenterList);
            } else {
                 costCenterBreakupService.updateCostCenterBreakups(costCenterList, getUserPoid());
            }
        }
        if (!billwiseList.isEmpty()) {
            if(isNewRecord){
                 billwiseBreakupService.insertBillwiseBreakup(billwiseList);
            } else {
                 billwiseBreakupService.updateBillwiseBreakups(billwiseList, getUserPoid());
            }
        }
    }

    @Override
    @Transactional
    public RecurringJvCreateResponse updateRecurringJv(Long transactionPoid, RecurringJvRequest request, String docId) {
        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(
                        () -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_TRANSACTION_POID, transactionPoid));

        // Create a copy of the existing entity for logging
        GlRecurringJvHdr oldEntity = new GlRecurringJvHdr();
        BeanUtils.copyProperties(header, oldEntity);

        Long createdScheduleCount = monthDtlRepository.countCreatedSchedulesByTransactionPoid(transactionPoid);
        if (createdScheduleCount > 0) {
            throw new IllegalStateException("Cannot update recurring JV with created JVs in schedule");
        }
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            validateRequest(request);
        }

        header.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : DateUtil.getCurrentDateInUserTimeZone());
        header.setNarration(request.getNarration());
        header.setStartDate(request.getStartDate());
        header.setTotalAmount(request.getTotalAmount());
        header.setNoOfMonths(request.getNoOfMonths());
        header.setMonthWiseAmt(request.getMonthWiseAmount());
        header.setRefType(request.getRefType());
        header.setEmployeePoid(request.getEmployeePoid());
        header.setFaPoid(request.getAssetPoid());
        header.setPolicyNumber(request.getPolicyNumber());
        header.setRemarks(request.getRemarks());
        hdrRepository.save(header);

        saveDetails(transactionPoid, request.getDetails(), header, false);
        updateScheduleDetails(transactionPoid, request.getScheduleDetails());
        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, header, GlRecurringJvHdr.class,
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return new RecurringJvCreateResponse(transactionPoid, "Recurring JV updated successfully");
    }

    private void updateScheduleDetails(Long transactionPoid, List<RecurringJVScheduleWiseDetailsDto> scheduleDetailsRequest) {
        String docId = UserContext.getDocumentId();
        if (scheduleDetailsRequest == null || scheduleDetailsRequest.isEmpty()) {
            return;
        }

        List<GlRecurringJvMonthDtl> detailsToPersist = new ArrayList<>();
        List<LogRequestDto<GlRecurringJvMonthDtl>> logRequests = new ArrayList<>();

        for (RecurringJVScheduleWiseDetailsDto dto : scheduleDetailsRequest) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : "";
            if ("ISUPDATED".equalsIgnoreCase(action)) {
                monthDtlRepository.findById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()))
                    .ifPresent(existing -> {
                        GlRecurringJvMonthDtl oldDetail = new GlRecurringJvMonthDtl();
                        BeanUtils.copyProperties(existing, oldDetail);

                        existing.setMonthWiseDate(dto.getMonthWiseDate());
                        existing.setRemarks(dto.getRemarks());
                        detailsToPersist.add(existing);

                        logRequests.add(new LogRequestDto<>(oldDetail, existing, GlRecurringJvMonthDtl.class, docId, transactionPoid.toString(),
                                String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId())));
                    });
            }
        }

        if (!detailsToPersist.isEmpty()) {
            monthDtlRepository.saveAll(detailsToPersist);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    @Override
    @Transactional
    public void deleteRecurringJv(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(
                        () -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_TRANSACTION_POID, transactionPoid));

        Long createdScheduleCount = monthDtlRepository.countCreatedSchedulesByTransactionPoid(transactionPoid);
        if (createdScheduleCount > 0) {
            throw new IllegalStateException("Cannot delete recurring JV with created JVs in schedule");
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                TABLE_RECURRING_JV_HDR,
                DB_COLUMN_TRANSACTION_POID,
                deleteReasonDto,
                header.getTransactionDate());

        // Log the deletion
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, DOC_ID_RECURRING_JV, transactionPoid.toString());
    }

    @Override
    @Transactional
    public CreateScheduleResponse createSchedule(Long transactionPoid, CreateScheduleRequest request) {

        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(
                        () -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_TRANSACTION_POID, transactionPoid));

        if (FLAG_YES.equals(header.getDeleted())) {
            throw new IllegalStateException("Cannot create schedule for deleted recurring JV");
        }

        List<GlRecurringJvDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        if (details.isEmpty()) {
            throw new IllegalStateException("No JV details exist for this recurring JV");
        }

        boolean hasDebit = details.stream()
                .anyMatch(d -> d.getDrAmt() != null && d.getDrAmt().compareTo(BigDecimal.ZERO) > 0);
        boolean hasCredit = details.stream()
                .anyMatch(d -> d.getCrAmt() != null && d.getCrAmt().compareTo(BigDecimal.ZERO) > 0);

        if (!hasDebit || !hasCredit) {
            throw new IllegalStateException("Recurring JV must have both debit and credit entries");
        }

        BigDecimal calculatedMonthWiseAmt = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getNoOfMonths()), 3, java.math.RoundingMode.HALF_UP);

        BigDecimal drTotal = request.getDetails().stream()
                .map(d -> d.getDrAmt() != null ? d.getDrAmt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (drTotal.compareTo(calculatedMonthWiseAmt) != 0) {
            throw new IllegalStateException("MonthWiseAmt must equal DrAmt from detail lines");
        }

        List<GlRecurringJvMonthDtl> existingSchedule = monthDtlRepository.findByTransactionPoid(transactionPoid);
        if (!existingSchedule.isEmpty()) {
            throw new IllegalStateException("Schedule already exists for this recurring JV");
        }

        try {
            procRepository.createSchedule(header.getGroupPoid(), getUserPoid(), header.getCompanyPoid(),
                    transactionPoid);
        } catch (Exception e) {
            log.error("Error calling stored procedure PROC_GL_RJV_CREATE_SCHEDULE: {}", e.getMessage(), e);
            throw new AsgException("Failed to create schedule: " + e.getMessage(), e);
        }

        List<GlRecurringJvMonthDtl> scheduleDetails = monthDtlRepository.findByTransactionPoid(transactionPoid);

        CreateScheduleResponse response = new CreateScheduleResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Schedule created successfully");
        response.setScheduleDetails(scheduleDetails.stream()
                .map(this::convertScheduleToResponse)
                .toList());

        return response;
    }

    @Override
    @Transactional
    public void deleteSchedule(Long transactionPoid) {

        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(
                        () -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_TRANSACTION_POID, transactionPoid));

        if (FLAG_YES.equals(header.getDeleted())) {
            throw new IllegalStateException("Cannot delete schedule for deleted recurring JV");
        }

        try {
            procRepository.deleteSchedule(header.getGroupPoid(), getUserPoid(), header.getCompanyPoid(),
                    transactionPoid);
        } catch (Exception e) {
            log.error("Error calling stored procedure PROC_GL_RJV_DELETE_SCHEDULE: {}", e.getMessage(), e);
            throw new AsgException("Failed to delete schedule: " + e.getMessage(), e);
        }

        log.info("Schedule deleted successfully for recurring JV: {}", transactionPoid);
    }

    private Long getNextDetRowIdForRecurring(Long transactionPoid) {
        if (transactionPoid == null) {
            return 1L;
        }
        return dtlRepository.getMaxDetRowIdByTransactionPoid(transactionPoid) + 1;
    }

    private List<CostCenterBreakupRequestDto> buildCostCenterBreakups(Long transactionPoid, Long detRowId,
            Long glPoid,
            List<CostCenterBreakupPopupRequestDto> costCenterBreakups) {
        return costCenterBreakups.stream()
                .filter(dto -> dto.getActionType() == null || !"NOCHANGES".equalsIgnoreCase(dto.getActionType()))
                .map(dto -> CostCenterBreakupRequestDto.builder()
                        .groupPoid(getGroupId())
                        .companyPoid(getCompanyId())
                        .docId(DOC_ID_RECURRING_JV)
                        .transactionPoid(transactionPoid)
                        .mainDetRowId(detRowId)
                        .glPoid(glPoid)
                        .costDetRowId(dto.getCostDetRowId())
                        .costGroup(dto.getCostGroup())
                        .costPoid(dto.getCostPoid())
                        .amount(dto.getAmount())
                        .loginUserPoid(getUserPoid())
                        .build())
                .toList();
    }

    private List<BillwiseBreakupRequestDto> buildBillwiseBreakups(Long transactionPoid, Long detRowId,
            Long glPoid, Long glCompanyPoid,
            List<BillwiseBreakupPopupRequestDto> billwiseBreakups) {
        return billwiseBreakups.stream()
                .filter(dto -> dto.getActionType() == null || !"NOCHANGES".equalsIgnoreCase(dto.getActionType()))
                .map(dto -> {
                    BigDecimal amount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;
                    BigDecimal drAmt = TYPE_DEBIT.equalsIgnoreCase(dto.getType()) ? amount : BigDecimal.ZERO;
                    BigDecimal crAmt = TYPE_CREDIT.equalsIgnoreCase(dto.getType()) ? amount : BigDecimal.ZERO;

                    return BillwiseBreakupRequestDto.builder()
                            .groupPoid(getGroupId())
                            .companyPoid(getCompanyId())
                            .docId(DOC_ID_RECURRING_JV)
                            .transactionPoid(transactionPoid)
                            .mainDetRowId(detRowId)
                            .glPoid(glPoid)
                            .glCompanyPoid(glCompanyPoid)
                            .billDetRowId(dto.getBillDetRowId())
                            .billOriginalAmount(amount)
                            .drAmt(drAmt)
                            .crAmt(crAmt)
                            .billRefType(dto.getBillRefType())
                            .billRef(dto.getBillRef())
                            .billDueDate(dto.getBillDueDate())
                            .billRemarks(dto.getBillRemarks())
                            .loginUserPoid(getUserPoid())
                            .build();
                })
                .toList();
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, DOC_ID_RECURRING_JV);
        params.put("SUB_GL", printService.load(REPORT_SUB_GL));
        params.put("SUB_SCHEDULE", printService.load(REPORT_SUB_SCHEDULE));
        JasperReport mainReport = printService.load(REPORT_MAIN);
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}
