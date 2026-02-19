package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.repository.*;
import com.asg.finance.service.ApPurchaseCnService;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApPurchaseCnServiceImpl implements ApPurchaseCnService {

    private final ApPurchaseCnHdrRepository hdrRepository;
    private final ApPurchaseCnItemDtlRepository itemDtlRepository;
    private final ApPurchaseCnChargeDtlRepository chargeDtlRepository;
    private final ApPurchaseCnGlDtlRepository glDtlRepository;
    private final ApPurchaseCnProcRepository procRepository;
    private final BillwiseBreakupService billwiseBreakupService;
    private final CostCenterBreakupService costCenterBreakupService;
    private final GLMasterRepository glMasterRepository;
    private final TaxMasterRepository taxMasterRepository;
    private final PrintService printService;
    private final DataSource dataSource;
    private final DocumentSearchService documentSearchService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final LovDataService lovService;

    @Override
    @Transactional
    public ApPurchaseCnHdrDto create(ApPurchaseCnHdrDto dto) {
        log.info("Creating supplier credit note for partyType: {}, refType: {}", dto.getPartyType(), dto.getRefType());
        
        try {
            // Before save validation
            Long partyPoid = "SUPPLIER".equals(dto.getPartyType()) ? dto.getSupplierPoid() : dto.getPrincipalPoid();
            procRepository.beforeSaveValidation(dto.getPartyType(), partyPoid, dto.getRefType(), dto.getPjReversalRef());
            
            ApPurchaseCnHdr hdr = mapToEntity(dto);
            hdr.setCreatedBy(UserContext.getUserId());
            hdr.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
            hdr.setDeleted("N");
            
            ApPurchaseCnHdr savedHdr = hdrRepository.save(hdr);
            log.info("Supplier credit note header saved with transactionPoid: {}", savedHdr.getTransactionPoid());
            
            saveDetails(savedHdr.getTransactionPoid(), dto);
            log.info("Supplier credit note created successfully with transactionPoid: {}", savedHdr.getTransactionPoid());
            
            return getById(savedHdr.getTransactionPoid());
        } catch (Exception e) {
            log.error("Error creating supplier credit note: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create supplier credit note: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApPurchaseCnHdrDto getById(Long transactionPoid) {
        log.info("Fetching supplier credit note with transactionPoid: {}", transactionPoid);
        
        try {
            ApPurchaseCnHdr hdr = hdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier Credit Note", "transactionPoid", transactionPoid));
            
            ApPurchaseCnHdrDto dto = mapToDto(hdr);
            
            dto.setItemDetails(itemDtlRepository.findByTransactionPoid(transactionPoid).stream()
                    .map(this::mapItemToDto).collect(Collectors.toList()));
            dto.setChargeDetails(chargeDtlRepository.findByTransactionPoid(transactionPoid).stream()
                    .map(e -> mapChargeToDto(e, hdr.getRefType())).collect(Collectors.toList()));
            dto.setGlDetails(glDtlRepository.findByTransactionPoid(transactionPoid).stream()
                    .map(dtl -> mapGlToDto(dtl, dto)).collect(Collectors.toList()));
            
            log.info("Successfully fetched supplier credit note with {} items, {} charges, {} GL entries", 
                    dto.getItemDetails().size(), dto.getChargeDetails().size(), dto.getGlDetails().size());
            
            return dto;
        } catch (Exception e) {
            log.error("Error fetching supplier credit note with transactionPoid {}: {}", transactionPoid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch supplier credit note: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ApPurchaseCnHdrDto update(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        log.info("Updating supplier credit note with transactionPoid: {}", transactionPoid);
        
        try {
            ApPurchaseCnHdr existing = hdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier Credit Note", "transactionPoid", transactionPoid));

            ApPurchaseCnHdr oldEntity = new ApPurchaseCnHdr();
            BeanUtils.copyProperties(existing, oldEntity);
            updateEntityFromDto(existing, dto);
            existing.setLastModifiedBy(UserContext.getUserId());
            existing.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
            
            hdrRepository.save(existing);
            log.info("Supplier credit note header updated successfully");
            
            updateDetailsByActionType(transactionPoid, dto);
            log.info("Supplier credit note updated successfully with transactionPoid: {}", transactionPoid);
            loggingService.logChanges(oldEntity, existing, ApPurchaseCnHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
            return getById(transactionPoid);


        } catch (Exception e) {
            log.error("Error updating supplier credit note with transactionPoid {}: {}", transactionPoid, e.getMessage(), e);
            throw new RuntimeException("Failed to update supplier credit note: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting supplier credit note with transactionPoid: {}", transactionPoid);
        
        try {
            ApPurchaseCnHdr hdr = hdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier Credit Note", "transactionPoid", transactionPoid));

            documentDeleteService.deleteDocument(
                    transactionPoid,
                    "AP_PURCHASE_CN_HDR",
                    "TRANSACTION_POID",
                    deleteReasonDto,
                    hdr.getTransactionDate()
            );

            log.info("Supplier credit note deleted successfully with transactionPoid: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error deleting supplier credit note with transactionPoid {}: {}", transactionPoid, e.getMessage(), e);
            throw new RuntimeException("Failed to delete supplier credit note: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> list(String documentId, FilterRequestDto filters, 
                                     LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentSearchService.search(documentId, filterList, operator, pageable, isDeleted,
                "LONG_NARRATION",
                "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public Map<String, Object> getPjRefDetails(Long pjPoid) {
        log.info("Fetching PJ reference details for pjPoid: {}", pjPoid);
        
        try {
            Map<String, Object> result = procRepository.getPjRefDetails(pjPoid);
            GlVoucherLoadBillwiseBreakupResponseDto blResponse = billwiseBreakupService.loadBillwiseBreakup(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), "200-103", pjPoid);
            GlVoucherCostCenterBreakupResponseDto cCResponse = costCenterBreakupService.loadCostCenterData("200-103", pjPoid, UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid());
            mapBillwiseAndCostCenterBreakup(result, blResponse, cCResponse);
            log.info("Successfully fetched PJ reference details for pjPoid: {}", pjPoid);
            return result;
        } catch (Exception e) {
            log.error("Error fetching PJ reference details for pjPoid {}: {}", pjPoid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch PJ reference details: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getPartyDetails(String partyType, Long partyPoid) {
        log.info("Fetching party details for partyType: {}, partyPoid: {}", partyType, partyPoid);
        
        try {
            Map<String, Object> result = procRepository.getPartyDetails(partyType, partyPoid);
            log.info("Successfully fetched party details for partyType: {}, partyPoid: {}", partyType, partyPoid);
            return result;
        } catch (Exception e) {
            log.error("Error fetching party details for partyType {}, partyPoid {}: {}", partyType, partyPoid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch party details: " + e.getMessage(), e);
        }
    }

    private String normalizeActionType(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) {
            return "noChange";
        }
        return actionType.trim().toLowerCase();
    }

    private void updateDetailsByActionType(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        validateRefTypeRequirements(dto);
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<LogRequestDto<ApPurchaseCnItemDtl>> itemLogRequests = new ArrayList<>();
        List<LogRequestDto<ApPurchaseCnChargeDtl>> chargeLogRequests = new ArrayList<>();
        List<LogRequestDto<ApPurchaseCnGlDtl>> glLogRequests = new ArrayList<>();
        
        if (dto.getItemDetails() != null) {
            for (ApPurchaseCnItemDtlDto itemDto : dto.getItemDetails()) {
                String actionType = normalizeActionType(itemDto.getActionType());
                if ("isdeleted".equals(actionType) && itemDto.getDetRowId() != null) {
                    itemDtlRepository.deleteById(new com.asg.finance.entity.key.ApPurchaseCnItemDtlKey(transactionPoid, itemDto.getDetRowId()));
                    loggingService.logDelete(itemDto, docId, docKeyPoid);
                } else if ("isupdated".equals(actionType) && itemDto.getDetRowId() != null) {
                    ApPurchaseCnItemDtl oldItem = itemDtlRepository.findById(new com.asg.finance.entity.key.ApPurchaseCnItemDtlKey(transactionPoid, itemDto.getDetRowId())).orElse(null);
                    ApPurchaseCnItemDtl item = mapItemToEntity(itemDto);
                    item.setTransactionPoid(transactionPoid);
                    item.setDetRowId(itemDto.getDetRowId());
                    item.setLastModifiedBy(UserContext.getUserId());
                    item.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
                    itemDtlRepository.save(item);
                    if (oldItem != null) {
                        String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, itemDto.getDetRowId());
                        itemLogRequests.add(new LogRequestDto<>(oldItem, item, ApPurchaseCnItemDtl.class, docId, docKeyPoid, logDetail));
                    }
                } else if ("iscreated".equals(actionType)) {
                    ApPurchaseCnItemDtl item = mapItemToEntity(itemDto);
                    item.setTransactionPoid(transactionPoid);
                    item.setDetRowId(getNextItemRowId(transactionPoid));
                    item.setCreatedBy(UserContext.getUserId());
                    item.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
                    itemDtlRepository.save(item);
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, "Row Created on Supplier Credit Note Item Detail with detRowId: " + item.getDetRowId());
                }
            }
        }
        
        if (dto.getChargeDetails() != null) {
            for (ApPurchaseCnChargeDtlDto chargeDto : dto.getChargeDetails()) {
                String actionType = normalizeActionType(chargeDto.getActionType());
                if ("isdeleted".equals(actionType) && chargeDto.getDetRowId() != null) {
                    chargeDtlRepository.deleteById(new com.asg.finance.entity.key.ApPurchaseCnChargeDtlKey(transactionPoid, chargeDto.getDetRowId()));
                    loggingService.logDelete(chargeDto, docId, docKeyPoid);
                } else if ("isupdated".equals(actionType) && chargeDto.getDetRowId() != null) {
                    ApPurchaseCnChargeDtl oldCharge = chargeDtlRepository.findById(new com.asg.finance.entity.key.ApPurchaseCnChargeDtlKey(transactionPoid, chargeDto.getDetRowId())).orElse(null);
                    ApPurchaseCnChargeDtl charge = mapChargeToEntity(chargeDto);
                    charge.setTransactionPoid(transactionPoid);
                    charge.setDetRowId(chargeDto.getDetRowId());
                    charge.setLastModifiedBy(UserContext.getUserId());
                    charge.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
                    chargeDtlRepository.save(charge);
                    if (oldCharge != null) {
                        String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, chargeDto.getDetRowId());
                        chargeLogRequests.add(new LogRequestDto<>(oldCharge, charge, ApPurchaseCnChargeDtl.class, docId, docKeyPoid, logDetail));
                    }
                } else if ("iscreated".equals(actionType)) {
                    ApPurchaseCnChargeDtl charge = mapChargeToEntity(chargeDto);
                    charge.setTransactionPoid(transactionPoid);
                    charge.setDetRowId(chargeDto.getDetRowId() != null ? chargeDto.getDetRowId() : getNextChargeRowId(transactionPoid));
                    charge.setCreatedBy(UserContext.getUserId());
                    charge.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
                    chargeDtlRepository.save(charge);
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, "Row Created on Supplier Credit Note Charge Detail with detRowId: " + charge.getDetRowId());
                }
            }
        }
        
        if (dto.getGlDetails() != null) {
            for (ApPurchaseCnGlDtlDto glDto : dto.getGlDetails()) {
                String actionType = normalizeActionType(glDto.getActionType());
                if ("isdeleted".equals(actionType) && glDto.getDetRowId() != null) {
                    glDtlRepository.deleteById(new com.asg.finance.entity.key.ApPurchaseCnGlDtlKey(transactionPoid, glDto.getDetRowId()));
                    loggingService.logDelete(glDto, docId, docKeyPoid);
                } else if ("isupdated".equals(actionType) && glDto.getDetRowId() != null) {
                    ApPurchaseCnGlDtl oldGl = glDtlRepository.findById(new com.asg.finance.entity.key.ApPurchaseCnGlDtlKey(transactionPoid, glDto.getDetRowId())).orElse(null);
                    ApPurchaseCnGlDtl gl = mapGlToEntity(glDto);
                    gl.setTransactionPoid(transactionPoid);
                    gl.setDetRowId(glDto.getDetRowId());
                    gl.setLastModifiedBy(UserContext.getUserId());
                    gl.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
                    glDtlRepository.save(gl);
                    glDto.setDetRowId(gl.getDetRowId());
                    if (oldGl != null) {
                        String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, glDto.getDetRowId());
                        glLogRequests.add(new LogRequestDto<>(oldGl, gl, ApPurchaseCnGlDtl.class, docId, docKeyPoid, logDetail));
                    }
                } else if ("iscreated".equals(actionType)) {
                    ApPurchaseCnGlDtl gl = mapGlToEntity(glDto);
                    gl.setTransactionPoid(transactionPoid);
                    gl.setDetRowId(glDto.getDetRowId() != null ? glDto.getDetRowId() : getNextGlRowId(transactionPoid));
                    gl.setCreatedBy(UserContext.getUserId());
                    gl.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
                    glDtlRepository.save(gl);
                    glDto.setDetRowId(gl.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, "Row Created on Supplier Credit Note GL Detail with detRowId: " + gl.getDetRowId());
                }
            }
            saveBillwiseForGl(transactionPoid, dto.getGlDetails().stream()
                .filter(g -> !"isdeleted".equals(normalizeActionType(g.getActionType()))).collect(Collectors.toList()), UserContext.getDocumentId(), true);
            saveCostCenterForGl(transactionPoid, dto.getGlDetails().stream()
                .filter(g -> !"isdeleted".equals(normalizeActionType(g.getActionType()))).collect(Collectors.toList()), UserContext.getDocumentId(), true);
        }
        
        // Batch process all update logs
        if (!itemLogRequests.isEmpty()) {
            loggingService.createLogBatch(itemLogRequests);
        }
        if (!chargeLogRequests.isEmpty()) {
            loggingService.createLogBatch(chargeLogRequests);
        }
        if (!glLogRequests.isEmpty()) {
            loggingService.createLogBatch(glLogRequests);
        }
    }

    private Long getNextItemRowId(Long transactionPoid) {
        return itemDtlRepository.findByTransactionPoid(transactionPoid).stream()
            .map(ApPurchaseCnItemDtl::getDetRowId).max(Long::compareTo).orElse(0L) + 1;
    }

    private Long getNextChargeRowId(Long transactionPoid) {
        return chargeDtlRepository.findByTransactionPoid(transactionPoid).stream()
            .map(ApPurchaseCnChargeDtl::getDetRowId).max(Long::compareTo).orElse(0L) + 1;
    }

    private Long getNextGlRowId(Long transactionPoid) {
        return glDtlRepository.findByTransactionPoid(transactionPoid).stream()
            .map(ApPurchaseCnGlDtl::getDetRowId).max(Long::compareTo).orElse(0L) + 1;
    }

    private void saveDetails(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        log.info("Saving details for transactionPoid: {}", transactionPoid);
        
        validateRefTypeRequirements(dto);
        
        if (dto.getItemDetails() != null) {
            long rowId = 1;
            for (ApPurchaseCnItemDtlDto itemDto : dto.getItemDetails()) {
                ApPurchaseCnItemDtl item = mapItemToEntity(itemDto);
                item.setTransactionPoid(transactionPoid);
                item.setDetRowId(rowId++);
                item.setCreatedBy(UserContext.getUserId());
                item.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
                itemDtlRepository.save(item);
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), "Row Created on Supplier Credit Note Item Detail with detRowId: " + item.getDetRowId());
            }
            log.info("Saved {} item details for transactionPoid: {}", dto.getItemDetails().size(), transactionPoid);
        }
        
        if (dto.getChargeDetails() != null) {
            long rowId = 1;
            for (ApPurchaseCnChargeDtlDto chargeDto : dto.getChargeDetails()) {
                ApPurchaseCnChargeDtl charge = mapChargeToEntity(chargeDto);
                charge.setTransactionPoid(transactionPoid);
                charge.setDetRowId(rowId++);
                charge.setCreatedBy(UserContext.getUserId());
                charge.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
                chargeDtlRepository.save(charge);
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), "Row Created on Supplier Credit Note Charge Detail with detRowId: " + charge.getDetRowId());
            }
            log.info("Saved {} charge details for transactionPoid: {}", dto.getChargeDetails().size(), transactionPoid);
        }
        
        if (dto.getGlDetails() != null) {
            long rowId = 1;
            for (ApPurchaseCnGlDtlDto glDto : dto.getGlDetails()) {
                ApPurchaseCnGlDtl gl = mapGlToEntity(glDto);
                gl.setTransactionPoid(transactionPoid);
                gl.setDetRowId(rowId++);
                gl.setCreatedBy(UserContext.getUserId());
                gl.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
                glDtlRepository.save(gl);
                glDto.setDetRowId(gl.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), "Row Created on Supplier Credit Note GL Detail with detRowId: " + gl.getDetRowId());
            }
            log.info("Saved {} GL details for transactionPoid: {}", dto.getGlDetails().size(), transactionPoid);
            
            saveBillwiseForGl(transactionPoid, dto.getGlDetails(), UserContext.getDocumentId(), false);
            saveCostCenterForGl(transactionPoid, dto.getGlDetails(), UserContext.getDocumentId(), false);
        }
    }
    
    private void validateRefTypeRequirements(ApPurchaseCnHdrDto dto) {
        String refType = dto.getRefType();
        if (refType == null) {
            throw new ValidationException("Reference type is required");
        }
        
        switch (refType.toUpperCase()) {
            case "GENERAL":
                if (dto.getGlDetails() == null || dto.getGlDetails().isEmpty()) {
                    throw new ValidationException("At least one GL detail is required for reference type: " + refType);
                }
                break;
                
            case "FF":
            case "FF_JOB":
                // FF reference type requires charge details
                if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
                    throw new ValidationException("At least one charge detail is required for reference type: FF");
                }
                break;
                
            case "FDA":
                if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
                    throw new ValidationException("At least one charge detail is required for reference type: " + refType);
                }
                break;
                
            case "PJ_REVERSAL":
                // Check PJ Reversal Ref Type for FF Jobs
                if ("FF".equalsIgnoreCase(dto.getPjReversalRefType()) || "FDA".equalsIgnoreCase(dto.getPjReversalRefType()) || "FF_JOB".equalsIgnoreCase(dto.getPjReversalRefType())) {
                    if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
                        throw new ValidationException("At least one charge detail is required for PJ Type 'FF Jobs or FDA jobs'");
                    }
                } else if ("GENERAL_PO".equalsIgnoreCase(dto.getPjReversalRefType()) || "GENERAL".equalsIgnoreCase(dto.getPjReversalRefType())) {
                    if (dto.getGlDetails() == null || dto.getGlDetails().isEmpty()) {
                        throw new ValidationException("At least one charge detail is required for PJ Type 'GENERAL PO Jobs'");
                    }
                } else {
                    // For other PJ types, require item details
                    if (dto.getItemDetails() == null || dto.getItemDetails().isEmpty()) {
                        throw new ValidationException("At least one item detail is required for reference type: " + refType);
                    }
                }
                break;
                
            default:
                log.warn("Unknown reference type: {}, skipping validation", refType);
        }
    }

    public void saveBillwiseForGl(Long transactionPoid, List<ApPurchaseCnGlDtlDto> glDetails, String docId, boolean isUpdate) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }
        
        log.info("Saving billwise breakup for {} GL details, transactionPoid: {}", glDetails.size(), transactionPoid);
        
        List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
        Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
        Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;

        for (ApPurchaseCnGlDtlDto glDto : glDetails) {
            if (glDto == null || glDto.getDetRowId() == null) {
                continue;
            }
            if (!glMasterRepository.existsByGlPoid(glDto.getGlPoid())) {
                throw new ResourceNotFoundException("Gl Master", "glPoid", glDto.getGlPoid());
            }
            if (glDto.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(glDto.getTaxPoid())) {
                throw new ResourceNotFoundException("Tax", "taxPoid", glDto.getTaxPoid());
            }
            long inital = 1L;
            if (glDto.getBreakupList() != null && !glDto.getBreakupList().isEmpty()) {
                for (BillwiseBreakupPopupRequestDto popup : glDto.getBreakupList()) {
                    BillwiseBreakupRequestDto req = new BillwiseBreakupRequestDto();
                    req.setGroupPoid(groupPoid);
                    req.setCompanyPoid(companyPoid);
                    req.setDocId(docId);
                    req.setTransactionPoid(transactionPoid);
                    req.setBillDetRowId(isUpdate ? popup.getBillDetRowId() : inital);
                    req.setBillRefType(popup.getBillRefType());
                    req.setBillRef(popup.getBillRef());
                    req.setBillDueDate(popup.getBillDueDate());
                    if ("DR".equalsIgnoreCase(popup.getType())) {
                        req.setDrAmt(popup.getAmount());
                        req.setCrAmt(BigDecimal.ZERO);
                    } else {
                        req.setDrAmt(BigDecimal.ZERO);
                        req.setCrAmt(popup.getAmount());
                    }
                    req.setBillRemarks(popup.getBillRemarks());
                    req.setLoginUserPoid(userPoid);
                    req.setMainDetRowId(glDto.getDetRowId());
                    req.setGlCompanyPoid(companyPoid);
                    req.setGlPoid(glDto.getGlPoid());
                    billwiseList.add(req);
                    inital++;
                }
            }
        }

        if (!billwiseList.isEmpty()) {
            if (isUpdate) {
                billwiseBreakupService.updateBillwiseBreakups(billwiseList, userPoid);
            } else {
                billwiseBreakupService.insertBillwiseBreakup(billwiseList);
            }
            log.info("Saved {} billwise breakup entries for transactionPoid: {}", billwiseList.size(), transactionPoid);
        }
    }

    public void saveCostCenterForGl(Long transactionPoid,
                                    List<ApPurchaseCnGlDtlDto> glDetails,
                                    String docId,
                                    boolean isUpdate) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();
        Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
        Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;

        long inital = 1L;
        for (ApPurchaseCnGlDtlDto glDto : glDetails) {
            if (glDto == null || glDto.getDetRowId() == null) {
                continue;
            }

            if (glDto.getCostCenterList() != null && !glDto.getCostCenterList().isEmpty()) {
                for (CostCenterBreakupPopupRequestDto popup : glDto.getCostCenterList()) {
                    CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
                    dto.setGroupPoid(groupPoid);
                    dto.setCompanyPoid(companyPoid);
                    dto.setDocId(docId);
                    dto.setTransactionPoid(transactionPoid);
                    dto.setMainDetRowId(glDto.getDetRowId());
                    dto.setGlPoid(glDto.getGlPoid());
                    dto.setCostDetRowId(isUpdate ? popup.getCostDetRowId() : inital);
                    dto.setCostGroup(popup.getCostGroup());
                    dto.setCostPoid(popup.getCostPoid());
                    dto.setAmount(popup.getAmount());
                    dto.setLoginUserPoid(userPoid);
                    costCenterList.add(dto);
                    inital++;
                }
            }
        }

        if (!costCenterList.isEmpty()) {
            if (isUpdate) {
                costCenterBreakupService.updateCostCenterBreakups(costCenterList, userPoid);
            } else  {
                costCenterBreakupService.saveCostCenterBreakups(costCenterList);
            }
            log.info("Saved {} cost center breakup entries for transactionPoid: {}", costCenterList.size(), transactionPoid);
        }
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        // To be updated
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "200-103");
        params.put("SUBREPORT_GL", printService.load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml"));
        params.put("SUBREPORT_CHARGE", printService.load("Finance/AP/PurchaseInvoiceChargeSubReport.jrxml"));
        JasperReport mainReport = printService.load("Finance/AP/PurchaseInvoiceReport_2.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private ApPurchaseCnHdr mapToEntity(ApPurchaseCnHdrDto dto) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        // Fallback to default values if UserContext returns null
        if (groupPoid == null) {
            groupPoid = 1L;
            log.warn("UserContext.getGroupPoid() returned null, using default value: {}", groupPoid);
        }
        if (companyPoid == null) {
            companyPoid = 1L;
            log.warn("UserContext.getCompanyPoid() returned null, using default value: {}", companyPoid);
        }
        
        log.info("Creating entity with groupPoid: {}, companyPoid: {}", groupPoid, companyPoid);
        
        return ApPurchaseCnHdr.builder()
                .transactionDate(dto.getTransactionDate())
                .docRef(dto.getDocRef())
                .poRef(dto.getPoRef())
                .groupPoid(groupPoid)
                .companyPoid(companyPoid)
                .currencyCode(dto.getCurrencyCode())
                .currencyRate(dto.getCurrencyRate())
                .supplierPoid(dto.getSupplierPoid())
                .subTotal(dto.getSubTotal())
                .discount(dto.getDiscount())
                .grandTotal(dto.getGrandTotal())
                .remarks(dto.getRemarks())
                .itemTotal(dto.getItemTotal())
                .chargeTotal(dto.getChargeTotal())
                .glTotal(dto.getGlTotal())
                .type(dto.getType())
                .description(dto.getDescription())
                .creditPeriod(dto.getCreditPeriod())
                .dueDate(dto.getDueDate())
                .refType(dto.getRefType())
                .narration(dto.getNarration())
                .supplierCnDate(dto.getSupplierCnDate())
                .supplierCnNo(dto.getSupplierCnNo())
                .supplierCnRemark(dto.getSupplierCnRemark())
                .multiCompany(Boolean.TRUE.equals(dto.getMultiCompany()) ? "Y" : "N")
                .bhdAmount(dto.getCurrencyRate().multiply(dto.getSupplierCnAmount()))
                .supplierCnAmount(dto.getSupplierCnAmount())
                .roundingAmount(dto.getRoundingAmount())
                .partyType(dto.getPartyType())
                .partyTinNumber(dto.getPartyTinNumber())
                .fdaCoveringRef(dto.getFdaCoveringRef())
                .pjReversalRef(dto.getPjReversalRef())
                .pjReversalRefType(dto.getPjReversalRefType())
                .pjReversalRefDetails(dto.getPjReversalRefDetails())
                .ffRef(dto.getFfRef())
                .build();
    }

    private void updateEntityFromDto(ApPurchaseCnHdr entity, ApPurchaseCnHdrDto dto) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        // Fallback to default values if UserContext returns null
        if (groupPoid == null) {
            groupPoid = 1L;
            log.warn("UserContext.getGroupPoid() returned null during update, using default value: {}", groupPoid);
        }
        if (companyPoid == null) {
            companyPoid = 1L;
            log.warn("UserContext.getCompanyPoid() returned null during update, using default value: {}", companyPoid);
        }
        
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setPoRef(dto.getPoRef());
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setCurrencyRate(dto.getCurrencyRate());
        entity.setSupplierPoid(dto.getSupplierPoid());
        entity.setSubTotal(dto.getSubTotal());
        entity.setDiscount(dto.getDiscount());
        entity.setGrandTotal(dto.getGrandTotal());
        entity.setRemarks(dto.getRemarks());
        entity.setItemTotal(dto.getItemTotal());
        entity.setChargeTotal(dto.getChargeTotal());
        entity.setGlTotal(dto.getGlTotal());
        entity.setType(dto.getType());
        entity.setDescription(dto.getDescription());
        entity.setCreditPeriod(dto.getCreditPeriod());
        entity.setDueDate(dto.getDueDate());
        entity.setRefType(dto.getRefType());
        entity.setNarration(dto.getNarration());
        entity.setSupplierCnDate(dto.getSupplierCnDate());
        entity.setSupplierCnNo(dto.getSupplierCnNo());
        entity.setSupplierCnRemark(dto.getSupplierCnRemark());
        entity.setMultiCompany(Boolean.TRUE.equals(dto.getMultiCompany()) ? "Y" : "N");
        entity.setBhdAmount(dto.getCurrencyRate().multiply(dto.getSupplierCnAmount()));
        entity.setSupplierCnAmount(dto.getSupplierCnAmount());
        entity.setRoundingAmount(dto.getRoundingAmount());
        entity.setPartyType(dto.getPartyType());
        entity.setPartyTinNumber(dto.getPartyTinNumber());
        entity.setFdaCoveringRef(dto.getFdaCoveringRef());
        entity.setPjReversalRef(dto.getPjReversalRef());
        entity.setPjReversalRefType(dto.getPjReversalRefType());
        entity.setPjReversalRefDetails(dto.getPjReversalRefDetails());
        entity.setFfRef(dto.getFfRef());
    }

    private ApPurchaseCnHdrDto mapToDto(ApPurchaseCnHdr entity) {
        ApPurchaseCnHdrDto dto = new ApPurchaseCnHdrDto();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setDocRef(entity.getDocRef());
        dto.setPoRef(entity.getPoRef());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencyRate(entity.getCurrencyRate());
        dto.setSupplierPoid(entity.getSupplierPoid());
        dto.setSubTotal(entity.getSubTotal());
        dto.setDiscount(entity.getDiscount());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setRemarks(entity.getRemarks());
        dto.setItemTotal(entity.getItemTotal());
        dto.setChargeTotal(entity.getChargeTotal());
        dto.setGlTotal(entity.getGlTotal());
        dto.setType(entity.getType());
        dto.setDescription(entity.getDescription());
        dto.setCreditPeriod(entity.getCreditPeriod());
        dto.setDueDate(entity.getDueDate());
        dto.setRefType(entity.getRefType());
        dto.setNarration(entity.getNarration());
        dto.setSupplierCnDate(entity.getSupplierCnDate());
        dto.setSupplierCnNo(entity.getSupplierCnNo());
        dto.setSupplierCnRemark(entity.getSupplierCnRemark());
        dto.setMultiCompany("Y".equals(entity.getMultiCompany()));
        dto.setBhdAmount(entity.getBhdAmount());
        dto.setSupplierCnAmount(entity.getSupplierCnAmount());
        dto.setRoundingAmount(entity.getRoundingAmount());
        dto.setPartyType(entity.getPartyType());
        dto.setPartyTinNumber(entity.getPartyTinNumber());
        dto.setFdaCoveringRef(entity.getFdaCoveringRef());
        dto.setPjReversalRef(entity.getPjReversalRef());
        dto.setPjReversalRefType(entity.getPjReversalRefType());
        dto.setPjReversalRefDetails(entity.getPjReversalRefDetails());
        dto.setFfRef(entity.getFfRef());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    private ApPurchaseCnItemDtl mapItemToEntity(ApPurchaseCnItemDtlDto dto) {
        return ApPurchaseCnItemDtl.builder()
                .stockPoid(dto.getStockPoid())
                .stockUnitPoid(dto.getStockUnitPoid())
                .quantity(dto.getQuantity())
                .price(dto.getPrice())
                .discount(dto.getDiscount())
                .total(dto.getTotal())
                .remarks(dto.getRemarks())
                .refDocId(dto.getRefDocId())
                .refDocPoid(dto.getRefDocPoid())
                .checkAll(dto.getCheckAll())
                .refDetRowId(dto.getRefDetRowId())
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .amount(dto.getAmount())
                .baseAmount(dto.getBaseAmount())
                .build();
    }

    private ApPurchaseCnItemDtlDto mapItemToDto(ApPurchaseCnItemDtl entity) {
        ApPurchaseCnItemDtlDto dto = new ApPurchaseCnItemDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setStockPoid(entity.getStockPoid());
        dto.setStockUnitPoid(entity.getStockUnitPoid());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setDiscount(entity.getDiscount());
        dto.setTotal(entity.getTotal());
        dto.setRemarks(entity.getRemarks());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setCheckAll(entity.getCheckAll());
        dto.setRefDetRowId(entity.getRefDetRowId());
        dto.setTaxPoid(entity.getTaxPoid());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setAmount(entity.getAmount());
        dto.setBaseAmount(entity.getBaseAmount());
        
        if (entity.getStockPoid() != null) {
            dto.setStockDet(lovService.getDetailsByPoidAndLovName(entity.getStockPoid(), "STOCK_MASTER"));
        }
        if (entity.getStockUnitPoid() != null) {
            dto.setStockUnitDet(lovService.getDetailsByPoidAndLovName(entity.getStockUnitPoid(), "STOCK_UNIT6"));
        }
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "INPUT_TAX_MASTER"));
        }
        
        return dto;
    }

    private ApPurchaseCnChargeDtl mapChargeToEntity(ApPurchaseCnChargeDtlDto dto) {
        return ApPurchaseCnChargeDtl.builder()
                .chargePoid(dto.getChargePoid())
                .chargeAmount(dto.getChargeAmount())
                .description(dto.getDescription())
                .remarks(dto.getRemarks())
                .refDocId(dto.getRefDocId())
                .refDocPoid(dto.getRefDocPoid())
                .refDetRowId(dto.getRefDetRowId())
                .checkAll(dto.getCheckAll())
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .chargeBaseAmount(dto.getChargeBaseAmount())
                .chargeFrom(dto.getChargeFrom())
                .supplierPoidFf(dto.getSupplierPoidFf())
                .build();
    }

    private ApPurchaseCnChargeDtlDto mapChargeToDto(ApPurchaseCnChargeDtl entity, String refType) {
        ApPurchaseCnChargeDtlDto dto = new ApPurchaseCnChargeDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setChargePoid(entity.getChargePoid());
        dto.setChargeAmount(entity.getChargeAmount());
        dto.setDescription(entity.getDescription());
        dto.setRemarks(entity.getRemarks());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setRefDetRowId(entity.getRefDetRowId());
        dto.setCheckAll(entity.getCheckAll());
        dto.setTaxPoid(entity.getTaxPoid());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setChargeBaseAmount(entity.getChargeBaseAmount());
        dto.setChargeFrom(entity.getChargeFrom());
        dto.setSupplierPoidFf(entity.getSupplierPoidFf());
        
        if (entity.getChargePoid() != null && refType != null) {
            if ("FDA".equals(refType)) {
                dto.setChargeDet(lovService.getDetailsByPoidAndLovName(entity.getChargePoid(), "FDA_CHARGE_MASTER_PJ"));
            } else if ("FF".equals(refType) || "FF_JOB".equals(refType)) {
                dto.setChargeDet(lovService.getDetailsByPoidAndLovName(entity.getChargePoid(), "FF_CHARGE_MASTER_PJ"));
            }
        }
        
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "INPUT_TAX_MASTER"));
        }
        
        if (entity.getRefDocPoid() != null && ("FF".equals(refType) || "FF_JOB".equals(refType))) {
            dto.setRefDocDet(lovService.getDetailsByPoidAndLovName(entity.getRefDocPoid(), "FF_JOBNO"));
        }
        
        return dto;
    }

    private ApPurchaseCnGlDtl mapGlToEntity(ApPurchaseCnGlDtlDto dto) {
        return ApPurchaseCnGlDtl.builder()
                .type(dto.getType())
                .companyPoid(dto.getCompanyPoid())
                .glPoid(dto.getGlPoid())
                .drAmount(dto.getDrAmount())
                .crAmount(dto.getCrAmount())
                .refDocId(dto.getRefDocId())
                .refDocPoid(dto.getRefDocPoid())
                .description(dto.getDescription())
                .remarks(dto.getRemarks())
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .totalAmount(dto.getTotalAmount())
                .build();
    }

    private ApPurchaseCnGlDtlDto mapGlToDto(ApPurchaseCnGlDtl entity, ApPurchaseCnHdrDto headerDto) {
        ApPurchaseCnGlDtlDto dto = new ApPurchaseCnGlDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setType(entity.getType());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setGlPoid(entity.getGlPoid());
        dto.setDrAmount(entity.getDrAmount());
        dto.setCrAmount(entity.getCrAmount());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setDescription(entity.getDescription());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxPoid(entity.getTaxPoid());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        
        if (entity.getCompanyPoid() != null) {
            dto.setCompanyDet(lovService.getDetailsByPoidAndLovName(entity.getCompanyPoid(), "COMPANY"));
        }
        if (entity.getGlPoid() != null) {
            dto.setGlDet(lovService.getDetailsByPoidAndLovName(entity.getGlPoid(), "GL_MASTER_LEDGERS_PJ"));
        }
        if (entity.getType() != null) {
            dto.setTypeDet(lovService.getDetailsByCodeAndLovName(entity.getType(), "ACC_TYPE_SHORT"));
        }
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "PJ_GL_INPUT_TAX"));
        }
        
        // Load breakup lists
        loadBreakupLists(entity.getTransactionPoid(), entity.getDetRowId(), dto, headerDto);
        
        return dto;
    }
    
    private void loadBreakupLists(Long transactionPoid, Long detRowId, ApPurchaseCnGlDtlDto dto, ApPurchaseCnHdrDto hdrDto) {
        try {
            // Load billwise breakup
            GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = billwiseBreakupService.loadBillwiseBreakup(
                    UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L,
                    UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L,
                    "200-107",
                    hdrDto.getTransactionPoid()
            );
            
            if (billwiseResponse != null && billwiseResponse.getLoadBillwiseBreakupResponseDtoList() != null) {
                List<BillwiseBreakupPopupRequestDto> breakupList = billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                    .filter(b -> detRowId.equals(b.getMainDetRowId()))
                    .map(b -> {
                        BillwiseBreakupPopupRequestDto popup = new BillwiseBreakupPopupRequestDto();
                        popup.setBillDetRowId(b.getBillDetRowId());
                        popup.setBillRefType(b.getBillRefType());
                        popup.setBillRef(b.getBillRef());
                        popup.setBillDueDate(b.getBillDueDate());
                        popup.setType(b.getDrAmt().compareTo(BigDecimal.ZERO) > 0 ? "DR" : "CR");
                        popup.setAmount(b.getDrAmt().compareTo(BigDecimal.ZERO) > 0 ? b.getDrAmt() : b.getCrAmt());
                        popup.setBillRemarks(b.getBillRemarks());
                        return popup;
                    })
                    .collect(Collectors.toList());
                dto.setBreakupList(breakupList);
            } else {
                dto.setBreakupList(new ArrayList<>());
            }
            
            // Load cost center breakup
            GlVoucherCostCenterBreakupResponseDto costCenterResponse = costCenterBreakupService.loadCostCenterData(
                    "200-107",
                    hdrDto.getTransactionPoid(),
                    UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L,
                    UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L,
                    UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L
            );
            
            if (costCenterResponse != null && costCenterResponse.getCostBreakupList() != null) {
                List<CostCenterBreakupPopupRequestDto> costCenterList = costCenterResponse.getCostBreakupList().stream()
                    .filter(c -> detRowId.equals(c.getMainDetRowId()))
                    .map(c -> {
                        CostCenterBreakupPopupRequestDto popup = new CostCenterBreakupPopupRequestDto();
                        popup.setCostDetRowId(c.getCostDetRowId());
                        popup.setCostGroup(c.getCostGroup());
                        popup.setCostPoid(c.getCostPoid());
                        popup.setAmount(BigDecimal.valueOf(c.getAmount()));
                        return popup;
                    })
                    .collect(Collectors.toList());
                dto.setCostCenterList(costCenterList);
            } else {
                dto.setCostCenterList(new ArrayList<>());
            }
            
        } catch (Exception e) {
            log.warn("Failed to load breakup lists for transactionPoid: {}, detRowId: {}: {}", 
                transactionPoid, detRowId, e.getMessage());
            // Set empty lists instead of null
            dto.setBreakupList(new ArrayList<>());
            dto.setCostCenterList(new ArrayList<>());
        }
    }

    private void mapBillwiseAndCostCenterBreakup(
            Map<String, Object> params,
            GlVoucherLoadBillwiseBreakupResponseDto billResponse,
            GlVoucherCostCenterBreakupResponseDto costResponse
    ) {
        String refTye = (String) params.getOrDefault("pjRefType", null);

        if ("GENERAL".equals(refTye)) {
            List<Map<String, Object>> lineItems = (List<Map<String, Object>>) params.getOrDefault("lineItems", null);
            if (CollectionUtils.isNotEmpty(lineItems)) {
                for (Map<String, Object> lineItem : lineItems) {
                    Long companyPoid = ((Number) lineItem.getOrDefault("COMPANY_POID", null)).longValue();
                    lineItem.put("companyDet", lovService.getDetailsByPoidAndLovName(companyPoid, "COMPANY"));
                    
                    Long glPoid = ((Number) lineItem.getOrDefault("GL_POID", null)).longValue();
                    lineItem.put("glDet", lovService.getDetailsByPoidAndLovName(glPoid, "GL_MASTER_LEDGERS_PJ"));

                    String type = (String) lineItem.get("TYPE");
                    if (type != null) {
                        lineItem.put("typeDet", lovService.getDetailsByCodeAndLovName(type, "ACC_TYPE_SHORT"));
                    }
                    
                    Object taxPoidObj = lineItem.get("TAX_POID");
                    if (taxPoidObj != null) {
                        Long taxPoid = ((Number) taxPoidObj).longValue();
                        lineItem.put("taxDet", lovService.getDetailsByPoidAndLovName(taxPoid, "PJ_GL_INPUT_TAX"));
                    }
                    
                    List<BillwiseBreakupPopupRequestDto> billwiseList = filterBillwiseBreakup(billResponse, glPoid);
                    List<CostCenterBreakupPopupRequestDto> costCenterList = filterCostCenterBreakup(costResponse, glPoid);
                    lineItem.put("BILL_WISE_BREAK_UP_LIST", billwiseList);
                    lineItem.put("COST_CENTER_BREAK_UP_LIST", costCenterList);
                }
            }
        } else if ("FDA".equals(refTye)) {
            List<Map<String, Object>> lineItems = (List<Map<String, Object>>) params.getOrDefault("lineItems", null);
            if (CollectionUtils.isNotEmpty(lineItems)) {
                for (Map<String, Object> lineItem : lineItems) {
                    Object chargePoidObj = lineItem.get("CHARGE_POID");
                    if (chargePoidObj != null) {
                        Long chargePoid = ((Number) chargePoidObj).longValue();
                        lineItem.put("chargeDet", lovService.getDetailsByPoidAndLovName(chargePoid, "FDA_CHARGE_MASTER_PJ"));
                    }
                    
                    Object taxPoidObj = lineItem.get("TAX_POID");
                    if (taxPoidObj != null) {
                        Long taxPoid = ((Number) taxPoidObj).longValue();
                        lineItem.put("taxDet", lovService.getDetailsByPoidAndLovName(taxPoid, "INPUT_TAX_MASTER"));
                    }
                }
            }
        } else if ("FF".equals(refTye) || "FF_JOB".equals(refTye)) {
            List<Map<String, Object>> lineItems = (List<Map<String, Object>>) params.getOrDefault("lineItems", null);
            if (CollectionUtils.isNotEmpty(lineItems)) {
                for (Map<String, Object> lineItem : lineItems) {
                    Object chargePoidObj = lineItem.get("CHARGE_POID");
                    if (chargePoidObj != null) {
                        Long chargePoid = ((Number) chargePoidObj).longValue();
                        lineItem.put("chargeDet", lovService.getDetailsByPoidAndLovName(chargePoid, "FF_CHARGE_MASTER_PJ"));
                    }
                    
                    Object taxPoidObj = lineItem.get("TAX_POID");
                    if (taxPoidObj != null) {
                        Long taxPoid = ((Number) taxPoidObj).longValue();
                        lineItem.put("taxDet", lovService.getDetailsByPoidAndLovName(taxPoid, "INPUT_TAX_MASTER"));
                    }
                    
                    Object refDocPoidObj = lineItem.get("REF_DOC_POID");
                    if (refDocPoidObj != null) {
                        Long refDocPoid = ((Number) refDocPoidObj).longValue();
                        lineItem.put("refDocDet", lovService.getDetailsByPoidAndLovName(refDocPoid, "FF_JOBNO"));
                    }
                }
            }
        } else if ("MTA_PO".equals(refTye)) {
            List<Map<String, Object>> lineItems = (List<Map<String, Object>>) params.getOrDefault("lineItems", null);
            if (CollectionUtils.isNotEmpty(lineItems)) {
                for (Map<String, Object> lineItem : lineItems) {
                    Object stockPoidObj = lineItem.get("STOCK_POID");
                    if (stockPoidObj != null) {
                        Long stockPoid = ((Number) stockPoidObj).longValue();
                        lineItem.put("stockDet", lovService.getDetailsByPoidAndLovName(stockPoid, "STOCK_MASTER"));
                    }
                    
                    Object stockUnitPoidObj = lineItem.get("STOCK_UNIT_POID");
                    if (stockUnitPoidObj != null) {
                        Long stockUnitPoid = ((Number) stockUnitPoidObj).longValue();
                        lineItem.put("stockUnitDet", lovService.getDetailsByPoidAndLovName(stockUnitPoid, "STOCK_UNIT6"));
                    }
                    
                    Object taxPoidObj = lineItem.get("TAX_POID");
                    if (taxPoidObj != null) {
                        Long taxPoid = ((Number) taxPoidObj).longValue();
                        lineItem.put("taxDet", lovService.getDetailsByPoidAndLovName(taxPoid, "INPUT_TAX_MASTER"));
                    }
                }
            }
        }
    }

    private List<CostCenterBreakupPopupRequestDto> filterCostCenterBreakup(
            GlVoucherCostCenterBreakupResponseDto response, Long detRowId) {

        if (response == null || response.getCostBreakupList() == null) {
            return Collections.emptyList();
        }

        return response.getCostBreakupList().stream()
                .filter(cc -> cc.getGlPoid().equals(detRowId))
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

    private List<BillwiseBreakupPopupRequestDto> filterBillwiseBreakup(
            GlVoucherLoadBillwiseBreakupResponseDto response, Long detRowId) {

        if (response == null || response.getLoadBillwiseBreakupResponseDtoList() == null) {
            return Collections.emptyList();
        }

        return response.getLoadBillwiseBreakupResponseDtoList().stream()
                .filter(bw -> bw.getGlPoid().equals(detRowId))
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
}
