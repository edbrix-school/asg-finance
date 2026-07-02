package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.GlobalTermsResponseDto;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.ApprovalService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.client.GlobalTermsServiceClient;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.repository.*;
import com.asg.finance.entity.key.GlBankDebitDtlGlId;
import com.asg.finance.entity.key.GlBankDebitChargeDtlId;
import com.asg.finance.entity.key.GlBankDebitItemDtlId;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BankDebitVoucherService;
import com.asg.finance.service.GlPostingService;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.TelexFileGenerateService;
import com.asg.finance.validator.BankDebitVoucherValidator;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;


@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class BankDebitVoucherServiceImpl implements BankDebitVoucherService {

    private final GlBankDebitHdrRepository headerRepository;
    private final GlBankDebitDtlGlRepository paymentGlRepository;
    private final GlBankDebitChargeDtlRepository chargeDetailRepository;
    private final GlBankDebitItemDtlRepository itemDetailRepository;
    private final GlBankRepository glBankRepository;
    private final TaxMasterRepository taxMasterRepository;
    private final GLMasterRepository glMasterRepository;
    private final ShipChargeRepository shipChargeRepository;
    private final BankPurposeCodeMasterRepository bankPurposeCodeMasterRepository;
    private final DocumentSearchService documentService;
    private final BankDebitVoucherCustomRepository bankDebitVoucherCustomRepository;
    private final LovDataService lovService;
    private final BankDebitVoucherValidator validator;
    private final BillwiseBreakupService billwiseBreakupService;
    private final CostCenterBreakupService costCenterBreakupService;
    private final BankPaymentVoucherSpRepository bankPaymentVoucherSpRepository;
    private final EntityManager entityManager;
    private final GlobalTermsServiceClient globalTermsServiceClient;
    private final GlobalTermsCustomChangesRepository globalTermsCustomChangesRepository;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final GlChequeCashConvertRepository glChequeCashConvertRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ApprovalService approvalService;
    private final GlPostingService glPostingService;
    private final BankDebitVoucherProcedureRepository bankDebitVoucherProcedureRepository;
    private final ApplicationContext applicationContext;
    private final GlobalParameterService globalParameterService;


    @Override
    @PerformGlPosting
    @Transactional
    public BankDebitVoucherResponse createBankDebitVoucher(BankDebitVoucherRequest request, String documentId) {
        // Step 1: Get self-reference to enable proxy interception for @Transactional
        BankDebitVoucherServiceImpl self = applicationContext.getBean(BankDebitVoucherServiceImpl.class);

        // Step 2: Save receipt data in a new transaction that commits immediately
        BankDebitVoucherResponse header = self.saveBankDebitVoucher(request, documentId);

        log.info("Receipt transaction committed. Data is now visible in database.");

        // Step 3: Call GL posting procedure OUTSIDE any transaction
        // The procedure can now see the committed data
        entityManager.flush();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    completePostSaveUpdates(documentId, header);
                }
            });
        } else {
            completePostSaveUpdates(documentId, header);
        }
        
        return header;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BankDebitVoucherResponse saveBankDebitVoucher(BankDebitVoucherRequest request, String documentId) {

        validator.validate(request, true);
        validateInputTaxVariance(request);

        // Step 2: Conditional field existence checks (only for fields that are required/validated)
        if (request.getBankPoid() == null || !glBankRepository.existsByBankPoid(request.getBankPoid())) {
            throw new ResourceNotFoundException("Bank", "bankPoid", request.getBankPoid());
        }

        if (lovService.getLovItemByCodeFast(request.getCurrencyCode(), "CURRENCY").getPoid() == null) {
            throw new ResourceNotFoundException("Currency", "currencyCode", request.getCurrencyCode());
        }

        // Conditional checks
        if (request.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(request.getTaxPoid())) {
            throw new ResourceNotFoundException("Tax", "taxPoid", request.getTaxPoid());
        }

        if (request.getPayGlPoid() != null && !glMasterRepository.existsByGlPoid(request.getPayGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "payGlPoid", request.getPayGlPoid());
        }

        if (request.getBeneficiaryBankPoid() != null && !glBankRepository.existsByBankPoid(request.getBeneficiaryBankPoid())) {
            throw new ResourceNotFoundException("Bank", "beneficiaryBankPoid", request.getBeneficiaryBankPoid());
        }

        if (request.getBankPurposePoid() != null && !bankPurposeCodeMasterRepository.existsByBankPurposePoid(request.getBankPurposePoid())) {
            throw new ResourceNotFoundException("Bank Purpose Code", "bankPurposePoid", request.getBankPurposePoid());
        }

        if (headerRepository.existsByDocRefIgnoreCase(request.getDocRef())) {
            throw new ResourceAlreadyExistsException("Doc Ref", request.getDocRef());
        }

        runPreSaveProcedures(null, request.isSuppressBalanceCheck() ? "Y" : "N");

        GlBankDebitHdr header = new GlBankDebitHdr();
        mapRequestToEntity(request, header);
        // Mirror GL_BANK_DEBIT_HDR trigger rules at application level
        validateTriggerRulesForCreate(header);
        populateCreateAudit(header);

        GlBankDebitHdr savedHeader = headerRepository.save(header);
        entityManager.flush();
        entityManager.refresh(header);

        // Log the creation BEFORE child record processing
        String key = savedHeader.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), key, String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedHeader.getDocRef()));

        // Post-save job cost updates (mirrors legacy DocumentAfterSave)
        persistChildCollections(request, savedHeader.getTransactionPoid(), true, documentId);

        BankDebitVoucherResponse response = mapEntityToResponse(savedHeader);

        loadBreakupsIntoResponse(response, savedHeader.getTransactionPoid(), documentId, savedHeader.getGroupPoid(), savedHeader.getCompanyPoid());
        return response;
    }

    private void completePostSaveUpdates(String documentId, BankDebitVoucherResponse savedHeader) {
        publishJobCostUpdateEvent(savedHeader, null, null);
    }

    private void completePostSaveUpdatesForUpdate(String documentId, BankDebitVoucherResponse savedHeader, String oldRefType, String oldRef) {
        // Release old job cost association when the reference has changed (mirrors legacy OldRef handling)
        String newRef = null;
        if ("FDA JOBS".equalsIgnoreCase(savedHeader.getRefType())) {
            newRef = savedHeader.getFdaRef() != null ? String.valueOf(savedHeader.getFdaRef()) : null;
        } else if ("FF JOBS".equalsIgnoreCase(savedHeader.getRefType())) {
            newRef = savedHeader.getFfRef();
        } else if ("MTA RFQ".equalsIgnoreCase(savedHeader.getRefType())) {
            newRef = savedHeader.getSalesQtnRef() != null ? String.valueOf(savedHeader.getSalesQtnRef()) : null;
        }

        boolean refChanged = oldRef != null && !oldRef.equals(newRef);
        if (refChanged) {
            bankPaymentVoucherSpRepository.releaseOldJobValues(
                    savedHeader.getGroupPoid(),
                    UserContext.getUserPoid(),
                    UserContext.getCompanyPoid(),
                    documentId,
                    String.valueOf(savedHeader.getTransactionPoid())
            );
        }

        publishJobCostUpdateEvent(savedHeader, oldRefType, oldRef);
    }

    @Override
    public BankDebitVoucherResponse getBankDebitVoucher(Long transactionPoid, String documentId) {
        GlBankDebitHdr header = headerRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));
        ReconcileResultDto reconDto = fetchReconDate("400-111", transactionPoid);

        BankDebitVoucherResponse response = mapEntityToResponse(header);

        // Load breakup data into response (reusable method)
        loadBreakupsIntoResponse(response, transactionPoid, documentId, header.getGroupPoid(), header.getCompanyPoid());

        GlobalTermsResponseDto termsResponse =
                globalTermsServiceClient.loadGlobalTermsList(
                        UserContext.getGroupPoid(),
                        UserContext.getCompanyPoid(),
                        documentId,
                        transactionPoid,
                        null
                );

        if (termsResponse != null && termsResponse.getTermsList() != null) {
            List<TermsAndConditionDto> termsDtoList = termsResponse.getTermsList().stream()
                    .map(this::mapTermsDto)
                    .toList();

            response.setTermsAndConditionDtoList(termsDtoList);
        }

        response.setReconcileInfo(reconDto);

        // Set formatted reconciliation date display (mirrors legacy reconciledDate getter)
        if (reconDto != null && reconDto.getReconcileDate() != null) {
            response.setReconciledDateDisplay("Clearing Date - " + reconDto.getReconcileDate());
        }

        // Set jobRefReadOnly flag: true when no job reference is linked (mirrors legacy DocumentBeforeEdit ReadOnlyJob logic)
        boolean jobRefReadOnly = (header.getFdaRef() == null)
                && StringUtils.isBlank(header.getFfRef())
                && (header.getSalesQtnRef() == null);
        response.setJobRefReadOnly(jobRefReadOnly);

        return response;
    }

    @Override
    @Transactional
    @PerformGlPosting
    public BankDebitVoucherResponse updateBankDebitVoucher(Long transactionPoid, BankDebitVoucherRequest request, String documentId) {
        BankDebitVoucherServiceImpl self = applicationContext.getBean(BankDebitVoucherServiceImpl.class);

        Object[] updateResult = self.persistBankDebitVoucherUpdate(transactionPoid, request, documentId);
        BankDebitVoucherResponse response = (BankDebitVoucherResponse) updateResult[0];
        String oldRefType = (String) updateResult[1];
        String oldRef = (String) updateResult[2];

        log.info("Update transaction committed. Data is now visible in database.");

        entityManager.flush();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    completePostSaveUpdatesForUpdate(documentId, response, oldRefType, oldRef);
                }
            });
        } else {
            completePostSaveUpdatesForUpdate(documentId, response, oldRefType, oldRef);
        }
        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object[] persistBankDebitVoucherUpdate(Long transactionPoid, BankDebitVoucherRequest request, String documentId) {

        GlBankDebitHdr header = headerRepository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));

        GlBankDebitHdr oldEntity = new GlBankDebitHdr();
        BeanUtils.copyProperties(header, oldEntity);

        validator.validate(request, false);
        validateInputTaxVariance(request);

        if (request.getBankPoid() == null || !glBankRepository.existsByBankPoid(request.getBankPoid())) {
            throw new ResourceNotFoundException("Bank", "bankPoid", request.getBankPoid());
        }

        if (lovService.getLovItemByCodeFast(request.getCurrencyCode(), "CURRENCY").getPoid() == null) {
            throw new ResourceNotFoundException("Currency", "currencyCode", request.getCurrencyCode());
        }

        if (request.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(request.getTaxPoid())) {
            throw new ResourceNotFoundException("Tax", "taxPoid", request.getTaxPoid());
        }

        if (request.getPayGlPoid() != null && !glMasterRepository.existsByGlPoid(request.getPayGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "payGlPoid", request.getPayGlPoid());
        }

        if (request.getBeneficiaryBankPoid() != null && !glBankRepository.existsByBankPoid(request.getBeneficiaryBankPoid())) {
            throw new ResourceNotFoundException("Bank", "beneficiaryBankPoid", request.getBeneficiaryBankPoid());
        }

        if (request.getBankPurposePoid() != null && !bankPurposeCodeMasterRepository.existsByBankPurposePoid(request.getBankPurposePoid())) {
            throw new ResourceNotFoundException("Bank Purpose Code", "bankPurposePoid", request.getBankPurposePoid());
        }

        if (request.getDocRef() != null && headerRepository.existsByDocRefIgnoreCaseAndTransactionPoidNot(request.getDocRef(), transactionPoid)) {
            throw new ResourceAlreadyExistsException("Doc Ref", request.getDocRef());
        }

        String approvalStatus = approvalService.getApprovalStatus(documentId, transactionPoid);
        if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
            throw new ValidationException("Cannot update a Bank Debit Voucher that has been approved");
        }

        runPreSaveProcedures(header, request.isSuppressBalanceCheck() ? "Y" : "N");

        // Capture old ref values before overwriting (mirrors legacy OldRefType/OldRef from PROC_GL_JOB_REL_OLD_VALUES)
        String oldRefType = header.getRefType();
        String oldRef = "FDA JOBS".equalsIgnoreCase(oldRefType) ? (header.getFdaRef() != null ? String.valueOf(header.getFdaRef()) : null)
                : "FF JOBS".equalsIgnoreCase(oldRefType) ? header.getFfRef()
                : "MTA RFQ".equalsIgnoreCase(oldRefType) ? (header.getSalesQtnRef() != null ? String.valueOf(header.getSalesQtnRef()) : null)
                : null;

        mapRequestToEntity(request, header);
        if (request.getTransactionDate() != null) {
            header.setTransactionDate(request.getTransactionDate());
        } else {
            header.setTransactionDate(oldEntity.getTransactionDate());
        }
        validateTriggerRulesForUpdate(oldEntity, header);

        validator.validateVoucherStatusInNewTransaction(header);

        // populateUpdateAudit(header);

        header = headerRepository.save(header);
        entityManager.flush();
        entityManager.refresh(header);

        // Post-update job cost updates (mirrors legacy DocumentAfterSave)
        persistChildCollections(request, header.getTransactionPoid(), false, documentId);

        String key = header.getTransactionPoid().toString();
        loggingService.logChanges(oldEntity, header, GlBankDebitHdr.class, documentId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        BankDebitVoucherResponse response = mapEntityToResponse(header);
        loadBreakupsIntoResponse(response, header.getTransactionPoid(), documentId, header.getGroupPoid(), header.getCompanyPoid());
        return new Object[]{response, oldRefType, oldRef};
    }

    @Override
    public void softDeleteBankDebitVoucher(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlBankDebitHdr header = headerRepository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));

        // Validate voucher can be deleted
        validator.validateVoucherStatusInNewTransaction(header);

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_BANK_DEBIT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                header.getTransactionDate().toLocalDate()
        );
    }

    @Override
    public Map<String, Object> listBankDebitVouchers(String documentId, FilterRequestDto filters, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDateValue, endDateValue);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "LONG_NARRATION",   // label
                "TRANSACTION_POID");    // value

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    // ---------- internal helpers -------------------------------------------------------------

    private void persistChildCollections(BankDebitVoucherRequest request, Long transactionPoid, boolean newVoucher, String documentId) {

        upsertPaymentGlDetails(transactionPoid, request.getPaymentGlDetails(), documentId);
        upsertChargeDetails(transactionPoid, request.getChargeDetails());
        upsertItemDetails(transactionPoid, request.getItemDetails());

        if (!newVoucher) {
            cleanupRemovedRows(transactionPoid, request);
        }

        if (request.getTermsAndConditionDtoList() != null &&
                !request.getTermsAndConditionDtoList().isEmpty()) {

            List<GlobalTermsInsertRequestDto> termsList = new ArrayList<>();
            long seq = 1;
            for (TermsAndConditionDto termsAndConditionDto : request.getTermsAndConditionDtoList()) {

                GlobalTermsInsertRequestDto dto = new GlobalTermsInsertRequestDto();

                dto.setGroupPoid(UserContext.getGroupPoid());
                dto.setCompanyPoid(UserContext.getCompanyPoid());
                dto.setDocId(documentId);
                dto.setDocKeyPoid(transactionPoid);

                dto.setLoginUserPoid(UserContext.getUserPoid());
                dto.setTermsPoid(termsAndConditionDto.getTermsPoid());
                dto.setDetRowId(termsAndConditionDto.getDetRowId());
                dto.setRowSeqNo(seq++);

                dto.setClauseNo(termsAndConditionDto.getClauseNo());
                dto.setClauseDetails(termsAndConditionDto.getClauseDetails());

                termsList.add(dto);
            }

            if (!termsList.isEmpty()) {
                globalTermsServiceClient.insertGlobalTerms(termsList);
            }
        }
    }

    private void upsertPaymentGlDetails(Long transactionPoid, List<PaymentGlDetails> details, String documentId) {

        if (details == null || details.isEmpty()) return;

        String docId = UserContext.getDocumentId();
        String key = transactionPoid.toString();
        List<BillwiseBreakupRequestDto> billwiseRequestDtoList = new ArrayList<>();
        List<CostCenterBreakupRequestDto> costCenterRequestDtoList = new ArrayList<>();
        List<LogRequestDto<GlBankDebitDtlGl>> logRequests = new ArrayList<>();

        for (PaymentGlDetails dtl : details) {

            String rawAction = dtl.getActionType();
            String action = (rawAction == null || rawAction.trim().isEmpty())
                    ? "NOCHANGES"
                    : rawAction.trim().toUpperCase();

            action = switch (action) {
                case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                case "ISDELETED", "DELETED" -> "ISDELETED";
                default -> "NOCHANGES";
            };

            switch (action) {
                case "NOCHANGES" -> {
                }
                case "ISDELETED" -> {
                    if (dtl.getDetRowId() != null) {
                        GlBankDebitDtlGl toDelete = paymentGlRepository.findById(new GlBankDebitDtlGlId(transactionPoid, dtl.getDetRowId()))
                                .orElse(null);
                        if (toDelete != null) {
                            loggingService.logDelete(toDelete, docId, key);
                        }
                        paymentGlRepository.deleteById(new GlBankDebitDtlGlId(transactionPoid, dtl.getDetRowId()));
                    }
                }
                case "ISCREATED" -> {
                    if (!glMasterRepository.existsByGlPoid(dtl.getGlPoid())) {
                        throw new ResourceNotFoundException("Gl Master", "glPoid", dtl.getGlPoid());
                    }
                    if (dtl.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(dtl.getTaxPoid())) {
                        throw new ResourceNotFoundException("Tax", "taxPoid", dtl.getTaxPoid());
                    }

                    GlBankDebitDtlGl entity = new GlBankDebitDtlGl();
                    Long detRowId = dtl.getDetRowId() != null ? dtl.getDetRowId() : getNextDetRowIdForPaymentGl(transactionPoid);
                    entity.setId(new GlBankDebitDtlGlId(transactionPoid, detRowId));
                    // populateCreateAudit(entity);
                    mapToPaymentGlEntity(dtl, entity);
                    paymentGlRepository.save(entity);

                    String logDetail = String.format("Row Created on Payment GL Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(docId, key, logDetail);

                    if (dtl.getBreakupList() != null && !dtl.getBreakupList().isEmpty()) {
                        List<BillwiseBreakupRequestDto> bwList = dtl.getBreakupList().stream().map(p -> {
                            BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();
                            dto.setGroupPoid(UserContext.getGroupPoid());
                            dto.setCompanyPoid(UserContext.getCompanyPoid());
                            dto.setDocId(documentId);
                            dto.setMainDetRowId(detRowId);
                            dto.setTransactionPoid(transactionPoid);
                            dto.setGlPoid(dtl.getGlPoid());
                            dto.setGlCompanyPoid(dtl.getCompanyPoid() != null ? dtl.getCompanyPoid() : UserContext.getCompanyPoid());
                            dto.setBillDetRowId(p.getBillDetRowId());
                            dto.setBillRefType(p.getBillRefType());
                            dto.setBillRef(p.getBillRef());
                            dto.setBillDueDate(p.getBillDueDate());
                            dto.setBillOriginalAmount(p.getBillOriginalAmount());
                            if ("CR".equalsIgnoreCase(p.getType())) {
                                dto.setCrAmt(p.getAmount());
                            } else {
                                dto.setDrAmt(p.getAmount());
                            }
                            dto.setBillRemarks(p.getBillRemarks());
                            dto.setLoginUserPoid(UserContext.getUserPoid());
                            return dto;
                        }).toList();
                        billwiseRequestDtoList.addAll(bwList);
                    }

                    if (dtl.getCostCenterList() != null && !dtl.getCostCenterList().isEmpty()) {
                        List<CostCenterBreakupRequestDto> ccList = dtl.getCostCenterList().stream().map(p -> {
                            CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
                            dto.setGroupPoid(UserContext.getGroupPoid());
                            dto.setCompanyPoid(UserContext.getCompanyPoid());
                            dto.setDocId(documentId);
                            dto.setTransactionPoid(transactionPoid);
                            dto.setMainDetRowId(detRowId);
                            dto.setGlPoid(dtl.getGlPoid());
                            dto.setCostDetRowId(p.getCostDetRowId());
                            dto.setCostGroup(p.getCostGroup());
                            dto.setCostPoid(p.getCostPoid());
                            dto.setAmount(p.getAmount());
                            dto.setLoginUserPoid(UserContext.getUserPoid());
                            return dto;
                        }).toList();
                        costCenterRequestDtoList.addAll(ccList);
                    }
                }
                case "ISUPDATED" -> {
                    if (!glMasterRepository.existsByGlPoid(dtl.getGlPoid())) {
                        throw new ResourceNotFoundException("Gl Master", "glPoid", dtl.getGlPoid());
                    }
                    if (dtl.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(dtl.getTaxPoid())) {
                        throw new ResourceNotFoundException("Tax", "taxPoid", dtl.getTaxPoid());
                    }

                    GlBankDebitDtlGl entity = paymentGlRepository.findById(new GlBankDebitDtlGlId(transactionPoid, dtl.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Payment GL Detail", "detRowId", dtl.getDetRowId()));

                    GlBankDebitDtlGl oldEntity = new GlBankDebitDtlGl();
                    BeanUtils.copyProperties(entity, oldEntity);

                    //populateUpdateAudit(entity);
                    mapToPaymentGlEntity(dtl, entity);
                    paymentGlRepository.save(entity);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dtl.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, entity, GlBankDebitDtlGl.class, docId, key, logDetail));

                    if (dtl.getBreakupList() != null && !dtl.getBreakupList().isEmpty()) {
                        List<BillwiseBreakupRequestDto> bwList = dtl.getBreakupList().stream().map(p -> {
                            BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();
                            dto.setGroupPoid(UserContext.getGroupPoid());
                            dto.setCompanyPoid(UserContext.getCompanyPoid());
                            dto.setDocId(documentId);
                            dto.setTransactionPoid(transactionPoid);
                            dto.setMainDetRowId(dtl.getDetRowId());
                            dto.setGlPoid(dtl.getGlPoid());
                            dto.setGlCompanyPoid(dtl.getCompanyPoid() != null ? dtl.getCompanyPoid() : UserContext.getCompanyPoid());
                            dto.setBillDetRowId(p.getBillDetRowId());
                            dto.setBillRefType(p.getBillRefType());
                            dto.setBillRef(p.getBillRef());
                            dto.setBillDueDate(p.getBillDueDate());
                            dto.setBillOriginalAmount(p.getBillOriginalAmount());
                            if ("CR".equalsIgnoreCase(p.getType())) {
                                dto.setCrAmt(p.getAmount());
                            } else {
                                dto.setDrAmt(p.getAmount());
                            }
                            dto.setBillRemarks(p.getBillRemarks());
                            dto.setLoginUserPoid(UserContext.getUserPoid());
                            return dto;
                        }).toList();
                        billwiseRequestDtoList.addAll(bwList);
                    }

                    if (dtl.getCostCenterList() != null && !dtl.getCostCenterList().isEmpty()) {
                        List<CostCenterBreakupRequestDto> ccList = dtl.getCostCenterList().stream().map(p -> {
                            CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
                            dto.setGroupPoid(UserContext.getGroupPoid());
                            dto.setCompanyPoid(UserContext.getCompanyPoid());
                            dto.setDocId(documentId);
                            dto.setTransactionPoid(transactionPoid);
                            dto.setMainDetRowId(dtl.getDetRowId());
                            dto.setGlPoid(dtl.getGlPoid());
                            dto.setCostDetRowId(p.getCostDetRowId());
                            dto.setCostGroup(p.getCostGroup());
                            dto.setCostPoid(p.getCostPoid());
                            dto.setAmount(p.getAmount());
                            dto.setLoginUserPoid(UserContext.getUserPoid());
                            return dto;
                        }).toList();
                        costCenterRequestDtoList.addAll(ccList);
                    }
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        if (CollectionUtils.isNotEmpty(costCenterRequestDtoList)) {
            costCenterBreakupService.updateCostCenterBreakups(costCenterRequestDtoList, UserContext.getUserPoid());
        }
        if (CollectionUtils.isNotEmpty(billwiseRequestDtoList)) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseRequestDtoList, UserContext.getUserPoid());
        }
    }

    private void upsertChargeDetails(Long transactionPoid, List<ChargeDetailDto> details) {

        if (details == null || details.isEmpty()) return;

        // Filter out rows where checkAll='N' — legacy drops these before save (GAP-17)
        details = details.stream()
                .filter(d -> !"N".equalsIgnoreCase(d.getCheckAll()))
                .collect(Collectors.toList());

        String docId = UserContext.getDocumentId();
        String key = transactionPoid.toString();
        List<LogRequestDto<GlBankDebitChargeDtl>> logRequests = new ArrayList<>();

        for (ChargeDetailDto dto : details) {

            String rawAction = dto.getActionType();
            String action = (rawAction == null || rawAction.trim().isEmpty()) ? "ISCREATED" : rawAction.trim().toUpperCase();

            action = switch (action) {
                case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                case "ISDELETED", "DELETED" -> "ISDELETED";
                default -> "NOCHANGES";
            };

            switch (action) {
                case "NOCHANGES" -> {
                }
                case "ISDELETED" -> {
                    if (dto.getDetRowId() != null) {
                        GlBankDebitChargeDtl toDelete = chargeDetailRepository.findById(new GlBankDebitChargeDtlId(transactionPoid, dto.getDetRowId()))
                                .orElse(null);
                        if (toDelete != null) {
                            loggingService.logDelete(toDelete, docId, key);
                        }
                        chargeDetailRepository.deleteById(new GlBankDebitChargeDtlId(transactionPoid, dto.getDetRowId()));
                    }
                }
                case "ISCREATED" -> {
                    if (!shipChargeRepository.existsByChargePoid(dto.getChargePoid())) {
                        throw new ResourceNotFoundException("Ship Charge", "chargePoid", dto.getChargePoid());
                    }
                    if (dto.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(dto.getTaxPoid())) {
                        throw new ResourceNotFoundException("Tax", "taxPoid", dto.getTaxPoid());
                    }

                    GlBankDebitChargeDtl entity = new GlBankDebitChargeDtl();
                    Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : getNextDetRowIdForCharge(transactionPoid);
                    entity.setId(new GlBankDebitChargeDtlId(transactionPoid, detRowId));
                    //populateCreateAudit(entity);
                    mapToChargeEntity(dto, entity);
                    chargeDetailRepository.save(entity);

                    String logDetail = String.format("Row Created on Charge Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(docId, key, logDetail);
                }
                case "ISUPDATED" -> {
                    if (!shipChargeRepository.existsByChargePoid(dto.getChargePoid())) {
                        throw new ResourceNotFoundException("Ship Charge", "chargePoid", dto.getChargePoid());
                    }
                    if (dto.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(dto.getTaxPoid())) {
                        throw new ResourceNotFoundException("Tax", "taxPoid", dto.getTaxPoid());
                    }

                    GlBankDebitChargeDtl entity = chargeDetailRepository.findById(new GlBankDebitChargeDtlId(transactionPoid, dto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId", dto.getDetRowId()));

                    GlBankDebitChargeDtl oldEntity = new GlBankDebitChargeDtl();
                    BeanUtils.copyProperties(entity, oldEntity);

                    // populateUpdateAudit(entity);
                    mapToChargeEntity(dto, entity);
                    chargeDetailRepository.save(entity);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, entity, GlBankDebitChargeDtl.class, docId, key, logDetail));
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void upsertItemDetails(Long transactionPoid, List<ItemDetailDto> details) {
        if (details == null || details.isEmpty()) return;

        String docId = UserContext.getDocumentId();
        String key = transactionPoid.toString();
        List<LogRequestDto<GlBankDebitItemDtl>> logRequests = new ArrayList<>();

        for (ItemDetailDto dto : details) {
            String rawAction = dto.getActionType();
            String action = (rawAction == null || rawAction.trim().isEmpty()) ? "ISCREATED" : rawAction.trim().toUpperCase();

            action = switch (action) {
                case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                case "ISDELETED", "DELETED" -> "ISDELETED";
                default -> "NOCHANGES";
            };

            switch (action) {
                case "NOCHANGES" -> {
                }
                case "ISDELETED" -> {
                    if (dto.getDetRowId() != null) {
                        GlBankDebitItemDtl toDelete = itemDetailRepository.findById(new GlBankDebitItemDtlId(transactionPoid, dto.getDetRowId()))
                                .orElse(null);
                        if (toDelete != null) {
                            loggingService.logDelete(toDelete, docId, key);
                        }
                        itemDetailRepository.deleteById(new GlBankDebitItemDtlId(transactionPoid, dto.getDetRowId()));
                    }
                }
                case "ISCREATED" -> {
                    GlBankDebitItemDtl entity = new GlBankDebitItemDtl();
                    Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : getNextDetRowIdForItem(transactionPoid);
                    entity.setId(new GlBankDebitItemDtlId(transactionPoid, detRowId));
                    mapToItemEntity(dto, entity);
                    itemDetailRepository.save(entity);

                    String logDetail = String.format("Row Created on Item Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(docId, key, logDetail);
                }
                case "ISUPDATED" -> {
                    GlBankDebitItemDtl entity = itemDetailRepository.findById(new GlBankDebitItemDtlId(transactionPoid, dto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Item Detail", "detRowId", dto.getDetRowId()));

                    GlBankDebitItemDtl oldEntity = new GlBankDebitItemDtl();
                    BeanUtils.copyProperties(entity, oldEntity);

                    mapToItemEntity(dto, entity);
                    itemDetailRepository.save(entity);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, entity, GlBankDebitItemDtl.class, docId, key, logDetail));
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void cleanupRemovedRows(Long transactionPoid, BankDebitVoucherRequest request) {
        Set<Long> paymentIds = ids(request.getPaymentGlDetails());
        Set<Long> chargeIds = ids(request.getChargeDetails());
        Set<Long> itemIds = ids(request.getItemDetails());

        if (paymentIds != null) {
            paymentGlRepository.deleteByTransactionPoidAndDetRowIdNotIn(transactionPoid, paymentIds);
        }
        if (chargeIds != null) {
            chargeDetailRepository.deleteByTransactionPoidAndDetRowIdNotIn(transactionPoid, chargeIds);
        }
        if (itemIds != null) {
            itemDetailRepository.deleteByTransactionPoidAndDetRowIdNotIn(transactionPoid, itemIds);
        }
    }

    private String[] runPreSaveProcedures(GlBankDebitHdr existingHeader, String suppressBalanceCheck) {
        // Always call validateBeforeSave (mirrors legacy DocumentBeforeSave DB-side validation)
        bankPaymentVoucherSpRepository.validateBeforeSave(
                existingHeader != null ? existingHeader.getTransactionPoid() : null,
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                getCurrentUser(),
                suppressBalanceCheck
        );

        if (existingHeader != null) {
            return bankDebitVoucherCustomRepository.procGlJobRelOldValues(
                    UserContext.getGroupPoid(),
                    UserContext.getUserPoid(),
                    UserContext.getCompanyPoid(),
                    "400-111",
                    existingHeader.getTransactionPoid()
            );
        }
        return null;
    }

    private void populateCreateAudit(GlBankDebitHdr entity) {
        entity.setDeleted("N");
    }

   /* private void populateUpdateAudit(GlBankDebitHdr entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }*/

    private void populateCreateAudit(GlBankDebitDtlGl entity) {
    }

  /*  private void populateUpdateAudit(GlBankDebitDtlGl entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }*/

  /*  private void populateCreateAudit(GlBankDebitChargeDtl entity) {
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }*/

  /*  private void populateUpdateAudit(GlBankDebitChargeDtl entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }*/

  /*  private void populateCreateAudit(GlBankDebitItemDtl entity) {
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }*/

  /*  private void populateUpdateAudit(GlBankDebitItemDtl entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }*/

    private void mapRequestToEntity(BankDebitVoucherRequest request, GlBankDebitHdr entity) {
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setBankPoid(request.getBankPoid());
        entity.setPayGlPoid(request.getPayGlPoid());
        entity.setAmount(request.getAmount());
        entity.setCurrencyCode(request.getCurrencyCode());
        entity.setCurrencyRate(request.getCurrencyRate());
        entity.setCurrencyAmt(request.getCurrencyAmt());
        entity.setLongNarration(request.getLongNarration());
        entity.setRemarks(request.getRemarks());
        entity.setPayingType(request.getPayingType());
        entity.setRefType(request.getRefType());
        entity.setFfRef(resolveEffectiveFfRef(request));
        entity.setFdaRef(request.getFdaRef());
        entity.setSalesQtnRef(request.getSalesQtnRef());
        entity.setMultiCompany(request.getMultiCompany());
        entity.setPayingToName(request.getPayingToName());
        entity.setTtChargeType(request.getTtChargeType());
        entity.setBeneficiaryIban(request.getBeneficiaryIban());
        entity.setBeneficiaryBankPoid(request.getBeneficiaryBankPoid());
        entity.setTaxAmount(request.getTaxAmount());
        entity.setTaxPoid(request.getTaxPoid());
        entity.setTaxPercentage(request.getTaxPercentage());
        entity.setBankPurposePoid(request.getBankPurposePoid());
        entity.setTtDate(request.getTtDate());
        entity.setGainLoss(request.getGainLoss());
        entity.setGainLossType(request.getGainLossType());
        entity.setBankCharges(request.getBankCharges());
        entity.setTtSpecialRate(request.getTtSpecialRate());
        entity.setRateDealNo(request.getRateDealNo());
//        entity.setDocRef(request.getDocRef());
        entity.setFileGenerated(request.getFileGenerated() != null ? request.getFileGenerated() : "N");
        entity.setFileName(request.getFileName());
        entity.setFileGeneratedDate(request.getFileGeneratedDate());
        entity.setFileGeneratedBy(request.getFileGeneratedBy());
        entity.setPayingTo(request.getPayingTo());
        entity.setFileUniqueId(request.getFileUniqueId());
        entity.setTransactionDate(LocalDateTime.now());
        entity.setSuppressValidation(request.isSuppressBalanceCheck() ? "Y" : "N");
//        entity.setConfidentialRemarks(request.getConfidentialRemarks());
    }

    private BankDebitVoucherResponse mapEntityToResponse(GlBankDebitHdr entity) {
        BankDebitVoucherResponse response = new BankDebitVoucherResponse();
        response.setTransactionPoid(entity.getTransactionPoid());
        response.setTransactionDate(entity.getTransactionDate());
        String docId = "400-111";
        response.setGroupPoid(entity.getGroupPoid());
        if (entity.getGroupPoid() != null) {
            response.setGroupDet(lovService.getDetailsByPoidAndLovName(entity.getGroupPoid(), "GROUP"));
        }
        response.setCompanyPoid(entity.getCompanyPoid());
        if (entity.getCompanyPoid() != null) {
            response.setCompanyDet(lovService.getDetailsByPoidAndLovName(entity.getCompanyPoid(), "COMPANY"));
        }
        response.setDocRef(entity.getDocRef());
        response.setPayGlPoid(entity.getPayGlPoid());
        if (entity.getPayGlPoid() != null) {
            response.setPayGlDet(lovService.getDetailsByPoidAndLovName(entity.getPayGlPoid(), "BDV_GL_MASTER_LEDGERS"));
        }
        response.setPayingTo(entity.getPayingTo());
        response.setPayingType(entity.getPayingType());
        response.setBankPoid(entity.getBankPoid());
        if (entity.getBankPoid() != null) {
            response.setBankDet(lovService.getDetailsByPoidAndLovName(entity.getBankPoid(), "BDV_BANK_MASTER_COMPANY_WISE"));
        }
        response.setAmount(entity.getAmount());
        response.setShortNarration(entity.getShortNarration());
        response.setLongNarration(entity.getLongNarration());
        response.setTtDate(entity.getTtDate());
        response.setCurrencyCode(entity.getCurrencyCode());
        if (StringUtils.isNoneBlank(entity.getCurrencyCode())) {
            response.setCurrencyDet(lovService.getDetailsByCodeAndLovName(entity.getCurrencyCode(), "CURRENCY"));
        }
        response.setCurrencyRate(entity.getCurrencyRate());
        response.setCurrencyAmt(entity.getCurrencyAmt());
        response.setRefType(entity.getRefType());
        response.setFfRef(entity.getFfRef());
        if (entity.getFfRef() != null && !entity.getFfRef().isBlank()) {
            List<String> rawRefs = Arrays.asList(entity.getFfRef().split(";"));
            response.setFfRefs(rawRefs);
            List<Long> ffPoids = rawRefs.stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try { return java.util.stream.Stream.of(Long.parseLong(s)); }
                        catch (NumberFormatException ignored) { return java.util.stream.Stream.empty(); }
                    })
                    .collect(Collectors.toList());
            if (!ffPoids.isEmpty()) {
                Map<Long, LovGetListDto> ffLovMap = lovService.getDetailsByPoidsAndLovName(ffPoids, "FF_JOBS_FOR_COST_BOOKING");
                List<LovGetListDto> ffRefsDtl = ffPoids.stream()
                        .map(ffLovMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!ffRefsDtl.isEmpty()) {
                    response.setFfRefsDtl(ffRefsDtl);
                }
            }
        }
        response.setFdaRef(entity.getFdaRef());
        response.setMtaRef(entity.getMtaRef());
        response.setSalesQtnRef(entity.getSalesQtnRef());
        response.setPayingToName(entity.getPayingToName());
        response.setRemarks(entity.getRemarks());
        response.setTtChargeType(entity.getTtChargeType());
        response.setBeneficiaryIban(entity.getBeneficiaryIban());
        response.setBeneficiaryBankPoid(entity.getBeneficiaryBankPoid());
        if (entity.getBeneficiaryBankPoid() != null) {
            response.setBeneficiaryBankDet(lovService.getDetailsByPoidAndLovName(entity.getBeneficiaryBankPoid(), "BANK_PURPOSE_CODE"));
        }
        response.setTaxAmount(entity.getTaxAmount());
        response.setTaxPoid(entity.getTaxPoid());
        if (entity.getTaxPoid() != null) {
            response.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "BDV_INPUT_TAX_MASTER"));
        }
        response.setTaxPercentage(entity.getTaxPercentage());
        response.setBankPurposePoid(entity.getBankPurposePoid());
        if (entity.getBankPurposePoid() != null) {
            response.setBankPurposeDet(lovService.getDetailsByPoidAndLovName(entity.getBankPurposePoid(), "BANK_PURPOSE_CODE"));
        }
        response.setDeleted(entity.getDeleted());
        response.setGainLoss(entity.getGainLoss());
        response.setGainLossType(entity.getGainLossType());
        response.setBankCharges(entity.getBankCharges());
        response.setTtSpecialRate(entity.getTtSpecialRate());
        response.setRateDealNo(entity.getRateDealNo());
        response.setDivisionCode(entity.getDivisionCode());
        response.setBdvSuppressBalanceByTx(entity.getSuppressValidation());
        response.setBankBalance(entity.getBankBalance());
        response.setAvailableBalance(entity.getAvailableBalance());
        response.setFileGenerated(entity.getFileGenerated());
        response.setFileName(entity.getFileName());
        response.setFileGeneratedDate(entity.getFileGeneratedDate());
        response.setFileGeneratedBy(entity.getFileGeneratedBy());
        response.setPayingTo(entity.getPayingTo());
        response.setFileUniqueId(entity.getFileUniqueId());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastModifiedBy(entity.getLastModifiedBy());
        response.setLastModifiedDate(entity.getLastModifiedDate());
//        response.setConfidentialRemarks(entity.getConfidentialRemarks());

        // Load child entities
        response.setPaymentGlDetails(paymentGlRepository.findByIdTransactionPoid(entity.getTransactionPoid())
                .stream().map(this::mapPaymentGlEntityToDto).toList());
        response.setChargeDetails(chargeDetailRepository.findByIdTransactionPoid(entity.getTransactionPoid())
                .stream().map(this::mapChargeEntityToDto).toList());
        response.setItemDetails(itemDetailRepository.findByIdTransactionPoid(entity.getTransactionPoid())
                .stream().map(this::mapItemEntityToDto).toList());
        response.setTermsAndConditionDtoList(
                globalTermsCustomChangesRepository
                        .findByIdDocIdAndIdDocKeyPoid(docId, entity.getTransactionPoid())
                        .stream()
                        .map(this::mapTermsEntityToDto)
                        .toList()
        );

        return response;
    }

    private void mapToPaymentGlEntity(PaymentGlDetails dto, GlBankDebitDtlGl entity) {
        entity.setType(dto.getType());
        // Use row-level companyPoid when provided (multi-company mode); fall back to session company (GAP-09)
        entity.setCompanyPoid(dto.getCompanyPoid() != null ? dto.getCompanyPoid() : UserContext.getCompanyPoid());
        entity.setGlPoid(dto.getGlPoid());
        entity.setDrAmt(dto.getDrAmt());
        entity.setCrAmt(dto.getCrAmt());
        entity.setRemarks(dto.getRemarks());
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setPartyInvNumber(dto.getPartyInvNumber());
        entity.setPartyInvDate(dto.getPartyInvDate());
    }

    private void mapToChargeEntity(ChargeDetailDto dto, GlBankDebitChargeDtl entity) {
        entity.setChargePoid(dto.getChargePoid() != null && dto.getChargePoid() > 0 ? dto.getChargePoid() : null);
        entity.setChargeAmount(dto.getChargeAmount());
        entity.setDescription(dto.getDescription());
        entity.setRemarks(dto.getRemarks());
        entity.setRefDocId(dto.getRefDocId());
        entity.setRefDocPoid(dto.getRefDocPoid() != null && dto.getRefDocPoid() > 0 ? dto.getRefDocPoid() : null);
        entity.setFdaDetRowId(dto.getFdaDetRowId());
        entity.setCheckAll(dto.getCheckAll());
        entity.setPdaAmount(dto.getPdaAmount());
        entity.setFfAmount(dto.getFfAmount());
        entity.setTaxPoid(dto.getTaxPoid() != null && dto.getTaxPoid() > 0 ? dto.getTaxPoid() : null);
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        entity.setChargeBaseAmount(dto.getChargeBaseAmount());
    }

    private void mapToItemEntity(ItemDetailDto dto, GlBankDebitItemDtl entity) {
        entity.setStockPoid(dto.getStockPoid());
        entity.setStockUnitPoid(dto.getStockUnitPoid());
        entity.setPoQty(dto.getPoQty());
        entity.setDnQty(dto.getDnQty());
        entity.setQtyReceived(dto.getQtyReceived());
        entity.setPrice(dto.getPrice());
        entity.setDiscount(dto.getDiscount());
        entity.setTotal(dto.getTotal());
        entity.setRemarks(dto.getRemarks());
        entity.setRefDocId(dto.getRefDocId());
        entity.setRefDocPoid(dto.getRefDocPoid());
        entity.setRefDetRowId(dto.getRefDetRowId());
    }

    private Long getNextDetRowIdForPaymentGl(Long transactionPoid) {
        Long maxId = paymentGlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        return maxId != null ? maxId + 1 : 1L;
    }

    private Long getNextDetRowIdForCharge(Long transactionPoid) {
        Long maxId = chargeDetailRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        return maxId != null ? maxId + 1 : 1L;
    }

    private Long getNextDetRowIdForItem(Long transactionPoid) {
        Long maxId = itemDetailRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        return maxId != null ? maxId + 1 : 1L;
    }

    private PaymentGlDetails mapPaymentGlEntityToDto(GlBankDebitDtlGl entity) {
        PaymentGlDetails dto = new PaymentGlDetails();
        dto.setDetRowId(entity.getId().getDetRowId());
        dto.setTransactionPoid(entity.getId().getTransactionPoid());
        dto.setType(entity.getType());
        dto.setCompanyPoid(entity.getCompanyPoid());
        if (entity.getCompanyPoid() != null) {
            dto.setCompanyDet(lovService.getDetailsByPoidAndLovName(entity.getCompanyPoid(), "COMPANY"));
        }
        dto.setGlPoid(entity.getGlPoid());
        if (entity.getGlPoid() != null) {
            dto.setGlDet(lovService.getDetailsByPoidAndLovName(entity.getGlPoid(), "GL_MASTER_LEDGERS_BDV_DTL"));
        }
        dto.setDrAmt(entity.getDrAmt());
        dto.setCrAmt(entity.getCrAmt());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxPoid(entity.getTaxPoid());
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "BDV_INPUT_TAX_MASTER"));
        }
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setPartyInvNumber(entity.getPartyInvNumber());
        dto.setPartyInvDate(entity.getPartyInvDate());
        return dto;
    }

    private ChargeDetailDto mapChargeEntityToDto(GlBankDebitChargeDtl entity) {
        ChargeDetailDto dto = new ChargeDetailDto();
        dto.setDetRowId(entity.getId().getDetRowId());
        dto.setTransactionPoid(entity.getId().getTransactionPoid());
        dto.setChargePoid(entity.getChargePoid());
        if (entity.getChargePoid() != null) {
            dto.setChargeDet(lovService.getDetailsByPoidAndLovName(entity.getChargePoid(), "CHARGE_MASTER_ALL"));
        }
        dto.setChargeAmount(entity.getChargeAmount());
        dto.setDescription(entity.getDescription());
        dto.setRemarks(entity.getRemarks());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setFdaDetRowId(entity.getFdaDetRowId());
        dto.setCheckAll(entity.getCheckAll());
        dto.setPdaAmount(entity.getPdaAmount());
        dto.setFfAmount(entity.getFfAmount());
        dto.setTaxPoid(entity.getTaxPoid());
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "BDV_INPUT_TAX_MASTER"));
        }
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setChargeBaseAmount(entity.getChargeBaseAmount());
        return dto;
    }

    private ItemDetailDto mapItemEntityToDto(GlBankDebitItemDtl entity) {
        ItemDetailDto dto = new ItemDetailDto();
        dto.setDetRowId(entity.getId().getDetRowId());
        dto.setTransactionPoid(entity.getId().getTransactionPoid());
        dto.setStockPoid(entity.getStockPoid());
        if (entity.getStockPoid() != null) {
            dto.setStockDet(lovService.getDetailsByPoidAndLovName(entity.getStockPoid(), "BANK_PURPOSE_CODE"));
        }
        dto.setStockUnitPoid(entity.getStockUnitPoid());
        if (entity.getStockUnitPoid() != null) {
            dto.setStockUnitDet(lovService.getDetailsByPoidAndLovName(entity.getStockUnitPoid(), "BANK_PURPOSE_CODE"));
        }
        dto.setPoQty(entity.getPoQty());
        dto.setDnQty(entity.getDnQty());
        dto.setQtyReceived(entity.getQtyReceived());
        dto.setPrice(entity.getPrice());
        dto.setDiscount(entity.getDiscount());
        dto.setTotal(entity.getTotal());
        dto.setRemarks(entity.getRemarks());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setRefDetRowId(entity.getRefDetRowId());
        return dto;
    }

    private TermsAndConditionDto mapTermsEntityToDto(GlobalTermsCustomChanges entity) {
        TermsAndConditionDto dto = new TermsAndConditionDto();

        if (entity.getId() != null) {
            dto.setDetRowId(entity.getId().getDetRowId());
        }

        dto.setTermsPoid(entity.getRefTermsPoid());
        dto.setClauseNo(entity.getClauseNo());
        dto.setClauseDetails(entity.getClauseDetails());
        return dto;
    }

    private TermsAndConditionDto mapTermsDto(GlobalTermsDto dto) {
        TermsAndConditionDto t = new TermsAndConditionDto();

        t.setDetRowId(dto.getDetRowId());
        t.setTermsPoid(dto.getRefTermsPoid());
        t.setClauseNo(dto.getClauseNo());
        t.setClauseDetails(dto.getClauseDetails());

        return t;
    }

    /**
     * Load billwise and cost center breakup data into response (reusable method)
     * Called from GET, CREATE, and UPDATE methods
     * Similar pattern to CreditNote, DebitNote, ApPurchaseJournal
     */
    private void loadBreakupsIntoResponse(BankDebitVoucherResponse response, Long transactionPoid, String documentId, Long groupPoid, Long companyPoid) {
        if (response.getPaymentGlDetails() == null || response.getPaymentGlDetails().isEmpty()) {
            return;
        }

        Long userPoid = UserContext.getUserPoid();

        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transactionPoid);

        GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                costCenterBreakupService.loadCostCenterData(documentId, transactionPoid, groupPoid, companyPoid, userPoid);

        for (PaymentGlDetails dtl : response.getPaymentGlDetails()) {
            Long detRowId = dtl.getDetRowId();

            // Billwise → popup list
            if (billwiseResponse != null
                    && billwiseResponse.getLoadBillwiseBreakupResponseDtoList() != null) {

                List<BillwiseBreakupPopupRequestDto> bwList =
                        billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                                .filter(bw -> Objects.equals(bw.getMainDetRowId(), detRowId))
                                .map(bw -> {
                                    BillwiseBreakupPopupRequestDto dto = new BillwiseBreakupPopupRequestDto();
                                    dto.setBillDetRowId(bw.getBillDetRowId());
                                    dto.setBillRefType(bw.getBillRefType());
                                    dto.setBillRef(bw.getBillRef());
                                    dto.setBillDueDate(bw.getBillDueDate());
                                    dto.setBillOriginalAmount(bw.getBillOriginalAmount() != null
                                            ? bw.getBillOriginalAmount()
                                            : (bw.getDrAmt() != null && bw.getDrAmt().compareTo(BigDecimal.ZERO) > 0
                                            ? bw.getDrAmt()
                                            : (bw.getCrAmt() != null && bw.getCrAmt().compareTo(BigDecimal.ZERO) > 0
                                            ? bw.getCrAmt()
                                            : null)));
                                    // Amount & type from DR/CR amounts - preserve null when neither side is set
                                    if (bw.getDrAmt() != null && bw.getDrAmt().compareTo(BigDecimal.ZERO) > 0) {
                                        dto.setType("DR");
                                        dto.setAmount(bw.getDrAmt());
                                    } else if (bw.getCrAmt() != null && bw.getCrAmt().compareTo(BigDecimal.ZERO) > 0) {
                                        dto.setType("CR");
                                        dto.setAmount(bw.getCrAmt());
                                    } else {
                                        dto.setType(null);
                                        dto.setAmount(null);
                                    }
                                    dto.setBillRemarks(bw.getBillRemarks());
                                    return dto;
                                })
                                .collect(Collectors.toList());

                dtl.setBreakupList(bwList);
            }

            // Cost center → popup list
            if (costCenterResponse != null
                    && costCenterResponse.getCostBreakupList() != null) {

                List<CostCenterBreakupPopupRequestDto> ccList =
                        costCenterResponse.getCostBreakupList().stream()
                                .filter(cc -> Objects.equals(cc.getMainDetRowId(), detRowId))
                                .map(cc -> {
                                    CostCenterBreakupPopupRequestDto dto = new CostCenterBreakupPopupRequestDto();
                                    dto.setCostDetRowId(cc.getCostDetRowId());
                                    dto.setCostGroup(cc.getCostGroup());
                                    dto.setCostPoid(cc.getCostPoid());
                                    dto.setAmount(
                                            cc.getAmount() != null
                                                    ? (cc.getAmount())
                                                    : BigDecimal.ZERO
                                    );
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

                dtl.setCostCenterList(ccList);
            }
        }
    }

    private String resolveEffectiveFfRef(BankDebitVoucherRequest req) {
        List<String> refs = req.getFfRefs();
        if (refs != null && !refs.isEmpty()) {
            String joined = refs.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .collect(Collectors.joining(";"));
            if (!joined.isBlank()) return joined;
        }
        return req.getFfRef();
    }

    private Set<Long> ids(List<?> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item instanceof PaymentGlDetails || item instanceof ChargeDetailDto || item instanceof ItemDetailDto)
                .map(item -> {
                    if (item instanceof PaymentGlDetails) return ((PaymentGlDetails) item).getDetRowId();
                    if (item instanceof ChargeDetailDto) return ((ChargeDetailDto) item).getDetRowId();
                    if (item instanceof ItemDetailDto) return ((ItemDetailDto) item).getDetRowId();
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    // ---------- loaders ----------
    @Override
    public List<ChargeFFDto> loadFFCharges(List<Long> ffRefPoids) {
        String joined = ffRefPoids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(";"));
        return bankDebitVoucherCustomRepository.procLoadFFCharges(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                joined
        );
    }

    @Override
    public List<ChargeFDADto> loadFDACharges(Long fdaRefPoid) {
        return bankDebitVoucherCustomRepository.procLoadFDACharges(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                fdaRefPoid
        );
    }

    @Override
    public List<ItemDetailDto> loadMTAItems(Long salesQtnRefPoid) {
        return bankDebitVoucherCustomRepository.procLoadMTAItems(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                salesQtnRefPoid
        );
    }

    @Override
    public String getMtaRef(Long salesQtnRefPoid) {
        // Calls PROC_GL_PETTY_SET_MTAREF — returns the derived MtaRef display value after LOV selection (GAP-10)
        return bankDebitVoucherCustomRepository.procSetMtaRef(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                salesQtnRefPoid
        );
    }

    /**
     * Generates default GL journal entry rows based on header fields.
     * Mirrors legacy BankDebitVoucherBean.ArrayTableStartDefaultRows() (GAP-05).
     *
     * GENERAL + PayingType != 4:
     *   Dr(PayGL, Amount ± GainLoss), Cr(BankGL, Amount)
     *   Dr(BankChargesGL, BankCharges + Tax), Cr(BankGL, BankCharges + Tax)  — if BankCharges > 0
     *   Dr or Cr(GainLossGL, GainLoss)                                       — if GainLoss > 0
     * CUSTOM + PayingType != 4:
     *   Cr(BankGL, Amount) only
     * PayingType = 4:
     *   Dr(BankChargesGL, Amount), Cr(BankGL, Amount)
     */
    @Override
    public List<PaymentGlDetails> generateDefaultGlRows(BankDebitVoucherRequest request) {
        List<PaymentGlDetails> rows = new ArrayList<>();
        if (request.getBankPoid() == null) return rows;

        Long bankGlPoid = bankDebitVoucherCustomRepository.getBankGlPoid(request.getBankPoid());
        if (bankGlPoid == null) return rows;

        String refType = request.getRefType() != null ? request.getRefType().trim() : "GENERAL";
        String payingType = request.getPayingType() != null ? request.getPayingType().trim() : "";
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        BigDecimal bankCharges = request.getBankCharges() != null ? request.getBankCharges() : BigDecimal.ZERO;
        BigDecimal taxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal gainLoss = request.getGainLoss() != null ? request.getGainLoss() : BigDecimal.ZERO;
        String gainLossType = request.getGainLossType();

        long detRowId = 1;

        if ("4".equals(payingType)) {
            // PayingType = 4 (Bank Charges only): Dr(BankChargesGL)/Cr(BankGL)
            String bankChargesGlPoidStr = globalParameterService.getParameterValue("BANK INTEREST CHARGES SHIPPING", "GROUP", "1", "30");
            if (bankChargesGlPoidStr != null) {
                Long bankChargesGlPoid = parseLong(bankChargesGlPoidStr);
                rows.add(buildGlRow(detRowId++, "DR", bankChargesGlPoid, amount, null));
                rows.add(buildGlRow(detRowId++, "CR", bankGlPoid, amount, null));
            }
            return rows;
        }

        if ("GENERAL".equalsIgnoreCase(refType) && request.getPayGlPoid() != null) {
            // Dr(PayGL, Amount ± GainLoss) / Cr(BankGL, Amount)
            BigDecimal payGlAmt = amount;
            if (gainLoss.compareTo(BigDecimal.ZERO) > 0) {
                payGlAmt = "CR".equalsIgnoreCase(gainLossType)
                        ? amount.add(gainLoss)
                        : amount.subtract(gainLoss);
            }
            rows.add(buildGlRow(detRowId++, "DR", request.getPayGlPoid(), payGlAmt, null));
            rows.add(buildGlRow(detRowId++, "CR", bankGlPoid, amount, null));

            // Bank charges rows
            if (bankCharges.compareTo(BigDecimal.ZERO) > 0) {
                String bankChargesGlPoidStr = globalParameterService.getParameterValue("BANK INTEREST CHARGES SHIPPING", "GROUP", "1", "30");
                if (bankChargesGlPoidStr != null) {
                    Long bankChargesGlPoid = parseLong(bankChargesGlPoidStr);
                    BigDecimal chargePlusTax = bankCharges.add(taxAmount);
                    rows.add(buildGlRow(detRowId++, "DR", bankChargesGlPoid, bankCharges, request.getTaxPoid(), request.getTaxPercentage(), taxAmount));
                    rows.add(buildGlRow(detRowId++, "CR", bankGlPoid, chargePlusTax, null));
                }
            }

            // FX Gain/Loss row
            if (gainLoss.compareTo(BigDecimal.ZERO) > 0) {
                String gainLossGlPoidStr = globalParameterService.getParameterValue("EXCHANGE GAIN LOSS ACCT", "GROUP", "1", "30");
                if (gainLossGlPoidStr != null) {
                    Long gainLossGlPoid = parseLong(gainLossGlPoidStr);
                    String gainLossRowType = "CR".equalsIgnoreCase(gainLossType) ? "CR" : "DR";
                    rows.add(buildGlRow(detRowId++, gainLossRowType, gainLossGlPoid, gainLoss, null));
                }
            }
        } else if ("CUSTOM".equalsIgnoreCase(refType)) {
            // CUSTOM + PayingType != 4: Cr(BankGL, Amount)
            rows.add(buildGlRow(detRowId++, "CR", bankGlPoid, amount, null));

            // Bank charges rows (mirrors legacy CUSTOM Bankcharges block)
            if (bankCharges.compareTo(BigDecimal.ZERO) > 0) {
                String bankChargesGlPoidStr = globalParameterService.getParameterValue("BANK INTEREST CHARGES SHIPPING", "GROUP", "1", "30");
                if (bankChargesGlPoidStr != null) {
                    Long bankChargesGlPoid = parseLong(bankChargesGlPoidStr);
                    BigDecimal chargePlusTax = bankCharges.add(taxAmount);
                    rows.add(buildGlRow(detRowId++, "CR", bankGlPoid, chargePlusTax, null));
                    rows.add(buildGlRow(detRowId++, "DR", bankChargesGlPoid, bankCharges, request.getTaxPoid(), request.getTaxPercentage(), taxAmount));
                }
            }

            // FX Gain/Loss row (mirrors legacy CUSTOM GainLoss block)
            if (gainLoss.compareTo(BigDecimal.ZERO) > 0) {
                String gainLossGlPoidStr = globalParameterService.getParameterValue("EXCHANGE GAIN LOSS ACCT", "GROUP", "1", "30");
                if (gainLossGlPoidStr != null) {
                    Long gainLossGlPoid = parseLong(gainLossGlPoidStr);
                    String gainLossRowType = "CR".equalsIgnoreCase(gainLossType) ? "CR" : "DR";
                    rows.add(buildGlRow(detRowId++, gainLossRowType, gainLossGlPoid, gainLoss, null));
                }
            }
        }

        return rows;
    }

    private PaymentGlDetails buildGlRow(long detRowId, String type, Long glPoid, BigDecimal amount, Long taxPoid) {
        return buildGlRow(detRowId, type, glPoid, amount, taxPoid, null, null);
    }

    private PaymentGlDetails buildGlRow(long detRowId, String type, Long glPoid, BigDecimal amount, Long taxPoid, BigDecimal taxPercentage, BigDecimal taxAmount) {
        PaymentGlDetails row = new PaymentGlDetails();
        row.setDetRowId(detRowId);
        row.setType(type);
        row.setGlPoid(glPoid);
        if ("DR".equals(type)) {
            row.setDrAmt(amount);
            row.setCrAmt(BigDecimal.ZERO);
        } else {
            row.setCrAmt(amount);
            row.setDrAmt(BigDecimal.ZERO);
        }
        row.setTaxPoid(taxPoid);
        row.setTaxPercentage(taxPercentage);
        row.setTaxAmount(taxAmount);
        BigDecimal total = amount != null ? amount : BigDecimal.ZERO;
        if (taxAmount != null) total = total.add(taxAmount);
        row.setTotalAmount(total);
        row.setActionType("ISCREATED");
        return row;
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------- utilities ----------
    @Override
    public BigDecimal getBankBalance(Long bankPoid, String documentId, LocalDate docDate) {
        return bankDebitVoucherCustomRepository.procGetBankBalance(UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(), documentId, docDate, bankPoid);
    }

    @Override
    public String getBeneficiaryName(Long beneficiaryId, String documentId) {
        return bankDebitVoucherCustomRepository.procGetBeneficiaryName(UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(), documentId, beneficiaryId);
    }

    @Override
    public void validatePayGLAndBeneficiary(PayGLValidationRequest req) {
        // re-use your internal runPreSaveProcedures.procGlBankPayGlBenVal call, but as an API
        bankDebitVoucherCustomRepository.procGlBankPayGlBenVal(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                "400-111", // doc id constant from SRS
                req.getTransactionPoid(),
                req.getPayingType(),
                req.getRefType(),
                req.getPayGlPoid(),
                req.getPayingTo(),
                req.getBankPoid()
        );
    }

    @Transactional
    @Override
    public void revertReconciliation(Long transactionPoid, String comments) {
        GlBankDebitHdr header = headerRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));
        bankPaymentVoucherSpRepository.revertReconciliation(
                header.getGroupPoid(),
                header.getCompanyPoid(),
                UserContext.getUserPoid(),
                header.getDocRef(),
                String.valueOf(transactionPoid),
                "Y",comments
        );
    }


    private ReconcileResultDto fetchReconDate(
            String docId,
            Long docKeyPoid) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_DEBIT_PAYMENT_RECON_DATE");

        // Register IN parameters
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);

        // Register OUT cursor
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        // Set input values
        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_DOC_KEY_POID", docKeyPoid);

        // Execute SP
        query.execute();

        Object cursor = query.getOutputParameterValue("OUTDATA");

        return mapCursorToDto(cursor);
    }

    private ReconcileResultDto mapCursorToDto(Object cursor) {

        try {
            ResultSet rs = (ResultSet) cursor;

            if (rs.next()) {
                return new ReconcileResultDto(
                        rs.getString("RECONCILE_DATE"),
                        rs.getString("HOLD")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading cursor", e);
        }

        return new ReconcileResultDto(null, null);
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        GlBankDebitHdr header = headerRepository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));

        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-111");
        JasperReport mainReport;
        String payingType = header.getPayingType();
        if (payingType != null && payingType.contains("3")) {
            mainReport = printService.load("Finance/BankPayments/BankDebitVouherCreditCard.jrxml");
        } else {
            mainReport = printService.load("Finance/BankPayments/BankDebitVoucher.jrxml");
        }
        params.put("BANK_DEBIT_VOUCHER_SUBREPORT_1", printService.load("Finance/BankPayments/BankDebitVoucher_subreport1.jrxml"));
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    /**
     * Post-save/post-update job cost updates (equivalent to BankDebitVoucherBean.DocumentAfterSave):
     * - For RefType = FDA JOBS → PROC_AP_PI_FDA_UPDATE_COST
     * - For RefType = FF JOBS  → PROC_AP_PI_FF_UPDATE_COST
     * - For RefType = MTA RFQ  → PROC_BANK_MTA_UPDATE
     */
    private void publishJobCostUpdateEvent(BankDebitVoucherResponse response, String oldRefType, String oldRef) {
        if (StringUtils.isBlank(response.getRefType()) && StringUtils.isBlank(oldRefType)) return;

        String ref = null;
        try {
            if ("FDA JOBS".equalsIgnoreCase(response.getRefType())) {
                ref = response.getFdaRef() != null ? String.valueOf(response.getFdaRef()) : null;
                applyProcResult(response, bankPaymentVoucherSpRepository.updateFdaCost(response.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(), ref, response.getTransactionPoid()));
            } else if ("FF JOBS".equalsIgnoreCase(response.getRefType())) {
                ref = response.getFfRef();
                applyProcResult(response, bankPaymentVoucherSpRepository.updateFfCost(response.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(), ref, response.getTransactionPoid()));
            } else if ("MTA RFQ".equalsIgnoreCase(response.getRefType())) {
                ref = response.getSalesQtnRef() != null ? String.valueOf(response.getSalesQtnRef()) : null;
                String procResult = bankPaymentVoucherSpRepository.updateMtaCost(response.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(), response.getTransactionPoid(), ref);
                // Legacy surfaces MTA messages only for errors/info, success is silent
                if (procResult != null && (procResult.contains("ERROR") || procResult.contains("Info:"))) {
                    applyProcResult(response, procResult);
                }
            }
        } catch (Exception e) {
            log.error("Job cost update failed for refType {} ref {}: {}", response.getRefType(), ref, e.getMessage(), e);
            addWarning(response, "Some error occurred while job cost update - " + e.getMessage());
        }
    }

    /**
     * Routes the proc result back to the caller without the SUCCESS/ERROR prefix,
     * alongside the response (Debit Note pattern): errors as warnings,
     * success/info text as informational messages.
     */
    private void applyProcResult(BankDebitVoucherResponse response, String procResult) {
        if (StringUtils.isBlank(procResult)) return;
        String message = procResult.contains(":") ? procResult.substring(procResult.indexOf(':') + 1).trim() : procResult.trim();
        if (procResult.trim().toUpperCase().startsWith("ERROR")) {
            addWarning(response, message);
        } else {
            addInfoMessage(response, message);
        }
    }

    private void addWarning(BankDebitVoucherResponse response, String warning) {
        if (response.getWarnings() == null) response.setWarnings(new ArrayList<>());
        response.getWarnings().add(warning);
    }

    private void addInfoMessage(BankDebitVoucherResponse response, String message) {
        if (response.getInfoMessages() == null) response.setInfoMessages(new ArrayList<>());
        response.getInfoMessages().add(message);
    }

    /**
     * Validates trigger rules before INSERT on GL_BANK_DEBIT_HDR (mirrors GL_BANK_DEBIT_HDR_GTTRG):
     * - Financial year check for transaction date.
     */
    private void validateInputTaxVariance(BankDebitVoucherRequest request) {
        String inputTaxLimitValue = globalParameterService.getParameterValue("INPUT_TAX_VARIANCE_LIMIT", "GROUP", "1", "0");
        BigDecimal inputTaxLimit;
        try {
            inputTaxLimit = new BigDecimal(inputTaxLimitValue);
        } catch (NumberFormatException ex) {
            throw new ValidationException("INPUT_TAX_VARIANCE_LIMIT parameter is not configured correctly.");
        }

        BigDecimal hundred = BigDecimal.valueOf(100);

        if (request.getPaymentGlDetails() != null) {
            for (int i = 0; i < request.getPaymentGlDetails().size(); i++) {
                PaymentGlDetails item = request.getPaymentGlDetails().get(i);
                if (item.getTaxPercentage() == null) continue;

                BigDecimal baseAmount = item.getDrAmt() != null && item.getDrAmt().compareTo(BigDecimal.ZERO) > 0
                        ? item.getDrAmt()
                        : item.getCrAmt() != null ? item.getCrAmt() : BigDecimal.ZERO;
                baseAmount = baseAmount.setScale(3, java.math.RoundingMode.HALF_UP);

                BigDecimal enteredTax = item.getTaxAmount() != null
                        ? item.getTaxAmount().setScale(3, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(3, java.math.RoundingMode.HALF_UP);

                if (baseAmount.compareTo(BigDecimal.ZERO) == 0 && enteredTax.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal expectedTax = baseAmount.multiply(item.getTaxPercentage())
                        .divide(hundred, 3, java.math.RoundingMode.HALF_UP);
                BigDecimal difference = enteredTax.subtract(expectedTax).abs();

                if (difference.compareTo(inputTaxLimit) > 0) {
                    throw new ValidationException("WARNING : Input tax difference (" + difference + "/-) should be within " + inputTaxLimit + "/- Please note the row number " + (i + 1));
                }
            }
        }

        if (request.getChargeDetails() != null) {
            for (int i = 0; i < request.getChargeDetails().size(); i++) {
                ChargeDetailDto item = request.getChargeDetails().get(i);
                if (item.getChargeBaseAmount() == null || item.getTaxPercentage() == null) continue;

                BigDecimal baseAmount = item.getChargeBaseAmount().setScale(3, java.math.RoundingMode.HALF_UP);
                BigDecimal enteredTax = item.getTaxAmount() != null
                        ? item.getTaxAmount().setScale(3, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(3, java.math.RoundingMode.HALF_UP);

                if (baseAmount.compareTo(BigDecimal.ZERO) == 0 && enteredTax.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal expectedTax = baseAmount.multiply(item.getTaxPercentage())
                        .divide(hundred, 3, java.math.RoundingMode.HALF_UP);
                BigDecimal difference = enteredTax.subtract(expectedTax).abs();

                if (difference.compareTo(inputTaxLimit) > 0) {
                    throw new ValidationException("WARNING : Input tax difference (" + difference + "/-) should be within " + inputTaxLimit + "/- Please note the row number " + (i + 1));
                }
            }
        }
    }

    private void validateTriggerRulesForCreate(GlBankDebitHdr header) {
        if (header.getCompanyPoid() == null || header.getTransactionDate() == null) {
            return;
        }
        LocalDate txDate = header.getTransactionDate().toLocalDate();
        if (!glChequeCashConvertRepository.isFinancialYearValid(header.getCompanyPoid(), txDate)) {
            throw new ValidationException("Changes allowed only within current Financial Period. Please set transaction date within the open financial period.");
        }
    }

    /**
     * Validates trigger rules before UPDATE on GL_BANK_DEBIT_HDR (mirrors GL_BANK_DEBIT_HDR_GTTRG):
     * - Financial year check for new transaction date.
     * - If date is changing: transaction period check for new date.
     * - Transaction period check for old transaction date (current period for edit).
     */
    private void validateTriggerRulesForUpdate(GlBankDebitHdr oldHeader, GlBankDebitHdr newHeader) {
        if (newHeader.getCompanyPoid() == null) {
            return;
        }
        Long companyPoid = newHeader.getCompanyPoid();
        LocalDate oldDate = oldHeader.getTransactionDate() != null ? oldHeader.getTransactionDate().toLocalDate() : null;
        LocalDate newDate = newHeader.getTransactionDate() != null ? newHeader.getTransactionDate().toLocalDate() : oldDate;

        if (newDate != null && !glChequeCashConvertRepository.isFinancialYearValid(companyPoid, newDate)) {
            throw new ValidationException("Changes allowed only within current Financial Period. Please set transaction date within the open financial period.");
        }

        if (oldDate != null && !oldDate.equals(newDate)) {
            if (newDate != null && !glChequeCashConvertRepository.isTransactionYearValid(companyPoid, newDate)) {
                throw new ValidationException("Transaction date cannot be updated. The new date is outside the current transaction period.");
            }
        }

        if (oldDate != null && !glChequeCashConvertRepository.isTransactionYearValid(companyPoid, oldDate)) {
            throw new ValidationException("Changes allowed only within current Transaction Period. This record's transaction date is outside the editable period.");
        }
    }

    @Override
    public byte[] printBillwise(Long transactionPoid) throws Exception {

        Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
        JasperReport mainReport = printService.load("Finance/BankPayments/BankDebitVoucher_Billwise.jrxml");

        // Add required subreport parameters
        params.put("SUB_HEADER", printService.load("Templates/DocHeaderSubReport.jrxml"));
        params.put("SUB_FOOTER", printService.load("Templates/DocFooterSubReport.jrxml"));

        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}
