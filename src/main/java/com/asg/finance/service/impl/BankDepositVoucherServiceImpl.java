package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.utility.DateUtil;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.BankDepositVoucherRequestDto;
import com.asg.finance.dto.BankDepositVoucherResponseDto;
import com.asg.finance.entity.GlBankDepositVoucherDtl;
import com.asg.finance.entity.GlBankDepositVoucherHdr;
import com.asg.finance.repository.GlBankDepositVoucherDtlRepository;
import com.asg.finance.repository.GlBankDepositVoucherHdrRepository;
import com.asg.finance.service.BankDepositVoucherService;
import com.asg.finance.service.GlPostingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import static com.asg.finance.utility.Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankDepositVoucherServiceImpl implements BankDepositVoucherService {

    private final GlBankDepositVoucherHdrRepository hdrRepository;
    private final GlBankDepositVoucherDtlRepository dtlRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LovDataService lovService;
    private final LoggingService loggingService;
    private final PlatformTransactionManager transactionManager;
    private final ApprovalService approvalService;
    private final GlPostingService glPostingService;
    private final GlobalParameterService globalParameterService;

    private static final String DOC_ID = "300-111";
    private static final String SCREEN_NAME = "Bank Deposit Voucher";
    private static final String TRANSACTION_POID = "transactionPoid";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public BankDepositVoucherResponseDto createBankDepositVoucher(BankDepositVoucherRequestDto request) {
        validateAndFilterDetails(request);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Long transactionPoid = tx.execute(status -> createHeaderAndDetails(request));
        callUpdatePaymentProcedure(transactionPoid, request.getType());

        BankDepositVoucherResponseDto response = getBankDepositVoucherById(transactionPoid);
        handlePostSaveWorkflow(transactionPoid, response.getDocRef(), response.getTransactionDate());

        return response;
    }

    private Long createHeaderAndDetails(BankDepositVoucherRequestDto request) {
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid()
                : request.getCompanyPoid();
        Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : request.getGroupPoid();
        hdrRepository.callBeforeSaveValidation(companyPoid, request.getBankPoid());

        GlBankDepositVoucherHdr hdr = GlBankDepositVoucherHdr.builder()
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate()
                        : DateUtil.getCurrentDateInUserTimeZone())
                .groupPoid(groupPoid)
                .companyPoid(companyPoid)
                .bankPoid(request.getBankPoid())
                .postingNarration(request.getPostingNarration())
                .remarks(request.getRemarks())
                .grandTotal(calculateGrandTotal(request.getDetails()))
                .refType(request.getType())
                .bankFilter(request.getBankFilter())
                .groupPosting(request.getGroupPosting() != null && request.getGroupPosting() ? "Y" : "N")
                .deleted("N")
                .build();

        GlBankDepositVoucherHdr savedHdr = hdrRepository.save(hdr);
        entityManager.flush();
        entityManager.refresh(hdr);

        // Log the creation BEFORE child record processing
        String docId = UserContext.getDocumentId();
        String key = savedHdr.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(docId, key, String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedHdr.getDocRef()));

        if (request.getDetails() != null) {
            List<GlBankDepositVoucherDtl> details = new ArrayList<>();
            long rowId = 1;
            for (BankDepositVoucherDtlDto dto : request.getDetails()) {
                if (ACTION_ISDELETED.equalsIgnoreCase(dto.getActionType())) {
                    continue;
                }
                GlBankDepositVoucherDtl detail = convertToDetailEntity(dto, savedHdr.getTransactionPoid());
                detail.setDetRowId(rowId++);
                details.add(detail);
            }
            dtlRepository.saveAll(details);

            details.forEach(detail -> {
                String logDetail = String.format("Row Created on Bank Deposit Voucher Detail with detRowId: %s",
                        detail.getDetRowId());
                loggingService.createLogSummaryEntry(docId, key, logDetail);
            });
        }
        hdrRepository.flush();
        return savedHdr.getTransactionPoid();
    }

    private void callUpdatePaymentProcedure(Long transactionPoid, String paymentType) {
        callMarkPaymentsCompleted(transactionPoid, paymentType);
    }

    private void callMarkPaymentsCompleted(Long transactionPoid, String paymentType) {
        String normalizedPaymentType = normalizePaymentType(paymentType);
        hdrRepository.markPaymentsCompleted(transactionPoid, normalizedPaymentType);
    }

    @Override
    @Transactional
    public BankDepositVoucherResponseDto updateBankDepositVoucher(Long transactionPoid,
                                                                  BankDepositVoucherRequestDto request) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> updateHeaderAndDetails(transactionPoid, request));
        callUpdatePaymentProcedure(transactionPoid, request.getType());
        BankDepositVoucherResponseDto response = getBankDepositVoucherById(transactionPoid);
        handlePostSaveWorkflow(transactionPoid, response.getDocRef(), response.getTransactionDate());

        return response;
    }

    private void updateHeaderAndDetails(Long transactionPoid, BankDepositVoucherRequestDto request) {
        GlBankDepositVoucherHdr hdr = getGlBankDepositVoucherHdr(transactionPoid);
        GlBankDepositVoucherHdr oldEntity = new GlBankDepositVoucherHdr();
        BeanUtils.copyProperties(hdr, oldEntity);
        validateAndFilterDetails(request);
        validateUpdateState(transactionPoid, request);
        updateHeaderFromRequest(hdr, request);
        hdrRepository.save(hdr);

        if (request.getDetails() != null) {
            processTransactionDetails(transactionPoid, request.getDetails());
        }

        loggingService.logChanges(oldEntity, hdr, GlBankDepositVoucherHdr.class,
                UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED,
                TRANSACTION_POID.toUpperCase());
    }

    private void validateUpdateState(Long transactionPoid, BankDepositVoucherRequestDto request) {
        GlBankDepositVoucherHdr hdr = getGlBankDepositVoucherHdr(transactionPoid);
        hdrRepository.callChequeStatusValidation(hdr.getDocRef(), transactionPoid);

        dtlRepository.findByTransactionPoid(transactionPoid).forEach(
                detail -> hdrRepository.callChequeStatusValidation(detail.getRefDocRef(), detail.getRefDocPoid()));
        hdrRepository.callBeforeSaveValidation(UserContext.getCompanyPoid(), request.getBankPoid());
    }

    private void updateHeaderFromRequest(GlBankDepositVoucherHdr hdr, BankDepositVoucherRequestDto request) {
        hdr.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate()
                : DateUtil.getCurrentDateInUserTimeZone());
        hdr.setBankPoid(request.getBankPoid());
        hdr.setPostingNarration(request.getPostingNarration());
        hdr.setRemarks(request.getRemarks());
        hdr.setGrandTotal(calculateGrandTotal(request.getDetails()));
        hdr.setRefType(request.getType());
        hdr.setBankFilter(request.getBankFilter());
        hdr.setGroupPosting(request.getGroupPosting() != null && request.getGroupPosting() ? "Y" : "N");
    }

    private void processTransactionDetails(Long transactionPoid, List<BankDepositVoucherDtlDto> details) {
        List<GlBankDepositVoucherDtl> toSave = new ArrayList<>();
        List<GlBankDepositVoucherDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlBankDepositVoucherDtl>> logRequests = new ArrayList<>();
        List<GlBankDepositVoucherDtl> existingDetailsList = dtlRepository.findByTransactionPoid(transactionPoid);
        Map<Long, GlBankDepositVoucherDtl> existingMap = existingDetailsList.stream()
                .collect(Collectors.toMap(GlBankDepositVoucherDtl::getDetRowId, d -> d));

        AtomicLong nextId = new AtomicLong(dtlRepository.getMaxDetRowIdByTransactionPoid(transactionPoid));

        for (BankDepositVoucherDtlDto dto : details) {
            applyDetailChange(dto, transactionPoid, nextId, existingMap, toSave, toUpdate, toDelete, logRequests);
        }

        saveNewDetails(transactionPoid, toSave);
        updateDetails(toUpdate, logRequests);
        deleteDetails(toDelete, existingDetailsList);
    }

    private void applyDetailChange(BankDepositVoucherDtlDto dto, Long transactionPoid, AtomicLong nextId,
                                   Map<Long, GlBankDepositVoucherDtl> existingMap, List<GlBankDepositVoucherDtl> toSave,
                                   List<GlBankDepositVoucherDtl> toUpdate, List<Long> toDelete,
                                   List<LogRequestDto<GlBankDepositVoucherDtl>> logRequests) {
        String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : ACTION_ISCREATED;
        switch (action) {
            case ACTION_ISCREATED:
                Long detId = nextId.incrementAndGet();
                GlBankDepositVoucherDtl newDetail = convertToDetailEntity(dto, transactionPoid);
                newDetail.setDetRowId(detId);
                toSave.add(newDetail);
                break;
            case ACTION_ISUPDATED:
                handleUpdateBatchAction(dto, transactionPoid, existingMap, toUpdate, logRequests);
                break;
            case ACTION_ISDELETED:
                toDelete.add(dto.getDetRowId());
                loggingService.logDelete(dto, UserContext.getDocumentId(), transactionPoid.toString());
                break;
            default:
                break;
        }
    }

    private void handleUpdateBatchAction(BankDepositVoucherDtlDto dto, Long transactionPoid,
                                         Map<Long, GlBankDepositVoucherDtl> existingMap,
                                         List<GlBankDepositVoucherDtl> toUpdate,
                                         List<LogRequestDto<GlBankDepositVoucherDtl>> logRequests) {
        GlBankDepositVoucherDtl existing = existingMap.get(dto.getDetRowId());
        if (existing == null) {
            throw new ResourceNotFoundException("Detail", "detRowId", dto.getDetRowId());
        }
        GlBankDepositVoucherDtl oldDetail = new GlBankDepositVoucherDtl();
        BeanUtils.copyProperties(existing, oldDetail);
        updateDetailEntity(existing, dto);
        toUpdate.add(existing);
        String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid,
                dto.getDetRowId());
        logRequests.add(new LogRequestDto<>(oldDetail, existing, GlBankDepositVoucherDtl.class,
                UserContext.getDocumentId(), transactionPoid.toString(), logDetail));
    }

    private void saveNewDetails(Long transactionPoid, List<GlBankDepositVoucherDtl> toSave) {
        if (!toSave.isEmpty()) {
            List<GlBankDepositVoucherDtl> saved = dtlRepository.saveAll(toSave);
            saved.forEach(detail -> {
                String logDetail = String.format("Row Created on Bank Deposit Voucher Detail with detRowId: %s",
                        detail.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(),
                        logDetail);
            });
        }
    }

    private void updateDetails(List<GlBankDepositVoucherDtl> toUpdate,
                               List<LogRequestDto<GlBankDepositVoucherDtl>> logRequests) {
        if (!toUpdate.isEmpty()) {
            dtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
    }

    private void deleteDetails(List<Long> toDelete, List<GlBankDepositVoucherDtl> existingDetailsList) {
        if (!toDelete.isEmpty()) {
            existingDetailsList.stream()
                    .filter(d -> toDelete.contains(d.getDetRowId()))
                    .forEach(dtlRepository::delete);
        }
    }

    private GlBankDepositVoucherHdr getGlBankDepositVoucherHdr(Long transactionPoid) {
        return hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SCREEN_NAME, TRANSACTION_POID, transactionPoid));
    }

    private String normalizePaymentType(String paymentType) {
        return paymentType == null ? null : paymentType.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public BankDepositVoucherResponseDto getBankDepositVoucherById(Long transactionPoid) {
        GlBankDepositVoucherHdr hdr = getGlBankDepositVoucherHdr(transactionPoid);

        List<GlBankDepositVoucherDtl> details = dtlRepository.findByTransactionPoidOrderByChqSeqNumAsc(transactionPoid);

        return BankDepositVoucherResponseDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .transactionDate(hdr.getTransactionDate())
                .docRef(hdr.getDocRef())
                .bankPoid(hdr.getBankPoid())
                .postingNarration(hdr.getPostingNarration())
                .remarks(hdr.getRemarks())
                .grandTotal(hdr.getGrandTotal())
                .groupPosting(hdr.getGroupPosting())
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .refType(hdr.getRefType())
                .bankFilter(hdr.getBankFilter())
                .details(details.stream().map(d -> convertToDetailDto(d, hdr.getCompanyPoid())).toList())
                .build();
    }

    @Override
    @Transactional
    public void softDeleteBankDepositVoucher(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlBankDepositVoucherHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SCREEN_NAME, TRANSACTION_POID, transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_BANK_DEPOSIT_VOUCHER_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                hdr.getTransactionDate());
    }

    @Override
    public Map<String, Object> listBankDepositVouchers(String documentId, FilterRequestDto filters, LocalDate startDate,
                                                       LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate,
                endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "DOC_REF", "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public List<BankDepositVoucherDtlDto> loadPendingPayments(Long bankPoid, String type, String bankFilter) {
        return hdrRepository.loadPendingPayments(bankPoid, type, bankFilter);
    }

    private BigDecimal calculateGrandTotal(List<BankDepositVoucherDtlDto> details) {
        if (details == null)
            return BigDecimal.ZERO;
        return details.stream()
                .filter(dto -> !ACTION_ISDELETED.equalsIgnoreCase(dto.getActionType()))
                .map(dto -> dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GlBankDepositVoucherDtl convertToDetailEntity(BankDepositVoucherDtlDto dto, Long transactionPoid) {
        return GlBankDepositVoucherDtl.builder()
                .transactionPoid(transactionPoid)
                .bankPoid(dto.getBankPoid())
                .pymtType(dto.getPymtType())
                .refDocPoid(dto.getRefDocPoid())
                .refDocRef(dto.getRefDocRef())
                .refDocId(dto.getRefDocId())
                .rcpDate(dto.getRcpDate())
                .chqAcName(dto.getChqAcName())
                .chqAcNo(dto.getChqAcNo())
                .chqCardNo(dto.getChqCardNo())
                .chqDate(dto.getChqDate())
                .amount(dto.getAmount())
                .remarks(dto.getRemarks())
                .selected(dto.getSelected() != null && dto.getSelected().length() == 1 ? dto.getSelected() : "Y")
                .chqSeqNum(Integer.valueOf(dto.getChqSeqNum()))
                .paymentMainPoid(dto.getPaymentMainPoid())
                .build();
    }

    private void updateDetailEntity(GlBankDepositVoucherDtl entity, BankDepositVoucherDtlDto dto) {
        entity.setBankPoid(dto.getBankPoid());
        entity.setPymtType(dto.getPymtType());
        entity.setRefDocPoid(dto.getRefDocPoid());
        entity.setRefDocRef(dto.getRefDocRef());
        entity.setRefDocId(dto.getRefDocId());
        entity.setRcpDate(dto.getRcpDate());
        entity.setChqAcName(dto.getChqAcName());
        entity.setChqAcNo(dto.getChqAcNo());
        entity.setChqCardNo(dto.getChqCardNo());
        entity.setChqDate(dto.getChqDate());
        entity.setAmount(dto.getAmount());
        entity.setRemarks(dto.getRemarks());
        entity.setSelected(dto.getSelected() != null && dto.getSelected().length() == 1 ? dto.getSelected() : "Y");
        entity.setChqSeqNum(Integer.valueOf(dto.getChqSeqNum()));
        entity.setPaymentMainPoid(dto.getPaymentMainPoid());
    }

    private BankDepositVoucherDtlDto convertToDetailDto(GlBankDepositVoucherDtl entity, Long companyPoid) {
        return BankDepositVoucherDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .bankDet(setBankDet(entity))
                .bankPoid(entity.getBankPoid())
                .pymtType(entity.getPymtType())
                .refDocPoid(entity.getRefDocPoid())
                .refDocRef(entity.getRefDocRef())
                .refDocId(entity.getRefDocId())
                .drilldownLinkInfo(hdrRepository.buildDrilldownLinkInfo(entity.getRefDocId(), entity.getRefDocPoid(), companyPoid))
                .rcpDate(entity.getRcpDate())
                .chqAcName(entity.getChqAcName())
                .chqAcNo(entity.getChqAcNo())
                .chqCardNo(entity.getChqCardNo())
                .chqDate(entity.getChqDate())
                .amount(entity.getAmount())
                .remarks(entity.getRemarks())
                .selected(entity.getSelected())
                .chqSeqNum(String.valueOf(entity.getChqSeqNum()))
                .paymentMainPoid(entity.getPaymentMainPoid())
                .build();
    }

    private LovGetListDto setBankDet(GlBankDepositVoucherDtl entity) {
        if (entity.getBankPoid() != null) {
            return lovService.getDetailsByPoidAndLovName(entity.getBankPoid(),"CUSTOMER_BANK_MASTER");
        }
        return  null;
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-109");
        params.put("SUB_CHEQUE", printService.load("Finance/GL/BankDepositVoucherPymt_subreport1.jrxml"));
        params.put("SUB_CASH", printService.load("Finance/GL/BankDepositVoucherGL_subreport1.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/BankDepositVoucherReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private void handlePostSaveWorkflow(Long transactionPoid, String docRef, LocalDate transactionDate) {
        try {
            Long groupPoid = UserContext.getGroupPoid();
            String approvalSubmission = globalParameterService.getParameterValue(
                    "BANK_DEPOSIT_APPROVAL_SUBMISSION", "GROUP", groupPoid != null ? groupPoid.toString() : "1",
                    "FALSE");

            String docId = UserContext.getDocumentId() != null ? UserContext.getDocumentId() : DOC_ID;
            if ("TRUE".equalsIgnoreCase(approvalSubmission)) {
                String approvalStatus = approvalService.getApprovalStatus(docId, transactionPoid);
                log.info("Approval check for BDV {}: status={}, parameter={}", docRef, approvalStatus,
                        approvalSubmission);

                if ("APPROVAL_NOT_APPLICABLE".equalsIgnoreCase(approvalStatus)) {
                    glPostingService.performGlPosting(docId, transactionPoid, docRef);
                } else if (approvalStatus == null || approvalStatus.isEmpty() ||
                        "NOT_SUBMITTED".equalsIgnoreCase(approvalStatus) ||
                        "RETURN_FOR_CORRECTION".equalsIgnoreCase(approvalStatus)) {
                    hdrRepository.callApprovalProcedure(UserContext.getCompanyPoid(), UserContext.getUserPoid(),
                            transactionPoid, docRef, transactionDate);
                }
            } else {
                glPostingService.performGlPosting(docId, transactionPoid, docRef);
            }
        } catch (Exception e) {
            log.error("Post-save workflow failed for BDV {}, but data is saved", docRef, e);
        }
    }

    private void validateAndFilterDetails(BankDepositVoucherRequestDto request) {

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException("No Details in this Transaction");
        }

        request.getDetails().removeIf(dto -> {

            boolean isUnselected = "N".equalsIgnoreCase(dto.getSelected());
            String action = dto.getActionType() != null
                    ? dto.getActionType().toUpperCase()
                    : ACTION_ISCREATED;

            if (isUnselected) {
                if (ACTION_ISCREATED.equals(action)) {
                    return true;
                } else {
                    dto.setActionType(ACTION_ISDELETED);
                    return false;
                }
            }

            return false;
        });

        long activeCount = request.getDetails().stream()
                .filter(dto -> !ACTION_ISDELETED.equalsIgnoreCase(dto.getActionType()))
                .count();

        if (activeCount == 0) {
            throw new IllegalArgumentException("No Details in this Transaction");
        }
    }
}