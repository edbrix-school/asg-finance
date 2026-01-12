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
        // Before save validation
        Long partyPoid = "SUPPLIER".equals(dto.getPartyType()) ? dto.getSupplierPoid() : dto.getPrincipalPoid();
        procRepository.beforeSaveValidation(dto.getPartyType(), partyPoid, dto.getRefType(), dto.getPjReversalRef());
        
        ApPurchaseCnHdr hdr = mapToEntity(dto);
        hdr.setCreatedBy(UserContext.getUserId());
        hdr.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        hdr.setDeleted("N");
        
        ApPurchaseCnHdr savedHdr = hdrRepository.save(hdr);
        
        saveDetails(savedHdr.getTransactionPoid(), dto);
        
        return getById(savedHdr.getTransactionPoid());
    }

    @Override
    @Transactional(readOnly = true)
    public ApPurchaseCnHdrDto getById(Long transactionPoid) {
        ApPurchaseCnHdr hdr = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Credit note not found"));
        
        ApPurchaseCnHdrDto dto = mapToDto(hdr);
        
        dto.setItemDetails(itemDtlRepository.findByTransactionPoid(transactionPoid).stream()
                .map(this::mapItemToDto).collect(Collectors.toList()));
        dto.setChargeDetails(chargeDtlRepository.findByTransactionPoid(transactionPoid).stream()
                .map(this::mapChargeToDto).collect(Collectors.toList()));
        dto.setGlDetails(glDtlRepository.findByTransactionPoid(transactionPoid).stream()
                .map(this::mapGlToDto).collect(Collectors.toList()));
        
        return dto;
    }

    @Override
    @Transactional
    public ApPurchaseCnHdrDto update(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        ApPurchaseCnHdr existing = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Credit note not found"));
        
        updateEntityFromDto(existing, dto);
        existing.setLastModifiedBy(UserContext.getUserId());
        existing.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        
        hdrRepository.save(existing);
        
        itemDtlRepository.deleteByTransactionPoid(transactionPoid);
        chargeDtlRepository.deleteByTransactionPoid(transactionPoid);
        glDtlRepository.deleteByTransactionPoid(transactionPoid);
        saveDetails(transactionPoid, dto);
        return getById(transactionPoid);
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid) {
        ApPurchaseCnHdr hdr = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Credit note not found"));
        hdr.setDeleted("Y");
        hdr.setLastModifiedBy(UserContext.getUserId());
        hdr.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        hdrRepository.save(hdr);
    }

    @Override
    public Map<String, Object> list(String documentId, FilterRequestDto filters, 
                                     LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getPjRefDetails(Long pjPoid) {
        return procRepository.getPjRefDetails(pjPoid);
    }

    @Override
    public Map<String, Object> getPartyDetails(String partyType, Long partyPoid) {
        return procRepository.getPartyDetails(partyType, partyPoid);
    }

    private void saveDetails(Long transactionPoid, ApPurchaseCnHdrDto dto) {
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
            saveBillwiseForGl(transactionPoid, dto.getGlDetails(), UserContext.getDocumentId());
            saveCostCenterForGl(transactionPoid, dto.getGlDetails(), UserContext.getDocumentId());
        }
    }

    public void saveBillwiseForGl(Long transactionPoid, List<ApPurchaseCnGlDtlDto> glDetails, String docId) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }
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
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 3L;
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
        return ApPurchaseCnHdr.builder()
                .transactionDate(dto.getTransactionDate())
                .docRef(dto.getDocRef())
                .poRef(dto.getPoRef())
                .companyPoid(dto.getCompanyPoid())
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
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setPoRef(dto.getPoRef());
        entity.setCompanyPoid(dto.getCompanyPoid());
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
        return dto;
    }
}
