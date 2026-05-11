package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.BankFileBatchResult;
import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.dto.TelexFileGenerateRequestDto;
import com.asg.finance.dto.TelexFileGenerateResponseDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.entity.GlBankFileDtl;
import com.asg.finance.entity.GlBankFileHdr;
import com.asg.finance.repository.GlBankDebitHdrRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.repository.TelexFileGenerateProcRepository;
import com.asg.finance.repository.GlBankFileDtlRepository;
import com.asg.finance.repository.GlBankFileHdrRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BankFileBatchService;
import com.asg.finance.service.TelexFileGenerateService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelexFileGenerateServiceImpl implements TelexFileGenerateService {

    private final GlBankFileHdrRepository hdrRepository;
    private final GlBankFileDtlRepository dtlRepository;
    private final TelexFileGenerateProcRepository procRepository;
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final GlBankDebitHdrRepository glBankDebitHdrRepository;
    private final BankFileBatchService bankFileBatchService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final EntityManager entityManager;
    private final ApplicationContext applicationContext;

    @Override
    public TelexFileGenerateResponseDto createTelexFile(TelexFileGenerateRequestDto request) {
        try {
            validateSelectedDetails(request.getDetails());

            TelexFileGenerateServiceImpl self = applicationContext.getBean(TelexFileGenerateServiceImpl.class);
            Long transactionPoid = self.saveTelexFileInternal(request);

            Long userId = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
            BankFileBatchResult batchResult = self.processFileAsync(transactionPoid, userId);

            TelexFileGenerateResponseDto response = getTelexFileById(transactionPoid);
            enrichWithBatchMessages(response, batchResult);
            return response;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException(extractTriggerErrorMessage(e));
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Long saveTelexFileInternal(TelexFileGenerateRequestDto request) {
        GlBankFileHdr hdr = new GlBankFileHdr();
        hdr.setTransactionDate(request.getTransactionDate());
        hdr.setGroupPoid(UserContext.getGroupPoid());
        hdr.setCompanyPoid(UserContext.getCompanyPoid());
        hdr.setBankPoid(request.getBankPoid());
        hdr.setBankList(request.getBankList() != null ? request.getBankList() : "Y");
        hdr.setLongNarration(request.getRemarks());
        hdr.setOnlyApproval(request.isApprovalOnly() ? "Y" : "N");
        hdr.setTtSuppressBalanceCheck(request.isSuppressBalanceCheck() ? "Y" : "N");
        hdr.setDeleted("N");

        GlBankFileHdr savedHdr = hdrRepository.saveAndFlush(hdr);
        entityManager.flush();
        entityManager.refresh(savedHdr);

        // Log header creation first
        String key = savedHdr.getTransactionPoid().toString();
        String docId = UserContext.getDocumentId();
        String docRef = savedHdr.getDocRef();
        loggingService.createLogSummaryEntry(
            docId,
            key,
            String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), docRef)
        );

        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            List<GlBankFileDtl> details = new ArrayList<>();

            for (int i = 0; i < request.getDetails().size(); i++) {
                TelexFileDtlDto dto = request.getDetails().get(i);
                if ("ISDELETED".equalsIgnoreCase(dto.getActionType())) {
                    continue;
                }
                GlBankFileDtl detail = convertToDetailEntity(dto, savedHdr.getTransactionPoid());
                detail.setDetRowId((long) (i + 1));
                details.add(detail);
            }
            dtlRepository.saveAll(details);
            dtlRepository.flush();

            details.forEach(detail -> {
                String logDetail = String.format("Row Created on Telex File Detail with detRowId: %s", detail.getDetRowId());
                loggingService.createLogSummaryEntry(docId, key, logDetail);
            });
        }

        return savedHdr.getTransactionPoid();
    }

    @Override
    public TelexFileGenerateResponseDto updateTelexFile(Long transactionPoid, TelexFileGenerateRequestDto request) {
        try {
            validateSelectedDetails(request.getDetails());

            TelexFileGenerateServiceImpl self = applicationContext.getBean(TelexFileGenerateServiceImpl.class);
            self.updateTelexFileInternal(transactionPoid, request);

            Long userId = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
            BankFileBatchResult batchResult = self.processFileAsync(transactionPoid, userId);

            TelexFileGenerateResponseDto response = getTelexFileById(transactionPoid);
            enrichWithBatchMessages(response, batchResult);
            return response;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException(extractTriggerErrorMessage(e));
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateTelexFileInternal(Long transactionPoid, TelexFileGenerateRequestDto request) {
        GlBankFileHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", transactionPoid));

        GlBankFileHdr oldHdr = new GlBankFileHdr();
        BeanUtils.copyProperties(hdr, oldHdr);
        hdr.setTransactionDate(request.getTransactionDate());
        hdr.setBankPoid(request.getBankPoid());
        hdr.setBankList(request.getBankList() != null ? request.getBankList() : "Y");
        hdr.setLongNarration(request.getRemarks());
        hdr.setOnlyApproval(request.isApprovalOnly() ? "Y" : "N");
        hdr.setTtSuppressBalanceCheck(request.isSuppressBalanceCheck() ? "Y" : "N");

        hdrRepository.saveAndFlush(hdr);

        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            processDetails(transactionPoid, request.getDetails());
        }

        String key = transactionPoid.toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldHdr, hdr, GlBankFileHdr.class,
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
    }

    @Override
    public TelexFileGenerateResponseDto getTelexFileById(Long transactionPoid) {
        GlBankFileHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", transactionPoid));

        List<GlBankFileDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);

        return convertToResponseDto(hdr, details);
    }

    @Override
    @Transactional
    public void softDeleteTelexFile(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        try {
            GlBankFileHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", transactionPoid));

            documentDeleteService.deleteDocument(
                    transactionPoid,
                    "GL_BANK_FILE_HDR",
                    "TRANSACTION_POID",
                    deleteReasonDto,
                    hdr.getTransactionDate()
            );

            hdr.setDeleted("Y");
            hdrRepository.saveAndFlush(hdr);
        } catch (Exception e) {
            String errorMessage = extractTriggerErrorMessage(e);
            throw new ValidationException(errorMessage);
        }
    }

    @Override
    public Map<String, Object> listTelexFiles(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "DOC_REF", "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public List<TelexFileDtlDto> loadTelexTransferData(String bankList) {
        return procRepository.loadTelexTransferData(bankList);
    }

    @Override
    @Transactional
    public String regenerateTelexFile(Long telexTransactionPoid, Long debitVoucherPoid) {
        glBankDebitHdrRepository.findByTransactionPoid(debitVoucherPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", debitVoucherPoid));

        Long userId = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
        String result = procRepository.regenerateTelexFile(UserContext.getGroupPoid(), UserContext.getCompanyPoid(),
                userId, debitVoucherPoid);

        // Log against telex transaction POID (matching legacy behavior)
        String docId = UserContext.getDocumentId();
        String key = telexTransactionPoid.toString();
        loggingService.createLogSummaryEntry(docId, key, result);

        return result;
    }


    @Override
    @Transactional
    public String generateBankFileButton(Long transactionPoid) {
        Long userId = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
        return bankFileBatchService.createBankFileBatch(transactionPoid, userId).getResult();
    }
    
    @Override
    public String checkBankBalance(Long transactionPoid) {
        return procRepository.checkOverdraft(transactionPoid);
    }

    private GlBankFileDtl convertToDetailEntity(TelexFileDtlDto dto, Long transactionPoid) {
        return GlBankFileDtl.builder()
                .transactionPoid(transactionPoid)
                .debitTransactionPoid(dto.getDebitTransactionPoid())
                .debitTransactionDate(dto.getDebitTransactionDate())
                .debitCompanyPoid(dto.getDebitCompanyPoid())
                .debitDocRef(dto.getDebitDocRef())
                .debitPayingToName(dto.getDebitPayingToName())
                .debitPayingType(dto.getDebitPayingType())
                .debitLongNarration(dto.getDebitLongNarration())
                .debitTtDate(dto.getDebitTtDate())
                .debitCurrencyCode(dto.getDebitCurrencyCode())
                .debitCurrencyRate(dto.getDebitCurrencyRate())
                .debitCurrencyAmt(dto.getDebitCurrencyAmt())
                .debitAmount(dto.getDebitAmount())
                .deleted(dto.getDeleted() != null ? dto.getDeleted() : "N")
                .selected(dto.getSelected() != null ? dto.getSelected() : "N")
                .drilldownLinkInfo(dto.getDrilldownLinkInfo())
                .debitTtChargeType(dto.getDebitTtChargeType())
                .build();
    }

    private TelexFileGenerateResponseDto convertToResponseDto(GlBankFileHdr hdr, List<GlBankFileDtl> details) {
        return TelexFileGenerateResponseDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .transactionDate(hdr.getTransactionDate())
                .groupPoid(hdr.getGroupPoid())
                .groupDet(lovService.getDetailsByPoidAndLovName(hdr.getGroupPoid(), "GROUP"))
                .companyPoid(hdr.getCompanyPoid())
                .companyDet(lovService.getDetailsByPoidAndLovName(hdr.getCompanyPoid(), "COMPANY"))
                .docRef(hdr.getDocRef())
                .bankPoid(hdr.getBankPoid())
                .bankDet(lovService.getDetailsByCodeAndLovName(hdr.getBankList(), "BANK_PAYMENT_FILE"))
                .bankList(hdr.getBankList())
                .remarks(hdr.getLongNarration())
                .approvalOnly("Y".equals(hdr.getOnlyApproval()))
                .suppressBalanceCheck("Y".equals(hdr.getTtSuppressBalanceCheck()))
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .details(details.stream().map(this::convertToDetailDto).collect(Collectors.toList()))
                .build();
    }

    private TelexFileDtlDto convertToDetailDto(GlBankFileDtl entity) {
        return TelexFileDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .debitTransactionPoid(entity.getDebitTransactionPoid())
                .debitTransactionDate(entity.getDebitTransactionDate())
                .debitCompanyPoid(entity.getDebitCompanyPoid())
                .debitCompanyDet(lovService.getDetailsByPoidAndLovName(entity.getDebitCompanyPoid(), "COMPANY"))
                .debitDocRef(entity.getDebitDocRef())
                .debitPayingToName(entity.getDebitPayingToName())
                .debitPayingType(entity.getDebitPayingType())
                .debitLongNarration(entity.getDebitLongNarration())
                .debitTtDate(entity.getDebitTtDate())
                .debitCurrencyCode(entity.getDebitCurrencyCode())
                .debitCurrencyRate(entity.getDebitCurrencyRate())
                .debitCurrencyAmt(entity.getDebitCurrencyAmt())
                .debitAmount(entity.getDebitAmount())
                .deleted(entity.getDeleted())
                .selected(entity.getSelected())
                .drilldownLinkInfo(entity.getDrilldownLinkInfo())
                .debitTtChargeType(entity.getDebitTtChargeType())
                .build();
    }

    private void processDetails(Long transactionPoid, List<TelexFileDtlDto> details) {
        String docId = UserContext.getDocumentId();
        String key = transactionPoid.toString();
        
        List<GlBankFileDtl> toSave = new ArrayList<>();
        List<GlBankFileDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlBankFileDtl>> logRequests = new ArrayList<>();
        
        Long maxDetRowId = dtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        Long nextDetRowId = (maxDetRowId == null) ? 1L : maxDetRowId + 1;
        
        for (TelexFileDtlDto dto : details) {
            String actionType = dto.getActionType() != null ? dto.getActionType().toUpperCase() : "NOCHANGES";
            
            switch (actionType) {
                case "ISCREATED":
                    GlBankFileDtl newDetail = convertToDetailEntity(dto, transactionPoid);
                    newDetail.setDetRowId(nextDetRowId++);
                    toSave.add(newDetail);
                    break;
                    
                case "ISUPDATED":
                    GlBankFileDtl existing = dtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Detail", "detRowId", dto.getDetRowId()));
                    
                    GlBankFileDtl oldDetail = new GlBankFileDtl();
                    BeanUtils.copyProperties(existing, oldDetail);
                    
                    existing.setDebitTransactionPoid(dto.getDebitTransactionPoid());
                    existing.setDebitTransactionDate(dto.getDebitTransactionDate());
                    existing.setDebitCompanyPoid(dto.getDebitCompanyPoid());
                    existing.setDebitDocRef(dto.getDebitDocRef());
                    existing.setDebitPayingToName(dto.getDebitPayingToName());
                    existing.setDebitPayingType(dto.getDebitPayingType());
                    existing.setDebitLongNarration(dto.getDebitLongNarration());
                    existing.setDebitTtDate(dto.getDebitTtDate());
                    existing.setDebitCurrencyCode(dto.getDebitCurrencyCode());
                    existing.setDebitCurrencyRate(dto.getDebitCurrencyRate());
                    existing.setDebitCurrencyAmt(dto.getDebitCurrencyAmt());
                    existing.setDebitAmount(dto.getDebitAmount());
                    existing.setSelected(dto.getSelected());
                    existing.setDebitTtChargeType(dto.getDebitTtChargeType());
                    toUpdate.add(existing);
                    
                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldDetail, existing, GlBankFileDtl.class, docId, key, logDetail));
                    break;
                    
                case "ISDELETED":
                    toDelete.add(dto.getDetRowId());
                    loggingService.logDelete(dto, docId, key);
                    break;
            }
        }
        
        if (!toSave.isEmpty()) {
            dtlRepository.saveAll(toSave);
            toSave.forEach(detail -> {
                String logDetail = String.format("Row Created on Telex File Detail with detRowId: %s", detail.getDetRowId());
                loggingService.createLogSummaryEntry(docId, key, logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            dtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            toDelete.forEach(detRowId -> dtlRepository.deleteByTransactionPoidAndDetRowId(transactionPoid, detRowId));
        }
    }

    private void validateIntermediaryBankDetails(Long debitTransactionPoid, String docRef, String currencyCode) {
        // Only validate for non-BHD currencies
        if ("BHD".equals(currencyCode)) {
            return;
        }
        Map<String, String> beneficiaryDetails = procRepository.getBeneficiaryDetails(debitTransactionPoid, docRef);
        String beneficiaryCountry = beneficiaryDetails.get("BENEFICIARY_COUNTRY");
        String intermediaryCountryPoid = beneficiaryDetails.get("INTERMEDIARY_COUNTRY_POID");
        String intermediaryAcct = beneficiaryDetails.get("INTERMEDIARY_ACCT");
        String intermediaryBank = beneficiaryDetails.get("INTERMEDIARY_BANK");

        // Rule 1: BENEFICIARY_COUNTRY must be defined
        if (beneficiaryCountry == null || beneficiaryCountry.equals("0") || beneficiaryCountry.isEmpty()) {
            throw new ValidationException("Validation failed : intermediary bank/country/account/bank name not defined in master...." + docRef);
        }

        // Rule 2: If INTERMEDIARY_ACCT exists, then INTERMEDIARY_COUNTRY_POID and INTERMEDIARY_BANK must exist
        if (intermediaryAcct != null && !intermediaryAcct.equals("XX") && !intermediaryAcct.isEmpty() && (intermediaryCountryPoid == null || intermediaryCountryPoid.equals("0") ||
                intermediaryBank == null || intermediaryBank.equals("XX") || intermediaryBank.isEmpty())) {
                throw new ValidationException("Validation failed : intermediary bank/country/account/bank name not defined in master...." + docRef);
            }


        // Rule 3: If INTERMEDIARY_BANK exists, then INTERMEDIARY_COUNTRY_POID and INTERMEDIARY_ACCT must exist
        if (intermediaryBank != null && !intermediaryBank.equals("XX") && !intermediaryBank.isEmpty() && (intermediaryCountryPoid == null || intermediaryCountryPoid.equals("0") ||
                intermediaryAcct == null || intermediaryAcct.equals("XX") || intermediaryAcct.isEmpty())) {
                throw new ValidationException("Validation failed : intermediary bank/country/account/bank name not defined in master...." + docRef);
            }

    }

    private String extractTriggerErrorMessage(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.contains("Changes allowed only within current Financial Period")) {
            return "Changes allowed only within current Financial Period";
        } else if (errorMessage != null && errorMessage.contains("Transaction date can not update")) {
            return "Transaction date cannot be updated";
        } else if (errorMessage != null && errorMessage.contains("Changes allowed only within current Transaction Period")) {
            return "Changes allowed only within current Transaction Period";
        }
        return errorMessage != null ? errorMessage : "Database operation failed";
    }

    public BankFileBatchResult processFileAsync(Long transactionPoid, Long userId) {
        try {
            return bankFileBatchService.createBankFileBatch(transactionPoid, userId);
        } catch (Exception e) {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> entry = new HashMap<>();
            entry.put("message", e.getMessage());
            entry.put("status", "ERROR");
            messages.add(entry);
            return new BankFileBatchResult("ERROR : " + e.getMessage(), messages);
        }
    }

    private void validateSelectedDetails(List<TelexFileDtlDto> details) {
        if (details == null || details.isEmpty()) return;
        for (TelexFileDtlDto dto : details) {
            if ("Y".equalsIgnoreCase(dto.getSelected()) && dto.getDebitTransactionPoid() != null) {
                validateIntermediaryBankDetails(dto.getDebitTransactionPoid(), dto.getDebitDocRef(), dto.getDebitCurrencyCode());
            }
        }
    }

    private void enrichWithBatchMessages(TelexFileGenerateResponseDto response, BankFileBatchResult batchResult) {
        if (batchResult == null || batchResult.getMessages() == null || batchResult.getMessages().isEmpty()) return;
        List<String> warnings = new ArrayList<>();
        List<String> infos = new ArrayList<>();
        for (Map<String, String> entry : batchResult.getMessages()) {
            String status = entry.get("status");
            String message = entry.get("message");
            if (message == null || message.isBlank()) continue;
            if ("WARNING".equals(status) || "ERROR".equals(status) || "COMPLETED_ERROR".equals(status)) {
                warnings.add(message);
            } else {
                infos.add(message);
            }
        }
        response.setWarnings(warnings.isEmpty() ? null : warnings);
        response.setInfos(infos.isEmpty() ? null : infos);
    }
}
