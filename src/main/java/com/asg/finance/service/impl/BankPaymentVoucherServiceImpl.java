package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GLPaymentVoucherDtlGLEntity;
import com.asg.finance.entity.GLPaymentVoucherHDREntity;
import com.asg.finance.entity.GlBankPaymentChargeDtlEntity;
import com.asg.finance.entity.GlBankPaymentItemDtlEntity;
import com.asg.finance.repository.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BankPaymentVoucherService;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.sql.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankPaymentVoucherServiceImpl implements BankPaymentVoucherService {

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

    @Autowired private PrintService printService;
    @Autowired private DataSource dataSource;
    @Autowired private LoggingService loggingService;
    @Autowired private DocumentDeleteService documentDeleteService;

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

   for(BankPaymentGLDetailResponse dtl : response.getGlDetails()){
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
    public BankPaymentVoucherResponse createBankPaymentVoucher(BankPaymentVoucherRequest req, String documentId) {

        validateRefType(req);
        if ("FF JOBS".equalsIgnoreCase(req.getRefType()) && req.getFfRefId() != null) {
            validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), String.valueOf(req.getFfRefId()), "FF JOBS");
        }
        if ("FDA JOBS".equalsIgnoreCase(req.getRefType()) && req.getFdaRefId() != null) {
            validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), String.valueOf(req.getFdaRefId()), "FDA JOBS");
        }

        GLPaymentVoucherHDREntity entity = mapHeaderFromRequest(req);

        if (req.getChequeNo() == null && req.getBankPoid() != null) {
            try {
                String nextCheque = spRepository.getNextChequeNumber(req.getBankPoid());
                entity.setChqCardNo(nextCheque);
            } catch (Exception e) {
                log.warn("Failed to auto-generate cheque number: {}", e.getMessage());
                // Continue without auto-generated cheque number
            }
        }

        GLPaymentVoucherHDREntity savedHeader = paymentVoucherRepository.save(entity);

        /*savedHeader.setDocRef("BPV-" + savedHeader.getTransactionPoid());*/
        savedHeader = paymentVoucherRepository.save(savedHeader);

        // Before save validation
        validateBeforeSaveInNewTransaction(savedHeader);

        if (req.getRefType().equalsIgnoreCase("GENERAL") ||
                req.getRefType().equalsIgnoreCase("CUSTOM")) {
            saveGLDetails(req.getGlDetails(), savedHeader.getTransactionPoid(), documentId);
        }

        if (req.getRefType().equalsIgnoreCase("FDA JOBS") ||
        req.getRefType().equalsIgnoreCase("FF JOBS")){
            saveChargeDetails(req.getChargeDetailRequests(), savedHeader.getTransactionPoid());
        }

        if (req.getRefType().equalsIgnoreCase("MTA RFQ")) {
            saveItemDetails(req.getItemDetailRequests(), savedHeader.getTransactionPoid());
        }

        // Post-save updates
        updateJobCostsInNewTransaction(savedHeader, req.getRefType());

        // Flush to ensure all changes are persisted
        paymentVoucherRepository.flush();

        // Log the creation
        String key = savedHeader.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, documentId, key);

        return getVoucherById(savedHeader.getTransactionPoid(),documentId);
    }

    @Override
    @Transactional
    public BankPaymentVoucherResponse updateBankPaymentVoucher(Long transactionPoid, BankPaymentVoucherRequest req, String documentId) {

        GLPaymentVoucherHDREntity existing = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Bank Payment Voucher not found for ID: " + transactionPoid));

        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(existing.getTransactionPoid())
                .transactionDate(existing.getTransactionDate())
                .groupPoid(existing.getGroupPoid())
                .companyPoid(existing.getCompanyPoid())
                .docRef(existing.getDocRef())
                .payGlPoid(existing.getPayGlPoid())
                .payingTo(existing.getPayingTo())
                .payingType(existing.getPayingType())
                .divisionCode(existing.getDivisionCode())
                .bankPoid(existing.getBankPoid())
                .chqCardNo(existing.getChqCardNo())
                .chqDate(existing.getChqDate())
                .currencyCode(existing.getCurrencyCode())
                .currencyRate(existing.getCurrencyRate())
                .currencyAmount(existing.getCurrencyAmount())
                .localAmount(existing.getLocalAmount())
                .shortNarration(existing.getShortNarration())
                .longNarration(existing.getLongNarration())
                .chqPrinted(existing.getChqPrinted())
                .chqPrintedUserCode(existing.getChqPrintedUserCode())
                .chqPrintedDate(existing.getChqPrintedDate())
                .chequeIssuePhysical(existing.getChequeIssuePhysical())
                .releasedDate(existing.getReleasedDate())
                .createdBy(existing.getCreatedBy())
                .createdDate(existing.getCreatedDate())
                .lastModifiedBy(existing.getLastModifiedBy())
                .lastModifiedDate(existing.getLastModifiedDate())
                .deleted(existing.getDeleted())
                .pdcBatchPoid(existing.getPdcBatchPoid())
                .availableBalance(existing.getAvailableBalance())
                .bankBalance(existing.getBankBalance())
                .prePrinted(existing.getPrePrinted())
                .multiCompany(existing.getMultiCompany())
                .released(existing.getReleased())
                .releasedByUserCode(existing.getReleasedByUserCode())
                .releasedPersonAddress(existing.getReleasedPersonAddress())
                .releasedPersonId(existing.getReleasedPersonId())
                .releasedSeqNo(existing.getReleasedSeqNo())
                .releasedToPerson(existing.getReleasedToPerson())
                .fdaRef(existing.getFdaRef())
                .ffRef(existing.getFfRef())
                .mtaRef(existing.getMtaRef())
                .poRef(existing.getPoRef())
                .refType(existing.getRefType())
                .salesQtnRef(existing.getSalesQtnRef())
                .chqSignType(existing.getChqSignType())
                .oldPvName(existing.getOldPvName())
                .payToOldCode(existing.getPayToOldCode())
                .accountPayee(existing.getAccountPayee())
                .reconciledDate(existing.getReconciledDate())
                .printWithoutBillwise(existing.getPrintWithoutBillwise())
                .remarks(existing.getRemarks())
                .hold(existing.getHold())
                .suppressValidation(existing.getSuppressValidation())
                .securityCheque(existing.getSecurityCheque())
                .build();

        String oldRefType = existing.getRefType();

        if ("FF JOBS".equalsIgnoreCase(req.getRefType()) && req.getFfRefId() != null) {
            validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), String.valueOf(req.getFfRefId()), "FF JOBS");
        }
        if ("FDA JOBS".equalsIgnoreCase(req.getRefType()) && req.getFdaRefId() != null) {
            validateJobInNewTransaction(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), String.valueOf(req.getFdaRefId()), "FDA JOBS");
        }

        validateBeforeSaveInNewTransaction(existing);

        updateHeaderFromRequest(existing, req);
        GLPaymentVoucherHDREntity updatedHeader = paymentVoucherRepository.save(existing);

        // Update details based on Ref Type
        switch (req.getRefType().toUpperCase()) {
            case "GENERAL", "CUSTOM" -> updateGLDetails(req.getGlDetails(), transactionPoid, documentId);
            case "FF JOBS", "FDA JOBS" -> saveChargeDetails(req.getChargeDetailRequests(), transactionPoid);
            case "MTA RFQ" -> saveItemDetails(req.getItemDetailRequests(), transactionPoid);
            default -> throw new ValidationException("Invalid Ref Type: " + req.getRefType());
        }

        // Post-update job costs
        updateJobCostsInNewTransaction(updatedHeader, req.getRefType());

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
        Long jobPoid = getOldJobPoid(existing, refType);
        if (jobPoid != null) {
            releaseOldJobValuesInNewTransaction(transactionPoid, jobPoid, refType);
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
        response.setBankDet(lovService.getDetailsByPoidAndLovName(header.getBankPoid(), "BANK_MASTER"));
        response.setFdaDet(lovService.getDetailsByPoidAndLovName(header.getFdaRef(), "FDA_JOB"));

        // =====================================================================
        // CASE HANDLING
        // Only one of the three lists will contain data
        // Others must be empty arrays, not null
        // =====================================================================

        boolean hasGL      = glList != null && !glList.isEmpty();
        boolean hasCharge  = chargeList != null && !chargeList.isEmpty();
        boolean hasItem    = itemList != null && !itemList.isEmpty();

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
        entity.setBankPoid(req.getBankPoid());
        entity.setPayGlPoid(req.getPayGlPoid());
        entity.setPayingTo(req.getPayingTo());
        entity.setRemarks(req.getRemarks());
        entity.setRefType(req.getRefType());
        entity.setFdaRef(req.getFdaRefId());
        entity.setFfRef(req.getFfRefId() != null ? String.valueOf(req.getFfRefId()) : null);
        entity.setMtaRef(req.getMtaRfqId());

        if (req.getChequeDate() != null && !req.getChequeDate().isEmpty()) {
            entity.setChqDate(LocalDate.parse(req.getChequeDate()));
        }

        entity.setChqCardNo(req.getChequeNo());
        entity.setLongNarration(req.getLongNarration());
        entity.setSuppressValidation(req.getSuppressValidation());
        entity.setAccountPayee(req.getAccountPayee());
        entity.setMultiCompany(req.getMultiple());
        entity.setSecurityCheque(req.getSecurityCheque());

        if (Boolean.TRUE.equals(req.getReleased())) {
            entity.setReleasedToPerson(req.getReleasedToPerson());
            entity.setReleasedPersonAddress(req.getContact());
            entity.setReleasedByUserCode(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
            entity.setReleasedDate(LocalDate.now());
        }

        entity.setLastModifiedBy(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void validateRefType(BankPaymentVoucherRequest req) {
        switch (req.getRefType().toUpperCase()) {
            case "GENERAL" -> {
                if (req.getPayGlPoid() == null)
                    throw new ValidationException("Pay GL is mandatory for Ref Type = GENERAL.");
            }
            case "FF JOBS" -> {
                if (req.getFfRefId() == null)
                    throw new ValidationException("FF Ref Id is mandatory for Ref Type = FF JOBS.");
            }
            case "FDA JOBS" -> {
                if (req.getFdaRefId() == null)
                    throw new ValidationException("FDA Ref Id is mandatory for Ref Type = FDA JOBS.");
            }
            case "MTA RFQ" -> {
                if (req.getMtaRfqId() == null)
                    throw new ValidationException("MTA RFQ Id is mandatory for Ref Type = MTA RFQ.");
            }
            case "CUSTOM" -> {
                // optional
            }
            default -> throw new ValidationException("Invalid Ref Type: " + req.getRefType());
        }
    }

    private GLPaymentVoucherHDREntity mapHeaderFromRequest(BankPaymentVoucherRequest req) {
        GLPaymentVoucherHDREntity entity = new GLPaymentVoucherHDREntity();

        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setBankPoid(req.getBankPoid());
        entity.setPayGlPoid(req.getPayGlPoid());
        entity.setPayingTo(req.getPayingTo());
        entity.setRemarks(req.getRemarks());
        entity.setRefType(req.getRefType());
        entity.setFdaRef(req.getFdaRefId());
        entity.setFfRef(req.getFfRefId() != null ? String.valueOf(req.getFfRefId()) : null);
        entity.setMtaRef(req.getMtaRfqId());

        if (req.getChequeDate() != null && !req.getChequeDate().isEmpty()) {
            entity.setChqDate(LocalDate.parse(req.getChequeDate()));
        }

        entity.setChqCardNo(req.getChequeNo());
        entity.setLongNarration(req.getLongNarration());
        entity.setSuppressValidation(req.getSuppressValidation());
        entity.setAccountPayee(req.getAccountPayee());
        entity.setMultiCompany(req.getMultiple());
        entity.setSecurityCheque(req.getSecurityCheque());
        entity.setPrePrinted("N");
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

        entity.setTransactionDate(LocalDate.now());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setCreatedBy(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
        entity.setChqPrintedUserCode(req.getChqPrintedUserCode());
        entity.setChqPrintedDate(req.getChqPrintedDate());
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
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(rowId++);
            entity.setType(detail.getType());
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
            entity.setCreatedBy(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
            entity.setCreatedDate(LocalDateTime.now());
            entities.add(entity);
        }

        paymentVoucherDetailsRepository.saveAll(entities);

        // Save billwise breakup for GL details
        saveBillwiseBreakup(glDetails, transactionPoid, documentId);
    }

    private void saveChargeDetails(List<BankPaymentChargeDetailRequest> chargeDetails, Long transactionPoid) {
        // Ensure the list is not null
        if (chargeDetails == null) chargeDetails = List.of();

        // Fetch existing charge entries for this transaction
        List<GlBankPaymentChargeDtlEntity> existing = chargeDtlRepository
                .findByTransactionPoid(transactionPoid);

        // Collect IDs from the incoming request
        List<Long> reqIds = chargeDetails.stream()
                .map(BankPaymentChargeDetailRequest::getDetRowId)
                .filter(Objects::nonNull)
                .toList();

        // Delete any existing charges that are no longer present in the request
        List<GlBankPaymentChargeDtlEntity> toDelete = existing.stream()
                .filter(e -> !reqIds.contains(e.getDetRowId()))
                .toList();
        if (!toDelete.isEmpty()) chargeDtlRepository.deleteAll(toDelete);

        // Map request objects to entities (save all, no filter)
        List<GlBankPaymentChargeDtlEntity> toSave = chargeDetails.stream()
                .map(detail -> {
                    // Check if entity already exists, otherwise create new
                    GlBankPaymentChargeDtlEntity e = existing.stream()
                            .filter(x -> x.getDetRowId() != null &&
                                    x.getDetRowId().equals(detail.getDetRowId()))
                            .findFirst()
                            .orElse(new GlBankPaymentChargeDtlEntity());

                    // Map fields from request to entity
                    e.setTransactionPoid(transactionPoid);
                    e.setDetRowId(detail.getDetRowId());
                    e.setChargePoid(detail.getChargePoid());
                    e.setChargeAmount(detail.getChargeAmount());
                    e.setDescription(detail.getDescription());
                    e.setRemarks(detail.getRemarks());
                    e.setRefDocId(detail.getRefDocId());
                    e.setRefDocPoid(detail.getRefDocPoid());
                    e.setFdaDetRowId(detail.getFdaDetRowId());
                    e.setPdaAmount(detail.getPdaAmount());
                    e.setFfAmount(detail.getFfAmount());
                    e.setLastModifiedBy(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
                    e.setLastModifiedDate(LocalDateTime.now());

                    // Set creation metadata if new
                    if (e.getCreatedBy() == null) {
                        e.setCreatedBy(UserContext.getCurrentUser().getUserName());
                        e.setCreatedDate(LocalDateTime.now());
                    }

                    return e;
                }).toList();

        // Save all entities to the repository
        chargeDtlRepository.saveAll(toSave);
    }

    // ============================================================
    // SAVE ITEM DETAILS (Composite key aware)
    // ============================================================

    private void saveItemDetails(List<BankPaymentItemDetailRequest> itemDetails, Long transactionPoid) {
        if (itemDetails == null) itemDetails = List.of();

        List<GlBankPaymentItemDtlEntity> existing = itemRepository
                .findByTransactionPoid(transactionPoid);

        List<Long> reqIds = itemDetails.stream()
                .map(BankPaymentItemDetailRequest::getDetRowId)
                .filter(Objects::nonNull)
                .toList();

        List<GlBankPaymentItemDtlEntity> toDelete = existing.stream()
                .filter(e -> !reqIds.contains(e.getDetRowId()))
                .toList();
        if (!toDelete.isEmpty()) itemRepository.deleteAll(toDelete);

        List<GlBankPaymentItemDtlEntity> toSave = itemDetails.stream()
                .map(detail -> {
                    GlBankPaymentItemDtlEntity e = existing.stream()
                            .filter(x -> x.getDetRowId().equals(detail.getDetRowId()))
                            .findFirst()
                            .orElse(new GlBankPaymentItemDtlEntity());

                    e.setTransactionPoid(transactionPoid);
                    e.setDetRowId(detail.getDetRowId());
                    e.setStockPoid(detail.getStockPoid());
                    e.setStockUnitPoid(detail.getStockUnitPoid());
                    e.setPoQty(detail.getPoQty());
                    e.setDnQty(detail.getDnQty());
                    e.setQtyReceived(detail.getQtyReceived());
                    e.setPrice(detail.getPrice());
                    e.setDiscount(detail.getDiscount());
                    e.setTotal(detail.getTotal());
                    e.setRemarks(detail.getRemarks());
                    e.setRefDocId(detail.getRefDocId());
                    e.setRefDocPoid(detail.getRefDocPoid());
                    e.setRefDetRowId(detail.getRefDetRowId());
                    e.setLastModifiedBy(Objects.requireNonNull(UserContext.getCurrentUser()).getUserName());
                    e.setLastModifiedDate(LocalDateTime.now());

                    if (e.getCreatedBy() == null) {
                        e.setCreatedBy(UserContext.getCurrentUser().getUserName());
                        e.setCreatedDate(LocalDateTime.now());
                    }

                    return e;
                }).toList();

        itemRepository.saveAll(toSave);
    }

    // ============================================================
    // NEW METHODS - STORED PROCEDURE OPERATIONS
    // ============================================================

    @Override
    public Map<String, BigDecimal> getBankBalance(String docId, Long docKeyPoid, Date docDate, Long bankPoid) {
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
                UserContext.getUserPoid(),
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
                getCurrentUser(),
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
        GLPaymentVoucherHDREntity oldEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(header.getTransactionPoid())
                .released(header.getReleased())
                .releasedToPerson(header.getReleasedToPerson())
                .releasedPersonAddress(header.getReleasedPersonAddress())
                .releasedByUserCode(header.getReleasedByUserCode())
                .releasedDate(header.getReleasedDate())
                .build();
        
        spRepository.releaseCheque(
                header.getGroupPoid(),
                getCurrentUser(),
                header.getCompanyPoid(),
                transactionPoid,
                releasedTo,
                contact
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
    public void unReleaseCheque(Long transactionPoid) {
        GLPaymentVoucherHDREntity header = paymentVoucherRepository.findById(transactionPoid)
                .orElseThrow(() -> new ValidationException("Voucher not found"));
        
        // Create a copy of the existing entity for logging
        GLPaymentVoucherHDREntity oldEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(header.getTransactionPoid())
                .released(header.getReleased())
                .releasedToPerson(header.getReleasedToPerson())
                .releasedPersonAddress(header.getReleasedPersonAddress())
                .releasedByUserCode(header.getReleasedByUserCode())
                .releasedDate(header.getReleasedDate())
                .build();
        
        spRepository.unReleaseCheque(
                UserContext.getGroupPoid(),
                UserContext.getUserId(),
                UserContext.getCompanyPoid(),
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
    public String revertReconciliation(Long transactionPoid, String documentId) {
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
                "Y"
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
                    getCurrentUser()
            );
        } catch (Exception e) {
            log.warn("Before save validation failed: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void validateJobInNewTransaction(Long groupPoid, Long companyPoid, String refPoid, String refType) {
        try {
            String result = spRepository.validateJob(groupPoid, null, companyPoid, null, refType, refPoid);
            if (result != null && !result.equals("SUCCESS")) {
                throw new ValidationException(result);
            }
        } catch (Exception e) {
            log.error("Job validation failed: {}", e.getMessage());
            throw new ValidationException("Job validation failed: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void validateVoucherStatusInNewTransaction(GLPaymentVoucherHDREntity header) {
        try {
            Long refPoid = getOldJobPoid(header, header.getRefType());
            String result = spRepository.validateVoucherStatus(
                    header.getGroupPoid(),
                    null,
                    header.getCompanyPoid(),
                    header.getDocRef(),
                    header.getRefType(),
                    refPoid != null ? String.valueOf(refPoid) : null
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
    protected void releaseOldJobValuesInNewTransaction(Long transactionPoid, Long oldJobPoid, String refType) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateJobCostsInNewTransaction(GLPaymentVoucherHDREntity header, String refType) {
        try {
            switch (refType.toUpperCase()) {
                case "FDA JOBS" -> spRepository.updateFdaCost(
                        header.getGroupPoid(),
                        header.getCompanyPoid(),
                        null,
                        String.valueOf(header.getFdaRef()),
                        header.getTransactionPoid()
                );
                case "FF JOBS" -> spRepository.updateFfCost(
                        header.getGroupPoid(),
                        header.getCompanyPoid(),
                        null,
                        header.getFfRef(),
                        header.getTransactionPoid()
                );
                case "MTA RFQ" -> spRepository.updateMtaCost(
                        header.getGroupPoid(),
                        header.getCompanyPoid(),
                        null,
                        header.getTransactionPoid(),
                        header.getMtaRef()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to update job costs for {}: {}", refType, e.getMessage());
        }
    }

    private Long getOldJobPoid(GLPaymentVoucherHDREntity entity, String refType) {
        return switch (refType != null ? refType.toUpperCase() : "") {
            case "FDA JOBS" -> entity.getFdaRef();
            case "FF JOBS" -> entity.getFfRef() != null ? Long.parseLong(entity.getFfRef()) : null;
            case "MTA RFQ" -> entity.getMtaRef() != null ? Long.parseLong(entity.getMtaRef()) : null;
            default -> null;
        };
    }

    private String getCurrentUser() {
        return Objects.requireNonNull(UserContext.getCurrentUser()).getUserName();
    }

    // ============================================================
    // UPDATE GL DETAILS WITH BILLWISE BREAKUP
    // ============================================================

    private void updateGLDetails(List<BankPaymentGLDetailRequest> glDetails, Long transactionPoid, String documentId) {
        saveGLDetails(glDetails, transactionPoid, documentId);
        updateBillwiseBreakup(glDetails, transactionPoid, documentId);
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
                    dto.setDrAmt(popup.getAmount());
                    dto.setCrAmt(popup.getAmount());
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
                    dto.setCostDetRowId(glDetail.getGlPoid()); // mapping GL → cost center
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
                    dto.setDocId(documentId);
                    dto.setTransactionPoid(transactionPoid); // SAME HDR POID
                    dto.setLoginUserPoid(userPoid);
                    dto.setGlPoid(glDetail.getGlPoid());
                    dto.setMainDetRowId(glDetail.getDetRowId());
                    dto.setBillDetRowId(popup.getBillDetRowId());   // GL Mapping
                    dto.setBillRefType(popup.getBillRefType());
                    dto.setBillRef(popup.getBillRef());
                    dto.setBillDueDate(popup.getBillDueDate());
                    dto.setDrAmt(popup.getAmount());
                    dto.setCrAmt(popup.getAmount());
                    dto.setBillRemarks(popup.getBillRemarks());

                    billwiseList.add(dto);
                }
            }
        }

        if (!billwiseList.isEmpty()) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseList,userPoid );
        }

        // === COST CENTER BREAKUP ===
        // -------------------------
        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();

        for (BankPaymentGLDetailRequest glDetail : glDetails) {
            if (glDetail.getCostCenterBreakup() != null && !glDetail.getCostCenterBreakup().isEmpty()) {
                for (CostCenterBreakupPopupRequestDto popup : glDetail.getCostCenterBreakup()) {
                    CostCenterBreakupRequestDto cc = CostCenterBreakupRequestDto.builder()
                            .costDetRowId(glDetail.getGlPoid())
                            .costGroup(UserContext.getGroupPoid().toString())
                            .costPoid(popup.getCostPoid())
                            .amount(popup.getAmount())
                            .docId(documentId)
                            .transactionPoid(transactionPoid)
                            .loginUserPoid(UserContext.getUserPoid())
                            .build();

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

        return list.stream().map(src ->
                BillwiseBreakupPopupRequestDto.builder()
                        .billRefType(src.getBillRefType())
                        .billRef(src.getBillRef())
                        .billDueDate(src.getBillDueDate())
                        .type(src.getDrAmt().toString())
                        .amount(src.getCrAmt())
                        .billRemarks(src.getBillRemarks())
                        .build()
        ).collect(Collectors.toList());
    }

    private List<CostCenterBreakupPopupRequestDto> mapToCostCenterPopupDto(List<CostCenterBreakupResponseDto> list) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .map(cc -> CostCenterBreakupPopupRequestDto.builder()
                        .costDetRowId(cc.getCostDetRowId())
                        .costGroup(cc.getCostGroup())
                        .costPoid(cc.getCostPoid())
                        .amount(BigDecimal.valueOf(cc.getAmount())) // converting Long → BigDecimal
                        .build())
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

}

