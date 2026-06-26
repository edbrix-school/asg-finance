package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.common.lib.utility.DateUtil;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GLPaymentVoucherDtlGLEntity;
import com.asg.finance.entity.GLPaymentVoucherHDREntity;
import com.asg.finance.entity.GlBankPaymentChargeDtlEntity;
import com.asg.finance.entity.GlBankPaymentItemDtlEntity;
import com.asg.finance.repository.*;
import com.asg.finance.service.BankPaymentVoucherService;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class BankPaymentVoucherServiceImpl implements BankPaymentVoucherService {

    private static final String GL_TYPE_DR = "DR";
    private static final String GL_TYPE_CR = "CR";
    private static final String BANK_GL_IND_BANK_ROW = "BANK_ROW";

    @Autowired
    private BankPaymentVoucherRepository paymentVoucherRepository;

    @Autowired
    private BankPaymentVoucherDetailsRepository paymentVoucherDetailsRepository;

    @Autowired
    private GlBankPaymentChargeDtlRepository chargeDtlRepository;

    @Autowired
    private GlBankPaymentItemDtlRepository itemRepository;

    @Autowired
    private BankPaymentVoucherSpRepository spRepository;

    @Autowired
    private BankPaymentLoadDataRepository loadDataRepository;

    @Autowired
    private DocumentSearchService documentService;

    @Autowired
    private LovDataService lovService;

    @Autowired
    private BillwiseBreakupDtlRepository billwiseBreakupRepository;

    @Autowired
    private CostCenterBreakupDtlRepository costCenterBreakupDtlRepository;

    @Autowired
    private BillwiseBreakupService billwiseBreakupService;

    @Autowired
    private CostCenterBreakupService costCenterBreakupService;

    @Autowired
    private PrintService printService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private LoggingService loggingService;
    @Autowired
    private DocumentDeleteService documentDeleteService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private GlobalParameterService globalParameterService;

    @Override
    @Transactional
    public BankPaymentVoucherResponse getVoucherById(Long transactionPoid, String documentId) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found with ID: " + transactionPoid));

        List<GLPaymentVoucherDtlGLEntity> glList = null;
        List<GlBankPaymentChargeDtlEntity> chargeList = null;
        List<GlBankPaymentItemDtlEntity> itemList = null;

        String refType = header.getRefType() != null ? header.getRefType().toUpperCase() : "";

        switch (refType) {
            case "GENERAL":
            case "CUSTOM":
                glList = paymentVoucherDetailsRepository.findByTransactionPoid(transactionPoid);
                break;

            case "FDA JOBS":
            case "FF JOBS":
                chargeList = chargeDtlRepository.findByTransactionPoid(transactionPoid);
                break;

            case "MTA RFQ":
                itemList = itemRepository.findByTransactionPoid(transactionPoid);
                break;

            default:
                log.warn("Unknown Ref Type while fetching voucher: {}", refType);
        }

        Long transPoid = header.getTransactionPoid();
        Long groupPoid = header.getGroupPoid();
        Long companyPoid = header.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transPoid);

        GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                costCenterBreakupService.loadCostCenterData(documentId, transPoid, groupPoid, companyPoid, userPoid);
        BankPaymentVoucherResponse response =
                mapToResponse(header, glList, chargeList, itemList);

        for (BankPaymentGLDetailResponse dtl : response.getGlDetails()) {
            Long detRowId = dtl.getDetRowId();
            if (null != billwiseResponse && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                dtl.setBillwiseBreakupList(mapToPopupDto(billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream().filter(x -> x.getMainDetRowId().equals(detRowId)).collect(Collectors.toList())));
            }
            if (null != costCenterResponse && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                dtl.setCostCenterBreakupList(mapToCostCenterPopupDto(costCenterResponse.getCostBreakupList().stream().filter(x -> x.getMainDetRowId().equals(detRowId)).collect(Collectors.toList())));
            }
        }

        return response;
    }

    @Override
    @Transactional
    @PerformGlPosting
    public BankPaymentVoucherResponse createBankPaymentVoucher(BankPaymentVoucherRequest req, String documentId) {

        validateRefType(req);
        // Legacy DocumentBeforeSave validations
        validateBeforeSaveRequest(req);
        if ("FF JOBS".equalsIgnoreCase(req.getRefType())) {
            String ffRef = resolveEffectiveFfRef(req);
            if (ffRef != null) {
                validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), ffRef, "FF JOBS", documentId, UserContext.getUserPoid());
            }
        }
        if ("FDA JOBS".equalsIgnoreCase(req.getRefType()) && req.getFdaRefId() != null) {
            validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), String.valueOf(req.getFdaRefId()), "FDA JOBS", documentId, UserContext.getUserPoid());
        }
        if ("MTA RFQ".equalsIgnoreCase(req.getRefType())) {
            String refPoid = req.getSalesQtnRef() != null ? String.valueOf(req.getSalesQtnRef()) : req.getMtaRfqId();
            if (refPoid != null) {
                validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), refPoid, "MTA RFQ", documentId, UserContext.getUserPoid());
            }
        }

        GLPaymentVoucherHDREntity entity = mapHeaderFromRequest(req);

        if (req.getChqCardNo() == null && req.getBankPoid() != null) {
            try {
                String nextCheque = spRepository.getNextChequeNumber(req.getBankPoid());
                entity.setChqCardNo(nextCheque);
            } catch (Exception e) {
                log.warn("Failed to auto-generate cheque number: {}", e.getMessage());
                // Continue without auto-generated cheque number
            }
        }

        GLPaymentVoucherHDREntity savedHeader = paymentVoucherRepository.save(entity);
        entityManager.flush();
        entityManager.refresh(entity);

        /*savedHeader.setDocRef("BPV-" + savedHeader.getTransactionPoid());*/
        savedHeader = paymentVoucherRepository.save(savedHeader);

        // Before save validation
        validateBeforeSaveInNewTransaction(savedHeader);

        // Log the main entity creation
        String key = savedHeader.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(documentId, key, String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedHeader.getDocRef()));

        if (req.getRefType().equalsIgnoreCase("GENERAL") ||
                req.getRefType().equalsIgnoreCase("CUSTOM")) {
            saveGLDetails(req.getGlDetails(), savedHeader.getTransactionPoid(), documentId);
        }

        if (req.getRefType().equalsIgnoreCase("FDA JOBS") ||
                req.getRefType().equalsIgnoreCase("FF JOBS")) {
            saveChargeDetails(req.getChargeDetailRequests(), savedHeader.getTransactionPoid());
        }

        if (req.getRefType().equalsIgnoreCase("MTA RFQ")) {
            saveItemDetails(req.getItemDetailRequests(), savedHeader.getTransactionPoid());
        }

        entityManager.flush();
        scheduleAfterSaveJobCostUpdates(savedHeader, req, savedHeader.getTransactionPoid(), null, null);

        return getVoucherById(savedHeader.getTransactionPoid(), documentId);
    }

    @Override
    @Transactional
    @PerformGlPosting
    public BankPaymentVoucherResponse updateBankPaymentVoucher(Long transactionPoid, BankPaymentVoucherRequest req, String documentId) {

        GLPaymentVoucherHDREntity existing = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Bank Payment Voucher not found for ID: " + transactionPoid));

        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = new GLPaymentVoucherHDREntity();
        BeanUtils.copyProperties(existing, oldEntity);

        String oldRefType = existing.getRefType();
        String oldRefPoid = resolveRefStringFromEntity(existing, oldRefType);

        // Legacy DocumentBeforeSave validations
        validateBeforeSaveRequest(req);
        if ("FF JOBS".equalsIgnoreCase(req.getRefType())) {
            String ffRef = resolveEffectiveFfRef(req);
            if (ffRef != null) {
                validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), ffRef, "FF JOBS", documentId, UserContext.getUserPoid());
            }
        }
        if ("FDA JOBS".equalsIgnoreCase(req.getRefType()) && req.getFdaRefId() != null) {
            validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), String.valueOf(req.getFdaRefId()), "FDA JOBS", documentId, UserContext.getUserPoid());
        }
        if ("MTA RFQ".equalsIgnoreCase(req.getRefType())) {
            String refPoid = req.getSalesQtnRef() != null ? String.valueOf(req.getSalesQtnRef()) : req.getMtaRfqId();
            if (refPoid != null) {
                validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), refPoid, "MTA RFQ", documentId, UserContext.getUserPoid());
            }
        }

        validateBeforeSaveInNewTransaction(existing);



        updateHeaderFromRequest(existing, req);
        GLPaymentVoucherHDREntity updatedHeader = paymentVoucherRepository.save(existing);

        if (oldRefType != null && req.getRefType() != null
                && !oldRefType.equalsIgnoreCase(req.getRefType())) {
            clearDetailsForOtherRefTypes(transactionPoid, req.getRefType());
        }

        // Update details based on Ref Type
        switch (req.getRefType().toUpperCase()) {
            case "GENERAL", "CUSTOM" -> updateGLDetails(req.getGlDetails(), transactionPoid, documentId);
            case "FF JOBS", "FDA JOBS" -> saveChargeDetails(req.getChargeDetailRequests(), transactionPoid);
            case "MTA RFQ" -> saveItemDetails(req.getItemDetailRequests(), transactionPoid);
            default -> throw new ValidationException("Invalid Ref Type: " + req.getRefType());
        }

        entityManager.flush();
        scheduleAfterSaveJobCostUpdates(updatedHeader, req, transactionPoid, oldRefType, oldRefPoid);

        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, updatedHeader, GLPaymentVoucherHDREntity.class,
                documentId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return getVoucherById(transactionPoid, documentId);
    }

    @Override
    @Transactional
    public void softDeleteVoucher(Long transactionPoid, String documentId, DeleteReasonDto deleteReasonDto) {
        GLPaymentVoucherHDREntity existing = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Bank Payment Voucher not found for ID: " + transactionPoid));

        // Validate voucher can be deleted
        validateVoucherStatusInNewTransaction(existing);

        // Release job allocations if applicable
        String refType = existing.getRefType();
        String refString = resolveRefStringFromEntity(existing, refType);
        if (refString != null) {
            releaseOldJobValuesInNewTransaction(transactionPoid, refString);
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_BANK_PAYMENT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                existing.getTransactionDate()
        );
    }

    private BankPaymentVoucherResponse mapToResponse(
            GLPaymentVoucherHDREntity header,
            List<GLPaymentVoucherDtlGLEntity> glList,
            List<GlBankPaymentChargeDtlEntity> chargeList,
            List<GlBankPaymentItemDtlEntity> itemList) {

        BankPaymentVoucherResponse response = new BankPaymentVoucherResponse();
        BeanUtils.copyProperties(header, response);

        response.setGroupDet(lovService.getDetailsByPoidAndLovName(header.getGroupPoid(), "COMPANY"));
        response.setCompanyDet(lovService.getDetailsByPoidAndLovName(header.getCompanyPoid(), "COMPANY"));
        response.setPayGlDet(lovService.getDetailsByPoidAndLovName(header.getPayGlPoid(), "GL_MASTER_LEDGERS"));
        response.setBankDet(lovService.getDetailsByPoidAndLovName(header.getBankPoid(), "BANK_MASTER"));
        response.setFdaDet(lovService.getDetailsByPoidAndLovName(header.getFdaRef(), "FDA_JOB"));

        if (header.getFfRef() != null && !header.getFfRef().isBlank()) {
            List<String> rawRefs = Arrays.asList(header.getFfRef().split(";"));
            response.setFfRefs(rawRefs);
            List<Long> ffPoids = rawRefs.stream()
                    .map(String::trim).filter(s -> !s.isBlank())
                    .flatMap(s -> { try { return java.util.stream.Stream.of(Long.parseLong(s)); }
                                   catch (NumberFormatException ignored) { return java.util.stream.Stream.empty(); } })
                    .collect(Collectors.toList());
            if (!ffPoids.isEmpty()) {
                Map<Long, LovGetListDto> ffLovMap = lovService.getDetailsByPoidsAndLovName(ffPoids, "FF_JOBS_FOR_COST_BOOKING");
                List<LovGetListDto> ffRefsDtl = ffPoids.stream()
                        .map(ffLovMap::get).filter(Objects::nonNull).collect(Collectors.toList());
                if (!ffRefsDtl.isEmpty()) response.setFfRefsDtl(ffRefsDtl);
            }
        }

        // =====================================================================
        // CASE HANDLING
        // Only one of the three lists will contain data
        // Others must be empty arrays, not null
        // =====================================================================

        boolean hasGL = glList != null && !glList.isEmpty();
        boolean hasCharge = chargeList != null && !chargeList.isEmpty();
        boolean hasItem = itemList != null && !itemList.isEmpty();

        // ----------------------------- GL DETAILS -----------------------------
        if (hasGL) {
            response.setGlDetails(
                    glList.stream().map(gl -> {
                        BankPaymentGLDetailResponse dto = new BankPaymentGLDetailResponse();
                        BeanUtils.copyProperties(gl, dto);
                        dto.setCompanyDet(lovService.getDetailsByPoidAndLovName(gl.getCompanyPoid(), "COMPANY"));
                        dto.setTaxDet(lovService.getDetailsByPoidAndLovName(gl.getTaxPoid(), "INPUT_TAX_MASTER"));
                        return dto;
                    }).toList()
            );
            // Others empty
            response.setChargeDetails(Collections.emptyList());
            response.setItemDetails(Collections.emptyList());

            // ----------------------------- CHARGE DETAILS -------------------------
        } else if (hasCharge) {

            response.setChargeDetails(
                    chargeList.stream().map(ch -> {
                        BankPaymentChargeDetailResponse dto = new BankPaymentChargeDetailResponse();
                        BeanUtils.copyProperties(ch, dto);
                        dto.setChargeDet(lovService.getDetailsByPoidAndLovName(ch.getChargePoid(), "TAX_PERIOD_CHARGE_MASTER"));
                        dto.setRefDocDet(lovService.getDetailsByPoidAndLovName(ch.getRefDocPoid(), "REF_DOC"));
                        return dto;
                    }).toList()
            );
            // Others empty
            response.setGlDetails(Collections.emptyList());
            response.setItemDetails(Collections.emptyList());

            // ----------------------------- ITEM DETAILS ---------------------------
        } else if (hasItem) {

            response.setItemDetails(
                    itemList.stream().map(it -> {
                        BankPaymentItemDetailResponse dto = new BankPaymentItemDetailResponse();
                        BeanUtils.copyProperties(it, dto);
                        dto.setStockDet(lovService.getDetailsByPoidAndLovName(it.getStockPoid(), "TAX_PERIOD_STOCK_MASTER"));
                        dto.setStockUnitDet(lovService.getDetailsByPoidAndLovName(it.getStockUnitPoid(), "STOCK_UNIT"));
                        dto.setRefDocDet(lovService.getDetailsByPoidAndLovName(it.getRefDocPoid(), "REF_DOC"));
                        return dto;
                    }).toList()
            );
            // Others empty
            response.setGlDetails(Collections.emptyList());
            response.setChargeDetails(Collections.emptyList());

        } else {
            // If all are empty → all empty arrays
            response.setGlDetails(Collections.emptyList());
            response.setChargeDetails(Collections.emptyList());
            response.setItemDetails(Collections.emptyList());
        }

        return response;
    }

    private void updateHeaderFromRequest(GLPaymentVoucherHDREntity entity, BankPaymentVoucherRequest req) {

        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setTransactionDate(req.getTransactionDate());
        entity.setBankPoid(req.getBankPoid());
        entity.setPayGlPoid(req.getPayGlPoid());
        entity.setPayingTo(req.getPayingTo());
        entity.setRemarks(req.getRemarks());
        entity.setRefType(req.getRefType());
        entity.setFdaRef(req.getFdaRefId());
        entity.setFfRef(resolveEffectiveFfRef(req));
        entity.setMtaRef(req.getMtaRfqId());

        if (req.getChqDate() != null && !req.getChqDate().isEmpty()) {
            entity.setChqDate(LocalDate.parse(req.getChqDate()));
        }


        entity.setChqCardNo(req.getChqCardNo());
        entity.setLongNarration(req.getLongNarration());
        entity.setSuppressValidation(req.getSuppressValidation());
        entity.setAccountPayee(req.getAccountPayee());
        entity.setMultiCompany(req.getMultiple());
        entity.setSecurityCheque(req.getSecurityCheque());
        entity.setCurrencyAmount(req.getCurrencyAmount());
        entity.setAvailableBalance(req.getAvailableBalance());

        if (Boolean.TRUE.equals(req.getReleased())) {
            entity.setReleasedToPerson(req.getReleasedToPerson());
            entity.setReleasedPersonAddress(req.getContact());
            entity.setReleasedByUserCode(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
            entity.setReleasedDate(LocalDate.now());
            entity.setReleased("Y");
        }
        entity.setSalesQtnRef(req.getSalesQtnRef() != null ? req.getSalesQtnRef() :
                (StringUtils.isNumeric(req.getMtaRfqId()) ? Long.valueOf(req.getMtaRfqId()) : null));

        entity.setPrePrinted(req.getPrePrinted() != null ? req.getPrePrinted() : "N");
        applyChequePrintedFieldsOnUpdate(entity, req);
    }

    private void applyChequePrintedFieldsOnUpdate(GLPaymentVoucherHDREntity entity, BankPaymentVoucherRequest req) {
        if (StringUtils.isNotBlank(req.getChqPrintedUserCode()) || req.getChqPrintedDate() != null) {
            entity.setChqPrintedUserCode(req.getChqPrintedUserCode());
            entity.setChqPrintedDate(req.getChqPrintedDate());
            entity.setChqPrinted("Y");
        }
    }

    private String normalizeDetailActionType(String actionTypeStr) {
        if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
            return "NOCHANGES";
        }
        String normalized = actionTypeStr.trim().toUpperCase();
        if ("NOCHANGE".equals(normalized) || "NOCHANGES".equals(normalized)) {
            return "NOCHANGES";
        }
        return normalized;
    }

    private void clearDetailsForOtherRefTypes(Long transactionPoid, String newRefType) {
        String ref = newRefType != null ? newRefType.toUpperCase() : "";
        if (!"GENERAL".equals(ref) && !"CUSTOM".equals(ref)) {
            List<GLPaymentVoucherDtlGLEntity> glRows = paymentVoucherDetailsRepository.findByTransactionPoid(transactionPoid);
            if (!glRows.isEmpty()) {
                paymentVoucherDetailsRepository.deleteAll(glRows);
            }
        }
        if (!"FF JOBS".equals(ref) && !"FDA JOBS".equals(ref)) {
            List<GlBankPaymentChargeDtlEntity> chargeRows = chargeDtlRepository.findByTransactionPoid(transactionPoid);
            if (!chargeRows.isEmpty()) {
                chargeDtlRepository.deleteAll(chargeRows);
            }
        }
        if (!"MTA RFQ".equals(ref)) {
            List<GlBankPaymentItemDtlEntity> itemRows = itemRepository.findByTransactionPoid(transactionPoid);
            if (!itemRows.isEmpty()) {
                itemRepository.deleteAll(itemRows);
            }
        }
    }

    private void validateRefType(BankPaymentVoucherRequest req) {
        switch (req.getRefType().toUpperCase()) {
            case "GENERAL" -> {
                if (req.getPayGlPoid() == null)
                    throw new ValidationException("Pay GL is mandatory for Ref Type = GENERAL.");
            }
            case "FF JOBS" -> {
                boolean hasFfRefs = req.getFfRefs() != null && req.getFfRefs().stream().anyMatch(r -> r != null && !r.isBlank());
                if (!hasFfRefs && req.getFfRefId() == null)
                    throw new ValidationException("FF Ref Id is mandatory for Ref Type = FF JOBS.");
            }
            case "FDA JOBS" -> {
                if (req.getFdaRefId() == null)
                    throw new ValidationException("FDA Ref Id is mandatory for Ref Type = FDA JOBS.");
            }
            case "MTA RFQ" -> {
                if (req.getMtaRfqId() == null && req.getSalesQtnRef() == null)
                    throw new ValidationException("MTA RFQ Id is mandatory for Ref Type = MTA RFQ.");
            }
            case "CUSTOM" -> {
                // optional
            }
            default -> throw new ValidationException("Invalid Ref Type: " + req.getRefType());
        }
    }

    // ============================================================
    // LEGACY DocumentBeforeSave VALIDATIONS
    // Ported from BankPaymentVoucherBean.java (ADF)
    // ============================================================

    /**
     * Cheque date must not be back-dated beyond the configured CHEQUE_DATE_VALIDATION_DAYS
     * Post-dated cheques are not allowed for MTA RFQ / FF JOBS / FDA JOBS
     * At least one detail row must exist for the matching ref type
     */
    private void validateBeforeSaveRequest(BankPaymentVoucherRequest req) {

        LocalDate today = DateUtil.getCurrentDateInUserTimeZone();

        validateChequeDate(req, today);
        validatePostDatedCheque(req, today);
        validateDetailsByRefType(req);
        validateInputTax(req);
    }

    private void validateChequeDate(BankPaymentVoucherRequest req, LocalDate today) {
        if (isEmpty(req.getChqDate())) return;

        LocalDate chqDate = LocalDate.parse(req.getChqDate());

        try {
            BigDecimal validateDays = new BigDecimal(
                    globalParameterService.getParameterValue("CHEQUE_DATE_VALIDATION_DAYS", "GROUP", "1", "0")
            );

            long noOfDays = java.time.temporal.ChronoUnit.DAYS.between(today, chqDate);
            BigDecimal noOfDaysBD = BigDecimal.valueOf(noOfDays);

            if (noOfDaysBD.compareTo(validateDays) < 0) {
                throw new ValidationException(
                        "Back Dated entries less than " + validateDays.abs() +
                                " days is not allowed from Payment Voucher, Please verify the Cheque Date...");
            }

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Cheque date validation skipped: {}", e.getMessage());
        }
    }

    private void validatePostDatedCheque(BankPaymentVoucherRequest req, LocalDate today) {
        if (isEmpty(req.getChqDate())) return;

        String refType = req.getRefType();
        if (!isRefType(refType, "MTA RFQ", "FF JOBS", "FDA JOBS")) return;

        LocalDate chqDate = LocalDate.parse(req.getChqDate());

        if (chqDate.isAfter(today)) {
            throw new ValidationException(
                    "Posted Date Cheques not allowed against MTA RFQ, FF, FDA...");
        }
    }

    private void validateDetailsByRefType(BankPaymentVoucherRequest req) {
        String refType = req.getRefType();
        if (refType == null) return;

        switch (refType.toUpperCase()) {
            case "FDA JOBS", "FF JOBS" -> {
                if (isEmpty(req.getChargeDetailRequests())) {
                    throw new ValidationException("No Details in this Transaction...");
                }
                validateChargePoid(req.getChargeDetailRequests());
            }
            case "MTA RFQ" -> {
                if (isEmpty(req.getItemDetailRequests())) {
                    throw new ValidationException("No Details in this Transaction...");
                }
            }
            case "GENERAL", "CUSTOM" -> {
                if (isEmpty(req.getGlDetails())) {
                    throw new ValidationException("No Details in this Transaction...");
                }
            }
        }
    }

    private void validateChargePoid(List<BankPaymentChargeDetailRequest> chargeDetails) {
        if (chargeDetails == null) return;
        for (int i = 0; i < chargeDetails.size(); i++) {
            BankPaymentChargeDetailRequest detail = chargeDetails.get(i);            
            if (detail.getChargePoid() == null) {
                throw new ValidationException(
                        "Charge is required for row " + (i + 1) + " in Charge Details.");
            }
        }
    }

    private void validateInputTax(BankPaymentVoucherRequest req) {

        String refType = req.getRefType();
        if (!isRefType(refType, "GENERAL", "CUSTOM")) return;

        List<BankPaymentGLDetailRequest> glDetails = req.getGlDetails();
        if (isEmpty(glDetails)) return;

        BigDecimal inputTaxLimit = getInputTaxLimit();
        if (inputTaxLimit == null) return;

        int displayRowNum = 0;
        for (int i = 0; i < glDetails.size(); i++) {
            BankPaymentGLDetailRequest row = glDetails.get(i);
            
            // Skip deleted rows
            if ("isDeleted".equalsIgnoreCase(row.getActionType())) {
                continue;
            }
            
            displayRowNum++;
            validateTax(row.getDrAmt(), row, inputTaxLimit, displayRowNum);
            validateTax(row.getCrAmt(), row, inputTaxLimit, displayRowNum);
        }
    }

    private void validateTax(BigDecimal amount, BankPaymentGLDetailRequest row,
                             BigDecimal limit, int rowNum) {

        if (amount == null || row.getTaxPercentage() == null) return;

        if (amount.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal taxPerc = row.getTaxPercentage()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal expectedTax = amount.multiply(taxPerc).setScale(3, RoundingMode.HALF_UP);

        BigDecimal enteredTax = row.getTaxAmount() != null
                ? row.getTaxAmount().setScale(3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(3);

        BigDecimal difference = enteredTax.subtract(expectedTax).abs();

        if (difference.compareTo(limit) > 0) {
            throw new ValidationException(
                    "WARNING : Input tax difference (" + difference +
                            "/-) should be within " + limit +
                            "/- Please note the row number " + rowNum);
        }
    }

    private boolean isEmpty(String val) {
        return val == null || val.trim().isEmpty();
    }

    private boolean isEmpty(Collection<?> list) {
        return list == null || list.isEmpty();
    }

    private boolean isRefType(String refType, String... values) {
        if (refType == null) return false;
        return Arrays.stream(values).anyMatch(v -> v.equalsIgnoreCase(refType));
    }

    private BigDecimal getInputTaxLimit() {
        try {
            return new BigDecimal(
                    globalParameterService.getParameterValue("INPUT_TAX_VARIANCE_LIMIT", "GROUP", "1", "0")
            );
        } catch (Exception e) {
            log.warn("INPUT_TAX_VARIANCE_LIMIT not available: {}", e.getMessage());
            return null;
        }
    }

    private GLPaymentVoucherHDREntity mapHeaderFromRequest(BankPaymentVoucherRequest req) {
        GLPaymentVoucherHDREntity entity = new GLPaymentVoucherHDREntity();

        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setBankPoid(req.getBankPoid());
        entity.setPayGlPoid(req.getPayGlPoid());
        entity.setTransactionDate(req.getTransactionDate());
        entity.setPayingTo(req.getPayingTo());
        entity.setRemarks(req.getRemarks());
        entity.setRefType(req.getRefType());
        entity.setFdaRef(req.getFdaRefId());
        entity.setFfRef(resolveEffectiveFfRef(req));
        entity.setMtaRef(req.getMtaRfqId());
        entity.setSalesQtnRef(req.getSalesQtnRef() != null ? req.getSalesQtnRef() :
                (StringUtils.isNumeric(req.getMtaRfqId()) ? Long.valueOf(req.getMtaRfqId()) : null));

        if (req.getChqDate() != null && !req.getChqDate().isEmpty()) {
            entity.setChqDate(LocalDate.parse(req.getChqDate()));
        }

        entity.setTransactionDate(LocalDate.now());

        entity.setChqCardNo(req.getChqCardNo());
        entity.setLongNarration(req.getLongNarration());
        entity.setSuppressValidation(req.getSuppressValidation());
        entity.setAccountPayee(req.getAccountPayee());
        entity.setMultiCompany(req.getMultiple());
        entity.setSecurityCheque(req.getSecurityCheque());
        entity.setCurrencyAmount(req.getCurrencyAmount());
        entity.setPrePrinted(req.getPrePrinted() != null ? req.getPrePrinted() : "N");
        entity.setReleased("N");
        entity.setHold("N");
        entity.setPrintWithoutBillwise("N");
        entity.setChequeIssuePhysical("N");
        entity.setDeleted("N");

        if (Boolean.TRUE.equals(req.getReleased())) {
            entity.setReleasedToPerson(req.getReleasedToPerson());
            entity.setReleasedPersonAddress(req.getContact());
            entity.setReleasedByUserCode(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
            entity.setReleasedDate(LocalDate.now());
            entity.setReleased("Y");
        }

        entity.setChqPrinted("N");
        entity.setChqPrintedUserCode(null);
        entity.setChqPrintedDate(null);
        entity.setAvailableBalance(req.getAvailableBalance());

        return entity;
    }

    // ============================================================
    // SAVE GL DETAILS
    // ============================================================

    private void saveGLDetails(List<BankPaymentGLDetailRequest> glDetails, Long transactionPoid, String documentId) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        List<GLPaymentVoucherDtlGLEntity> entities = new ArrayList<>();
        long rowId = 1;
        for (BankPaymentGLDetailRequest detail : glDetails) {
            GLPaymentVoucherDtlGLEntity entity = new GLPaymentVoucherDtlGLEntity();
            detail.setDetRowId(rowId++);
            mapGLFields(entity, detail, transactionPoid);
            entities.add(entity);
        }

        List<GLPaymentVoucherDtlGLEntity> savedEntities = paymentVoucherDetailsRepository.saveAll(entities);
        // Log child record creation
        savedEntities.forEach(entity -> {
            String logDetail = String.format("Row Created on GL Detail with detRowId: %s", entity.getDetRowId());
            loggingService.createLogSummaryEntry(documentId, transactionPoid.toString(), logDetail);
        });

        // Save billwise breakup for GL details
        saveBillwiseBreakup(glDetails, transactionPoid, documentId);
    }

    private void saveChargeDetails(List<BankPaymentChargeDetailRequest> chargeDetails, Long transactionPoid) {
        // Ensure the list is not null
        if (chargeDetails == null) chargeDetails = List.of();

        // Fetch existing charge entries for this transaction
        List<GlBankPaymentChargeDtlEntity> existing = chargeDtlRepository
                .findByTransactionPoid(transactionPoid);

        Map<Long, GlBankPaymentChargeDtlEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(GlBankPaymentChargeDtlEntity::getDetRowId, d -> d));

        List<GlBankPaymentChargeDtlEntity> toSave = new ArrayList<>();
        List<GlBankPaymentChargeDtlEntity> toDelete = new ArrayList<>();
        List<Long> createdDetRowIds = new ArrayList<>();
        List<LogRequestDto<GlBankPaymentChargeDtlEntity>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        // Auto-generate detRowId for new records
        Long maxDetRowId = existing.stream()
                .mapToLong(GlBankPaymentChargeDtlEntity::getDetRowId)
                .max().orElse(0L);

        for (BankPaymentChargeDetailRequest detail : chargeDetails) {
            String actionType = normalizeDetailActionType(detail.getActionType());

            switch (actionType) {
                case "ISCREATED":
                    // Create new record with auto-generated detRowId
                    GlBankPaymentChargeDtlEntity newEntity = new GlBankPaymentChargeDtlEntity();
                    detail.setDetRowId(++maxDetRowId); // Auto-generate detRowId
                    mapChargeFields(newEntity, detail, transactionPoid);
                    toSave.add(newEntity);
                    createdDetRowIds.add(newEntity.getDetRowId());
                    break;

                case "ISUPDATED":
                    // Update existing record, or recreate when missing (e.g. after approval reversal)
                    GlBankPaymentChargeDtlEntity existingEntity = existingMap.get(detail.getDetRowId());
                    if (existingEntity != null) {
                        GlBankPaymentChargeDtlEntity oldEntity = new GlBankPaymentChargeDtlEntity();
                        BeanUtils.copyProperties(existingEntity, oldEntity);

                        mapChargeFields(existingEntity, detail, transactionPoid);
                        toSave.add(existingEntity);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detail.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlBankPaymentChargeDtlEntity.class, documentId, transactionPoid.toString(), logDetail));
                    } else {
                        GlBankPaymentChargeDtlEntity recreatedEntity = new GlBankPaymentChargeDtlEntity();
                        if (detail.getDetRowId() == null) {
                            detail.setDetRowId(++maxDetRowId);
                        }
                        mapChargeFields(recreatedEntity, detail, transactionPoid);
                        toSave.add(recreatedEntity);
                        createdDetRowIds.add(recreatedEntity.getDetRowId());
                    }
                    break;

                case "ISDELETED":
                    // Mark for deletion
                    GlBankPaymentChargeDtlEntity entityToDelete = existingMap.get(detail.getDetRowId());
                    if (entityToDelete != null) {
                        toDelete.add(entityToDelete);
                        loggingService.logDelete(entityToDelete, documentId, transactionPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                default:
                    GlBankPaymentChargeDtlEntity unchangedEntity = existingMap.get(detail.getDetRowId());
                    if (unchangedEntity != null) {
                        toSave.add(unchangedEntity);
                    } else {
                        GlBankPaymentChargeDtlEntity recreatedUnchanged = new GlBankPaymentChargeDtlEntity();
                        if (detail.getDetRowId() == null) {
                            detail.setDetRowId(++maxDetRowId);
                        }
                        mapChargeFields(recreatedUnchanged, detail, transactionPoid);
                        toSave.add(recreatedUnchanged);
                        createdDetRowIds.add(recreatedUnchanged.getDetRowId());
                    }
                    break;
            }
        }

        // Delete records marked for deletion
        if (!toDelete.isEmpty()) {
            chargeDtlRepository.deleteAll(toDelete);
        }

        // Save all entities to the repository
        List<GlBankPaymentChargeDtlEntity> savedEntities = chargeDtlRepository.saveAll(toSave);

        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }

        // Log creation for new records
        savedEntities.stream()
                .filter(entity -> createdDetRowIds.contains(entity.getDetRowId()))
                .forEach(entity -> {
                    String logDetail = String.format("Row Created on Charge Detail with detRowId: %s", entity.getDetRowId());
                    loggingService.createLogSummaryEntry(documentId, transactionPoid.toString(), logDetail);
                });
    }

    private void mapChargeFields(GlBankPaymentChargeDtlEntity entity, BankPaymentChargeDetailRequest detail, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detail.getDetRowId());
        entity.setChargePoid(detail.getChargePoid());
        entity.setChargeAmount(detail.getChargeAmount());
        entity.setDescription(detail.getDescription());
        entity.setRemarks(detail.getRemarks());
        entity.setRefDocId(detail.getRefDocId());
        entity.setRefDocPoid(detail.getRefDocPoid());
        entity.setFdaDetRowId(detail.getFdaDetRowId());
        entity.setPdaAmount(detail.getPdaAmount());
        entity.setFfAmount(detail.getFfAmount());
    }

    // ============================================================
    // SAVE ITEM DETAILS (Composite key aware)
    // ============================================================

    private void saveItemDetails(List<BankPaymentItemDetailRequest> itemDetails, Long transactionPoid) {
        if (itemDetails == null) itemDetails = List.of();

        List<GlBankPaymentItemDtlEntity> existing = itemRepository
                .findByTransactionPoid(transactionPoid);

        Map<Long, GlBankPaymentItemDtlEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(GlBankPaymentItemDtlEntity::getDetRowId, d -> d));

        List<GlBankPaymentItemDtlEntity> toSave = new ArrayList<>();
        List<GlBankPaymentItemDtlEntity> toDelete = new ArrayList<>();
        List<Long> createdDetRowIds = new ArrayList<>();
        List<LogRequestDto<GlBankPaymentItemDtlEntity>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        // Auto-generate detRowId for new records
        Long maxDetRowId = existing.stream()
                .mapToLong(GlBankPaymentItemDtlEntity::getDetRowId)
                .max().orElse(0L);

        for (BankPaymentItemDetailRequest detail : itemDetails) {
            String actionType = normalizeDetailActionType(detail.getActionType());

            switch (actionType) {
                case "ISCREATED":
                    GlBankPaymentItemDtlEntity newEntity = new GlBankPaymentItemDtlEntity();
                    detail.setDetRowId(++maxDetRowId);
                    mapItemFields(newEntity, detail, transactionPoid);
                    toSave.add(newEntity);
                    createdDetRowIds.add(newEntity.getDetRowId());
                    break;

                case "ISUPDATED":
                    GlBankPaymentItemDtlEntity existingEntity = existingMap.get(detail.getDetRowId());
                    if (existingEntity != null) {
                        GlBankPaymentItemDtlEntity oldEntity = new GlBankPaymentItemDtlEntity();
                        BeanUtils.copyProperties(existingEntity, oldEntity);

                        mapItemFields(existingEntity, detail, transactionPoid);
                        toSave.add(existingEntity);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detail.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlBankPaymentItemDtlEntity.class, documentId, transactionPoid.toString(), logDetail));
                    } else {
                        GlBankPaymentItemDtlEntity recreatedEntity = new GlBankPaymentItemDtlEntity();
                        if (detail.getDetRowId() == null) {
                            detail.setDetRowId(++maxDetRowId);
                        }
                        mapItemFields(recreatedEntity, detail, transactionPoid);
                        toSave.add(recreatedEntity);
                        createdDetRowIds.add(recreatedEntity.getDetRowId());
                    }
                    break;

                case "ISDELETED":
                    GlBankPaymentItemDtlEntity entityToDelete = existingMap.get(detail.getDetRowId());
                    if (entityToDelete != null) {
                        toDelete.add(entityToDelete);
                        loggingService.logDelete(entityToDelete, documentId, transactionPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                default:
                    GlBankPaymentItemDtlEntity unchangedEntity = existingMap.get(detail.getDetRowId());
                    if (unchangedEntity != null) {
                        toSave.add(unchangedEntity);
                    } else {
                        GlBankPaymentItemDtlEntity recreatedUnchanged = new GlBankPaymentItemDtlEntity();
                        if (detail.getDetRowId() == null) {
                            detail.setDetRowId(++maxDetRowId);
                        }
                        mapItemFields(recreatedUnchanged, detail, transactionPoid);
                        toSave.add(recreatedUnchanged);
                        createdDetRowIds.add(recreatedUnchanged.getDetRowId());
                    }
                    break;
            }
        }

        // Delete records marked for deletion
        if (!toDelete.isEmpty()) {
            itemRepository.deleteAll(toDelete);
        }

        List<GlBankPaymentItemDtlEntity> savedEntities = itemRepository.saveAll(toSave);

        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }

        // Log creation for new records
        savedEntities.stream()
                .filter(entity -> createdDetRowIds.contains(entity.getDetRowId()))
                .forEach(entity -> {
                    String logDetail = String.format("Row Created on Item Detail with detRowId: %s", entity.getDetRowId());
                    loggingService.createLogSummaryEntry(documentId, transactionPoid.toString(), logDetail);
                });
    }

    private void mapItemFields(GlBankPaymentItemDtlEntity entity, BankPaymentItemDetailRequest detail, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detail.getDetRowId());
        entity.setStockPoid(detail.getStockPoid());
        entity.setStockUnitPoid(detail.getStockUnitPoid());
        entity.setPoQty(detail.getPoQty());
        entity.setDnQty(detail.getDnQty());
        entity.setQtyReceived(detail.getQtyReceived());
        entity.setPrice(detail.getPrice());
        entity.setDiscount(detail.getDiscount());
        entity.setTotal(detail.getTotal());
        entity.setRemarks(detail.getRemarks());
        entity.setRefDocId(detail.getRefDocId());
        entity.setRefDocPoid(detail.getRefDocPoid());
        entity.setRefDetRowId(detail.getRefDetRowId());
    }

    // ============================================================
    // NEW METHODS - STORED PROCEDURE OPERATIONS
    // ============================================================

    @Override
    public Map<String, BigDecimal> getBankBalance(String docId, Long docKeyPoid, LocalDate docDate, Long bankPoid) {
        return spRepository.getBankBalance(docId, docKeyPoid, docDate, bankPoid);
    }

    /*@Override
    public List<BankPaymentChargeDetailResponse> loadFfCharges(Long ffRefId) {
        return loadDataRepository.loadFfCharges(ffRefId);
    }*/

    @Override
    public BankPayCreateFromFfResponse createBankPayFromFf(String ffPoid) {
        return loadDataRepository.executeBankPayFromFf(ffPoid);
    }

   /* @Override
    public List<BankPaymentChargeDetailResponse> loadFdaCharges(Long fdaRefId) {
        return loadDataRepository.loadFdaCharges(fdaRefId);
    }*/

    @Override
    public BankPayCreateFromFdaResponse createBankPayFromFda(String fdaPoid) {
        return loadDataRepository.executeBankPayFromFda(fdaPoid);
    }

   /* @Override
    public List<BankPaymentItemDetailResponse> loadMtaItems(Long mtaRfqId) {
        return loadDataRepository.loadMtaItems(mtaRfqId);
    }*/

    public BankPayCreateFromMtaResponse createBankPayment(String rfqPoid) {
        return loadDataRepository.executeBankPayProc(rfqPoid);
    }

    @Override
    @Transactional
    public void validateChequePrint(Long transactionPoid) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found"));
        spRepository.validateBeforeChequePrint(
                UserContext.getGroupPoid(),
                String.valueOf(UserContext.getUserPoid()),
                UserContext.getCompanyPoid(),
                header.getBankPoid(),
                header.getChqSignType(),
                header.getTransactionPoid(),
                header.getSuppressValidation()
        );
    }

    @Override
    @Transactional
    public void markChequePrinted(Long transactionPoid) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found"));

        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(header.getTransactionPoid())
                .chqPrinted(header.getChqPrinted())
                .chqPrintedUserCode(header.getChqPrintedUserCode())
                .chqPrintedDate(header.getChqPrintedDate())
                .build();

        spRepository.afterChequePrint(
                UserContext.getGroupPoid(),
                String.valueOf(UserContext.getUserPoid()),
                UserContext.getCompanyPoid(),
                transactionPoid,
                header.getBankPoid(),
                header.getChqSignType(),
                UserContext.getUserPoid()
        );

        // Reload entity to get updated values
        GLPaymentVoucherHDREntity updatedHeader = paymentVoucherRepository.findById(transactionPoid)
                .orElse(header);

        // Log the update
        String key = transactionPoid.toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, updatedHeader, GLPaymentVoucherHDREntity.class,
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
    }

    @Override
    @Transactional
    public void releaseCheque(Long transactionPoid, String releasedTo, String contact) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found"));

        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = new GLPaymentVoucherHDREntity();
        BeanUtils.copyProperties(header, oldEntity);

        spRepository.releaseCheque(
                header.getGroupPoid(),
                getCurrentUser(),
                header.getCompanyPoid(),
                transactionPoid,
                releasedTo,
                contact
        );

        // Reload entity to get updated values
        entityManager.refresh(header);
        GLPaymentVoucherHDREntity updatedHeader =header;

        // Log the update
        String key = transactionPoid.toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, updatedHeader, GLPaymentVoucherHDREntity.class,
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
    }

    @Override
    @Transactional
    public void unReleaseCheque(Long transactionPoid) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found"));

        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = new GLPaymentVoucherHDREntity();
        BeanUtils.copyProperties(header, oldEntity);

        spRepository.unReleaseCheque(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                transactionPoid
        );

        // Reload entity to get updated values
        entityManager.refresh(header);
        GLPaymentVoucherHDREntity updatedHeader = header;

        // Log the update
        String key = transactionPoid.toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, updatedHeader, GLPaymentVoucherHDREntity.class,
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
    }

    @Override
    @Transactional
    public void resetChequeStatus(Long transactionPoid) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found"));

        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(header.getTransactionPoid())
                .chqPrinted(header.getChqPrinted())
                .chqPrintedUserCode(header.getChqPrintedUserCode())
                .chqPrintedDate(header.getChqPrintedDate())
                .released(header.getReleased())
                .releasedToPerson(header.getReleasedToPerson())
                .releasedPersonAddress(header.getReleasedPersonAddress())
                .releasedByUserCode(header.getReleasedByUserCode())
                .releasedDate(header.getReleasedDate())
                .build();

        spRepository.resetChequeStatus(
                header.getGroupPoid(),
                header.getCompanyPoid(),
                null,
                transactionPoid
        );

        // Reload entity to get updated values
        GLPaymentVoucherHDREntity updatedHeader = paymentVoucherRepository.findById(transactionPoid)
                .orElse(header);

        // Log the update
        String key = transactionPoid.toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, updatedHeader, GLPaymentVoucherHDREntity.class,
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
    }

    @Override
    @Transactional
    public String revertReconciliation(Long transactionPoid, String documentId, String comments) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found"));

        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(header.getTransactionPoid())
                .reconciledDate(header.getReconciledDate())
                .build();

        String status = spRepository.revertReconciliation(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                documentId,
                String.valueOf(transactionPoid),
                "Y",
                comments
        );

        // Reload entity to get updated values
        GLPaymentVoucherHDREntity updatedHeader = paymentVoucherRepository.findById(transactionPoid)
                .orElse(header);

        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, updatedHeader, GLPaymentVoucherHDREntity.class,
                documentId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return status;
    }

    // ============================================================
    // HELPER METHODS FOR SP OPERATIONS
    // ============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void validateBeforeSaveInNewTransaction(GLPaymentVoucherHDREntity header) {
        try {
            spRepository.validateBeforeSave(
                    header.getTransactionPoid(),
                    header.getGroupPoid(),
                    header.getCompanyPoid(),
                    getCurrentUser(),
                    "N"
            );
        } catch (Exception e) {
            log.warn("Before save validation failed: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void validateJobInNewTransaction(Long groupPoid, Long companyPoid, String refPoid, String refType, String docId, Long userPoid) {
        try {
            String result = spRepository.validateJob(groupPoid, userPoid, companyPoid, docId, refType, refPoid);
            if (result != null) {
                if (result.contains("CLOSED")) {
                    throw new ValidationException("WARNING : Selected MTA RFQ is in closed status,Unable to save");
                }
                if (result.startsWith("ERROR") || result.startsWith("WARNING")) {
                    throw new ValidationException(result);
                }
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Job validation failed: {}", e.getMessage());
            throw new ValidationException("Job validation failed: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void validateVoucherStatusInNewTransaction(GLPaymentVoucherHDREntity header) {
        try {
            String refString = resolveRefStringFromEntity(header, header.getRefType());
            String result = spRepository.validateVoucherStatus(
                    header.getGroupPoid(),
                    null,
                    header.getCompanyPoid(),
                    header.getDocRef(),
                    header.getRefType(),
                    refString
            );
            if (result != null && !result.equals("SUCCESS")) {
                throw new ValidationException(result);
            }
        } catch (Exception e) {
            log.error("Voucher status validation failed: {}", e.getMessage());
            throw new ValidationException("Voucher cannot be modified: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void releaseOldJobValuesInNewTransaction(Long transactionPoid, String refString) {
        try {
            GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ValidationException("Voucher not found"));
            spRepository.releaseOldJobValues(
                    header.getGroupPoid(),
                    null,
                    header.getCompanyPoid(),
                    header.getDocRef(),
                    String.valueOf(transactionPoid)
            );
        } catch (Exception e) {
            log.warn("Failed to release old job values: {}", e.getMessage());
        }
    }

    private void scheduleAfterSaveJobCostUpdates(GLPaymentVoucherHDREntity header,
                                                BankPaymentVoucherRequest req,
                                                Long transactionPoid,
                                                String oldRefType,
                                                String oldRefPoid) {
        Long groupPoid = header.getGroupPoid();
        Long companyPoid = header.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        String newRefType = req.getRefType();

        Runnable job = () -> runAfterSaveJobCostUpdates(
                groupPoid, companyPoid, userPoid, transactionPoid, newRefType, req, oldRefType, oldRefPoid);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    
    private void runAfterSaveJobCostUpdates(Long groupPoid,
                                            Long companyPoid,
                                            Long userPoid,
                                            Long transactionPoid,
                                            String newRefType,
                                            BankPaymentVoucherRequest req,
                                            String oldRefType,
                                            String oldRefPoid) {
        String normalizedNewRefType = normalizeRefType(newRefType);
        log.info("[AfterSaveJobCost] txnPoid={} oldRefType='{}' oldRefPoid='{}' newRefType='{}'",
                transactionPoid, oldRefType, oldRefPoid, normalizedNewRefType);

        if (hasText(oldRefType) && hasText(oldRefPoid)) {
            String normalizedOldRefType = normalizeRefType(oldRefType);
            String newRefForOldType = resolveRefPoidFromRequest(req, normalizedOldRefType);
            boolean referenceChanged = !normalizedOldRefType.equals(normalizedNewRefType)
                    || !oldRefPoid.trim().equals(newRefForOldType == null ? "" : newRefForOldType.trim());
            if (referenceChanged) {
                log.info("[AfterSaveJobCost] Invoking OLD-ref proc: refType='{}' refPoid='{}'",
                        normalizedOldRefType, oldRefPoid);
                executeJobCostProc(normalizedOldRefType, oldRefPoid, groupPoid, companyPoid, userPoid, transactionPoid);
            }
        }

        String newRefPoid = resolveRefPoidFromRequest(req, normalizedNewRefType);
        if (hasText(normalizedNewRefType) && hasText(newRefPoid)) {
            log.info("[AfterSaveJobCost] Invoking NEW-ref proc: refType='{}' refPoid='{}'",
                    normalizedNewRefType, newRefPoid);
            executeJobCostProc(normalizedNewRefType, newRefPoid, groupPoid, companyPoid, userPoid, transactionPoid);
        }
    }

    private void executeJobCostProc(String refType,
                                    String refPoid,
                                    Long groupPoid,
                                    Long companyPoid,
                                    Long userPoid,
                                    Long transactionPoid) {
        String normalizedRefType = normalizeRefType(refType);
        try {
            String procResult = switch (normalizedRefType) {
                case "FDA JOBS" -> spRepository.updateFdaCost(
                        groupPoid, companyPoid, userPoid, refPoid, transactionPoid);
                case "FF JOBS" -> spRepository.updateFfCost(
                        groupPoid, companyPoid, userPoid, refPoid, transactionPoid);
                case "MTA RFQ" -> spRepository.updateMtaCost(
                        groupPoid, companyPoid, userPoid, transactionPoid, refPoid);
                default -> null;
            };
            if (procResult != null && procResult.toUpperCase().contains("ERROR")) {
                log.warn("Job cost update failed for {} ref {}: {}", refType, refPoid, procResult);
            } else {
                log.info("Job cost update for {} ref {} => {}", refType, refPoid, procResult);
            }
        } catch (Exception e) {
            log.warn("Failed to update job costs for {} ref {}: {}", refType, refPoid, e.getMessage());
        }
    }

    private String resolveRefPoidFromRequest(BankPaymentVoucherRequest req, String refType) {
        return switch (normalizeRefType(refType)) {
            case "FF JOBS" -> resolveEffectiveFfRef(req);
            case "FDA JOBS" -> req.getFdaRefId() != null ? String.valueOf(req.getFdaRefId()) : null;
            case "MTA RFQ" -> req.getSalesQtnRef() != null
                    ? String.valueOf(req.getSalesQtnRef())
                    : req.getMtaRfqId();
            default -> null;
        };
    }

    private String normalizeRefType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String resolveRefStringFromEntity(GLPaymentVoucherHDREntity entity, String refType) {
        return switch (refType != null ? refType.toUpperCase() : "") {
            case "FDA JOBS" -> entity.getFdaRef() != null ? String.valueOf(entity.getFdaRef()) : null;
            case "FF JOBS"  -> entity.getFfRef();
            case "MTA RFQ"  -> entity.getSalesQtnRef() != null ? String.valueOf(entity.getSalesQtnRef()) : entity.getMtaRef();
            default         -> null;
        };
    }

    private String resolveEffectiveFfRef(BankPaymentVoucherRequest req) {
        List<String> refs = req.getFfRefs();
        if (refs != null && !refs.isEmpty()) {
            String joined = refs.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .collect(Collectors.joining(";"));
            if (!joined.isBlank()) return joined;
        }
        return req.getFfRefId() != null ? String.valueOf(req.getFfRefId()) : null;
    }

    private String getCurrentUser() {
        return Objects.requireNonNull(UserContext.getCurrentUser()).getUserName();
    }

    // ============================================================
    // UPDATE GL DETAILS WITH BILLWISE BREAKUP
    // ============================================================

    private void updateGLDetails(List<BankPaymentGLDetailRequest> glDetails, Long transactionPoid, String documentId) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        // Get existing records
        List<GLPaymentVoucherDtlGLEntity> existing = paymentVoucherDetailsRepository.findByTransactionPoid(transactionPoid);
        Map<Long, GLPaymentVoucherDtlGLEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(GLPaymentVoucherDtlGLEntity::getDetRowId, d -> d));

        List<GLPaymentVoucherDtlGLEntity> toSave = new ArrayList<>();
        List<GLPaymentVoucherDtlGLEntity> toDelete = new ArrayList<>();
        List<Long> createdDetRowIds = new ArrayList<>();
        List<LogRequestDto<GLPaymentVoucherDtlGLEntity>> logRequests = new ArrayList<>();

        // Auto-generate detRowId for new records
        Long maxDetRowId = existing.stream()
                .mapToLong(GLPaymentVoucherDtlGLEntity::getDetRowId)
                .max().orElse(0L);

        for (BankPaymentGLDetailRequest detail : glDetails) {
            String actionType = normalizeDetailActionType(detail.getActionType());

            switch (actionType) {
                case "ISCREATED":
                    GLPaymentVoucherDtlGLEntity newEntity = new GLPaymentVoucherDtlGLEntity();
                    detail.setDetRowId(++maxDetRowId);
                    mapGLFields(newEntity, detail, transactionPoid);
                    toSave.add(newEntity);
                    createdDetRowIds.add(newEntity.getDetRowId());
                    break;

                case "ISUPDATED":
                    GLPaymentVoucherDtlGLEntity existingEntity = existingMap.get(detail.getDetRowId());
                    if (existingEntity != null) {
                        GLPaymentVoucherDtlGLEntity oldEntity = new GLPaymentVoucherDtlGLEntity();
                        BeanUtils.copyProperties(existingEntity, oldEntity);

                        mapGLFields(existingEntity, detail, transactionPoid);
                        toSave.add(existingEntity);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detail.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GLPaymentVoucherDtlGLEntity.class, documentId, transactionPoid.toString(), logDetail));
                    } else {
                        GLPaymentVoucherDtlGLEntity recreatedEntity = new GLPaymentVoucherDtlGLEntity();
                        if (detail.getDetRowId() == null) {
                            detail.setDetRowId(++maxDetRowId);
                        }
                        mapGLFields(recreatedEntity, detail, transactionPoid);
                        toSave.add(recreatedEntity);
                        createdDetRowIds.add(recreatedEntity.getDetRowId());
                    }
                    break;

                case "ISDELETED":
                    GLPaymentVoucherDtlGLEntity entityToDelete = existingMap.get(detail.getDetRowId());
                    if (entityToDelete != null) {
                        toDelete.add(entityToDelete);
                        loggingService.logDelete(entityToDelete, documentId, transactionPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                default:
                    GLPaymentVoucherDtlGLEntity unchangedEntity = existingMap.get(detail.getDetRowId());
                    if (unchangedEntity != null) {
                        toSave.add(unchangedEntity);
                    } else {
                        GLPaymentVoucherDtlGLEntity recreatedUnchanged = new GLPaymentVoucherDtlGLEntity();
                        if (detail.getDetRowId() == null) {
                            detail.setDetRowId(++maxDetRowId);
                        }
                        mapGLFields(recreatedUnchanged, detail, transactionPoid);
                        toSave.add(recreatedUnchanged);
                        createdDetRowIds.add(recreatedUnchanged.getDetRowId());
                    }
                    break;
            }
        }

        // Delete records marked for deletion
        if (!toDelete.isEmpty()) {
            paymentVoucherDetailsRepository.deleteAll(toDelete);
        }

        // Save records and log creations
        List<GLPaymentVoucherDtlGLEntity> savedEntities = paymentVoucherDetailsRepository.saveAllAndFlush(toSave);

        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }

        savedEntities.stream()
                .filter(entity -> createdDetRowIds.contains(entity.getDetRowId()))
                .forEach(entity -> {
                    String logDetail = String.format("Row Created on GL Detail with detRowId: %s", entity.getDetRowId());
                    loggingService.createLogSummaryEntry(documentId, transactionPoid.toString(), logDetail);
                });

        // Update billwise breakup (keep existing logic)
        updateBillwiseBreakup(glDetails, transactionPoid, documentId);
    }

    private void mapGLFields(GLPaymentVoucherDtlGLEntity entity, BankPaymentGLDetailRequest detail, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detail.getDetRowId());
        String type = resolveGlDetailType(detail);
        entity.setType(type);
        entity.setBankGlInd(resolveBankGlInd(detail, type));
        entity.setCompanyPoid(detail.getCompanyPoid());
        entity.setGlPoid(detail.getGlPoid());
        entity.setDrAmt(detail.getDrAmt());
        entity.setCrAmt(detail.getCrAmt());
        entity.setTaxPoid(detail.getTaxPoid());
        entity.setTaxPercentage(detail.getTaxPercentage());
        entity.setTaxAmount(detail.getTaxAmount());
        entity.setTotalAmount(detail.getTotalAmount());
        entity.setPartyInvNumber(detail.getPartyInvNumber());
        entity.setPartyInvDate(detail.getPartyInvDate());
        entity.setRemarks(detail.getRemarks());
    }

    private String resolveGlDetailType(BankPaymentGLDetailRequest detail) {
        if (StringUtils.isNotBlank(detail.getType())) {
            return detail.getType().trim().toUpperCase();
        }
        if (detail.getDrAmt() != null && detail.getDrAmt().compareTo(BigDecimal.ZERO) > 0) {
            return GL_TYPE_DR;
        }
        if (detail.getCrAmt() != null && detail.getCrAmt().compareTo(BigDecimal.ZERO) > 0) {
            return GL_TYPE_CR;
        }
        return detail.getType();
    }

    private String resolveBankGlInd(BankPaymentGLDetailRequest detail, String type) {
        if (StringUtils.isNotBlank(detail.getBankGlInd())) {
            return detail.getBankGlInd().trim();
        }
        if (GL_TYPE_CR.equalsIgnoreCase(type)) {
            return BANK_GL_IND_BANK_ROW;
        }
        return null;
    }


    private void saveBillwiseBreakup(List<BankPaymentGLDetailRequest> glDetails, Long transactionPoid, String documentId) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        List<BillwiseBreakupRequestDto> breakupList = new ArrayList<>();

        for (BankPaymentGLDetailRequest glDetail : glDetails) {

            // -------------------------------
            // 1️⃣ Billwise breakup
            // -------------------------------
            if (glDetail.getBillWiseBreakup() != null && !glDetail.getBillWiseBreakup().isEmpty()) {
                for (BillwiseBreakupPopupRequestDto popup : glDetail.getBillWiseBreakup()) {
                    BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();

                    dto.setGroupPoid(UserContext.getGroupPoid());
                    dto.setCompanyPoid(UserContext.getCompanyPoid());
                    dto.setGlCompanyPoid(popup.getGlCompanyPoid() != null ? popup.getGlCompanyPoid() : (glDetail.getCompanyPoid() != null ? glDetail.getCompanyPoid() : UserContext.getCompanyPoid()));
                    dto.setDocId(documentId);
                    dto.setLoginUserPoid(UserContext.getUserPoid());
                    dto.setTransactionPoid(transactionPoid);
                    dto.setGlPoid(glDetail.getGlPoid());
                    // GL → Billwise mapping
                    dto.setMainDetRowId(glDetail.getDetRowId());
                    dto.setBillDetRowId(popup.getBillDetRowId());
                    dto.setBillRefType(popup.getBillRefType());
                    dto.setBillRef(popup.getBillRef());
                    dto.setBillDueDate(popup.getBillDueDate());
                    dto.setBillOriginalAmount(popup.getAmount());
                    if ("DR".equalsIgnoreCase(popup.getType())) {
                        dto.setDrAmt(popup.getAmount());
                        dto.setCrAmt(BigDecimal.ZERO);
                    } else {
                        dto.setCrAmt(popup.getAmount());
                        dto.setDrAmt(BigDecimal.ZERO);
                    }
                    dto.setBillRemarks(popup.getBillRemarks());

                    breakupList.add(dto);
                }
            }
        }

        if (!breakupList.isEmpty()) {
            billwiseBreakupRepository.insertBillwiseBreakup(breakupList);
        }

        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();

        for (BankPaymentGLDetailRequest glDetail : glDetails) {

            if (glDetail.getCostCenterBreakup() != null && !glDetail.getCostCenterBreakup().isEmpty()) {
                for (CostCenterBreakupPopupRequestDto popup : glDetail.getCostCenterBreakup()) {
                    CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
                    dto.setGroupPoid(UserContext.getGroupPoid());
                    dto.setCompanyPoid(UserContext.getCompanyPoid());
                    dto.setDocId(documentId);
                    dto.setTransactionPoid(transactionPoid);
                    dto.setMainDetRowId(glDetail.getDetRowId());
                    dto.setGlPoid(glDetail.getGlPoid());
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
            costCenterBreakupDtlRepository.insertCostBreakup(costCenterList);
        }
    }


    // ============================================================
    // UPDATE BILLWISE BREAKUP
    // ============================================================

    private void updateBillwiseBreakup(List<BankPaymentGLDetailRequest> glDetails, Long transactionPoid, String documentId) {


        List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
        Long userPoid = UserContext.getUserPoid();

        for (BankPaymentGLDetailRequest glDetail : glDetails) {

            if (glDetail.getBillWiseBreakup() != null &&
                    !glDetail.getBillWiseBreakup().isEmpty()) {

                for (BillwiseBreakupPopupRequestDto popup : glDetail.getBillWiseBreakup()) {

                    BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();

                    dto.setGroupPoid(UserContext.getGroupPoid());
                    dto.setCompanyPoid(UserContext.getCompanyPoid());
                    dto.setGlCompanyPoid(popup.getGlCompanyPoid() != null ? popup.getGlCompanyPoid() : (glDetail.getCompanyPoid() != null ? glDetail.getCompanyPoid() : UserContext.getCompanyPoid()));
                    dto.setDocId(documentId);
                    dto.setTransactionPoid(transactionPoid); // SAME HDR POID
                    dto.setLoginUserPoid(userPoid);
                    dto.setGlPoid(glDetail.getGlPoid());
                    dto.setMainDetRowId(glDetail.getDetRowId());
                    dto.setBillDetRowId(popup.getBillDetRowId());   // GL Mapping
                    dto.setBillRefType(popup.getBillRefType());
                    dto.setBillRef(popup.getBillRef());
                    dto.setBillDueDate(popup.getBillDueDate());
                    if ("DR".equalsIgnoreCase(popup.getType())) {
                        dto.setDrAmt(popup.getAmount());
                        dto.setCrAmt(BigDecimal.ZERO);
                    } else {
                        dto.setCrAmt(popup.getAmount());
                        dto.setDrAmt(BigDecimal.ZERO);
                    }
                    dto.setBillRemarks(popup.getBillRemarks());

                    billwiseList.add(dto);
                }
            }
        }

        if (!billwiseList.isEmpty()) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseList, userPoid);
        }

        // === COST CENTER BREAKUP ===
        // -------------------------

        costCenterBreakupDtlRepository.deleteCostCenters(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                documentId,
                transactionPoid,
                userPoid
        );

        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();

        for (BankPaymentGLDetailRequest glDetail : glDetails) {
            if (glDetail.getCostCenterBreakup() != null && !glDetail.getCostCenterBreakup().isEmpty()) {
                for (CostCenterBreakupPopupRequestDto popup : glDetail.getCostCenterBreakup()) {
                    CostCenterBreakupRequestDto cc = new CostCenterBreakupRequestDto();
                    cc.setGroupPoid(UserContext.getGroupPoid());
                    cc.setCompanyPoid(UserContext.getCompanyPoid());
                    cc.setDocId(documentId);
                    cc.setTransactionPoid(transactionPoid);
                    cc.setMainDetRowId(glDetail.getDetRowId());
                    cc.setGlPoid(glDetail.getGlPoid());
                    cc.setCostDetRowId(popup.getCostDetRowId());
                    cc.setCostGroup(popup.getCostGroup());
                    cc.setCostPoid(popup.getCostPoid());
                    cc.setAmount(popup.getAmount());
                    cc.setLoginUserPoid(userPoid);

                    costCenterList.add(cc);
                }
            }
        }

        if (!costCenterList.isEmpty()) {
            costCenterBreakupDtlRepository.insertCostBreakup(costCenterList);
        }
    }

    @Override
    public Map<String, Object> listBankPaymentVouchers(String documentId, FilterRequestDto request, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDateValue, endDateValue);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "TRANSACTION_POID",
                "DOC_REF");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private List<BillwiseBreakupPopupRequestDto> mapToPopupDto(List<LoadBillwiseBreakupResponseDto> list) {

        if (list == null) return Collections.emptyList();

        return list.stream().map(src -> {
            boolean isDebit = src.getDrAmt() != null && src.getDrAmt().compareTo(BigDecimal.ZERO) > 0;
            boolean isCredit = src.getCrAmt() != null && src.getCrAmt().compareTo(BigDecimal.ZERO) > 0;
            String type = isDebit ? "DR" : "CR";
            BigDecimal amount = isDebit ? src.getDrAmt() : (isCredit ? src.getCrAmt() : BigDecimal.ZERO);
            return BillwiseBreakupPopupRequestDto.builder()
                    .billDetRowId(src.getBillDetRowId())
                    .billRefType(src.getBillRefType())
                    .billRef(src.getBillRef())
                    .billDueDate(src.getBillDueDate())
                    .billOriginalAmount(src.getBillOriginalAmount() != null ? src.getBillOriginalAmount() : amount)
                    .type(type)
                    .amount(amount)
                    .billRemarks(src.getBillRemarks())
                    .build();
        }).collect(Collectors.toList());
    }

    private List<CostCenterBreakupPopupRequestDto> mapToCostCenterPopupDto(List<CostCenterBreakupResponseDto> list) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .map(cc -> {
                    CostCenterBreakupPopupRequestDto dto = CostCenterBreakupPopupRequestDto.builder()
                            .costDetRowId(cc.getCostDetRowId())
                            .costGroup(cc.getCostGroup())
                            .costPoid(cc.getCostPoid())
                            .amount(cc.getAmount())
                            .build();
                    if (cc.getCostPoid() != null && !cc.getCostPoid().isEmpty() &&
                            cc.getCostGroup() != null && !cc.getCostGroup().isEmpty()) {

                        if (StringUtils.isNotEmpty(cc.getCostPoid()) && StringUtils.isNotEmpty(cc.getCostGroup())) {
                            try {
                                Long poid = Long.parseLong(cc.getCostPoid());
                                dto.setCostCenterDetails(lovService.getDetailsByPoidAndLovName(poid, cc.getCostGroup()));
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
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found with ID: " + transactionPoid));

        Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
        if (header.getRefType() != null && header.getRefType().contains("CUSTOM")) {
            params.put("P_PRINT_WITHOUT_BILL", "Y");
        } else {
            params.put("P_PRINT_WITHOUT_BILL", "N");
        }

        JasperReport mainReport = null;
        if (null != header.getPrePrinted() && header.getPrePrinted().contains("Y")) {
            params.put("SUB_DETAIL", printService.load("Finance/BankPayments/BankPaymentVoucher_ManualCheque1_subreport1.jrxml"));
            mainReport = printService.load("Finance/BankPayments/BankPaymentVoucher_ManualCheque.jrxml");
        } else {
            mainReport = printService.load("Finance/BankPayments/BankPaymentVoucher_WithOutCheque.jrxml");
        }
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
    @Override
    public byte[] printchequeLeaf(Long transactionPoid) throws Exception {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found with ID: " + transactionPoid));
    
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
        params.put("BANK_POID", header.getBankPoid());
        params.put("ACCOUNT_PAYEE_IMG", "jasper/Finance/BankPayments/AccountsPayeeOnly.png");
        JasperReport mainReport = null;
        if (null != header.getPrePrinted() && header.getPrePrinted().contains("Y")) {
            mainReport = printService.load("Finance/BankPayments/BankPaymentVoucherChequeLeaf.jrxml");
        }
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }


    @Override
    public ReconcileResultDto getReconciledDate(String documentId, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_DEBIT_PAYMENT_RECON_DATE");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", documentId);
        query.setParameter("P_DOC_KEY_POID", transactionPoid);

        query.execute();

        Object cursor = query.getOutputParameterValue("OUTDATA");
        return mapReconCursorToDto(cursor);
    }

    private ReconcileResultDto mapReconCursorToDto(Object cursor) {
        try {
            ResultSet rs = (ResultSet) cursor;
            if (rs != null && rs.next()) {
                return new ReconcileResultDto(
                        rs.getString("RECONCILE_DATE"),
                        rs.getString("HOLD")
                );
            }
        } catch (SQLException e) {
            log.error("Error reading PROC_DEBIT_PAYMENT_RECON_DATE cursor", e);
            throw new RuntimeException("Error reading reconcile date result", e);
        }
        return new ReconcileResultDto(null, null);
    }

    @Override
    public Map<String, Object> getBankBeneficiary(String documentId, Long transactionPoid, Long payGlPoid) {
        if (payGlPoid == null) {
            return Collections.emptyMap();
        }
        return spRepository.getBankBeneficiary(documentId, transactionPoid, String.valueOf(payGlPoid));
    }

    @Override
    @Transactional
    public Map<String, String> updateSuppressValidation(Long transactionPoid, com.asg.finance.dto.BankPaymentSuppressRequest request) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Bank Payment Voucher not found for ID: " + transactionPoid));

        if (StringUtils.isBlank(request.getSuppressValidation())) {
            return Map.of("suppressValidation", StringUtils.defaultString(header.getSuppressValidation()));
        }

        String suppressValue = request.getSuppressValidation().trim().toUpperCase();
        if (!"Y".equals(suppressValue) && !"N".equals(suppressValue)) {
            throw new ValidationException("suppressValidation must be Y or N");
        }

        GLPaymentVoucherHDREntity oldEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(header.getTransactionPoid())
                .suppressValidation(header.getSuppressValidation())
                .build();

        header.setSuppressValidation(suppressValue);
        paymentVoucherRepository.save(header);

        loggingService.logChanges(oldEntity, header, GLPaymentVoucherHDREntity.class,
                UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return Map.of("suppressValidation", suppressValue);
    }

}
