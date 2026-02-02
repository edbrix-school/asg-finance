package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Value("${app.doc-id.debit-note:300-110}")
    private String debitNoteDocId;

    @Override
    @Transactional
    public DebitNoteHeaderDto createDebitNote(DebitNoteHeaderDto debitNoteDto) {

        // VALIDATION BEFORE SAVE
        validateDebitNoteInput(debitNoteDto);

        applyBusinessLogic(debitNoteDto);

        ArDebitNoteHdr entity = mapToEntity(debitNoteDto);
        ArDebitNoteHdr savedEntity = debitNoteHdrRepository.saveAndFlush(entity);

        // Save details (GL + Charge) — GL will be saved if provided regardless of refType
        saveDetails(debitNoteDto, savedEntity.getTransactionPoid());

        // --- INSERT BILLWISE & COST CENTER BREAKUPS (minimal changes) ---
        insertBillwiseBreakups(debitNoteDto, savedEntity);
        insertCostCenterBreakups(debitNoteDto, savedEntity);

        // Build response by reading saved header + details from DB (so DB-generated fields are included)
        DebitNoteHeaderDto result = mapToDto(savedEntity);
        loadDetails(result, savedEntity.getTransactionPoid(), savedEntity.getRefType());

        // Load breakups into response
        loadBreakups(result, savedEntity.getTransactionPoid());

        // Log the creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), savedEntity.getTransactionPoid().toString());

        return result;
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

        existingEntity.setGroupPoid(UserContext.getGroupPoid());
        existingEntity.setCompanyPoid(UserContext.getCompanyPoid());

        existingEntity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        existingEntity.setLastModifiedDate(LocalDateTime.now());
        debitNoteHdrRepository.save(existingEntity);

        deleteExistingDetails(transactionPoid);
        saveDetails(debitNoteDto, transactionPoid);

        // --- UPDATE BILLWISE & COST CENTER BREAKUPS ---
        updateBillwiseBreakups(debitNoteDto, existingEntity.getTransactionPoid(), existingEntity.getGroupPoid(), existingEntity.getCompanyPoid());
        updateCostCenterBreakups(debitNoteDto, existingEntity.getTransactionPoid(), existingEntity.getGroupPoid(), existingEntity.getCompanyPoid());

        // return database-backed DTO (with details loaded from DB)
        DebitNoteHeaderDto result = mapToDto(existingEntity);
        loadDetails(result, transactionPoid, existingEntity.getRefType());

        // Load breakups into response
       // loadBreakups(result, transactionPoid);

        // Log the update
        loggingService.logChanges(oldEntity, existingEntity, ArDebitNoteHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return result;
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

    private String getOldJobPoid(ArDebitNoteHdr entity, String refType) {
        return switch (refType != null ? refType.toUpperCase() : "") {
            case "FDA JOBS" -> entity.getFdaRef();
            case "FF JOBS" -> entity.getFfRef() != null ? entity.getFfRef() : null;
            default -> null;
        };
    }

    @Override
    public DebitNoteHeaderDto getDebitNote(Long transactionPoid) {
        ArDebitNoteHdr entity = debitNoteHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", "transactionPoid", transactionPoid));

        DebitNoteHeaderDto dto = mapToDto(entity);
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

        if (!"BHD".equals(debitNoteDto.getCurrencyCode()) && debitNoteDto.getCurrencyRate() != null && debitNoteDto.getGrandTotal() != null) {
            debitNoteDto.setBhdAmount(debitNoteDto.getGrandTotal().multiply(debitNoteDto.getCurrencyRate()));
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

    private void deleteExistingDetails(Long transactionPoid) {
        debitNoteDtlRepository.deleteByTransactionPoid(transactionPoid);
        debitNoteChargeDtlRepository.deleteByTransactionPoid(transactionPoid);
    }

    private void loadDetails(DebitNoteHeaderDto dto, Long transactionPoid, String refType) {
        // always read GL details (if any)
        List<ArDebitNoteDtl> glDetails = debitNoteDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setGlDetails(glDetails.stream().map(this::mapGlDetailToDto).collect(Collectors.toList()));

        // read charge details as well
        List<ArDebitNoteChargeDtl> chargeDetails = debitNoteChargeDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setChargeDetails(chargeDetails.stream().map(this::mapChargeDetailToDto).collect(Collectors.toList()));
    }

    private void saveGlDetails(List<DebitNoteGlDetailDto> glDetails, Long transactionPoid) {
        debitNoteDtlRepository.deleteByTransactionPoid(transactionPoid);

        Long detRowId = 0L;
        String user = ASGHelperUtils.getCurrentUser();

        for (DebitNoteGlDetailDto dto : glDetails) {
            detRowId++;

            ArDebitNoteDtl entity = new ArDebitNoteDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(detRowId);
            entity.setType(dto.getType());
            entity.setCompanyPoid(UserContext.getCompanyPoid());
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
            detRowId++;

            ArDebitNoteChargeDtl entity = new ArDebitNoteChargeDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(detRowId);
            entity.setChargePoid(dto.getChargeId());
            entity.setChargeAmount(dto.getChargeAmount());
            entity.setRemarks(dto.getRemarks());
            entity.setTaxPoid(dto.getTaxId());
            entity.setTaxPercentage(dto.getTaxPercentage());
            entity.setTaxAmount(dto.getTaxAmount());
            entity.setCostAmount(dto.getCostAmount());
            entity.setPrintSeqNo(dto.getSeqNo());
            entity.setCreatedBy(user);
            entity.setCreatedDate(LocalDateTime.now());

            debitNoteChargeDtlRepository.save(entity);
        }
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
        entity.setRefType(dto.getRefType());
        entity.setDrTotal(dto.getDrTotal());
        entity.setCrTotal(dto.getCrTotal());
        entity.setPostingNarration(dto.getPostingNarration());
        entity.setGrandTotal(dto.getGrandTotal());
        entity.setDueDate(dto.getDueDate());
        entity.setCreditPeriod(dto.getCreditPeriod());
        entity.setPoRef(dto.getPoRef());
        entity.setBankPoid(dto.getBankPoid());
        entity.setTinNumber(dto.getTinNumber());
        entity.setBhdAmount(dto.getBhdAmount());
        entity.setVoucherType(dto.getVoucherType());
        entity.setCostRefNumber(dto.getCostRefNumber());
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
        dto.setBankPoid(entity.getBankPoid());
        dto.setTinNumber(entity.getTinNumber());
        dto.setBhdAmount(entity.getBhdAmount());
        dto.setVoucherType(entity.getVoucherType());
        dto.setCostRefNumber(entity.getCostRefNumber());
        dto.setPrintDivisionPoid(entity.getPrintDivisionPoid());
        dto.setMultiCompany("Y".equals(entity.getMultiCompany()));
        dto.setRemarksPrintable("Y".equals(entity.getRemarksPrintable()));
        dto.setShowBankDetailsInPrint("Y".equals(entity.getShowBankDetailsInPrint()));
        dto.setDeleted(entity.getDeleted());
        dto.setDocRef(entity.getDocRef());
        dto.setVoyageRef(entity.getVoyageRef());
        return dto;
    }

    private DebitNoteGlDetailDto mapGlDetailToDto(ArDebitNoteDtl entity) {
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

        return dto;
    }

    private DebitNoteChargeDetailDto mapChargeDetailToDto(ArDebitNoteChargeDtl entity) {
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
                                    BillwiseBreakupPopupRequestDto bw = new BillwiseBreakupPopupRequestDto();

                                    // billwise DTO does NOT have GL-level fields
                                    bw.setBillDetRowId(x.getBillDetRowId());
                                    bw.setBillRefType(x.getBillRefType());
                                    bw.setBillRef(x.getBillRef());
                                    bw.setBillDueDate(x.getBillDueDate());
                                    bw.setAmount(bw.getAmount());
                                    bw.setBillRemarks(x.getBillRemarks());

                                    return bw;
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

}

