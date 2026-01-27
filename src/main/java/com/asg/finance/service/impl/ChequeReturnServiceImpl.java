package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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
        validateRequest(request);
        Date dbDate = getCurrentDbDate();

        ChequeReturn header = ChequeReturn.builder()
                .status(defaultStatus(request.getChequeHeader().getStatus()))
                .remarks(request.getChequeHeader().getRemarks())
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
                .createdBy(UserContext.getUserId())
                .lastModifiedBy(UserContext.getUserId())
                .createdDate(dbDate)
                .lastModifiedDate(dbDate)
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

        // Create a copy of the existing entity for logging
        ChequeReturn oldEntity = new ChequeReturn();
        BeanUtils.copyProperties(header, oldEntity);

        // 🔹 2. Check if already closed
        if ("CLOSED".equalsIgnoreCase(header.getStatus())) {
            throw new IllegalArgumentException("Cannot edit Cheque Return - already in CLOSED status");
        }
        // 🔹 3. Normalize and update status + remarks
        String status = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "OPEN";
        header.setStatus(status);
        header.setCloseDetail(request.getCloseDetail());
        header.setLastModifiedDate(getCurrentDbDate());
        header.setLastModifiedBy(UserContext.getUserId());
        headerRepo.save(header);
        
        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, header, ChequeReturn.class, 
                DOC_ID_CHEQUE_RETURN, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        return getChequeReturn(transactionPoid);
    }

    // ============================================================
    // UPDATE V2 (FULL UPDATE WITH PAYLOAD)
    // ============================================================
    @Override
    @Transactional
    public ChequeReturnResponse updateChequeReturnV2(Long transactionPoid, ChequeReturnRequest request) {
        validateRequest(request);
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
        header.setCloseDetail(request.getChequeHeader().getCloseDetail());
        header.setLastModifiedDate(dbDate);
        header.setLastModifiedBy(UserContext.getUserId());

        // Delete existing child records
        detailRepo.deleteById_TransactionPoid(transactionPoid);
        glDetailRepo.deleteById_TransactionPoid(transactionPoid);

        // Insert new child records
        List<ChequeReturnDetail> detailEntities =
                buildAndSaveDetails(transactionPoid, header, request.getChequeDetails(), dbDate, false);
        List<ChequeReturnGlDetail> glEntities =
                buildAndSaveGlDetails(transactionPoid, header, request.getGlDetails(), dbDate, false);

        checkPostingValidations(glEntities, detailEntities);
        headerRepo.save(header);
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
                .filter(h -> h.getDeleted() == null || "N".equalsIgnoreCase(h.getDeleted()))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cheque Return not found or deleted: " + transactionPoid));

        List<ChequeReturnDetail> details = detailRepo.findByChequeReturn_TransactionPoid(transactionPoid);
        List<ChequeReturnGlDetail> gls = glDetailRepo.findByChequeReturn_TransactionPoid(transactionPoid);

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
    private void validateRequest(ChequeReturnRequest request) {
        // Business logic validation only - field validations are handled by Bean Validation
        double detailsTotal = request.getChequeDetails().stream().mapToDouble(ChequeReturnRequest.ChequeDetailDto::getAmount).sum();
        double glDrTotal = request.getGlDetails().stream()
                .filter(x -> "DR".equalsIgnoreCase(x.getType()))
                .mapToDouble(ChequeReturnRequest.GlDetailDto::getAmount).sum();

        if (Math.round(detailsTotal * 100.0) != Math.round(glDrTotal * 100.0)) {
            throw new IllegalArgumentException("Total of cheque details must match total GL DR amount");
        }
    }

    // ============================================================
    // BUILD METHODS
    // ============================================================
    private List<ChequeReturnDetail> buildAndSaveDetails(Long trnPoid, ChequeReturn header,
                                                         List<ChequeReturnRequest.ChequeDetailDto> dtos,
                                                         Date dbDate, boolean freshInsert) {
        long startIndex = freshInsert ? 1 : (detailRepo.countById_TransactionPoid(trnPoid) + 1);
        List<ChequeReturnDetail> entities = new ArrayList<>();
        long i = startIndex;

        for (ChequeReturnRequest.ChequeDetailDto d : dtos) {
            Long detId = freshInsert ? i++ : (d.getDetRowId() != null ? d.getDetRowId() : i++);
            d.setDetRowId(detId);
            d.setTransactionPoid(trnPoid);
            ChequeReturnDetail entity = ChequeReturnDetail.builder()
                    .id(new ChequeReturnDetailId(trnPoid, detId))
                    .chequeReturn(header)
                    .paymentMainPoid(d.getPaymentMainPoid())
                    .amount(d.getAmount())
                    .choPoid(d.getChoPoid())
                    .choDate(d.getChoDate() != null ? Date.from(d.getChoDate().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null)
                    .refDocId(d.getRefDocId())
                    .refDocPoid(d.getRefDocPoid())
                    .pymtType(d.getPymtType())
                    .chqCardNo(d.getChqCardNo())
                    .chqDate(Date.from(d.getChqDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .bankPoid(d.getBankPoid())
                    .addressPoid(d.getAddressPoid())
                    .chqAcName(d.getChqAcName())
                    .chqAcNo(d.getChqAcNo())
                    .remarks(d.getRemarks())
                    .status("OPEN")
                    .voucherType(StringUtils.defaultIfBlank(d.getVoucherType(), "NORMAL"))
                    .rcpDate(Date.from(d.getRcpDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .refDocRef(d.getRefDocRef())
                    .createdDate(dbDate)
                    .lastModifiedDate(dbDate)
                    .createdBy(UserContext.getUserId())
                    .lastModifiedBy(UserContext.getUserId())
                    .build();
            entities.add(entity);
        }
        return detailRepo.saveAll(entities);
    }

    private List<ChequeReturnGlDetail> buildAndSaveGlDetails(Long trnPoid, ChequeReturn header,
                                                             List<ChequeReturnRequest.GlDetailDto> dtos,
                                                             Date dbDate, boolean freshInsert) {
        long startIndex = freshInsert ? 1 : (glDetailRepo.countById_TransactionPoid(trnPoid) + 1);
        List<ChequeReturnGlDetail> entities = new ArrayList<>();
        long i = startIndex;

        for (ChequeReturnRequest.GlDetailDto g : dtos) {
            Long detId = freshInsert ? i++ : (g.getDetRowId() != null ? g.getDetRowId() : i++);
            g.setDetRowId(detId);
            g.setTransactionPoid(trnPoid);
            double dr = "DR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d;
            double cr = "CR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d;

            ChequeReturnGlDetail entity = ChequeReturnGlDetail.builder()
                    .id(new ChequeReturnGlDetailId(trnPoid, detId))
                    .chequeReturn(header)
                    .type(g.getType().toUpperCase())
                    .companyPoid(g.getCompanyPoid())
                    .glPoid(g.getGlPoid())
                    .drAmt(dr)
                    .crAmt(cr)
                    .remarks(g.getRemarks())
                    .createdDate(dbDate)
                    .lastModifiedDate(dbDate)
                    .createdBy(UserContext.getUserId())
                    .lastModifiedBy(UserContext.getUserId())
                    .build();
            entities.add(entity);
        }
        return glDetailRepo.saveAll(entities);
    }

    // ============================================================
    // RECONCILE METHODS
    // ============================================================
    private void reconcileDetails(Long trnPoid, ChequeReturn header,
                                  List<ChequeReturnRequest.ChequeDetailDto> incoming, Date dbDate) {
        Map<Long, ChequeReturnRequest.ChequeDetailDto> incomingById = incoming.stream()
                .filter(x -> x.getDetRowId() != null)
                .collect(Collectors.toMap(ChequeReturnRequest.ChequeDetailDto::getDetRowId, x -> x));

        List<ChequeReturnDetail> existing = detailRepo.findByChequeReturn_TransactionPoid(trnPoid);

        for (ChequeReturnDetail e : existing) {
            ChequeReturnRequest.ChequeDetailDto d = incomingById.get(e.getId().getDetRowId());
            if (d != null) {
                e.setPymtType(d.getPymtType());
                e.setChqCardNo(d.getChqCardNo());
                e.setChqDate(Date.from(d.getChqDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                e.setBankPoid(d.getBankPoid());
                e.setChqAcName(d.getChqAcName());
                e.setChqAcNo(d.getChqAcNo());
                e.setAmount(d.getAmount());
                e.setRemarks(d.getRemarks());
                e.setVoucherType(StringUtils.defaultIfBlank(d.getVoucherType(), "NORMAL"));
                e.setLastModifiedDate(dbDate);
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

        List<ChequeReturnGlDetail> existing = glDetailRepo.findByChequeReturn_TransactionPoid(trnPoid);

        for (ChequeReturnGlDetail e : existing) {
            ChequeReturnRequest.GlDetailDto g = incomingById.get(e.getId().getDetRowId());
            if (g != null) {
                e.setType(g.getType().toUpperCase());
                e.setCompanyPoid(g.getCompanyPoid());
                e.setGlPoid(g.getGlPoid());
                e.setDrAmt("DR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d);
                e.setCrAmt("CR".equalsIgnoreCase(g.getType()) ? g.getAmount() : 0d);
                e.setRemarks(g.getRemarks());
                e.setLastModifiedDate(dbDate);
            }
        }
        glDetailRepo.saveAll(existing);

        Set<Long> incomingIds = incomingById.keySet();
        List<ChequeReturnGlDetail> toDelete = existing.stream()
                .filter(e -> !incomingIds.contains(e.getId().getDetRowId()))
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
        LocalDate chequeLocalDate = null;
        if (e.getChqDate() != null) {
            chequeLocalDate = new java.sql.Timestamp(e.getChqDate().getTime())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        LocalDate receiptLocalDate = null;
        if (e.getRcpDate() != null) {
            receiptLocalDate = new java.sql.Timestamp(e.getRcpDate().getTime())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        LocalDate choLocalDate = null;
        if (e.getChoDate() != null) {
            choLocalDate = new java.sql.Timestamp(e.getChoDate().getTime())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        ChequeReturnRequest.ChequeDetailDto dto =
                ChequeReturnRequest.ChequeDetailDto.builder()
        //return ChequeReturnRequest.ChequeDetailDto.builder()
                .detRowId(e.getId() != null ? e.getId().getDetRowId() : null)
                .transactionPoid(e.getId() != null ? e.getId().getTransactionPoid() : null)
                .paymentMainPoid(e.getPaymentMainPoid())
                .amount(e.getAmount())
                .choPoid(e.getChoPoid())
                .choDate(choLocalDate)
                .refDocId(e.getRefDocId())
                .refDocPoid(e.getRefDocPoid())
                .pymtType(e.getPymtType())
                .chqCardNo(e.getChqCardNo())
                .chqDate(chequeLocalDate)
                .bankPoid(e.getBankPoid())
                .addressPoid(e.getAddressPoid())
                .chqAcName(e.getChqAcName())
                .chqAcNo(e.getChqAcNo())
                .remarks(e.getRemarks())
                .voucherType(e.getVoucherType())
                .rcpDate(receiptLocalDate)
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
        //return ChequeReturnRequest.GlDetailDto.builder()
                .transactionPoid(e.getId() != null ? e.getId().getTransactionPoid() : null)
                .detRowId(e.getId() != null ? e.getId().getDetRowId() : null)
                .companyPoid(e.getCompanyPoid())

                .type(type)
                .glPoid(e.getGlPoid())
                .amount(dr > 0 ? dr : cr)
                .remarks(e.getRemarks())
                .build();
        gl.setCompanyDtl(
                lovService.getDetailsByPoidAndLovName(gl.getCompanyPoid(), "COMPANY")
        );

        gl.setGlDtl(
                lovService.getDetailsByPoidAndLovName(gl.getGlPoid(), "GL_MASTER_LEDGERS")
        );
        return gl;
    }

    private ChequeReturnResponse toResponse(ChequeReturn header, ChequeReturnRequest request, String msg) {
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
