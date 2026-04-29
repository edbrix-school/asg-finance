package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.ShowPendingBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.finance.annotation.DocAfterSave;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.event.PettyCashVoucherSaveEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.entity.SupplierMasterEntity;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GLPettyCashItemDtl;
import com.asg.finance.dto.GlPettyCashItemDtlRequestDto;
import com.asg.finance.entity.GlPettyCashChargeDtl;
import com.asg.finance.entity.GlPettyCashPaymentDtl;
import com.asg.finance.entity.GlPettyCashPaymentGrnDtl;
import com.asg.finance.entity.GlPettyCashPaymentHdr;
import com.asg.finance.dto.GlPettyCashPaymentGrnDtlRequestDto;
import com.asg.finance.repository.GlPettyCashPaymentGrnDtlRepository;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.PettyCashVoucherService;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PettyCashVoucherServiceImpl implements PettyCashVoucherService {

    private final GlPettyCashPaymentHdrRepository glPettyCashPaymentHdrRepository;
    private final GlPettyCashPaymentDtlRepository glPettyCashPaymentDtlRepository;
    private final GLPettyCashItemDtlRepository glPettyCashItemDtlRepository;
    private final GlPettyCashChargeDtlRepository glPettyCashChargeDtlRepository;
    private final GlPettyCashPaymentGrnDtlRepository glPettyCashPaymentGrnDtlRepository;

    private final GLMasterRepository glMasterRepository;
    private final StockMasterRepository stockMasterRepository;
    private final ShipChargeRepository shipChargeRepository;
    private final UnitMasterRepository unitMasterRepository;
    private final DocumentSearchService documentService;
    private final TaxMasterRepository taxMasterRepository;
    private final com.asg.common.lib.service.LovDataService lovService;


    private final CostCenterBreakupService costCenterBreakupService;
    private final BillwiseBreakupService billwiseBreakupService;
    private final DocumentDeleteService documentDeleteService;

    private final PettyCashLoadByRefTypeRepository pettyCashLoadByRefTypeRepository;
    private final PettyCashPaymentVoucherCustomRepository pettyCashPaymentVoucherCustomRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final AdvancePettyCashHdrRepository advancePettyCashHdrRepository;
    private final AssetLocationMasterRepository assetLocationMasterRepository;

    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final GlobalParameterService globalParameterService;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @PerformGlPosting
    @DocAfterSave
    @Transactional
    public PettyCashResponseDto createPettyCash(PettyCashCreateRequestDto requestDto, String documentId) {
        StringBuilder result = new StringBuilder();

        try {
            GlPettyCashPaymentHdr header = buildPettyCashHeader(requestDto);
            applyNewDocumentDefaults(header);
            validateTransactionDate(requestDto.getTransactionDate());
            validateRefPoidRequired(requestDto);

            // Ref-type aware voucher validation — skip when the relevant ref is blank
            String voucherRef = resolveVoucherRefByType(requestDto.getRefType(), requestDto);
            if (hasText(voucherRef)) {
                pettyCashPaymentVoucherCustomRepository.validateGlVouchers(
                        UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                        requestDto.getDocId(), requestDto.getRefType(),
                        voucherRef, result
                );
                assertVoucherValidationOk(requestDto.getRefType(), result);
                logResult("PROC_GL_VOUCHERS_VALIDATIONS", result);
            }

            // Job validation for ref-based types (mirrors update-path ordering)
            String jobRefPoid = resolveRefPoidByType(requestDto, requestDto.getRefType());
            if (hasText(jobRefPoid)) {
                StringBuilder jobValidationResult = new StringBuilder();
                pettyCashPaymentVoucherCustomRepository.validateJobBeforeSave(
                        UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                        requestDto.getDocId(), requestDto.getRefType(),
                        jobRefPoid, jobValidationResult
                );
                assertJobValidationOk(requestDto.getRefType(), jobValidationResult);
                logResult("PROC_GL_JOB_VAL_BEFORE_SAVE", jobValidationResult);
            }

            StringBuilder taxInputGlPoid = new StringBuilder();
            String glPoidStringWithoutTax = "";
            String glPoidStringAll = "";
            if (isPaymentDtlRefType(requestDto.getRefType())) {
                glPoidStringWithoutTax = buildGlPoidStringWithoutTax(requestDto.getGlPettyCashPaymentDtlRequestDtos());
                glPoidStringAll = buildGlPoidStringAll(requestDto.getGlPettyCashPaymentDtlRequestDtos());
            }
            pettyCashPaymentVoucherCustomRepository.validateBeforeSave(
                    UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                    requestDto.getDocId(), requestDto.getRefType(),
                    glPoidStringWithoutTax, glPoidStringAll,
                    "", safeStatus(requestDto.getStatus()),
                    requestDto.getPettyCashGlPoid(), taxInputGlPoid, result
            );
            logResult("PROC_GL_DTL_BEFORE_SAVE_VAL_V2", result);
            validateTaxAndVatRules(requestDto, taxInputGlPoid.toString());
            validateRoundingAmount(requestDto.getRoundingAmount());

            validateCashBalance(requestDto, documentId);

            GlPettyCashPaymentHdr savedHeader = glPettyCashPaymentHdrRepository.save(header);
            entityManager.flush();
            entityManager.refresh(header);
            Long hdrPoid = savedHeader.getTransactionPoid();


            List<GlPettyCashPaymentDtlResponseDto> paymentDtls = new ArrayList<>();
            List<GlPettyCashChargeDtlResponseDto> chargeDtls = new ArrayList<>();
            List<GLPettyCashItemDtlResponseDto> itemDtls = new ArrayList<>();

            String refType = requestDto.getRefType();
            switch (refType.toUpperCase()) {
                case "GENERAL" -> {
                    List<GlPettyCashPaymentDtlRequestDto> activePmt = getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos());
                    if (activePmt.isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    List<GlPettyCashPaymentDtlRequestDto> effectiveDtls = ensurePettyCashGlAndValidateTally(requestDto);
                    var savedPaymentDtls = glPettyCashPaymentDtlRepository.saveAll(
                            mapPaymentDtlsFromList(effectiveDtls, hdrPoid));
                    paymentDtls = mapPaymentResponse(savedPaymentDtls);

                    // Log child record creation
                    savedPaymentDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Payment Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });

                    // BILLWISE BREAKUP INSERTION FOR EACH GL ROW
                    List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
                    for (GlPettyCashPaymentDtlRequestDto dtl : effectiveDtls) {
                        Long mainDetRowId = dtl.getDetRowId();
                        if (dtl.getBillwiseBreakupList() != null && !dtl.getBillwiseBreakupList().isEmpty()) {
                            for (BillwiseBreakupPopupRequestDto popup : dtl.getBillwiseBreakupList()) {
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    actionTypeStr = "isCreated";
                                }
                                if (!"ISCREATED".equals(actionTypeStr.toUpperCase())) continue;
                                BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();
                                dto.setGroupPoid(UserContext.getGroupPoid());
                                dto.setCompanyPoid(UserContext.getCompanyPoid());
                                dto.setDocId(documentId);
                                dto.setTransactionPoid(hdrPoid);
                                dto.setGlPoid(dtl.getGlPoid());
                                dto.setMainDetRowId(mainDetRowId);
                                dto.setBillDetRowId(popup.getBillDetRowId());
                                dto.setBillRefType(popup.getBillRefType());
                                dto.setBillRef(popup.getBillRef());
                                dto.setBillDueDate(popup.getBillDueDate());
                                dto.setDrAmt(resolveBillwiseDrAmt(popup));
                                dto.setCrAmt(resolveBillwiseCrAmt(popup));
                                dto.setBillRemarks(popup.getBillRemarks());
                                billwiseList.add(dto);
                            }
                        }
                    }
                    if (!billwiseList.isEmpty()) {
                        billwiseBreakupService.insertBillwiseBreakup(billwiseList);
                    }

                    // COST CENTER BREAKUP INSERTION FOR EACH GL ROW
                    List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();
                    for (GlPettyCashPaymentDtlRequestDto dtl : effectiveDtls) {
                        Long mainDetRowId = dtl.getDetRowId();
                        if (dtl.getCostCenterBreakupList() != null && !dtl.getCostCenterBreakupList().isEmpty()) {
                            for (CostCenterBreakupPopupRequestDto popup : dtl.getCostCenterBreakupList()) {
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    actionTypeStr = "isCreated";
                                }
                                if (!"ISCREATED".equals(actionTypeStr.toUpperCase())) continue;
                                CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
                                dto.setGroupPoid(UserContext.getGroupPoid());
                                dto.setCompanyPoid(UserContext.getCompanyPoid());
                                dto.setDocId(documentId);
                                dto.setTransactionPoid(hdrPoid);
                                dto.setGlPoid(dtl.getGlPoid());
                                dto.setMainDetRowId(mainDetRowId);
                                dto.setCostDetRowId(popup.getCostDetRowId());
                                dto.setCostGroup(popup.getCostGroup());
                                dto.setCostPoid(popup.getCostPoid());
                                dto.setAmount(popup.getAmount());
                                dto.setLoginUserPoid(UserContext.getUserPoid());
                                costCenterList.add(dto);
                            }
                        }
                    }
                    if (!costCenterList.isEmpty()) {
                        costCenterBreakupService.saveCostCenterBreakups(costCenterList);
                    }
                }
                case "CUSTOM" -> {
                    // CUSTOM: same DR=CR tally as GENERAL (mirrors legacy lines 1279-1355), no billwise/cost-center
                    if (getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos()).isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    List<GlPettyCashPaymentDtlRequestDto> effectiveDtls = ensurePettyCashGlAndValidateTally(requestDto);
                    var savedPaymentDtls = glPettyCashPaymentDtlRepository.saveAll(
                            mapPaymentDtlsFromList(effectiveDtls, hdrPoid));
                    paymentDtls = mapPaymentResponse(savedPaymentDtls);
                    savedPaymentDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Payment Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                case "SUPPLIER" -> {
                    if (getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos()).isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    validateSupplierCustomerGlMatch(requestDto, "SUPPLIER");
                    var savedPaymentDtls = glPettyCashPaymentDtlRepository.saveAll(
                            mapPaymentDtlsFromList(requestDto.getGlPettyCashPaymentDtlRequestDtos(), hdrPoid));
                    paymentDtls = mapPaymentResponse(savedPaymentDtls);
                    savedPaymentDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Payment Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                case "CUSTOMER" -> {
                    if (getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos()).isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    validateSupplierCustomerGlMatch(requestDto, "CUSTOMER");
                    var savedPaymentDtls = glPettyCashPaymentDtlRepository.saveAll(
                            mapPaymentDtlsFromList(requestDto.getGlPettyCashPaymentDtlRequestDtos(), hdrPoid));
                    paymentDtls = mapPaymentResponse(savedPaymentDtls);
                    savedPaymentDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Payment Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                case "FF JOBS", "FDA JOBS" -> {
                    validateAmountVsChargeTotal(requestDto);
                    var savedChargeDtls = glPettyCashChargeDtlRepository.saveAll(mapChargeDtls(requestDto, hdrPoid));
                    chargeDtls = mapChargeResponse(savedChargeDtls);
                    savedChargeDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Charge Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                case "MTA RFQ", "GENERAL PO" -> {
                    validateAmountVsItemTotal(requestDto);
                    var savedItemDtls = glPettyCashItemDtlRepository.saveAll(mapItemDtls(requestDto, hdrPoid));
                    itemDtls = mapItemResponse(savedItemDtls);
                    savedItemDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Item Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                case "GRN_JOBS" -> {
                    validateAmountVsGrnTotal(requestDto);
                    List<GlPettyCashPaymentGrnDtlRequestDto> activeGrnDtls = filterGrnCheckAll(requestDto.getGlPettyCashGrnDtlRequestDtos());
                    if (activeGrnDtls.isEmpty()) {
                        throw new ValidationException("At least one GRN detail row is required.");
                    }
                    var savedGrnDtls = glPettyCashPaymentGrnDtlRepository.saveAll(mapGrnDtls(activeGrnDtls, hdrPoid));
                    savedGrnDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on GRN Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                default -> throw new IllegalArgumentException("Invalid RefType: " + refType);
            }

            // Load billwise and cost center breakup data for response
            if (refType.equalsIgnoreCase("GENERAL") && !paymentDtls.isEmpty()) {
                Long transPoid = savedHeader.getTransactionPoid();
                Long groupPoid = savedHeader.getGroupPoid();
                Long companyPoid = savedHeader.getCompanyPoid();
                Long userPoid = UserContext.getUserPoid();

                GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                        billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transPoid);

                GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                        costCenterBreakupService.loadCostCenterData(documentId, transPoid, groupPoid, companyPoid, userPoid);

                // Populate billwise and cost center breakup in payment details
                for (GlPettyCashPaymentDtlResponseDto dtl : paymentDtls) {
                    Long detRowId = dtl.getDetRowId();
                    if (null != billwiseResponse && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                        dtl.setBillwiseBreakupList(mapToPopupDto(billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                    if (null != costCenterResponse && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                        dtl.setCostCenterBreakupList(mapToCostCenterPopupDto(costCenterResponse.getCostBreakupList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                }
            }


            loggingService.createLogSummaryEntry(
                    documentId,
                    savedHeader.getTransactionPoid().toString(),
                    String.format("%s %s", LogDetailsEnum.CREATED, savedHeader.getDocRef())
            );

            entityManager.flush();
            String capturedDocId = hasText(UserContext.getDocumentId()) ? UserContext.getDocumentId()
                    : (hasText(requestDto.getDocId()) ? requestDto.getDocId() : "400-101");
            eventPublisher.publishEvent(new PettyCashVoucherSaveEvent(
                    this, requestDto, hdrPoid,
                    UserContext.getGroupPoid(), UserContext.getCompanyPoid(),
                    UserContext.getUserPoid(), capturedDocId,
                    null, null));
            return mapToResponseDto(savedHeader, paymentDtls, chargeDtls, itemDtls);

        } catch (ValidationException | EntityNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error during petty cash creation", e);
            throw new ValidationException("Error during petty cash creation: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    @PerformGlPosting
    @DocAfterSave
    public PettyCashResponseDto updatePettyCash(Long transactionPoid,
                                                PettyCashUpdateRequestDto requestDto, String documentId) {
        try {
            // Step 1: Fetch existing petty cash header
            GlPettyCashPaymentHdr existingHdr = glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new RuntimeException("Petty cash not found with ID: " + transactionPoid));

            // Create copy of old entity for logging
            GlPettyCashPaymentHdr oldEntity = new GlPettyCashPaymentHdr();
            BeanUtils.copyProperties(existingHdr, oldEntity);

            StringBuilder oldRefType = new StringBuilder();
            StringBuilder oldRefPoid = new StringBuilder();
            Long userGroupPoid = UserContext.getGroupPoid();
            Long userCompanyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            //  Step 2: Get old reference type and POID
            pettyCashPaymentVoucherCustomRepository.getOldJobReferences(
                    userGroupPoid,
                    userPoid,
                    userCompanyPoid,
                    requestDto.getDocId(),
                    String.valueOf(transactionPoid),
                    oldRefType,
                    oldRefPoid
            );

            //  Step 3: Validate job before saving (FF/FDA/MTQ)
            StringBuilder validationResult = new StringBuilder();
            String validationRefPoid = resolveRefPoidByType(requestDto, requestDto.getRefType());
            pettyCashPaymentVoucherCustomRepository.validateJobBeforeSave(
                    userGroupPoid,
                    userPoid,
                    userCompanyPoid,
                    requestDto.getDocId(),
                    requestDto.getRefType(),
                    validationRefPoid,
                    validationResult
            );

            assertJobValidationOk(requestDto.getRefType(), validationResult);
            logResult("PROC_GL_JOB_VAL_BEFORE_SAVE", validationResult);

            // Ref-type aware voucher validation
            StringBuilder voucherResult = new StringBuilder();
            String updateVoucherRef = resolveVoucherRefByType(requestDto.getRefType(), requestDto);
            if (hasText(updateVoucherRef)) {
                pettyCashPaymentVoucherCustomRepository.validateGlVouchers(
                        userGroupPoid, userPoid, userCompanyPoid,
                        requestDto.getDocId(), requestDto.getRefType(),
                        updateVoucherRef, voucherResult
                );
                assertVoucherValidationOk(requestDto.getRefType(), voucherResult);
                logResult("PROC_GL_VOUCHERS_VALIDATIONS", voucherResult);
            }

            StringBuilder taxInputGlPoid = new StringBuilder();
            StringBuilder beforeSaveResult = new StringBuilder();
            String updGlPoidStringWithoutTax = "";
            String updGlPoidStringAll = "";
            if (isPaymentDtlRefType(requestDto.getRefType())) {
                updGlPoidStringWithoutTax = buildGlPoidStringWithoutTax(requestDto.getGlPettyCashPaymentDtlRequestDtos());
                updGlPoidStringAll = buildGlPoidStringAll(requestDto.getGlPettyCashPaymentDtlRequestDtos());
            }
            pettyCashPaymentVoucherCustomRepository.validateBeforeSave(
                    userGroupPoid, userPoid, userCompanyPoid,
                    requestDto.getDocId(), requestDto.getRefType(),
                    updGlPoidStringWithoutTax, updGlPoidStringAll,
                    "", safeStatus(requestDto.getStatus()),
                    requestDto.getPettyCashGlPoid(), taxInputGlPoid, beforeSaveResult
            );
            logResult("PROC_GL_DTL_BEFORE_SAVE_VAL_V2", beforeSaveResult);
            validateTaxAndVatRules(requestDto, taxInputGlPoid.toString());
            validateTransactionDate(requestDto.getTransactionDate());
            validateRefPoidRequired(requestDto);
            validateRoundingAmount(requestDto.getRoundingAmount());

            // Step 4: Update parent (header) fields
            validateCashBalance(requestDto, documentId);
            updateHeaderFields(existingHdr, requestDto, userPoid);
            GlPettyCashPaymentHdr updatedHdr = glPettyCashPaymentHdrRepository.save(existingHdr);

            //  Step 5: Merge & save child details partially
            String refType = updatedHdr.getRefType().toUpperCase();

            List<GlPettyCashPaymentDtlResponseDto> paymentDtls = new ArrayList<>();
            List<GlPettyCashChargeDtlResponseDto> chargeDtls = new ArrayList<>();
            List<GLPettyCashItemDtlResponseDto> itemDtls = new ArrayList<>();

            switch (refType) {
                case "GENERAL" -> {
                    if (getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos()).isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    List<GlPettyCashPaymentDtlRequestDto> effectiveDtls = ensurePettyCashGlAndValidateTally(requestDto);
                    var existingDtls = glPettyCashPaymentDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergePaymentDtls(existingDtls, effectiveDtls, transactionPoid);
                    glPettyCashPaymentDtlRepository.saveAll(merged);
                    paymentDtls = mapPaymentResponse(merged);

                    // UPDATE BILLWISE BREAKUP ENTRIES
                    List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
                    for (GlPettyCashPaymentDtlRequestDto dtl : requestDto.getGlPettyCashPaymentDtlRequestDtos()) {
                        if (dtl.getBillwiseBreakupList() != null && !dtl.getBillwiseBreakupList().isEmpty()) {
                            for (BillwiseBreakupPopupRequestDto popup : dtl.getBillwiseBreakupList()) {
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    actionTypeStr = (popup.getBillDetRowId() == null || popup.getBillDetRowId() == 0)
                                            ? "isCreated" : "isUpdated";
                                }
                                String actionType = actionTypeStr.toUpperCase();
                                if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) continue;
                                BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();
                                dto.setGroupPoid(UserContext.getGroupPoid());
                                dto.setCompanyPoid(UserContext.getCompanyPoid());
                                dto.setDocId(documentId);
                                dto.setTransactionPoid(transactionPoid);
                                dto.setLoginUserPoid(userPoid);
                                dto.setGlPoid(dtl.getGlPoid());
                                dto.setMainDetRowId(dtl.getDetRowId());
                                dto.setBillDetRowId(popup.getBillDetRowId());
                                dto.setBillRefType(popup.getBillRefType());
                                dto.setBillRef(popup.getBillRef());
                                dto.setBillDueDate(popup.getBillDueDate());
                                dto.setDrAmt(resolveBillwiseDrAmt(popup));
                                dto.setCrAmt(resolveBillwiseCrAmt(popup));
                                dto.setBillRemarks(popup.getBillRemarks());
                                billwiseList.add(dto);
                            }
                        }
                    }
                    if (!billwiseList.isEmpty()) {
                        billwiseBreakupService.updateBillwiseBreakups(billwiseList, userPoid);
                    }

                    // UPDATE COST CENTER BREAKUP ENTRIES
                    List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();
                    for (GlPettyCashPaymentDtlRequestDto dtl : requestDto.getGlPettyCashPaymentDtlRequestDtos()) {
                        if (dtl.getCostCenterBreakupList() != null && !dtl.getCostCenterBreakupList().isEmpty()) {
                            for (CostCenterBreakupPopupRequestDto popup : dtl.getCostCenterBreakupList()) {
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    actionTypeStr = (popup.getCostDetRowId() == null || popup.getCostDetRowId() == 0)
                                            ? "isCreated" : "isUpdated";
                                }
                                String actionType = actionTypeStr.toUpperCase();
                                if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) continue;
                                CostCenterBreakupRequestDto cc = CostCenterBreakupRequestDto.builder()
                                        .groupPoid(UserContext.getGroupPoid())
                                        .companyPoid(UserContext.getCompanyPoid())
                                        .docId(documentId)
                                        .transactionPoid(transactionPoid)
                                        .mainDetRowId(dtl.getDetRowId())
                                        .glPoid(dtl.getGlPoid())
                                        .costDetRowId(popup.getCostDetRowId())
                                        .costGroup(popup.getCostGroup())
                                        .costPoid(popup.getCostPoid())
                                        .amount(popup.getAmount())
                                        .loginUserPoid(userPoid)
                                        .build();
                                costCenterList.add(cc);
                            }
                        }
                    }
                    if (!costCenterList.isEmpty()) {
                        costCenterBreakupService.updateCostCenterBreakups(costCenterList, userPoid);
                    }
                }
                case "CUSTOM" -> {
                    // CUSTOM: same DR=CR tally as GENERAL (mirrors legacy lines 1279-1355), no billwise/cost-center
                    if (getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos()).isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    List<GlPettyCashPaymentDtlRequestDto> effectiveDtls = ensurePettyCashGlAndValidateTally(requestDto);
                    var existingDtls = glPettyCashPaymentDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergePaymentDtls(existingDtls, effectiveDtls, transactionPoid);
                    glPettyCashPaymentDtlRepository.saveAll(merged);
                    paymentDtls = mapPaymentResponse(merged);
                }
                case "SUPPLIER" -> {
                    if (getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos()).isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    validateSupplierCustomerGlMatch(requestDto, "SUPPLIER");
                    var existingDtls = glPettyCashPaymentDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergePaymentDtls(existingDtls, requestDto.getGlPettyCashPaymentDtlRequestDtos(), transactionPoid);
                    glPettyCashPaymentDtlRepository.saveAll(merged);
                    paymentDtls = mapPaymentResponse(merged);
                }
                case "CUSTOMER" -> {
                    if (getActivePaymentDtls(requestDto.getGlPettyCashPaymentDtlRequestDtos()).isEmpty()) {
                        throw new ValidationException("At least one payment detail row is required.");
                    }
                    validateSupplierCustomerGlMatch(requestDto, "CUSTOMER");
                    var existingDtls = glPettyCashPaymentDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergePaymentDtls(existingDtls, requestDto.getGlPettyCashPaymentDtlRequestDtos(), transactionPoid);
                    glPettyCashPaymentDtlRepository.saveAll(merged);
                    paymentDtls = mapPaymentResponse(merged);
                }
                case "FF JOBS", "FDA JOBS" -> {
                    validateAmountVsChargeTotal(requestDto);
                    var existingDtls = glPettyCashChargeDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergeChargeDtls(existingDtls, requestDto, transactionPoid);
                    glPettyCashChargeDtlRepository.saveAll(merged);
                    chargeDtls = mapChargeResponse(merged);
                }
                case "MTA RFQ", "GENERAL PO" -> {
                    validateAmountVsItemTotal(requestDto);
                    var existingDtls = glPettyCashItemDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergeItemDtls(existingDtls, requestDto, transactionPoid);
                    glPettyCashItemDtlRepository.saveAll(merged);
                    itemDtls = mapItemResponse(merged);
                }
                case "GRN_JOBS" -> {
                    validateAmountVsGrnTotal(requestDto);
                    var existingGrnDtls = glPettyCashPaymentGrnDtlRepository.findByTransactionPoid(transactionPoid);
                    var mergedGrn = mergeGrnDtls(existingGrnDtls, requestDto, transactionPoid);
                    glPettyCashPaymentGrnDtlRepository.saveAll(mergedGrn);
                }
                default -> throw new IllegalArgumentException("Invalid RefType: " + refType);
            }

            String updateDocId = hasText(UserContext.getDocumentId()) ? UserContext.getDocumentId()
                    : (hasText(requestDto.getDocId()) ? requestDto.getDocId() : "400-101");
            eventPublisher.publishEvent(new PettyCashVoucherSaveEvent(
                    this, requestDto, transactionPoid,
                    UserContext.getGroupPoid(), UserContext.getCompanyPoid(),
                    UserContext.getUserPoid(), updateDocId,
                    oldRefType.toString(), oldRefPoid.toString()));

            //  Step 7: Load billwise and cost center breakup data for response
            if (refType.equalsIgnoreCase("GENERAL") && !paymentDtls.isEmpty()) {
                Long transPoid = updatedHdr.getTransactionPoid();
                Long groupPoid = updatedHdr.getGroupPoid();
                Long companyPoid = updatedHdr.getCompanyPoid();
                // userPoid is already declared above

                GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                        billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transPoid);

                GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                        costCenterBreakupService.loadCostCenterData(documentId, transPoid, groupPoid, companyPoid, userPoid);

                // Populate billwise and cost center breakup in payment details
                for (GlPettyCashPaymentDtlResponseDto dtl : paymentDtls) {
                    Long detRowId = dtl.getDetRowId();
                    if (null != billwiseResponse && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                        dtl.setBillwiseBreakupList(mapToPopupDto(billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                    if (null != costCenterResponse && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                        dtl.setCostCenterBreakupList(mapToCostCenterPopupDto(costCenterResponse.getCostBreakupList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                }
            }

            //  Step 8: Return the final response DTO
            
            // Logging for update operation
            loggingService.logChanges(oldEntity, updatedHdr, GlPettyCashPaymentHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

            entityManager.flush();
            return mapToResponseDto(updatedHdr, paymentDtls, chargeDtls, itemDtls);

        } catch (ValidationException | EntityNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error during petty cash update", e);
            throw new ValidationException("Error during petty cash update: " + e.getMessage());
        }
    }


    private void updateHeaderFields(GlPettyCashPaymentHdr header, PettyCashRequestBase dto, Long loginUserPoid) {
        if (dto.getTransactionDate() != null) header.setTransactionDate(dto.getTransactionDate());
        if (dto.getCurrencyCode() != null) header.setCurrencyCode(dto.getCurrencyCode());
        if (dto.getCurrencyRate() != null) header.setCurrencyRate(dto.getCurrencyRate());
        if (dto.getPettyCashGlPoid() != null) header.setPettyCashGlPoid(dto.getPettyCashGlPoid());
        if (dto.getBalance() != null) header.setBalance(dto.getBalance());
        if (dto.getAmount() != null) header.setAmount(dto.getAmount());
        if (dto.getPayingTo() != null) header.setPayingTo(dto.getPayingTo());
        if (dto.getNarration() != null) header.setNarration(dto.getNarration());
        if (dto.getAdvance() != null) header.setAdvance(dto.getAdvance());
        if (dto.getRefType() != null) header.setRefType(dto.getRefType());
        if (dto.getFdaRef() != null) header.setFdaRef(dto.getFdaRef());
        if (dto.getFfRef() != null) header.setFfRef(dto.getFfRef());
        if (dto.getSettledDate() != null) header.setSettledDate(dto.getSettledDate());
        if (dto.getRemarks() != null) header.setRemarks(dto.getRemarks());
        if (dto.getSettledTotal() != null) header.setSettledTotal(dto.getSettledTotal());
        if (dto.getStatus() != null) header.setStatus(dto.getStatus());
        if (dto.getGrandTotal() != null) header.setGrandTotal(dto.getGrandTotal());
        if (dto.getMtaRef() != null) header.setMtaRef(dto.getMtaRef());
        if (dto.getMultiCompany() != null) header.setMultiCompany(dto.getMultiCompany());
        if (dto.getPoRef() != null) header.setPoRef(dto.getPoRef());
        if (dto.getSalesQtnRef() != null) header.setSalesQtnRef(dto.getSalesQtnRef());
        if (dto.getCrTotal() != null) header.setCrTotal(dto.getCrTotal());
        if (dto.getDrTotal() != null) header.setDrTotal(dto.getDrTotal());
        if (dto.getRoundingAmount() != null) header.setRoundingAmount(dto.getRoundingAmount());
        if (dto.getGrnSupplierPoid() != null) header.setGrnSupplierPoid(dto.getGrnSupplierPoid());
        if (dto.getSupplierGlPoid() != null) header.setSupplierGlPoid(dto.getSupplierGlPoid());
        if (dto.getCustomerGlPoid() != null) header.setCustomerGlPoid(dto.getCustomerGlPoid());
        if (dto.getAdvancePettyCashPoid() != null) header.setAdvancePettyCashPoid(dto.getAdvancePettyCashPoid());
        if (dto.getAdvanceStatus() != null) header.setAdvanceStatus(dto.getAdvanceStatus());
        if (dto.getAdvanceAmount() != null) header.setAdvanceAmount(dto.getAdvanceAmount());
        if (dto.getCompanyDivPoid() != null) header.setCompanyDivPoid(dto.getCompanyDivPoid());
    }

    private List<GlPettyCashPaymentDtl> mergePaymentDtls(List<GlPettyCashPaymentDtl> existing,
                                                         List<GlPettyCashPaymentDtlRequestDto> dtlsToMerge,
                                                         Long hdrPoid) {
        Map<Long, GlPettyCashPaymentDtl> existingMap = existing.stream()
                .collect(Collectors.toMap(GlPettyCashPaymentDtl::getDetRowId, d -> d));

        List<GlPettyCashPaymentDtl> toSave = new ArrayList<>();
        List<GlPettyCashPaymentDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GlPettyCashPaymentDtl>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        for (var dto : Optional.ofNullable(dtlsToMerge)
                .orElse(Collections.emptyList())) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = dto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();

            switch (actionType) {
                case "ISCREATED":
                    // Create new record
                    GlPettyCashPaymentDtl newEntity = new GlPettyCashPaymentDtl();
                    newEntity.setTransactionPoid(hdrPoid);
                    
                    // Auto-generate detRowId if not provided
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        // Find max existing detRowId and increment
                        Long maxDetRowId = existing.stream()
                                .map(GlPettyCashPaymentDtl::getDetRowId)
                                .filter(Objects::nonNull)
                                .max(Long::compareTo)
                                .orElse(0L);
                        detRowId = maxDetRowId + 1;
                    }
                    newEntity.setDetRowId(detRowId);
                    newEntity.setType(dto.getType());
                    newEntity.setCompanyPoid(dto.getCompanyPoid());
                    newEntity.setDrAmt(dto.getDrAmt());
                    newEntity.setCrAmt(dto.getCrAmt());
                    newEntity.setVatAmount(dto.getVatAmount());
                    newEntity.setTotalAmount(dto.getTotalAmount());
                    newEntity.setVatSupplier(dto.getVatSupplier());
                    newEntity.setInputVatNumber(dto.getInputVatNumber());
                    newEntity.setSupplierInvDate(dto.getSupplierInvDate());
                    newEntity.setTaxPoid(dto.getTaxPoid());
                    newEntity.setTaxPercentage(dto.getTaxPercentage());
                    newEntity.setVatPartyName(dto.getVatPartyName());
                    newEntity.setRemarks(dto.getRemarks());
                    newEntity.setCreatedBy(getCurrentUser());
                    newEntity.setCreatedDate(LocalDateTime.now());
                    newEntity.setLastModifiedBy(getCurrentUser());
                    newEntity.setLastModifiedDate(LocalDateTime.now());

                    newEntity.setGlPoid(dto.getGlPoid());
                    newEntity.setChargePoid(dto.getChargePoid());

                    toSave.add(newEntity);
                    break;

                case "ISUPDATED":
                    // Update existing record
                    GlPettyCashPaymentDtl existingEntity = existingMap.get(dto.getDetRowId());
                    if (existingEntity == null) {
                        throw new ValidationException("Payment detail not found with detRowId: " + dto.getDetRowId());
                    }
                    
                    // Create copy for logging
                    GlPettyCashPaymentDtl oldEntity = new GlPettyCashPaymentDtl();
                    BeanUtils.copyProperties(existingEntity, oldEntity);
                    
                    existingEntity.setType(dto.getType());
                    existingEntity.setCompanyPoid(dto.getCompanyPoid());
                    existingEntity.setDrAmt(dto.getDrAmt());
                    existingEntity.setCrAmt(dto.getCrAmt());
                    existingEntity.setVatAmount(dto.getVatAmount());
                    existingEntity.setTotalAmount(dto.getTotalAmount());
                    existingEntity.setVatSupplier(dto.getVatSupplier());
                    existingEntity.setInputVatNumber(dto.getInputVatNumber());
                    existingEntity.setSupplierInvDate(dto.getSupplierInvDate());
                    existingEntity.setTaxPoid(dto.getTaxPoid());
                    existingEntity.setTaxPercentage(dto.getTaxPercentage());
                    existingEntity.setVatPartyName(dto.getVatPartyName());
                    existingEntity.setRemarks(dto.getRemarks());
                    existingEntity.setLastModifiedBy(getCurrentUser());
                    existingEntity.setLastModifiedDate(LocalDateTime.now());

                    existingEntity.setGlPoid(dto.getGlPoid());
                    existingEntity.setChargePoid(dto.getChargePoid());

                    toSave.add(existingEntity);
                    
                    // Add to batch logging
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", hdrPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlPettyCashPaymentDtl.class, documentId, hdrPoid.toString(), logDetail));
                    break;

                case "ISDELETED":
                    // Mark for deletion - find entity and add to delete list
                    GlPettyCashPaymentDtl paymentEntityToDelete = existingMap.get(dto.getDetRowId());
                    if (paymentEntityToDelete != null) {
                        toDelete.add(paymentEntityToDelete);
                        // Log the deletion
                        loggingService.logDelete(paymentEntityToDelete, documentId, hdrPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                    // Keep existing record as-is - add to toSave so it appears in response
                    GlPettyCashPaymentDtl unchangedPaymentEntity = existingMap.get(dto.getDetRowId());
                    if (unchangedPaymentEntity != null) {
                        toSave.add(unchangedPaymentEntity);
                    }
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    GlPettyCashPaymentDtl defaultPaymentEntity = existingMap.get(dto.getDetRowId());
                    if (defaultPaymentEntity != null) {
                        toSave.add(defaultPaymentEntity);
                    }
                    break;
            }
        }

        // Delete records marked for deletion - use deleteAll with entity objects (composite key)
        if (!toDelete.isEmpty()) {
            glPettyCashPaymentDtlRepository.deleteAll(toDelete);
        }
        
        // Save all entities
        List<GlPettyCashPaymentDtl> savedEntities = glPettyCashPaymentDtlRepository.saveAll(toSave);
        
        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        
        // Log creation for new records
        savedEntities.stream()
            .filter(entity -> entity.getCreatedDate() != null && entity.getCreatedDate().isAfter(LocalDateTime.now().minusMinutes(1)))
            .forEach(entity -> {
                String logDetail = String.format("Row Created on Payment Detail with detRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
            });

        return savedEntities;
    }

    private List<GlPettyCashChargeDtl> mergeChargeDtls(List<GlPettyCashChargeDtl> existing,
                                                       PettyCashRequestBase requestDto,
                                                       Long hdrPoid) {
        Map<Long, GlPettyCashChargeDtl> existingMap = existing.stream()
                .collect(Collectors.toMap(GlPettyCashChargeDtl::getDetRowId, d -> d));

        List<GlPettyCashChargeDtl> toSave = new ArrayList<>();
        List<GlPettyCashChargeDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GlPettyCashChargeDtl>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        for (var dto : Optional.ofNullable(requestDto.getGlPettyCashChargeDtlRequestDtos())
                .orElse(Collections.emptyList())) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = dto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();

            switch (actionType) {
                case "ISCREATED":
                    if ("N".equalsIgnoreCase(dto.getCheckAll())) break;
                    // Create new record
                    GlPettyCashChargeDtl newEntity = new GlPettyCashChargeDtl();
                    newEntity.setTransactionPoid(hdrPoid);

                    // Auto-generate detRowId if not provided
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        // Find max existing detRowId and increment
                        Long maxDetRowId = existing.stream()
                                .map(GlPettyCashChargeDtl::getDetRowId)
                                .filter(Objects::nonNull)
                                .max(Long::compareTo)
                                .orElse(0L);
                        detRowId = maxDetRowId + 1;
                    }
                    newEntity.setDetRowId(detRowId);
                    newEntity.setDescription(dto.getDescription());
                    newEntity.setRemarks(dto.getRemarks());
                    newEntity.setChargeAmount(dto.getChargeAmount());
                    newEntity.setRefDocId(dto.getRefDocId());
                    newEntity.setRefDocPoid(dto.getRefDocPoid());
                    newEntity.setFdaDetRowId(dto.getFdaDetRowId());
                    newEntity.setCheckAll(dto.getCheckAll());
                    newEntity.setPdaAmount(dto.getPdaAmount());
                    newEntity.setFfAmount(dto.getFfAmount());
                    newEntity.setChargeFrom(resolveChargeFrom(dto.getChargeFrom(), requestDto.getRefType()));
                    newEntity.setVatPartyName(dto.getVatPartyName());
                    newEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    newEntity.setPartyInvDate(dto.getPartyInvDate());
                    newEntity.setTaxPoid(dto.getTaxPoid());
                    newEntity.setCreatedBy(getCurrentUser());
                    newEntity.setCreatedDate(LocalDateTime.now());
                    newEntity.setLastModifiedBy(getCurrentUser());
                    newEntity.setLastModifiedDate(LocalDateTime.now());

                    newEntity.setChargePoid(dto.getChargePoid());

                    toSave.add(newEntity);
                    break;

                case "ISUPDATED":
                    // Update existing record
                    GlPettyCashChargeDtl existingEntity = existingMap.get(dto.getDetRowId());
                    if (existingEntity == null) {
                        throw new ValidationException("Charge detail not found with detRowId: " + dto.getDetRowId());
                    }
                    
                    // Create copy for logging
                    GlPettyCashChargeDtl oldEntity = new GlPettyCashChargeDtl();
                    BeanUtils.copyProperties(existingEntity, oldEntity);
                    
                    existingEntity.setDescription(dto.getDescription());
                    existingEntity.setRemarks(dto.getRemarks());
                    existingEntity.setChargeAmount(dto.getChargeAmount());
                    existingEntity.setRefDocId(dto.getRefDocId());
                    existingEntity.setRefDocPoid(dto.getRefDocPoid());
                    existingEntity.setFdaDetRowId(dto.getFdaDetRowId());
                    existingEntity.setCheckAll(dto.getCheckAll());
                    existingEntity.setPdaAmount(dto.getPdaAmount());
                    existingEntity.setFfAmount(dto.getFfAmount());
                    existingEntity.setChargeFrom(resolveChargeFrom(dto.getChargeFrom(), requestDto.getRefType()));
                    existingEntity.setVatPartyName(dto.getVatPartyName());
                    existingEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    existingEntity.setPartyInvDate(dto.getPartyInvDate());
                    existingEntity.setTaxPoid(dto.getTaxPoid());
                    existingEntity.setLastModifiedBy(getCurrentUser());
                    existingEntity.setLastModifiedDate(LocalDateTime.now());

                    existingEntity.setChargePoid(dto.getChargePoid());

                    toSave.add(existingEntity);
                    
                    // Add to batch logging
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", hdrPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlPettyCashChargeDtl.class, documentId, hdrPoid.toString(), logDetail));
                    break;

                case "ISDELETED":
                    // Mark for deletion - find entity and add to delete list
                    GlPettyCashChargeDtl chargeEntityToDelete = existingMap.get(dto.getDetRowId());
                    if (chargeEntityToDelete != null) {
                        toDelete.add(chargeEntityToDelete);
                        // Log the deletion
                        loggingService.logDelete(chargeEntityToDelete, documentId, hdrPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                    // Keep existing record as-is - add to toSave so it appears in response
                    GlPettyCashChargeDtl unchangedChargeEntity = existingMap.get(dto.getDetRowId());
                    if (unchangedChargeEntity != null) {
                        toSave.add(unchangedChargeEntity);
                    }
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    GlPettyCashChargeDtl defaultChargeEntity = existingMap.get(dto.getDetRowId());
                    if (defaultChargeEntity != null) {
                        toSave.add(defaultChargeEntity);
                    }
                    break;
            }
        }

        // Delete records marked for deletion - use deleteAll with entity objects (composite key)
        if (!toDelete.isEmpty()) {
            glPettyCashChargeDtlRepository.deleteAll(toDelete);
        }
        
        // Save all entities
        List<GlPettyCashChargeDtl> savedEntities = glPettyCashChargeDtlRepository.saveAll(toSave);
        
        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        
        // Log creation for new records
        savedEntities.stream()
            .filter(entity -> entity.getCreatedDate() != null && entity.getCreatedDate().isAfter(LocalDateTime.now().minusMinutes(1)))
            .forEach(entity -> {
                String logDetail = String.format("Row Created on Charge Detail with detRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
            });

        return savedEntities;
    }

    private List<GLPettyCashItemDtl> mergeItemDtls(List<GLPettyCashItemDtl> existing,
                                                   PettyCashRequestBase requestDto,
                                                   Long hdrPoid) {
        Map<Long, GLPettyCashItemDtl> existingMap = existing.stream()
                .collect(Collectors.toMap(GLPettyCashItemDtl::getDetRowId, d -> d));

        List<GLPettyCashItemDtl> toSave = new ArrayList<>();
        List<GLPettyCashItemDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GLPettyCashItemDtl>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        for (var dto : Optional.ofNullable(requestDto.getGlPettyCashItemDtlRequestDtos())
                .orElse(Collections.emptyList())) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = dto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();

            switch (actionType) {
                case "ISCREATED":
                    if ("N".equalsIgnoreCase(dto.getCheckAll())) break;
                    // Create new record
                    GLPettyCashItemDtl newEntity = new GLPettyCashItemDtl();
                    newEntity.setTransactionPoid(hdrPoid);
                    
                    // Auto-generate detRowId if not provided
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        // Find max existing detRowId and increment
                        Long maxDetRowId = existing.stream()
                                .map(GLPettyCashItemDtl::getDetRowId)
                                .filter(Objects::nonNull)
                                .max(Long::compareTo)
                                .orElse(0L);
                        detRowId = maxDetRowId + 1;
                    }
                    newEntity.setDetRowId(detRowId);
                    newEntity.setPoQty(dto.getPoQty());
                    newEntity.setDnQty(dto.getDnQty());
                    newEntity.setQtyReceived(dto.getQtyReceived());
                    newEntity.setPrice(dto.getPrice());
                    newEntity.setDiscount(dto.getDiscount());
                    newEntity.setTotal(dto.getTotal());
                    newEntity.setRemarks(dto.getRemarks());
                    newEntity.setRefDocId(dto.getRefDocId());
                    newEntity.setRefDocPoid(dto.getRefDocPoid());
                    newEntity.setCheckAll(dto.getCheckAll());
                    newEntity.setRefDetRowId(dto.getRefDetRowId());
                    newEntity.setVatPartyName(dto.getVatPartyName());
                    newEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    newEntity.setPartyInvDate(dto.getPartyInvDate());
                    newEntity.setTaxPoid(dto.getTaxPoid());
                    newEntity.setCreatedBy(getCurrentUser());
                    newEntity.setCreatedDate(LocalDateTime.now());
                    newEntity.setLastModifiedBy(getCurrentUser());
                    newEntity.setLastModifiedDate(LocalDateTime.now());

                    newEntity.setStockPoid(dto.getStockPoid());
                    newEntity.setStockUnitPoid(dto.getStockUnitPoid());

                    toSave.add(newEntity);
                    break;

                case "ISUPDATED":
                    // Update existing record
                    GLPettyCashItemDtl existingEntity = existingMap.get(dto.getDetRowId());
                    if (existingEntity == null) {
                        throw new ValidationException("Item detail not found with detRowId: " + dto.getDetRowId());
                    }
                    
                    // Create copy for logging
                    GLPettyCashItemDtl oldEntity = new GLPettyCashItemDtl();
                    BeanUtils.copyProperties(existingEntity, oldEntity);
                    
                    existingEntity.setPoQty(dto.getPoQty());
                    existingEntity.setDnQty(dto.getDnQty());
                    existingEntity.setQtyReceived(dto.getQtyReceived());
                    existingEntity.setPrice(dto.getPrice());
                    existingEntity.setDiscount(dto.getDiscount());
                    existingEntity.setTotal(dto.getTotal());
                    existingEntity.setRemarks(dto.getRemarks());
                    existingEntity.setRefDocId(dto.getRefDocId());
                    existingEntity.setRefDocPoid(dto.getRefDocPoid());
                    existingEntity.setCheckAll(dto.getCheckAll());
                    existingEntity.setRefDetRowId(dto.getRefDetRowId());
                    existingEntity.setVatPartyName(dto.getVatPartyName());
                    existingEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    existingEntity.setPartyInvDate(dto.getPartyInvDate());
                    existingEntity.setTaxPoid(dto.getTaxPoid());
                    existingEntity.setLastModifiedBy(getCurrentUser());
                    existingEntity.setLastModifiedDate(LocalDateTime.now());

                    existingEntity.setStockPoid(dto.getStockPoid());
                    existingEntity.setStockUnitPoid(dto.getStockUnitPoid());

                    toSave.add(existingEntity);
                    
                    // Add to batch logging
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", hdrPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GLPettyCashItemDtl.class, documentId, hdrPoid.toString(), logDetail));
                    break;

                case "ISDELETED":
                    // Mark for deletion - find entity and add to delete list
                    GLPettyCashItemDtl entityToDelete = existingMap.get(dto.getDetRowId());
                    if (entityToDelete != null) {
                        toDelete.add(entityToDelete);
                        // Log the deletion
                        loggingService.logDelete(entityToDelete, documentId, hdrPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                    // Keep existing record as-is - add to toSave so it appears in response
                    GLPettyCashItemDtl unchangedItemEntity = existingMap.get(dto.getDetRowId());
                    if (unchangedItemEntity != null) {
                        toSave.add(unchangedItemEntity);
                    }
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    GLPettyCashItemDtl defaultItemEntity = existingMap.get(dto.getDetRowId());
                    if (defaultItemEntity != null) {
                        toSave.add(defaultItemEntity);
                    }
                    break;
            }
        }

        // Delete records marked for deletion - use deleteAll with entity objects (composite key)
        if (!toDelete.isEmpty()) {
            glPettyCashItemDtlRepository.deleteAll(toDelete);
        }
        
        // Save all entities
        List<GLPettyCashItemDtl> savedEntities = glPettyCashItemDtlRepository.saveAll(toSave);
        
        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        
        // Log creation for new records
        savedEntities.stream()
            .filter(entity -> entity.getCreatedDate() != null && entity.getCreatedDate().isAfter(LocalDateTime.now().minusMinutes(1)))
            .forEach(entity -> {
                String logDetail = String.format("Row Created on Item Detail with detRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
            });

        return savedEntities;
    }


    private void logResult(String procedureName, StringBuilder result) {
        log.info("[{}] => {}", procedureName, result);
        result.setLength(0);  /*clear for next procedure call*/
    }

    /**
     * Mirrors legacy DocumentBeforeEdit closure check on PROC_GL_VOUCHERS_VALIDATIONS:
     * if the result string contains CLOSED, throw a ref-type-aware error message.
     * Also throws if the result starts with ERROR/WARNING.
     */
    private void assertVoucherValidationOk(String refType, StringBuilder result) {
        if (result == null || result.length() == 0) {
            return;
        }
        String response = result.toString();
        String normalized = response.toUpperCase(Locale.ROOT);
        if (normalized.contains("CLOSED")) {
            String label = legacyRefTypeLabel(refType);
            throw new ValidationException(
                    "Corresponding " + label + " is closed, can not edit...");
        }
        if (normalized.startsWith("ERROR") || normalized.startsWith("WARNING")) {
            throw new ValidationException(
                    "PROC_GL_VOUCHERS_VALIDATIONS:- " + response);
        }
    }

    /**
     * Mirrors legacy DocumentBeforeSave closure check on PROC_GL_JOB_VAL_BEFORE_SAVE:
     * if the result contains CLOSED, throw "This {RefType} status is 'Closed', could not save..."
     * Also throws if the result starts with ERROR/WARNING.
     */
    private void assertJobValidationOk(String refType, StringBuilder result) {
        if (result == null || result.length() == 0) {
            return;
        }
        String response = result.toString();
        String normalized = response.toUpperCase(Locale.ROOT);
        if (normalized.contains("CLOSED")) {
            String label = legacyRefTypeLabel(refType);
            throw new ValidationException(
                    "This " + label + " status is 'Closed', could not save...");
        }
        if (normalized.startsWith("ERROR") || normalized.startsWith("WARNING")) {
            throw new ValidationException(
                    "Before Save Validation PROC_GL_JOB_VAL_BEFORE_SAVE:- " + response);
        }
    }

    private String legacyRefTypeLabel(String refType) {
        String normalized = normalizeRefType(refType);
        return switch (normalized) {
            case "FDA JOBS" -> "FDA";
            case "FF JOBS" -> "FF Job";
            case "MTA RFQ" -> "MTA RFQ";
            case "GENERAL PO" -> "General PO";
            case "GRN_JOBS" -> "GRN Job";
            default -> hasText(refType) ? refType : "reference";
        };
    }

    private String resolveRefPoidByType(PettyCashRequestBase requestDto, String refType) {
        return switch (normalizeRefType(refType)) {
            case "FF JOBS" -> requestDto.getFfRef();
            case "FDA JOBS" -> requestDto.getFdaRef();
            case "MTA RFQ" -> requestDto.getSalesQtnRef();
            case "GENERAL PO" -> requestDto.getPoRef();
            case "GRN_JOBS" -> hasText(requestDto.getPoRef()) ? requestDto.getPoRef()
                    : (requestDto.getGrnSupplierPoid() != null
                    ? String.valueOf(requestDto.getGrnSupplierPoid()) : null);
            default -> null;
        };
    }

    private String safeStatus(String status) {
        return status == null ? "" : status;
    }

    private String normalizeRefType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public PettyCashResponseDto findById(Long transactionPoid, String documentId) {

        try {

            GlPettyCashPaymentHdr header = glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new EntityNotFoundException("Header not found for TransactionPoid: " + transactionPoid));

            log.info("Header retrieved successfully: {}", header.getDocRef());

            List<GlPettyCashPaymentDtl> paymentEntities =
                    glPettyCashPaymentDtlRepository.findByTransactionPoid(header.getTransactionPoid());
            List<GlPettyCashChargeDtl> chargeEntities =
                    glPettyCashChargeDtlRepository.findByTransactionPoid(header.getTransactionPoid());
            List<GLPettyCashItemDtl> itemEntities =
                    glPettyCashItemDtlRepository.findByTransactionPoid(header.getTransactionPoid());

            List<GlPettyCashPaymentDtlResponseDto> paymentDtls = mapPaymentResponse(paymentEntities);

            Long transPoid = header.getTransactionPoid();
            Long groupPoid = header.getGroupPoid();
            Long companyPoid = header.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                    billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transPoid);

            GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                    costCenterBreakupService.loadCostCenterData(documentId, transPoid, groupPoid, companyPoid, userPoid);

            for (GlPettyCashPaymentDtlResponseDto dtl : paymentDtls) {
                Long detRowId = dtl.getDetRowId();
                if (null != billwiseResponse && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                    dtl.setBillwiseBreakupList(mapToPopupDto(billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream().filter(x -> x.getMainDetRowId().equals(detRowId)).collect(Collectors.toList())));
                }
                if (null != costCenterResponse && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                    dtl.setCostCenterBreakupList(mapToCostCenterPopupDto(costCenterResponse.getCostBreakupList().stream().filter(x -> x.getMainDetRowId().equals(detRowId)).collect(Collectors.toList())));
                }
            }

            List<GlPettyCashChargeDtlResponseDto> chargeDtls = mapChargeResponse(chargeEntities);
            List<GLPettyCashItemDtlResponseDto> itemDtls = mapItemResponse(itemEntities);

            return mapToResponseDto(header, paymentDtls, chargeDtls, itemDtls);

        } catch (Exception e) {

            throw new ValidationException("Failed to load Petty Cash details: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deletePettyCashVoucher(Long transactionPoid, String docId, String refType, DeleteReasonDto deleteReasonDto) {
        GlPettyCashPaymentHdr header = glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new EntityNotFoundException("Header not found for TransactionPoid: " + transactionPoid));

        Long userGroupPoid = UserContext.getGroupPoid();
        Long userCompanyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        pettyCashPaymentVoucherCustomRepository.validateVoucherBeforeDelete(
                userGroupPoid, userPoid, userCompanyPoid, docId, refType, refType);

        loggingService.logDelete(header, docId, transactionPoid.toString());

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_PETTY_CASH_PAYMENT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                header.getTransactionDate()
        );
    }

    @Override
    public Map<String, Object> listPettyCashVoucher(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "REF_TYPE",
                "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private static BigDecimal scale3(BigDecimal value) {
        return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    public PettyRefTypeResponse<PettyCashFromPoDto> loadPettyCashFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid
    ) {
        log.info("Loading Petty Cash From PO. RFQ POID: {}", rfqPoid);
        PettyRefTypeResponse<PettyCashFromPoDto> response = pettyCashLoadByRefTypeRepository.loadPettyCashFromPo(
                loginGroupPoid, loginCompanyPoid, loginUserPoid, rfqPoid
        );
        enrichPoLoadResponse(response.getResponseList());
        return response;
    }

    @Override
    public PettyRefTypeResponse<PettyCashFromFfDto> loadPettyCashFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    ) {
        log.info("Loading Petty Cash From FF. FF POID: {}", ffPoid);
        PettyRefTypeResponse<PettyCashFromFfDto> response = pettyCashLoadByRefTypeRepository.loadPettyCashFromFf(
                loginGroupPoid, loginCompanyPoid, loginUserPoid, ffPoid
        );
        enrichFfLoadResponse(response.getResponseList());
        return response;
    }

    @Override
    public PettyRefTypeResponse<PettyCashFromFdaDto> loadPettyCashFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid
    ) {
        log.info("Loading Petty Cash From FDA. FDA POID: {}", fdaPoid);
        PettyRefTypeResponse<PettyCashFromFdaDto> response = pettyCashLoadByRefTypeRepository.loadPettyCashFromFda(
                loginGroupPoid, loginCompanyPoid, loginUserPoid, fdaPoid
        );
        enrichFdaLoadResponse(response.getResponseList());
        return response;
    }

    @Override
    public PettyRefTypeResponse<PettyGlBalanceDto> loadPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String documentId,
            Long docKeyPoid,
            String lovName,
            Long lovValue
    ) {
        log.info("Loading Petty Cash GL Balance for DOC_ID: {}, LOV_NAME: {}", documentId, lovName);
        return pettyCashLoadByRefTypeRepository.getPettyGlBalance(
                loginGroupPoid, loginCompanyPoid, loginUserPoid, documentId, docKeyPoid, lovName, lovValue
        );
    }

    @Override
    public PettyRefTypeResponse<PettyCashFromGrnDto> loadPettyCashFromGrn(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String transactionDate,
            String grnSupplierPoid
    ) {
        log.info("Loading Petty Cash From GRN. Supplier POID: {}", grnSupplierPoid);
        PettyRefTypeResponse<PettyCashFromGrnDto> response = pettyCashLoadByRefTypeRepository.loadPettyCashFromGrn(
                loginGroupPoid, loginCompanyPoid, loginUserPoid, transactionDate, grnSupplierPoid
        );
        enrichGrnLoadResponse(response.getResponseList());
        return response;
    }

    @Override
    public PettyRefTypeResponse<PettyCashFromGenrlPoDto> loadPettyCashFromCompletedPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    ) {
        log.info("Loading Petty Cash From Completed PO. PO POID: {}", poPoid);
        PettyRefTypeResponse<PettyCashFromGenrlPoDto> response = pettyCashLoadByRefTypeRepository.loadPettyCashFromCompletedPo(
                loginGroupPoid, loginCompanyPoid, loginUserPoid, poPoid
        );
        enrichCompletedPoLoadResponse(response.getResponseList());
        return response;
    }

    // -----------------------------------------------------------------------
    // Enrich helpers: populate *Dtl LOV fields on load responses
    // -----------------------------------------------------------------------

    private void enrichPoLoadResponse(List<PettyCashFromPoDto> rows) {
        if (rows == null) return;
        rows.forEach(row -> {
            if (row.getStockPoid() != null) {
                stockMasterRepository.findByStockPoid(row.getStockPoid()).ifPresent(s ->
                        row.setStockPoidDtl(new DetailsDto(s.getStockPoid(), s.getStockCode(),
                                s.getStockName(), s.getGroupPoid(), s.getStockDescription(), s.getSeqNo())));
            }
            if (row.getStockUnitPoid() != null) {
                unitMasterRepository.findByUnitPoid(row.getStockUnitPoid()).ifPresent(u ->
                        row.setStockUnitPoidDtl(new DetailsDto(u.getUnitPoid(), u.getUnitCode(),
                                u.getUnitName(), u.getGroupPoid(), u.getUnitName2(), u.getSeqNo())));
            }
            if (row.getTaxPoid() != null) {
                taxMasterRepository.findByTaxPoid(row.getTaxPoid()).ifPresent(t ->
                        row.setTaxPoidDtl(new DetailsDto(t.getTaxPoid(), t.getTaxCode(),
                                t.getTaxName(), t.getGroupPoid(), t.getTaxName2(), t.getSeqNo())));
            }
        });
    }

    private void enrichFfLoadResponse(List<PettyCashFromFfDto> rows) {
        if (rows == null) return;
        rows.forEach(row -> {
            if (row.getChargePoid() != null) {
                shipChargeRepository.findByChargePoid(row.getChargePoid()).ifPresent(c ->
                        row.setChargePoidDtl(new DetailsDto(c.getChargePoid(), c.getChargeCode(),
                                c.getChargeName(), c.getGroupPoid(), c.getChargeName2(), c.getSeqNo())));
            }
            if (row.getTaxPoid() != null) {
                taxMasterRepository.findByTaxPoid(row.getTaxPoid()).ifPresent(t ->
                        row.setTaxPoidDtl(new DetailsDto(t.getTaxPoid(), t.getTaxCode(),
                                t.getTaxName(), t.getGroupPoid(), t.getTaxName2(), t.getSeqNo())));
            }

            if (row.getRefDocPoid() != null) {
                try {
                    LovGetListDto lov = lovService.getDetailsByPoidAndLovName(row.getRefDocPoid(), "FF_JOBNO");
                    if (lov != null && lov.getPoid() != null) {
                        row.setRefDocPoidDtl(new DetailsDto(
                                lov.getPoid(), lov.getCode(), lov.getLabel(),
                                lov.getValue(), lov.getDescription(), lov.getSeqNo()));
                    }
                } catch (NumberFormatException ignored) {
                    // refDocPoid is not a numeric POID — skip enrichment
                }
            }
        });
    }

    private void enrichFdaLoadResponse(List<PettyCashFromFdaDto> rows) {
        if (rows == null) return;
        rows.forEach(row -> {
            if (row.getChargePoid() != null) {
                shipChargeRepository.findByChargePoid(row.getChargePoid()).ifPresent(c ->
                        row.setChargePoidDtl(new DetailsDto(c.getChargePoid(), c.getChargeCode(),
                                c.getChargeName(), c.getGroupPoid(), c.getChargeName2(), c.getSeqNo())));
            }
            if (row.getTaxPoid() != null) {
                taxMasterRepository.findByTaxPoid(row.getTaxPoid()).ifPresent(t ->
                        row.setTaxPoidDtl(new DetailsDto(t.getTaxPoid(), t.getTaxCode(),
                                t.getTaxName(), t.getGroupPoid(), t.getTaxName2(), t.getSeqNo())));
            }
            if (row.getRefDocPoid() != null && !row.getRefDocPoid().isBlank()) {
                try {
                    Long refDocPoid = Long.parseLong(row.getRefDocPoid().trim());
                    LovGetListDto lov = lovService.getDetailsByPoidAndLovName(refDocPoid, "PROCESS_FDA_IN_PI");
                    if (lov != null && lov.getPoid() != null) {
                        row.setRefDocPoidDtl(new DetailsDto(
                                lov.getPoid(), lov.getCode(), lov.getLabel(),
                                lov.getValue(), lov.getDescription(), lov.getSeqNo()));
                    }
                } catch (NumberFormatException ignored) {
                    // refDocPoid is not a numeric POID — skip enrichment
                }
            }
        });
    }

    private void enrichGrnLoadResponse(List<PettyCashFromGrnDto> rows) {
        if (rows == null) return;
        rows.forEach(row -> {
            if (row.getSupplierPoid() != null) {
                SupplierMasterEntity supplier = supplierMasterRepository.findBySupplierPoid(row.getSupplierPoid());
                if (supplier != null) {
                    row.setSupplierPoidDtl(new DetailsDto(supplier.getSupplierPoid(), supplier.getSupplierCode(),
                            supplier.getSupplierName(), supplier.getGroupPoid(), supplier.getSupplierName2(),
                            supplier.getSeqNo() != null ? supplier.getSeqNo().intValue() : null));
                }
            }
            if (row.getLocationPoid() != null) {
                LovGetListDto loc = lovService.getDetailsByPoidAndLovName(row.getLocationPoid(), "LOCATION");
                if (loc != null && loc.getPoid() != null) {
                    row.setLocationPoidDtl(new DetailsDto(loc.getPoid(), loc.getCode(), loc.getLabel(),
                            loc.getValue(), loc.getDescription(), loc.getSeqNo()));
                }
            }
        });
    }

    private void enrichCompletedPoLoadResponse(List<PettyCashFromGenrlPoDto> rows) {
        if (rows == null) return;
        rows.forEach(row -> {
            if (row.getStockPoid() != null) {
                stockMasterRepository.findByStockPoid(row.getStockPoid()).ifPresent(s ->
                        row.setStockPoidDtl(new DetailsDto(s.getStockPoid(), s.getStockCode(),
                                s.getStockName(), s.getGroupPoid(), s.getStockDescription(), s.getSeqNo())));
            }
            if (row.getStockUnitPoid() != null) {
                unitMasterRepository.findByUnitPoid(row.getStockUnitPoid()).ifPresent(u ->
                        row.setStockUnitPoidDtl(new DetailsDto(u.getUnitPoid(), u.getUnitCode(),
                                u.getUnitName(), u.getGroupPoid(), u.getUnitName2(), u.getSeqNo())));
            }
        });
    }

    @Override
    public List<AdvanceDetailDto> loadAdvanceDetails(BigDecimal amount, String advancePoid) {
        StringBuilder result = new StringBuilder();
        List<AdvanceDetailDto> outData = new ArrayList<>();
        pettyCashPaymentVoucherCustomRepository.loadAdvanceDetails(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                amount,
                advancePoid,
                result,
                outData
        );
        logResult("PROC_GL_PETTY_ADVANCE_DTLLOAD", result);
        return outData;
    }

    @Override
    public List<String> getAllowedRefTypes(Long userPoid) {
        log.info("Getting allowed ref types for userPoid: {}", userPoid);
        String whereClause = pettyCashPaymentVoucherCustomRepository.getRefTypeWhereClause(userPoid);
        if (whereClause == null || whereClause.isBlank()) {
            return Collections.emptyList();
        }
        // The procedure returns a SQL IN-clause string e.g. "'GENERAL','FF JOBS','FDA JOBS'"
        // Parse it into a clean list of values
        return Arrays.stream(whereClause.split(","))
                .map(s -> s.trim().replace("'", ""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    @Override
    public PettyCashGlobalParamsDto getPettyCashGlobalParams(Long pettyCashGlPoid) {
        // Ledger-specific params use the petty cash GL POID as the key; fall back to "1" when not provided
        String ledgerKey = pettyCashGlPoid != null ? pettyCashGlPoid.toString() : "1";

        return PettyCashGlobalParamsDto.builder()
                .defaultPayingTo(globalParameterService.getParameterValue(
                        "PETTY_CASH_DEFAULT_PAYING_TO", "GROUP", "1", ""))
                .defaultRefType(globalParameterService.getParameterValue(
                        "DEFAULT_PETTY_CASH_REF_TYPE", "GROUP", "1", "GENERAL"))
                .vatRelatedFieldsVisible("TRUE".equalsIgnoreCase(globalParameterService.getParameterValue(
                        "PETTY_CASH_GL_VAT_RELATED_FIELDS", "GROUP", "1", "FALSE")))
                .roundingLimit(getConfiguredDecimal("ROUNDING_LIMIT", BigDecimal.ZERO))
                .vatAmountLimit(getConfiguredDecimal("PETTY_CASH_VAT_AMOUNT_LIMIT", BigDecimal.ZERO))
                .inputTaxVarianceLimit(getConfiguredDecimal("INPUT_TAX_VARIANCE_LIMIT", BigDecimal.ZERO))
                .mtaPettyCashGlCode(globalParameterService.getParameterValue(
                        "MTA_PETTY_CASH_GL_CODE", "GROUP", "1", ""))
                .advanceLedgerGlPoid(parseLongOrNull(globalParameterService.getParameterValue(
                        "PETTY_CASH_ADVANCE_LEDGER", "GROUP", ledgerKey, "0")))
                .pettyCashLedgerGlPoid(parseLongOrNull(globalParameterService.getParameterValue(
                        "PETTY_CASH_LEDGER", "GROUP", ledgerKey, "0")))
                .advRefundAutoApproval("Y".equalsIgnoreCase(globalParameterService.getParameterValue(
                        "PETTY_CASH_ADV_REFND_APPR_SUBMN", "GROUP", "1", "N")))
                .build();
    }

    private void validateCashBalance(PettyCashRequestBase requestDto, String documentId) {
        if (requestDto.getPettyCashGlPoid() == null || requestDto.getAmount() == null) {
            return;
        }
        List<PettyGlBalanceDto> balanceList = pettyCashLoadByRefTypeRepository.getPettyGlBalance(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                documentId,
                requestDto.getPettyCashGlPoid(),
                null,
                0L
        ).getResponseList();
        if (balanceList == null || balanceList.isEmpty()) {
            return;
        }
        BigDecimal balance = balanceList.get(0).getBalance();
        if (balance != null && requestDto.getAmount().compareTo(balance) > 0) {
            throw new ValidationException(
                    "Paid amount (" + requestDto.getAmount().toPlainString()
                    + ") exceeds available petty cash balance (" + balance.toPlainString() + ").");
        }
    }

    private List<BillwiseBreakupPopupRequestDto> mapToPopupDto(List<LoadBillwiseBreakupResponseDto> list) {

        if (list == null) return Collections.emptyList();

        return list.stream().map(src -> {
            BillwiseBreakupPopupRequestDto.BillwiseBreakupPopupRequestDtoBuilder builder =
                    BillwiseBreakupPopupRequestDto.builder()
                            .billDetRowId(src.getBillDetRowId())
                            .billRefType(src.getBillRefType())
                            .billRef(src.getBillRef())
                            .billDueDate(src.getBillDueDate())
                            .billRemarks(src.getBillRemarks())
                            .actionType("noChanges"); // Default actionType for loaded data

            // Set type and amount based on which one has value
            if (src.getDrAmt() != null && src.getDrAmt().compareTo(BigDecimal.ZERO) > 0) {
                builder.type("DR");
                builder.amount(scale3(src.getDrAmt()));
            } else if (src.getCrAmt() != null && src.getCrAmt().compareTo(BigDecimal.ZERO) > 0) {
                builder.type("CR");
                builder.amount(scale3(src.getCrAmt()));
            } else {
                // Default to DR if both are zero/null
                builder.type("DR");
                builder.amount(src.getDrAmt() != null ? scale3(src.getDrAmt()) : BigDecimal.ZERO);
            }

            return builder.build();
        }).collect(Collectors.toList());
    }

    private List<PendingBillwiseBreakupDto> mapToPendingDto(
            List<ShowPendingBillwiseBreakupResponseDto> list) {

        if (list == null) return Collections.emptyList();

        return list.stream().map(src ->
                PendingBillwiseBreakupDto.builder()
                        .billRef(src.getBillRef())
                        .billDueDate(src.getBillDueDate())
                        .remarks(src.getRemarks())
                        .balance(src.getBalance())
                        .build()
        ).collect(Collectors.toList());
    }

    private List<CostCenterBreakupPopupRequestDto> mapToCostCenterPopupDto(List<CostCenterBreakupResponseDto> list) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .map(cc -> {
                    CostCenterBreakupPopupRequestDto dto = CostCenterBreakupPopupRequestDto.builder()
                            .costDetRowId(cc.getCostDetRowId())
                            .costGroup(cc.getCostGroup())
                            .costPoid(cc.getCostPoid())
                            .amount(scale3(cc.getAmount()))
                            .actionType("noChanges")
                            .build();
                    if (cc.getCostPoid() != null && !cc.getCostPoid().isEmpty() && 
                        cc.getCostGroup() != null && !cc.getCostGroup().isEmpty()) {


                        if (StringUtils.isNotEmpty(cc.getCostPoid()) && StringUtils.isNotEmpty(cc.getCostGroup())) {
                            try {

                                LovGetListDto lovGetListDto = lovService.getDetailsByCodeAndLovName(cc.getCostPoid(), cc.getCostGroup());
                                if (lovGetListDto.getPoid() == null) {
                                    Long poid = Long.parseLong(cc.getCostPoid());
                                    lovGetListDto = lovService.getDetailsByPoidAndLovName(poid, cc.getCostGroup());
                                }
                                dto.setCostCenterDetails(lovGetListDto);
                            } catch (NumberFormatException e) {
                                dto.setCostCenterDetails(lovService.getDetailsByCodeAndLovName(cc.getCostPoid(), cc.getCostGroup()));
                            }
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-101");
        params.put("SUB_PAYMENT_DTL", printService.load("Finance/GL/PettyCashPaymentDtl_subreport1.jrxml"));
        params.put("SUB_ITEM_DTL_2", printService.load("Finance/GL/PettyCashPaymentItemSubreport2.jrxml"));
        params.put("SUB_ITEM_DTL_1", printService.load("Finance/GL/PettyCashPaymentItemSubreport1.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/PettyCashPayment.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal resolveBillwiseDrAmt(BillwiseBreakupPopupRequestDto popup) {
        String type = popup == null ? "" : trim(popup.getType()).toUpperCase(Locale.ROOT);
        BigDecimal amount = popup == null ? BigDecimal.ZERO : safe(popup.getAmount());
        return "CR".equals(type) ? BigDecimal.ZERO : amount;
    }

    private BigDecimal resolveBillwiseCrAmt(BillwiseBreakupPopupRequestDto popup) {
        String type = popup == null ? "" : trim(popup.getType()).toUpperCase(Locale.ROOT);
        BigDecimal amount = popup == null ? BigDecimal.ZERO : safe(popup.getAmount());
        return "CR".equals(type) ? amount : BigDecimal.ZERO;
    }

    private void validateTaxAndVatRules(PettyCashRequestBase requestDto, String taxInputGlPoidValue) {
        if (!isVatValidationApplicable(requestDto.getRefType())) {
            return;
        }

        List<GlPettyCashPaymentDtlRequestDto> details = Optional.ofNullable(requestDto.getGlPettyCashPaymentDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(this::isNotDeletedAction)
                .toList();

        if (details.isEmpty()) {
            return;
        }

        BigDecimal vatAmountLimit = getConfiguredDecimal("PETTY_CASH_VAT_AMOUNT_LIMIT", BigDecimal.ZERO);
        BigDecimal inputTaxVarianceLimit = getConfiguredDecimal("INPUT_TAX_VARIANCE_LIMIT", BigDecimal.ZERO);
        Long inputTaxGlPoid = parseLongOrNull(hasText(taxInputGlPoidValue)
                ? taxInputGlPoidValue
                : globalParameterService.getParameterValue("TAX_INPUT_GL_POID", "TAX", "1", "0"));

        BigDecimal hundred = BigDecimal.valueOf(100);
        for (int i = 0; i < details.size(); i++) {
            GlPettyCashPaymentDtlRequestDto row = details.get(i);
            int rowNum = i + 1;
            BigDecimal drAmt = safe(row.getDrAmt());
            BigDecimal crAmt = safe(row.getCrAmt());
            BigDecimal vatAmount = safe(row.getVatAmount());
            BigDecimal taxPercentage = safe(row.getTaxPercentage());

            if (vatAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (row.getVatSupplier() == null || row.getVatSupplier() <= 0) {
                    throw new ValidationException("VAT supplier not found. Please note the row number " + rowNum);
                }
                if (!hasText(row.getInputVatNumber())) {
                    throw new ValidationException("Input VAT Number not found. Please note the row number " + rowNum);
                }
                if (row.getSupplierInvDate() == null) {
                    throw new ValidationException("Supplier Invoice Date not found. Please note the row number " + rowNum);
                }
                if (vatAmountLimit.compareTo(BigDecimal.ZERO) > 0 && drAmt.compareTo(vatAmountLimit) > 0) {
                    throw new ValidationException("Cash Purchase having VAT should be within "
                            + vatAmountLimit.stripTrailingZeros().toPlainString()
                            + "BD. Please note the row number " + rowNum);
                }
            }

            if (inputTaxGlPoid != null && inputTaxGlPoid > 0 &&
                    row.getGlPoid() != null && row.getGlPoid().equals(inputTaxGlPoid)) {
                BigDecimal amount = drAmt.compareTo(BigDecimal.ZERO) != 0 ? drAmt : crAmt.abs();
                if (vatAmountLimit.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(vatAmountLimit) > 0) {
                    throw new ValidationException("Cash Purchase having VAT should be within "
                            + vatAmountLimit.stripTrailingZeros().toPlainString()
                            + "BD. Please note the row number " + rowNum);
                }
            }

            if (inputTaxVarianceLimit.compareTo(BigDecimal.ZERO) > 0 &&
                    drAmt.compareTo(BigDecimal.ZERO) > 0 &&
                    taxPercentage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal expectedTaxAmount = drAmt.multiply(taxPercentage)
                        .divide(hundred, 3, RoundingMode.HALF_UP);
                BigDecimal difference = vatAmount.subtract(expectedTaxAmount).abs();
                if (difference.compareTo(inputTaxVarianceLimit) > 0) {
                    throw new ValidationException("WARNING : Input tax difference ("
                            + difference.stripTrailingZeros().toPlainString()
                            + "/-) should be within "
                            + inputTaxVarianceLimit.stripTrailingZeros().toPlainString()
                            + "/- Please note the row number " + rowNum);
                }
            }
        }
    }

    private boolean isVatValidationApplicable(String refType) {
        String normalized = normalizeRefType(refType);
        return "GENERAL".equals(normalized)
                || "CUSTOM".equals(normalized)
                || "SUPPLIER".equals(normalized)
                || "CUSTOMER".equals(normalized);
    }

    private boolean isNotDeletedAction(GlPettyCashPaymentDtlRequestDto row) {
        String action = row == null || row.getActionType() == null ? "" : row.getActionType().trim().toUpperCase(Locale.ROOT);
        return !"ISDELETED".equals(action);
    }

    private BigDecimal getConfiguredDecimal(String paramName, BigDecimal defaultValue) {
        String value = globalParameterService.getParameterValue(paramName, "GROUP", "1", defaultValue.toPlainString());
        if (!hasText(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException(paramName + " parameter is not configured correctly.");
        }
    }

    private Long parseLongOrNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // New helper methods — legacy bean flow parity
    // -----------------------------------------------------------------------

    /**
     * Applies global-parameter defaults to a newly built header when the
     * corresponding fields were not supplied by the caller (DocumentAfterNew parity).
     */
    private void applyNewDocumentDefaults(GlPettyCashPaymentHdr header) {
        if (!hasText(header.getPayingTo())) {
            String defaultPayingTo = globalParameterService.getParameterValue(
                    "PETTY_CASH_DEFAULT_PAYING_TO", "GROUP", "1", null);
            if (hasText(defaultPayingTo)) {
                header.setPayingTo(defaultPayingTo);
            }
        }
        if (!hasText(header.getRefType())) {
            String defaultRefType = globalParameterService.getParameterValue(
                    "DEFAULT_PETTY_CASH_REF_TYPE", "GROUP", "1", null);
            if (hasText(defaultRefType)) {
                header.setRefType(defaultRefType);
            }
        }
    }

    /**
     * Validates that the transaction date is within the configured post-date and
     * back-date windows (PETTY_CASH_VALIDATION_DAYS / PETTY_CASH_BACK_DATE_VALIDATION_DAYS).
     */
    private void validateRefPoidRequired(PettyCashRequestBase req) {
        switch (normalizeRefType(req.getRefType())) {
            case "FDA JOBS" -> {
                if (!hasText(req.getFdaRef()))
                    throw new ValidationException("Select a FDA Ref...");
            }
            case "FF JOBS" -> {
                if (!hasText(req.getFfRef()))
                    throw new ValidationException("Select a FF Ref...");
            }
            case "MTA RFQ" -> {
                if (!hasText(req.getSalesQtnRef()))
                    throw new ValidationException("Select a MTA RFQ Ref...");
            }
        }
    }

    private void validateTransactionDate(LocalDate transactionDate) {
        if (transactionDate == null) return;
        LocalDate today = LocalDate.now();

        String postDaysStr = globalParameterService.getParameterValue(
                "PETTY_CASH_VALIDATION_DAYS", "GROUP", "1", "0");
        try {
            int postDays = Integer.parseInt(postDaysStr.trim());
            if (postDays > 0) {
                long daysAhead = ChronoUnit.DAYS.between(today, transactionDate);
                if (daysAhead > postDays) {
                    throw new ValidationException(
                            "Pettycash Payments not allowed more than " + postDays + " days post dated...");
                }
            }
        } catch (NumberFormatException ignored) {}

        String backDaysStr = globalParameterService.getParameterValue(
                "PETTY_CASH_BACK_DATE_VALIDATION_DAYS", "GROUP", "1", "0");
        try {
            int backDays = Integer.parseInt(backDaysStr.trim());
            if (backDays > 0) {
                long daysBehind = ChronoUnit.DAYS.between(transactionDate, today);
                if (daysBehind > backDays) {
                    throw new ValidationException(
                            "Pettycash Payments not allowed less than " + backDays + " days back dated...");
                }
            }
        } catch (NumberFormatException ignored) {}
    }

    /**
     * Validates that the rounding amount does not exceed the ROUNDING_LIMIT parameter.
     */
    private void validateRoundingAmount(BigDecimal roundingAmount) {
        if (roundingAmount == null || roundingAmount.compareTo(BigDecimal.ZERO) == 0) return;
        BigDecimal roundingLimit = getConfiguredDecimal("ROUNDING_LIMIT", BigDecimal.ZERO);
        if (roundingLimit.compareTo(BigDecimal.ZERO) > 0
                && roundingAmount.abs().compareTo(roundingLimit) > 0) {
            throw new ValidationException(
                    "RoundingAmount is greater than " + roundingLimit.toPlainString());
        }
    }

    /**
     * Validates that paid amount == sum(chargeAmount) + rounding for FDA JOBS / FF JOBS.
     */
    private void validateAmountVsChargeTotal(PettyCashRequestBase requestDto) {
        BigDecimal amount = safe(requestDto.getAmount());
        BigDecimal rounding = safe(requestDto.getRoundingAmount());
        List<GlPettyCashChargeDtlRequestDto> activeRows = Optional.ofNullable(requestDto.getGlPettyCashChargeDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                .filter(d -> !"N".equalsIgnoreCase(d.getCheckAll()))
                .collect(Collectors.toList());
        if (activeRows.isEmpty()) {
            throw new ValidationException("No Details in this Transaction...");
        }
        BigDecimal chargeTotal = activeRows.stream()
                .map(d -> safe(d.getChargeAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.compareTo(chargeTotal.add(rounding)) != 0) {
            throw new ValidationException(
                    "Paid Amount(" + amount.toPlainString() + ") is not matching with Total Amount("
                            + chargeTotal.toPlainString() + ")...");
        }
    }

    /**
     * Validates that paid amount == sum(item total) + rounding for MTA RFQ / GENERAL PO.
     */
    private void validateAmountVsItemTotal(PettyCashRequestBase requestDto) {
        BigDecimal amount = safe(requestDto.getAmount());
        BigDecimal rounding = safe(requestDto.getRoundingAmount());
        List<GlPettyCashItemDtlRequestDto> activeRows = Optional.ofNullable(requestDto.getGlPettyCashItemDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                .filter(d -> !"N".equalsIgnoreCase(d.getCheckAll()))
                .collect(Collectors.toList());
        if (activeRows.isEmpty()) {
            throw new ValidationException("No Details in this Transaction...");
        }
        BigDecimal itemTotal = activeRows.stream()
                .map(d -> safe(d.getTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.compareTo(itemTotal.add(rounding)) != 0) {
            throw new ValidationException(
                    "Paid Amount(" + amount.toPlainString() + ") is not matching with Total Amount("
                            + itemTotal.toPlainString() + ")...");
        }
    }

    /**
     * For SUPPLIER / CUSTOMER: validates that the party GL poid appears as a DR row and
     * the DR amount for that GL matches the paid amount.
     */
    private void validateSupplierCustomerGlMatch(PettyCashRequestBase requestDto, String partyType) {
        Long partyGlPoid = "SUPPLIER".equalsIgnoreCase(partyType)
                ? requestDto.getSupplierGlPoid()
                : requestDto.getCustomerGlPoid();
        if (partyGlPoid == null) return;

        List<GlPettyCashPaymentDtlRequestDto> activeDtls =
                Optional.ofNullable(requestDto.getGlPettyCashPaymentDtlRequestDtos())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                        .collect(Collectors.toList());

        boolean partyGlFound = activeDtls.stream()
                .anyMatch(d -> "Dr".equalsIgnoreCase(d.getType()) && partyGlPoid.equals(d.getGlPoid()));
        if (!partyGlFound) {
            String notFoundMsg = "SUPPLIER".equalsIgnoreCase(partyType)
                    ? "Selected Supplier and Debited supplier are not matching...."
                    : "Selected Customer and Debited Customer are not matching....";
            throw new ValidationException(notFoundMsg);
        }

        // Use TotalAmount (includes VAT) to match legacy lines 1221-1234
        BigDecimal drTotal = activeDtls.stream()
                .filter(d -> "Dr".equalsIgnoreCase(d.getType()) && partyGlPoid.equals(d.getGlPoid()))
                .map(d -> safe(d.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = safe(requestDto.getAmount());
        if (drTotal.compareTo(amount) != 0) {
            throw new ValidationException(
                    "Paid Amount (" + amount.toPlainString()
                            + ") is not matching with Supplier GL Debit Amount(" + drTotal.toPlainString() + ")....");
        }
    }

    /**
     * For GENERAL type: auto-inserts the petty cash GL as a CR row if it is absent,
     * then validates DR = CR + rounding tally.
     * Returns the effective list of payment detail DTOs (may include the auto-added row).
     */
    private List<GlPettyCashPaymentDtlRequestDto> ensurePettyCashGlAndValidateTally(
            PettyCashRequestBase requestDto) {

        List<GlPettyCashPaymentDtlRequestDto> allDtls = new ArrayList<>(
                Optional.ofNullable(requestDto.getGlPettyCashPaymentDtlRequestDtos())
                        .orElse(Collections.emptyList()));

        List<GlPettyCashPaymentDtlRequestDto> activeDtls = allDtls.stream()
                .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                .collect(Collectors.toList());

        BigDecimal amount = safe(requestDto.getAmount());
        Long pettyCashGlPoid = requestDto.getPettyCashGlPoid();

        // Auto-insert CR row for petty cash GL if not already present;
        // if already present, assert its TotalAmount == header Amount (mirrors legacy line 1021).
        if (pettyCashGlPoid != null) {
            Optional<GlPettyCashPaymentDtlRequestDto> existingCr = activeDtls.stream()
                    .filter(d -> "Cr".equalsIgnoreCase(d.getType())
                            && pettyCashGlPoid.equals(d.getGlPoid()))
                    .findFirst();
            if (existingCr.isEmpty()) {
                long maxId = activeDtls.stream()
                        .mapToLong(d -> d.getDetRowId() != null ? d.getDetRowId() : 0L)
                        .max().orElse(0L);
                GlPettyCashPaymentDtlRequestDto crRow = GlPettyCashPaymentDtlRequestDto.builder()
                        .detRowId(maxId + 1)
                        .type("Cr")
                        .glPoid(pettyCashGlPoid)
                        .crAmt(amount)
                        .totalAmount(amount)
                        .actionType("isCreated")
                        .build();
                allDtls.add(crRow);
                activeDtls.add(crRow);
            } else {
                // Legacy assertion: the cash-CR row's TotalAmount must equal header Amount
                BigDecimal cashCrTotal = safe(existingCr.get().getTotalAmount());
                if (cashCrTotal.compareTo(amount) != 0) {
                    throw new ValidationException(
                            "Paid Amount (" + amount.toPlainString()
                            + ") is not matching with Total Amount (" + cashCrTotal.toPlainString() + ")....");
                }
            }
        }

        // Validate DR = CR tally using TotalAmount (includes VAT), mirrors legacy lines 973-983.
        // Legacy: when DrAmt != null → sum TotalAmount to DR side; when CrAmt != null → sum TotalAmount to CR side.
        BigDecimal drTotal = activeDtls.stream()
                .filter(d -> d.getDrAmt() != null)
                .map(d -> safe(d.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal crTotal = activeDtls.stream()
                .filter(d -> d.getCrAmt() != null)
                .map(d -> safe(d.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (drTotal.compareTo(crTotal) != 0) {
            throw new ValidationException(
                    "Total Debit(" + drTotal.toPlainString() + ") Amounts and Credit("
                            + crTotal.toPlainString() + ") Amounts are not tallying...");
        }

        return allDtls;
    }

    /**
     * Maps a list of payment detail DTOs directly to entities (for use with effectiveDtls
     * that may include auto-added rows not present in the original request).
     */
    private List<GlPettyCashPaymentDtl> mapPaymentDtlsFromList(
            List<GlPettyCashPaymentDtlRequestDto> dtlList, Long hdrPoid) {
        List<GlPettyCashPaymentDtlRequestDto> filteredDtos = Optional.ofNullable(dtlList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(dtl -> {
                    String at = dtl.getActionType();
                    if (at == null || at.trim().isEmpty()) return true;
                    return "ISCREATED".equals(at.toUpperCase());
                })
                .collect(Collectors.toList());

        final long[] counter = {1};
        return filteredDtos.stream()
                .map(dtl -> {
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) detRowId = counter[0]++;
                    return GlPettyCashPaymentDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(detRowId)
                            .type(dtl.getType())
                            .companyPoid(dtl.getCompanyPoid())
                            .glPoid(dtl.getGlPoid())
                            .chargePoid(dtl.getChargePoid())
                            .drAmt(dtl.getDrAmt())
                            .crAmt(dtl.getCrAmt())
                            .vatAmount(dtl.getVatAmount())
                            .totalAmount(dtl.getTotalAmount())
                            .vatSupplier(dtl.getVatSupplier())
                            .inputVatNumber(dtl.getInputVatNumber())
                            .supplierInvDate(dtl.getSupplierInvDate())
                            .taxPoid(dtl.getTaxPoid())
                            .taxPercentage(dtl.getTaxPercentage())
                            .vatPartyName(dtl.getVatPartyName())
                            .remarks(dtl.getRemarks())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Helper: resolveVoucherRefByType
    // -----------------------------------------------------------------------

    private String resolveVoucherRefByType(String refType, PettyCashRequestBase requestDto) {
        return switch (normalizeRefType(refType)) {
            case "FDA JOBS" -> requestDto.getFdaRef();
            case "FF JOBS" -> requestDto.getFfRef();
            case "MTA RFQ" -> requestDto.getSalesQtnRef();
            case "GENERAL PO" -> requestDto.getPoRef();
            case "GRN_JOBS" -> hasText(requestDto.getPoRef()) ? requestDto.getPoRef()
                    : (requestDto.getGrnSupplierPoid() != null
                    ? String.valueOf(requestDto.getGrnSupplierPoid()) : null);
            default -> null;
        };
    }

    // -----------------------------------------------------------------------
    // Helper: GL poid string builders and related helpers
    // -----------------------------------------------------------------------

    private boolean isPaymentDtlRefType(String refType) {
        String n = normalizeRefType(refType);
        return "GENERAL".equals(n) || "CUSTOM".equals(n) || "SUPPLIER".equals(n) || "CUSTOMER".equals(n);
    }

    private String buildGlPoidStringWithoutTax(List<GlPettyCashPaymentDtlRequestDto> dtls) {
        return Optional.ofNullable(dtls).orElse(Collections.emptyList()).stream()
                .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                .filter(d -> d.getTaxPoid() == null)
                .filter(d -> d.getGlPoid() != null)
                .map(d -> String.valueOf(d.getGlPoid()))
                .collect(Collectors.joining(","));
    }

    private String buildGlPoidStringAll(List<GlPettyCashPaymentDtlRequestDto> dtls) {
        return Optional.ofNullable(dtls).orElse(Collections.emptyList()).stream()
                .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                .filter(d -> d.getGlPoid() != null)
                .map(d -> String.valueOf(d.getGlPoid()))
                .collect(Collectors.joining(","));
    }

    private String resolveChargeFrom(String existing, String refType) {
        if (hasText(existing)) return existing;
        return switch (normalizeRefType(refType)) {
            case "FF JOBS" -> "FF";
            case "FDA JOBS" -> "FDA";
            default -> null;
        };
    }

    private List<GlPettyCashPaymentDtlRequestDto> getActivePaymentDtls(List<GlPettyCashPaymentDtlRequestDto> dtls) {
        return Optional.ofNullable(dtls).orElse(Collections.emptyList()).stream()
                .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Helper: GRN-specific methods
    // -----------------------------------------------------------------------

    private void validateAmountVsGrnTotal(PettyCashRequestBase requestDto) {
        BigDecimal amount = safe(requestDto.getAmount());
        BigDecimal rounding = safe(requestDto.getRoundingAmount());
        List<GlPettyCashPaymentGrnDtlRequestDto> activeRows = filterGrnCheckAll(requestDto.getGlPettyCashGrnDtlRequestDtos())
                .stream()
                .filter(d -> !"ISDELETED".equalsIgnoreCase(d.getActionType()))
                .collect(Collectors.toList());
        if (activeRows.isEmpty()) {
            throw new ValidationException("No Details in this Transaction...");
        }
        BigDecimal grnTotal = activeRows.stream()
                .map(d -> safe(d.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Strict equality — mirrors legacy GrnTotal.compareTo(InvAmount) == 0
        if (amount.compareTo(grnTotal.add(rounding)) != 0) {
            throw new ValidationException(
                    "Paid Amount (" + amount.toPlainString() + ") is not matching with Total GRN Amount ("
                            + grnTotal.toPlainString() + ")...");
        }
    }

    private List<GlPettyCashPaymentGrnDtlRequestDto> filterGrnCheckAll(List<GlPettyCashPaymentGrnDtlRequestDto> dtls) {
        return Optional.ofNullable(dtls).orElse(Collections.emptyList()).stream()
                .filter(d -> !"N".equalsIgnoreCase(d.getCheckAll()))
                .collect(Collectors.toList());
    }

    private List<GlPettyCashPaymentGrnDtl> mapGrnDtls(List<GlPettyCashPaymentGrnDtlRequestDto> dtlList, Long hdrPoid) {
        final long[] counter = {1};
        return dtlList.stream()
                .filter(dtl -> {
                    String at = dtl.getActionType();
                    return at == null || at.trim().isEmpty() || "ISCREATED".equals(at.toUpperCase());
                })
                .map(dtl -> {
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) detRowId = counter[0]++;
                    return GlPettyCashPaymentGrnDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(detRowId)
                            .grnPoid(dtl.getGrnPoid())
                            .checkAll(dtl.getCheckAll())
                            .amount(dtl.getAmount())
                            .remarks(dtl.getRemarks())
                            .refDocId(dtl.getRefDocId())
                            .refDocPoid(dtl.getRefDocPoid())
                            .refDetRowId(dtl.getRefDetRowId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<GlPettyCashPaymentGrnDtl> mergeGrnDtls(List<GlPettyCashPaymentGrnDtl> existing,
                                                         PettyCashRequestBase requestDto,
                                                         Long hdrPoid) {
        Map<Long, GlPettyCashPaymentGrnDtl> existingMap = existing.stream()
                .collect(Collectors.toMap(GlPettyCashPaymentGrnDtl::getDetRowId, d -> d));
        List<GlPettyCashPaymentGrnDtl> toSave = new ArrayList<>();
        List<GlPettyCashPaymentGrnDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GlPettyCashPaymentGrnDtl>> logRequests = new ArrayList<>();
        List<Long> newDetRowIds = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        for (var dto : filterGrnCheckAll(requestDto.getGlPettyCashGrnDtlRequestDtos())) {
            String actionTypeStr = dto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) actionTypeStr = "noChanges";
            String actionType = actionTypeStr.toUpperCase();

            switch (actionType) {
                case "ISCREATED" -> {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        detRowId = existing.stream().map(GlPettyCashPaymentGrnDtl::getDetRowId)
                                .filter(Objects::nonNull).max(Long::compareTo).orElse(0L) + 1;
                    }
                    GlPettyCashPaymentGrnDtl newEntity = GlPettyCashPaymentGrnDtl.builder()
                            .transactionPoid(hdrPoid).detRowId(detRowId)
                            .grnPoid(dto.getGrnPoid()).checkAll(dto.getCheckAll())
                            .amount(dto.getAmount()).remarks(dto.getRemarks())
                            .refDocId(dto.getRefDocId()).refDocPoid(dto.getRefDocPoid())
                            .refDetRowId(dto.getRefDetRowId())
                            .build();
                    toSave.add(newEntity);
                    newDetRowIds.add(detRowId);
                }
                case "ISUPDATED" -> {
                    GlPettyCashPaymentGrnDtl e = existingMap.get(dto.getDetRowId());
                    if (e == null) throw new ValidationException("GRN detail not found with detRowId: " + dto.getDetRowId());
                    GlPettyCashPaymentGrnDtl oldEntity = new GlPettyCashPaymentGrnDtl();
                    BeanUtils.copyProperties(e, oldEntity);
                    e.setGrnPoid(dto.getGrnPoid());
                    e.setCheckAll(dto.getCheckAll());
                    e.setAmount(dto.getAmount());
                    e.setRemarks(dto.getRemarks());
                    e.setRefDocId(dto.getRefDocId());
                    e.setRefDocPoid(dto.getRefDocPoid());
                    e.setRefDetRowId(dto.getRefDetRowId());
                    toSave.add(e);
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", hdrPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, e, GlPettyCashPaymentGrnDtl.class, documentId, hdrPoid.toString(), logDetail));
                }
                case "ISDELETED" -> {
                    GlPettyCashPaymentGrnDtl e = existingMap.get(dto.getDetRowId());
                    if (e != null) {
                        toDelete.add(e);
                        loggingService.logDelete(e, documentId, hdrPoid.toString());
                    }
                }
                default -> {
                    GlPettyCashPaymentGrnDtl e = existingMap.get(dto.getDetRowId());
                    if (e != null) toSave.add(e);
                }
            }
        }

        if (!toDelete.isEmpty()) glPettyCashPaymentGrnDtlRepository.deleteAll(toDelete);
        List<GlPettyCashPaymentGrnDtl> savedEntities = glPettyCashPaymentGrnDtlRepository.saveAll(toSave);

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }

        savedEntities.stream()
                .filter(entity -> newDetRowIds.contains(entity.getDetRowId()))
                .forEach(entity -> {
                    String logDetail = String.format("Row Created on GRN Detail with detRowId: %s", entity.getDetRowId());
                    loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                });

        return savedEntities;
    }

    private GlPettyCashPaymentHdr buildPettyCashHeader(PettyCashRequestBase requestDto) {

        return GlPettyCashPaymentHdr.builder()
                .transactionDate(requestDto.getTransactionDate())
                .groupPoid(UserContext.getGroupPoid())
                .companyPoid(UserContext.getCompanyPoid())
                .currencyCode(requestDto.getCurrencyCode())
                .currencyRate(requestDto.getCurrencyRate())
                .pettyCashGlPoid(requestDto.getPettyCashGlPoid())
                .balance(requestDto.getBalance())
                .amount(requestDto.getAmount())
                .payingTo(requestDto.getPayingTo())
                .narration(requestDto.getNarration())
                .advance(requestDto.getAdvance())
                .refType(requestDto.getRefType())
                .fdaRef(requestDto.getFdaRef())
                .ffRef(requestDto.getFfRef())
                .settledDate(requestDto.getSettledDate())
                .remarks(requestDto.getRemarks())
                .settledTotal(requestDto.getSettledTotal())
                .status(requestDto.getStatus())
                .grandTotal(requestDto.getGrandTotal())
                .mtaRef(requestDto.getMtaRef())
                .multiCompany(requestDto.getMultiCompany())
                .poRef(requestDto.getPoRef())
                .salesQtnRef(requestDto.getSalesQtnRef())
                .crTotal(requestDto.getCrTotal())
                .drTotal(requestDto.getDrTotal())
                .roundingAmount(requestDto.getRoundingAmount())
                .grnSupplierPoid(requestDto.getGrnSupplierPoid())
                .supplierGlPoid(requestDto.getSupplierGlPoid())
                .customerGlPoid(requestDto.getCustomerGlPoid())
                .advancePettyCashPoid(requestDto.getAdvancePettyCashPoid())
                .advanceStatus(requestDto.getAdvanceStatus())
                .advanceAmount(requestDto.getAdvanceAmount())
                .companyDivPoid(requestDto.getCompanyDivPoid())
                .build();
    }

    private List<GlPettyCashPaymentDtl> mapPaymentDtls(PettyCashRequestBase requestDto, Long hdrPoid) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GlPettyCashPaymentDtlRequestDto> filteredDtos = Optional.ofNullable(requestDto.getGlPettyCashPaymentDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(dtl -> {
                    String actionTypeStr = dtl.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .collect(Collectors.toList());

        final long[] detRowIdCounter = {1};
        return filteredDtos.stream()
                .map(dtl -> {
                    // Auto-generate detRowId if not provided
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) {
                        detRowId = detRowIdCounter[0]++;
                    }

                    return GlPettyCashPaymentDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(detRowId)  // Use auto-generated or provided detRowId
                            .type(dtl.getType())
                            .companyPoid(dtl.getCompanyPoid())
                            .glPoid(dtl.getGlPoid())
                            .chargePoid(dtl.getChargePoid())
                            .drAmt(dtl.getDrAmt())
                            .crAmt(dtl.getCrAmt())
                            .vatAmount(dtl.getVatAmount())
                            .totalAmount(dtl.getTotalAmount())
                            .vatSupplier(dtl.getVatSupplier())
                            .inputVatNumber(dtl.getInputVatNumber())
                            .supplierInvDate(dtl.getSupplierInvDate())
                            .taxPoid(dtl.getTaxPoid())
                            .taxPercentage(dtl.getTaxPercentage())
                            .vatPartyName(dtl.getVatPartyName())
                            .remarks(dtl.getRemarks())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<GLPettyCashItemDtl> mapItemDtls(PettyCashRequestBase requestDto, Long hdrPoid) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GlPettyCashItemDtlRequestDto> filteredDtos = Optional.ofNullable(requestDto.getGlPettyCashItemDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(dtl -> {
                    String actionTypeStr = dtl.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .filter(dtl -> !"N".equalsIgnoreCase(dtl.getCheckAll()))
                .collect(Collectors.toList());

        final long[] detRowIdCounter = {1};
        return filteredDtos.stream()
                .map(dtl -> {
                    // Auto-generate detRowId if not provided
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) {
                        detRowId = detRowIdCounter[0]++;
                    }

                    return GLPettyCashItemDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(detRowId)
                            .stockPoid(dtl.getStockPoid())
                            .stockUnitPoid(dtl.getStockUnitPoid())
                            .poQty(dtl.getPoQty())
                            .dnQty(dtl.getDnQty())
                            .qtyReceived(dtl.getQtyReceived())
                            .price(dtl.getPrice())
                            .discount(dtl.getDiscount())
                            .total(dtl.getTotal())
                            .remarks(dtl.getRemarks())
                            .refDocId(dtl.getRefDocId())
                            .refDocPoid(dtl.getRefDocPoid())
                            .checkAll(dtl.getCheckAll())
                            .refDetRowId(dtl.getRefDetRowId())
                            .vatPartyName(dtl.getVatPartyName())
                            .partyInvNumber(dtl.getPartyInvNumber())
                            .partyInvDate(dtl.getPartyInvDate())
                            .taxPoid(dtl.getTaxPoid())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<GlPettyCashChargeDtl> mapChargeDtls(PettyCashRequestBase requestDto, Long hdrPoid) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GlPettyCashChargeDtlRequestDto> filteredDtos = Optional.ofNullable(requestDto.getGlPettyCashChargeDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(dtl -> {
                    String actionTypeStr = dtl.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .filter(dtl -> !"N".equalsIgnoreCase(dtl.getCheckAll()))
                .collect(Collectors.toList());

        final long[] detRowIdCounter = {1};
        return filteredDtos.stream()
                .map(dtl -> {
                    // Auto-generate detRowId if not provided
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) {
                        detRowId = detRowIdCounter[0]++;
                    }

                    return GlPettyCashChargeDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(detRowId)
                            .chargePoid(dtl.getChargePoid())
                            .chargeAmount(dtl.getChargeAmount())
                            .description(dtl.getDescription())
                            .remarks(dtl.getRemarks())
                            .refDocId(dtl.getRefDocId())
                            .refDocPoid(dtl.getRefDocPoid())
                            .fdaDetRowId(dtl.getFdaDetRowId())
                            .checkAll(dtl.getCheckAll())
                            .pdaAmount(dtl.getPdaAmount())
                            .ffAmount(dtl.getFfAmount())
                            .chargeFrom(resolveChargeFrom(dtl.getChargeFrom(), requestDto.getRefType()))
                            .vatPartyName(dtl.getVatPartyName())
                            .partyInvNumber(dtl.getPartyInvNumber())
                            .partyInvDate(dtl.getPartyInvDate())
                            .taxPoid(dtl.getTaxPoid())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private PettyCashResponseDto mapToResponseDto(
            GlPettyCashPaymentHdr savedHeader,
            List<GlPettyCashPaymentDtlResponseDto> paymentDtls,
            List<GlPettyCashChargeDtlResponseDto> chargeDtls,
            List<GLPettyCashItemDtlResponseDto> itemDtls) {

        PettyCashResponseDto.PettyCashResponseDtoBuilder builder = PettyCashResponseDto.builder()
                .transactionPoid(savedHeader.getTransactionPoid())
                .docRef(savedHeader.getDocRef())
                .transactionDate(savedHeader.getTransactionDate())
                .groupPoid(savedHeader.getGroupPoid())
                .companyPoid(savedHeader.getCompanyPoid())
                .currencyCode(savedHeader.getCurrencyCode())
                .currencyRate(scale3(savedHeader.getCurrencyRate()))
                .pettyCashGlPoid(savedHeader.getPettyCashGlPoid())
                .balance(scale3(savedHeader.getBalance()))
                .amount(scale3(savedHeader.getAmount()))
                .payingTo(savedHeader.getPayingTo())
                .narration(savedHeader.getNarration())
                .advance(savedHeader.getAdvance())
                .refType(savedHeader.getRefType())
                .fdaRef(savedHeader.getFdaRef())
                .ffRef(savedHeader.getFfRef())
                .settledDate(savedHeader.getSettledDate())
                .remarks(savedHeader.getRemarks())
                .settledTotal(scale3(savedHeader.getSettledTotal()))
                .createdBy(savedHeader.getCreatedBy())
                .createdDate(savedHeader.getCreatedDate())
                .lastModifiedBy(savedHeader.getLastModifiedBy())
                .lastModifiedDate(savedHeader.getLastModifiedDate())
                .deleted(savedHeader.getDeleted())
                .status(savedHeader.getStatus())
                .grandTotal(scale3(savedHeader.getGrandTotal()))
                .mtaRef(savedHeader.getMtaRef())
                .multiCompany(savedHeader.getMultiCompany())
                .poRef(savedHeader.getPoRef())
                .salesQtnRef(savedHeader.getSalesQtnRef())
                .crTotal(scale3(savedHeader.getCrTotal()))
                .drTotal(scale3(savedHeader.getDrTotal()))
                .roundingAmount(scale3(savedHeader.getRoundingAmount()))
                .grnSupplierPoid(savedHeader.getGrnSupplierPoid())
                .supplierGlPoid(savedHeader.getSupplierGlPoid())
                .customerGlPoid(savedHeader.getCustomerGlPoid())
                .advancePettyCashPoid(savedHeader.getAdvancePettyCashPoid())
                .advanceStatus(savedHeader.getAdvanceStatus())
                .advanceAmount(scale3(savedHeader.getAdvanceAmount()))
                .companyDivPoid(savedHeader.getCompanyDivPoid())
                .paymentDtls(paymentDtls)
                .chargeDtls(chargeDtls)
                .itemDtls(itemDtls);

        if (savedHeader.getPettyCashGlPoid() != null) {
            Optional<GLMaster> pettyCashGlPoidOp =
                    glMasterRepository.findByGlPoid(savedHeader.getPettyCashGlPoid());
            if (pettyCashGlPoidOp.isPresent()) {
                GLMaster glMaster = pettyCashGlPoidOp.get();

                DetailsDto detailsDto = new DetailsDto(
                        glMaster.getGlPoid(),
                        glMaster.getGlCode(),
                        glMaster.getGlDescription(),
                        glMaster.getGroupPoid(),
                        glMaster.getGlDescription2(),
                        glMaster.getSeqno()
                );

                builder.pettyCashGlPoidDtl(detailsDto);
            }
        }

        if (savedHeader.getGrnSupplierPoid() != null) {
            SupplierMasterEntity supplier = supplierMasterRepository.findBySupplierPoid(savedHeader.getGrnSupplierPoid());
            if (supplier != null) {
                builder.grnSupplierPoidDtl(new DetailsDto(
                        supplier.getSupplierPoid(),
                        supplier.getSupplierCode(),
                        supplier.getSupplierName(),
                        supplier.getGroupPoid(),
                        supplier.getSupplierName2(),
                        supplier.getSeqNo() != null ? supplier.getSeqNo().intValue() : null
                ));
            }
        }

        if (savedHeader.getSupplierGlPoid() != null) {
            glMasterRepository.findByGlPoid(savedHeader.getSupplierGlPoid()).ifPresent(gl ->
                    builder.supplierGlPoidDtl(new DetailsDto(
                            gl.getGlPoid(), gl.getGlCode(), gl.getGlDescription(),
                            gl.getGroupPoid(), gl.getGlDescription2(), gl.getSeqno()
                    ))
            );
        }

        if (savedHeader.getCustomerGlPoid() != null) {
            glMasterRepository.findByGlPoid(savedHeader.getCustomerGlPoid()).ifPresent(gl ->
                    builder.customerGlPoidDtl(new DetailsDto(
                            gl.getGlPoid(), gl.getGlCode(), gl.getGlDescription(),
                            gl.getGroupPoid(), gl.getGlDescription2(), gl.getSeqno()
                    ))
            );
        }

        if (savedHeader.getAdvancePettyCashPoid() != null) {
            LovGetListDto advLov = lovService.getDetailsByPoidAndLovName(savedHeader.getAdvancePettyCashPoid(), "ADVANCE_PETTY_CASH_PENDING");
            if (advLov != null && advLov.getPoid() != null) {
                builder.advancePettyCashPoidDtl(new DetailsDto(
                        advLov.getPoid(), advLov.getCode(), advLov.getLabel(),
                        advLov.getValue(), advLov.getDescription(), advLov.getSeqNo()
                ));
            }
        }

        if (savedHeader.getFfRef() != null && !savedHeader.getFfRef().isBlank()) {
            try {
                Long ffPoid = Long.parseLong(savedHeader.getFfRef().trim());
                LovGetListDto lov = lovService.getDetailsByPoidAndLovName(ffPoid, "FF_JOBS_FOR_COST_BOOKING");
                if (lov != null && lov.getPoid() != null) {
                    builder.ffRefDtl(new DetailsDto(
                            lov.getPoid(), lov.getCode(), lov.getLabel(),
                            lov.getValue(), lov.getDescription(), lov.getSeqNo()
                    ));
                }
            } catch (NumberFormatException ignored) {
                // ffRef is not a numeric POID — skip enrichment
            }
        }

        if (savedHeader.getFdaRef() != null && !savedHeader.getFdaRef().isBlank()) {
            try {
                Long fdaPoid = Long.parseLong(savedHeader.getFdaRef().trim());
                LovGetListDto lov = lovService.getDetailsByPoidAndLovName(fdaPoid, "PROCESS_FDA_IN_PI");
                if (lov != null && lov.getPoid() != null) {
                    builder.fdaRefDtl(new DetailsDto(
                            lov.getPoid(), lov.getCode(), lov.getLabel(),
                            lov.getValue(), lov.getDescription(), lov.getSeqNo()
                    ));
                }
            } catch (NumberFormatException ignored) {
                // fdaRef is not a numeric POID — skip enrichment
            }
        }

        if (savedHeader.getSalesQtnRef() != null && !savedHeader.getSalesQtnRef().isBlank()) {
            try {
                Long salesQtnPoid = Long.parseLong(savedHeader.getSalesQtnRef().trim());
                LovGetListDto lov = lovService.getDetailsByPoidAndLovName(salesQtnPoid, "PETTY_MTA_BASED_RFQ");
                if (lov != null && lov.getPoid() != null) {
                    builder.salesQtnRefDtl(new DetailsDto(
                            lov.getPoid(), lov.getCode(), lov.getLabel(),
                            lov.getValue(), lov.getDescription(), lov.getSeqNo()
                    ));
                }
            } catch (NumberFormatException ignored) {
                // salesQtnRef is not a numeric POID — skip enrichment
            }
        }

        return builder.build();
    }

    private List<GlPettyCashPaymentDtlResponseDto> mapPaymentResponse(List<GlPettyCashPaymentDtl> savedDtls) {
        return savedDtls.stream()
                .map(dtl -> {
                    GlPettyCashPaymentDtlResponseDto.GlPettyCashPaymentDtlResponseDtoBuilder builder =
                            GlPettyCashPaymentDtlResponseDto.builder()
                                    .transactionPoid(dtl.getTransactionPoid())
                                    .detRowId(dtl.getDetRowId())
                                    .type(dtl.getType())
                                    .companyPoid(dtl.getCompanyPoid())
                                    .glPoid(dtl.getGlPoid())
                                    .chargePoid(dtl.getChargePoid())
                                    .drAmt(scale3(dtl.getDrAmt()))
                                    .crAmt(scale3(dtl.getCrAmt()))
                                    .remarks(dtl.getRemarks())
                                    .createdBy(dtl.getCreatedBy())
                                    .createdDate(dtl.getCreatedDate())
                                    .lastModifiedBy(dtl.getLastModifiedBy())
                                    .lastModifiedDate(dtl.getLastModifiedDate())
                                    .vatAmount(scale3(dtl.getVatAmount()))
                                    .totalAmount(scale3(dtl.getTotalAmount()))
                                    .vatSupplier(dtl.getVatSupplier())
                                    .inputVatNumber(dtl.getInputVatNumber())
                                    .supplierInvDate(dtl.getSupplierInvDate())
                                    .taxPoid(dtl.getTaxPoid())
                                    .taxPercentage(scale3(dtl.getTaxPercentage()))
                                    .vatPartyName(dtl.getVatPartyName());

                    if (dtl.getGlPoid() != null) {
                        glMasterRepository.findByGlPoid(dtl.getGlPoid())
                                .ifPresent(glMaster -> {
                                    DetailsDto glDetails = new DetailsDto(
                                            glMaster.getGlPoid(),
                                            glMaster.getGlCode(),
                                            glMaster.getGlDescription(),
                                            glMaster.getGroupPoid(),
                                            glMaster.getGlDescription2(),
                                            glMaster.getSeqno()
                                    );
                                    builder.glPoidDtl(glDetails);
                                });
                    }

                    if (dtl.getChargePoid() != null) {
                        shipChargeRepository.findByChargePoid(dtl.getChargePoid())
                                .ifPresent(charge -> {
                                    DetailsDto chargeDetails = new DetailsDto(
                                            charge.getChargePoid(),
                                            charge.getChargeCode(),
                                            charge.getChargeName(),
                                            charge.getGroupPoid(),
                                            charge.getChargeName2(),
                                            charge.getSeqNo()
                                    );
                                    builder.chargePoidDtl(chargeDetails);
                                });
                    }

                    GlPettyCashPaymentDtlResponseDto responseDto = builder.build();

                    // Fetch and set taxPoidDtl if taxPoid exists
                    if (dtl.getTaxPoid() != null) {
                        taxMasterRepository.findByTaxPoid(dtl.getTaxPoid())
                                .ifPresent(tax -> {
                                    DetailsDto taxDetails = new DetailsDto(
                                            tax.getTaxPoid(),
                                            tax.getTaxCode(),
                                            tax.getTaxName(),
                                            tax.getGroupPoid(),
                                            tax.getTaxName2(),
                                            tax.getSeqNo()
                                    );
                                    responseDto.setTaxPoidDtl(taxDetails);
                                });
                    }

                    if (dtl.getVatSupplier() != null) {
                        SupplierMasterEntity supplier = supplierMasterRepository.findBySupplierPoid(dtl.getVatSupplier());
                        if (supplier != null) {
                            responseDto.setVatSupplierDtl(new DetailsDto(
                                    supplier.getSupplierPoid(),
                                    supplier.getSupplierCode(),
                                    supplier.getSupplierName(),
                                    supplier.getGroupPoid(),
                                    supplier.getSupplierName2(),
                                    supplier.getSeqNo() != null ? supplier.getSeqNo().intValue() : null
                            ));
                        }
                    }

                    if (dtl.getCompanyPoid() != null) {
                        LovGetListDto companyLov = lovService.getDetailsByPoidAndLovName(dtl.getCompanyPoid(), "COMPANY");
                        if (companyLov != null && companyLov.getPoid() != null) {
                            responseDto.setCompanyPoidDtl(new DetailsDto(
                                    companyLov.getPoid(), companyLov.getCode(), companyLov.getLabel(),
                                    companyLov.getValue(), companyLov.getDescription(), companyLov.getSeqNo()
                            ));
                        }
                    }

                    return responseDto;
                })
                .toList();
    }

    private List<GlPettyCashChargeDtlResponseDto> mapChargeResponse(List<GlPettyCashChargeDtl> savedDtls) {
        return savedDtls.stream()
                .map(dtl -> {
                    GlPettyCashChargeDtlResponseDto.GlPettyCashChargeDtlResponseDtoBuilder builder =
                            GlPettyCashChargeDtlResponseDto.builder()
                                    .transactionPoid(dtl.getTransactionPoid())
                                    .detRowId(dtl.getDetRowId())
                                    .chargePoid(dtl.getChargePoid())
                                    .chargeAmount(scale3(dtl.getChargeAmount()))
                                    .description(dtl.getDescription())
                                    .remarks(dtl.getRemarks())
                                    .createdBy(dtl.getCreatedBy())
                                    .createdDate(dtl.getCreatedDate())
                                    .lastModifiedBy(dtl.getLastModifiedBy())
                                    .lastModifiedDate(dtl.getLastModifiedDate())
                                    .refDocId(dtl.getRefDocId())
                                    .refDocPoid(dtl.getRefDocPoid())
                                    .fdaDetRowId(dtl.getFdaDetRowId())
                                    .checkAll(dtl.getCheckAll())
                                    .pdaAmount(scale3(dtl.getPdaAmount()))
                                    .ffAmount(scale3(dtl.getFfAmount()))
                                    .chargeFrom(dtl.getChargeFrom())
                                    .vatPartyName(dtl.getVatPartyName())
                                    .partyInvNumber(dtl.getPartyInvNumber())
                                    .partyInvDate(dtl.getPartyInvDate())
                                    .taxPoid(dtl.getTaxPoid());


                    if (dtl.getChargePoid() != null) {
                        shipChargeRepository.findByChargePoid(dtl.getChargePoid())
                                .ifPresent(charge -> {
                                    DetailsDto chargeDetails = new DetailsDto(
                                            charge.getChargePoid(),
                                            charge.getChargeCode(),
                                            charge.getChargeName(),
                                            charge.getGroupPoid(),
                                            charge.getChargeName2(),
                                            charge.getSeqNo()
                                    );
                                    builder.chargePoidDtl(chargeDetails);
                                });
                    }

                    GlPettyCashChargeDtlResponseDto responseDto = builder.build();

                    // Fetch and set taxPoidDtl if taxPoid exists
                    if (dtl.getTaxPoid() != null) {
                        taxMasterRepository.findByTaxPoid(dtl.getTaxPoid())
                                .ifPresent(tax -> {
                                    DetailsDto taxDetails = new DetailsDto(
                                            tax.getTaxPoid(),
                                            tax.getTaxCode(),
                                            tax.getTaxName(),
                                            tax.getGroupPoid(),
                                            tax.getTaxName2(),
                                            tax.getSeqNo()
                                    );
                                    responseDto.setTaxPoidDtl(taxDetails);
                                });
                    }

                    // Enrich refDocPoidDtl for FF charges only (LOV: FF_JOBNO)
                    if ("FF".equalsIgnoreCase(dtl.getChargeFrom()) && dtl.getRefDocPoid() != null) {
                        LovGetListDto ffJobLov = lovService.getDetailsByPoidAndLovName(dtl.getRefDocPoid(), "FF_JOBNO");
                        if (ffJobLov != null && ffJobLov.getPoid() != null) {
                            responseDto.setRefDocPoidDtl(new DetailsDto(
                                    ffJobLov.getPoid(), ffJobLov.getCode(), ffJobLov.getLabel(),
                                    ffJobLov.getValue(), ffJobLov.getDescription(), ffJobLov.getSeqNo()
                            ));
                        }
                    }

                    return responseDto;
                })
                .toList();
    }

    private List<GLPettyCashItemDtlResponseDto> mapItemResponse(List<GLPettyCashItemDtl> savedDtls) {
        return savedDtls.stream()
                .map(dtl -> {
                    GLPettyCashItemDtlResponseDto.GLPettyCashItemDtlResponseDtoBuilder builder =
                            GLPettyCashItemDtlResponseDto.builder()
                                    .transactionPoid(dtl.getTransactionPoid())
                                    .detRowId(dtl.getDetRowId())
                                    .stockPoid(dtl.getStockPoid())
                                    .stockUnitPoid(dtl.getStockUnitPoid())
                                    .poQty(scale3(dtl.getPoQty()))
                                    .dnQty(scale3(dtl.getDnQty()))
                                    .qtyReceived(scale3(dtl.getQtyReceived()))
                                    .price(scale3(dtl.getPrice()))
                                    .discount(scale3(dtl.getDiscount()))
                                    .total(scale3(dtl.getTotal()))
                                    .remarks(dtl.getRemarks())
                                    .createdBy(dtl.getCreatedBy())
                                    .createdDate(dtl.getCreatedDate())
                                    .lastModifiedBy(dtl.getLastModifiedBy())
                                    .lastModifiedDate(dtl.getLastModifiedDate())
                                    .refDocId(dtl.getRefDocId())
                                    .refDocPoid(dtl.getRefDocPoid())
                                    .checkAll(dtl.getCheckAll())
                                    .refDetRowId(dtl.getRefDetRowId())
                                    .vatPartyName(dtl.getVatPartyName())
                                    .partyInvNumber(dtl.getPartyInvNumber())
                                    .partyInvDate(dtl.getPartyInvDate())
                                    .taxPoid(dtl.getTaxPoid());

                    if (dtl.getStockPoid() != null) {
                        stockMasterRepository.findByStockPoid(dtl.getStockPoid())
                                .ifPresent(stock -> {
                                    DetailsDto stockDetails = new DetailsDto(
                                            stock.getStockPoid(),
                                            stock.getStockCode(),
                                            stock.getStockName(),
                                            stock.getGroupPoid(),
                                            stock.getStockDescription(),
                                            stock.getSeqNo()
                                    );
                                    builder.stockPoidDtl(stockDetails);
                                });
                    }

                    if (dtl.getStockUnitPoid() != null) {
                        unitMasterRepository.findByUnitPoid(dtl.getStockUnitPoid())
                                .ifPresent(unit -> {
                                    DetailsDto unitDetails = new DetailsDto(
                                            unit.getUnitPoid(),
                                            unit.getUnitCode(),
                                            unit.getUnitName(),
                                            unit.getGroupPoid(),
                                            unit.getUnitName2(),
                                            unit.getSeqNo()
                                    );
                                    builder.stockUnitPoidDtl(unitDetails);
                                });
                    }

                    GLPettyCashItemDtlResponseDto responseDto = builder.build();

                    // Fetch and set taxPoidDtl if taxPoid exists
                    if (dtl.getTaxPoid() != null) {
                        taxMasterRepository.findByTaxPoid(dtl.getTaxPoid())
                                .ifPresent(tax -> {
                                    DetailsDto taxDetails = new DetailsDto(
                                            tax.getTaxPoid(),
                                            tax.getTaxCode(),
                                            tax.getTaxName(),
                                            tax.getGroupPoid(),
                                            tax.getTaxName2(),
                                            tax.getSeqNo()
                                    );
                                    responseDto.setTaxPoidDtl(taxDetails);
                                });
                    }

                    return responseDto;
                })
                .toList();
    }

}