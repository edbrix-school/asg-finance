package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.*;
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
import lombok.RequiredArgsConstructor;
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

import javax.sql.DataSource;
import java.math.BigDecimal;
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
 *  - Billwise breakup (create/update/get)
 *  - Cost center breakup (create/update/get)
 *
 * Minimal changes only — existing logic preserved.
 *
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

    @Value("${app.doc-id.debit-note:300-110}")
    private String debitNoteDocId;

    @Override
    @Transactional
    public DebitNoteHeaderDto createDebitNote(DebitNoteHeaderDto debitNoteDto) {

        // VALIDATION BEFORE SAVE
        validateDebitNoteInput(debitNoteDto);

        applyBusinessLogic(debitNoteDto);
        applyAutoBalancing(debitNoteDto);

        ArDebitNoteHdr entity = mapToEntity(debitNoteDto);
        ArDebitNoteHdr savedEntity = debitNoteHdrRepository.saveAndFlush(entity);

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
        loadDetails(result, savedEntity.getTransactionPoid(), savedEntity.getRefType());

        // Load breakups into response
        loadBreakups(result, savedEntity.getTransactionPoid());

        // Log the creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), savedEntity.getTransactionPoid().toString());

        return getDebitNote(savedEntity.getTransactionPoid());
    }

    @Override
    @Transactional
    public DebitNoteHeaderDto updateDebitNote(Long transactionPoid, DebitNoteHeaderDto debitNoteDto) {
        ArDebitNoteHdr existingEntity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));

        // Create a copy of the old entity for logging
        ArDebitNoteHdr oldEntity = new ArDebitNoteHdr();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        validateDebitNoteInput(debitNoteDto);
        // Validate using stored procedure for Edit

        if (debitNoteDto.getRefType().equals("FDA JOBS") || debitNoteDto.getRefType().equals("FF JOBS"))
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
        existingEntity.setTransactionDate(LocalDate.now());
        debitNoteHdrRepository.save(existingEntity);

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
        loadDetails(result, transactionPoid, existingEntity.getRefType());

        // Load breakups into response
       // loadBreakups(result, transactionPoid);

        // Log the update
        loggingService.logChanges(oldEntity, existingEntity, ArDebitNoteHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), transactionPoid.toString());
        if (!detailSummaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(detailSummaryLogs);
        }

        return getDebitNote(transactionPoid);
    }

    @Override
    @Transactional
    public void deleteDebitNote(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        ArDebitNoteHdr entity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));
        documentDeleteService.deleteDocument(
                transactionPoid,
                "AR_DEBIT_NOTE_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                entity.getTransactionDate()
        );
    }

    @Override
    public DebitNoteHeaderDto getDebitNote(Long transactionPoid) {
        ArDebitNoteHdr entity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));

        DebitNoteHeaderDto dto = mapToDto(entity);
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        loadDetails(dto, transactionPoid, entity.getRefType());

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

        if (!"BHD".equals(debitNoteDto.getCurrencyCode())
                && debitNoteDto.getCurrencyRate() != null
                && debitNoteDto.getGrandTotal() != null) {
            BigDecimal newBhd = debitNoteDto.getGrandTotal().multiply(debitNoteDto.getCurrencyRate());
            if (debitNoteDto.getBhdAmount() == null || debitNoteDto.getBhdAmount().compareTo(newBhd) != 0) {
                debitNoteDto.setBhdAmount(newBhd);
            }
        }
    }

    /**
     * Save details:
     *  - Always save GL details if provided (user requested this change so FDA manual GLs persist)
     *  - Save charge details when provided (FDA/FDA_DIRECT/OTHER_CHARGES)
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
        List<ArDebitNoteDtl> newlyCreated = new ArrayList<>();
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
            String actionType = dto.getActionType();
            if (actionType == null || actionType.trim().isEmpty()) {
                actionType = (dto.getDetRowId() == null) ? "ISCREATED" : "ISUPDATED";
            } else {
                actionType = actionType.trim().toUpperCase();
            }
            if ("NOCHANGES".equals(actionType)) {
                actionType = "NOCHANGE";
            }
            if ("ISUPDATED".equals(actionType) && dto.getDetRowId() == null) {
                actionType = "ISCREATED";
            }

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
                    newlyCreated.add(newEntity);
                    break;
                }
                case "ISUPDATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        break;
                    }
                    ArDebitNoteDtl existing = existingMap.get(detRowId);
                    if (existing == null) {
                        Long newDetRowId = detRowId;
                        if (newDetRowId > nextDetRowId) {
                            nextDetRowId = newDetRowId;
                        }
                        ArDebitNoteDtl newEntity = new ArDebitNoteDtl();
                        mapGlDtoToEntity(dto, newEntity, transactionPoid);
                        newEntity.setDetRowId(newDetRowId);
                        newEntity.setCreatedBy(currentUser);
                        newEntity.setCreatedDate(now);
                        newEntity.setLastModifiedBy(currentUser);
                        newEntity.setLastModifiedDate(now);
                        toSave.add(newEntity);
                        newlyCreated.add(newEntity);
                        break;
                    }

                    ArDebitNoteDtl oldEntity = new ArDebitNoteDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapGlDtoToEntity(dto, existing, transactionPoid);
                    existing.setLastModifiedBy(currentUser);
                    existing.setLastModifiedDate(now);
                    toSave.add(existing);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ArDebitNoteDtl.class, docId, docKeyPoid, logDetail));
                    break;
                }
                case "ISDELETED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        break;
                    }
                    toDelete.add(detRowId);
                    ArDebitNoteDtl oldEntityForDelete = existingMap.get(detRowId);
                    if (oldEntityForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, glPoid:%s, drAmt:%s, crAmt:%s, remarks:%s",
                                oldEntityForDelete.getDetRowId(), transactionPoid, oldEntityForDelete.getGlPoid(),
                                oldEntityForDelete.getDrAmt(), oldEntityForDelete.getCrAmt(), oldEntityForDelete.getRemarks());
                        String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                        summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage));
                    }
                    break;
                }
                case "NOCHANGE":
                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            debitNoteDtlRepository.saveAll(toSave);
            for (ArDebitNoteDtl newlyCreatedEntity : newlyCreated) {
                if (newlyCreatedEntity.getDetRowId() != null) {
                    String summaryMessage = String.format("Row Created on Debit Note GL Detail with DetRowId: %s", newlyCreatedEntity.getDetRowId());
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, summaryMessage));
                }
            }
        }
        if (!toDelete.isEmpty()) {
            debitNoteDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void updateChargeDetailsWithLogging(List<DebitNoteChargeDetailDto> chargeDetails, Long transactionPoid, List<GlobalLogSummary> summaryLogs) {
        if (chargeDetails == null || chargeDetails.isEmpty()) return;

        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<ArDebitNoteChargeDtl> toSave = new ArrayList<>();
        List<ArDebitNoteChargeDtl> newlyCreated = new ArrayList<>();
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
            String actionType = dto.getActionType();
            if (actionType == null || actionType.trim().isEmpty()) {
                actionType = (dto.getDetRowId() == null) ? "ISCREATED" : "ISUPDATED";
            } else {
                actionType = actionType.trim().toUpperCase();
            }
            if ("NOCHANGES".equals(actionType)) {
                actionType = "NOCHANGE";
            }
            if ("ISUPDATED".equals(actionType) && dto.getDetRowId() == null) {
                actionType = "ISCREATED";
            }

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
                    newlyCreated.add(newEntity);
                    break;
                }
                case "ISUPDATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        break;
                    }
                    ArDebitNoteChargeDtl existing = existingMap.get(detRowId);
                    if (existing == null) {
                        Long newDetRowId = detRowId;
                        if (newDetRowId > nextDetRowId) {
                            nextDetRowId = newDetRowId;
                        }
                        ArDebitNoteChargeDtl newEntity = new ArDebitNoteChargeDtl();
                        mapChargeDtoToEntity(dto, newEntity, transactionPoid);
                        newEntity.setDetRowId(newDetRowId);
                        newEntity.setCreatedBy(currentUser);
                        newEntity.setCreatedDate(now);
                        newEntity.setLastModifiedBy(currentUser);
                        newEntity.setLastModifiedDate(now);
                        toSave.add(newEntity);
                        newlyCreated.add(newEntity);
                        break;
                    }

                    ArDebitNoteChargeDtl oldEntity = new ArDebitNoteChargeDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapChargeDtoToEntity(dto, existing, transactionPoid);
                    existing.setLastModifiedBy(currentUser);
                    existing.setLastModifiedDate(now);
                    toSave.add(existing);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ArDebitNoteChargeDtl.class, docId, docKeyPoid, logDetail));
                    break;
                }
                case "ISDELETED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        break;
                    }
                    toDelete.add(detRowId);
                    ArDebitNoteChargeDtl oldEntityForDelete = existingMap.get(detRowId);
                    if (oldEntityForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, chargePoid:%s, chargeAmount:%s, remarks:%s",
                                oldEntityForDelete.getDetRowId(), transactionPoid, oldEntityForDelete.getChargePoid(),
                                oldEntityForDelete.getChargeAmount(), oldEntityForDelete.getRemarks());
                        String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                        summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage));
                    }
                    break;
                }
                case "NOCHANGE":
                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            debitNoteChargeDtlRepository.saveAll(toSave);
            for (ArDebitNoteChargeDtl newlyCreatedEntity : newlyCreated) {
                if (newlyCreatedEntity.getDetRowId() != null) {
                    String summaryMessage = String.format("Row Created on Debit Note Charge Detail with DetRowId: %s", newlyCreatedEntity.getDetRowId());
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, summaryMessage));
                }
            }
        }
        if (!toDelete.isEmpty()) {
            debitNoteChargeDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void deleteExistingDetails(Long transactionPoid) {
        debitNoteDtlRepository.deleteByTransactionPoid(transactionPoid);
        debitNoteChargeDtlRepository.deleteByTransactionPoid(transactionPoid);
    }

    private void loadDetails(DebitNoteHeaderDto dto, Long transactionPoid, String refType) {
        // always read GL details (if any)
        List<ArDebitNoteDtl> glDetails = debitNoteDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setGlDetails(glDetails.stream().map(entity -> mapGlDetailToDto(entity, refType)).collect(Collectors.toList()));

        // read charge details as well
        List<ArDebitNoteChargeDtl> chargeDetails = debitNoteChargeDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setChargeDetails(chargeDetails.stream().map(entity -> mapChargeDetailToDto(entity, refType)).collect(Collectors.toList()));
    }

    private void saveGlDetails(List<DebitNoteGlDetailDto> glDetails, Long transactionPoid) {
        debitNoteDtlRepository.deleteByTransactionPoid(transactionPoid);

        Long detRowId = 0L;
        String user = ASGHelperUtils.getCurrentUser();

        for (DebitNoteGlDetailDto dto : glDetails) {
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

            debitNoteDtlRepository.save(entity);
        }
    }

    private void saveChargeDetails(List<DebitNoteChargeDetailDto> chargeDetails, Long transactionPoid) {
        debitNoteChargeDtlRepository.deleteByTransactionPoid(transactionPoid);

        Long detRowId = 0L;
        String user = ASGHelperUtils.getCurrentUser();

        for (DebitNoteChargeDetailDto dto : chargeDetails) {
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

            debitNoteChargeDtlRepository.save(entity);
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
        entity.setCostAmount(dto.getCostAmount());
        entity.setCostPoid(dto.getCostPoid());
        entity.setCostGroup(dto.getCostGroup() != null ? dto.getCostGroup().toString() : null);
        entity.setCheckAll(dto.getCheckAll());
        entity.setPrintSeqNo(dto.getSeqNo());
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
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

        entity.setTransactionDate(LocalDate.now());
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
        entity.setPrintDivisionPoid(dto.getPrintDivisionPoid() != null ? dto.getPrintDivisionPoid() : 1L);
        entity.setMultiCompany(dto.getMultiCompany() != null && dto.getMultiCompany() ? "Y" : "N");
        entity.setRemarksPrintable(dto.getRemarksPrintable() != null && dto.getRemarksPrintable() ? "Y" : "N");
        entity.setShowBankDetailsInPrint(dto.getShowBankDetailsInPrint() != null && dto.getShowBankDetailsInPrint() ? "Y" : "N");

        entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setVoyageRef(dto.getVoyageRef());

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
        dto.setCostGroupPoid(parseLongSafely(entity.getCostGroup()));
        dto.setPrintDivisionPoid(entity.getPrintDivisionPoid());
        dto.setMultiCompany("Y".equals(entity.getMultiCompany()));
        dto.setRemarksPrintable("Y".equals(entity.getRemarksPrintable()));
        dto.setShowBankDetailsInPrint("Y".equals(entity.getShowBankDetailsInPrint()));
        dto.setDeleted(entity.getDeleted());
        dto.setDocRef(entity.getDocRef());
        dto.setVoyageRef(entity.getVoyageRef());
        
        // Populate header LOV details
        if (dto.getFdaRefPoid() != null) {
            dto.setFdaRefDetails(lovService.getDetailsByPoidAndLovName(dto.getFdaRefPoid(), "PROCESS_FDA_IN_PI"));
        }
        if (dto.getFdaDirectRefPoid() != null) {
            dto.setFdaDirectRefDetails(lovService.getDetailsByPoidAndLovName(dto.getFdaDirectRefPoid(), "PROCESS_FDA_DIRECT_IN_DN"));
        }
        if (dto.getCostGroupPoid() != null) {
            dto.setCostGroupDetails(lovService.getDetailsByPoidAndLovName(dto.getCostGroupPoid(), "DN_GL_COST_GROUPS"));
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

    private DebitNoteChargeDetailDto mapChargeDetailToDto(ArDebitNoteChargeDtl entity, String refType) {
        DebitNoteChargeDetailDto dto = new DebitNoteChargeDetailDto();

        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setDetRowId(entity.getDetRowId());
        dto.setChargeId(entity.getChargePoid());
        dto.setChargeAmount(entity.getChargeAmount());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxId(entity.getTaxPoid());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setCostAmount(entity.getCostAmount());
        dto.setSeqNo(entity.getPrintSeqNo());
        dto.setCostPoid(entity.getCostPoid());
        dto.setCostGroup(entity.getCostGroup());
        dto.setCheckAll(entity.getCheckAll());
        dto.setCostAmount(entity.getPdaAmount());
        
        // Populate LOV details based on refType
        if (entity.getChargePoid() != null) {
            String chargeLov = "OTHER_CHARGES".equalsIgnoreCase(refType) 
                ? "DEBIT_NOTE_OTHER_CHARGES" 
                : "CHARGE_MASTER_IN_DN_FOR_SH";
            dto.setChargeDetails(lovService.getDetailsByPoidAndLovName(entity.getChargePoid(), chargeLov));
        }
        if (entity.getTaxPoid() != null) {
            dto.setTaxDetails(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "DR_TAX_MASTER"));
        }

        if (entity.getCostPoid() != null && (refType.equalsIgnoreCase("FDA") || refType.equalsIgnoreCase("FDA_DIRECT") ) ) {
            dto.setCostCenterDetails(lovService.getDetailsByPoidAndLovName(Long.valueOf(entity.getCostPoid()), "DN_GL_COST_CENTRE"));
        }
        
        return dto;
    }

    // ---------------------------
    // Breakup helpers
    // ---------------------------

    private void insertBillwiseBreakups(DebitNoteHeaderDto dto, ArDebitNoteHdr savedEntity) {
        if (dto.getGlDetails() == null) return;

        List<BillwiseBreakupRequestDto> billwiseRequests = new ArrayList<>();

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
                req.setBillDetRowId(bw.getBillDetRowId());
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

        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
            if (gl.getBreakupList() == null || gl.getBreakupList().isEmpty()) continue;

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
                req.setBillDetRowId(bw.getBillDetRowId());
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
            }
        }

        if (!billwiseRequests.isEmpty()) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseRequests, UserContext.getUserPoid());
        }
    }

    private void updateCostCenterBreakups(DebitNoteHeaderDto dto, Long transactionPoid, Long groupPoid, Long companyPoid) {
        if (dto.getGlDetails() == null) return;

        List<CostCenterBreakupRequestDto> ccRequests = new ArrayList<>();

        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {
            if (gl.getCostCenterList() == null || gl.getCostCenterList().isEmpty()) continue;

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
                req.setCostDetRowId(cb.getCostDetRowId());
                req.setCostGroup(cb.getCostGroup());
                req.setCostPoid(cb.getCostPoid());
                req.setAmount(cb.getAmount());
                req.setLoginUserPoid(UserContext.getUserPoid());

                ccRequests.add(req);
            }
        }

        if (!ccRequests.isEmpty()) {
            costCenterBreakupService.updateCostCenterBreakups(ccRequests, UserContext.getUserPoid());
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
                                    popup.setType(x.getDrAmt().compareTo(BigDecimal.ZERO) > 0 ? "DR" : "CR");
                                    popup.setAmount(x.getDrAmt().compareTo(BigDecimal.ZERO) > 0 ? x.getDrAmt() : x.getCrAmt());
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
                                            x.getAmount() != null ? BigDecimal.valueOf(x.getAmount()) : BigDecimal.ZERO
                                    );
//                                    cb.setGlDescription(x.getDescription());  // optional: SRS uses description as GL desc

                                    if (StringUtils.isNotEmpty(x.getCostPoid()) && StringUtils.isNotEmpty(x.getCostGroup())) {
                                        cb.setCostCenterDetails(lovService.getDetailsByPoidAndLovName(Long.valueOf(x.getCostPoid()), x.getCostGroup()));
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
                throw new ResourceNotFoundException("FDA Reference Poid Not Valid: " ,"FdaRefPoid", dto.getFdaRefPoid());
            }
        }

        if ("FDA_DIRECT".equalsIgnoreCase(dto.getRefType()) && dto.getFdaDirectRefPoid() == null)
        {
            throw new ValidationException("FdaDirectRefPoid is Mandatory for ref Type FDA_DIRECT");
        }
    }

    private void validatePartyPoid(Long partyPoid, String partyType) {
        if (partyPoid == null || partyType == null) return;

        boolean exists = switch (partyType.toUpperCase()) {
            case "SUPPLIER_MASTER_FOR_DN" -> supplierMasterRepository.findBySupplierPoid(partyPoid) != null;
            case "CUSTOMER_MASTER_FOR_DN" -> Boolean.TRUE.equals(salesCustomerMasterRepository.existsByCustomerPoid(partyPoid));
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

    private void applyAutoBalancing(DebitNoteHeaderDto dto) {

        if (dto.getGlDetails() == null || dto.getGlDetails().isEmpty())
            return;

        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;

        for (DebitNoteGlDetailDto gl : dto.getGlDetails()) {

            BigDecimal dr = gl.getDebitAmount() == null ? BigDecimal.ZERO : gl.getDebitAmount();
            BigDecimal cr = gl.getCreditAmount() == null ? BigDecimal.ZERO : gl.getCreditAmount();

            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }

        if (totalDr.compareTo(totalCr) == 0)
            return; // already balanced

        Long partyGlPoid = getPartyGLPoid(dto.getPartyPoid(), dto.getPartyType());

        // check if party GL already exists
        boolean partyRowExists = dto.getGlDetails().stream()
                .anyMatch(gl -> Objects.equals(gl.getGlId(), partyGlPoid));

        if (partyRowExists)
            return;

        BigDecimal difference = totalDr.subtract(totalCr).abs();

        DebitNoteGlDetailDto balancingRow = new DebitNoteGlDetailDto();
        balancingRow.setGlId(partyGlPoid);
        balancingRow.setCompanyPoid(UserContext.getCompanyPoid());
        balancingRow.setRemarks("Auto Balance Entry");

        if (totalDr.compareTo(totalCr) > 0) {
            balancingRow.setType("CR");
            balancingRow.setCreditAmount(difference);
            balancingRow.setDebitAmount(BigDecimal.ZERO);
        } else {
            balancingRow.setType("DR");
            balancingRow.setDebitAmount(difference);
            balancingRow.setCreditAmount(BigDecimal.ZERO);
        }

        balancingRow.setTotalAmount(difference);
        balancingRow.setDetRowId(null); // auto assign later
        balancingRow.setActionType("isCreated");

        dto.getGlDetails().add(balancingRow);
    }

}

