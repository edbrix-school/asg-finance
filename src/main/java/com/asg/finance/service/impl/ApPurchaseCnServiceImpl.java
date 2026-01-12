package com.asg.finance.service.impl;

import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.repository.*;
import com.asg.finance.service.ApPurchaseCnService;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    .map(this::mapChargeToDto).collect(Collectors.toList()));
            dto.setGlDetails(glDtlRepository.findByTransactionPoid(transactionPoid).stream()
                    .map(this::mapGlToDto).collect(Collectors.toList()));
            
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
            
            updateEntityFromDto(existing, dto);
            existing.setLastModifiedBy(UserContext.getUserId());
            existing.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
            
            hdrRepository.save(existing);
            log.info("Supplier credit note header updated successfully");
            
            // Delete existing details and save new ones
            itemDtlRepository.deleteByTransactionPoid(transactionPoid);
            chargeDtlRepository.deleteByTransactionPoid(transactionPoid);
            glDtlRepository.deleteByTransactionPoid(transactionPoid);
            log.info("Existing detail records deleted for transactionPoid: {}", transactionPoid);
            
            saveDetails(transactionPoid, dto);
            log.info("Supplier credit note updated successfully with transactionPoid: {}", transactionPoid);
            
            return getById(transactionPoid);
        } catch (Exception e) {
            log.error("Error updating supplier credit note with transactionPoid {}: {}", transactionPoid, e.getMessage(), e);
            throw new RuntimeException("Failed to update supplier credit note: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid) {
        log.info("Deleting supplier credit note with transactionPoid: {}", transactionPoid);
        
        try {
            ApPurchaseCnHdr hdr = hdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier Credit Note", "transactionPoid", transactionPoid));
            
            hdr.setDeleted("Y");
            hdr.setLastModifiedBy(UserContext.getUserId());
            hdr.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
            hdrRepository.save(hdr);
            
            log.info("Supplier credit note deleted successfully with transactionPoid: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error deleting supplier credit note with transactionPoid {}: {}", transactionPoid, e.getMessage(), e);
            throw new RuntimeException("Failed to delete supplier credit note: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> list(String documentId, FilterRequestDto filters, 
                                     LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getPjRefDetails(Long pjPoid) {
        log.info("Fetching PJ reference details for pjPoid: {}", pjPoid);
        
        try {
            Map<String, Object> result = procRepository.getPjRefDetails(pjPoid);
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

    private void saveDetails(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        log.info("Saving details for transactionPoid: {}", transactionPoid);
        
        // Validate reference type requirements
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
            }
            log.info("Saved {} GL details for transactionPoid: {}", dto.getGlDetails().size(), transactionPoid);
            
            saveBillwiseForGl(transactionPoid, dto.getGlDetails(), UserContext.getDocumentId());
            saveCostCenterForGl(transactionPoid, dto.getGlDetails(), UserContext.getDocumentId());
        }
    }
    
    private void validateRefTypeRequirements(ApPurchaseCnHdrDto dto) {
        String refType = dto.getRefType();
        if (refType == null) {
            throw new RuntimeException("Reference type is required");
        }
        
        switch (refType.toUpperCase()) {
            case "GENERAL":
            case "GENERAL_PO":
                if (dto.getGlDetails() == null || dto.getGlDetails().isEmpty()) {
                    throw new RuntimeException("At least one GL detail is required for reference type: " + refType);
                }
                break;
                
            case "FF":
                // FF reference type requires charge details
                if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
                    throw new RuntimeException("At least one charge detail is required for reference type: FF");
                }
                break;
                
            case "FDA":
                if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
                    throw new RuntimeException("At least one charge detail is required for reference type: " + refType);
                }
                break;
                
            case "PJ_REVERSAL":
                // Check PJ Reversal Ref Type for FF Jobs
                if ("FF".equalsIgnoreCase(dto.getPjReversalRefType())) {
                    if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
                        throw new RuntimeException("At least one charge detail is required for PJ Type 'FF Jobs'");
                    }
                } else {
                    // For other PJ types, require item details
                    if (dto.getItemDetails() == null || dto.getItemDetails().isEmpty()) {
                        throw new RuntimeException("At least one item detail is required for reference type: " + refType);
                    }
                }
                break;
                
            default:
                log.warn("Unknown reference type: {}, skipping validation", refType);
        }
    }

    public void saveBillwiseForGl(Long transactionPoid, List<ApPurchaseCnGlDtlDto> glDetails, String docId) {
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
            if (!taxMasterRepository.existsByTaxPoid(glDto.getTaxPoid())) {
                throw new ResourceNotFoundException("Tax", "taxPoid", glDto.getTaxPoid());
            }

            if (glDto.getBreakupList() != null && !glDto.getBreakupList().isEmpty()) {
                for (BillwiseBreakupPopupRequestDto popup : glDto.getBreakupList()) {
                    BillwiseBreakupRequestDto req = new BillwiseBreakupRequestDto();
                    req.setGroupPoid(groupPoid);
                    req.setCompanyPoid(companyPoid);
                    req.setDocId(docId);
                    req.setTransactionPoid(transactionPoid);
                    req.setBillDetRowId(popup.getBillDetRowId());
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
                }
            }
        }

        if (!billwiseList.isEmpty()) {
            billwiseBreakupService.insertBillwiseBreakup(billwiseList);
            log.info("Saved {} billwise breakup entries for transactionPoid: {}", billwiseList.size(), transactionPoid);
        }
    }

    public void saveCostCenterForGl(Long transactionPoid, List<ApPurchaseCnGlDtlDto> glDetails, String docId) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();
        Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
        Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;

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
                    dto.setCostDetRowId(glDto.getDetRowId());
                    dto.setCostGroup(popup.getCostGroup());
                    dto.setCostPoid(popup.getCostPoid());
                    dto.setAmount(popup.getAmount());
                    dto.setLoginUserPoid(userPoid);
                    costCenterList.add(dto);
                }
            }
        }

        if (!costCenterList.isEmpty()) {
            costCenterBreakupService.saveCostCenterBreakups(costCenterList);
            log.info("Saved {} cost center breakup entries for transactionPoid: {}", costCenterList.size(), transactionPoid);
        }
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
                .billType(dto.getBillType())
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
        entity.setBillType(dto.getBillType());
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
        dto.setBillType(entity.getBillType());
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
        dto.setActionType("isCreated"); // Set default action type for response
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
        dto.setActionType("isCreated"); // Set default action type for response
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

    private ApPurchaseCnChargeDtlDto mapChargeToDto(ApPurchaseCnChargeDtl entity) {
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
        dto.setActionType("isCreated"); // Set default action type for response
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

    private ApPurchaseCnGlDtlDto mapGlToDto(ApPurchaseCnGlDtl entity) {
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
        dto.setActionType("isCreated"); // Set default action type for response
        
        // Load breakup lists
        loadBreakupLists(entity.getTransactionPoid(), entity.getDetRowId(), dto);
        
        return dto;
    }
    
    private void loadBreakupLists(Long transactionPoid, Long detRowId, ApPurchaseCnGlDtlDto dto) {
        try {
            // Load billwise breakup
            var billwiseResponse = billwiseBreakupService.loadBillwiseBreakup(
                UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L,
                UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L,
                UserContext.getDocumentId(),
                transactionPoid
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
                        popup.setActionType("isCreated");
                        return popup;
                    })
                    .collect(Collectors.toList());
                dto.setBreakupList(breakupList);
            } else {
                dto.setBreakupList(new ArrayList<>());
            }
            
            // Load cost center breakup
            var costCenterResponse = costCenterBreakupService.loadCostCenterData(
                UserContext.getDocumentId(),
                transactionPoid,
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
                        popup.setActionType("isCreated");
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
}
