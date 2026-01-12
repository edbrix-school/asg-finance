package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public Map<String, Object> listRecurringJvs(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "NARRATION",
                "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringJvResponse getRecurringJvById(Long transactionPoid) {

        GlRecurringJvHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring JV", "transactionPoid", transactionPoid));
        

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
        return total != null ? total : BigDecimal.ZERO.setScale(3);
    }

    private BigDecimal calculateCrTotal(Long transactionPoid) {
        BigDecimal total = dtlRepository.getCrTotalByTransactionPoid(transactionPoid);
        return total != null ? total : BigDecimal.ZERO.setScale(3);
    }

    private RecurringJvResponse buildResponse(
            GlRecurringJvHdr header,
            List<GlRecurringJvDtl> details,
            List<GlRecurringJvMonthDtl> scheduleDetails,
            BigDecimal drTotal,
            BigDecimal crTotal) {

        RecurringJvResponse response = new RecurringJvResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setDocRef(header.getDocRef());
        response.setTransactionDate(header.getTransactionDate() != null ? Timestamp.valueOf(header.getTransactionDate().atStartOfDay()) : null);
        response.setNarration(header.getNarration());
        response.setStartDate(header.getStartDate() != null ? Timestamp.valueOf(header.getStartDate().atStartOfDay()) : null);
        response.setTotalAmount(header.getTotalAmount());
        response.setNoOfMonths(header.getNoOfMonths());
        response.setMonthWiseAmt(header.getMonthWiseAmt());
        response.setRefType(header.getRefType());
        response.setEmployeeId(header.getEmployeePoid());
        response.setAssetId(header.getFaPoid());
        response.setPolicyNumber(header.getPolicyNumber());
        response.setRemarks(header.getRemarks());
        response.setGroupPoid(header.getGroupPoid());
        response.setCompanyPoid(header.getCompanyPoid());
        response.setDrTotal(drTotal);
        response.setCrTotal(crTotal);
        response.setGlPosting(false);



        boolean hasBillWiseCapable = details.stream()
                .anyMatch(dtl -> dtl.getGlPoid() != null &&
                    glMasterRepository.findById(dtl.getGlPoid())
                        .map(gl -> "Y".equalsIgnoreCase(gl.getBillwise()))
                        .orElse(false));
        response.setBillWiseCapable(hasBillWiseCapable);
        if (header.getEmployeePoid() != null) {
            response.setEmployeeDet(lovService.getDetailsByPoidAndLovName(header.getEmployeePoid(), "RJV_EMPLOYEE_DTLS"));
        }
        if (header.getFaPoid() != null) {
            response.setAssetDet(lovService.getDetailsByPoidAndLovName(header.getFaPoid(), "RJV_FIXED_ASSET_DTLS"));
        }
        if (header.getCompanyPoid() != null) {
            response.setCompanyDet(lovService.getDetailsByPoidAndLovName(header.getCompanyPoid(), "COMPANY"));
        }

   List<RecurringJvDetailResponse> detailResponses = details.stream()
                .map(dtl -> convertDetailToResponse(dtl, header.getEmployeePoid(), header.getFaPoid()))
                .collect(Collectors.toList());
        response.setDetails(detailResponses);
        List<RecurringJvScheduleDetailResponse> scheduleResponses = scheduleDetails.stream()
  .map(schedule -> convertScheduleToResponse(schedule, header.getDocRef()))
                .collect(Collectors.toList());
        response.setScheduleDetails(scheduleResponses);

        return response;
    }

    private RecurringJvDetailResponse convertDetailToResponse(GlRecurringJvDtl dtl,Long employeePoid,Long FaPoid) {
        RecurringJvDetailResponse response = new RecurringJvDetailResponse();
        response.setLineId(dtl.getDetRowId());
        response.setType(dtl.getType());
        response.setCompanyId(dtl.getCompanyPoid());
        response.setGlId(dtl.getGlPoid());
        response.setDrAmt(dtl.getDrAmt());
        response.setCrAmt(dtl.getCrAmt());
        response.setRemarks(dtl.getRemarks());

        if (dtl.getGlPoid() != null) {
            response.setGlDet(lovService.getDetailsByPoidAndLovName(dtl.getGlPoid(), "GL_MASTER_LEDGERS"));
        }

        
        response.setCostCenter(loadCostCenterBreakup(dtl.getTransactionPoid(), dtl.getDetRowId()));
        response.setBillWiseBreakup(loadBillWiseBreakup(dtl.getTransactionPoid(), dtl.getDetRowId()));

        return response;
    }

    private RecurringJvScheduleDetailResponse convertScheduleToResponse(GlRecurringJvMonthDtl schedule,String docRef) {
        RecurringJvScheduleDetailResponse response = new RecurringJvScheduleDetailResponse();
        response.setScheduleId(schedule.getDetRowId());
        response.setMonthWiseDate(schedule.getMonthWiseDate() != null ? Timestamp.valueOf(schedule.getMonthWiseDate().atStartOfDay()) : null);
        response.setJv(docRef);
        response.setAmount(schedule.getAmount());
        response.setStatus(schedule.getStatus());
        response.setRemarks(schedule.getRemarks());
        response.setDrilldownLinkInfo(schedule.getDrilldownLinkInfo());
        return response;
    }

    @Override
    @Transactional
    public RecurringJvCreateResponse createRecurringJv(RecurringJvRequest request, String docId) {
        validateRequest(request);

        BigDecimal monthWiseAmt = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getNoOfMonths()), 3, RoundingMode.HALF_UP);

GlRecurringJvHdr header = GlRecurringJvHdr.builder()
        .transactionDate(LocalDate.now())
        .groupPoid(getGroupId())
        .companyPoid(getCompanyId())
        .narration(request.getNarration())
        .startDate(request.getStartDate())
        .totalAmount(request.getTotalAmount())
        .noOfMonths(request.getNoOfMonths())
        .monthWiseAmt(monthWiseAmt)
        .refType(request.getRefType())
        .employeePoid(request.getEmployeeId())
        .faPoid(request.getAssetId())
        .policyNumber(request.getPolicyNumber())
        .remarks(request.getRemarks())
        .docRef(request.getDocRef())
        .deleted(FLAG_NO)
        .createdBy(getCurrentUser())
        .createdDate(LocalDateTime.now())
        .build();

        header = hdrRepository.save(header);


        Long transactionPoid = header.getTransactionPoid();
        saveDetails(transactionPoid, request.getDetails(),header,docId);
        
        // Log the creation
        String key = transactionPoid.toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);
        
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
        
        for (RecurringJvDetailRequest detail : request.getDetails()) {
            if ("Dr".equalsIgnoreCase(detail.getType())) {
                if (detail.getDrAmt() == null || detail.getDrAmt().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("DrAmt must be greater than 0 for Type=Dr");
                }
            } else if ("Cr".equalsIgnoreCase(detail.getType())) {
                if (detail.getCrAmt() == null || detail.getCrAmt().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("CrAmt must be greater than 0 for Type=Cr");
                }
            }
            
            if (!glMasterRepository.existsByGlPoid(detail.getGlId())) {
                throw new ResourceNotFoundException("GL Master", "glId", detail.getGlId());
            }
        }
    }
    
    private void saveDetails(Long transactionPoid, List<RecurringJvDetailRequest> details, GlRecurringJvHdr header,String docId) {
        Long headerCompanyPoid = header.getCompanyPoid();
        String currentUser = getCurrentUser();
        List<CostCenterBreakupRequestDto> costCenterRequestDtoList = new ArrayList<>();
        List<BillwiseBreakupRequestDto> billwiseRequestDtoList = new ArrayList<>();
        
        for (RecurringJvDetailRequest detail : details) {
            String rawAction = detail.getActionType();
            String action = (rawAction == null || rawAction.trim().isEmpty())
                    ? "NOCHANGES"
                    : rawAction.trim().toUpperCase();

            action = switch (action) {
                case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                case "ISDELETED", "DELETED" -> "ISDELETED";
                default -> "NOCHANGES";
            };

            Long companyPoid = detail.getCompanyId() != null ? detail.getCompanyId() : headerCompanyPoid;
            
            if (companyPoid == null) {
                throw new IllegalArgumentException("Company ID is required");
            }

            switch (action) {
                case ACTION_NOCHANGES -> {}
                case ACTION_ISDELETED -> {
                    if (detail.getLineId() != null) {
                        dtlRepository.deleteById(new TransactionDetailKey(transactionPoid,detail.getLineId()));
                    }
                }
                case ACTION_ISCREATED -> {
                    Long detRowId = detail.getLineId() != null ? detail.getLineId() : getNextDetRowIdForRecurring(transactionPoid);
                    GlRecurringJvDtl dtl = new GlRecurringJvDtl();
                    dtl.setTransactionPoid(transactionPoid);
                    dtl.setDetRowId(detRowId);
                    dtl.setType(detail.getType());
                    dtl.setCompanyPoid(companyPoid);
                    dtl.setGlPoid(detail.getGlId());
                    dtl.setDrAmt(detail.getDrAmt());
                    dtl.setCrAmt(detail.getCrAmt());
                    dtl.setRemarks(detail.getRemarks());
                    dtl.setCreatedBy(currentUser);
                    dtl.setCreatedDate(LocalDateTime.now());
                    dtlRepository.save(dtl);
                    
                    if (detail.getCostCenter() != null && !detail.getCostCenter().isEmpty()) {
                        costCenterRequestDtoList.addAll(buildCostCenterBreakups(transactionPoid, detRowId, docId, detail.getGlId(), detail.getCostCenter()));
                    }
                    if (detail.getBillWiseBreakup() != null && !detail.getBillWiseBreakup().isEmpty()) {
                        billwiseRequestDtoList.addAll(buildBillwiseBreakups(transactionPoid, detRowId, docId, detail.getGlId(), detail.getBillWiseBreakup()));
                    }
                }
                case ACTION_ISUPDATED -> {

                    if (!glMasterRepository.existsByGlPoid(detail.getGlId())) {
                        throw new ResourceNotFoundException("GL Master", "glId", detail.getGlId());
                    }

                    GlRecurringJvDtl dtl = dtlRepository.findById(new TransactionDetailKey(transactionPoid, detail.getLineId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Recurring JV Detail", "lineId", detail.getLineId()));
                    dtl.setType(detail.getType());
                    dtl.setCompanyPoid(companyPoid);
                    dtl.setGlPoid(detail.getGlId());
                    dtl.setDrAmt(detail.getDrAmt());
                    dtl.setCrAmt(detail.getCrAmt());
                    dtl.setRemarks(detail.getRemarks());
                    dtl.setLastModifiedBy(currentUser);
                    dtl.setLastModifiedDate(LocalDateTime.now());
                    dtlRepository.save(dtl);
                    
                    if (detail.getCostCenter() != null && !detail.getCostCenter().isEmpty()) {
                        costCenterRequestDtoList.addAll(buildCostCenterBreakups(transactionPoid, detail.getLineId(), docId, detail.getGlId(), detail.getCostCenter()));
                    }
                    if (detail.getBillWiseBreakup() != null && !detail.getBillWiseBreakup().isEmpty()) {
                        billwiseRequestDtoList.addAll(buildBillwiseBreakups(transactionPoid, detail.getLineId(), docId, detail.getGlId(), detail.getBillWiseBreakup()));
                    }
                }
            }
        }
        
        if (!costCenterRequestDtoList.isEmpty()) {
            costCenterBreakupService.updateCostCenterBreakups(costCenterRequestDtoList, getUserPoid());
        }
        if (!billwiseRequestDtoList.isEmpty()) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseRequestDtoList, getUserPoid());
        }
    }

    @Override
    @Transactional
    public RecurringJvCreateResponse updateRecurringJv(Long transactionPoid, RecurringJvRequest request,String docId) {
        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring JV", "transactionPoid", transactionPoid));
        
        // Create a copy of the existing entity for logging
        GlRecurringJvHdr oldEntity = new GlRecurringJvHdr();
        BeanUtils.copyProperties(header, oldEntity);

        Long createdScheduleCount = monthDtlRepository.countCreatedSchedulesByTransactionPoid(transactionPoid);
        if (createdScheduleCount > 0) {
            throw new IllegalStateException("Cannot update recurring JV with created JVs in schedule");
        }
        
        validateRequest(request);
        
        BigDecimal newMonthWiseAmt = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getNoOfMonths()), 3, java.math.RoundingMode.HALF_UP);
        
        header.setNarration(request.getNarration());
        header.setStartDate(request.getStartDate());
        header.setTotalAmount(request.getTotalAmount());
        header.setNoOfMonths(request.getNoOfMonths());
        header.setMonthWiseAmt(newMonthWiseAmt);
        header.setRefType(request.getRefType());
        header.setEmployeePoid(request.getEmployeeId());
        header.setFaPoid(request.getAssetId());
        header.setPolicyNumber(request.getPolicyNumber());
        header.setRemarks(request.getRemarks());
        header.setDocRef(request.getDocRef());
        header.setLastModifiedBy(getCurrentUser());
        header.setLastModifiedDate(LocalDateTime.now());
        
        hdrRepository.save(header);
        
        dtlRepository.deleteByTransactionPoid(transactionPoid);
        saveDetails(transactionPoid, request.getDetails(), header,docId);
        
        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, header, GlRecurringJvHdr.class, 
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        
        return new RecurringJvCreateResponse(transactionPoid, "Recurring JV updated successfully");
    }

    @Override
    @Transactional
    public void deleteRecurringJv(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring JV", "transactionPoid", transactionPoid));
        
        Long createdScheduleCount = monthDtlRepository.countCreatedSchedulesByTransactionPoid(transactionPoid);
        if (createdScheduleCount > 0) {
            throw new IllegalStateException("Cannot delete recurring JV with created JVs in schedule");
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_RECURRING_JV_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                header.getTransactionDate()
        );
    }

    @Override
    @Transactional
    public CreateScheduleResponse createSchedule(Long transactionPoid, CreateScheduleRequest request) {

        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring JV", "transactionPoid", transactionPoid));
        
        if ("Y".equals(header.getDeleted())) {
            throw new IllegalStateException("Cannot create schedule for deleted recurring JV");
        }
        
        List<GlRecurringJvDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        if (details.isEmpty()) {
            throw new IllegalStateException("No detail lines exist for this recurring JV");
        }
        
        boolean hasDebit = details.stream().anyMatch(d -> d.getDrAmt() != null && d.getDrAmt().compareTo(BigDecimal.ZERO) > 0);
        boolean hasCredit = details.stream().anyMatch(d -> d.getCrAmt() != null && d.getCrAmt().compareTo(BigDecimal.ZERO) > 0);
        
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
            procRepository.createSchedule(header.getGroupPoid(), getUserPoid(), header.getCompanyPoid(), transactionPoid);
        } catch (Exception e) {
            log.error("Error calling stored procedure PROC_GL_RJV_CREATE_SCHEDULE: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create schedule: " + e.getMessage(), e);
        }
        
        List<GlRecurringJvMonthDtl> scheduleDetails = monthDtlRepository.findByTransactionPoid(transactionPoid);
        
        CreateScheduleResponse response = new CreateScheduleResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Schedule created successfully");
        response.setScheduleDetails(scheduleDetails.stream()
                .map(schedule -> convertScheduleToResponse(schedule, header.getDocRef()))
                .collect(Collectors.toList()));
        
        return response;
    }

    @Override
    @Transactional
    public void deleteSchedule(Long transactionPoid) {

        GlRecurringJvHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring JV", "transactionPoid", transactionPoid));
        
        if ("Y".equals(header.getDeleted())) {
            throw new IllegalStateException("Cannot delete schedule for deleted recurring JV");
        }
        
        try {
            procRepository.deleteSchedule(header.getGroupPoid(), getUserPoid(), header.getCompanyPoid(), transactionPoid);
        } catch (Exception e) {
            log.error("Error calling stored procedure PROC_GL_RJV_DELETE_SCHEDULE: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete schedule: " + e.getMessage(), e);
        }
        
        log.info("Schedule deleted successfully for recurring JV: {}", transactionPoid);
    }

    private Long getNextDetRowIdForRecurring(Long transactionPoid) {
        Long maxId = dtlRepository.findByTransactionPoid(transactionPoid)
                .stream()
                .map(GlRecurringJvDtl::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L);
        return maxId + 1;
    }

    private List<CostCenterBreakupRequestDto> buildCostCenterBreakups(Long transactionPoid, Long detRowId, String docId, Long glPoid,
                                       List<CostCenterBreakupPopupRequestDto> costCenterBreakups) {
        return costCenterBreakups.stream()
                .map(dto -> CostCenterBreakupRequestDto.builder()
                        .groupPoid(getGroupId())
                        .companyPoid(getCompanyId())
                        .docId(docId)
                        .transactionPoid(transactionPoid)
                        .mainDetRowId(detRowId)
                        .glPoid(glPoid)
                        .costDetRowId(dto.getCostDetRowId())
                        .costGroup(dto.getCostGroup())
                        .costPoid(dto.getCostPoid())
                        .amount(dto.getAmount())
                        .loginUserPoid(getUserPoid())
                        .build())
                .collect(Collectors.toList());
    }

    private List<BillwiseBreakupRequestDto> buildBillwiseBreakups(Long transactionPoid, Long detRowId, String docId, Long glPoid,
                                     List<BillwiseBreakupPopupRequestDto> billwiseBreakups) {
        return billwiseBreakups.stream()
                .map(dto -> BillwiseBreakupRequestDto.builder()
                        .groupPoid(getGroupId())
                        .companyPoid(getCompanyId())
                        .docId(docId)
                        .transactionPoid(transactionPoid)
                        .mainDetRowId(detRowId)
                        .glPoid(glPoid)
                        .billDetRowId(dto.getBillDetRowId())
                        .billRefType(dto.getBillRefType())
                        .billRef(dto.getBillRef())
                        .billDueDate(dto.getBillDueDate())
                        .drAmt("Dr".equals(dto.getType()) ? dto.getAmount() : BigDecimal.ZERO)
                        .crAmt("Cr".equals(dto.getType()) ? dto.getAmount() : BigDecimal.ZERO)
                        .billRemarks(dto.getBillRemarks())
                        .loginUserPoid(getUserPoid())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CostCenterBreakupPopupRequestDto> loadCostCenterBreakup(Long transactionPoid, Long detRowId) {
        try {
            GlVoucherCostCenterBreakupResponseDto response = costCenterBreakupService.loadCostCenterData(
                    "400-102", transactionPoid, getGroupId(), getCompanyId(), getUserPoid());
            
            if (response != null && response.getCostBreakupList() != null) {
                return response.getCostBreakupList().stream()
                        .filter(cc -> cc.getMainDetRowId().equals(detRowId))
                        .map(cc -> {
                            CostCenterBreakupPopupRequestDto dto = new CostCenterBreakupPopupRequestDto();
                            dto.setCostDetRowId(cc.getCostDetRowId());
                            dto.setCostGroup(cc.getCostGroup());
                            dto.setCostPoid(cc.getCostPoid());
                            dto.setAmount(BigDecimal.valueOf(cc.getAmount()));
                            return dto;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to load cost center breakup for transaction: {}, detRowId: {}", transactionPoid, detRowId, e);
        }
        return Collections.emptyList();
    }

    private List<BillwiseBreakupPopupRequestDto> loadBillWiseBreakup(Long transactionPoid, Long detRowId) {
        try {
            GlVoucherLoadBillwiseBreakupResponseDto response = billwiseBreakupService.loadBillwiseBreakup(
                    getGroupId(), getCompanyId(), "400-102", transactionPoid);
            
            if (response != null && response.getLoadBillwiseBreakupResponseDtoList() != null) {
                return response.getLoadBillwiseBreakupResponseDtoList().stream()
                        .filter(bw -> bw.getMainDetRowId().equals(detRowId))
                        .map(bw -> {
                            BillwiseBreakupPopupRequestDto dto = new BillwiseBreakupPopupRequestDto();
                            dto.setBillDetRowId(bw.getBillDetRowId());
                            dto.setBillRefType(bw.getBillRefType());
                            dto.setBillRef(bw.getBillRef());
                            dto.setBillDueDate(bw.getBillDueDate());
                            dto.setAmount(bw.getDrAmt() != null ? bw.getDrAmt() : bw.getCrAmt());
                            dto.setType(bw.getDrAmt() != null && bw.getDrAmt().compareTo(BigDecimal.ZERO) > 0 ? "Dr" : "Cr");
                            dto.setBillRemarks(bw.getBillRemarks());
                            return dto;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to load billwise breakup for transaction: {}, detRowId: {}", transactionPoid, detRowId, e);
        }
        return Collections.emptyList();
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-102");
        params.put("SUB_GL", printService.load("Finance/GL/RecurringJVGLSubreport1.jrxml"));
        params.put("SUB_SCHEDULE", printService.load("Finance/GL/RecurringJVScheduleWiseSubreport2.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/RecurringJVReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }



}
