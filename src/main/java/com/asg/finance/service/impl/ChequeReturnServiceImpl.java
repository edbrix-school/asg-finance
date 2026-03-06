package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.dto.ChequeReturnEditRequest;
import com.asg.finance.dto.ChequeReturnLoadResponseDto;
import com.asg.finance.dto.ChequeReturnRequest;
import com.asg.finance.dto.ChequeReturnResponse;
import com.asg.finance.entity.*;
import com.asg.finance.repository.ChequeReturnDetailRepository;
import com.asg.finance.repository.ChequeReturnGlDetailRepository;
import com.asg.finance.repository.ChequeReturnLoadRepository;
import com.asg.finance.repository.ChequeReturnRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.service.ChequeReturnService;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChequeReturnServiceImpl implements ChequeReturnService {

    private final ChequeReturnRepository headerRepo;
    private final ChequeReturnDetailRepository detailRepo;
    private final ChequeReturnGlDetailRepository glDetailRepo;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final ChequeReturnLoadRepository chequeReturnLoadRepository;
    private final LovDataService lovService;
    private final LoggingService loggingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String DOC_ID_CHEQUE_RETURN = "400-114";

    // ============================================================
    // CREATE
    // ============================================================
    @Override
    @Transactional
    public ChequeReturnResponse createChequeReturn(ChequeReturnRequest request) {
        validateRequest(request,false);
        Date dbDate = getCurrentDbDate();

        ChequeReturn header = ChequeReturn.builder()
                .status(defaultStatus(request.getChequeHeader().getStatus()))
                .remarks(request.getChequeHeader().getRemarks())
                .receiptNumber(request.getChequeHeader().getReceiptNumber())
                .groupPoid(UserContext.getGroupPoid())
                .companyPoid(UserContext.getCompanyPoid())
                .transactionDate(
                        request.getChequeHeader().getTransactionDate() != null
                                ? Date.from(request.getChequeHeader().getTransactionDate().atStartOfDay(ZoneId.systemDefault()).toInstant())
                                : dbDate
                )
                .docRef(request.getChequeHeader().getDocRef())
                .chequeNumber(request.getChequeHeader().getChequeNumber())
                .closeDetail(request.getChequeHeader().getCloseDetail())
                .deleted("N")
                .build();

        header = headerRepo.save(header);
        Long trnPoid = header.getTransactionPoid();
        request.getChequeHeader().setTransactionPoid(trnPoid);
        request.getChequeHeader().setDocRef(header.getDocRef());

        List<ChequeReturnDetail> detailEntities =
                buildAndSaveDetails(trnPoid, header, request.getChequeDetails(), dbDate, true);
        List<ChequeReturnGlDetail> glEntities =
                buildAndSaveGlDetails(trnPoid, header, request.getGlDetails(), dbDate, true);

        checkPostingValidations(glEntities, detailEntities);

        // Schedule after save procedure to run after transaction commit
        scheduleAfterSave(header.getTransactionPoid(), header.getDocRef());

        // Log the creation
        String key = header.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, DOC_ID_CHEQUE_RETURN, key);

        return toResponse(header, request, "Cheque Return created successfully.");
    }

    // ============================================================
    // UPDATE (LIMITED: Status/Narration/Close Details)
    // ============================================================
    @Transactional
    public ChequeReturnResponse updateChequeReturnMinimal(Long transactionPoid, ChequeReturnEditRequest request) {
        // 🔹 1. Validate header record existence
        ChequeReturn header = headerRepo.findById(transactionPoid)
                .orElseThrow(() -> new EntityNotFoundException("Cheque Return not found: " + transactionPoid));

        // 🔹 2. Check if already closed
        if ("CLOSED".equalsIgnoreCase(header.getStatus())) {
            throw new IllegalArgumentException("Cannot edit Cheque Return - already in CLOSED status");
        }
        // 🔹 3. Normalize and update status + remarks
        String status = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "OPEN";
        header.setStatus(status);
        header.setReceiptNumber(request.getReceiptNumber());
        header.setCloseDetail(request.getCloseDetail());
        headerRepo.save(header);

        // Log the update

        String logDetails = String.format("Cheque Return Closed - %s", header.getDocRef());
        loggingService.createLogSummaryEntry(DOC_ID_CHEQUE_RETURN, header.getTransactionPoid().toString(), logDetails);

        return getChequeReturn(transactionPoid);
    }

    // ============================================================
    // UPDATE V2 (FULL UPDATE WITH PAYLOAD)
    // ============================================================
    @Override
    @Transactional
    public ChequeReturnResponse updateChequeReturnV2(Long transactionPoid, ChequeReturnRequest request) {
        validateRequest(request,true);
        Date dbDate = getCurrentDbDate();

        // Validate header exists
        ChequeReturn header = headerRepo.findById(transactionPoid)
                .orElseThrow(() -> new EntityNotFoundException("Cheque Return not found: " + transactionPoid));

        // Create a copy of the existing entity for logging

        ChequeReturn oldEntity = new ChequeReturn();
        BeanUtils.copyProperties(header, oldEntity);

        // Check if already closed
        if ("CLOSED".equalsIgnoreCase(header.getStatus())) {
            throw new IllegalArgumentException("Cannot edit Cheque Return - already in CLOSED status");
        }

        // Update header (except docRef)
        header.setStatus(defaultStatus(request.getChequeHeader().getStatus()));
        header.setRemarks(request.getChequeHeader().getRemarks());
        header.setTransactionDate(
                request.getChequeHeader().getTransactionDate() != null
                        ? Date.from(request.getChequeHeader().getTransactionDate().atStartOfDay(ZoneId.systemDefault()).toInstant())
                        : header.getTransactionDate()
        );
        header.setChequeNumber(request.getChequeHeader().getChequeNumber());
        header.setReceiptNumber(request.getChequeHeader().getReceiptNumber());
        header.setCloseDetail(request.getChequeHeader().getCloseDetail());

        List<ChequeReturnDetail> detailEntities =
                buildAndSaveDetails(transactionPoid, header, request.getChequeDetails(), dbDate, false);
        List<ChequeReturnGlDetail> glEntities =
                buildAndSaveGlDetails(transactionPoid, header, request.getGlDetails(), dbDate, false);

        headerRepo.save(header);

        checkPostingValidations(glEntities, detailEntities);
        // Schedule after save procedure to run after transaction commit
        scheduleAfterSave(header.getTransactionPoid(), header.getDocRef());

        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, header, ChequeReturn.class,
                    DOC_ID_CHEQUE_RETURN, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        return toResponse(header, request, "Cheque Return updated successfully.");

    }

    // ============================================================
    // SCHEDULE AFTER SAVE LOGIC
    // ============================================================
    private void scheduleAfterSave(Long transactionPoid, String docRef) {
        // Register transaction synchronization to call after save procedure after commit
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        callAfterSaveInSeparateTransaction(transactionPoid, docRef);
                    }
                }
        );
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void callAfterSaveInSeparateTransaction(Long transactionPoid, String docRef) {
        try {
            String afterSaveStatus = callAfterSaveProcedure(transactionPoid, docRef);
            if (afterSaveStatus != null && afterSaveStatus.contains("ERROR")) {
                System.err.println("After save procedure failed: " + afterSaveStatus);
            }
        } catch (Exception e) {
            System.err.println("After save procedure execution failed: " + e.getMessage());
        }
    }

    // ============================================================
    // LOAD PROC METHOD (PROC_CHEQUE_RETURN_LOAD)
    // ============================================================
    @SuppressWarnings("unchecked")
    private Map<String, List<Object[]>> loadChequeReturnData(Long groupPoid, Long companyPoid,
                                                             String loginUser, String chequeNum) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_CHEQUE_RETURN_LOAD");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CHEQUE_NUM", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter("OUTDATA1", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_LOGIN_USER", loginUser);
        query.setParameter("P_CHEQUE_NUM", chequeNum);

        query.execute();

        Map<String, List<Object[]>> result = new HashMap<>();
        result.put("OUTDATA", query.getResultList());
        if (query.hasMoreResults()) {
            result.put("OUTDATA1", query.getResultList());
        } else {
            result.put("OUTDATA1", List.of());
        }

        return result;
    }

    // ============================================================
    // AFTER SAVE PROC METHOD (PROC_CHEQUE_RETURN_AFTER_SAVE)
    // ============================================================
    private String callAfterSaveProcedure(Long transactionPoid, String docRef) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_CHEQUE_RETURN_AFTER_SAVE");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("STATUS", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
            query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
            query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
            query.setParameter("P_DOC_ID", DOC_ID_CHEQUE_RETURN);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);
            query.setParameter("P_DOC_REF", docRef);

            query.execute();

            String status = (String) query.getOutputParameterValue("STATUS");
            return status != null ? status : "SUCCESS";

        } catch (Exception e) {
            return "ERROR: Exception during procedure execution - " + e.getMessage();
        }
    }

    // ============================================================
    // GET
    // ============================================================
    @Override
    @Transactional
    public ChequeReturnResponse getChequeReturn(Long transactionPoid) {
        ChequeReturn header = headerRepo.findById(transactionPoid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cheque Return not found or deleted: " + transactionPoid));

        List<ChequeReturnDetail> details = detailRepo.findByIdTransactionPoid(transactionPoid);
        List<ChequeReturnGlDetail> gls = glDetailRepo.findByTransactionPoid(transactionPoid);

        if (StringUtils.isBlank(header.getReceiptNumber()) && !details.isEmpty()) {
            Long refDocPoid = details.getFirst().getRefDocPoid();
            if (refDocPoid != null) {
                header.setReceiptNumber(refDocPoid.toString());
                headerRepo.save(header);
            }
        }

        ChequeReturnRequest req = new ChequeReturnRequest(
                ChequeReturnRequest.ChequeHeaderDto.builder()
                        .transactionPoid(header.getTransactionPoid())
                        .status(header.getStatus())
                        .remarks(header.getRemarks())
                        .transactionDate(header.getTransactionDate() != null ?
                                new java.sql.Timestamp(header.getTransactionDate().getTime())
                                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                        .docRef(header.getDocRef())
                        .chequeNumber(header.getChequeNumber())
                        .receiptNumber(header.getReceiptNumber())
                        .closeDetail(header.getCloseDetail())
                        .createdBy(header.getCreatedBy())
                        .createdDate(header.getCreatedDate())
                        .build(),
                details.stream().map(this::toChequeDetailDto).collect(Collectors.toList()),
                gls.stream().map(this::toGlDetailDto).collect(Collectors.toList())
        );

        return toResponse(header, req, "Fetched successfully");
    }


    // ============================================================
    // SOFT DELETE
    // ============================================================
    @Override
    @Transactional
    public void softDeleteChequeReturn(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        ChequeReturn existing = headerRepo.findById(transactionPoid)
                .orElseThrow(() -> new EntityNotFoundException("Cheque Return not found: " + transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_CHEQUE_RETURN_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                null
        );
    }

    // ============================================================
    // HELPERS & VALIDATIONS (existing)
    // ============================================================
    private Date getCurrentDbDate() {
        return jdbcTemplate.queryForObject("SELECT SYSDATE FROM DUAL", Date.class);
    }

    private String defaultStatus(String status) {
        return StringUtils.isBlank(status) ? "OPEN" : status.trim().toUpperCase();
    }

    private String safeMsg(String msg) {
        return msg == null ? "" : msg.trim();
    }


    // ============================================================
    // VALIDATION
    // ============================================================
    private void validateRequest(ChequeReturnRequest request, boolean isUpdate) {

        double detailsTotal = request.getChequeDetails().stream()
                .filter(detail ->
                        !isUpdate ||
                                detail.getActionType() == null ||
                                !"isdeleted".equalsIgnoreCase(detail.getActionType())
                )
                .mapToDouble(ChequeReturnRequest.ChequeDetailDto::getAmount)
                .sum();

        double glDrTotal = request.getGlDetails().stream()
                .filter(gl ->
                        "DR".equalsIgnoreCase(gl.getType()) &&
                                (!isUpdate ||
                                        gl.getActionType() == null ||
                                        !"isdeleted".equalsIgnoreCase(gl.getActionType()))
                )
                .mapToDouble(ChequeReturnRequest.GlDetailDto::getAmount)
                .sum();

        log.info("details total -> {} gldr detail -> {}", detailsTotal, glDrTotal);

        if (Math.round(detailsTotal * 100.0) != Math.round(glDrTotal * 100.0)) {
            throw new IllegalArgumentException(
                    "Total of cheque details must match total GL DR amount"
            );
        }
    }
    // ============================================================
    // BUILD METHODS
    // ============================================================
    private List<ChequeReturnDetail> buildAndSaveDetails(Long trnPoid, ChequeReturn header,
                                                         List<ChequeReturnRequest.ChequeDetailDto> dtos,
                                                         Date dbDate, boolean freshInsert) {
        if (freshInsert) {
            // Original create logic
            long i = 1;
            List<ChequeReturnDetail> entities = new ArrayList<>();
            for (ChequeReturnRequest.ChequeDetailDto d : dtos) {
                Long detId = i++;
                d.setDetRowId(detId);
                d.setTransactionPoid(trnPoid);
                ChequeReturnDetail entity = buildDetailEntity(trnPoid, header, d, dbDate, detId);
                entities.add(entity);
            }
            List<ChequeReturnDetail> savedDetails = detailRepo.saveAll(entities);
            savedDetails.forEach(detail -> {
                String logDetail = String.format("Row Created on Cheque Return Detail with detRowId: %s", detail.getId().getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), trnPoid.toString(), logDetail);
            });
            return savedDetails;
        } else {
            // ActionType-based logic
            return updateDetailsWithActionType(trnPoid, header, dtos, dbDate);
        }
    }

    private List<ChequeReturnDetail> updateDetailsWithActionType(Long trnPoid, ChequeReturn header,
                                                                 List<ChequeReturnRequest.ChequeDetailDto> dtos,
                                                                 Date dbDate) {
        String docId = DOC_ID_CHEQUE_RETURN;
        String docKeyPoid = trnPoid.toString();

        List<ChequeReturnDetail> toSave = new ArrayList<>();
        List<ChequeReturnDetail> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ChequeReturnDetail>> logRequests = new ArrayList<>();


        Long maxDetRowId = detailRepo.countById_TransactionPoid(trnPoid);

        for (ChequeReturnRequest.ChequeDetailDto d : dtos) {
            String action = d.getActionType() != null ? d.getActionType().toUpperCase() : "NOCHANGE";
            switch (action) {
                case "ISDELETED":
                    toDelete.add(d.getDetRowId());
                    break;
                case "ISCREATED":
                    Long detId = d.getDetRowId() != null ? d.getDetRowId() : ++maxDetRowId;
                    d.setDetRowId(detId);
                    d.setTransactionPoid(trnPoid);
                    ChequeReturnDetail newDetail = buildDetailEntity(trnPoid, header, d, dbDate, detId);
                    toSave.add(newDetail);
                    break;
                case "ISUPDATED":
                    ChequeReturnDetail existing = detailRepo.findById(new ChequeReturnDetailId(trnPoid, d.getDetRowId()))
                            .orElseThrow(() -> new EntityNotFoundException("Detail not found: " + d.getDetRowId()));
                    ChequeReturnDetail oldDetail = new ChequeReturnDetail();
                    BeanUtils.copyProperties(existing, oldDetail);
                    updateDetailEntity(existing, d, dbDate);
                    toUpdate.add(existing);
                    
                    String logUpdateDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", trnPoid, d.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldDetail, existing, ChequeReturnDetail.class, docId, docKeyPoid, logUpdateDetail));
                    break;
                case "NOCHANGE":
                default:
                    break;
            }
        }

        List<ChequeReturnDetail> allDetails = new ArrayList<>();

        if (!toDelete.isEmpty()) {
            toDelete.forEach(id -> detailRepo.deleteById(new ChequeReturnDetailId(trnPoid, id)));
            toDelete.forEach(entity -> loggingService.logDelete(entity, UserContext.getDocumentId(),
                    trnPoid.toString()));
        }
        if (!toSave.isEmpty()) {
            List<ChequeReturnDetail> saved = detailRepo.saveAll(toSave);
            allDetails.addAll(saved);
            saved.forEach(detail -> {
                String logDetail = String.format("Row Created on Cheque Return Detail with detRowId: %s", detail.getId().getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            List<ChequeReturnDetail> updated = detailRepo.saveAll(toUpdate);
            allDetails.addAll(updated);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (allDetails.isEmpty()) {
            allDetails = detailRepo.findByIdTransactionPoid(trnPoid);
        }
        
        return allDetails;
    }

    private ChequeReturnDetail buildDetailEntity(Long trnPoid, ChequeReturn header,
                                                 ChequeReturnRequest.ChequeDetailDto d,
                                                 Date dbDate, Long detId) {
        return ChequeReturnDetail.builder()
                .id(new ChequeReturnDetailId(trnPoid, detId))
                .paymentMainPoid(d.getPaymentMainPoid())
                .amount(d.getAmount())
                .choPoid(d.getChoPoid())
                .choDate(d.getChoDate())
                .refDocId(d.getRefDocId())
                .refDocPoid(d.getRefDocPoid())
                .pymtType(d.getPymtType())
                .chqCardNo(d.getChqCardNo())
                .chqDate(d.getChqDate())
                .bankPoid(d.getBankPoid())
                .addressPoid(d.getAddressPoid())
                .chqAcName(d.getChqAcName())
                .chqAcNo(d.getChqAcNo())
                .remarks(d.getRemarks())
                .status("OPEN")
                .voucherType(StringUtils.defaultIfBlank(d.getVoucherType(), "NORMAL"))
                .rcpDate(d.getRcpDate())
                .refDocRef(d.getRefDocRef())
                .build();
    }

    private void updateDetailEntity(ChequeReturnDetail entity, ChequeReturnRequest.ChequeDetailDto d, Date dbDate) {
        entity.setPaymentMainPoid(d.getPaymentMainPoid());
        entity.setAmount(d.getAmount());
        entity.setChoPoid(d.getChoPoid());
        entity.setChoDate(d.getChoDate());
        entity.setRefDocId(d.getRefDocId());
        entity.setRefDocPoid(d.getRefDocPoid());
        entity.setPymtType(d.getPymtType());
        entity.setChqCardNo(d.getChqCardNo());
        entity.setChqDate(d.getChqDate());
        entity.setBankPoid(d.getBankPoid());
        entity.setAddressPoid(d.getAddressPoid());
        entity.setChqAcName(d.getChqAcName());
        entity.setChqAcNo(d.getChqAcNo());
        entity.setRemarks(d.getRemarks());
        entity.setVoucherType(StringUtils.defaultIfBlank(d.getVoucherType(), "NORMAL"));
        entity.setRcpDate(d.getRcpDate());
        entity.setRefDocRef(d.getRefDocRef());
    }

    private List<ChequeReturnGlDetail> buildAndSaveGlDetails(Long trnPoid, ChequeReturn header,
                                                             List<ChequeReturnRequest.GlDetailDto> dtos,
                                                             Date dbDate, boolean freshInsert) {
        if (freshInsert) {
            long i = 1;
            List<ChequeReturnGlDetail> entities = new ArrayList<>();
            for (ChequeReturnRequest.GlDetailDto g : dtos) {
                Long detId = i++;
                g.setDetRowId(detId);
                g.setTransactionPoid(trnPoid);
                ChequeReturnGlDetail entity = buildGlDetailEntity(trnPoid, header, g, dbDate, detId);
                entities.add(entity);
            }
            List<ChequeReturnGlDetail> savedGlDetails = glDetailRepo.saveAll(entities);
            savedGlDetails.forEach(glDetail -> {
                String logDetail = String.format("Row Created on Cheque Return GL Detail with detRowId: %s", glDetail.getDetRowId());
                loggingService.createLogSummaryEntry(DOC_ID_CHEQUE_RETURN, trnPoid.toString(), logDetail);
            });
            return savedGlDetails;
        } else {
            return updateGlDetailsWithActionType(trnPoid, header, dtos, dbDate);
        }
    }

    private List<ChequeReturnGlDetail> updateGlDetailsWithActionType(Long trnPoid, ChequeReturn header,
                                                                      List<ChequeReturnRequest.GlDetailDto> dtos,
                                                                      Date dbDate) {
        String docId = DOC_ID_CHEQUE_RETURN;
        String docKeyPoid = trnPoid.toString();
        
        List<ChequeReturnGlDetail> toSave = new ArrayList<>();
        List<ChequeReturnGlDetail> toUpdate = new ArrayList<>();
        List<ChequeReturnGlDetail> toDelete = new ArrayList<>();
        List<ChequeReturnGlDetail> noChangeRecords = new ArrayList<>();
        List<LogRequestDto<ChequeReturnGlDetail>> logRequests = new ArrayList<>();

        Long maxDetRowId = glDetailRepo.countByTransactionPoid(trnPoid);
        
        for (ChequeReturnRequest.GlDetailDto g : dtos) {
            String action = g.getActionType() != null ? g.getActionType().toUpperCase() : "NOCHANGE";
            switch (action) {

                case "ISDELETED":
                    if (g.getDetRowId() != null) {

                        ChequeReturnGlDetail entity = glDetailRepo
                                .findByTransactionPoidAndDetRowId(trnPoid, g.getDetRowId())
                                .orElseThrow(() -> new ResourceNotFoundException("GL Details", "DetRowId",
                                        g.getDetRowId()));
                        toDelete.add(entity);
                    } else {
                        throw new ValidationException("GL Detail DetRowId is null");
                    }
                              break;

                case "ISCREATED":
                    Long detId = g.getDetRowId() != null ? g.getDetRowId() : ++maxDetRowId;
                    g.setDetRowId(detId);
                    g.setTransactionPoid(trnPoid);
                    ChequeReturnGlDetail newDetail = buildGlDetailEntity(trnPoid, header, g, dbDate, detId);
                    toSave.add(newDetail);
                    break;
                case "ISUPDATED":
                    ChequeReturnGlDetail existing = glDetailRepo.findById(new ChequeReturnGlDetailId(trnPoid, g.getDetRowId()))
                            .orElseThrow(() -> new EntityNotFoundException("GL Detail not found: " + g.getDetRowId()));
                    ChequeReturnGlDetail oldGlDetail = new ChequeReturnGlDetail();
                    BeanUtils.copyProperties(existing, oldGlDetail);
                    updateGlDetailEntity(existing, g, dbDate);
                    toUpdate.add(existing);
                    
                    String logUpdateDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", oldGlDetail.getTransactionPoid(), g.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldGlDetail, existing, ChequeReturnGlDetail.class, docId, docKeyPoid, logUpdateDetail));
                    break;
                case "NOCHANGE":
                default:
                    if (g.getDetRowId() != null) {
                        glDetailRepo.findById(new ChequeReturnGlDetailId(trnPoid, g.getDetRowId()))
                                .ifPresent(noChangeRecords::add);
                    }
                    break;
            }
        }
        
        List<ChequeReturnGlDetail> allGlDetails = new ArrayList<>();

        if (!toDelete.isEmpty()) {
            glDetailRepo.deleteAll(toDelete);
            toDelete.forEach(entity -> loggingService.logDelete(entity, UserContext.getDocumentId(),
                    trnPoid.toString()));
        }


        if (!toSave.isEmpty()) {
            List<ChequeReturnGlDetail> saved = glDetailRepo.saveAll(toSave);
            allGlDetails.addAll(saved);
            saved.forEach(glDetail -> {
                String logDetailForCreated = String.format("Row Created on Cheque Return GL Detail with detRowId: %s", glDetail.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetailForCreated);
            });
        }
        if (!toUpdate.isEmpty()) {
            List<ChequeReturnGlDetail> updated = glDetailRepo.saveAll(toUpdate);
            allGlDetails.addAll(updated);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        allGlDetails.addAll(noChangeRecords);

        if (allGlDetails.isEmpty()) {
            allGlDetails = glDetailRepo.findByTransactionPoid(trnPoid);
        }
        
        return allGlDetails;
    }

    private ChequeReturnGlDetail buildGlDetailEntity(Long trnPoid, ChequeReturn header,
                                                     ChequeReturnRequest.GlDetailDto g,
                                                     Date dbDate, Long detId) {
        double dr = "DR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d;
        double cr = "CR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d;
        return ChequeReturnGlDetail.builder()
                .detRowId(detId)
                .transactionPoid(trnPoid)
                .type(g.getType().toUpperCase())
                .companyPoid(g.getCompanyPoid())
                .glPoid(g.getGlPoid())
                .drAmt(dr)
                .crAmt(cr)
                .remarks(g.getRemarks())
                .build();
    }

    private void updateGlDetailEntity(ChequeReturnGlDetail entity, ChequeReturnRequest.GlDetailDto g, Date dbDate) {
        entity.setType(g.getType().toUpperCase());
        entity.setCompanyPoid(g.getCompanyPoid());
        entity.setGlPoid(g.getGlPoid());
        entity.setDrAmt("DR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d);
        entity.setCrAmt("CR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d);
        entity.setRemarks(g.getRemarks());
    }

    // ============================================================
    // RECONCILE METHODS
    // ============================================================
    private void reconcileDetails(Long trnPoid, ChequeReturn header,
                                  List<ChequeReturnRequest.ChequeDetailDto> incoming, Date dbDate) {
        Map<Long, ChequeReturnRequest.ChequeDetailDto> incomingById = incoming.stream()
                .filter(x -> x.getDetRowId() != null)
                .collect(Collectors.toMap(ChequeReturnRequest.ChequeDetailDto::getDetRowId, x -> x));

        List<ChequeReturnDetail> existing = detailRepo.findByIdTransactionPoid(trnPoid);

        for (ChequeReturnDetail e : existing) {
            ChequeReturnRequest.ChequeDetailDto d = incomingById.get(e.getId().getDetRowId());
            if (d != null) {
                e.setPymtType(d.getPymtType());
                e.setChqCardNo(d.getChqCardNo());
                e.setChqDate(d.getChqDate());
                e.setBankPoid(d.getBankPoid());
                e.setChqAcName(d.getChqAcName());
                e.setChqAcNo(d.getChqAcNo());
                e.setAmount(d.getAmount());
                e.setRemarks(d.getRemarks());
                e.setVoucherType(StringUtils.defaultIfBlank(d.getVoucherType(), "NORMAL"));
            }
        }
        detailRepo.saveAll(existing);

        // Delete missing
        Set<Long> incomingIds = incomingById.keySet();
        List<ChequeReturnDetail> toDelete = existing.stream()
                .filter(e -> !incomingIds.contains(e.getId().getDetRowId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) detailRepo.deleteAll(toDelete);

        // Insert new
        List<ChequeReturnRequest.ChequeDetailDto> toInsert = incoming.stream()
                .filter(x -> x.getDetRowId() == null).collect(Collectors.toList());
        if (!toInsert.isEmpty()) buildAndSaveDetails(trnPoid, header, toInsert, dbDate, false);
    }

    private void reconcileGlDetails(Long trnPoid, ChequeReturn header,
                                    List<ChequeReturnRequest.GlDetailDto> incoming, Date dbDate) {
        Map<Long, ChequeReturnRequest.GlDetailDto> incomingById = incoming.stream()
                .filter(x -> x.getDetRowId() != null)
                .collect(Collectors.toMap(ChequeReturnRequest.GlDetailDto::getDetRowId, x -> x));

        List<ChequeReturnGlDetail> existing = glDetailRepo.findByTransactionPoid(trnPoid);

        for (ChequeReturnGlDetail e : existing) {
            ChequeReturnRequest.GlDetailDto g = incomingById.get(e.getDetRowId());
            if (g != null) {
                e.setType(g.getType().toUpperCase());
                e.setCompanyPoid(g.getCompanyPoid());
                e.setGlPoid(g.getGlPoid());
                e.setDrAmt("DR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d);
                e.setCrAmt("CR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d);
                e.setRemarks(g.getRemarks());
            }
        }
        glDetailRepo.saveAll(existing);

        Set<Long> incomingIds = incomingById.keySet();
        List<ChequeReturnGlDetail> toDelete = existing.stream()
                .filter(e -> !incomingIds.contains(e.getDetRowId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) glDetailRepo.deleteAll(toDelete);

        List<ChequeReturnRequest.GlDetailDto> toInsert = incoming.stream()
                .filter(x -> x.getDetRowId() == null).collect(Collectors.toList());
        if (!toInsert.isEmpty()) buildAndSaveGlDetails(trnPoid, header, toInsert, dbDate, false);
    }


    // ============================================================
    // VALIDATION CHECKS
    // ============================================================
    private void checkPostingValidations(List<ChequeReturnGlDetail> glEntities,
                                         List<ChequeReturnDetail> detailEntities) {
        double glDr = glEntities.stream().mapToDouble(e -> Optional.ofNullable(e.getDrAmt()).orElse(0d)).sum();
        double glCr = glEntities.stream().mapToDouble(e -> Optional.ofNullable(e.getCrAmt()).orElse(0d)).sum();
        double detailTotal = detailEntities.stream().mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0d)).sum();

        if (Math.round(detailTotal * 100.0) != Math.round(glDr * 100.0)) {
            throw new IllegalArgumentException("Cheque details total must match GL DR total");
        }
        if (Math.round(glDr * 100.0) != Math.round(glCr * 100.0)) {
            throw new IllegalArgumentException("GL debit and credit totals must be equal");
        }
    }

    // ============================================================
    // MAPPERS
    // ============================================================
    private ChequeReturnRequest.ChequeDetailDto toChequeDetailDto(ChequeReturnDetail e) {
        ChequeReturnRequest.ChequeDetailDto dto =
                ChequeReturnRequest.ChequeDetailDto.builder()
        //return ChequeReturnRequest.ChequeDetailDto.builder()
                .detRowId(e.getId() != null ? e.getId().getDetRowId() : null)
                .transactionPoid(e.getId() != null ? e.getId().getTransactionPoid() : null)
                .paymentMainPoid(e.getPaymentMainPoid())
                .amount(e.getAmount())
                .choPoid(e.getChoPoid())
                .choDate(e.getChoDate())
                .refDocId(e.getRefDocId())
                .refDocPoid(e.getRefDocPoid())
                .pymtType(e.getPymtType())
                .chqCardNo(e.getChqCardNo())
                .chqDate(e.getChqDate())
                .bankPoid(e.getBankPoid())
                .addressPoid(e.getAddressPoid())
                .chqAcName(e.getChqAcName())
                .chqAcNo(e.getChqAcNo())
                .remarks(e.getRemarks())
                .voucherType(e.getVoucherType())
                .rcpDate(e.getRcpDate())
                .refDocRef(e.getRefDocRef())
                .build();
        dto.setBankDtl(
                lovService.getDetailsByPoidAndLovName(dto.getBankPoid(), "CUSTOMER_BANK_MASTER")
        );
        return dto;
    }

    private ChequeReturnRequest.GlDetailDto toGlDetailDto(ChequeReturnGlDetail e) {
        String type;
        double dr = Optional.ofNullable(e.getDrAmt()).orElse(0d);
        double cr = Optional.ofNullable(e.getCrAmt()).orElse(0d);
        if (dr > 0) type = "DR"; else if (cr > 0) type = "CR"; else type = "DR";
        ChequeReturnRequest.GlDetailDto gl = ChequeReturnRequest.GlDetailDto.builder()
                .transactionPoid(e.getTransactionPoid())
                .detRowId(e.getDetRowId())
                .companyPoid(e.getCompanyPoid())
                .type(type)
                .glPoid(e.getGlPoid())
                .amount(dr > 0 ? dr : cr)
                .remarks(e.getRemarks())
                .build();
        gl.setCompanyDtl(
                lovService.getDetailsByPoidAndLovNameFast(gl.getCompanyPoid(), "COMPANY")
        );

        gl.setGlDtl(
                lovService.getDetailsByPoidAndLovNameFast(gl.getGlPoid(), "GL_MASTER_LEDGERS")
        );
        return gl;
    }

    private ChequeReturnResponse toResponse(ChequeReturn header, ChequeReturnRequest request, String msg) {
        // Update the request header with saved entity's audit fields
        request.getChequeHeader().setCreatedBy(header.getCreatedBy());
        request.getChequeHeader().setCreatedDate(header.getCreatedDate());
        request.getChequeHeader().setLastModifiedBy(header.getLastModifiedBy());
        request.getChequeHeader().setLastModifiedDate(header.getLastModifiedDate());

        return ChequeReturnResponse.builder()
                .chequeHeader(request.getChequeHeader())
                .chequeDetails(request.getChequeDetails())
                .glDetails(request.getGlDetails())
                .build();
    }

    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "CHEQUE_NUMBER",
                "TRANSACTION_POID");


        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public ChequeReturnLoadResponseDto loadChequeData(String chequeNumber, String receiptNo) {
        return chequeReturnLoadRepository.loadChequeReturnDto(chequeNumber, receiptNo);
    }
}
