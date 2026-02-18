package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.projection.CurrencyRateProjection;
import com.asg.finance.repository.*;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlJournalFaCapitalization;
import com.asg.finance.entity.GlJournalVoucherAssetDtl;
import com.asg.finance.entity.GlJournalVoucherDtl;
import com.asg.finance.entity.GlJournalVoucherHdr;

import com.asg.finance.entity.key.TransactionDetailKey;
import com.asg.finance.entity.master.FixedAsset;
import com.asg.finance.repository.master.FixedAssetRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.JournalVoucherService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.*;
import static com.asg.finance.utility.Constants.*;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalVoucherServiceImpl implements JournalVoucherService {


    public static final String REF_TYPE_GENERAL = "GENERAL";
    public static final String REF_TYPE_ASSET_DISPOSAL = "ASSET_DISPOSAL";
    public static final String REF_TYPE_ASSET_CAPITALIZATION = "ASSET_CAPITALIZATION";
    public static final String STATUS_POSTED = "POSTED";


    public static final String PROCESS_SCRAP = "SCRAP";
    public static final String PROCESS_SOLD = "SOLD";
    public static final String PROCESS_OBSOLETE = "OBSOLETE";
    public static final String PROCESS_NOT_APPLICABLE = "Not Applicable";

    private final GlJournalVoucherHdrRepository glJournalVoucherHdrRepository;
    private final GlJournalVoucherDtlRepository glJournalVoucherDtlRepository;
    private final GlJournalVoucherAssetDtlRepository glJournalVoucherAssetDtlRepository;
    private final GlJournalFaCapitalizationRepository glJournalFaCapitalizationRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final GLMasterRepository glMasterRepository;
    private final CostCenterBreakupService costCenterBreakupService;
    private final BillwiseBreakupService billwiseBreakupService;
    private final LovDataService lovDataService;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;

    @Override
    @Transactional
    public JournalVoucherResponse createJournalVoucher(JournalVoucherRequest request,String docId) {
        log.info("Creating Journal Voucher - RefType: {}, TransactionDate: {}, Amount: {}", 
                request.getRefType(), request.getTransactionDate(), request.getAmount());
        
        try {
            validateJournalVoucher(request);

        String currentUser = getCurrentUser();
        boolean isMultiCompany = Boolean.TRUE.equals(request.getMultiCompany());

        BigDecimal bhdAmount = request.getBhdAmount() != null ? request.getBhdAmount() : 
                (request.getCurrencyRate() != null ? request.getAmount().multiply(request.getCurrencyRate()) : request.getAmount());

        GlJournalVoucherHdr header = GlJournalVoucherHdr.builder()
                .transactionDate(request.getTransactionDate())
                .groupPoid(getGroupId())
                .companyPoid(getCompanyId())
                .refType(request.getRefType())
                .currencyCode(request.getCurrencyCode())
                .currencyRate(request.getCurrencyRate())
                .amount(request.getAmount())
                .bhdAmount(bhdAmount)
                .postingNarration(request.getPostingNarration())
                .wdvAccountGl(request.getWdvAccountGl())
                .multiCompany(isMultiCompany ? FLAG_YES : FLAG_NO)
                .remarks(request.getRemarks())
                .confidentialRemarks(request.getConfidentialRemarks())
                .deleted(FLAG_NO)
                .createdBy(currentUser)
                .createdDate(LocalDateTime.now())
                .build();

        header = glJournalVoucherHdrRepository.save(header);
        log.info("Journal Voucher header saved - TransactionPoid: {}, DocRef: {}", 
                header.getTransactionPoid(), header.getDocRef());

        if (REF_TYPE_GENERAL.equalsIgnoreCase(request.getRefType()) && request.getGlDetails() != null) {
            log.debug("Saving {} GL detail lines", request.getGlDetails().size());
            saveGlDetails(header, request.getGlDetails(), isMultiCompany,docId);
        } else if (REF_TYPE_ASSET_DISPOSAL.equalsIgnoreCase(request.getRefType()) && request.getAssetDetails() != null) {
            log.debug("Saving {} asset disposal detail lines", request.getAssetDetails().size());
            saveAssetDetails(header.getTransactionPoid(), request.getAssetDetails());
        } else if (REF_TYPE_ASSET_CAPITALIZATION.equalsIgnoreCase(request.getRefType()) && request.getAssetCapitalization() != null) {
            log.debug("Saving {} asset capitalization detail lines", request.getAssetCapitalization().size());
            saveCapitalizationDetails(header.getTransactionPoid(), request.getAssetCapitalization());
            saveGlDetails(header, request.getGlDetails(), isMultiCompany,docId);
        }

        log.info("Journal Voucher created successfully - TransactionPoid: {}, DocRef: {}, RefType: {}", 
                header.getTransactionPoid(), header.getDocRef(), request.getRefType());
        
        // Log the creation
        String key = header.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);
        
        return JournalVoucherResponse.builder()
                .transactionPoid(header.getTransactionPoid())
                .docRef(header.getDocRef())
                .message("Journal Voucher created successfully")
                .build();
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating Journal Voucher - RefType: {}, Error: {}", 
                    request.getRefType(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating Journal Voucher - RefType: {}, Error: {}", 
                    request.getRefType(), e.getMessage(), e);
            throw new RuntimeException("Failed to create journal voucher: " + e.getMessage(), e);
        }
    }

    private void validateJournalVoucher(JournalVoucherRequest request) {
        String refType = request.getRefType();
        if (refType == null || refType.isBlank()) {
            throw new IllegalArgumentException("RefType is mandatory");
        }
        if (!REF_TYPE_GENERAL.equalsIgnoreCase(refType) && !REF_TYPE_ASSET_DISPOSAL.equalsIgnoreCase(refType) && !REF_TYPE_ASSET_CAPITALIZATION.equalsIgnoreCase(refType)) {
            throw new IllegalArgumentException("RefType must be GENERAL, ASSET_DISPOSAL, or ASSET_CAPITALIZATION");
        }

        if (request.getTransactionDate() == null) {
            throw new IllegalArgumentException("Transaction date is mandatory");
        }
        if (request.getTransactionDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount is mandatory and must be greater than 0");
        }

        if (request.getPostingNarration() == null || request.getPostingNarration().trim().isEmpty()) {
            throw new IllegalArgumentException("Posting Narration is mandatory");
        }

        if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
            if (request.getCurrencyRate() == null || request.getCurrencyRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Currency rate must be greater than 0 for foreign currency");
            }
        }

        try {
            if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
                lovDataService.getDetailsByCodeAndLovName(request.getCurrencyCode(), "CURRENCY");
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Currency","Code",request.getCurrencyCode());
        }

        switch (refType) {
            case REF_TYPE_GENERAL -> {
                if (request.getGlDetails() == null || request.getGlDetails().isEmpty()) {
                    throw new IllegalArgumentException("At least one GL detail line is required for GENERAL type");
                }
                validateGlDetails(request.getGlDetails(), Boolean.TRUE.equals(request.getMultiCompany()));
                validateDebitCreditBalance(request.getGlDetails());
            }
            case REF_TYPE_ASSET_DISPOSAL -> {
                if (request.getWdvAccountGl() == null) {
                    log.error("WDV Account GL is required for ASSET_DISPOSAL");
                    throw new IllegalArgumentException("WDV Account GL is required for ASSET_DISPOSAL type");
                }
                if (request.getAssetDetails() == null || request.getAssetDetails().isEmpty()) {
                    log.error("At least one asset detail line is required for ASSET_DISPOSAL");
                    throw new IllegalArgumentException("At least one asset detail line is required for ASSET_DISPOSAL type");
                }
                validateAssetDetailsForDisposal(request.getAssetDetails());
            }
            case REF_TYPE_ASSET_CAPITALIZATION -> {
                if (request.getAssetCapitalization() == null || request.getAssetCapitalization().isEmpty()) {
                    log.error("At least one asset capitalization line is required for ASSET_CAPITALIZATION");
                    throw new IllegalArgumentException("At least one asset capitalization line is required for ASSET_CAPITALIZATION type");
                }
                validateAssetDetailsForCapitalization(request.getAssetCapitalization());
            }
        }
    }

    private void validateGlDetails(List<JournalVoucherGlDetailDto> glDetails, boolean multiCompany) {
        for (JournalVoucherGlDetailDto detail : glDetails) {
            if (!TYPE_DEBIT.equalsIgnoreCase(detail.getType()) && !TYPE_CREDIT.equalsIgnoreCase(detail.getType())) {
                throw new IllegalArgumentException("Type must be 'Dr' or 'Cr'");
            }


            if (TYPE_DEBIT.equalsIgnoreCase(detail.getType())) {
                if (detail.getDrAmt() == null || detail.getDrAmt().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("DrAmt must be greater than 0 for Type=Dr");
                }
                if (detail.getCrAmt() != null && detail.getCrAmt().compareTo(BigDecimal.ZERO) != 0) {
                    throw new IllegalArgumentException("CrAmt must be 0 or null for Type=Dr");
                }
            } else {
                if (detail.getCrAmt() == null || detail.getCrAmt().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("CrAmt must be greater than 0 for Type=Cr");
                }
                if (detail.getDrAmt() != null && detail.getDrAmt().compareTo(BigDecimal.ZERO) != 0) {
                    throw new IllegalArgumentException("DrAmt must be 0 or null for Type=Cr");
                }
            }

            if (multiCompany && detail.getCompanyPoid() == null) {
                throw new IllegalArgumentException("CompanyPoid is mandatory for GL detail when multiCompany is true");
            }

            if (detail.getGlPoid() == null) {
                throw new IllegalArgumentException("GlPoid is mandatory for GL detail");
            }
        }
    }

    private void validateAssetDetailsForDisposal(List<JournalVoucherAssetDetailDto> assetDetails) {
        for (JournalVoucherAssetDetailDto detail : assetDetails) {
            if (detail.getFaPoid() == null) {
                throw new IllegalArgumentException("FaPoid is mandatory for asset disposal");
            }
            if (!fixedAssetRepository.existsById(detail.getFaPoid())) {
                throw new IllegalArgumentException("Invalid asset: " + detail.getFaPoid());
            }
            try {
                glJournalVoucherHdrRepository.fetchAssetDepreciationDetails(detail.getFaPoid());
            } catch (Exception e) {
                throw new IllegalArgumentException("Asset not available for disposal: " + detail.getFaPoid());
            }
        }
    }

    private void validateAssetDetailsForCapitalization(List<JournalVoucherCapitalizationDto> capitalizationDetails) {
        for (JournalVoucherCapitalizationDto detail : capitalizationDetails) {
            if (detail.getFaPoid() == null) {
                throw new IllegalArgumentException("FaPoid is mandatory for asset capitalization");
            }
            if (!fixedAssetRepository.existsById(detail.getFaPoid())) {
                throw new IllegalArgumentException("Invalid asset: " + detail.getFaPoid());
            }
            try {
                glJournalVoucherHdrRepository.fetchFixedAssetDetails(detail.getFaPoid());
            } catch (Exception e) {
                throw new IllegalArgumentException("Asset not valid for capitalization: " + detail.getFaPoid());
            }
        }
    }

    private void validateDebitCreditBalance(List<JournalVoucherGlDetailDto> glDetails) {
        log.debug("Validating debit/credit balance for {} GL details", glDetails.size());
        BigDecimal totalDr = glDetails.stream()
                .map(d -> d.getDrAmt() != null ? d.getDrAmt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCr = glDetails.stream()
                .map(d -> d.getCrAmt() != null ? d.getCrAmt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDr.compareTo(totalCr) != 0) {
            log.error("Debit/Credit mismatch - DrTotal: {}, CrTotal: {}, Difference: {}", 
                    totalDr, totalCr, totalDr.subtract(totalCr));
            throw new IllegalArgumentException("Debit total must equal Credit total");
        }
    }

    private void saveGlDetails(GlJournalVoucherHdr header, List<JournalVoucherGlDetailDto> glDetails, boolean multiCompany,String docId) {
        Long transactionPoid = header.getTransactionPoid();
        Long headerCompanyPoid = header.getCompanyPoid();
        String currentUser = getCurrentUser();

        List<CostCenterBreakupRequestDto> costCenterRequestDtoList = new ArrayList<>();
        List<BillwiseBreakupRequestDto> billwiseRequestDtoList = new ArrayList<>();
        List<LogRequestDto<GlJournalVoucherDtl>> logRequests = new ArrayList<>();

        for (JournalVoucherGlDetailDto dto : glDetails) {

            String rawAction = dto.getActionType();
            String action = (rawAction == null || rawAction.trim().isEmpty())
                    ? "NOCHANGES"
                    : rawAction.trim().toUpperCase();

            action = switch (action) {
                case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                case "ISDELETED", "DELETED" -> "ISDELETED";
                default -> "NOCHANGES";
            };

            Long companyPoid = multiCompany ? dto.getCompanyPoid() : headerCompanyPoid;

            switch (action) {
                case ACTION_NOCHANGES -> {}
                case ACTION_ISDELETED -> {
                    if (dto.getDetRowId() != null) {
                        glJournalVoucherDtlRepository.deleteById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()));
                        loggingService.logDelete(dto, docId, transactionPoid.toString());
                    }
                }
                case ACTION_ISCREATED -> {
                   validateGLPoid(dto.getGlPoid());
                    Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : getNextDetRowIdForGl(transactionPoid);
                    GlJournalVoucherDtl detail = GlJournalVoucherDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(detRowId)
                            .type(dto.getType())
                            .companyPoid(companyPoid)
                            .glPoid(dto.getGlPoid())
                            .drAmt(dto.getDrAmt())
                            .crAmt(dto.getCrAmt())
                            .remarks(dto.getRemarks())
                            .createdBy(currentUser)
                            .createdDate(LocalDateTime.now())
                            .build();
                    glJournalVoucherDtlRepository.save(detail);
                    
                    String logDetail = String.format("Row Created on Journal Voucher GL Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);

                    if (dto.getCostCenterBreakup() != null && !dto.getCostCenterBreakup().isEmpty()) {
                        costCenterRequestDtoList.addAll(buildCostCenterBreakups(transactionPoid, detRowId, docId, dto.getGlPoid(), dto.getCostCenterBreakup()));
                    }
                    if (dto.getBillWiseBreakup() != null && !dto.getBillWiseBreakup().isEmpty()) {
                        billwiseRequestDtoList.addAll(buildBillwiseBreakups(transactionPoid, detRowId, docId, dto.getGlPoid(), dto.getBillWiseBreakup()));
                    }
                }
                case ACTION_ISUPDATED -> {
                    if (!glMasterRepository.existsByGlPoid(dto.getGlPoid())) {
                        throw new ResourceNotFoundException("Gl Master", "glPoid", dto.getGlPoid());
                    }
                    GlJournalVoucherDtl detail = glJournalVoucherDtlRepository.findById(
                            new TransactionDetailKey(transactionPoid, dto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("GL Detail", "detRowId", dto.getDetRowId()));
                    
                    GlJournalVoucherDtl oldDetail = new GlJournalVoucherDtl();
                    BeanUtils.copyProperties(detail, oldDetail);
                    
                    detail.setType(dto.getType());
                    detail.setCompanyPoid(companyPoid);
                    detail.setGlPoid(dto.getGlPoid());
                    detail.setDrAmt(dto.getDrAmt());
                    detail.setCrAmt(dto.getCrAmt());
                    detail.setRemarks(dto.getRemarks());
                    detail.setLastModifiedBy(currentUser);
                    detail.setLastModifiedDate(LocalDateTime.now());
                    glJournalVoucherDtlRepository.save(detail);
                    
                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldDetail, detail, GlJournalVoucherDtl.class, docId, transactionPoid.toString(), logDetail));

                    if (dto.getCostCenterBreakup() != null && !dto.getCostCenterBreakup().isEmpty()) {
                        costCenterRequestDtoList.addAll(buildCostCenterBreakups(transactionPoid, dto.getDetRowId(), docId, dto.getGlPoid(), dto.getCostCenterBreakup()));
                    }
                    if (dto.getBillWiseBreakup() != null && !dto.getBillWiseBreakup().isEmpty()) {
                        billwiseRequestDtoList.addAll(buildBillwiseBreakups(transactionPoid, dto.getDetRowId(), docId, dto.getGlPoid(), dto.getBillWiseBreakup()));
                    }
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        if (!costCenterRequestDtoList.isEmpty()) {
            costCenterBreakupService.updateCostCenterBreakups(costCenterRequestDtoList, getUserPoid());
        }
        if (!billwiseRequestDtoList.isEmpty()) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseRequestDtoList, getUserPoid());
        }
    }

    private Long getNextDetRowIdForGl(Long transactionPoid) {
        Long maxId = glJournalVoucherDtlRepository.findAll((root, query, cb) -> 
                cb.equal(root.get("transactionPoid"), transactionPoid))
                .stream()
                .map(GlJournalVoucherDtl::getDetRowId)
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
                        .drAmt("Dr".equalsIgnoreCase(dto.getType()) ? dto.getAmount() : BigDecimal.ZERO)
                        .crAmt("Cr".equalsIgnoreCase(dto.getType()) ? dto.getAmount() : BigDecimal.ZERO)
                        .billRemarks(dto.getBillRemarks())
                        .loginUserPoid(getUserPoid())
                        .build())
                .collect(Collectors.toList());
    }

    private void saveAssetDetails(Long transactionPoid, List<JournalVoucherAssetDetailDto> assetDetails) {
        String docId = UserContext.getDocumentId();
        List<GlJournalVoucherAssetDtl> toSave = new ArrayList<>();
        List<GlJournalVoucherAssetDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlJournalVoucherAssetDtl>> logRequests = new ArrayList<>();
        
        List<GlJournalVoucherAssetDtl> existingList = glJournalVoucherAssetDtlRepository.findAll((root, query, cb) -> 
                cb.equal(root.get("transactionPoid"), transactionPoid));
        Map<Long, GlJournalVoucherAssetDtl> existingMap = existingList.stream()
                .collect(Collectors.toMap(GlJournalVoucherAssetDtl::getDetRowId, d -> d));
        
        Long[] maxSn = {existingList.stream()
                .map(GlJournalVoucherAssetDtl::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L)};
        
        for (JournalVoucherAssetDetailDto dto : assetDetails) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : "ISCREATED";
            switch (action) {
                case "ISCREATED":
                    Long sn = ++maxSn[0];
                    GlJournalVoucherAssetDtl newDetail = GlJournalVoucherAssetDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(sn)
                            .faPoid(dto.getFaPoid())
                            .lifeYear(dto.getLifeYear())
                            .purchaseDate(dto.getPurchaseDate())
                            .depreciationStartDate(dto.getDepreciationStartDate())
                            .assetValue(dto.getAssetValue())
                            .depreciatedAmt(dto.getDepreciatedAmt())
                            .wdvValue(dto.getWdvValue())
                            .process(dto.getProcess())
                            .scrapSoldDate(dto.getScrapSoldDate())
                            .scrapSoldValue(dto.getScrapSoldValue())
                            .remarks(dto.getRemarks())
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .build();
                    toSave.add(newDetail);
                    break;
                case "ISUPDATED":
                    GlJournalVoucherAssetDtl existing = existingMap.get(dto.getSn());
                    if (existing == null) {
                        throw new ResourceNotFoundException("Asset Detail", "sn", dto.getSn());
                    }
                    GlJournalVoucherAssetDtl oldDetail = new GlJournalVoucherAssetDtl();
                    BeanUtils.copyProperties(existing, oldDetail);
                    existing.setFaPoid(dto.getFaPoid());
                    existing.setLifeYear(dto.getLifeYear());
                    existing.setPurchaseDate(dto.getPurchaseDate());
                    existing.setDepreciationStartDate(dto.getDepreciationStartDate());
                    existing.setAssetValue(dto.getAssetValue());
                    existing.setDepreciatedAmt(dto.getDepreciatedAmt());
                    existing.setWdvValue(dto.getWdvValue());
                    existing.setProcess(dto.getProcess());
                    existing.setScrapSoldDate(dto.getScrapSoldDate());
                    existing.setScrapSoldValue(dto.getScrapSoldValue());
                    existing.setRemarks(dto.getRemarks());
                    existing.setLastModifiedBy(getCurrentUser());
                    existing.setLastModifiedDate(LocalDateTime.now());
                    toUpdate.add(existing);
                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dto.getSn());
                    logRequests.add(new LogRequestDto<>(oldDetail, existing, GlJournalVoucherAssetDtl.class, docId, transactionPoid.toString(), logDetail));
                    break;
                case "ISDELETED":
                    toDelete.add(dto.getSn());
                    loggingService.logDelete(dto, docId, transactionPoid.toString());
                    break;
            }
        }
        
        if (!toSave.isEmpty()) {
            List<GlJournalVoucherAssetDtl> saved = glJournalVoucherAssetDtlRepository.saveAll(toSave);
            saved.forEach(detail -> {
                String logDetail = String.format("Row Created on Journal Voucher Asset Detail with sn: %s", detail.getDetRowId());
                loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            glJournalVoucherAssetDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            existingList.stream()
                    .filter(d -> toDelete.contains(d.getDetRowId()))
                    .forEach(glJournalVoucherAssetDtlRepository::delete);
        }
    }

    private void saveCapitalizationDetails(Long transactionPoid, List<JournalVoucherCapitalizationDto> capitalizationDetails) {
        String docId = UserContext.getDocumentId();
        List<GlJournalFaCapitalization> toSave = new ArrayList<>();
        List<GlJournalFaCapitalization> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlJournalFaCapitalization>> logRequests = new ArrayList<>();
        
        List<GlJournalFaCapitalization> existingList = glJournalFaCapitalizationRepository.findAll((root, query, cb) -> 
                cb.equal(root.get("transactionPoid"), transactionPoid));
        Map<Long, GlJournalFaCapitalization> existingMap = existingList.stream()
                .collect(Collectors.toMap(GlJournalFaCapitalization::getDetRowId, d -> d));
        
        Long[] maxSn = {existingList.stream()
                .map(GlJournalFaCapitalization::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L)};
        
        for (JournalVoucherCapitalizationDto dto : capitalizationDetails) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : "ISCREATED";
            switch (action) {
                case "ISCREATED":
                    Long sn = ++maxSn[0];
                    GlJournalFaCapitalization newDetail = GlJournalFaCapitalization.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(sn)
                            .faPoid(dto.getFaPoid())
                            .faDescription(dto.getFaDescription())
                            .faCategory(dto.getFaCategory())
                            .assetType(dto.getAssetType())
                            .assetValue(dto.getAssetValue())
                            .remarks(dto.getRemarks())
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .build();
                    toSave.add(newDetail);
                    break;
                case "ISUPDATED":
                    GlJournalFaCapitalization existing = existingMap.get(dto.getSn());
                    if (existing == null) {
                        throw new ResourceNotFoundException("Capitalization Detail", "sn", dto.getSn());
                    }
                    GlJournalFaCapitalization oldDetail = new GlJournalFaCapitalization();
                    BeanUtils.copyProperties(existing, oldDetail);
                    existing.setFaPoid(dto.getFaPoid());
                    existing.setFaDescription(dto.getFaDescription());
                    existing.setFaCategory(dto.getFaCategory());
                    existing.setAssetType(dto.getAssetType());
                    existing.setAssetValue(dto.getAssetValue());
                    existing.setRemarks(dto.getRemarks());
                    existing.setLastModifiedBy(getCurrentUser());
                    existing.setLastModifiedDate(LocalDateTime.now());
                    toUpdate.add(existing);
                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dto.getSn());
                    logRequests.add(new LogRequestDto<>(oldDetail, existing, GlJournalFaCapitalization.class, docId, transactionPoid.toString(), logDetail));
                    break;
                case "ISDELETED":
                    toDelete.add(dto.getSn());
                    loggingService.logDelete(dto, docId, transactionPoid.toString());
                    break;
            }
        }
        
        if (!toSave.isEmpty()) {
            List<GlJournalFaCapitalization> saved = glJournalFaCapitalizationRepository.saveAll(toSave);
            saved.forEach(detail -> {
                String logDetail = String.format("Row Created on Journal Voucher Capitalization Detail with sn: %s", detail.getDetRowId());
                loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            glJournalFaCapitalizationRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            existingList.stream()
                    .filter(d -> toDelete.contains(d.getDetRowId()))
                    .forEach(glJournalFaCapitalizationRepository::delete);
        }
    }

    @Override
    @Transactional
    public JournalVoucherResponse updateJournalVoucher(Long transactionPoid, JournalVoucherRequest request,String docId) {
        GlJournalVoucherHdr existing = glJournalVoucherHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Journal Voucher", "POID", transactionPoid));
        
        // Create a copy of the existing entity for logging
        GlJournalVoucherHdr oldEntity = new GlJournalVoucherHdr();
        BeanUtils.copyProperties(existing, oldEntity);

        if (!existing.getRefType().equalsIgnoreCase(request.getRefType())) {
            throw new IllegalArgumentException("RefType cannot be changed after creation");
        }

        validateJournalVoucher(request);

        boolean isMultiCompany = Boolean.TRUE.equals(request.getMultiCompany());
        BigDecimal bhdAmount = request.getBhdAmount() != null ? request.getBhdAmount() : 
                (request.getCurrencyRate() != null ? request.getAmount().multiply(request.getCurrencyRate()) : request.getAmount());

        existing.setTransactionDate(request.getTransactionDate());
        existing.setCurrencyCode(request.getCurrencyCode());
        existing.setCurrencyRate(request.getCurrencyRate());
        existing.setAmount(request.getAmount());
        existing.setBhdAmount(bhdAmount);
        existing.setPostingNarration(request.getPostingNarration());
        existing.setWdvAccountGl(request.getWdvAccountGl());
        existing.setMultiCompany(isMultiCompany ? FLAG_YES : FLAG_NO);
        existing.setRemarks(request.getRemarks());
        existing.setConfidentialRemarks(request.getConfidentialRemarks());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        glJournalVoucherHdrRepository.save(existing);

        if (REF_TYPE_GENERAL.equalsIgnoreCase(request.getRefType())) {
            saveGlDetails(existing, request.getGlDetails(), isMultiCompany,docId);
        } else if (REF_TYPE_ASSET_DISPOSAL.equalsIgnoreCase(request.getRefType())) {
            glJournalVoucherAssetDtlRepository.deleteAll(glJournalVoucherAssetDtlRepository.findAll((root, query, cb) -> 
                    cb.equal(root.get("transactionPoid"), transactionPoid)));
            saveAssetDetails(transactionPoid, request.getAssetDetails());
        } else if (REF_TYPE_ASSET_CAPITALIZATION.equalsIgnoreCase(request.getRefType())) {
            glJournalFaCapitalizationRepository.deleteAll(glJournalFaCapitalizationRepository.findAll((root, query, cb) -> 
                    cb.equal(root.get("transactionPoid"), transactionPoid)));
            saveCapitalizationDetails(transactionPoid, request.getAssetCapitalization());
            saveGlDetails(existing, request.getGlDetails(), isMultiCompany,docId);
        }

        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, existing, GlJournalVoucherHdr.class, 
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return JournalVoucherResponse.builder()
                .transactionPoid(existing.getTransactionPoid())
                .docRef(existing.getDocRef())
                .message("Journal Voucher updated successfully")
                .build();
    }

    @Override
    public JournalVoucherDetailResponse getJournalVoucherById(Long transactionPoid) {

        GlJournalVoucherHdr hdr = glJournalVoucherHdrRepository
                .findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Journal Voucher", "POID", transactionPoid));

        JournalVoucherDetailResponse.JournalVoucherDetailResponseBuilder response = buildBaseResponse(hdr);

        switch (hdr.getRefType()) {
            case REF_TYPE_GENERAL:
                populateGeneralDetails(response, transactionPoid);
                break;

            case REF_TYPE_ASSET_DISPOSAL:
                populateAssetDisposalDetails(response, transactionPoid);
                break;

            case REF_TYPE_ASSET_CAPITALIZATION:
                populateAssetCapitalizationDetails(response, transactionPoid);
                populateGeneralDetails(response, transactionPoid);
                break;

            default:
                break;
        }

        return response.build();
    }



    @Override
    @Transactional
    public void deleteJournalVoucher(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlJournalVoucherHdr entity = glJournalVoucherHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Journal Voucher", "POID", transactionPoid));
        
        if (entity.getPostedFromDocId() != null && !entity.getPostedFromDocId().isEmpty()) {
            log.error("Cannot delete: Journal Voucher has been posted to GL");
            throw new IllegalStateException("Cannot delete: Journal Voucher has been posted to GL");
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_JOURNAL_VOUCHER_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                entity.getTransactionDate()
        );
    }

    @Override
    public Map<String, Object> listJournalVouchers(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "POSTING_NARRATION",
                "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public PostJournalVoucherResponse postJournalVoucher(Long transactionPoid, PostJournalVoucherRequest request,String documentId) {
        GlJournalVoucherHdr entity = glJournalVoucherHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Journal Voucher", "POID", transactionPoid));



        if (REF_TYPE_GENERAL.equalsIgnoreCase(entity.getRefType())) {
            BigDecimal drTotal = glJournalVoucherDtlRepository.sumDrAmtByTransactionPoid(transactionPoid);
            BigDecimal crTotal = glJournalVoucherDtlRepository.sumCrAmtByTransactionPoid(transactionPoid);
            if (drTotal.compareTo(crTotal) != 0) {
                throw new IllegalStateException("Cannot post: Debit total must equal Credit total");
            }
        }

        String postingRef = glJournalVoucherHdrRepository.postJournalVoucher(
                transactionPoid,
                entity,
                documentId
        );

        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        glJournalVoucherHdrRepository.save(entity);

        return PostJournalVoucherResponse.builder()
                .transactionPoid(transactionPoid)
                .status(STATUS_POSTED)
                .postingReference(postingRef)
                .message("Journal Voucher posted successfully")
                .build();
    }

    @Override
    public JournalVoucherTotalsResponse getGlDetailTotals(Long transactionPoid) {
        GlJournalVoucherHdr entity = glJournalVoucherHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Journal Voucher", "POID", transactionPoid));

        if (!REF_TYPE_GENERAL.equalsIgnoreCase(entity.getRefType())) {
            throw new IllegalArgumentException("Totals are only applicable for GENERAL ref type");
        }

        BigDecimal drTotal = glJournalVoucherDtlRepository.sumDrAmtByTransactionPoid(transactionPoid);
        BigDecimal crTotal = glJournalVoucherDtlRepository.sumCrAmtByTransactionPoid(transactionPoid);
        BigDecimal difference = drTotal.subtract(crTotal);

        return JournalVoucherTotalsResponse.builder()
                .drTotal(drTotal)
                .crTotal(crTotal)
                .difference(difference)
                .isBalanced(difference.compareTo(BigDecimal.ZERO) == 0)
                .build();
    }

    @Override
    public JournalVoucherAssetDetailDto getAssetDepreciationDetails(Long faPoid) {
        List<JournalVoucherAssetDetailDto> results = glJournalVoucherHdrRepository.fetchAssetDepreciationDetails(faPoid);
        
        if (results == null || results.isEmpty()) {
            throw new ResourceNotFoundException("Asset", "FA_POID", faPoid);
        }
        
        return results.get(0);
    }

    @Override
    @Transactional
    public void updateAssetDetail(Long transactionPoid, Long sn, UpdateAssetDetailRequest request) {
        // Validate process type
        if (request.getProcess() != null && 
            !PROCESS_SCRAP.equalsIgnoreCase(request.getProcess()) &&
            !PROCESS_SOLD.equalsIgnoreCase(request.getProcess()) &&
            !PROCESS_OBSOLETE.equalsIgnoreCase(request.getProcess()) &&
            !PROCESS_NOT_APPLICABLE.equalsIgnoreCase(request.getProcess())) {
            throw new IllegalArgumentException("Invalid process type. Must be SCRAP, SOLD, OBSOLETE, or Not Applicable");
        }

        // Find asset detail
        GlJournalVoucherAssetDtl detail = glJournalVoucherAssetDtlRepository.findById(
                new TransactionDetailKey(transactionPoid, sn))
                .orElseThrow(() -> new ResourceNotFoundException("Asset Detail", "SN", sn));

        // Update asset detail
        detail.setFaPoid(request.getFaPoid());
        detail.setProcess(request.getProcess());
        detail.setScrapSoldValue(request.getScrapSoldValue());
        detail.setScrapSoldDate(request.getScrapSoldDate());
        detail.setRemarks(request.getRemarks());
        detail.setLastModifiedBy(getCurrentUser());
        detail.setLastModifiedDate(LocalDateTime.now());
        glJournalVoucherAssetDtlRepository.save(detail);

        glJournalVoucherHdrRepository.updateAssetDetail(
                request.getFaPoid(), 
                request.getScrapSoldValue(), 
                request.getScrapSoldDate(), 
                request.getProcess(),
                transactionPoid
        );
    }

    @Override
    public JournalVoucherCapitalizationDto getAssetCapitalizationDetails(Long faPoid) throws SQLException {
        List<JournalVoucherCapitalizationDto> results = glJournalVoucherHdrRepository.fetchFixedAssetDetails(faPoid);
        
        if (results == null || results.isEmpty()) {
            throw new ResourceNotFoundException("Asset", "FA_POID", faPoid);
        }
        
        return results.get(0);
    }

    @Override
    @Transactional
    public CreateFixedAssetResponse createFixedAsset(CreateFixedAssetRequest request) {
        // Validate asset code uniqueness
        if (fixedAssetRepository.existsByFaCode(request.getFaCode())) {
            throw new IllegalArgumentException("Asset code already exists: " + request.getFaCode());
        }

        FixedAsset fixedAsset = FixedAsset.builder()
                .faCode(request.getFaCode())
                .faDescription(request.getFaDescription())
                .assetType(request.getAssetType())
                .grossValue(request.getAssetValue())
                .faCategoryPoid(request.getFaCategory())
                .groupPoid(getGroupId())
                .companyPoid(getCompanyId())
                .active(FLAG_YES)
                .deleted(FLAG_NO)
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .build();

        fixedAsset = fixedAssetRepository.save(fixedAsset);

        return CreateFixedAssetResponse.builder()
                .faPoid(fixedAsset.getFaPoid())
                .faCode(fixedAsset.getFaCode())
                .message("Fixed Asset created successfully")
                .build();
    }


    private JournalVoucherDetailResponse.JournalVoucherDetailResponseBuilder buildBaseResponse(GlJournalVoucherHdr hdr) {
        return JournalVoucherDetailResponse.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .transactionDate(hdr.getTransactionDate())
                .docRef(hdr.getDocRef())
                .refType(hdr.getRefType())
                .currencyCode(hdr.getCurrencyCode())
                .currencyRate(hdr.getCurrencyRate())
                .amount(hdr.getAmount())
                .bhdAmount(hdr.getBhdAmount())
                .postingNarration(hdr.getPostingNarration())
                .wdvAccountGl(hdr.getWdvAccountGl())
                .multiCompany(FLAG_YES.equals(hdr.getMultiCompany()))
                .remarks(hdr.getRemarks())
                .confidentialRemarks(hdr.getConfidentialRemarks())
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .modifiedBy(hdr.getLastModifiedBy())
                .modifiedDate(hdr.getLastModifiedDate());
    }


    private void populateGeneralDetails(
            JournalVoucherDetailResponse.JournalVoucherDetailResponseBuilder response,
            Long transactionPoid) {

        List<GlJournalVoucherDtl> dtls = glJournalVoucherDtlRepository.findByTransactionPoid(transactionPoid);

        GlVoucherCostCenterBreakupResponseDto costCenterResponse = loadAllCostCenterData(transactionPoid);
        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = loadAllBillwiseData(transactionPoid);

        List<JournalVoucherDetailResponse.GlDetailResponse> glDetails = dtls.stream()
                .map(dtl -> mapGeneralDetailWithBreakups(dtl, costCenterResponse, billwiseResponse))
                .collect(Collectors.toList());

        response.glDetails(glDetails);
        response.drTotal(sumAmounts(dtls, GlJournalVoucherDtl::getDrAmt));
        response.crTotal(sumAmounts(dtls, GlJournalVoucherDtl::getCrAmt));
    }

    private GlVoucherCostCenterBreakupResponseDto loadAllCostCenterData(Long transactionPoid) {
        try {
            return costCenterBreakupService.loadCostCenterData(
                    "400-100", transactionPoid, getGroupId(), getCompanyId(), getUserPoid());
        } catch (Exception e) {
            log.warn("Failed to load cost center breakup for transaction: {}", transactionPoid, e);
            return null;
        }
    }

    private GlVoucherLoadBillwiseBreakupResponseDto loadAllBillwiseData(Long transactionPoid) {
        try {
            return billwiseBreakupService.loadBillwiseBreakup(
                    getGroupId(), getCompanyId(), "400-100", transactionPoid);
        } catch (Exception e) {
            log.warn("Failed to load billwise breakup for transaction: {}", transactionPoid, e);
            return null;
        }
    }

    private JournalVoucherDetailResponse.GlDetailResponse mapGeneralDetailWithBreakups(
            GlJournalVoucherDtl dtl, 
            GlVoucherCostCenterBreakupResponseDto costCenterResponse,
            GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse) {
        
        return JournalVoucherDetailResponse.GlDetailResponse.builder()
                .sn(dtl.getDetRowId())
                .type(dtl.getType())
                .companyPoid(toStringOrNull(dtl.getCompanyPoid()))
                .glPoid(toStringOrNull(dtl.getGlPoid()))
                .glCode(null)
                .glDescription(null)
                .drAmt(dtl.getDrAmt())
                .crAmt(dtl.getCrAmt())
                .remarks(dtl.getRemarks())
                .costCenterBreakup(filterCostCenterBreakup(costCenterResponse, dtl.getDetRowId()))
                .billWiseBreakup(filterBillwiseBreakup(billwiseResponse, dtl.getDetRowId()))
                .build();
    }

    private List<CostCenterBreakupResponseDto> filterCostCenterBreakup(
            GlVoucherCostCenterBreakupResponseDto response, Long detRowId) {
        
        if (response == null || response.getCostBreakupList() == null) {
            return List.of();
        }
        
        return response.getCostBreakupList().stream()
                .filter(cc -> cc.getMainDetRowId().equals(detRowId))
                .map(cc -> {
                    CostCenterBreakupResponseDto dto = new CostCenterBreakupResponseDto();
                    dto.setAmount(cc.getAmount());
                    dto.setGlPoid(cc.getGlPoid());
                    dto.setDescription(cc.getDescription());
                    dto.setCostDetRowId(cc.getCostDetRowId());
                    dto.setMainDetRowId(cc.getMainDetRowId());
                    dto.setCostGroup(cc.getCostGroup());
                    dto.setCostPoid(cc.getCostPoid());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<LoadBillwiseBreakupResponseDto> filterBillwiseBreakup(
            GlVoucherLoadBillwiseBreakupResponseDto response, Long detRowId) {
        
        if (response == null || response.getLoadBillwiseBreakupResponseDtoList() == null) {
            return List.of();
        }
        
        return response.getLoadBillwiseBreakupResponseDtoList().stream()
                .filter(bw -> bw.getMainDetRowId().equals(detRowId))
                .map(bw -> {
                    LoadBillwiseBreakupResponseDto dto = new LoadBillwiseBreakupResponseDto();
                    dto.setMainDetRowId(bw.getMainDetRowId());
                    dto.setBillDetRowId(bw.getBillDetRowId());
                    dto.setGlPoid(bw.getGlPoid());
                    dto.setBillRefType(bw.getBillRefType());
                    dto.setBillRef(bw.getBillRef());
                    dto.setBillDueDate(bw.getBillDueDate());
                    dto.setDrAmt(bw.getDrAmt());
                    dto.setCrAmt(bw.getCrAmt());
                    dto.setBillRemarks(bw.getBillRemarks());
                    return dto;
                })
                .collect(Collectors.toList());
    }


    private void populateAssetDisposalDetails(
            JournalVoucherDetailResponse.JournalVoucherDetailResponseBuilder response,
            Long transactionPoid) {

        List<JournalVoucherAssetDetailDto> assetDetails =
                glJournalVoucherAssetDtlRepository.findAll(
                                (root, query, cb) -> cb.equal(root.get("transactionPoid"), transactionPoid))
                        .stream()
                        .map(this::mapAssetDisposalDetail)
                        .collect(Collectors.toList());

        response.assetDetails(assetDetails);
    }

    private JournalVoucherAssetDetailDto mapAssetDisposalDetail(GlJournalVoucherAssetDtl dtl) {
        return JournalVoucherAssetDetailDto.builder()
                .sn(dtl.getDetRowId())
                .faPoid(dtl.getFaPoid())
                .lifeYear(dtl.getLifeYear())
                .purchaseDate(dtl.getPurchaseDate())
                .depreciationStartDate(dtl.getDepreciationStartDate())
                .assetValue(dtl.getAssetValue())
                .depreciatedAmt(dtl.getDepreciatedAmt())
                .wdvValue(dtl.getWdvValue())
                .process(dtl.getProcess())
                .scrapSoldDate(dtl.getScrapSoldDate())
                .scrapSoldValue(dtl.getScrapSoldValue())
                .remarks(dtl.getRemarks())
                .build();
    }

    private void populateAssetCapitalizationDetails(
            JournalVoucherDetailResponse.JournalVoucherDetailResponseBuilder response,
            Long transactionPoid) {

        List<JournalVoucherCapitalizationDto> capDetails =
                glJournalFaCapitalizationRepository.findAll(
                                (root, query, cb) -> cb.equal(root.get("transactionPoid"), transactionPoid))
                        .stream()
                        .map(this::mapCapDetail)
                        .collect(Collectors.toList());

        response.assetCapitalization(capDetails);
    }

    private JournalVoucherCapitalizationDto mapCapDetail(GlJournalFaCapitalization dtl) {
        return JournalVoucherCapitalizationDto.builder()
                .sn(dtl.getDetRowId())
                .faPoid(dtl.getFaPoid())
                .faDescription(dtl.getFaDescription())
                .faCategory(dtl.getFaCategory())
                .assetType(dtl.getAssetType())
                .assetValue(dtl.getAssetValue())
                .remarks(dtl.getRemarks())
                .build();
    }

    private BigDecimal sumAmounts(
            List<GlJournalVoucherDtl> dtls,
            Function<GlJournalVoucherDtl, BigDecimal> getter) {

        return dtls.stream()
                .map(d -> Optional.ofNullable(getter.apply(d)).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }


    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-100");
        params.put("SUB_GENERAL", printService.load("Finance/GL/JournalVoucher_subreport1.jrxml"));
        params.put("SUB_ASSET_DISPOSAL", printService.load("Finance/GL/JournalVoucherAssetDisposalSubreport2.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/JournalVoucher.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private void validateGLPoid(Long glPoid){
        if (!glMasterRepository.existsByGlPoid(glPoid)) {
            throw new ResourceNotFoundException("Gl Master", "glPoid", glPoid);
        }
    }

}
