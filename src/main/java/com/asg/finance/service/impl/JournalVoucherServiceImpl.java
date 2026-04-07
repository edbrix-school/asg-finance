package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.entity.*;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.repository.*;
import com.asg.finance.dto.*;

import com.asg.finance.entity.key.TransactionDetailKey;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
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
    private static final String RES_JOURNAL_VOUCHER = "Journal Voucher";
    private static final String RES_ASSET_DETAIL = "Asset Detail";
    private static final String RES_ASSET = "Asset";
    private static final String FIELD_POID = "POID";
    private static final String FIELD_TRANSACTION_POID = "TRANSACTION_POID";
    private static final String FIELD_FA_POID = "FA_POID";
    private static final String FIELD_GL_POID = "glPoid";
    private static final String CONST_DOC_ID = "400-100";
    private static final String CREATED_SUCCESS_MSG = "Journal Voucher created successfully";
    private static final String UPDATED_SUCCESS_MSG = "Journal Voucher updated successfully";

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
    private final ApplicationContext applicationContext;

    @Override
    @Transactional
    @PerformGlPosting
    public JournalVoucherResponse createJournalVoucher(JournalVoucherRequest request, String docId) {
        JournalVoucherServiceImpl self = applicationContext.getBean(JournalVoucherServiceImpl.class);
        return self.saveJournalVoucherData(request, docId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalVoucherResponse saveJournalVoucherData(JournalVoucherRequest request, String docId) {
        log.info("Saving Journal Voucher Data in REQUIRES_NEW - RefType: {}, docId: {}", request.getRefType(), docId);
        try {
            validateJournalVoucher(request);
            boolean isMultiCompany = Boolean.TRUE.equals(request.getMultiCompany());

            BigDecimal bhdAmount = calculateBhdAmount(request);

            GlJournalVoucherHdr header = buildJournalVoucherHeader(request, bhdAmount, isMultiCompany);
            header = glJournalVoucherHdrRepository.save(header);

            log.info("Journal Voucher header saved - TransactionPoid: {}, DocRef: {}",
                    header.getTransactionPoid(), header.getDocRef());

            processDetailsByType(header, request, isMultiCompany, docId);

            log.info("{} created successfully - TransactionPoid: {}, DocRef: {}, RefType: {}",
                    RES_JOURNAL_VOUCHER, header.getTransactionPoid(), header.getDocRef(), request.getRefType());

            String key = header.getTransactionPoid().toString();
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

            return JournalVoucherResponse.builder()
                    .transactionPoid(header.getTransactionPoid())
                    .docRef(header.getDocRef())
                    .message(CREATED_SUCCESS_MSG)
                    .build();
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            log.error("Validation error creating {} - RefType: {}, Error: {}",
                    RES_JOURNAL_VOUCHER, request.getRefType(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating {} - RefType: {}, Error: {}",
                    RES_JOURNAL_VOUCHER, request.getRefType(), e.getMessage(), e);
            throw new AsgException("Failed to create journal voucher: " + e.getMessage(), e);
        }
    }

    private BigDecimal calculateBhdAmount(JournalVoucherRequest request) {
        if (request.getBhdAmount() != null) {
            return request.getBhdAmount();
        }
        if (request.getCurrencyRate() != null) {
            return request.getAmount().multiply(request.getCurrencyRate());
        }
        return request.getAmount();
    }

    private GlJournalVoucherHdr buildJournalVoucherHeader(JournalVoucherRequest request, BigDecimal bhdAmount,
            boolean isMultiCompany) {
        return GlJournalVoucherHdr.builder()
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate()
                        : DateUtil.getCurrentDateInUserTimeZone())
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
                .build();
    }

    private void processDetailsByType(GlJournalVoucherHdr header, JournalVoucherRequest request, boolean isMultiCompany,
            String docId) {
        String refType = request.getRefType();
        if (REF_TYPE_GENERAL.equalsIgnoreCase(refType)) {
            saveGeneralDetails(header, request, isMultiCompany, docId);
        } else if (REF_TYPE_ASSET_DISPOSAL.equalsIgnoreCase(refType)) {
            saveAssetDisposalDetails(header, request);
        } else if (REF_TYPE_ASSET_CAPITALIZATION.equalsIgnoreCase(refType)) {
            saveAssetCapitalizationDetails(header, request, isMultiCompany, docId);
        }
    }

    private void saveGeneralDetails(GlJournalVoucherHdr header, JournalVoucherRequest request, boolean isMultiCompany,
            String docId) {
        if (request.getGlDetails() != null) {
            log.debug("Saving {} GL detail lines", request.getGlDetails().size());
            request.getGlDetails().forEach(detail -> detail.setActionType(ACTION_ISCREATED));
            saveGlDetails(header, request.getGlDetails(), isMultiCompany, docId);
        }
    }

    private void saveAssetDisposalDetails(GlJournalVoucherHdr header, JournalVoucherRequest request) {
        if (request.getAssetDetails() != null) {
            log.debug("Saving {} asset disposal detail lines", request.getAssetDetails().size());
            request.getAssetDetails().forEach(detail -> detail.setActionType(ACTION_ISCREATED));
            saveAssetDetails(header.getTransactionPoid(), request.getAssetDetails());
            glJournalVoucherAssetDtlRepository.flush();
            if (!request.getAssetDetails().isEmpty()) {
                glJournalVoucherHdrRepository.updateAssetDetail(header.getTransactionPoid());
            }
        }
    }

    private void saveAssetCapitalizationDetails(GlJournalVoucherHdr header, JournalVoucherRequest request,
            boolean isMultiCompany, String docId) {
        if (request.getAssetCapitalization() != null) {
            log.debug("Saving {} asset capitalization detail lines", request.getAssetCapitalization().size());
            if (request.getGlDetails() != null) {
                request.getGlDetails().forEach(detail -> detail.setActionType(ACTION_ISCREATED));
            }
            request.getAssetCapitalization().forEach(detail -> detail.setActionType(ACTION_ISCREATED));
            saveCapitalizationDetails(header.getTransactionPoid(), request.getAssetCapitalization());
            if (request.getGlDetails() != null) {
                saveGlDetails(header, request.getGlDetails(), isMultiCompany, docId);
            }
        }
    }

    private void validateJournalVoucher(JournalVoucherRequest request) {
        String refType = getString(request);
        validateCommonFields(request);
        handleCurrencyValidation(request);
        validateSpecificRefType(request, refType);
    }

    private void validateCommonFields(JournalVoucherRequest request) {
        if (request.getTransactionDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }
    }

    private void handleCurrencyValidation(JournalVoucherRequest request) {
        String currencyCode = request.getCurrencyCode();
        if (currencyCode != null && !currencyCode.isBlank()) {
            if (request.getCurrencyRate() == null || request.getCurrencyRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Currency rate must be greater than 0 for foreign currency");
            }
            try {
                lovDataService.getDetailsByCodeAndLovName(currencyCode, "CURRENCY");
            } catch (ResourceNotFoundException e) {
                throw new ResourceNotFoundException("Currency", "Code", currencyCode);
            }
        }
    }

    private void validateSpecificRefType(JournalVoucherRequest request, String refType) {
        switch (refType.toUpperCase()) {
            case REF_TYPE_GENERAL -> validateGeneralType(request);
            case REF_TYPE_ASSET_DISPOSAL -> validateAssetDisposalType(request);
            case REF_TYPE_ASSET_CAPITALIZATION -> validateAssetCapitalizationType(request);
            default -> throw new IllegalArgumentException("Unsupported RefType: " + refType);
        }
    }

    private void validateGeneralType(JournalVoucherRequest request) {
        if (request.getGlDetails() == null || request.getGlDetails().isEmpty()) {
            throw new IllegalArgumentException("At least one GL detail line is required for GENERAL type");
        }
        validateGlDetails(request.getGlDetails(), Boolean.TRUE.equals(request.getMultiCompany()));
        validateDebitCreditBalance(request.getGlDetails(), calculateBhdAmount(request));
    }

    private void validateAssetDisposalType(JournalVoucherRequest request) {
        if (request.getWdvAccountGl() == null) {
            log.error("WDV Account GL is required for ASSET_DISPOSAL");
            throw new IllegalArgumentException("WDV Account GL is required for ASSET_DISPOSAL type");
        }
        if (request.getAssetDetails() == null || request.getAssetDetails().isEmpty()) {
            String msg = "At least one asset detail line is required for ASSET_DISPOSAL type";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        validateAssetDetailsForDisposal(request.getAssetDetails());
    }

    private void validateAssetCapitalizationType(JournalVoucherRequest request) {
        if (request.getGlDetails() == null || request.getGlDetails().isEmpty()) {
            throw new IllegalArgumentException("At least one GL detail line is required for ASSET_CAPITALIZATION type");
        }
        if (request.getAssetCapitalization() == null || request.getAssetCapitalization().isEmpty()) {
            String msg = "At least one asset capitalization line is required for ASSET_CAPITALIZATION type";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        validateGlDetails(request.getGlDetails(), Boolean.TRUE.equals(request.getMultiCompany()));
        validateDebitCreditBalance(request.getGlDetails(), calculateBhdAmount(request));
        validateAssetDetailsForCapitalization(request.getAssetCapitalization());
        validateCapitalizationNature(request.getGlDetails(), request.getAssetCapitalization());
    }

    private String getString(JournalVoucherRequest request) {
        String refType = request.getRefType();
        if (!REF_TYPE_GENERAL.equalsIgnoreCase(refType) && !REF_TYPE_ASSET_DISPOSAL.equalsIgnoreCase(refType)
                && !REF_TYPE_ASSET_CAPITALIZATION.equalsIgnoreCase(refType)) {
            throw new IllegalArgumentException("RefType must be GENERAL, ASSET_DISPOSAL, or ASSET_CAPITALIZATION");
        }
        return refType;
    }

    private void validateGlDetails(List<JournalVoucherGlDetailDto> glDetails, boolean multiCompany) {
        for (JournalVoucherGlDetailDto detail : glDetails) {
            validateSingleGlDetail(detail, multiCompany);
        }
    }

    private void validateSingleGlDetail(JournalVoucherGlDetailDto detail, boolean multiCompany) {
        String type = detail.getType();
        if (!TYPE_DEBIT.equalsIgnoreCase(type) && !TYPE_CREDIT.equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Type must be 'Dr' or 'Cr'");
        }

        if (TYPE_DEBIT.equalsIgnoreCase(type)) {
            validateDebitAmount(detail);
        } else {
            validateCreditAmount(detail);
        }

        if (multiCompany && detail.getCompanyPoid() == null) {
            throw new IllegalArgumentException("CompanyPoid is mandatory for GL detail when multiCompany is true");
        }

        if (detail.getGlPoid() == null) {
            throw new IllegalArgumentException("GlPoid is mandatory for GL detail");
        }
    }

    private void validateDebitAmount(JournalVoucherGlDetailDto detail) {
        if (detail.getDrAmt() == null || detail.getDrAmt().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("DrAmt must be greater than 0 for Type=Dr");
        }
        if (detail.getCrAmt() != null && detail.getCrAmt().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("CrAmt must be 0 or null for Type=Dr");
        }
    }

    private void validateCreditAmount(JournalVoucherGlDetailDto detail) {
        if (detail.getCrAmt() == null || detail.getCrAmt().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("CrAmt must be greater than 0 for Type=Cr");
        }
        if (detail.getDrAmt() != null && detail.getDrAmt().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("DrAmt must be 0 or null for Type=Cr");
        }
    }

    private void validateAssetDetailsForDisposal(List<JournalVoucherAssetDetailDto> assetDetails) {
        for (JournalVoucherAssetDetailDto detail : assetDetails) {
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

    private void validateDebitCreditBalance(List<JournalVoucherGlDetailDto> glDetails, BigDecimal bhdAmount) {
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

        if (totalCr.compareTo(bhdAmount) != 0) {
            log.error("Detail total {} does not match header amount {}", totalCr, bhdAmount);
            throw new IllegalArgumentException(
                    "Total detail amount (" + totalCr + ") does not match header amount (" + bhdAmount + ")");
        }
    }

    private void validateCapitalizationNature(List<JournalVoucherGlDetailDto> glDetails,
            List<JournalVoucherCapitalizationDto> capitalization) {
        BigDecimal assetSum = capitalization.stream()
                .map(d -> d.getAssetValue() != null ? d.getAssetValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fixedAssetNatureGlTotal = BigDecimal.ZERO;

        for (JournalVoucherGlDetailDto detail : glDetails) {
            if (TYPE_DEBIT.equalsIgnoreCase(detail.getType())) {
                GLMaster gl = glMasterRepository.findByGlPoid(detail.getGlPoid())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("GL Master", FIELD_GL_POID, detail.getGlPoid()));

                if ("FIXED_ASSET".equalsIgnoreCase(gl.getControlAcNature())) {
                    fixedAssetNatureGlTotal = fixedAssetNatureGlTotal.add(detail.getDrAmt());
                }
            }
        }

        if (fixedAssetNatureGlTotal.compareTo(assetSum) != 0) {
            throw new IllegalArgumentException("Asset Item Total (" + assetSum
                    + ") is not matching with debit to the Asset Group GL total (" + fixedAssetNatureGlTotal + ")");
        }
    }

    private void saveGlDetails(GlJournalVoucherHdr header, List<JournalVoucherGlDetailDto> glDetails,
            boolean multiCompany, String docId) {
        Long transactionPoid = header.getTransactionPoid();
        Long headerCompanyPoid = header.getCompanyPoid();

        long maxDetRowId = glJournalVoucherDtlRepository.getMaxDetRowIdByTransactionPoid(transactionPoid);

        List<CostCenterBreakupRequestDto> costCenterRequestDtoList = new ArrayList<>();
        List<BillwiseBreakupRequestDto> billwiseRequestDtoList = new ArrayList<>();
        List<LogRequestDto<GlJournalVoucherDtl>> logRequests = new ArrayList<>();

        for (JournalVoucherGlDetailDto dto : glDetails) {
            String action = resolveAction(dto.getActionType());
            Long companyPoid = multiCompany ? dto.getCompanyPoid() : headerCompanyPoid;

            switch (action) {
                case ACTION_ISCREATED -> {
                    maxDetRowId++;
                    GlJournalVoucherDtl detail = new GlJournalVoucherDtl();
                    BeanUtils.copyProperties(dto, detail);
                    detail.setTransactionPoid(transactionPoid);
                    detail.setDetRowId(maxDetRowId);
                    detail.setCompanyPoid(companyPoid);
                    glJournalVoucherDtlRepository.save(detail);

                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(),
                            String.format("Row Created on %s GL Detail with detRowId: %s", RES_JOURNAL_VOUCHER,
                                    maxDetRowId));

                    if (dto.getCostCenterBreakup() != null && !dto.getCostCenterBreakup().isEmpty()) {
                        costCenterRequestDtoList.addAll(buildCostCenterBreakups(transactionPoid, maxDetRowId, docId,
                                dto.getGlPoid(), dto.getCostCenterBreakup()));
                    }
                    if (dto.getBillWiseBreakup() != null && !dto.getBillWiseBreakup().isEmpty()) {
                        billwiseRequestDtoList.addAll(buildBillwiseBreakups(transactionPoid, maxDetRowId, docId,
                                dto.getGlPoid(), companyPoid, dto.getBillWiseBreakup()));
                    }
                }
                case ACTION_ISUPDATED -> {
                    GlJournalVoucherDtl detail = glJournalVoucherDtlRepository
                            .findById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("GL Detail", "detRowId", dto.getDetRowId()));

                    GlJournalVoucherDtl oldDetail = new GlJournalVoucherDtl();
                    BeanUtils.copyProperties(detail, oldDetail);

                    BeanUtils.copyProperties(dto, detail, "detRowId", "transactionPoid");
                    detail.setCompanyPoid(companyPoid);
                    glJournalVoucherDtlRepository.save(detail);

                    String logDetail = String.format("KeyId = %s:%s DET_ROW_ID:%s", FIELD_TRANSACTION_POID,
                            transactionPoid,
                            dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldDetail, detail, GlJournalVoucherDtl.class, docId,
                            transactionPoid.toString(), logDetail));

                    if (dto.getCostCenterBreakup() != null && !dto.getCostCenterBreakup().isEmpty()) {
                        costCenterRequestDtoList.addAll(buildCostCenterBreakups(transactionPoid, dto.getDetRowId(),
                                docId, dto.getGlPoid(), dto.getCostCenterBreakup()));
                    }
                    if (dto.getBillWiseBreakup() != null && !dto.getBillWiseBreakup().isEmpty()) {
                        billwiseRequestDtoList.addAll(buildBillwiseBreakups(transactionPoid, dto.getDetRowId(), docId,
                                dto.getGlPoid(), companyPoid, dto.getBillWiseBreakup()));
                    }
                }
                case ACTION_ISDELETED -> {
                    glJournalVoucherDtlRepository
                            .deleteById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()));
                    loggingService.logDelete(dto, docId, transactionPoid.toString());
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

    private List<CostCenterBreakupRequestDto> buildCostCenterBreakups(Long transactionPoid, Long detRowId, String docId,
            Long glPoid,
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

    private List<BillwiseBreakupRequestDto> buildBillwiseBreakups(Long transactionPoid, Long detRowId, String docId,
            Long glPoid, Long glCompanyPoid,
            List<BillwiseBreakupPopupRequestDto> billwiseBreakups) {
        return billwiseBreakups.stream()
                .map(dto -> BillwiseBreakupRequestDto.builder()
                        .groupPoid(getGroupId())
                        .companyPoid(getCompanyId())
                        .docId(docId)
                        .transactionPoid(transactionPoid)
                        .mainDetRowId(detRowId)
                        .glPoid(glPoid)
                        .glCompanyPoid(glCompanyPoid)
                        .billDetRowId(dto.getBillDetRowId())
                        .drAmt("Dr".equalsIgnoreCase(dto.getType()) ? dto.getAmount() : BigDecimal.ZERO)
                        .crAmt("Cr".equalsIgnoreCase(dto.getType()) ? dto.getAmount() : BigDecimal.ZERO)
                        .billRefType(dto.getBillRefType())
                        .billRef(dto.getBillRef())
                        .billDueDate(dto.getBillDueDate())
                        .billRemarks(dto.getBillRemarks())
                        .loginUserPoid(getUserPoid())
                        .build())
                .collect(Collectors.toList());
    }

    private void saveAssetDetails(Long transactionPoid, List<JournalVoucherAssetDetailDto> assetDetails) {
        String docId = UserContext.getDocumentId();

        long maxSn = glJournalVoucherAssetDtlRepository.getMaxDetRowIdByTransactionPoid(transactionPoid);

        List<LogRequestDto<GlJournalVoucherAssetDtl>> logRequests = new ArrayList<>();

        for (JournalVoucherAssetDetailDto dto : assetDetails) {
            String action = resolveAction(dto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> {
                    maxSn++;
                    GlJournalVoucherAssetDtl detail = new GlJournalVoucherAssetDtl();
                    BeanUtils.copyProperties(dto, detail);
                    detail.setTransactionPoid(transactionPoid);
                    detail.setDetRowId(maxSn);
                    glJournalVoucherAssetDtlRepository.save(detail);

                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(),
                            String.format("Row Created on %s with sn: %s", RES_ASSET_DETAIL, maxSn));
                }
                case ACTION_ISUPDATED -> {
                    GlJournalVoucherAssetDtl existing = glJournalVoucherAssetDtlRepository
                            .findById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException(RES_ASSET_DETAIL, "sn", dto.getDetRowId()));

                    GlJournalVoucherAssetDtl oldDetail = new GlJournalVoucherAssetDtl();
                    BeanUtils.copyProperties(existing, oldDetail);

                    BeanUtils.copyProperties(dto, existing, "detRowId", "transactionPoid");
                    glJournalVoucherAssetDtlRepository.save(existing);

                    logRequests.add(new LogRequestDto<>(oldDetail, existing, GlJournalVoucherAssetDtl.class,
                            docId, transactionPoid.toString(), "sn:" + dto.getDetRowId()));
                }
                case ACTION_ISDELETED -> {
                    glJournalVoucherAssetDtlRepository
                            .deleteById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()));
                    loggingService.logDelete(dto, docId, transactionPoid.toString());
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        if (!assetDetails.isEmpty()) {
            glJournalVoucherHdrRepository.updateAssetDetail(transactionPoid);
        }
    }

    private void saveCapitalizationDetails(Long transactionPoid,
            List<JournalVoucherCapitalizationDto> capitalizationDetails) {
        String docId = UserContext.getDocumentId();

        long maxSn = glJournalFaCapitalizationRepository.getMaxDetRowIdByTransactionPoid(transactionPoid);

        List<LogRequestDto<GlJournalFaCapitalization>> logRequests = new ArrayList<>();

        for (JournalVoucherCapitalizationDto dto : capitalizationDetails) {
            String action = resolveAction(dto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> {
                    maxSn++;
                    GlJournalFaCapitalization detail = new GlJournalFaCapitalization();
                    BeanUtils.copyProperties(dto, detail);
                    detail.setTransactionPoid(transactionPoid);
                    detail.setDetRowId(maxSn);
                    glJournalFaCapitalizationRepository.save(detail);

                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(),
                            String.format("Row Created on %s Capitalization Detail with sn: %s",
                                    RES_JOURNAL_VOUCHER, maxSn));
                }
                case ACTION_ISUPDATED -> {
                    GlJournalFaCapitalization existing = glJournalFaCapitalizationRepository
                            .findById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Capitalization Detail", "sn",
                                    dto.getDetRowId()));

                    GlJournalFaCapitalization oldDetail = new GlJournalFaCapitalization();
                    BeanUtils.copyProperties(existing, oldDetail);

                    BeanUtils.copyProperties(dto, existing, "detRowId", "transactionPoid");
                    glJournalFaCapitalizationRepository.save(existing);

                    logRequests.add(new LogRequestDto<>(oldDetail, existing, GlJournalFaCapitalization.class,
                            docId, transactionPoid.toString(), "sn:" + dto.getDetRowId()));
                }
                case ACTION_ISDELETED -> {
                    glJournalFaCapitalizationRepository
                            .deleteById(new TransactionDetailKey(transactionPoid, dto.getDetRowId()));
                    loggingService.logDelete(dto, docId, transactionPoid.toString());
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalVoucherResponse updateJournalVoucherData(Long transactionPoid, JournalVoucherRequest request,
                                                           String docId) {
        GlJournalVoucherHdr existing = glJournalVoucherHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(RES_JOURNAL_VOUCHER, FIELD_POID, transactionPoid));

        if (!existing.getRefType().equalsIgnoreCase(request.getRefType())) {
            throw new IllegalArgumentException("RefType cannot be changed after creation");
        }

        // Create a copy of the existing entity for logging
        GlJournalVoucherHdr oldEntity = new GlJournalVoucherHdr();
        BeanUtils.copyProperties(existing, oldEntity);

        validateJournalVoucher(request);

        boolean isMultiCompany = Boolean.TRUE.equals(request.getMultiCompany());
        BigDecimal bhdAmount = request.getBhdAmount() != null ? request.getBhdAmount()
                : (request.getCurrencyRate() != null ? request.getAmount().multiply(request.getCurrencyRate())
                   : request.getAmount());

        existing.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate()
                : DateUtil.getCurrentDateInUserTimeZone());
        existing.setCurrencyCode(request.getCurrencyCode());
        existing.setCurrencyRate(request.getCurrencyRate());
        existing.setAmount(request.getAmount());
        existing.setBhdAmount(bhdAmount);
        existing.setPostingNarration(request.getPostingNarration());
        existing.setWdvAccountGl(request.getWdvAccountGl());
        existing.setMultiCompany(isMultiCompany ? FLAG_YES : FLAG_NO);
        existing.setRemarks(request.getRemarks());
        existing.setConfidentialRemarks(request.getConfidentialRemarks());

        glJournalVoucherHdrRepository.save(existing);

        if (REF_TYPE_GENERAL.equalsIgnoreCase(request.getRefType())
                && request.getGlDetails() != null) {
            saveGlDetails(existing, request.getGlDetails(), isMultiCompany, docId);
        } else if (REF_TYPE_ASSET_DISPOSAL.equalsIgnoreCase(request.getRefType())
                && request.getAssetDetails() != null) {
            saveAssetDetails(existing.getTransactionPoid(), request.getAssetDetails());

            glJournalVoucherAssetDtlRepository.flush();
            if (!request.getAssetDetails().isEmpty()) {
                glJournalVoucherHdrRepository.updateAssetDetail(existing.getTransactionPoid());
            }
        } else if (REF_TYPE_ASSET_CAPITALIZATION.equalsIgnoreCase(request.getRefType())
                && request.getAssetCapitalization() != null) {
            saveCapitalizationDetails(existing.getTransactionPoid(), request.getAssetCapitalization());
            if (request.getGlDetails() != null) {
                saveGlDetails(existing, request.getGlDetails(), isMultiCompany, docId);
            }
        }

        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, existing, GlJournalVoucherHdr.class,
                docId, key, LogDetailsEnum.MODIFIED, FIELD_TRANSACTION_POID);

        return JournalVoucherResponse.builder()
                .transactionPoid(existing.getTransactionPoid())
                .docRef(existing.getDocRef())
                .message(UPDATED_SUCCESS_MSG)
                .build();
    }

    @Override
    @Transactional
    @PerformGlPosting
    public JournalVoucherResponse updateJournalVoucher(Long transactionPoid, JournalVoucherRequest request,
            String docId) {
        JournalVoucherServiceImpl self = applicationContext.getBean(JournalVoucherServiceImpl.class);
        return self.updateJournalVoucherData(transactionPoid, request, docId);
    }


    @Override
    public JournalVoucherDetailResponse getJournalVoucherById(Long transactionPoid) {

        GlJournalVoucherHdr hdr = glJournalVoucherHdrRepository
                .findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(RES_JOURNAL_VOUCHER, FIELD_POID, transactionPoid));

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
                .orElseThrow(() -> new ResourceNotFoundException(RES_JOURNAL_VOUCHER, FIELD_POID, transactionPoid));

        if (entity.getPostedFromDocId() != null && !entity.getPostedFromDocId().isEmpty()) {
            String msg = "Cannot delete: " + RES_JOURNAL_VOUCHER + " has been posted to GL";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_JOURNAL_VOUCHER_HDR",
                FIELD_TRANSACTION_POID,
                deleteReasonDto,
                entity.getTransactionDate());
    }

    @Override
    public Map<String, Object> listJournalVouchers(String documentId, FilterRequestDto filters, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate,
                endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "POSTING_NARRATION",
                FIELD_TRANSACTION_POID);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public JournalVoucherAssetDetailDto getAssetDepreciationDetails(Long faPoid) {
        List<JournalVoucherAssetDetailDto> results = glJournalVoucherHdrRepository
                .fetchAssetDepreciationDetails(faPoid);

        if (results == null || results.isEmpty()) {
            throw new ResourceNotFoundException(RES_ASSET, FIELD_FA_POID, faPoid);
        }

        return results.getFirst();
    }

    @Override
    public JournalVoucherCapitalizationDto getAssetCapitalizationDetails(Long faPoid) {
        List<JournalVoucherCapitalizationDto> results = glJournalVoucherHdrRepository.fetchFixedAssetDetails(faPoid);

        if (results == null || results.isEmpty()) {
            throw new ResourceNotFoundException(RES_ASSET, FIELD_FA_POID, faPoid);
        }

        return results.getFirst();
    }

    private JournalVoucherDetailResponse.JournalVoucherDetailResponseBuilder buildBaseResponse(
            GlJournalVoucherHdr hdr) {
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
                .toList();

        response.glDetails(glDetails);
        response.drTotal(sumAmounts(dtls, GlJournalVoucherDtl::getDrAmt));
        response.crTotal(sumAmounts(dtls, GlJournalVoucherDtl::getCrAmt));
    }

    private GlVoucherCostCenterBreakupResponseDto loadAllCostCenterData(Long transactionPoid) {
        try {
            return costCenterBreakupService.loadCostCenterData(
                    UserContext.getDocumentId(), transactionPoid, getGroupId(), getCompanyId(), getUserPoid());
        } catch (Exception e) {
            log.warn("Failed to load cost center breakup for transaction: {}", transactionPoid, e);
            return null;
        }
    }

    private GlVoucherLoadBillwiseBreakupResponseDto loadAllBillwiseData(Long transactionPoid) {
        try {
            return billwiseBreakupService.loadBillwiseBreakup(
                    getGroupId(), getCompanyId(), CONST_DOC_ID, transactionPoid);
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
                .detRowId(dtl.getDetRowId())
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
                .toList();
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
                .toList();
    }

    private void populateAssetDisposalDetails(
            JournalVoucherDetailResponse.JournalVoucherDetailResponseBuilder response,
            Long transactionPoid) {

        List<JournalVoucherAssetDetailDto> assetDetails = glJournalVoucherAssetDtlRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("transactionPoid"), transactionPoid))
                .stream()
                .map(this::mapAssetDisposalDetail)
                .toList();

        response.assetDetails(assetDetails);
    }

    private JournalVoucherAssetDetailDto mapAssetDisposalDetail(GlJournalVoucherAssetDtl dtl) {
        return JournalVoucherAssetDetailDto.builder()
                .detRowId(dtl.getDetRowId())
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

        List<JournalVoucherCapitalizationDto> capDetails = glJournalFaCapitalizationRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("transactionPoid"), transactionPoid))
                .stream()
                .map(this::mapCapDetail)
                .toList();

        response.assetCapitalization(capDetails);
    }

    private JournalVoucherCapitalizationDto mapCapDetail(GlJournalFaCapitalization dtl) {
        return JournalVoucherCapitalizationDto.builder()
                .detRowId(dtl.getDetRowId())
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
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, CONST_DOC_ID);
        params.put("SUB_GENERAL", printService.load("Finance/GL/JournalVoucher_subreport1.jrxml"));
        params.put("SUB_ASSET_DISPOSAL", printService.load("Finance/GL/JournalVoucherAssetDisposalSubreport2.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/JournalVoucher.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
    private String resolveAction(String rawAction) {
        String action = (rawAction == null || rawAction.trim().isEmpty()) ? ACTION_NOCHANGES : rawAction.trim().toUpperCase();
        return switch (action) {
            case "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
            case "ISUPDATED", "UPDATED" -> ACTION_ISUPDATED;
            case "ISDELETED", "DELETED" -> ACTION_ISDELETED;
            default -> ACTION_NOCHANGES;
        };
    }

}
