package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.client.GlPostingServiceClient;
import com.asg.finance.dto.*;
import com.asg.finance.dto.ProcessFdaRequestDto;
import com.asg.finance.entity.ArDebitNoteChargeDtl;
import com.asg.finance.entity.ArDebitNoteDtl;
import com.asg.finance.entity.ArDebitNoteHdr;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.entity.SupplierMasterEntity;
import com.asg.finance.repository.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.DebitNoteService;
import com.asg.finance.service.GlPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * DebitNoteServiceImpl updated to support:
 * - Billwise breakup (create/update/get)
 * - Cost center breakup (create/update/get)
 * <p>
 * Minimal changes only — existing logic preserved.
 * <p>
 * SRS reference (local file): /mnt/data/Accounts Receivable - Debit Note V 1.0 .pdf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebitNoteServiceImpl implements DebitNoteService {

    private final ArDebitNoteHdrRepository debitNoteHdrRepository;
    private final ArDebitNoteDtlRepository debitNoteDtlRepository;
    private final ArDebitNoteChargeDtlRepository debitNoteChargeDtlRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final SalesCustomerMasterRepository salesCustomerMasterRepository;
    private final DocumentSearchService documentService;
    private final DebitNoteCustomRepository debitNoteCustomRepository;
    private final BillwiseBreakupDtlRepository billwiseBreakupDtlRepository;
    private final ShipPrincipalMasterRepository shipPrincipalMasterRepository;

    // Breakup service beans (must exist in project)
    private final BillwiseBreakupService billwiseBreakupService;
    private final CostCenterBreakupService costCenterBreakupService;
    private final LovDataService lovService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final GlobalLogSummaryRepository globalLogSummaryRepository;
    private final DebitNoteProcedureRepository debitNoteProcedureRepository;
    private final TaxMasterRepository taxMasterRepository;
    private final GlPostingService glPostingService;
    private final GLMasterRepository glMasterRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GlobalParameterService globalParameterService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.doc-id.debit-note:300-110}")
    private String debitNoteDocId;

    @Override
    @Transactional
    @PerformGlPosting
    public DebitNoteHeaderDto createDebitNote(DebitNoteHeaderDto debitNoteDto) {

        // VALIDATION BEFORE SAVE
        preSaveValidate(debitNoteDto);
        validateDebitNoteInput(debitNoteDto);
        applyBusinessLogic(debitNoteDto);

        ArDebitNoteHdr entity = mapToEntity(debitNoteDto);
        ArDebitNoteHdr savedEntity = debitNoteHdrRepository.saveAndFlush(entity);
        // Refresh to pull back trigger-generated DOC_REF from the database
        entityManager.flush();
        entityManager.refresh(savedEntity);
        debitNoteDto.setDocRef(savedEntity.getDocRef());

        // Pre-save DB-level validations (after flush so TRANSACTION_POID is visible to procedures)
        runPreSaveProcedures(savedEntity.getTransactionPoid(), debitNoteDto);

        DocumentBeforeSaveBillwiseCostGroups(debitNoteDto);

        // Log the header creation first, before any child record processing
        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                savedEntity.getTransactionPoid().toString(),
                String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedEntity.getDocRef())
        );

        // Save details (GL + Charge) — GL will be saved if provided regardless of refType
        saveDetails(debitNoteDto, savedEntity.getTransactionPoid());

        // --- INSERT BILLWISE & COST CENTER BREAKUPS (minimal changes) ---
        insertBillwiseBreakups(debitNoteDto, savedEntity);
        insertCostCenterBreakups(debitNoteDto, savedEntity);

        // Build response by reading saved header + details from DB (so DB-generated fields are included)
        DebitNoteHeaderDto result = mapToDto(savedEntity);
        result.setCreatedBy(savedEntity.getCreatedBy());
        result.setCreatedDate(savedEntity.getCreatedDate());
        result.setLastModifiedBy(savedEntity.getLastModifiedBy());
        result.setLastModifiedDate(savedEntity.getLastModifiedDate());
        loadDetails(result, savedEntity.getTransactionPoid(), savedEntity);

        // Load breakups into response
        loadBreakups(result, savedEntity.getTransactionPoid());

        // Publish event for after-save processing (will run after transaction commit)
        publishAfterSaveEvent(savedEntity, null, null);

        DebitNoteHeaderDto finalResult = getDebitNote(savedEntity.getTransactionPoid());
        if (debitNoteDto.getWarnings() != null && !debitNoteDto.getWarnings().isEmpty()) {
            finalResult.setWarnings(debitNoteDto.getWarnings());
        }
        return finalResult;
    }

    @Override
    @Transactional
    @PerformGlPosting
    public DebitNoteHeaderDto updateDebitNote(Long transactionPoid, DebitNoteHeaderDto debitNoteDto) {
        ArDebitNoteHdr existingEntity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));

        // Create a copy of the old entity for logging
        ArDebitNoteHdr oldEntity = new ArDebitNoteHdr();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        // Store old FDA references for after-save processing
        String oldFdaRef = existingEntity.getFdaRef();
        String oldRefType = existingEntity.getRefType();

        // Release old FDA/FF job holds before applying new values (must happen before new ref is written)
        if (isFdaLike(existingEntity.getRefType())) {
            Long oldRefPoid = "FDA_DIRECT".equalsIgnoreCase(existingEntity.getRefType())
                    ? parseLongSafely(existingEntity.getFdaDirectRef())
                    : parseLongSafely(existingEntity.getFdaRef());
            if (oldRefPoid != null) {
                try {
                    debitNoteProcedureRepository.releaseJobOldValues(
                            existingEntity.getGroupPoid(), existingEntity.getCompanyPoid(),
                            UserContext.getUserPoid(), transactionPoid,
                            existingEntity.getRefType(), oldRefPoid);
                } catch (Exception e) {
                    log.warn("releaseJobOldValues failed for transaction {}: {}", transactionPoid, e.getMessage());
                }
            }
        }

        preSaveValidate(debitNoteDto);
        validateDebitNoteInput(debitNoteDto);

        if (debitNoteDto.getRefType().equals("FDA JOBS")
                || debitNoteDto.getRefType().equals("FF JOBS")
                || debitNoteDto.getRefType().equals("FDA")
                || debitNoteDto.getRefType().equals("FDA_DIRECT")
        )
            debitNoteCustomRepository.validateDebitNote(
                    debitNoteDto.getRefType(),
                    debitNoteDto.getPartyType(),
                    debitNoteDto.getPartyPoid(),
                    debitNoteDto.getFdaRefPoid(),
                    debitNoteDto.getPoRef()
            );

        applyBusinessLogic(debitNoteDto);

        BeanUtils.copyProperties(debitNoteDto, existingEntity, "transactionPoid", "createdBy", "createdDate", "groupPoid", "companyPoid", "transactionDate");

        existingEntity.setMultiCompany(debitNoteDto.getMultiCompany() != null && debitNoteDto.getMultiCompany() ? "Y" : "N");
        existingEntity.setRemarksPrintable(debitNoteDto.getRemarksPrintable() != null && debitNoteDto.getRemarksPrintable() ? "Y" : "N");
        existingEntity.setShowBankDetailsInPrint(debitNoteDto.getShowBankDetailsInPrint() != null && debitNoteDto.getShowBankDetailsInPrint() ? "Y" : "N");
        existingEntity.setGroupPoid(UserContext.getGroupPoid());
        existingEntity.setCompanyPoid(UserContext.getCompanyPoid());
        existingEntity.setOtherCurrAmount(debitNoteDto.getOtherCurrAmount());
        existingEntity.setFdaRef(debitNoteDto.getFdaRefPoid().toString());
        existingEntity.setFdaDirectRef(debitNoteDto.getFdaDirectRefPoid().toString());

        existingEntity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        existingEntity.setLastModifiedDate(LocalDateTime.now());
        existingEntity.setTransactionDate(debitNoteDto.getTransactionDate());
        existingEntity.setFfRef(debitNoteDto.getFfRefPoid() != null ? debitNoteDto.getFfRefPoid().toString() : null);
        existingEntity.setPropertyInvoice(Boolean.TRUE.equals(debitNoteDto.getPropertyInvoice()) ? "Y" : "N");
        existingEntity.setPrintCompanyPoid(debitNoteDto.getPrintCompanyPoid());
        existingEntity.setRemarks(debitNoteDto.getRemarks());
        debitNoteHdrRepository.save(existingEntity);
        entityManager.flush();
        entityManager.refresh(existingEntity);

        debitNoteDto.setDocRef(existingEntity.getDocRef());

        // Pre-save DB-level validations (after flush so TRANSACTION_POID is visible to procedures)
        runPreSaveProcedures(existingEntity.getTransactionPoid(), debitNoteDto);

        DocumentBeforeSaveBillwiseCostGroups(debitNoteDto);

        List<GlobalLogSummary> detailSummaryLogs = new ArrayList<>();
        updateGlDetailsWithLogging(debitNoteDto.getGlDetails(), transactionPoid, detailSummaryLogs);
        updateChargeDetailsWithLogging(debitNoteDto.getChargeDetails(), transactionPoid, detailSummaryLogs);

        // --- UPDATE BILLWISE & COST CENTER BREAKUPS ---
        updateBillwiseBreakups(debitNoteDto, existingEntity.getTransactionPoid(), existingEntity.getGroupPoid(), existingEntity.getCompanyPoid());
        updateCostCenterBreakups(debitNoteDto, existingEntity.getTransactionPoid(), existingEntity.getGroupPoid(), existingEntity.getCompanyPoid());

        // return database-backed DTO (with details loaded from DB)
        DebitNoteHeaderDto result = mapToDto(existingEntity);
        result.setCreatedBy(existingEntity.getCreatedBy());
        result.setCreatedDate(existingEntity.getCreatedDate());
        result.setLastModifiedBy(existingEntity.getLastModifiedBy());
        result.setLastModifiedDate(existingEntity.getLastModifiedDate());
        loadDetails(result, transactionPoid, existingEntity);

        // Load breakups into response
        // loadBreakups(result, transactionPoid);

        loggingService.logChanges(oldEntity, existingEntity, ArDebitNoteHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        if (!detailSummaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(detailSummaryLogs);
        }

        // Publish event for after-save processing (will run after transaction commit)
        publishAfterSaveEvent(existingEntity, oldFdaRef, oldRefType);

        DebitNoteHeaderDto finalResult = getDebitNote(transactionPoid);
        if (debitNoteDto.getWarnings() != null && !debitNoteDto.getWarnings().isEmpty()) {
            finalResult.setWarnings(debitNoteDto.getWarnings());
        }
        return finalResult;
    }

    @Override
    @Transactional
    public void deleteDebitNote(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        ArDebitNoteHdr entity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));

        // Release FDA/FDA_DIRECT holds before soft-delete (mirrors legacy DocumentAfterDelete)
        try {
            if (isFdaLike(entity.getRefType())) {
                String fdaRef = "FDA_DIRECT".equalsIgnoreCase(entity.getRefType())
                        ? entity.getFdaDirectRef() : entity.getFdaRef();
                if (fdaRef != null && !fdaRef.trim().isEmpty()) {
                    debitNoteProcedureRepository.updateFdaAmount(
                            entity.getGroupPoid(), entity.getCompanyPoid(),
                            UserContext.getUserPoid(), fdaRef);
                }
            }
        } catch (Exception e) {
            log.error("FDA amount release failed on delete for transaction {}: {}", transactionPoid, e.getMessage());
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "AR_DEBIT_NOTE_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                entity.getTransactionDate()
        );
    }

    @Override
    public DebitNoteHeaderDto  getDebitNote(Long transactionPoid) {
        ArDebitNoteHdr entity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));

        DebitNoteHeaderDto dto = mapToDto(entity);
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        loadDetails(dto, transactionPoid, entity);

        // Load breakups into GL details
        loadBreakups(dto, transactionPoid);

        return dto;
    }

    @Override
    public Map<String, Object> listDebitNotes(FilterRequestDto filters, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDateValue, endDateValue);

        RawSearchResult raw = documentService.search(debitNoteDocId, filterList, operator, pageable, isDeleted,
                "PARTY_TYPE", "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void applyBusinessLogic(DebitNoteHeaderDto debitNoteDto) {
        if (debitNoteDto.getPartyPoid() != null && "SUPPLIER".equals(debitNoteDto.getPartyType())) {
            SupplierMasterEntity supplier = supplierMasterRepository.findBySupplierPoid(debitNoteDto.getPartyPoid());
            if (supplier != null && supplier.getTinNumber() != null) {
                debitNoteDto.setTinNumber(supplier.getTinNumber());
            }
        }

        if (debitNoteDto.getCreditPeriod() != null && debitNoteDto.getCreditPeriod() > 0) {
            debitNoteDto.setDueDate(LocalDate.now().plusDays(debitNoteDto.getCreditPeriod()));
        }

        applyCurrencyConversion(debitNoteDto);
    }

    private void applyCurrencyConversion(DebitNoteHeaderDto dto) {
        if (!"BHD".equalsIgnoreCase(dto.getCurrencyCode())) {
            if (dto.getCurrencyRate() == null) {
                throw new ValidationException("Currency Rate is required for non-BHD currencies");
            }
            if (dto.getOtherCurrAmount() == null) {
                throw new ValidationException("Other Currency Amount is required for non-BHD currencies");
            }
            dto.setBhdAmount(dto.getOtherCurrAmount().multiply(dto.getCurrencyRate()).setScale(3, RoundingMode.HALF_UP));
        }
    }

    /**
     * Save details:
     * - Always save GL details if provided (user requested this change so FDA manual GLs persist)
     * - Save charge details when provided (FDA/FDA_DIRECT/OTHER_CHARGES)
     */
    private void saveDetails(DebitNoteHeaderDto debitNoteDto, Long transactionPoid) {
        // always save GL if provided (support manual GL entries for all ref types)
        if (debitNoteDto.getGlDetails() != null && !debitNoteDto.getGlDetails().isEmpty()) {
            saveGlDetails(debitNoteDto.getGlDetails(), transactionPoid);
        }

        // save charges when provided (existing behavior)
        if (debitNoteDto.getChargeDetails() != null && !debitNoteDto.getChargeDetails().isEmpty()) {
            saveChargeDetails(debitNoteDto.getChargeDetails(), transactionPoid);
        }

    }

    private void updateGlDetailsWithLogging(List<DebitNoteGlDetailDto> glDetails, Long transactionPoid, List<GlobalLogSummary> summaryLogs) {
        if (glDetails == null || glDetails.isEmpty()) return;

        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<ArDebitNoteDtl> toSave = new ArrayList<>();
        List<ArDebitNoteDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArDebitNoteDtl>> logRequests = new ArrayList<>();

        List<ArDebitNoteDtl> existingDetails = debitNoteDtlRepository.findByTransactionPoid(transactionPoid);
        Map<Long, ArDebitNoteDtl> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(ArDebitNoteDtl::getDetRowId, Function.identity(), (a, b) -> a));

        Long nextDetRowId = debitNoteDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (nextDetRowId == null) {
            nextDetRowId = 0L;
        }

        for (DebitNoteGlDetailDto dto : glDetails) {
            if (dto.isEmpty()) continue;

            String actionType = resolveDetailUpdateActionType(dto.getActionType(), dto.getDetRowId());

            switch (actionType) {
                case "ISCREATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        detRowId = ++nextDetRowId;
                        dto.setDetRowId(detRowId);
                    } else if (detRowId > nextDetRowId) {
                        nextDetRowId = detRowId;
                    }

                    ArDebitNoteDtl newEntity = new ArDebitNoteDtl();
                    mapGlDtoToEntity(dto, newEntity, transactionPoid);
                    newEntity.setDetRowId(detRowId);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    toSave.add(newEntity);
                    break;
                }
                case "ISUPDATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        throw new ValidationException("Debit Note GL Detail detRowId is required for update");
                    }
                    ArDebitNoteDtl existing = existingMap.get(detRowId);
                    if (existing == null) {
                        throw new ValidationException("Debit Note GL Detail not found for detRowId: " + detRowId);
                    }

                    ArDebitNoteDtl oldEntity = new ArDebitNoteDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapGlDtoToEntity(dto, existing, transactionPoid);
                    existing.setLastModifiedBy(currentUser);
                    existing.setLastModifiedDate(now);
                    toUpdate.add(existing);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ArDebitNoteDtl.class, docId, docKeyPoid, logDetail));
                    break;
                }
                case "ISDELETED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId != null && detRowId > 0) {
                        toDelete.add(detRowId);
                        loggingService.logDelete(dto, docId, docKeyPoid);
                    }

                    break;
                }
                case "NOCHANGE":
                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            List<ArDebitNoteDtl> savedItems = debitNoteDtlRepository.saveAll(toSave);
            savedItems.forEach(entity -> {
                String summaryMessage = String.format("Row Created on Debit Note GL Detail with DetRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, summaryMessage);
            });
        }
        if (!toUpdate.isEmpty()) {
            debitNoteDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            debitNoteDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
    }

    private void updateChargeDetailsWithLogging(List<DebitNoteChargeDetailDto> chargeDetails, Long transactionPoid, List<GlobalLogSummary> summaryLogs) {
        if (chargeDetails == null || chargeDetails.isEmpty()) return;

        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<ArDebitNoteChargeDtl> toSave = new ArrayList<>();
        List<ArDebitNoteChargeDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArDebitNoteChargeDtl>> logRequests = new ArrayList<>();

        List<ArDebitNoteChargeDtl> existingDetails = debitNoteChargeDtlRepository.findByTransactionPoid(transactionPoid);
        Map<Long, ArDebitNoteChargeDtl> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(ArDebitNoteChargeDtl::getDetRowId, Function.identity(), (a, b) -> a));

        Long nextDetRowId = debitNoteChargeDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (nextDetRowId == null) {
            nextDetRowId = 0L;
        }

        for (DebitNoteChargeDetailDto dto : chargeDetails) {
            if (dto.isEmpty()) continue;

            String actionType = resolveDetailUpdateActionType(dto.getActionType(), dto.getDetRowId());

            switch (actionType) {
                case "ISCREATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        detRowId = ++nextDetRowId;
                        dto.setDetRowId(detRowId);
                    } else if (detRowId > nextDetRowId) {
                        nextDetRowId = detRowId;
                    }

                    ArDebitNoteChargeDtl newEntity = new ArDebitNoteChargeDtl();
                    mapChargeDtoToEntity(dto, newEntity, transactionPoid);
                    newEntity.setDetRowId(detRowId);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    toSave.add(newEntity);
                    break;
                }
                case "ISUPDATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        throw new ValidationException("Debit Note Charge Detail detRowId is required for update");
                    }
                    ArDebitNoteChargeDtl existing = existingMap.get(detRowId);
                    if (existing == null) {
                        throw new ValidationException("Debit Note Charge Detail not found for detRowId: " + detRowId);
                    }

                    ArDebitNoteChargeDtl oldEntity = new ArDebitNoteChargeDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapChargeDtoToEntity(dto, existing, transactionPoid);
                    existing.setLastModifiedBy(currentUser);
                    existing.setLastModifiedDate(now);
                    toUpdate.add(existing);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ArDebitNoteChargeDtl.class, docId, docKeyPoid, logDetail));
                    break;
                }
                case "ISDELETED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId != null && detRowId > 0) {
                        toDelete.add(detRowId);
                        loggingService.logDelete(dto, docId, docKeyPoid);
                    }

                    break;
                }
                case "NOCHANGE":
                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            List<ArDebitNoteChargeDtl> savedItems = debitNoteChargeDtlRepository.saveAll(toSave);
            savedItems.forEach(entity -> {
                String summaryMessage = String.format("Row Created on Debit Note Charge Detail with DetRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, summaryMessage);
            });
        }
        if (!toUpdate.isEmpty()) {
            debitNoteChargeDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            debitNoteChargeDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
    }

    private void deleteExistingDetails(Long transactionPoid) {
        debitNoteDtlRepository.deleteByTransactionPoid(transactionPoid);
        debitNoteChargeDtlRepository.deleteByTransactionPoid(transactionPoid);
    }

    private String normalizeDetailUpdateActionType(String actionType) {
        String normalizedActionType = normalizeActionType(actionType);
        if (normalizedActionType.isEmpty()) {
            return "NOCHANGE";
        }
        if ("NOCHANGES".equals(normalizedActionType)) {
            return "NOCHANGE";
        }
        return normalizedActionType;
    }

    private String resolveDetailUpdateActionType(String actionType, Long detRowId) {
        return normalizeDetailUpdateActionType(actionType);
    }

    private void loadDetails(DebitNoteHeaderDto dto, Long transactionPoid, ArDebitNoteHdr header) {
        // always read GL details (if any)
        List<ArDebitNoteDtl> glDetails = debitNoteDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setGlDetails(glDetails.stream().map(entity -> mapGlDetailToDto(entity, header.getRefType())).collect(Collectors.toList()));

        // read charge details as well
        List<ArDebitNoteChargeDtl> chargeDetails = debitNoteChargeDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setChargeDetails(chargeDetails.stream().map(entity -> mapChargeDetailToDto(entity, header)).collect(Collectors.toList()));
    }

    private void saveGlDetails(List<DebitNoteGlDetailDto> glDetails, Long transactionPoid) {
        debitNoteDtlRepository.deleteByTransactionPoid(transactionPoid);

        Long detRowId = 0L;
        String user = ASGHelperUtils.getCurrentUser();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        for (DebitNoteGlDetailDto dto : glDetails) {
            if (dto.isEmpty()) continue;

            String actionType = dto.getActionType();
            if (actionType == null || !actionType.equalsIgnoreCase("isCreated")) {
                continue;
            }

            Long incomingDetRowId = dto.getDetRowId();
            if (incomingDetRowId != null) {
                detRowId = Math.max(detRowId, incomingDetRowId);
            } else {
                detRowId++;
                incomingDetRowId = detRowId;
            }

            ArDebitNoteDtl entity = new ArDebitNoteDtl();
            mapGlDtoToEntity(dto, entity, transactionPoid);
            entity.setDetRowId(incomingDetRowId);
            entity.setCreatedBy(user);
            entity.setCreatedDate(LocalDateTime.now());

            ArDebitNoteDtl saved = debitNoteDtlRepository.save(entity);

            String summaryMessage = String.format("Row Created on Debit Note GL Detail with DetRowId: %s", saved.getDetRowId());
            loggingService.createLogSummaryEntry(docId, docKeyPoid, summaryMessage);
        }
    }

    private void saveChargeDetails(List<DebitNoteChargeDetailDto> chargeDetails, Long transactionPoid) {
        debitNoteChargeDtlRepository.deleteByTransactionPoid(transactionPoid);

        Long detRowId = 0L;
        String user = ASGHelperUtils.getCurrentUser();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        for (DebitNoteChargeDetailDto dto : chargeDetails) {
            if (dto.isEmpty()) continue;

            String actionType = dto.getActionType();
            if (actionType == null || !actionType.equalsIgnoreCase("isCreated")) {
                continue;
            }

            Long incomingDetRowId = dto.getDetRowId();
            if (incomingDetRowId != null) {
                detRowId = Math.max(detRowId, incomingDetRowId);
            } else {
                detRowId++;
                incomingDetRowId = detRowId;
            }

            ArDebitNoteChargeDtl entity = new ArDebitNoteChargeDtl();
            mapChargeDtoToEntity(dto, entity, transactionPoid);
            entity.setDetRowId(incomingDetRowId);
            entity.setCreatedBy(user);
            entity.setCreatedDate(LocalDateTime.now());

            ArDebitNoteChargeDtl saved = debitNoteChargeDtlRepository.save(entity);

            String summaryMessage = String.format("Row Created on Debit Note Charge Detail with DetRowId: %s", saved.getDetRowId());
            loggingService.createLogSummaryEntry(docId, docKeyPoid, summaryMessage);
        }
    }

    private void mapGlDtoToEntity(DebitNoteGlDetailDto dto, ArDebitNoteDtl entity, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setType(dto.getType());
        Long finalCompanyPoid =
                (dto.getCompanyPoid() == null || dto.getCompanyPoid() == 0)
                        ? UserContext.getCompanyPoid()
                        : dto.getCompanyPoid();

        entity.setCompanyPoid(finalCompanyPoid);
        entity.setGlPoid(dto.getGlId());
        entity.setDrAmt(dto.getDebitAmount());
        entity.setCrAmt(dto.getCreditAmount());
        entity.setRemarks(dto.getRemarks());
        entity.setTaxPoid(dto.getTaxId());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        if (dto.getTotalAmount() != null) {
            entity.setTotalAmount(dto.getTotalAmount());
        } else {
            BigDecimal dr = dto.getDebitAmount() == null ? BigDecimal.ZERO : dto.getDebitAmount();
            BigDecimal cr = dto.getCreditAmount() == null ? BigDecimal.ZERO : dto.getCreditAmount();
            entity.setTotalAmount(dr.add(cr));
        }
    }

    private void mapChargeDtoToEntity(DebitNoteChargeDetailDto dto, ArDebitNoteChargeDtl entity, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setChargePoid(dto.getChargeId());
        entity.setChargeAmount(dto.getChargeAmount());
        entity.setRemarks(dto.getRemarks());
        entity.setTaxPoid(dto.getTaxId());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        entity.setPdaAmount(dto.getCostAmount());
        entity.setCostPoid(dto.getCostPoid());
        entity.setCostGroup(dto.getCostGroup() != null ? dto.getCostGroup().toString() : null);
        entity.setCheckAll(dto.getCheckAll());
        entity.setPrintSeqNo(dto.getSeqNo());
        entity.setFdaDetRowId(dto.getFdaDetRowId());
        entity.setRefDocId(dto.getRefDocId());
        entity.setRefDocPoid(dto.getRefDocPoid());

        if (dto.getTotalAmount() != null) {
            entity.setTotalAmount(dto.getTotalAmount());
        }
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(LocalDateTime.now());
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }

    private ArDebitNoteHdr mapToEntity(DebitNoteHeaderDto dto) {
        ArDebitNoteHdr entity = new ArDebitNoteHdr();

        // Get next sequence value manually
        Long nextId = debitNoteHdrRepository.getNextSequenceValue();
        entity.setTransactionPoid(nextId);

        entity.setTransactionDate(dto.getTransactionDate());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setCurrencyRate(dto.getCurrencyRate());
        entity.setPartyType(dto.getPartyType());
        entity.setPartyPoid(dto.getPartyPoid());
        entity.setOtherCurrAmount(dto.getOtherCurrAmount());
        entity.setRefType(dto.getRefType());
        entity.setDrTotal(dto.getDrTotal());
        entity.setCrTotal(dto.getCrTotal());
        entity.setPostingNarration(dto.getPostingNarration());
        entity.setGrandTotal(dto.getGrandTotal());
        entity.setDueDate(dto.getDueDate());
        entity.setCreditPeriod(dto.getCreditPeriod());
        entity.setFdaRef(dto.getFdaRefPoid().toString());
        entity.setFdaDirectRef(dto.getFdaDirectRefPoid().toString());
        entity.setPoRef(dto.getPoRef());
        entity.setRemarks(dto.getRemarks());
        entity.setBankPoid(dto.getBankPoid());
        entity.setTinNumber(dto.getTinNumber());
        entity.setBhdAmount(dto.getBhdAmount());
        entity.setVoucherType(dto.getVoucherType());
        entity.setCostRefNumber(dto.getCostRefNumber());
        entity.setCostGroup(dto.getCostGroupPoid() != null ? dto.getCostGroupPoid().toString() : null);
        entity.setPrintDivisionPoid(dto.getPrintDivisionPoid());
        entity.setMultiCompany(dto.getMultiCompany() != null && dto.getMultiCompany() ? "Y" : "N");
        entity.setRemarksPrintable(dto.getRemarksPrintable() != null && dto.getRemarksPrintable() ? "Y" : "N");
        entity.setShowBankDetailsInPrint(dto.getShowBankDetailsInPrint() != null && dto.getShowBankDetailsInPrint() ? "Y" : "N");

        entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setVoyageRef(dto.getVoyageRef());
        entity.setFfRef(dto.getFfRefPoid() != null ? dto.getFfRefPoid().toString() : null);
        entity.setPropertyInvoice(Boolean.TRUE.equals(dto.getPropertyInvoice()) ? "Y" : "N");
        entity.setPrintCompanyPoid(dto.getPrintCompanyPoid());

        return entity;
    }

    private DebitNoteHeaderDto mapToDto(ArDebitNoteHdr entity) {
        DebitNoteHeaderDto dto = new DebitNoteHeaderDto();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencyRate(entity.getCurrencyRate());
        dto.setRemarks(entity.getRemarks());
        dto.setPartyType(entity.getPartyType());
        dto.setPartyPoid(entity.getPartyPoid());
        dto.setRefType(entity.getRefType());
        dto.setDocRef(entity.getDocRef());
        dto.setDrTotal(entity.getDrTotal());
        dto.setCrTotal(entity.getCrTotal());
        dto.setPostingNarration(entity.getPostingNarration());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setDueDate(entity.getDueDate());
        dto.setCreditPeriod(entity.getCreditPeriod());
        dto.setPoRef(entity.getPoRef());
        dto.setFdaRefPoid(parseLongSafely(entity.getFdaRef()));
        dto.setFdaDirectRefPoid(parseLongSafely(entity.getFdaDirectRef()));
        dto.setBankPoid(entity.getBankPoid());
        dto.setTinNumber(entity.getTinNumber());
        dto.setBhdAmount(entity.getBhdAmount());
        dto.setOtherCurrAmount(entity.getOtherCurrAmount());
        dto.setVoucherType(entity.getVoucherType());
        dto.setCostRefNumber(entity.getCostRefNumber());
        dto.setCostGroupPoid(entity.getCostGroup());
        dto.setPrintDivisionPoid(entity.getPrintDivisionPoid());
        dto.setMultiCompany("Y".equals(entity.getMultiCompany()));
        dto.setRemarksPrintable("Y".equals(entity.getRemarksPrintable()));
        dto.setShowBankDetailsInPrint("Y".equals(entity.getShowBankDetailsInPrint()));
        dto.setDeleted(entity.getDeleted());
        dto.setDocRef(entity.getDocRef());
        dto.setVoyageRef(entity.getVoyageRef());
        dto.setFfRefPoid(parseLongSafely(entity.getFfRef()));
        dto.setPropertyInvoice("Y".equals(entity.getPropertyInvoice()));
        dto.setPrintCompanyPoid(entity.getPrintCompanyPoid());
        // voucherTypeReadOnly: true once the record has been saved (doc ref exists)
        dto.setVoucherTypeReadOnly(entity.getDocRef() != null && !entity.getDocRef().trim().isEmpty());

        // Populate header LOV details
        if (dto.getFdaRefPoid() != null) {
            dto.setFdaRefDetails(lovService.getDetailsByPoidAndLovName(dto.getFdaRefPoid(), "PROCESS_FDA_IN_PI"));
        }
        if (dto.getFdaDirectRefPoid() != null) {
            dto.setFdaDirectRefDetails(lovService.getDetailsByPoidAndLovName(dto.getFdaDirectRefPoid(), "PROCESS_FDA_DIRECT_IN_DN"));
        }
        if (dto.getCostGroupPoid() != null) {
            try {
                LovGetListDto details = lovService.getDetailsByPoidAndLovName(Long.valueOf(dto.getCostGroupPoid()), "DN_GL_COST_GROUPS");
                if (details == null || details.getCode() == null) {
                    details = lovService.getDetailsByCodeAndLovName(dto.getCostGroupPoid(), "DN_GL_COST_GROUPS");
                }
                dto.setCostGroupDetails(details);
            } catch (NumberFormatException e) {
                dto.setCostGroupDetails(lovService.getDetailsByCodeAndLovName(dto.getCostGroupPoid(), "DN_GL_COST_GROUPS"));
            }
        }
        if (dto.getDisposalJvRefPoid() != null) {
            dto.setDisposalJvRefDetails(lovService.getDetailsByPoidAndLovName(dto.getDisposalJvRefPoid(), "DISPOSAL_JV_REF_FOR_DN"));
        }

        return dto;
    }

    private Long parseLongSafely(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long from value: {}", value);
            return null;
        }
    }

    private DebitNoteGlDetailDto mapGlDetailToDto(ArDebitNoteDtl entity, String refType) {
        DebitNoteGlDetailDto dto = new DebitNoteGlDetailDto();

        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setDetRowId(entity.getDetRowId());
        dto.setType(entity.getType());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setGlId(entity.getGlPoid());
        dto.setDebitAmount(entity.getDrAmt());
        dto.setCreditAmount(entity.getCrAmt());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxId(entity.getTaxPoid());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());

        // Populate LOV details based on refType
        if (entity.getType() != null) {
            dto.setTypeDetails(lovService.getDetailsByCodeAndLovName(entity.getType(), "ACC_TYPE_SHORT"));
        }
        if (entity.getGlPoid() != null) {
            dto.setGlDetails(lovService.getDetailsByPoidAndLovName(entity.getGlPoid(), "GL_MASTER_LEDGERS_A_L"));
        }
        if (entity.getTaxPoid() != null) {
            dto.setTaxDetails(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "DR_TAX_MASTER"));
        }
        if (entity.getCompanyPoid() != null) {
            dto.setCompanyDetails(lovService.getDetailsByPoidAndLovName(entity.getCompanyPoid(), "COMPANY"));
        }

        return dto;
    }

    private DebitNoteChargeDetailDto mapChargeDetailToDto(ArDebitNoteChargeDtl entity, ArDebitNoteHdr header) {
        DebitNoteChargeDetailDto dto = new DebitNoteChargeDetailDto();

        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setDetRowId(entity.getDetRowId());
        dto.setChargeId(entity.getChargePoid());
        dto.setChargeAmount(entity.getChargeAmount());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxId(entity.getTaxPoid());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setCostAmount(entity.getCostAmount());
        dto.setSeqNo(entity.getPrintSeqNo());
        dto.setCostPoid(entity.getCostPoid());
        dto.setCostGroup(entity.getCostGroup());
        dto.setCheckAll(entity.getCheckAll());
        dto.setCostAmount(entity.getPdaAmount());
        dto.setFdaDetRowId(entity.getFdaDetRowId());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());

        // Populate LOV details based on refType
        if (entity.getChargePoid() != null) {
            String chargeLov = "OTHER_CHARGES".equalsIgnoreCase(header.getRefType())
                    ? "DEBIT_NOTE_OTHER_CHARGES"
                    : "CHARGE_MASTER_IN_DN_FOR_SH";
            dto.setChargeDetails(lovService.getDetailsByPoidAndLovName(entity.getChargePoid(), chargeLov));
        }
        if (entity.getTaxPoid() != null) {

            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "DR_TAX_MASTER");

            if (lovGetListDto == null || lovGetListDto.getCode() == null) {

                lovGetListDto = taxMasterRepository.findByTaxPoid(entity.getTaxPoid())
                        .map(tm -> new LovGetListDto(tm.getTaxPoid(), tm.getTaxCode(), tm.getTaxName(), tm.getTaxPoid(), tm.getTaxName(), tm.getSeqNo(), null))
                        .orElse(null);
            }

            dto.setTaxDetails(lovGetListDto);
        }

        if (entity.getCostPoid() != null) {
            String customLov = getCustomLovList(header.getCostGroup()).getOrDefault("lovName", "DN_GL_COST_CENTRE");
            try {
                LovGetListDto details = lovService.getDetailsByPoidAndLovName(Long.valueOf(entity.getCostPoid()), customLov);
                if (details == null || details.getCode() == null) {
                    details = lovService.getDetailsByCodeAndLovName(entity.getCostPoid(), customLov);
                }
                dto.setCostCenterDetails(details);
            } catch (NumberFormatException e) {
                dto.setCostCenterDetails(lovService.getDetailsByCodeAndLovName(entity.getCostPoid(), customLov));
            }
        }

        return dto;
    }

    // ---------------------------
    // Breakup helpers
    // ---------------------------

    private void insertBillwiseBreakups(DebitNoteHeaderDto dto, ArDebitNoteHdr savedEntity) {
        if (dto.getGlDetails() == null) return;

        List<BillwiseBreakupRequestDto> billwiseRequests = new ArrayList<>();
        long inital = 1L;
        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
            if (gl.getBreakupList() == null || gl.getBreakupList().isEmpty()) continue;

            for (BillwiseBreakupPopupRequestDto bw : gl.getBreakupList()) {
                // For CREATE: only process "isCreated" or null/empty actionType
                String actionType = bw.getActionType();
                if (actionType == null || actionType.trim().isEmpty()) {
                    actionType = "isCreated"; // Default to create if actionType is null/empty
                }
                String actionTypeUpper = actionType.toUpperCase();
                if (!"ISCREATED".equals(actionTypeUpper)) {
                    continue; // Skip non-created entries in CREATE operation
                }

                BillwiseBreakupRequestDto req = new BillwiseBreakupRequestDto();
                req.setGroupPoid(savedEntity.getGroupPoid());
                req.setCompanyPoid(savedEntity.getCompanyPoid());
                req.setDocId(debitNoteDocId);
                req.setTransactionPoid(savedEntity.getTransactionPoid());
                req.setGlPoid(gl.getGlId());
                req.setMainDetRowId(gl.getDetRowId());
                req.setBillDetRowId(inital);
                req.setBillRefType(bw.getBillRefType());
                req.setBillRef(bw.getBillRef());
                req.setBillDueDate(bw.getBillDueDate());
                if ("DR".equalsIgnoreCase(bw.getType())) {
                    req.setDrAmt(bw.getAmount());
                    req.setCrAmt(BigDecimal.ZERO);
                } else {
                    req.setDrAmt(BigDecimal.ZERO);
                    req.setCrAmt(bw.getAmount());
                }
                req.setBillRemarks(bw.getBillRemarks());
                req.setLoginUserPoid(UserContext.getUserPoid());

                billwiseRequests.add(req);
                inital++;
            }
        }

        if (!billwiseRequests.isEmpty()) {
            billwiseBreakupService.insertBillwiseBreakup(billwiseRequests);
        }
    }

    private void insertCostCenterBreakups(DebitNoteHeaderDto dto, ArDebitNoteHdr savedEntity) {
        if (dto.getGlDetails() == null) return;

        List<CostCenterBreakupRequestDto> ccRequests = new ArrayList<>();

        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
            if (gl.getCostCenterList() == null || gl.getCostCenterList().isEmpty()) continue;

            for (CostCenterBreakupPopupRequestDto cb : gl.getCostCenterList()) {
                // For CREATE: only process "isCreated" or null/empty actionType
                String actionType = cb.getActionType();
                if (actionType == null || actionType.trim().isEmpty()) {
                    actionType = "isCreated"; // Default to create if actionType is null/empty
                }
                String actionTypeUpper = actionType.toUpperCase();
                if (!"ISCREATED".equals(actionTypeUpper)) {
                    continue; // Skip non-created entries in CREATE operation
                }

                CostCenterBreakupRequestDto req = new CostCenterBreakupRequestDto();
                req.setGroupPoid(savedEntity.getGroupPoid());
                req.setCompanyPoid(savedEntity.getCompanyPoid());
                req.setDocId(debitNoteDocId);
                req.setTransactionPoid(savedEntity.getTransactionPoid());
                req.setGlPoid(gl.getGlId());
                req.setMainDetRowId(gl.getDetRowId());
                req.setCostDetRowId(cb.getCostDetRowId());
                req.setCostGroup(cb.getCostGroup());
                req.setCostPoid(cb.getCostPoid());
                req.setAmount(cb.getAmount());
                req.setLoginUserPoid(UserContext.getUserPoid());

                ccRequests.add(req);
            }
        }

        if (!ccRequests.isEmpty()) {
            costCenterBreakupService.saveCostCenterBreakups(ccRequests);
        }
    }

    private void updateBillwiseBreakups(DebitNoteHeaderDto dto, Long transactionPoid, Long groupPoid, Long companyPoid) {
        if (dto.getGlDetails() == null) return;

        List<BillwiseBreakupRequestDto> billwiseRequests = new ArrayList<>();
        long intial = 1;
        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
            if (gl.getBreakupList() == null || gl.getBreakupList().isEmpty() || gl.getActionType().equalsIgnoreCase("isDeleted")) continue;

            for (BillwiseBreakupPopupRequestDto bw : gl.getBreakupList()) {
                // Skip deleted and no-change entries
                String actionType = bw.getActionType();
                if (actionType != null) {
                    String actionTypeUpper = actionType.toUpperCase();
                    if ("ISDELETED".equals(actionTypeUpper) || "NOCHANGES".equals(actionTypeUpper)) {
                        continue;
                    }
                }

                BillwiseBreakupRequestDto req = new BillwiseBreakupRequestDto();
                req.setGroupPoid(groupPoid);
                req.setCompanyPoid(companyPoid);
                req.setDocId(debitNoteDocId);
                req.setTransactionPoid(transactionPoid);
                req.setGlPoid(gl.getGlId());
                req.setMainDetRowId(gl.getDetRowId());
                req.setBillDetRowId(intial);
                req.setBillRefType(bw.getBillRefType());
                req.setBillRef(bw.getBillRef());
                req.setBillDueDate(bw.getBillDueDate());
                if ("DR".equalsIgnoreCase(bw.getType())) {
                    req.setDrAmt(bw.getAmount());
                    req.setCrAmt(BigDecimal.ZERO);
                } else {
                    req.setDrAmt(BigDecimal.ZERO);
                    req.setCrAmt(bw.getAmount());
                }
                req.setBillRemarks(bw.getBillRemarks());
                req.setLoginUserPoid(UserContext.getUserPoid());

                billwiseRequests.add(req);
                intial++;
            }
        }

        if (!billwiseRequests.isEmpty()) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseRequests, UserContext.getUserPoid());
        } else {
            // No billwise data in request — clean up any previously saved records
            billwiseBreakupService.deleteBillwiseBreakup(groupPoid, companyPoid, debitNoteDocId, transactionPoid, UserContext.getUserPoid());
        }
    }

    private void updateCostCenterBreakups(DebitNoteHeaderDto dto, Long transactionPoid, Long groupPoid, Long companyPoid) {
        if (dto.getGlDetails() == null) return;

        List<CostCenterBreakupRequestDto> ccRequests = new ArrayList<>();

        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
            if (gl.getCostCenterList() == null || gl.getCostCenterList().isEmpty() || gl.getActionType().equalsIgnoreCase("isDeleted")) continue;
            long intial = 1;
            for (CostCenterBreakupPopupRequestDto cb : gl.getCostCenterList()) {
                // Skip deleted and no-change entries
                String actionType = cb.getActionType();
                if (actionType != null) {
                    String actionTypeUpper = actionType.toUpperCase();
                    if ("ISDELETED".equals(actionTypeUpper) || "NOCHANGES".equals(actionTypeUpper)) {
                        continue;
                    }
                }

                CostCenterBreakupRequestDto req = new CostCenterBreakupRequestDto();
                req.setGroupPoid(groupPoid);
                req.setCompanyPoid(companyPoid);
                req.setDocId(debitNoteDocId);
                req.setTransactionPoid(transactionPoid);
                req.setGlPoid(gl.getGlId());
                req.setMainDetRowId(gl.getDetRowId());
                req.setCostDetRowId(intial);
                req.setCostGroup(cb.getCostGroup());
                req.setCostPoid(cb.getCostPoid());
                req.setAmount(cb.getAmount());
                req.setLoginUserPoid(UserContext.getUserPoid());

                ccRequests.add(req);
                intial++;
            }
        }

        if (!ccRequests.isEmpty()) {
            costCenterBreakupService.updateCostCenterBreakups(ccRequests, UserContext.getUserPoid());
        } else {
            // No cost center data in request — clean up any previously saved records
            costCenterBreakupService.deleteCostCenterData(debitNoteDocId, transactionPoid, groupPoid, companyPoid, UserContext.getUserPoid());
        }
    }

    private void loadBreakups(DebitNoteHeaderDto dto, Long transactionPoid) {
        if (dto.getGlDetails() == null || dto.getGlDetails().isEmpty()) return;

        Long groupPoid = dto.getGroupPoid();
        Long companyPoid = dto.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        // load billwise from service
        var billwiseResponse = billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, debitNoteDocId, transactionPoid);

        // load cost center from service
        var costCenterResponse = costCenterBreakupService.loadCostCenterData(debitNoteDocId, transactionPoid, groupPoid, companyPoid, userPoid);

        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
            if ("ISDELETED".equals(gl.getActionType() != null ? gl.getActionType().trim().toUpperCase() : "")) continue;
            Long detRowId = gl.getDetRowId();

            // map billwise breakup
            if (billwiseResponse != null && billwiseResponse.getLoadBillwiseBreakupResponseDtoList() != null) {

                List<BillwiseBreakupPopupRequestDto> mappedBw =
                        billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                                .filter(x -> Objects.equals(x.getMainDetRowId(), detRowId))
                                .map(x -> {
                                    BillwiseBreakupPopupRequestDto popup = new BillwiseBreakupPopupRequestDto();
                                    popup.setBillDetRowId(x.getBillDetRowId());
                                    popup.setBillRefType(x.getBillRefType());
                                    popup.setBillRef(x.getBillRef());
                                    popup.setBillDueDate(x.getBillDueDate());
                                    BigDecimal drAmt = x.getDrAmt() != null ? x.getDrAmt() : BigDecimal.ZERO;
                                    BigDecimal crAmt = x.getCrAmt() != null ? x.getCrAmt() : BigDecimal.ZERO;
                                    popup.setType(drAmt.compareTo(BigDecimal.ZERO) > 0 ? "DR" : "CR");
                                    popup.setAmount(drAmt.compareTo(BigDecimal.ZERO) > 0 ? drAmt : crAmt);
                                    popup.setBillRemarks(x.getBillRemarks());
                                    return popup;
                                })
                                .collect(Collectors.toList());

                gl.setBreakupList(mappedBw);
            }

            // map cost center breakup
            if (costCenterResponse != null && costCenterResponse.getCostBreakupList() != null) {

                List<CostCenterBreakupPopupRequestDto> mappedCc =
                        costCenterResponse.getCostBreakupList().stream()
                                .filter(x -> Objects.equals(x.getMainDetRowId(), detRowId))
                                .map(x -> {
                                    CostCenterBreakupPopupRequestDto cb = new CostCenterBreakupPopupRequestDto();

                                    // Only map valid fields from CostCenterBreakupResponseDto
                                    cb.setCostDetRowId(x.getCostDetRowId());
                                    cb.setCostGroup(x.getCostGroup());
                                    cb.setCostPoid(
                                            x.getCostPoid() != null ? x.getCostPoid() : null
                                    );
                                    cb.setAmount(
                                            x.getAmount() != null ? (x.getAmount()) : BigDecimal.ZERO
                                    );
                                    if (StringUtils.isNotEmpty(x.getCostPoid()) && StringUtils.isNotEmpty(x.getCostGroup())) {
                                        try {
                                            LovGetListDto codeDet  = lovService.getDetailsByCodeAndLovName(x.getCostPoid(), x.getCostGroup());
                                            cb.setCostCenterDetails(codeDet);
                                            if (codeDet.getPoid() == null) {
                                                LovGetListDto det = lovService.getDetailsByPoidAndLovName(Long.valueOf(x.getCostPoid()), x.getCostGroup());
                                                cb.setCostCenterDetails(det);
                                                cb.setCostPoid(det.getCode());
                                            }
                                        } catch (NumberFormatException e) {
                                            cb.setCostCenterDetails(lovService.getDetailsByCodeAndLovName(x.getCostPoid(), x.getCostGroup()));
                                        }
                                    }

                                    return cb;
                                })
                                .collect(Collectors.toList());

                gl.setCostCenterList(mappedCc);
            }

        }
    }

    @Override
    public Map<String, Object> loadFdaCharges(Long fdaPoid) {
        return debitNoteCustomRepository.loadFdaCharges(fdaPoid);
    }

    @Override
    public Map<String, Object> getChargeTax(Long chargeId, String partyType, Long partyPoid) {
//        validatePartyPoid(partyPoid, partyType);
        return debitNoteCustomRepository.getTaxPercentage(chargeId, partyType, partyPoid);
    }

    @Override
    @Transactional
    public void updateCostAmount(Long transactionPoid) {
        debitNoteCustomRepository.updateCostAmount(transactionPoid);
    }

    @Override
    public Map<String, Object> checkSailDate(Long fdaPoid) {
        Map<String, Object> result = debitNoteCustomRepository.checkSailDate(fdaPoid);
        Object sailDateObj = result.get("sailDate");
        if (sailDateObj == null) {
            return Map.of("warning", "FDA Saildate is null...");
        }
        return Map.of("sailDate", (java.sql.Date) sailDateObj);
    }

    @Override
    public Map<String, Object> getPartyDefaults(Long partyPoid, String partyType) {
        validatePartyPoid(partyPoid, partyType);
        return debitNoteCustomRepository.getPartyDefaults(partyPoid, partyType);
    }

    private void validateDebitNoteInput(DebitNoteHeaderDto dto) {
        // Validate FDA Reference
        if ("FDA".equalsIgnoreCase(dto.getRefType())) {
            if (dto.getFdaRefPoid() == null) {
                throw new ValidationException("FdaRefPoid is Mandatory for ref Type FDA");
            }
            LovGetListDto detailsObj = lovService.getDetailsByPoidAndLovName(dto.getFdaRefPoid(), "PROCESS_FDA_IN_PI");
            if (detailsObj.getCode() == null) {
                throw new ResourceNotFoundException("FDA Reference Poid Not Valid: ", "FdaRefPoid", dto.getFdaRefPoid());
            }
        }

        if ("FDA_DIRECT".equalsIgnoreCase(dto.getRefType()) && dto.getFdaDirectRefPoid() == null) {
            throw new ValidationException("FdaDirectRefPoid is Mandatory for ref Type FDA_DIRECT");
        }
    }

    private void validatePartyPoid(Long partyPoid, String partyType) {
        if (partyPoid == null || partyType == null) return;

        boolean exists = switch (partyType.toUpperCase()) {
            case "SUPPLIER_MASTER_FOR_DN" -> supplierMasterRepository.findBySupplierPoid(partyPoid) != null;
            case "CUSTOMER_MASTER_FOR_DN" ->
                    Boolean.TRUE.equals(salesCustomerMasterRepository.existsByCustomerPoid(partyPoid));
            case "PRINCIPAL_MASTER_FOR_DN" -> shipPrincipalMasterRepository.existsByPrincipalPoid(partyPoid);
            default -> false;
        };

        if (!exists) {
            throw new ResourceNotFoundException(partyType, "partyPoid", partyPoid);
        }
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-110");
        params.put("INVOICE_PRINT", 'Y');
        params.put("SUB_DEBIT_DTL_1", printService.load("Finance/AR/DebitNoteDtl_subreport1.jrxml"));
        params.put("SUB_DEBIT_DTL_VAT", printService.load("Finance/AR/DebitNoteDtlSubreportVAT2019.jrxml"));
        params.put("SUB_CHARGE_1", printService.load("Finance/AR/DebitNoteChargeSubreport1.jrxml"));
        params.put("SUB_CHARGE_VAT", printService.load("Finance/AR/DebitNoteChargeSubreportVAT2019.jrxml"));
        JasperReport mainReport = printService.load("Finance/AR/DebitNote.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private Long getPartyGLPoid(Long partyPoid, String partyType) {

        String sql = "BEGIN PROC_GL_GET_DR_PARTY_GLPOID(?, ?, ?, ?, ?, ?, ?); END;";

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setLong(1, UserContext.getGroupPoid());
            cs.setLong(2, UserContext.getCompanyPoid());
            cs.setLong(3, UserContext.getUserPoid());
            cs.setLong(4, partyPoid != null ? partyPoid : 0);
            cs.setString(5, partyType);
            cs.registerOutParameter(6, Types.NUMERIC);
            cs.registerOutParameter(7, Types.VARCHAR);

            cs.execute();

            Long partyGl = cs.getLong(6);

            if (partyGl == null || partyGl == 0) {
                throw new RuntimeException("Party GL not found.");
            }

            return partyGl;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching Party GL", e);
        }
    }

    private void DocumentBeforeSaveBillwiseCostGroups(DebitNoteHeaderDto dto) {
        BigDecimal documentTotal = firstNonNull(dto.getBhdAmount(), dto.getGrandTotal());
        if (documentTotal == null) {
            throw new ValidationException("Total Amount is not found...");
        }
        if (dto.getPartyPoid() == null) {
            throw new ValidationException("Party is not found...");
        }
        if (StringUtils.isBlank(dto.getRefType())) {
            throw new ValidationException("Ref Type is not found...");
        }
        String rtUpper = dto.getRefType().toUpperCase();
        if (!"GENERAL".equals(rtUpper)) {
            return;
        }

        Long partyGl = getPartyGLPoid(dto.getPartyPoid(), dto.getPartyType());
        if (partyGl == null) {
            throw new ValidationException("Selected Party GL_CODE is not found...");
        }

        List<DebitNoteGlDetailDto> effectiveGlDetails = getEffectiveGlDetails(dto);
        BigDecimal totalDrAmt = sumByType(effectiveGlDetails, "DR");
        BigDecimal totalCrAmt = sumByType(effectiveGlDetails, "CR");

        if (totalCrAmt.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("No Credit Entries Entered...");
        }

        boolean partyGlFound = effectiveGlDetails.stream()
                .anyMatch(gl -> Objects.equals(gl.getGlId(), partyGl));

        if (!partyGlFound) {
            BigDecimal balancingAmount = totalCrAmt.subtract(totalDrAmt);
            if (balancingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                balancingAmount = documentTotal;
            }

            Long nextDetRowId = dto.getGlDetails().stream()
                    .map(DebitNoteGlDetailDto::getDetRowId)
                    .filter(Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(0L) + 1;

            DebitNoteGlDetailDto balancingRow = new DebitNoteGlDetailDto();
            balancingRow.setType("DR");
            balancingRow.setCompanyPoid(UserContext.getCompanyPoid());
            balancingRow.setGlId(partyGl);
            balancingRow.setDebitAmount(balancingAmount);
            balancingRow.setCreditAmount(BigDecimal.ZERO);
            balancingRow.setTotalAmount(balancingAmount);
            balancingRow.setRemarks("Auto Balance Entry");
            balancingRow.setActionType("isCreated");
            balancingRow.setDetRowId(nextDetRowId);

            if (isBillwiseApplicable(partyGl)) {
                balancingRow.setBreakupList(List.of(createDefaultBillwiseBreakup(dto, balancingAmount)));
            }

            if (dto.getGlDetails() == null) {
                dto.setGlDetails(new ArrayList<>());
            }
            dto.getGlDetails().add(balancingRow);

            effectiveGlDetails = getEffectiveGlDetails(dto);
            totalDrAmt = sumByType(effectiveGlDetails, "DR");
            totalCrAmt = sumByType(effectiveGlDetails, "CR");
        }

        if (totalDrAmt.compareTo(documentTotal) != 0) {
            throw new ValidationException(
                    "Amount (" + documentTotal + ") is not matching with party debit amount(" + totalDrAmt + ")"
            );
        }

        if (totalCrAmt.compareTo(totalDrAmt) != 0) {
            throw new ValidationException(
                    "Total Debits and Credits not tallying. Cr> " + totalCrAmt + " Dr> " + totalDrAmt
            );
        }
    }

    private boolean isBillwiseApplicable(Long glPoid) {

        if (glPoid == null) {
            return false;
        }

        return glMasterRepository
                .findByGlPoid(glPoid)
                .map(gl -> "Y".equalsIgnoreCase(gl.getBillwise()))
                .orElse(false);
    }

    private BillwiseBreakupPopupRequestDto createDefaultBillwiseBreakup(DebitNoteHeaderDto dto, BigDecimal amount) {
        BillwiseBreakupPopupRequestDto billwise = new BillwiseBreakupPopupRequestDto();
        billwise.setBillDueDate(dto.getDueDate());
        billwise.setBillRefType("NEW");
        billwise.setBillRef(StringUtils.defaultIfBlank(dto.getDocRef(), "DN-TEMP"));
        billwise.setBillDueDate(resolveDueDate(dto));
        billwise.setType("DR");
        billwise.setAmount(amount);
        billwise.setBillRemarks(dto.getPostingNarration());
        billwise.setActionType("isCreated");
        return billwise;
    }

    private LocalDate resolveDueDate(DebitNoteHeaderDto dto) {
        if (dto.getDueDate() != null) {
            return dto.getDueDate();
        }
        if (dto.getCreditPeriod() != null && dto.getCreditPeriod() > 0) {
            return LocalDate.now().plusDays(dto.getCreditPeriod());
        }
        return LocalDate.now();
    }

    private List<DebitNoteGlDetailDto> getEffectiveGlDetails(DebitNoteHeaderDto dto) {
        if (dto.getGlDetails() == null) {
            return new ArrayList<>();
        }
        return dto.getGlDetails().stream()
                .filter(Objects::nonNull)
                .filter(gl -> !"ISDELETED".equalsIgnoreCase(normalizeActionType(gl.getActionType())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private BigDecimal sumByType(List<DebitNoteGlDetailDto> glDetails, String type) {
        return glDetails.stream()
                .filter(gl -> type.equalsIgnoreCase(gl.getType()))
                .map(this::resolveLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveLineAmount(DebitNoteGlDetailDto glDetail) {
        if ("DR".equalsIgnoreCase(glDetail.getType())) {
            return firstNonNull(glDetail.getTotalAmount(), glDetail.getDebitAmount(), BigDecimal.ZERO);
        }
        if ("CR".equalsIgnoreCase(glDetail.getType())) {
            return firstNonNull(glDetail.getTotalAmount(), glDetail.getCreditAmount(), BigDecimal.ZERO);
        }
        return firstNonNull(glDetail.getTotalAmount(), BigDecimal.ZERO);
    }

    private String normalizeActionType(String actionType) {
        return actionType == null ? "" : actionType.trim().toUpperCase();
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> validateEditRequest(Long transactionPoid) {
        ArDebitNoteHdr entity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));

        List<String> errors = new ArrayList<>();

        if (entity.getRefType() != null && (entity.getRefType().equals("FDA JOBS") || entity.getRefType().equals("FF JOBS")
                || entity.getRefType().equals("FDA") || entity.getRefType().equals("FDA_DIRECT"))) {
            try {
                debitNoteCustomRepository.validateDebitNote(
                        entity.getRefType(), entity.getPartyType(), entity.getPartyPoid(),
                        parseLongSafely(entity.getFdaRef()), entity.getPoRef()
                );
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        return errors.isEmpty()
                ? Map.of("valid", true)
                : Map.of("valid", false, "errors", errors);
    }

    private void publishAfterSaveEvent(ArDebitNoteHdr entity, String oldFdaRef, String oldRefType) {
        eventPublisher.publishEvent(new DebitNoteAfterSaveEvent(entity, oldFdaRef, oldRefType));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterSaveCommit(DebitNoteAfterSaveEvent event) {
        performAfterSaveProcessing(event.getEntity(), event.getOldFdaRef(), event.getOldRefType());
    }

    /**
     * Performs after-save processing including bill reference updates and FDA amount updates
     * This runs AFTER the main transaction has been committed
     */
    private void performAfterSaveProcessing(ArDebitNoteHdr entity, String oldFdaRef, String oldRefType) {
        try {
            // Release old FDA/FDA_DIRECT amounts on ref-type change during edit
            if (oldRefType != null && oldFdaRef != null) {
                if ("FDA".equalsIgnoreCase(oldRefType) || "FDA_DIRECT".equalsIgnoreCase(oldRefType)) {
                    String result = debitNoteProcedureRepository.updateFdaAmount(
                            entity.getGroupPoid(), entity.getCompanyPoid(),
                            UserContext.getUserPoid(), oldFdaRef);
                    log.info("Old {} reference amount update completed: {}", oldRefType, result);
                }
            }

            // 1. Bill Reference Update for GENERAL and CUSTOM ref types
            if ("GENERAL".equalsIgnoreCase(entity.getRefType()) || "CUSTOM".equalsIgnoreCase(entity.getRefType())) {
                String result = debitNoteProcedureRepository.updateBillReference(
                        entity.getGroupPoid(), entity.getCompanyPoid(),
                        UserContext.getUserPoid(), entity.getTransactionPoid(),
                        entity.getDocRef(), debitNoteDocId,
                        entity.getRefType(), entity.getPartyType());
                log.info("Bill reference update completed for {} RefType: {}", entity.getRefType(), result);
            }

            // 2. FDA Amount Update for FDA ref type
            if ("FDA".equalsIgnoreCase(entity.getRefType()) && entity.getFdaRef() != null) {
                String result = debitNoteProcedureRepository.updateFdaAmount(
                        entity.getGroupPoid(), entity.getCompanyPoid(),
                        UserContext.getUserPoid(), entity.getFdaRef());
                log.info("FDA amount update completed: {}", result);
                if (result != null && result.contains("ERROR")) {
                    log.error("FDA amount update failed: {}", result);
                }
            }

            // 3. FDA_DIRECT Amount Update
            if ("FDA_DIRECT".equalsIgnoreCase(entity.getRefType()) && entity.getFdaDirectRef() != null) {
                String result = debitNoteProcedureRepository.updateFdaAmount(
                        entity.getGroupPoid(), entity.getCompanyPoid(),
                        UserContext.getUserPoid(), entity.getFdaDirectRef());
                log.info("FDA_DIRECT amount update completed: {}", result);
                if (result != null && result.contains("ERROR")) {
                    log.error("FDA_DIRECT amount update failed: {}", result);
                }
            }

        } catch (Exception e) {
            log.error("Error in after-save processing for transaction {}: {}", entity.getTransactionPoid(), e.getMessage(), e);
        }
    }

    /**
     * Event class for after-save processing
     */
    private static class DebitNoteAfterSaveEvent {
        private final ArDebitNoteHdr entity;
        private final String oldFdaRef;
        private final String oldRefType;

        public DebitNoteAfterSaveEvent(ArDebitNoteHdr entity, String oldFdaRef, String oldRefType) {
            this.entity = entity;
            this.oldFdaRef = oldFdaRef;
            this.oldRefType = oldRefType;
        }

        public ArDebitNoteHdr getEntity() {
            return entity;
        }

        public String getOldFdaRef() {
            return oldFdaRef;
        }

        public String getOldRefType() {
            return oldRefType;
        }
    }

    // -------------------------------------------------------------------------
    // Pre-save validation pipeline (mirrors legacy DocumentBeforeSave)
    // -------------------------------------------------------------------------

    private static final Map<String, Set<String>> ALLOWED_VOUCHER_TYPES = Map.of(
            "GENERAL",       Set.of("DEBIT_NOTE", "INVOICE"),
            "CUSTOM",        Set.of("DEBIT_NOTE", "INVOICE"),
            "OTHER_CHARGES", Set.of("DEBIT_NOTE", "INVOICE"),
            "FDA",           Set.of("DEBIT_NOTE"),
            "FDA_DIRECT",    Set.of("DEBIT_NOTE"),
            "FF",            Set.of("DEBIT_NOTE"),
            "VOYAGE",        Set.of("DEBIT_NOTE")
    );

    private void preSaveValidate(DebitNoteHeaderDto dto) {
        // V8 — party and refType required
        if (dto.getPartyPoid() == null) {
            throw new ValidationException("Party is required");
        }
        if (StringUtils.isBlank(dto.getRefType())) {
            throw new ValidationException("Ref Type is required");
        }

        // V2 — transaction date must be after 31-DEC-2018
        LocalDate cutoff = LocalDate.of(2018, 12, 31);
        if (dto.getTransactionDate() == null || !dto.getTransactionDate().isAfter(cutoff)) {
            throw new ValidationException("Transaction Date must be after 31-DEC-2018");
        }

        // V7 — BHD amount required when currency is not BHD
        if (dto.getCurrencyCode() != null && !"BHD".equalsIgnoreCase(dto.getCurrencyCode())
                && dto.getBhdAmount() == null && dto.getOtherCurrAmount() == null) {
            throw new ValidationException("BHD Amount or Other Currency Amount is required when currency is not BHD");
        }

        // V3 — credit period must not exceed CREDIT_PERIOD_VALIDATION_DAYS parameter
        if (dto.getCreditPeriod() != null) {
            String groupPoidStr = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid().toString() : "0";
            String maxDaysStr = globalParameterService.getParameterValue(
                    "CREDIT_PERIOD_VALIDATION_DAYS", "GROUP", groupPoidStr, "120");
            int maxDays = 120;
            try { maxDays = Integer.parseInt(maxDaysStr); } catch (NumberFormatException ignored) {}
            if (dto.getCreditPeriod() > maxDays) {
                throw new ValidationException("Credit Period cannot exceed " + maxDays + " days");
            }
        }

        // V10 — posting narration max 2000 chars
        if (dto.getPostingNarration() != null && dto.getPostingNarration().length() > 2000) {
            throw new ValidationException("Posting Narration cannot exceed 2000 characters");
        }

        // V6 — strip blank rows before further validation
        if (dto.getGlDetails() != null) {
            dto.setGlDetails(dto.getGlDetails().stream().filter(gl -> !gl.isEmpty()).collect(Collectors.toList()));
        }
        if (dto.getChargeDetails() != null) {
            dto.setChargeDetails(dto.getChargeDetails().stream().filter(c -> !c.isEmpty()).collect(Collectors.toList()));
        }
        if (dto.getOtherChargeDetails() != null) {
            dto.setOtherChargeDetails(dto.getOtherChargeDetails().stream().filter(c -> !c.isEmpty()).collect(Collectors.toList()));
        }

        // V9 — per-refType detail list non-empty
        String rt = dto.getRefType().toUpperCase();
        boolean needsGl = "GENERAL".equals(rt) || "CUSTOM".equals(rt);
        boolean needsCharges = "FDA".equals(rt) || "FDA_DIRECT".equals(rt) || "OTHER_CHARGES".equals(rt);
        if (needsGl && (dto.getGlDetails() == null || dto.getGlDetails().isEmpty())) {
            throw new ValidationException("GL Details are required for Ref Type: " + dto.getRefType());
        }
        if (needsCharges && (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty())) {
            throw new ValidationException("Charge Details are required for Ref Type: " + dto.getRefType());
        }

        // V1 — voucher type × ref type matrix (gated by parameter)
        if (dto.getVoucherType() != null) {
            String groupPoidStr = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid().toString() : "0";
            String voucherValidationEnabled = globalParameterService.getParameterValue(
                    "DEBIT_NOTE_VOUCHER_TYPE_VALIDATION_ENABLE", "GROUP", groupPoidStr, "N");
            if ("Y".equalsIgnoreCase(voucherValidationEnabled)) {
                Set<String> allowed = ALLOWED_VOUCHER_TYPES.get(rt);
                if (allowed != null && !allowed.contains(dto.getVoucherType().toUpperCase())) {
                    throw new ValidationException("Voucher Type '" + dto.getVoucherType()
                            + "' is not allowed for Ref Type '" + dto.getRefType() + "'");
                }
            }
        }

        // V11 — voyage ref required when parameter VOYAGE_REF_IN_DN_CN = Y
        if ("VOYAGE".equals(rt)) {
            String groupPoidStr = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid().toString() : "0";
            String voyageRefRequired = globalParameterService.getParameterValue(
                    "VOYAGE_REF_IN_DN_CN", "GROUP", groupPoidStr, "N");
            if ("Y".equalsIgnoreCase(voyageRefRequired) && StringUtils.isBlank(dto.getVoyageRef())) {
                throw new ValidationException("Voyage Reference is required");
            }
        }

        // V4 — chargeAmount >= pdaAmount per charge row
        if (dto.getChargeDetails() != null) {
            for (DebitNoteChargeDetailDto charge : dto.getChargeDetails()) {
                if ("ISDELETED".equals(charge.getActionType() != null ? charge.getActionType().trim().toUpperCase() : "")) continue;
                if (charge.getChargeAmount() != null && charge.getCostAmount() != null
                        && charge.getChargeAmount().compareTo(charge.getCostAmount()) < 0) {
                    throw new ValidationException("Charge Amount is less than Cost Amount");
                }
            }
        }

        // V5 — grand total must match sum of charge totals for FDA/FDA_DIRECT/OTHER_CHARGES
        if ("FDA".equals(rt) || "FDA_DIRECT".equals(rt) || "OTHER_CHARGES".equals(rt)) {
            if (dto.getChargeDetails() != null && !dto.getChargeDetails().isEmpty() && dto.getGrandTotal() != null) {
                BigDecimal chargesTotal = dto.getChargeDetails().stream()
                        .filter(c -> !"ISDELETED".equals(c.getActionType() != null ? c.getActionType().trim().toUpperCase() : ""))
                        .filter(c -> c.getTotalAmount() != null)
                        .map(DebitNoteChargeDetailDto::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (chargesTotal.compareTo(BigDecimal.ZERO) > 0
                        && chargesTotal.compareTo(dto.getGrandTotal()) != 0) {
                    throw new ValidationException("Grand Total (" + dto.getGrandTotal()
                            + ") does not match sum of charge totals (" + chargesTotal + ")");
                }
            }
        }

        // Sail-date enforcement for FDA / FDA_DIRECT
        if ("FDA".equals(rt) || "FDA_DIRECT".equals(rt)) {
            Long fdaPoid = "FDA_DIRECT".equals(rt) ? dto.getFdaDirectRefPoid() : dto.getFdaRefPoid();
            if (fdaPoid != null) {
                try {
                    Map<String, Object> sailResult = debitNoteCustomRepository.checkSailDate(fdaPoid);
                    Object sailDateObj = sailResult.get("sailDate");
                    if (sailDateObj instanceof java.sql.Date sqlDate) {
                        LocalDate sailDate = sqlDate.toLocalDate();
                        if (dto.getTransactionDate() != null && dto.getTransactionDate().isBefore(sailDate)) {
                            String warning = "Transaction Date (" + dto.getTransactionDate()
                                    + ") is before FDA Sail Date (" + sailDate + "). Please verify.";
                            log.warn(warning);
                            if (dto.getWarnings() == null) dto.setWarnings(new ArrayList<>());
                            dto.getWarnings().add(warning);
                        }
                    }
                } catch (ValidationException ve) {
                    throw ve;
                } catch (Exception e) {
                    log.warn("Sail date check failed for FDA POID {}: {}", fdaPoid, e.getMessage());
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stored-procedure save-path wiring (P1–P5)
    // -------------------------------------------------------------------------

    private void runPreSaveProcedures(Long transactionPoid, DebitNoteHeaderDto dto) {
        Long gp = UserContext.getGroupPoid();
        Long cp = UserContext.getCompanyPoid();
        Long up = UserContext.getUserPoid();
        String rt = dto.getRefType() != null ? dto.getRefType().toUpperCase() : "";

        // P1: PROC_GL_JOB_VAL_BEFORE_SAVE — FDA / FDA_DIRECT / FF only
        if ("FDA".equals(rt) || "FDA_DIRECT".equals(rt) || "FF".equals(rt)) {
            String jobStatus = debitNoteProcedureRepository.validateJobBeforeSave(
                    gp, cp, up, transactionPoid, dto.getRefType(), dto.getFdaRefPoid() != null ? dto.getFdaRefPoid() : dto.getFdaDirectRefPoid());
            if (jobStatus != null && (jobStatus.contains("CLOSED") || jobStatus.contains("ERROR"))) {
                throw new ValidationException(jobStatus);
            }
        }

        // P2: PROC_GL_DEBIT_NOTE_PTY_CMP_CHK — only when propertyInvoice is explicitly set
        if (dto.getPropertyInvoice() != null) {
            String ptyStatus = debitNoteProcedureRepository.validateDebitNotePartyCompany(
                    gp, cp, up, Boolean.TRUE.equals(dto.getPropertyInvoice()) ? "Y" : "N");
            if (ptyStatus != null && !ptyStatus.isBlank() && !isSuccess(ptyStatus)) {
                if (dto.getWarnings() == null) dto.setWarnings(new java.util.ArrayList<>());
                dto.getWarnings().add(ptyStatus);
            }
        }

        // P3: PROC_DN_CHECK_GL_L_A — GENERAL / CUSTOM only; only cursor rows block save (STATUS ERROR is non-blocking in legacy)
        if ("GENERAL".equals(rt) || "CUSTOM".equals(rt)) {
            String glResult = debitNoteProcedureRepository.checkGlLedgerAccount(gp, cp, up, buildGlList(dto));
            if (glResult != null && glResult.startsWith("ERROR:")) {
                throw new ValidationException(glResult);
            }
        }

        // P4: PROC_AR_DN_BEFORE_SAVE_VAL — only when partyPoid is set (required field, but guard matches legacy)
        if (dto.getPartyPoid() != null) {
            java.math.BigDecimal taxAmount = java.math.BigDecimal.ZERO;
            if (("FDA".equals(rt) || "FDA_DIRECT".equals(rt)) && dto.getChargeDetails() != null) {
                taxAmount = dto.getChargeDetails().stream()
                        .filter(c -> !"ISDELETED".equals(c.getActionType() != null ? c.getActionType().trim().toUpperCase() : ""))
                        .filter(c -> c.getTaxAmount() != null)
                        .map(c -> c.getTaxAmount())
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            } else if ("OTHER_CHARGES".equals(rt) && dto.getOtherChargeDetails() != null) {
                taxAmount = dto.getOtherChargeDetails().stream()
                        .filter(c -> !"ISDELETED".equals(c.getActionType() != null ? c.getActionType().trim().toUpperCase() : ""))
                        .filter(c -> c.getTaxAmount() != null)
                        .map(c -> c.getTaxAmount())
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            }
            String bsStatus = debitNoteProcedureRepository.validateBeforeSave(
                    gp, cp, up, dto.getDocRef(), transactionPoid,
                    dto.getPartyPoid(), taxAmount, dto.getTransactionDate());
            if (bsStatus != null && bsStatus.contains("WARNING")) {
                if (dto.getWarnings() == null) dto.setWarnings(new java.util.ArrayList<>());
                dto.getWarnings().add(bsStatus);
            } else if (bsStatus != null && bsStatus.contains("ERROR")) {
                throw new ValidationException(bsStatus);
            }
        }

        // NOTE: PROC_GL_VOUCHERS_VALIDATIONS is NOT called on save in legacy —
        // it runs only on before-edit and LOV-change events, so it is excluded here.
    }

    private String buildGlList(DebitNoteHeaderDto dto) {
        StringBuilder sb = new StringBuilder("0");
        if (dto.getGlDetails() != null) {
            for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
                if ("ISDELETED".equals(gl.getActionType() != null ? gl.getActionType().trim().toUpperCase() : "")) continue;
                if (gl.getGlId() != null) {
                    sb.append("~").append(gl.getGlId());
                }
            }
        }
        return sb.toString();
    }

    private boolean isSuccess(String status) {
        if (status == null || status.trim().isEmpty()) return true;
        String s = status.trim();
        return "SUCCESS".equalsIgnoreCase(s) || "SUCESS".equalsIgnoreCase(s);
    }

    private boolean isFdaLike(String refType) {
        if (refType == null) return false;
        String rt = refType.toUpperCase();
        return "FDA".equals(rt) || "FDA_DIRECT".equals(rt) || "FF".equals(rt);
    }

    // -------------------------------------------------------------------------
    // New service-interface methods
    // -------------------------------------------------------------------------

    @Override
    public Map<String, Object> processFdaCharges(ProcessFdaRequestDto req) {
        if (req == null || req.getFdaPoid() == null) {
            throw new ValidationException("FDA POID is required");
        }
        return debitNoteCustomRepository.loadFdaCharges(req.getFdaPoid());
    }

    @Override
    public Map<String, Object> processFdaDirectCharges(ProcessFdaRequestDto req) {
        if (req == null || req.getFdaPoid() == null) {
            throw new ValidationException("FDA Direct POID is required");
        }
        return debitNoteCustomRepository.loadFdaCharges(req.getFdaPoid());
    }

    @Override
    public Map<String, Object> getDnParameters() {
        String groupPoidStr = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid().toString() : "0";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("globalTaxApplicable",
                globalParameterService.getParameterValue("GLOBAL_TAX_APPLICABLE", "GROUP", groupPoidStr, "N"));
        params.put("creditPeriodValidationDays",
                globalParameterService.getParameterValue("CREDIT_PERIOD_VALIDATION_DAYS", "GROUP", groupPoidStr, "120"));
        params.put("voyageRefInDnCn",
                globalParameterService.getParameterValue("VOYAGE_REF_IN_DN_CN", "GROUP", groupPoidStr, "N"));
        params.put("debitNoteVoucherTypeValidationEnable",
                globalParameterService.getParameterValue("DEBIT_NOTE_VOUCHER_TYPE_VALIDATION_ENABLE", "GROUP", groupPoidStr, "N"));
        params.put("printCompanyInDn",
                globalParameterService.getParameterValue("PRINT_COMPANY_IN_DN", "GROUP", groupPoidStr, "N"));
        params.put("propertyInvoiceInDn",
                globalParameterService.getParameterValue("PROPERTY_INVOICE_IN_DN", "GROUP", groupPoidStr, "N"));
        return params;
    }

    @Override
    @Transactional
    public void releaseJobValues(Long transactionPoid) {
        ArDebitNoteHdr entity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));
        if (isFdaLike(entity.getRefType())) {
            Long refPoid = "FDA_DIRECT".equalsIgnoreCase(entity.getRefType())
                    ? parseLongSafely(entity.getFdaDirectRef())
                    : parseLongSafely(entity.getFdaRef());
            if (refPoid != null) {
                try {
                    debitNoteProcedureRepository.releaseJobOldValues(
                            entity.getGroupPoid(), entity.getCompanyPoid(),
                            UserContext.getUserPoid(), transactionPoid,
                            entity.getRefType(), refPoid);
                    log.info("Released job old values for transaction {} refType {} refPoid {}",
                            transactionPoid, entity.getRefType(), refPoid);
                } catch (Exception e) {
                    log.error("releaseJobOldValues failed for transaction {}: {}", transactionPoid, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Map<String, String> getCustomLovList(String costGroup) {
        if (costGroup == null || costGroup.trim().isEmpty()) {
            return Map.of(
                    "customLovList", "ChargePoid=DEBIT_NOTE_OTHER_CHARGES,ChargePoid,POID,false,250,false;CostPoid=DN_GL_COST_CENTRE,CostPoid,CODE,false,180,false;"
            );
        }

        String customLovList = "ChargePoid=DEBIT_NOTE_OTHER_CHARGES,ChargePoid,POID,false,250,false;";
        String lovName = "DN_GL_COST_CENTRE";

        switch (costGroup.toUpperCase()) {
            case "GL_SH_BLS":
                lovName = "GL_SH_BLS";
                break;
            case "GL_FDA_JOBS":
                lovName = "GL_FDA_JOBS";
                break;
            case "GL_FF_JOBS":
                lovName = "GL_FF_JOBS";
                break;
            case "GL_FFP_JOBS":
                lovName = "GL_FFP_JOBS";
                break;
            case "ANOOD_MANSION":
                lovName = "ANOOD_MANSION";
                break;
            case "NAJOOD_MANSION":
                lovName = "NAJOOD_MANSION";
                break;
            case "PROPERTIES":
                lovName = "DN_PROPERTIES";
                break;
            case "GL_COST_CENTRE":
                lovName = "DN_GL_COST_CENTRE";
                break;
            case "FIXED_ASSET":
                lovName = "DN_FIXED_ASSET";
                break;
            case "OASIS":
                lovName = "DN_OASIS";
                break;
            default:
                lovName = "DN_GL_COST_CENTRE";
        }

        customLovList += "CostPoid=" + lovName + ",CostPoid,CODE,false,180,false;";

        return Map.of(
                "customLovList", customLovList,
                "costGroup", costGroup,
                "lovName", lovName
        );
    }

}
