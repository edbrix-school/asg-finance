package com.asg.finance.service;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.dto.response.GlobalTermsResponseDto;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.client.GlobalTermsServiceClient;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.repository.*;
import com.asg.finance.entity.key.GlBankDebitDtlGlId;
import com.asg.finance.entity.key.GlBankDebitChargeDtlId;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.validator.BankDebitVoucherValidator;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public BankDebitVoucherResponse createBankDebitVoucher(BankDebitVoucherRequest request, String documentId) {

        validator.validate(request, true);

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

        runPreSaveProcedures(null);

        GlBankDebitHdr header = new GlBankDebitHdr();
        mapRequestToEntity(request, header);
        populateCreateAudit(header);

        GlBankDebitHdr savedHeader = headerRepository.save(header);
        entityManager.flush();

        if (request.getFfRef() != null) {
            bankPaymentVoucherSpRepository.updateFfCost(
                    UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(),
                    UserContext.getUserPoid(),
                    String.valueOf(header.getFfRef()),
                    savedHeader.getTransactionPoid());
        }
        if (request.getFdaRef() != null) {
            bankPaymentVoucherSpRepository.updateFdaCost(
                    UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(),
                    UserContext.getUserPoid(),
                    String.valueOf(header.getFdaRef()),
                    savedHeader.getTransactionPoid());
        }


        persistChildCollections(request, savedHeader.getTransactionPoid(), true,documentId);

        return mapEntityToResponse(savedHeader);
    }

    @Override
    public BankDebitVoucherResponse getBankDebitVoucher(Long transactionPoid, String documentId) {
        GlBankDebitHdr header = headerRepository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));
        ReconcileResultDto reconDto = fetchReconDate("400-111", transactionPoid);

        BankDebitVoucherResponse response = mapEntityToResponse(header);

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

        return response;
    }

    @Override
    public BankDebitVoucherResponse updateBankDebitVoucher(Long transactionPoid, BankDebitVoucherRequest request,String documentId) {

        GlBankDebitHdr header = headerRepository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));

        validator.validate(request, false);

        // Step 3: Conditional field existence checks
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

        if (headerRepository.existsByDocRefIgnoreCaseAndTransactionPoidNot(request.getDocRef(), transactionPoid)) {
            throw new ResourceAlreadyExistsException("Doc Ref", request.getDocRef());
        }

        runPreSaveProcedures(header);

        mapRequestToEntity(request, header);

        validator.validateVoucherStatusInNewTransaction(header);

        populateUpdateAudit(header);

        header = headerRepository.save(header);
        entityManager.flush();
        entityManager.refresh(header);

        if (request.getFfRef() != null) {
            bankPaymentVoucherSpRepository.updateFfCost(
                    UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(),
                    UserContext.getUserPoid(),
                    String.valueOf(header.getFfRef()),
                    header.getTransactionPoid());
        }
        if (request.getFdaRef() != null) {
            bankPaymentVoucherSpRepository.updateFdaCost(
                    UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(),
                    UserContext.getUserPoid(),
                    String.valueOf(header.getFdaRef()),
                    header.getTransactionPoid());
        }


        persistChildCollections(request, header.getTransactionPoid(), false,documentId);

        return mapEntityToResponse(header);
    }

    @Override
    public void softDeleteBankDebitVoucher(Long transactionPoid) {
        GlBankDebitHdr header = headerRepository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Debit Voucher", "transactionPoid", transactionPoid));

        // Validate voucher can be deleted
        validator.validateVoucherStatusInNewTransaction(header);

        header.setDeleted("Y");
        populateUpdateAudit(header);
        headerRepository.save(header);
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

    private void upsertPaymentGlDetails(Long transactionPoid,
                                        List<PaymentGlDetails> details,
                                        String documentId) {

        if (details == null || details.isEmpty()) return;

        List<BillwiseBreakupRequestDto> billwiseRequestDtoList = new ArrayList<>();
        List<CostCenterBreakupRequestDto> costCenterRequestDtoList = new ArrayList<>();

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
                case "NOCHANGES" -> {}
                case "ISDELETED" -> {
                    if (dtl.getDetRowId() != null) {
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
                    populateCreateAudit(entity);
                    mapToPaymentGlEntity(dtl, entity);
                    paymentGlRepository.save(entity);

                    if (dtl.getBreakupList() != null && !dtl.getBreakupList().isEmpty()) {
                        List<BillwiseBreakupRequestDto> bwList = dtl.getBreakupList().stream().map(p -> {
                            BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();
                            dto.setGroupPoid(UserContext.getGroupPoid());
                            dto.setCompanyPoid(UserContext.getCompanyPoid());
                            dto.setDocId(documentId);
                            dto.setMainDetRowId(detRowId);
                            dto.setTransactionPoid(transactionPoid);
                            dto.setBillDetRowId(p.getBillDetRowId());
                            dto.setBillRefType(p.getBillRefType());
                            dto.setBillRef(p.getBillRef());
                            dto.setBillDueDate(p.getBillDueDate());
                            if("CR".equalsIgnoreCase(p.getType())) {
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
                            dto.setCostDetRowId(dto.getCostDetRowId());
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
                    populateUpdateAudit(entity);
                    mapToPaymentGlEntity(dtl, entity);
                    paymentGlRepository.save(entity);

                    if (dtl.getBreakupList() != null && !dtl.getBreakupList().isEmpty()) {
                        List<BillwiseBreakupRequestDto> bwList = dtl.getBreakupList().stream().map(p -> {
                            BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();
                            dto.setGroupPoid(UserContext.getGroupPoid());
                            dto.setCompanyPoid(UserContext.getCompanyPoid());
                            dto.setDocId(documentId);
                            dto.setTransactionPoid(transactionPoid);
                            dto.setMainDetRowId(dtl.getDetRowId());
                            dto.setBillDetRowId(p.getBillDetRowId());
                            dto.setBillRefType(p.getBillRefType());
                            dto.setBillRef(p.getBillRef());
                            dto.setBillDueDate(p.getBillDueDate());
                            if("CR".equalsIgnoreCase(p.getType())) {
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
                            dto.setCostDetRowId(dto.getCostDetRowId());
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

        if(CollectionUtils.isNotEmpty(costCenterRequestDtoList)) {
            costCenterBreakupService.updateCostCenterBreakups(costCenterRequestDtoList, UserContext.getUserPoid());
        }
        if(CollectionUtils.isNotEmpty(billwiseRequestDtoList)) {
            billwiseBreakupService.updateBillwiseBreakups(billwiseRequestDtoList, UserContext.getUserPoid());
        }
    }

    private void upsertChargeDetails(Long transactionPoid, List<ChargeDetailDto> details) {

        if (details == null || details.isEmpty()) return;

        for (ChargeDetailDto dto : details) {

            String rawAction = dto.getActionType();
            String action = (rawAction == null || rawAction.trim().isEmpty())
                    ? "ISCREATED"
                    : rawAction.trim().toUpperCase();

            action = switch (action) {
                case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                case "ISDELETED", "DELETED" -> "ISDELETED";
                default -> "NOCHANGES";
            };

            switch (action) {
                case "NOCHANGES" -> {}
                case "ISDELETED" -> {
                    if (dto.getDetRowId() != null) {
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
                    populateCreateAudit(entity);
                    mapToChargeEntity(dto, entity);
                    chargeDetailRepository.save(entity);
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
                    populateUpdateAudit(entity);
                    mapToChargeEntity(dto, entity);
                    chargeDetailRepository.save(entity);
                }
            }
        }
    }

    private void cleanupRemovedRows(Long transactionPoid, BankDebitVoucherRequest request) {
        Set<Long> paymentIds = ids(request.getPaymentGlDetails());
        Set<Long> chargeIds = ids(request.getChargeDetails());

        if (paymentIds != null) {
            paymentGlRepository.deleteByTransactionPoidAndDetRowIdNotIn(transactionPoid, paymentIds);
        }
        if (chargeIds != null) {
            chargeDetailRepository.deleteByTransactionPoidAndDetRowIdNotIn(transactionPoid, chargeIds);
        }
    }

    private void runPreSaveProcedures(GlBankDebitHdr existingHeader) {
        if (existingHeader != null) {
            bankDebitVoucherCustomRepository.procGlJobRelOldValues(
                    UserContext.getGroupPoid(),
                    UserContext.getUserPoid(),
                    UserContext.getCompanyPoid(),
                    "400-111",
                    existingHeader.getTransactionPoid()
            );
        }
    }

    private void populateCreateAudit(GlBankDebitHdr entity) {
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setDeleted("N");
    }

    private void populateUpdateAudit(GlBankDebitHdr entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void populateCreateAudit(GlBankDebitDtlGl entity) {
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void populateUpdateAudit(GlBankDebitDtlGl entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void populateCreateAudit(GlBankDebitChargeDtl entity) {
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void populateUpdateAudit(GlBankDebitChargeDtl entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void populateCreateAudit(GlBankDebitItemDtl entity) {
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void populateUpdateAudit(GlBankDebitItemDtl entity) {
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

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
        entity.setFfRef(request.getFfRef());
        entity.setFdaRef(request.getFdaRef());
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
        entity.setDocRef(request.getDocRef());
        entity.setFileGenerated(request.getFileGenerated());
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
        response.setBankBalance(entity.getBankBalance());
        response.setAvailableBalance(entity.getAvailableBalance());
        response.setFileGenerated(entity.getFileGenerated());
        response.setFileName(entity.getFileName());
        response.setFileGeneratedDate(entity.getFileGeneratedDate());
        response.setFileGeneratedBy(entity.getFileGeneratedBy());
        response.setPayingTo(entity.getPayingTo());
        response.setFileUniqueId(entity.getFileUniqueId());
//        response.setConfidentialRemarks(entity.getConfidentialRemarks());


        // Load child entities
        response.setPaymentGlDetails(paymentGlRepository.findByIdTransactionPoid(entity.getTransactionPoid())
                .stream().map(this::mapPaymentGlEntityToDto).toList());
        response.setChargeDetails(chargeDetailRepository.findByIdTransactionPoid(entity.getTransactionPoid())
                .stream().map(this::mapChargeEntityToDto).toList());
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
        entity.setCompanyPoid(UserContext.getCompanyPoid());
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
            dto.setGlDet(lovService.getDetailsByPoidAndLovName(entity.getGlPoid(), "GL_MASTER_LEDGERS_BDV_DT"));
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
    public List<ChargeFFDto> loadFFCharges(Long ffRefPoid) {
        return bankDebitVoucherCustomRepository.procLoadFFCharges(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                ffRefPoid
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

    // ---------- utilities ----------
    @Override
    public BigDecimal getBankBalance(Long bankPoid,String documentId, Date docDate) {
        return bankDebitVoucherCustomRepository.procGetBankBalance(UserContext.getGroupPoid(), UserContext.getUserPoid(),  UserContext.getCompanyPoid(), documentId,docDate, bankPoid);
    }

    @Override
    public String getBeneficiaryName(Long beneficiaryId,String documentId) {
        return bankDebitVoucherCustomRepository.procGetBeneficiaryName(UserContext.getGroupPoid(),UserContext.getUserPoid(),  UserContext.getCompanyPoid(),documentId, beneficiaryId);
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
                "Y"
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
        JasperReport mainReport = null;
        if (header.getPayingType() != null) {
            String payingType = header.getPayingType();
            if (payingType.contains("3")) {
                mainReport = printService.load("Finance/BankPayments/BankDebitVouherCreditCard.jrxml");
            } else if(payingType.contains("4")){
                mainReport = printService.load("Finance/BankPayments/BankDebitVouherBankCharges.jrxml");
            }else {
                mainReport = printService.load("Finance/BankPayments/BankDebitVoucher.jrxml");
            }
        }
        params.put("BANK_DEBIT_VOUCHER_SUBREPORT_1", printService.load("Finance/BankPayments/BankDebitVoucher_subreport1.jrxml"));
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}