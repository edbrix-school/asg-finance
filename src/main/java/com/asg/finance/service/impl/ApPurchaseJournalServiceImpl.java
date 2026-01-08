package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.service.ApPurchaseServiceJournal;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import net.sf.jasperreports.engine.JasperReport;
import javax.sql.DataSource;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.entity.key.ApPurchaseInvoiceAssetDtlKey;
import com.asg.finance.entity.key.ApPurchaseInvoiceGlDtlKey;
import com.asg.finance.entity.key.ApPurchaseInvoiceItemDtlKey;
import com.asg.finance.entity.key.ApPurchaseInvRjvDetailsKey;
import com.asg.finance.entity.master.ShipChargeEntity;
import com.asg.finance.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.security.util.UserContext;
import org.apache.commons.collections4.CollectionUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApPurchaseJournalServiceImpl implements ApPurchaseServiceJournal {

    private final ApPurchaseInvoiceHdrRepository repository;
    private final ApPurchaseInvoiceItemDtlRepository apPurchaseInvoiceItemDtlRepository;
    private final ApPurchaseInvoiceGlDtlRepository apPurchaseInvoiceGlDtlRepository;
    private final ApPurchaseInvoiceAssetDtlRepository apPurchaseInvoiceAssetDtlRepository;
    private final ApPurchaseInvRjvDetailsRepository apPurchaseInvRjvDetailsRepository;
    private final PurchaseInvoiceChargeDtlRepository purchaseInvoiceChargeDtlRepository;
    private final ApPurchaseJournalRepository apPurchaseJournalRepositoryImpl;
    private final LovDataService lovService;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final BillwiseBreakupService billwiseBreakupService;
    private final CostCenterBreakupService costCenterBreakupService;
    private final PrintService printService;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public ApPurchaseInvoiceHdrDto fetchApPurchaseInvoiceHdr(Long transactionPoid) {
        ApPurchaseInvoiceHdrEntity apPurchaseInvoiceHdrEntity = repository.findByTransactionPoid(transactionPoid);
        if (apPurchaseInvoiceHdrEntity == null) {
            throw new ResourceNotFoundException("ApPurchaseInvoiceHdr", "transactionPoid", transactionPoid);
        }
        ApPurchaseInvoiceHdrDto apPurchaseInvoiceHdrDto = new ApPurchaseInvoiceHdrDto();
        apPurchaseInvoiceHdrDto.setTransactionPoid(apPurchaseInvoiceHdrEntity.getTransactionPoid());
        apPurchaseInvoiceHdrDto.setTransactionDate(apPurchaseInvoiceHdrEntity.getTransactionDate());
        apPurchaseInvoiceHdrDto.setGroupPoid(apPurchaseInvoiceHdrEntity.getGroupPoid());
        apPurchaseInvoiceHdrDto.setDocRef(apPurchaseInvoiceHdrEntity.getDocRef());
        apPurchaseInvoiceHdrDto.setPoRef(apPurchaseInvoiceHdrEntity.getPoRef());
        apPurchaseInvoiceHdrDto.setFdaRef(apPurchaseInvoiceHdrEntity.getFdaRef());
        apPurchaseInvoiceHdrDto.setFfRef(apPurchaseInvoiceHdrEntity.getFfRef());
        apPurchaseInvoiceHdrDto.setShipRef(apPurchaseInvoiceHdrEntity.getShipRef());
        apPurchaseInvoiceHdrDto.setCompanyPoid(apPurchaseInvoiceHdrEntity.getCompanyPoid());
        apPurchaseInvoiceHdrDto.setCurrencyCode(apPurchaseInvoiceHdrEntity.getCurrencyCode());
        apPurchaseInvoiceHdrDto.setCurrencyRate(apPurchaseInvoiceHdrEntity.getCurrencyRate());
        apPurchaseInvoiceHdrDto.setSupplierPoid(apPurchaseInvoiceHdrEntity.getSupplierPoid());
        apPurchaseInvoiceHdrDto.setLocationPoid(apPurchaseInvoiceHdrEntity.getLocationPoid());
        apPurchaseInvoiceHdrDto.setSubTotal(apPurchaseInvoiceHdrEntity.getSubTotal());
        apPurchaseInvoiceHdrDto.setDiscount(apPurchaseInvoiceHdrEntity.getDiscount());
        apPurchaseInvoiceHdrDto.setExpenseBySupplier(apPurchaseInvoiceHdrEntity.getExpenseBySupplier());
        apPurchaseInvoiceHdrDto.setGrandTotal(apPurchaseInvoiceHdrEntity.getGrandTotal());
        apPurchaseInvoiceHdrDto.setRemarks(apPurchaseInvoiceHdrEntity.getRemarks());
        apPurchaseInvoiceHdrDto.setCreatedBy(apPurchaseInvoiceHdrEntity.getCreatedBy());
        apPurchaseInvoiceHdrDto.setCreatedDate(apPurchaseInvoiceHdrEntity.getCreatedDate());
        apPurchaseInvoiceHdrDto.setLastModifiedBy(apPurchaseInvoiceHdrEntity.getLastModifiedBy());
        apPurchaseInvoiceHdrDto.setLastModifiedDate(apPurchaseInvoiceHdrEntity.getLastModifiedDate());
        apPurchaseInvoiceHdrDto.setDeleted(apPurchaseInvoiceHdrEntity.getDeleted());
        apPurchaseInvoiceHdrDto.setItemTotal(apPurchaseInvoiceHdrEntity.getItemTotal());
        apPurchaseInvoiceHdrDto.setChargeTotal(apPurchaseInvoiceHdrEntity.getChargeTotal());
        apPurchaseInvoiceHdrDto.setGlTotal(apPurchaseInvoiceHdrEntity.getGlTotal());
        apPurchaseInvoiceHdrDto.setType(apPurchaseInvoiceHdrEntity.getType());
        apPurchaseInvoiceHdrDto.setDescription(apPurchaseInvoiceHdrEntity.getDescription());
        apPurchaseInvoiceHdrDto.setCreditPeriod(apPurchaseInvoiceHdrEntity.getCreditPeriod());
        apPurchaseInvoiceHdrDto.setDueDate(apPurchaseInvoiceHdrEntity.getDueDate());
        apPurchaseInvoiceHdrDto.setInvnoOld(apPurchaseInvoiceHdrEntity.getInvnoOld());
        apPurchaseInvoiceHdrDto.setModcodeOld(apPurchaseInvoiceHdrEntity.getModcodeOld());
        apPurchaseInvoiceHdrDto.setRefType(apPurchaseInvoiceHdrEntity.getRefType());
        apPurchaseInvoiceHdrDto.setSalesQtnPoid(apPurchaseInvoiceHdrEntity.getSalesQtnPoid());
        apPurchaseInvoiceHdrDto.setNarration(apPurchaseInvoiceHdrEntity.getNarration());
        apPurchaseInvoiceHdrDto.setSupplierInvDate(apPurchaseInvoiceHdrEntity.getSupplierInvDate());
        apPurchaseInvoiceHdrDto.setSupplierInvNo(apPurchaseInvoiceHdrEntity.getSupplierInvNo());
        apPurchaseInvoiceHdrDto.setSupplierInvRemark(apPurchaseInvoiceHdrEntity.getSupplierInvRemark());
        apPurchaseInvoiceHdrDto.setMtaRef(apPurchaseInvoiceHdrEntity.getMtaRef());
        apPurchaseInvoiceHdrDto.setMultiCompany(apPurchaseInvoiceHdrEntity.getMultiCompany());
        apPurchaseInvoiceHdrDto.setBhdAmount(apPurchaseInvoiceHdrEntity.getBhdAmount());
        apPurchaseInvoiceHdrDto.setSupplierInvAmount(apPurchaseInvoiceHdrEntity.getSupplierInvAmount());
        apPurchaseInvoiceHdrDto.setRoundingAmount(apPurchaseInvoiceHdrEntity.getRoundingAmount());
        apPurchaseInvoiceHdrDto.setBillType(apPurchaseInvoiceHdrEntity.getBillType());
        apPurchaseInvoiceHdrDto.setProvisionalInvoice(apPurchaseInvoiceHdrEntity.getProvisionalInvoice());
        apPurchaseInvoiceHdrDto.setPartyType(apPurchaseInvoiceHdrEntity.getPartyType());
        apPurchaseInvoiceHdrDto.setGrnSupplierPoid(apPurchaseInvoiceHdrEntity.getGrnSupplierPoid());
        apPurchaseInvoiceHdrDto.setPartyTinNumber(apPurchaseInvoiceHdrEntity.getPartyTinNumber());
        apPurchaseInvoiceHdrDto.setPaidAgainst(apPurchaseInvoiceHdrEntity.getPaidAgainst());
        apPurchaseInvoiceHdrDto.setFdaCoveringRef(apPurchaseInvoiceHdrEntity.getFdaCoveringRef());
        if (apPurchaseInvoiceHdrEntity.getGroupPoid() != null) {
            apPurchaseInvoiceHdrDto.setGroupDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceHdrEntity.getGroupPoid(), "GROUP"));
        }
        if (apPurchaseInvoiceHdrEntity.getCompanyPoid() != null) {
            apPurchaseInvoiceHdrDto.setCompanyDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceHdrEntity.getCompanyPoid(), "COMPANY"));
        }
        if (apPurchaseInvoiceHdrEntity.getSupplierPoid() != null) {
            apPurchaseInvoiceHdrDto.setSupplierDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceHdrEntity.getSupplierPoid(), "SUPPLIER"));
        }
        if (apPurchaseInvoiceHdrEntity.getLocationPoid() != null) {
            apPurchaseInvoiceHdrDto.setLocationDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceHdrEntity.getLocationPoid(), "LOCATION"));
        }
        if (apPurchaseInvoiceHdrEntity.getSalesQtnPoid() != null) {
            apPurchaseInvoiceHdrDto.setSalesQtnDet(lovService.getDetailsByPoidAndLovName(Long.valueOf(apPurchaseInvoiceHdrEntity.getSalesQtnPoid()), "SALES_QTN_REF"));
        }
        if (apPurchaseInvoiceHdrEntity.getGrnSupplierPoid() != null) {
            apPurchaseInvoiceHdrDto.setGrnSupplierDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceHdrEntity.getGrnSupplierPoid(), "SUPPLIER"));
        }
        apPurchaseInvoiceHdrDto.setItemDtls(buildItemDtls(apPurchaseInvoiceItemDtlRepository.findByIdTransactionPoid(transactionPoid)));
        List<ApPurchaseInvoiceGlDtlDto> glDetailDtos = buildGlDtls(apPurchaseInvoiceGlDtlRepository.findByIdTransactionPoid(transactionPoid));
        loadBillwiseAndCostCenterBreakup(glDetailDtos, transactionPoid, apPurchaseInvoiceHdrEntity);
        apPurchaseInvoiceHdrDto.setGlDtls(glDetailDtos);
        apPurchaseInvoiceHdrDto.setAssetDtls(buildAssetDtls(apPurchaseInvoiceAssetDtlRepository.findByIdTransactionPoid(transactionPoid)));
        apPurchaseInvoiceHdrDto.setRjvDtls(buildRjvDtls(apPurchaseInvRjvDetailsRepository.findByIdTransactionPoid(transactionPoid)));
        apPurchaseInvoiceHdrDto.setChargeDtls(buildChargeDtls(
                purchaseInvoiceChargeDtlRepository.findByIdTransactionPoid(transactionPoid)
        ));
        return apPurchaseInvoiceHdrDto;
    }

    private List<ApPurchaseInvoiceItemDtlDto> buildItemDtls(List<ApPurchaseInvoiceItemDtlEntity> itemDtls) {
        return itemDtls.stream().map(apPurchaseInvoiceItemDtlEntity -> {
            ApPurchaseInvoiceItemDtlDto apPurchaseInvoiceItemDtlDto = new ApPurchaseInvoiceItemDtlDto();

            apPurchaseInvoiceItemDtlDto.setTransactionPoid(apPurchaseInvoiceItemDtlEntity.getId().getTransactionPoid());
            apPurchaseInvoiceItemDtlDto.setDetRowId(apPurchaseInvoiceItemDtlEntity.getId().getDetRowId());

            apPurchaseInvoiceItemDtlDto.setStockPoid(apPurchaseInvoiceItemDtlEntity.getStockPoid());
            apPurchaseInvoiceItemDtlDto.setStockUnitPoid(apPurchaseInvoiceItemDtlEntity.getStockUnitPoid());
            apPurchaseInvoiceItemDtlDto.setPoQty(apPurchaseInvoiceItemDtlEntity.getPoQty());
            apPurchaseInvoiceItemDtlDto.setDnQty(apPurchaseInvoiceItemDtlEntity.getDnQty());
            apPurchaseInvoiceItemDtlDto.setQtyReceived(apPurchaseInvoiceItemDtlEntity.getQtyReceived());
            apPurchaseInvoiceItemDtlDto.setPrice(apPurchaseInvoiceItemDtlEntity.getPrice());
            apPurchaseInvoiceItemDtlDto.setDiscount(apPurchaseInvoiceItemDtlEntity.getDiscount());
            apPurchaseInvoiceItemDtlDto.setTotal(apPurchaseInvoiceItemDtlEntity.getTotal());
            apPurchaseInvoiceItemDtlDto.setRemarks(apPurchaseInvoiceItemDtlEntity.getRemarks());
            apPurchaseInvoiceItemDtlDto.setCreatedBy(apPurchaseInvoiceItemDtlEntity.getCreatedBy());
            apPurchaseInvoiceItemDtlDto.setCreatedDate(apPurchaseInvoiceItemDtlEntity.getCreatedDate());
            apPurchaseInvoiceItemDtlDto.setLastModifiedBy(apPurchaseInvoiceItemDtlEntity.getLastModifiedBy());
            apPurchaseInvoiceItemDtlDto.setLastModifiedDate(apPurchaseInvoiceItemDtlEntity.getLastModifiedDate());
            apPurchaseInvoiceItemDtlDto.setRefDocId(apPurchaseInvoiceItemDtlEntity.getRefDocId());
            apPurchaseInvoiceItemDtlDto.setRefDocPoid(apPurchaseInvoiceItemDtlEntity.getRefDocPoid());
            apPurchaseInvoiceItemDtlDto.setCheckAll(apPurchaseInvoiceItemDtlEntity.getCheckAll());
            apPurchaseInvoiceItemDtlDto.setRefDetRowId(apPurchaseInvoiceItemDtlEntity.getRefDetRowId());
            apPurchaseInvoiceItemDtlDto.setTaxPoid(apPurchaseInvoiceItemDtlEntity.getTaxPoid());
            apPurchaseInvoiceItemDtlDto.setTaxPercentage(apPurchaseInvoiceItemDtlEntity.getTaxPercentage());
            apPurchaseInvoiceItemDtlDto.setTaxAmount(apPurchaseInvoiceItemDtlEntity.getTaxAmount());
            apPurchaseInvoiceItemDtlDto.setAmount(apPurchaseInvoiceItemDtlEntity.getAmount());
            apPurchaseInvoiceItemDtlDto.setBaseAmount(apPurchaseInvoiceItemDtlEntity.getBaseAmount());
            apPurchaseInvoiceItemDtlDto.setStockDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceItemDtlEntity.getStockPoid(), "STOCK"));
            apPurchaseInvoiceItemDtlDto.setStockUnitDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceItemDtlEntity.getStockUnitPoid(), "STOCK_UNIT"));
            apPurchaseInvoiceItemDtlDto.setTaxDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceItemDtlEntity.getTaxPoid(), "TAX"));
            apPurchaseInvoiceItemDtlDto.setRefDocDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceItemDtlEntity.getRefDocPoid(), "DOCUMENTS"));

            return apPurchaseInvoiceItemDtlDto;
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<ApPurchaseInvoiceGlDtlDto> buildGlDtls(List<ApPurchaseInvoiceGlDtlEntity> glDtls) {
        return glDtls.stream().map(apPurchaseInvoiceGlDtlEntity -> {
            ApPurchaseInvoiceGlDtlDto apPurchaseInvoiceGlDtlDto = new ApPurchaseInvoiceGlDtlDto();

            apPurchaseInvoiceGlDtlDto.setTransactionPoid(apPurchaseInvoiceGlDtlEntity.getId().getTransactionPoid());
            apPurchaseInvoiceGlDtlDto.setDetRowId(apPurchaseInvoiceGlDtlEntity.getId().getDetRowId());

            apPurchaseInvoiceGlDtlDto.setType(apPurchaseInvoiceGlDtlEntity.getType());
            apPurchaseInvoiceGlDtlDto.setCompanyPoid(apPurchaseInvoiceGlDtlEntity.getCompanyPoid());
            apPurchaseInvoiceGlDtlDto.setGlPoid(apPurchaseInvoiceGlDtlEntity.getGlPoid());
            apPurchaseInvoiceGlDtlDto.setDrAmount(apPurchaseInvoiceGlDtlEntity.getDrAmount());
            apPurchaseInvoiceGlDtlDto.setCrAmount(apPurchaseInvoiceGlDtlEntity.getCrAmount());
            apPurchaseInvoiceGlDtlDto.setRefDocId(apPurchaseInvoiceGlDtlEntity.getRefDocId());
            apPurchaseInvoiceGlDtlDto.setRefDocPoid(apPurchaseInvoiceGlDtlEntity.getRefDocPoid());
            apPurchaseInvoiceGlDtlDto.setDescription(apPurchaseInvoiceGlDtlEntity.getDescription());
            apPurchaseInvoiceGlDtlDto.setRemarks(apPurchaseInvoiceGlDtlEntity.getRemarks());
            apPurchaseInvoiceGlDtlDto.setCreatedBy(apPurchaseInvoiceGlDtlEntity.getCreatedBy());
            apPurchaseInvoiceGlDtlDto.setCreatedDate(apPurchaseInvoiceGlDtlEntity.getCreatedDate());
            apPurchaseInvoiceGlDtlDto.setLastModifiedBy(apPurchaseInvoiceGlDtlEntity.getLastModifiedBy());
            apPurchaseInvoiceGlDtlDto.setLastModifiedDate(apPurchaseInvoiceGlDtlEntity.getLastModifiedDate());
            apPurchaseInvoiceGlDtlDto.setJobNoOld(apPurchaseInvoiceGlDtlEntity.getJobNoOld());
            apPurchaseInvoiceGlDtlDto.setModCodeOld(apPurchaseInvoiceGlDtlEntity.getModCodeOld());
            apPurchaseInvoiceGlDtlDto.setTaxPoid(apPurchaseInvoiceGlDtlEntity.getTaxPoid());
            apPurchaseInvoiceGlDtlDto.setTaxPercentage(apPurchaseInvoiceGlDtlEntity.getTaxPercentage());
            apPurchaseInvoiceGlDtlDto.setTaxAmount(apPurchaseInvoiceGlDtlEntity.getTaxAmount());
            apPurchaseInvoiceGlDtlDto.setTotalAmount(apPurchaseInvoiceGlDtlEntity.getTotalAmount());
            apPurchaseInvoiceGlDtlDto.setCompanyDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceGlDtlEntity.getCompanyPoid(), "COMPANY"));
            apPurchaseInvoiceGlDtlDto.setGlDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceGlDtlEntity.getGlPoid(), "GL"));
            apPurchaseInvoiceGlDtlDto.setRefDocDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceGlDtlEntity.getRefDocPoid(), "DOCUMENTS"));
            apPurchaseInvoiceGlDtlDto.setTaxDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvoiceGlDtlEntity.getTaxPoid(), "TAX"));

            return apPurchaseInvoiceGlDtlDto;
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<ApPurchaseInvoiceAssetDtlDto> buildAssetDtls(List<ApPurchaseInvoiceAssetDtlEntity> assetDtls) {
        return assetDtls.stream().map(apPurchaseInvoiceAssetDtlEntity -> {
            ApPurchaseInvoiceAssetDtlDto apPurchaseInvoiceAssetDtlDto = new ApPurchaseInvoiceAssetDtlDto();

            apPurchaseInvoiceAssetDtlDto.setTransactionPoid(apPurchaseInvoiceAssetDtlEntity.getId().getTransactionPoid());
            apPurchaseInvoiceAssetDtlDto.setDetRowId(apPurchaseInvoiceAssetDtlEntity.getId().getDetRowId());

            apPurchaseInvoiceAssetDtlDto.setFaCode(apPurchaseInvoiceAssetDtlEntity.getFaCode());
            apPurchaseInvoiceAssetDtlDto.setFaDescription(apPurchaseInvoiceAssetDtlEntity.getFaDescription());
            apPurchaseInvoiceAssetDtlDto.setFaCategory(apPurchaseInvoiceAssetDtlEntity.getFaCategory());
            apPurchaseInvoiceAssetDtlDto.setAssetType(apPurchaseInvoiceAssetDtlEntity.getAssetType());
            apPurchaseInvoiceAssetDtlDto.setValue(apPurchaseInvoiceAssetDtlEntity.getValue());
            apPurchaseInvoiceAssetDtlDto.setRemarks(apPurchaseInvoiceAssetDtlEntity.getRemarks());
            apPurchaseInvoiceAssetDtlDto.setCreatedBy(apPurchaseInvoiceAssetDtlEntity.getCreatedBy());
            apPurchaseInvoiceAssetDtlDto.setCreatedDate(apPurchaseInvoiceAssetDtlEntity.getCreatedDate());
            apPurchaseInvoiceAssetDtlDto.setLastModifiedBy(apPurchaseInvoiceAssetDtlEntity.getLastModifiedBy());
            apPurchaseInvoiceAssetDtlDto.setLastModifiedDate(apPurchaseInvoiceAssetDtlEntity.getLastModifiedDate());

            return apPurchaseInvoiceAssetDtlDto;
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<ApPurchaseInvRjvDetailsDto> buildRjvDtls(List<ApPurchaseInvRjvDetailsEntity> apPurchaseInvRjvDetailsEntities) {
        return apPurchaseInvRjvDetailsEntities.stream().map(apPurchaseInvRjvDetailsEntity -> {
            ApPurchaseInvRjvDetailsDto apPurchaseInvRjvDetailsDto = new ApPurchaseInvRjvDetailsDto();

            apPurchaseInvRjvDetailsDto.setTransactionPoid(apPurchaseInvRjvDetailsEntity.getId().getTransactionPoid());
            apPurchaseInvRjvDetailsDto.setDetRowId(apPurchaseInvRjvDetailsEntity.getId().getDetRowId());

            apPurchaseInvRjvDetailsDto.setDrilldownLinkInfo(apPurchaseInvRjvDetailsEntity.getDrilldownLinkInfo());
            apPurchaseInvRjvDetailsDto.setRjvPoid(apPurchaseInvRjvDetailsEntity.getRjvPoid());
            apPurchaseInvRjvDetailsDto.setRjvTrnDate(apPurchaseInvRjvDetailsEntity.getRjvTrnDate());
            apPurchaseInvRjvDetailsDto.setRjvDocRef(apPurchaseInvRjvDetailsEntity.getRjvDocRef());
            apPurchaseInvRjvDetailsDto.setRjvCompanyPoid(apPurchaseInvRjvDetailsEntity.getRjvCompanyPoid());
            apPurchaseInvRjvDetailsDto.setRjvRefType(apPurchaseInvRjvDetailsEntity.getRjvRefType());
            apPurchaseInvRjvDetailsDto.setRjvAmount(apPurchaseInvRjvDetailsEntity.getRjvAmount());
            apPurchaseInvRjvDetailsDto.setRjvRemarks(apPurchaseInvRjvDetailsEntity.getRjvRemarks());
            apPurchaseInvRjvDetailsDto.setRemarks(apPurchaseInvRjvDetailsEntity.getRemarks());
            apPurchaseInvRjvDetailsDto.setCreatedBy(apPurchaseInvRjvDetailsEntity.getCreatedBy());
            apPurchaseInvRjvDetailsDto.setCreatedDate(apPurchaseInvRjvDetailsEntity.getCreatedDate());
            apPurchaseInvRjvDetailsDto.setLastModifiedBy(apPurchaseInvRjvDetailsEntity.getLastModifiedBy());
            apPurchaseInvRjvDetailsDto.setLastModifiedDate(apPurchaseInvRjvDetailsEntity.getLastModifiedDate());
            apPurchaseInvRjvDetailsDto.setRjvCompanyDet(lovService.getDetailsByPoidAndLovName(apPurchaseInvRjvDetailsEntity.getRjvCompanyPoid(), "COMPANY"));

            return apPurchaseInvRjvDetailsDto;
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<PurchaseInvoiceChargeDtlRequestDto> buildChargeDtls(List<PurchaseInvoiceChargeDtl> list) {
        return list.stream().map(e -> {

            PurchaseInvoiceChargeDtlRequestDto dto = new PurchaseInvoiceChargeDtlRequestDto();

            dto.setTransactionPoid(e.getId().getTransactionPoid());
            dto.setDetRowId(e.getId().getDetRowId());

            dto.setChargePoid(e.getChargePoid());
            dto.setChargeAmount(e.getChargeAmount());
            dto.setDescription(e.getDescription());
            dto.setRemarks(e.getRemarks());

            dto.setRefDocId(e.getRefDocId());
            dto.setRefDocPoid(e.getRefDocPoid());

            dto.setFdaDetRowId(e.getFdaDetRowId());
            dto.setCheckAll(e.getCheckAll());

            dto.setPdaAmount(e.getPdaAmount());
            dto.setFfAmount(e.getFfAmount());
            dto.setChargeFrom(e.getChargeFrom());

            dto.setTaxPoid(e.getTaxPoid());
            dto.setTaxPercentage(e.getTaxPercentage());
            dto.setTaxAmount(e.getTaxAmount());

            dto.setChargeBaseAmount(e.getChargeBaseAmount());
            dto.setSupplierPoidFf(e.getSupplierPoidFf());

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApPurchaseInvoiceHdrDto createApPurchaseInvoice(ApPurchaseInvoiceHdrDto apPurchaseInvoiceHdrDto, String documentId) {

        ApPurchaseInvoiceHdrEntity apPurchaseInvoiceHdrEntity = new ApPurchaseInvoiceHdrEntity();
        apPurchaseInvoiceHdrEntity.setTransactionDate(apPurchaseInvoiceHdrDto.getTransactionDate() != null ? apPurchaseInvoiceHdrDto.getTransactionDate() : LocalDate.now());
        apPurchaseInvoiceHdrEntity.setGroupPoid(apPurchaseInvoiceHdrDto.getGroupPoid());
        // apPurchaseInvoiceHdrEntity.setDocRef(apPurchaseInvoiceHdrDto.getDocRef());
        apPurchaseInvoiceHdrEntity.setPoRef(apPurchaseInvoiceHdrDto.getPoRef());
        apPurchaseInvoiceHdrEntity.setFdaRef(apPurchaseInvoiceHdrDto.getFdaRef());
        apPurchaseInvoiceHdrEntity.setFfRef(apPurchaseInvoiceHdrDto.getFfRef());
        apPurchaseInvoiceHdrEntity.setShipRef(apPurchaseInvoiceHdrDto.getShipRef());
        apPurchaseInvoiceHdrEntity.setCompanyPoid(apPurchaseInvoiceHdrDto.getCompanyPoid());
        apPurchaseInvoiceHdrEntity.setCurrencyCode(apPurchaseInvoiceHdrDto.getCurrencyCode());
        apPurchaseInvoiceHdrEntity.setCurrencyRate(apPurchaseInvoiceHdrDto.getCurrencyRate());
        apPurchaseInvoiceHdrEntity.setSupplierPoid(apPurchaseInvoiceHdrDto.getSupplierPoid());
        apPurchaseInvoiceHdrEntity.setLocationPoid(apPurchaseInvoiceHdrDto.getLocationPoid());
        apPurchaseInvoiceHdrEntity.setSubTotal(apPurchaseInvoiceHdrDto.getSubTotal());
        apPurchaseInvoiceHdrEntity.setDiscount(apPurchaseInvoiceHdrDto.getDiscount());
        apPurchaseInvoiceHdrEntity.setExpenseBySupplier(apPurchaseInvoiceHdrDto.getExpenseBySupplier());
        apPurchaseInvoiceHdrEntity.setGrandTotal(apPurchaseInvoiceHdrDto.getGrandTotal());
        apPurchaseInvoiceHdrEntity.setRemarks(apPurchaseInvoiceHdrDto.getRemarks());
        apPurchaseInvoiceHdrEntity.setCreatedBy(getCurrentUser());
        apPurchaseInvoiceHdrEntity.setCreatedDate(LocalDateTime.now());
        apPurchaseInvoiceHdrEntity.setLastModifiedBy(getCurrentUser());
        apPurchaseInvoiceHdrEntity.setLastModifiedDate(LocalDateTime.now());
        apPurchaseInvoiceHdrEntity.setDeleted(apPurchaseInvoiceHdrDto.getDeleted() != null ? apPurchaseInvoiceHdrDto.getDeleted() : "N");
        apPurchaseInvoiceHdrEntity.setItemTotal(apPurchaseInvoiceHdrDto.getItemTotal());
        apPurchaseInvoiceHdrEntity.setChargeTotal(apPurchaseInvoiceHdrDto.getChargeTotal());
        apPurchaseInvoiceHdrEntity.setGlTotal(apPurchaseInvoiceHdrDto.getGlTotal());
        apPurchaseInvoiceHdrEntity.setType(apPurchaseInvoiceHdrDto.getType());
        apPurchaseInvoiceHdrEntity.setDescription(apPurchaseInvoiceHdrDto.getDescription());
        apPurchaseInvoiceHdrEntity.setCreditPeriod(apPurchaseInvoiceHdrDto.getCreditPeriod());
        apPurchaseInvoiceHdrEntity.setDueDate(apPurchaseInvoiceHdrDto.getDueDate());
        apPurchaseInvoiceHdrEntity.setInvnoOld(apPurchaseInvoiceHdrDto.getInvnoOld());
        apPurchaseInvoiceHdrEntity.setModcodeOld(apPurchaseInvoiceHdrDto.getModcodeOld());
        apPurchaseInvoiceHdrEntity.setRefType(apPurchaseInvoiceHdrDto.getRefType());
        apPurchaseInvoiceHdrEntity.setSalesQtnPoid(apPurchaseInvoiceHdrDto.getSalesQtnPoid());
        apPurchaseInvoiceHdrEntity.setNarration(apPurchaseInvoiceHdrDto.getNarration());
        apPurchaseInvoiceHdrEntity.setSupplierInvDate(apPurchaseInvoiceHdrDto.getSupplierInvDate());
        apPurchaseInvoiceHdrEntity.setSupplierInvNo(apPurchaseInvoiceHdrDto.getSupplierInvNo());
        apPurchaseInvoiceHdrEntity.setSupplierInvRemark(apPurchaseInvoiceHdrDto.getSupplierInvRemark());
        apPurchaseInvoiceHdrEntity.setMtaRef(apPurchaseInvoiceHdrDto.getMtaRef());
        apPurchaseInvoiceHdrEntity.setMultiCompany(apPurchaseInvoiceHdrDto.getMultiCompany());
        apPurchaseInvoiceHdrEntity.setBhdAmount(apPurchaseInvoiceHdrDto.getBhdAmount());
        apPurchaseInvoiceHdrEntity.setSupplierInvAmount(apPurchaseInvoiceHdrDto.getSupplierInvAmount());
        apPurchaseInvoiceHdrEntity.setRoundingAmount(apPurchaseInvoiceHdrDto.getRoundingAmount());
        apPurchaseInvoiceHdrEntity.setBillType(apPurchaseInvoiceHdrDto.getBillType());
        apPurchaseInvoiceHdrEntity.setProvisionalInvoice(apPurchaseInvoiceHdrDto.getProvisionalInvoice());
        apPurchaseInvoiceHdrEntity.setPartyType(apPurchaseInvoiceHdrDto.getPartyType());
        apPurchaseInvoiceHdrEntity.setGrnSupplierPoid(apPurchaseInvoiceHdrDto.getGrnSupplierPoid());
        apPurchaseInvoiceHdrEntity.setPartyTinNumber(apPurchaseInvoiceHdrDto.getPartyTinNumber());
        apPurchaseInvoiceHdrEntity.setPaidAgainst(apPurchaseInvoiceHdrDto.getPaidAgainst());
        apPurchaseInvoiceHdrEntity.setFdaCoveringRef(apPurchaseInvoiceHdrDto.getFdaCoveringRef());

        ApPurchaseInvoiceHdrEntity savedApPurchaseInvoiceHdrEntity = repository.save(apPurchaseInvoiceHdrEntity);
        entityManager.flush();
        entityManager.refresh(savedApPurchaseInvoiceHdrEntity);

        Long transactionPoid = savedApPurchaseInvoiceHdrEntity.getTransactionPoid();

        String refType = apPurchaseInvoiceHdrDto.getRefType();
        if (refType != null) refType = refType.trim().toUpperCase();

        Long refPoid = null;

        if ("FF JOBS".equalsIgnoreCase(refType) || "FDA JOBS".equalsIgnoreCase(refType)) {
            if (apPurchaseInvoiceHdrDto.getChargeDtls() != null &&
                    !apPurchaseInvoiceHdrDto.getChargeDtls().isEmpty()) {

                refPoid = apPurchaseInvoiceHdrDto.getChargeDtls().get(0).getRefDocPoid();
            }
        }

        if (refPoid == null) {
            refPoid = transactionPoid;
        }

        String validateStatus = validateVoucher(
                documentId,
                refType,
                String.valueOf(refPoid)
        );

        String jobValidation = validateBeforeSave(
                documentId,
                refType,
                String.valueOf(transactionPoid)
        );

        if (jobValidation != null &&
                !jobValidation.equalsIgnoreCase("SUCCESS")) {
            throw new RuntimeException("Before Save Validation Failed → " + jobValidation);
        }


        switch (refType) {

            case "GENERAL":
            case "CUSTOM":
            case "GENERAL PO":
                saveGlDetails(transactionPoid, apPurchaseInvoiceHdrDto);
                break;

            case "FF JOBS":
            case "FDA JOBS":
                saveChargeDetails(transactionPoid, apPurchaseInvoiceHdrDto);
                break;

            case "MTA PO":
                saveItemDetails(transactionPoid, apPurchaseInvoiceHdrDto);
                break;

            default:
                log.info("No action for refType: {}", refType);
        }
        saveAssetDetails(transactionPoid, apPurchaseInvoiceHdrDto);
        saveRjvDetails(transactionPoid, apPurchaseInvoiceHdrDto);

        return fetchApPurchaseInvoiceHdr(transactionPoid);
    }

    private void saveItemDetails(Long transactionPoid, ApPurchaseInvoiceHdrDto dto) {

        if (dto.getItemDtls() == null || dto.getItemDtls().isEmpty()) {
            log.info("No Item details found.");
            return;
        }

        Long maxDetRowId = apPurchaseInvoiceItemDtlRepository
                .findMaxDetRowIdByTransactionPoid(transactionPoid);

        long detRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;

        List<ApPurchaseInvoiceItemDtlEntity> items = new ArrayList<>();

        for (ApPurchaseInvoiceItemDtlDto d : dto.getItemDtls()) {
            String actionTypeStr = d.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "isCreated";
            }
            String actionType = actionTypeStr.toUpperCase();

            if (!"ISCREATED".equals(actionType)) {
                continue;
            }

            ApPurchaseInvoiceItemDtlKey key = new ApPurchaseInvoiceItemDtlKey(
                    transactionPoid,
                    detRowId++
            );

            ApPurchaseInvoiceItemDtlEntity e = new ApPurchaseInvoiceItemDtlEntity();
            e.setId(key);

            e.setStockPoid(d.getStockPoid());
            e.setStockUnitPoid(d.getStockUnitPoid());
            e.setPoQty(d.getPoQty());
            e.setDnQty(d.getDnQty());
            e.setQtyReceived(d.getQtyReceived());
            e.setPrice(d.getPrice());
            e.setDiscount(d.getDiscount());
            e.setTotal(d.getTotal());
            e.setRemarks(d.getRemarks());

            e.setCreatedBy(getCurrentUser());
            e.setCreatedDate(LocalDateTime.now());
            e.setLastModifiedBy(getCurrentUser());
            e.setLastModifiedDate(LocalDateTime.now());

            e.setRefDocId(d.getRefDocId());
            e.setRefDocPoid(d.getRefDocPoid());
            e.setCheckAll(d.getCheckAll());
            e.setRefDetRowId(d.getRefDetRowId());
            e.setTaxPoid(d.getTaxPoid());
            e.setTaxPercentage(d.getTaxPercentage());
            e.setTaxAmount(d.getTaxAmount());
            e.setAmount(d.getAmount());
            e.setBaseAmount(d.getBaseAmount());

            items.add(e);
        }

        apPurchaseInvoiceItemDtlRepository.saveAll(items);
    }

    private void saveGlDetails(Long transactionPoid, ApPurchaseInvoiceHdrDto dto) {

        if (dto.getGlDtls() == null || dto.getGlDtls().isEmpty()) {
            log.info("No GL details found.");
            return;
        }

        Long maxDetRowId = apPurchaseInvoiceGlDtlRepository
                .findMaxDetRowIdByTransactionPoid(transactionPoid);

        long detRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;

        List<ApPurchaseInvoiceGlDtlEntity> list = new ArrayList<>();
        List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();

        for (ApPurchaseInvoiceGlDtlDto g : dto.getGlDtls()) {
            String actionTypeStr = g.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "isCreated";
            }
            String actionType = actionTypeStr.toUpperCase();

            if (!"ISCREATED".equals(actionType)) {
                continue;
            }

            Long mainDetRowId = detRowId;
            ApPurchaseInvoiceGlDtlKey key = new ApPurchaseInvoiceGlDtlKey(
                    transactionPoid,
                    detRowId++
            );

            ApPurchaseInvoiceGlDtlEntity e = new ApPurchaseInvoiceGlDtlEntity();
            e.setId(key);

            e.setType(g.getType());
            e.setCompanyPoid(g.getCompanyPoid());
            e.setGlPoid(g.getGlPoid());
            e.setDrAmount(g.getDrAmount());
            e.setCrAmount(g.getCrAmount());
            e.setRefDocId(g.getRefDocId());
            e.setRefDocPoid(g.getRefDocPoid());
            e.setDescription(g.getDescription());
            e.setRemarks(g.getRemarks());

            e.setCreatedBy(getCurrentUser());
            e.setCreatedDate(LocalDateTime.now());
            e.setLastModifiedBy(getCurrentUser());
            e.setLastModifiedDate(LocalDateTime.now());

            e.setJobNoOld(g.getJobNoOld());
            e.setModCodeOld(g.getModCodeOld());
            e.setTaxPoid(g.getTaxPoid());
            e.setTaxPercentage(g.getTaxPercentage());
            e.setTaxAmount(g.getTaxAmount());
            e.setTotalAmount(g.getTotalAmount());

            list.add(e);

            // Process billwise breakup
            if (g.getBillwiseBreakupList() != null && !g.getBillwiseBreakupList().isEmpty()) {
                for (BillwiseBreakupPopupRequestDto popup : g.getBillwiseBreakupList()) {
                    String billActionTypeStr = popup.getActionType();
                    if (billActionTypeStr == null || billActionTypeStr.trim().isEmpty()) {
                        billActionTypeStr = "isCreated";
                    }
                    String billActionType = billActionTypeStr.toUpperCase();

                    if (!"ISCREATED".equals(billActionType)) {
                        continue;
                    }

                    BillwiseBreakupRequestDto dto1 = new BillwiseBreakupRequestDto();
                    dto1.setGroupPoid(UserContext.getGroupPoid());
                    dto1.setCompanyPoid(UserContext.getCompanyPoid());
                    dto1.setDocId("200-103");
                    dto1.setTransactionPoid(transactionPoid);
                    dto1.setGlPoid(g.getGlPoid());
                    dto1.setMainDetRowId(mainDetRowId);
                    dto1.setBillDetRowId(popup.getBillDetRowId());
                    dto1.setBillRefType(popup.getBillRefType());
                    dto1.setBillRef(popup.getBillRef());
                    dto1.setBillDueDate(popup.getBillDueDate());
                    dto1.setDrAmt(popup.getAmount());
                    dto1.setCrAmt(popup.getAmount());
                    dto1.setBillRemarks(popup.getBillRemarks());
                    billwiseList.add(dto1);
                }
            }

            // Process cost center breakup
            if (g.getCostCenterBreakupList() != null && !g.getCostCenterBreakupList().isEmpty()) {
                for (CostCenterBreakupPopupRequestDto popup : g.getCostCenterBreakupList()) {
                    String costActionTypeStr = popup.getActionType();
                    if (costActionTypeStr == null || costActionTypeStr.trim().isEmpty()) {
                        costActionTypeStr = "isCreated";
                    }
                    String costActionType = costActionTypeStr.toUpperCase();

                    if (!"ISCREATED".equals(costActionType)) {
                        continue;
                    }

                    CostCenterBreakupRequestDto dto2 = new CostCenterBreakupRequestDto();
                    dto2.setGroupPoid(UserContext.getGroupPoid());
                    dto2.setCompanyPoid(UserContext.getCompanyPoid());
                    dto2.setDocId("200-103");
                    dto2.setTransactionPoid(transactionPoid);
                    dto2.setGlPoid(g.getGlPoid());
                    dto2.setMainDetRowId(mainDetRowId);
                    dto2.setCostDetRowId(popup.getCostDetRowId());
                    dto2.setCostGroup(popup.getCostGroup());
                    dto2.setCostPoid(popup.getCostPoid());
                    dto2.setAmount(popup.getAmount());
                    costCenterList.add(dto2);
                }
            }
        }

        apPurchaseInvoiceGlDtlRepository.saveAll(list);

        if (!billwiseList.isEmpty()) {
            billwiseBreakupService.insertBillwiseBreakup(billwiseList);
        }
        if (!costCenterList.isEmpty()) {
            costCenterBreakupService.saveCostCenterBreakups(costCenterList);
        }
    }

    private void saveChargeDetails(Long transactionPoid, ApPurchaseInvoiceHdrDto dto) {

        if (dto.getChargeDtls() == null || dto.getChargeDtls().isEmpty()) {
            log.info("No Charge details found.");
            return;
        }

        Long maxDetRowId = purchaseInvoiceChargeDtlRepository
                .findMaxDetRowIdByTransactionPoid(transactionPoid);

        long detRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;

        List<PurchaseInvoiceChargeDtl> list = new ArrayList<>();

        for (PurchaseInvoiceChargeDtlRequestDto c : dto.getChargeDtls()) {
            String actionTypeStr = c.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "isCreated";
            }
            String actionType = actionTypeStr.toUpperCase();

            if (!"ISCREATED".equals(actionType)) {
                continue;
            }

            PurchaseInvoiceChargeDtlId key = new PurchaseInvoiceChargeDtlId(
                    transactionPoid,
                    detRowId++
            );

            PurchaseInvoiceChargeDtl e = new PurchaseInvoiceChargeDtl();
            e.setId(key);

            e.setChargePoid(c.getChargePoid());
            e.setChargeAmount(c.getChargeAmount());
            e.setDescription(c.getDescription());
            e.setRemarks(c.getRemarks());

            e.setCreatedBy(getCurrentUser());
            e.setCreatedDate(LocalDateTime.now());
            e.setLastModifiedBy(getCurrentUser());
            e.setLastModifiedDate(LocalDateTime.now());

            e.setRefDocId(c.getRefDocId());
            e.setRefDocPoid(c.getRefDocPoid());
            e.setFdaDetRowId(c.getFdaDetRowId());
            e.setCheckAll(c.getCheckAll());
            e.setPdaAmount(c.getPdaAmount());
            e.setFfAmount(c.getFfAmount());
            e.setChargeFrom(c.getChargeFrom());
            e.setTaxPoid(c.getTaxPoid());
            e.setTaxPercentage(c.getTaxPercentage());
            e.setTaxAmount(c.getTaxAmount());
            e.setChargeBaseAmount(c.getChargeBaseAmount());
            e.setSupplierPoidFf(c.getSupplierPoidFf());

            list.add(e);
        }

        purchaseInvoiceChargeDtlRepository.saveAll(list);
    }

    private void saveAssetDetails(Long transactionPoid, ApPurchaseInvoiceHdrDto dto) {

        if (dto.getAssetDtls() == null || dto.getAssetDtls().isEmpty()) {
            return;
        }

        Long nextDetRowId =
                apPurchaseInvoiceAssetDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);

        long det = (nextDetRowId != null ? nextDetRowId : 0L) + 1L;

        log.info("Asset details start detRowId for transaction {}: {}", transactionPoid, det);

        List<ApPurchaseInvoiceAssetDtlEntity> entities = new ArrayList<>();

        for (ApPurchaseInvoiceAssetDtlDto adto : dto.getAssetDtls()) {
            String actionTypeStr = adto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "isCreated";
            }
            String actionType = actionTypeStr.toUpperCase();

            if (!"ISCREATED".equals(actionType)) {
                continue;
            }

            ApPurchaseInvoiceAssetDtlEntity entity = new ApPurchaseInvoiceAssetDtlEntity();

            ApPurchaseInvoiceAssetDtlKey key =
                    new ApPurchaseInvoiceAssetDtlKey(transactionPoid, det++);
            entity.setId(key);

            log.debug("Asset key: {}", key);

            entity.setFaCode(adto.getFaCode());
            entity.setFaDescription(adto.getFaDescription());
            entity.setFaCategory(adto.getFaCategory());
            entity.setAssetType(adto.getAssetType());
            entity.setValue(adto.getValue());
            entity.setRemarks(adto.getRemarks());
            entity.setCreatedBy(getCurrentUser());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setLastModifiedBy(getCurrentUser());
            entity.setLastModifiedDate(LocalDateTime.now());

            entities.add(entity);
        }

        apPurchaseInvoiceAssetDtlRepository.saveAll(entities);
    }

    private void saveRjvDetails(Long transactionPoid, ApPurchaseInvoiceHdrDto dto) {

        if (dto.getRjvDtls() == null || dto.getRjvDtls().isEmpty()) {
            return;
        }

        Long nextDetRowId =
                apPurchaseInvRjvDetailsRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);

        long det = (nextDetRowId != null ? nextDetRowId : 0L) + 1L;

        log.info("RJV details start detRowId for transaction {}: {}", transactionPoid, det);

        List<ApPurchaseInvRjvDetailsEntity> entities = new ArrayList<>();

        for (ApPurchaseInvRjvDetailsDto rdto : dto.getRjvDtls()) {
            String actionTypeStr = rdto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "isCreated";
            }
            String actionType = actionTypeStr.toUpperCase();

            if (!"ISCREATED".equals(actionType)) {
                continue;
            }

            ApPurchaseInvRjvDetailsEntity entity = new ApPurchaseInvRjvDetailsEntity();

            ApPurchaseInvRjvDetailsKey key =
                    new ApPurchaseInvRjvDetailsKey(transactionPoid, det++);
            entity.setId(key);

            log.debug("RJV key: {}", key);

            entity.setDrilldownLinkInfo(rdto.getDrilldownLinkInfo());
            entity.setRjvPoid(rdto.getRjvPoid());
            entity.setRjvTrnDate(rdto.getRjvTrnDate());
            entity.setRjvDocRef(rdto.getRjvDocRef());
            entity.setRjvCompanyPoid(rdto.getRjvCompanyPoid());
            entity.setRjvRefType(rdto.getRjvRefType());
            entity.setRjvAmount(rdto.getRjvAmount());
            entity.setRjvRemarks(rdto.getRjvRemarks());
            entity.setRemarks(rdto.getRemarks());

            entity.setCreatedBy(getCurrentUser());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setLastModifiedBy(getCurrentUser());
            entity.setLastModifiedDate(LocalDateTime.now());

            entities.add(entity);
        }

        apPurchaseInvRjvDetailsRepository.saveAll(entities);
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    @Transactional
    public ApPurchaseInvoiceHdrDto updateApPurchaseInvoice(Long transactionPoid, ApPurchaseInvoiceHdrDto apPurchaseInvoiceHdrDto) {
        ApPurchaseInvoiceHdrEntity apPurchaseInvoiceHdrEntity = repository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("ApPurchaseJournal", "transactionPoid", transactionPoid));


        if (apPurchaseInvoiceHdrDto.getTransactionDate() != null)
            apPurchaseInvoiceHdrEntity.setTransactionDate(apPurchaseInvoiceHdrDto.getTransactionDate());
        apPurchaseInvoiceHdrEntity.setGroupPoid(apPurchaseInvoiceHdrDto.getGroupPoid());
        apPurchaseInvoiceHdrEntity.setDocRef(apPurchaseInvoiceHdrDto.getDocRef());
        apPurchaseInvoiceHdrEntity.setPoRef(apPurchaseInvoiceHdrDto.getPoRef());
        apPurchaseInvoiceHdrEntity.setFdaRef(apPurchaseInvoiceHdrDto.getFdaRef());
        apPurchaseInvoiceHdrEntity.setFfRef(apPurchaseInvoiceHdrDto.getFfRef());
        apPurchaseInvoiceHdrEntity.setShipRef(apPurchaseInvoiceHdrDto.getShipRef());
        apPurchaseInvoiceHdrEntity.setCompanyPoid(apPurchaseInvoiceHdrDto.getCompanyPoid());
        apPurchaseInvoiceHdrEntity.setCurrencyCode(apPurchaseInvoiceHdrDto.getCurrencyCode());
        apPurchaseInvoiceHdrEntity.setCurrencyRate(apPurchaseInvoiceHdrDto.getCurrencyRate());
        apPurchaseInvoiceHdrEntity.setSupplierPoid(apPurchaseInvoiceHdrDto.getSupplierPoid());
        apPurchaseInvoiceHdrEntity.setLocationPoid(apPurchaseInvoiceHdrDto.getLocationPoid());
        apPurchaseInvoiceHdrEntity.setSubTotal(apPurchaseInvoiceHdrDto.getSubTotal());
        apPurchaseInvoiceHdrEntity.setDiscount(apPurchaseInvoiceHdrDto.getDiscount());
        apPurchaseInvoiceHdrEntity.setExpenseBySupplier(apPurchaseInvoiceHdrDto.getExpenseBySupplier());
        apPurchaseInvoiceHdrEntity.setGrandTotal(apPurchaseInvoiceHdrDto.getGrandTotal());
        apPurchaseInvoiceHdrEntity.setRemarks(apPurchaseInvoiceHdrDto.getRemarks());
        apPurchaseInvoiceHdrEntity.setLastModifiedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
        apPurchaseInvoiceHdrEntity.setLastModifiedDate(LocalDateTime.now());
        apPurchaseInvoiceHdrEntity.setDeleted(apPurchaseInvoiceHdrDto.getDeleted() != null ? apPurchaseInvoiceHdrDto.getDeleted() : apPurchaseInvoiceHdrEntity.getDeleted());
        apPurchaseInvoiceHdrEntity.setItemTotal(apPurchaseInvoiceHdrDto.getItemTotal());
        apPurchaseInvoiceHdrEntity.setChargeTotal(apPurchaseInvoiceHdrDto.getChargeTotal());
        apPurchaseInvoiceHdrEntity.setGlTotal(apPurchaseInvoiceHdrDto.getGlTotal());
        apPurchaseInvoiceHdrEntity.setType(apPurchaseInvoiceHdrDto.getType());
        apPurchaseInvoiceHdrEntity.setDescription(apPurchaseInvoiceHdrDto.getDescription());
        apPurchaseInvoiceHdrEntity.setCreditPeriod(apPurchaseInvoiceHdrDto.getCreditPeriod());
        apPurchaseInvoiceHdrEntity.setDueDate(apPurchaseInvoiceHdrDto.getDueDate());
        apPurchaseInvoiceHdrEntity.setInvnoOld(apPurchaseInvoiceHdrDto.getInvnoOld());
        apPurchaseInvoiceHdrEntity.setModcodeOld(apPurchaseInvoiceHdrDto.getModcodeOld());
        apPurchaseInvoiceHdrEntity.setRefType(apPurchaseInvoiceHdrDto.getRefType());
        apPurchaseInvoiceHdrEntity.setSalesQtnPoid(apPurchaseInvoiceHdrDto.getSalesQtnPoid());
        apPurchaseInvoiceHdrEntity.setNarration(apPurchaseInvoiceHdrDto.getNarration());
        apPurchaseInvoiceHdrEntity.setSupplierInvDate(apPurchaseInvoiceHdrDto.getSupplierInvDate());
        apPurchaseInvoiceHdrEntity.setSupplierInvNo(apPurchaseInvoiceHdrDto.getSupplierInvNo());
        apPurchaseInvoiceHdrEntity.setSupplierInvRemark(apPurchaseInvoiceHdrDto.getSupplierInvRemark());
        apPurchaseInvoiceHdrEntity.setMtaRef(apPurchaseInvoiceHdrDto.getMtaRef());
        apPurchaseInvoiceHdrEntity.setMultiCompany(apPurchaseInvoiceHdrDto.getMultiCompany());
        apPurchaseInvoiceHdrEntity.setBhdAmount(apPurchaseInvoiceHdrDto.getBhdAmount());
        apPurchaseInvoiceHdrEntity.setSupplierInvAmount(apPurchaseInvoiceHdrDto.getSupplierInvAmount());
        apPurchaseInvoiceHdrEntity.setRoundingAmount(apPurchaseInvoiceHdrDto.getRoundingAmount());
        apPurchaseInvoiceHdrEntity.setBillType(apPurchaseInvoiceHdrDto.getBillType());
        apPurchaseInvoiceHdrEntity.setProvisionalInvoice(apPurchaseInvoiceHdrDto.getProvisionalInvoice());
        apPurchaseInvoiceHdrEntity.setPartyType(apPurchaseInvoiceHdrDto.getPartyType());
        apPurchaseInvoiceHdrEntity.setGrnSupplierPoid(apPurchaseInvoiceHdrDto.getGrnSupplierPoid());
        apPurchaseInvoiceHdrEntity.setPartyTinNumber(apPurchaseInvoiceHdrDto.getPartyTinNumber());
        apPurchaseInvoiceHdrEntity.setPaidAgainst(apPurchaseInvoiceHdrDto.getPaidAgainst());
        apPurchaseInvoiceHdrEntity.setFdaCoveringRef(apPurchaseInvoiceHdrDto.getFdaCoveringRef());

        ApPurchaseInvoiceHdrEntity savedEntity = repository.save(apPurchaseInvoiceHdrEntity);

        String refType = (apPurchaseInvoiceHdrDto.getRefType() == null)
                ? ""
                : apPurchaseInvoiceHdrDto.getRefType().trim().toUpperCase();

        switch (refType) {
            case "GENERAL":
            case "CUSTOM":
            case "GENERAL PO": {
                if (apPurchaseInvoiceHdrDto.getGlDtls() != null && !apPurchaseInvoiceHdrDto.getGlDtls().isEmpty()) {

                    Long maxGlDet = apPurchaseInvoiceGlDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
                    long nextGlDet = (maxGlDet != null ? maxGlDet : 0L) + 1L;

                    List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
                    List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();

                    for (ApPurchaseInvoiceGlDtlDto gdto : apPurchaseInvoiceHdrDto.getGlDtls()) {
                        String actionTypeStr = gdto.getActionType();
                        if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                            actionTypeStr = "isCreated";
                        }
                        String actionType = actionTypeStr.toUpperCase();

                        if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) {
                            continue;
                        }

                        ApPurchaseInvoiceGlDtlEntity glDetails = new ApPurchaseInvoiceGlDtlEntity();
                        Long useDet = (gdto.getDetRowId() != null && gdto.getDetRowId() > 0)
                                ? gdto.getDetRowId()
                                : nextGlDet++;

                        glDetails.setId(new ApPurchaseInvoiceGlDtlKey(transactionPoid, useDet));
                        glDetails.setType(gdto.getType());
                        glDetails.setCompanyPoid(gdto.getCompanyPoid());
                        glDetails.setGlPoid(gdto.getGlPoid());
                        glDetails.setDrAmount(gdto.getDrAmount());
                        glDetails.setCrAmount(gdto.getCrAmount());
                        glDetails.setRefDocId(gdto.getRefDocId());
                        glDetails.setRefDocPoid(gdto.getRefDocPoid());
                        glDetails.setDescription(gdto.getDescription());
                        glDetails.setRemarks(gdto.getRemarks());
                        glDetails.setCreatedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                        glDetails.setCreatedDate(LocalDateTime.now());
                        glDetails.setLastModifiedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                        glDetails.setLastModifiedDate(LocalDateTime.now());
                        glDetails.setJobNoOld(gdto.getJobNoOld());
                        glDetails.setModCodeOld(gdto.getModCodeOld());
                        glDetails.setTaxPoid(gdto.getTaxPoid());
                        glDetails.setTaxPercentage(gdto.getTaxPercentage());
                        glDetails.setTaxAmount(gdto.getTaxAmount());
                        glDetails.setTotalAmount(gdto.getTotalAmount());

                        apPurchaseInvoiceGlDtlRepository.save(glDetails);

                        // Process billwise breakup
                        if (gdto.getBillwiseBreakupList() != null && !gdto.getBillwiseBreakupList().isEmpty()) {
                            for (BillwiseBreakupPopupRequestDto popup : gdto.getBillwiseBreakupList()) {
                                String billActionTypeStr = popup.getActionType();
                                if (billActionTypeStr == null || billActionTypeStr.trim().isEmpty()) {
                                    billActionTypeStr = "isCreated";
                                }
                                String billActionType = billActionTypeStr.toUpperCase();

                                if ("NOCHANGES".equals(billActionType)) {
                                    continue;
                                }

                                BillwiseBreakupRequestDto dto1 = new BillwiseBreakupRequestDto();
                                dto1.setGroupPoid(UserContext.getGroupPoid());
                                dto1.setCompanyPoid(UserContext.getCompanyPoid());
                                dto1.setDocId("200-103");
                                dto1.setTransactionPoid(transactionPoid);
                                dto1.setGlPoid(gdto.getGlPoid());
                                dto1.setMainDetRowId(useDet);
                                dto1.setBillDetRowId(popup.getBillDetRowId());
                                dto1.setBillRefType(popup.getBillRefType());
                                dto1.setBillRef(popup.getBillRef());
                                dto1.setBillDueDate(popup.getBillDueDate());
                                dto1.setDrAmt(popup.getAmount());
                                dto1.setCrAmt(popup.getAmount());
                                dto1.setBillRemarks(popup.getBillRemarks());
                                billwiseList.add(dto1);
                            }
                        }

                        // Process cost center breakup
                        if (gdto.getCostCenterBreakupList() != null && !gdto.getCostCenterBreakupList().isEmpty()) {
                            for (CostCenterBreakupPopupRequestDto popup : gdto.getCostCenterBreakupList()) {
                                String costActionTypeStr = popup.getActionType();
                                if (costActionTypeStr == null || costActionTypeStr.trim().isEmpty()) {
                                    costActionTypeStr = "isCreated";
                                }
                                String costActionType = costActionTypeStr.toUpperCase();

                                if ("NOCHANGES".equals(costActionType)) {
                                    continue;
                                }

                                CostCenterBreakupRequestDto dto2 = new CostCenterBreakupRequestDto();
                                dto2.setGroupPoid(UserContext.getGroupPoid());
                                dto2.setCompanyPoid(UserContext.getCompanyPoid());
                                dto2.setDocId("200-103");
                                dto2.setTransactionPoid(transactionPoid);
                                dto2.setGlPoid(gdto.getGlPoid());
                                dto2.setMainDetRowId(useDet);
                                dto2.setCostDetRowId(popup.getCostDetRowId());
                                dto2.setCostGroup(popup.getCostGroup());
                                dto2.setCostPoid(popup.getCostPoid());
                                dto2.setAmount(popup.getAmount());
                                costCenterList.add(dto2);
                            }
                        }
                    }

                    if (!billwiseList.isEmpty()) {
                        billwiseBreakupService.updateBillwiseBreakups(billwiseList, UserContext.getUserPoid());
                    }
                    if (!costCenterList.isEmpty()) {
                        costCenterBreakupService.updateCostCenterBreakups(costCenterList, UserContext.getUserPoid());
                    }

                    updateBillwiseForGl(transactionPoid, apPurchaseInvoiceHdrDto.getGlDtls(), "200-103");
                    updateCostCenterForGl(transactionPoid, apPurchaseInvoiceHdrDto.getGlDtls(), "200-103");
                }

                break;
            }


            case "FF JOBS":
            case "FDA JOBS": {

                if (apPurchaseInvoiceHdrDto.getChargeDtls() != null
                        && !apPurchaseInvoiceHdrDto.getChargeDtls().isEmpty()) {

                    Long maxChargeDet = purchaseInvoiceChargeDtlRepository
                            .findMaxDetRowIdByTransactionPoid(transactionPoid);

                    long nextChargeDet = (maxChargeDet != null ? maxChargeDet : 0L) + 1L;

                    for (PurchaseInvoiceChargeDtlRequestDto cdto : apPurchaseInvoiceHdrDto.getChargeDtls()) {
                        String actionTypeStr = cdto.getActionType();
                        if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                            actionTypeStr = "isCreated";
                        }
                        String actionType = actionTypeStr.toUpperCase();

                        if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) {
                            continue;
                        }

                        PurchaseInvoiceChargeDtl entity = new PurchaseInvoiceChargeDtl();

                        Long useDet = (cdto.getDetRowId() != null && cdto.getDetRowId() > 0)
                                ? cdto.getDetRowId()
                                : nextChargeDet++;

                        entity.setId(new PurchaseInvoiceChargeDtlId(transactionPoid, useDet));

                        entity.setChargePoid(cdto.getChargePoid());
                        entity.setChargeAmount(cdto.getChargeAmount());
                        entity.setDescription(cdto.getDescription());
                        entity.setRemarks(cdto.getRemarks());
                        entity.setRefDocId(cdto.getRefDocId());
                        entity.setRefDocPoid(cdto.getRefDocPoid());
                        entity.setFdaDetRowId(cdto.getFdaDetRowId());
                        entity.setCheckAll(cdto.getCheckAll());
                        entity.setPdaAmount(cdto.getPdaAmount());
                        entity.setFfAmount(cdto.getFfAmount());
                        entity.setChargeFrom(cdto.getChargeFrom());
                        entity.setTaxPoid(cdto.getTaxPoid());
                        entity.setTaxPercentage(cdto.getTaxPercentage());
                        entity.setTaxAmount(cdto.getTaxAmount());
                        entity.setChargeBaseAmount(cdto.getChargeBaseAmount());
                        entity.setSupplierPoidFf(cdto.getSupplierPoidFf());
                        entity.setLastModifiedBy(getCurrentUser());
                        entity.setLastModifiedDate(LocalDateTime.now());

                        if (cdto.getChargePoid() != null) {
                            entity.setShipChargeMaster(
                                    ShipChargeEntity.builder().chargePoid(cdto.getChargePoid()).build());
                        }
                        purchaseInvoiceChargeDtlRepository.save(entity);
                    }
                }

                break;
            }

            case "MTA PO": {

                if (apPurchaseInvoiceHdrDto.getItemDtls() != null && !apPurchaseInvoiceHdrDto.getItemDtls().isEmpty()) {

                    Long maxItemDet = apPurchaseInvoiceItemDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
                    long nextItemDet = (maxItemDet != null ? maxItemDet : 0L) + 1L;

                    for (ApPurchaseInvoiceItemDtlDto idto : apPurchaseInvoiceHdrDto.getItemDtls()) {
                        String actionTypeStr = idto.getActionType();
                        if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                            actionTypeStr = "isCreated";
                        }
                        String actionType = actionTypeStr.toUpperCase();

                        if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) {
                            continue;
                        }

                        ApPurchaseInvoiceItemDtlEntity item = new ApPurchaseInvoiceItemDtlEntity();

                        Long useDet = (idto.getDetRowId() != null && idto.getDetRowId() > 0)
                                ? idto.getDetRowId()
                                : nextItemDet++;

                        item.setId(new ApPurchaseInvoiceItemDtlKey(transactionPoid, useDet));
                        item.setStockPoid(idto.getStockPoid());
                        item.setStockUnitPoid(idto.getStockUnitPoid());
                        item.setPoQty(idto.getPoQty());
                        item.setDnQty(idto.getDnQty());
                        item.setQtyReceived(idto.getQtyReceived());
                        item.setPrice(idto.getPrice());
                        item.setDiscount(idto.getDiscount());
                        item.setTotal(idto.getTotal());
                        item.setRemarks(idto.getRemarks());
                        item.setCreatedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                        item.setCreatedDate(LocalDateTime.now());
                        item.setLastModifiedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                        item.setLastModifiedDate(LocalDateTime.now());
                        item.setRefDocId(idto.getRefDocId());
                        item.setRefDocPoid(idto.getRefDocPoid());
                        item.setCheckAll(idto.getCheckAll());
                        item.setRefDetRowId(idto.getRefDetRowId());
                        item.setTaxPoid(idto.getTaxPoid());
                        item.setTaxPercentage(idto.getTaxPercentage());
                        item.setTaxAmount(idto.getTaxAmount());
                        item.setAmount(idto.getAmount());
                        item.setBaseAmount(idto.getBaseAmount());

                        apPurchaseInvoiceItemDtlRepository.save(item);
                    }
                }

                break;
            }

            default:
                log.warn("Unknown refType '{}', skipping Item/GL/Charge sections", refType);
                break;
        }

        if (apPurchaseInvoiceHdrDto.getAssetDtls() != null && !apPurchaseInvoiceHdrDto.getAssetDtls().isEmpty()) {

            Long maxAssetDet = apPurchaseInvoiceAssetDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            long nextAssetDet = (maxAssetDet != null ? maxAssetDet : 0L) + 1L;

            for (ApPurchaseInvoiceAssetDtlDto adto : apPurchaseInvoiceHdrDto.getAssetDtls()) {
                String actionTypeStr = adto.getActionType();
                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                    actionTypeStr = "isCreated";
                }
                String actionType = actionTypeStr.toUpperCase();

                if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) {
                    continue;
                }

                ApPurchaseInvoiceAssetDtlEntity asset = new ApPurchaseInvoiceAssetDtlEntity();

                Long useDet = (adto.getDetRowId() != null && adto.getDetRowId() > 0)
                        ? adto.getDetRowId()
                        : nextAssetDet++;

                asset.setId(new ApPurchaseInvoiceAssetDtlKey(transactionPoid, useDet));
                asset.setFaCode(adto.getFaCode());
                asset.setFaDescription(adto.getFaDescription());
                asset.setFaCategory(adto.getFaCategory());
                asset.setAssetType(adto.getAssetType());
                asset.setValue(adto.getValue());
                asset.setRemarks(adto.getRemarks());
                asset.setCreatedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                asset.setCreatedDate(LocalDateTime.now());
                asset.setLastModifiedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                asset.setLastModifiedDate(LocalDateTime.now());

                apPurchaseInvoiceAssetDtlRepository.save(asset);
            }
        }


        if (apPurchaseInvoiceHdrDto.getRjvDtls() != null && !apPurchaseInvoiceHdrDto.getRjvDtls().isEmpty()) {

            Long maxRjvDet = apPurchaseInvRjvDetailsRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            long nextRjvDet = (maxRjvDet != null ? maxRjvDet : 0L) + 1L;

            for (ApPurchaseInvRjvDetailsDto rdto : apPurchaseInvoiceHdrDto.getRjvDtls()) {
                String actionTypeStr = rdto.getActionType();
                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                    actionTypeStr = "isCreated";
                }
                String actionType = actionTypeStr.toUpperCase();

                if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) {
                    continue;
                }

                ApPurchaseInvRjvDetailsEntity rjv = new ApPurchaseInvRjvDetailsEntity();

                Long useDet = (rdto.getDetRowId() != null && rdto.getDetRowId() > 0)
                        ? rdto.getDetRowId()
                        : nextRjvDet++;

                rjv.setId(new ApPurchaseInvRjvDetailsKey(transactionPoid, useDet));
                rjv.setDrilldownLinkInfo(rdto.getDrilldownLinkInfo());
                rjv.setRjvPoid(rdto.getRjvPoid());
                rjv.setRjvTrnDate(rdto.getRjvTrnDate());
                rjv.setRjvDocRef(rdto.getRjvDocRef());
                rjv.setRjvCompanyPoid(rdto.getRjvCompanyPoid());
                rjv.setRjvRefType(rdto.getRjvRefType());
                rjv.setRjvAmount(rdto.getRjvAmount());
                rjv.setRjvRemarks(rdto.getRjvRemarks());
                rjv.setRemarks(rdto.getRemarks());
                rjv.setCreatedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                rjv.setCreatedDate(LocalDateTime.now());
                rjv.setLastModifiedBy(apPurchaseInvoiceHdrDto.getLastModifiedBy());
                rjv.setLastModifiedDate(LocalDateTime.now());

                apPurchaseInvRjvDetailsRepository.save(rjv);
            }
        }

        return fetchApPurchaseInvoiceHdr(savedEntity.getTransactionPoid());
    }

    @Override
    @Transactional
    public ApPurchaseInvoiceHdrDto softDeleteApPurchaseInvoice(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        ApPurchaseInvoiceHdrEntity existing = repository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("ApPurchaseJournal", "transactionPoid", transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "AP_PURCHASE_INVOICE_HDR",
                "TRANSACTION_POID",
                deleteReasonDto.getDeleteReason(),
                existing.getTransactionDate()
        );

        return fetchApPurchaseInvoiceHdr(transactionPoid);
    }

    @Override
    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        java.util.List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "TRANSACTION_POID",
                "DOC_REF");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    public List<ApPurchaseJournalResponseDto> createFromFf(String ffPoid, StringBuilder result) {
        return apPurchaseJournalRepositoryImpl.createFromFf(
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                ffPoid,
                result);
    }

    @Override
    @Transactional
    public String updateFfCost(String ffPoid, Long piPoid) {

        log.info("Updating FF Cost for FF_POID={}, PI_POID={}", ffPoid, piPoid);

        String result = apPurchaseJournalRepositoryImpl.updateFfCost(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                ffPoid,
                piPoid
        );

        log.info("FF Cost update result: {}", result);

        return result;
    }

    @Override
    public String updateFdaCost(String fdaPoid, Long piPoid) {
        log.info("Updating FDA Cost for FDA_POID={}, PI_POID={}", fdaPoid, piPoid);
        String result = apPurchaseJournalRepositoryImpl.updateFdaCost(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                fdaPoid,
                piPoid);

        log.info("FDA Cost update result: {}", result);

        return result;

    }


    public List<ApPurchaseJournalResponseDto> createFromFda(String fdaPoid, StringBuilder result) {
        return apPurchaseJournalRepositoryImpl.createFromFda(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                fdaPoid,
                result);
    }

    // -----------------------------------------
    // BILLWISE BREAKUP METHODS
    // -----------------------------------------


    private void updateBillwiseForGl(Long transactionPoid, List<ApPurchaseInvoiceGlDtlDto> glDetails, String docId) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        List<BillwiseBreakupRequestDto> billwiseList = buildBillwiseBreakupList(transactionPoid, glDetails, docId);

        if (!billwiseList.isEmpty()) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseList, UserContext.getUserPoid());
        }
    }

    private List<BillwiseBreakupRequestDto> buildBillwiseBreakupList(Long transactionPoid, List<ApPurchaseInvoiceGlDtlDto> glDetails, String docId) {
        List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();

        for (ApPurchaseInvoiceGlDtlDto dtl : glDetails) {
            if (dtl.getBillwiseBreakupList() != null && !dtl.getBillwiseBreakupList().isEmpty()) {
                for (BillwiseBreakupPopupRequestDto popup : dtl.getBillwiseBreakupList()) {
                    BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();

                    dto.setGroupPoid(UserContext.getGroupPoid());
                    dto.setCompanyPoid(UserContext.getCompanyPoid());
                    dto.setDocId(docId);
                    dto.setTransactionPoid(transactionPoid);
                    dto.setGlPoid(dtl.getGlPoid());

                    // GL → Billwise mapping: detRowId → billDetRowId and mainDetRowId
                    dto.setMainDetRowId(dtl.getDetRowId());
                    dto.setBillDetRowId(popup.getBillDetRowId());


                    dto.setBillRefType(popup.getBillRefType());
                    dto.setBillRef(popup.getBillRef());
                    dto.setBillDueDate(popup.getBillDueDate());
                    BigDecimal amount = popup.getAmount() == null ? BigDecimal.ZERO : popup.getAmount();
                    String type = popup.getType();

                    if (type == null) {

                        dto.setDrAmt(BigDecimal.ZERO);
                        dto.setCrAmt(amount);

                    } else if ("DR".equalsIgnoreCase(type)) {

                        dto.setDrAmt(amount);
                        dto.setCrAmt(BigDecimal.ZERO);

                    } else if ("CR".equalsIgnoreCase(type)) {

                        dto.setDrAmt(BigDecimal.ZERO);
                        dto.setCrAmt(amount);

                    } else {
                        // Any other type: try numeric parse, otherwise move to CR by default
                        try {
                            dto.setDrAmt(new BigDecimal(type));  // only if numeric
                            dto.setCrAmt(amount);
                        } catch (Exception e) {
                            dto.setDrAmt(BigDecimal.ZERO);
                            dto.setCrAmt(amount);
                        }
                    }

                    dto.setBillRemarks(popup.getBillRemarks());

                    billwiseList.add(dto);
                }
            }
        }

        return billwiseList;
    }

    private void updateCostCenterForGl(Long transactionPoid, List<ApPurchaseInvoiceGlDtlDto> glDetails, String docId) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        List<CostCenterBreakupRequestDto> costCenterList = buildCostCenterBreakupList(transactionPoid, glDetails, docId);

        if (!costCenterList.isEmpty()) {
            costCenterBreakupService.updateCostCenterBreakups(costCenterList, UserContext.getUserPoid());
        }
    }

    private List<CostCenterBreakupRequestDto> buildCostCenterBreakupList(Long transactionPoid, List<ApPurchaseInvoiceGlDtlDto> glDetails, String docId) {
        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();

        for (ApPurchaseInvoiceGlDtlDto dtl : glDetails) {
            if (dtl.getCostCenterBreakupList() != null && !dtl.getCostCenterBreakupList().isEmpty()) {
                for (CostCenterBreakupPopupRequestDto popup : dtl.getCostCenterBreakupList()) {
                    CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();

                    dto.setGroupPoid(UserContext.getGroupPoid());
                    dto.setCompanyPoid(UserContext.getCompanyPoid());
                    dto.setDocId(docId);
                    dto.setTransactionPoid(transactionPoid);
                    dto.setGlPoid(dtl.getGlPoid());

                    // GL → Cost center mapping: detRowId → costDetRowId and mainDetRowId
                    dto.setMainDetRowId(dtl.getDetRowId());
                    dto.setCostDetRowId(popup.getCostDetRowId());


                    dto.setCostGroup(popup.getCostGroup());
                    dto.setCostPoid(popup.getCostPoid());
                    dto.setAmount(BigDecimal.valueOf(popup.getAmount() != null ? popup.getAmount().longValue() : 0L));

                    costCenterList.add(dto);
                }
            }
        }

        return costCenterList;
    }

    // -----------------------------------------
    // LOAD BILLWISE AND COST CENTER BREAKUP
    // -----------------------------------------

    private void loadBillwiseAndCostCenterBreakup(List<ApPurchaseInvoiceGlDtlDto> glDetailDtos, Long transactionPoid, ApPurchaseInvoiceHdrEntity header) {
        if (glDetailDtos == null || glDetailDtos.isEmpty()) {
            return;
        }

        Long groupPoid = header.getGroupPoid();
        Long companyPoid = header.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        String docId = "200-103";

        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = null;
        GlVoucherCostCenterBreakupResponseDto costCenterResponse = null;

        try {
            billwiseResponse = billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, docId, transactionPoid);
        } catch (Exception e) {
            log.warn("Error loading billwise breakup for transactionPoid {}: {}", transactionPoid, e.getMessage());
        }

        try {
            costCenterResponse = costCenterBreakupService.loadCostCenterData(docId, transactionPoid, groupPoid, companyPoid, userPoid);
        } catch (Exception e) {
            log.warn("Error loading cost center breakup for transactionPoid {}: {}", transactionPoid, e.getMessage());
        }

        for (ApPurchaseInvoiceGlDtlDto glDto : glDetailDtos) {
            Long detRowId = glDto.getDetRowId();

            // Load billwise breakup
            if (billwiseResponse != null && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                List<BillwiseBreakupPopupRequestDto> billwiseList = billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                        .filter(item -> item.getMainDetRowId() != null && item.getMainDetRowId().equals(detRowId))
                        .map(item -> {
                            BillwiseBreakupPopupRequestDto popupDto = new BillwiseBreakupPopupRequestDto();
                            popupDto.setBillDetRowId(item.getBillDetRowId());
                            popupDto.setBillRefType(item.getBillRefType());
                            popupDto.setBillRef(item.getBillRef());
                            popupDto.setBillDueDate(item.getBillDueDate());

                            // Determine type and amount from drAmt/crAmt
                            if (item.getDrAmt() != null && item.getDrAmt().compareTo(BigDecimal.ZERO) > 0) {
                                popupDto.setType("DR");
                                popupDto.setAmount(item.getDrAmt());
                            } else if (item.getCrAmt() != null && item.getCrAmt().compareTo(BigDecimal.ZERO) > 0) {
                                popupDto.setType("CR");
                                popupDto.setAmount(item.getCrAmt());
                            }

                            popupDto.setBillRemarks(item.getBillRemarks());
                            return popupDto;
                        })
                        .collect(Collectors.toList());
                glDto.setBillwiseBreakupList(billwiseList);
            } else {
                glDto.setBillwiseBreakupList(Collections.emptyList());
            }

            // Load cost center breakup
            if (costCenterResponse != null && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                List<CostCenterBreakupPopupRequestDto> costCenterList = costCenterResponse.getCostBreakupList().stream()
                        .filter(item -> item.getMainDetRowId() != null && item.getMainDetRowId().equals(detRowId))
                        .map(item -> {
                            CostCenterBreakupPopupRequestDto popupDto = new CostCenterBreakupPopupRequestDto();
                            popupDto.setCostDetRowId(item.getCostDetRowId());
                            popupDto.setCostGroup(item.getCostGroup());
                            popupDto.setCostPoid(item.getCostPoid());
                            popupDto.setAmount(BigDecimal.valueOf(item.getAmount()));
                            return popupDto;
                        })
                        .collect(Collectors.toList());
                glDto.setCostCenterBreakupList(costCenterList);
            } else {
                glDto.setCostCenterBreakupList(Collections.emptyList());
            }
        }
    }

    @Override
    public String validateVoucher(
            String docId,
            String refType,
            String refPoid
    ) {

        log.info("Validating Voucher → DOC_ID={}, REF_TYPE={}, REF_POID={}", docId, refType, refPoid);

        String result = apPurchaseJournalRepositoryImpl.validateVoucher(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                docId,
                refType,
                refPoid
        );

        log.info("Voucher Validation Result → {}", result);

        return result;
    }

    @Override
    public String validateBeforeSave(
            String docId,
            String refType,
            String refPoid
    ) {

        log.info("Validating BEFORE SAVE → docId={}, refType={}, refPoid={}",
                docId, refType, refPoid);

        String result = apPurchaseJournalRepositoryImpl.validateBeforeSave(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                docId,
                refType,
                refPoid
        );

        log.info("Before-Save Validation Result → {}", result);

        return result;
    }

    @Override
    public String updateMtaPoBookingDetails(
            String poPoid,
            Long bookPoid
    ) {

        log.info("Updating MTA PO Booking Details → PO_POID={}, BOOK_POID={}", poPoid, bookPoid);

        String result = apPurchaseJournalRepositoryImpl.updateMtaPoBookingDetails(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                poPoid,
                bookPoid
        );


        log.info("Update result → {}", result);

        return result;
    }

    @Override
    public List<ApPiFromPoResponseDto> createPiFromPo(String poPoid) {

        StringBuilder result = new StringBuilder();

        List<ApPiFromPoResponseDto> list =
                apPurchaseJournalRepositoryImpl.createPiFromPo(
                        UserContext.getGroupPoid(),
                        UserContext.getUserPoid(),
                        UserContext.getCompanyPoid(),
                        poPoid, result);

        log.info("Result from PROC_AP_PI_CREATE_FROM_PO → {}", result);
        return list;
    }

    @Override
    @Transactional
    public List<ApPiFromGeneralPoResponseDto> createPiFromGeneralPo(String poPoid) {

        StringBuilder result = new StringBuilder();

        List<ApPiFromGeneralPoResponseDto> list =
                apPurchaseJournalRepositoryImpl.createPiFromGeneralPo(
                        UserContext.getGroupPoid(),
                        UserContext.getUserPoid(),
                        UserContext.getCompanyPoid(),
                        poPoid, result);

        log.info("General PO PI Creation Result → {}", result);

        return list;
    }

    @Override
    @Transactional()
    public List<ApPiFaDefaultDetailsDto> getFaDefaultDetails(String faPoid) {

        log.info("Fetching FA default details for FA_POID={}", faPoid);

        List<ApPiFaDefaultDetailsDto> list =
                apPurchaseJournalRepositoryImpl.getFaDefaultDetails(
                        UserContext.getGroupPoid(),
                        UserContext.getUserPoid(),
                        UserContext.getCompanyPoid(),
                        faPoid);

        return list;
    }

    
    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "200-103");
        params.put("SUBREPORT_GL", printService.load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml"));
        params.put("SUBREPORT_CHARGE", printService.load("Finance/AP/PurchaseInvoiceChargeSubReport.jrxml"));
        JasperReport mainReport = printService.load("Finance/AP/PurchaseInvoiceReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}
