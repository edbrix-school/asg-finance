package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.dto.GlChequeCashConvertHdrDto;
import com.asg.finance.dto.GlChequeCashConvertInDtlDto;
import com.asg.finance.dto.GlChequeCashConvertOutDtlDto;
import com.asg.finance.dto.GlChequeConversionLoadResponseDto;
import com.asg.finance.entity.GlChequeCashConvertHdrEntity;
import com.asg.finance.entity.GlChequeCashConvertInDtlEntity;
import com.asg.finance.entity.GlChequeCashConvertOutDtlEntity;
import com.asg.finance.entity.key.GlChequeCashConvertInDtlKey;
import com.asg.finance.entity.key.GlChequeCashConvertOutDtlKey;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.finance.repository.GlChequeCashConvertHdrRepository;
import com.asg.finance.repository.GlChequeCashConvertInDtlRepository;
import com.asg.finance.repository.GlChequeCashConvertOutDtlRepository;
import com.asg.finance.repository.GlChequeCashConvertRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.GlChequeCashConvertService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.asg.common.lib.exception.ValidationException;


import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class GlChequeCashConvertServiceImpl implements GlChequeCashConvertService {

    private final GlChequeCashConvertHdrRepository glChequeCashConvertHdrRepository;
    private final GlChequeCashConvertInDtlRepository glChequeCashConvertInDtlRepository;
    private final GlChequeCashConvertOutDtlRepository glChequeCashConvertOutDtlRepository;
    private final GlChequeCashConvertRepository glChequeCashConvertRepository;
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final GlobalLogSummaryRepository globalLogSummaryRepository;

    @Override
    public GlChequeCashConvertHdrDto getGlChequeCashConvert(Long transactionPoid) {
        GlChequeCashConvertHdrEntity glChequeCashConvertHdrEntity = glChequeCashConvertHdrRepository.findByTransactionPoid(transactionPoid);
        if (glChequeCashConvertHdrEntity == null) {

            throw new ResourceNotFoundException("ChequeAndCashConvention", "transactionPoid", transactionPoid);

        }
        GlChequeCashConvertHdrDto glChequeCashConvertHdrDto = new GlChequeCashConvertHdrDto();
        glChequeCashConvertHdrDto.setTransactionPoid(glChequeCashConvertHdrEntity.getTransactionPoid());
        glChequeCashConvertHdrDto.setTransactionDate(glChequeCashConvertHdrEntity.getTransactionDate());
        glChequeCashConvertHdrDto.setGroupPoid(glChequeCashConvertHdrEntity.getGroupPoid());
        glChequeCashConvertHdrDto.setCompanyPoid(glChequeCashConvertHdrEntity.getCompanyPoid());
        glChequeCashConvertHdrDto.setDocRef(glChequeCashConvertHdrEntity.getDocRef());
        glChequeCashConvertHdrDto.setType(glChequeCashConvertHdrEntity.getType());
        glChequeCashConvertHdrDto.setPostingNarration(glChequeCashConvertHdrEntity.getPostingNarration());
        glChequeCashConvertHdrDto.setCash(glChequeCashConvertHdrEntity.getCash());
        glChequeCashConvertHdrDto.setRemarks(glChequeCashConvertHdrEntity.getRemarks());
        glChequeCashConvertHdrDto.setCreatedBy(glChequeCashConvertHdrEntity.getCreatedBy());
        glChequeCashConvertHdrDto.setCreatedDate(glChequeCashConvertHdrEntity.getCreatedDate());
        glChequeCashConvertHdrDto.setLastModifiedBy(glChequeCashConvertHdrEntity.getLastModifiedBy());
        glChequeCashConvertHdrDto.setLastModifiedDate(glChequeCashConvertHdrEntity.getLastModifiedDate());
        glChequeCashConvertHdrDto.setDeleted(glChequeCashConvertHdrEntity.getDeleted());
        glChequeCashConvertHdrDto.setChqAcNo(glChequeCashConvertHdrEntity.getChqAcNo());
        glChequeCashConvertHdrDto.setChqCardNo(glChequeCashConvertHdrEntity.getChqCardNo());
        glChequeCashConvertHdrDto.setRoundingAmt(glChequeCashConvertHdrEntity.getRoundingAmt());
        glChequeCashConvertHdrDto.setCompanyDet(lovService.getDetailsByPoidAndLovName(glChequeCashConvertHdrEntity.getCompanyPoid(), "COMPANY"));
        glChequeCashConvertHdrDto.setGroupDet(lovService.getDetailsByPoidAndLovName(glChequeCashConvertHdrEntity.getGroupPoid(), "GROUP"));
        glChequeCashConvertHdrDto.setInDtls(buildInDtls(glChequeCashConvertInDtlRepository.findByIdTransactionPoid(transactionPoid)));
        glChequeCashConvertHdrDto.setOutDtls(buildOutDtls(glChequeCashConvertOutDtlRepository.findByIdTransactionPoid(transactionPoid)));
        return glChequeCashConvertHdrDto;
    }


    private List<GlChequeCashConvertInDtlDto> buildInDtls(List<GlChequeCashConvertInDtlEntity> inDtls) {
        return inDtls.stream().map(inDtl -> {
            GlChequeCashConvertInDtlDto inDtlDto = new GlChequeCashConvertInDtlDto();
            inDtlDto.setTransactionPoid(inDtl.getId().getTransactionPoid());
            inDtlDto.setDetRowId(inDtl.getId().getDetRowId());
            inDtlDto.setBankPoid(inDtl.getBankPoid());
            inDtlDto.setChqAcName(inDtl.getChqAcName());
            inDtlDto.setChqAcNo(inDtl.getChqAcNo());
            inDtlDto.setChqCardNo(inDtl.getChqCardNo());
            inDtlDto.setChqDate(inDtl.getChqDate());
            inDtlDto.setAmount(inDtl.getAmount());
            inDtlDto.setRemarks(inDtl.getRemarks());
           /* inDtlDto.setCreatedBy(inDtl.getCreatedBy());
            inDtlDto.setCreatedDate(inDtl.getCreatedDate());
            inDtlDto.setLastModifiedBy(inDtl.getLastModifiedBy());
            inDtlDto.setLastModifiedDate(inDtl.getLastModifiedDate());*/
            inDtlDto.setVoucherType(inDtl.getVoucherType());
            inDtlDto.setChequeCompanyPoid(inDtl.getChequeCompanyPoid());
            inDtlDto.setPaymentMainPoid(inDtl.getPaymentMainPoid());
            inDtlDto.setLineType(inDtl.getLineType());
            inDtlDto.setPymtType(inDtl.getPymtType());
            inDtlDto.setTtBankPoid(inDtl.getTtBankPoid());
            inDtlDto.setTtRef(inDtl.getTtRef());
            inDtlDto.setCardPoid(inDtl.getCardPoid());
            inDtlDto.setCardType(inDtl.getCardType());
            inDtlDto.setCreditCardRef(inDtl.getCreditCardRef());
            inDtlDto.setBankDet(lovService.getDetailsByPoidAndLovName(inDtl.getBankPoid(), "CUSTOMER_BANK_MASTER"));
            inDtlDto.setChequeCompanyDet(lovService.getDetailsByPoidAndLovName(inDtl.getChequeCompanyPoid(), "COMPANY"));
            inDtlDto.setPaymentMainDet(lovService.getDetailsByPoidAndLovName(inDtl.getPaymentMainPoid(), "PAYMENT_MAIN"));
            inDtlDto.setTtBankDet(lovService.getDetailsByPoidAndLovName(inDtl.getTtBankPoid(), "CUSTOMER_BANK_MASTER"));
            inDtlDto.setCardDet(lovService.getDetailsByPoidAndLovName(inDtl.getCardPoid(), "CARD"));

            return inDtlDto;
        }).collect(Collectors.toList());
    }

    private List<GlChequeCashConvertOutDtlDto> buildOutDtls(List<GlChequeCashConvertOutDtlEntity> outDtls) {
        return outDtls.stream().map(outDtl -> {
            GlChequeCashConvertOutDtlDto outDtlDto = new GlChequeCashConvertOutDtlDto();
            outDtlDto.setTransactionPoid(outDtl.getId().getTransactionPoid());
            outDtlDto.setDetRowId(outDtl.getId().getDetRowId());
            outDtlDto.setPaymentMainPoid(outDtl.getPaymentMainPoid());

            outDtlDto.setAmount(outDtl.getAmount());
            outDtlDto.setRemarks(outDtl.getRemarks());
           /* outDtlDto.setCreatedBy(outDtl.getCreatedBy());
            outDtlDto.setCreatedDate(outDtl.getCreatedDate());
            outDtlDto.setLastModifiedBy(outDtl.getLastModifiedBy());
            outDtlDto.setLastModifiedDate(outDtl.getLastModifiedDate());*/
            outDtlDto.setBankPoid(outDtl.getBankPoid());
            outDtlDto.setChqAcName(outDtl.getChqAcName());
            outDtlDto.setChqAcNo(outDtl.getChqAcNo());
            outDtlDto.setChqCardNo(outDtl.getChqCardNo());
            outDtlDto.setChqDate(outDtl.getChqDate());
            outDtlDto.setSelected(outDtl.getSelected());
            outDtlDto.setVoucherType(outDtl.getVoucherType());
            outDtlDto.setLineType(outDtl.getLineType());
            outDtlDto.setBankDet(lovService.getDetailsByPoidAndLovName(outDtl.getBankPoid(), "CUSTOMER_BANK_MASTER"));
            outDtlDto.setPaymentMainDet(lovService.getDetailsByPoidAndLovName(outDtl.getPaymentMainPoid(), "PAYMENT_MAIN"));
            return outDtlDto;
        }).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void softDeleteByTransactionPoid(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        glChequeCashConvertHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("ChequeAndCashConvert", "transactionPoid", transactionPoid));

        // Use DocumentDeleteService for consistent soft delete handling
        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_CHEQUE_CASH_CONVERT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                null
        );
    }

    public Map<String, Object> listOfRecordsAndGenericSearch(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {

        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_POID",startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "TRANSACTION_POID",
                "DOC_REF");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public GlChequeCashConvertHdrDto createGlChequeCashConvert(GlChequeCashConvertHdrDto dto) {

        try{
        GlChequeCashConvertHdrEntity hdrEntity = new GlChequeCashConvertHdrEntity();

        // Validate transaction date to avoid DB trigger 500s
        if (dto.getTransactionDate() == null) {
            throw new ValidationException("transactionDate is required and must be within the open financial period");
        }
        validateTransactionDate(dto.getTransactionDate());
        validateAmounts(dto);
        hdrEntity.setTransactionDate(dto.getTransactionDate());

        hdrEntity.setGroupPoid(UserContext.getGroupPoid());
        hdrEntity.setCompanyPoid(UserContext.getCompanyPoid());
        hdrEntity.setDocRef(dto.getDocRef());
        hdrEntity.setType(dto.getType());
        hdrEntity.setPostingNarration(dto.getPostingNarration());
        hdrEntity.setCash(dto.getCash());
        hdrEntity.setRemarks(dto.getRemarks());
        hdrEntity.setChqAcNo(dto.getChqAcNo());
        hdrEntity.setChqCardNo(dto.getChqCardNo());
        hdrEntity.setRoundingAmt(dto.getRoundingAmt());
        hdrEntity.setCreatedBy(getCurrentUser());
        hdrEntity.setCreatedDate(LocalDateTime.now());
        hdrEntity.setDeleted("N");

        GlChequeCashConvertHdrEntity savedHdr;
        try {
            savedHdr = glChequeCashConvertHdrRepository.save(hdrEntity);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("ORA-20001") || msg.contains("GL_CHEQUE_CASH_CONVERT_HDR_GTTRG") || msg.toUpperCase().contains("FINANCIAL PERIOD")) {
                throw new ValidationException("Changes allowed only within current Financial Period. Please set transactionDate within the open financial period.");
            }
            throw e;
        }

        if (dto.getInDtls() != null && !dto.getInDtls().isEmpty()) {
            Long transactionPoid = savedHdr.getTransactionPoid();
            long detRowIdCounter = 1;
            List<GlChequeCashConvertInDtlEntity> inDtlEntities = new ArrayList<>();
            
            for (GlChequeCashConvertInDtlDto inDto : dto.getInDtls()) {
                if (inDto.getDetRowId() == null) {
                    inDto.setDetRowId(detRowIdCounter++);
                }
                
                GlChequeCashConvertInDtlEntity inEntity = new GlChequeCashConvertInDtlEntity();
                GlChequeCashConvertInDtlKey key = new GlChequeCashConvertInDtlKey();
                key.setTransactionPoid(transactionPoid);
                key.setDetRowId(inDto.getDetRowId());
                inEntity.setId(key);
                inEntity.setBankPoid(inDto.getBankPoid());
                inEntity.setChqAcName(inDto.getChqAcName());
                inEntity.setChqCardNo(inDto.getChqCardNo());
                inEntity.setChqAcNo(inDto.getChqAcNo());
                inEntity.setChqDate(inDto.getChqDate());
                inEntity.setAmount(inDto.getAmount());
                inEntity.setRemarks(inDto.getRemarks());
                inEntity.setVoucherType(inDto.getVoucherType());
                inEntity.setChequeCompanyPoid(inDto.getChequeCompanyPoid());
                inEntity.setPaymentMainPoid(inDto.getPaymentMainPoid());
                inEntity.setLineType(inDto.getLineType());
                inEntity.setPymtType(inDto.getPymtType());
                inEntity.setTtBankPoid(inDto.getTtBankPoid());
                inEntity.setTtRef(inDto.getTtRef());
                inEntity.setCardPoid(inDto.getCardPoid());
                inEntity.setCardType(inDto.getCardType());
                inEntity.setCreditCardRef(inDto.getCreditCardRef());
                inEntity.setCreatedBy(getCurrentUser());
                inEntity.setCreatedDate(LocalDateTime.now());
                inDtlEntities.add(inEntity);
            }

            List<GlChequeCashConvertInDtlEntity> savedInDtls = glChequeCashConvertInDtlRepository.saveAll(inDtlEntities);
            
            savedInDtls.forEach(inDtl -> {
                String logDetail = String.format("Row Created on In Detail with detRowId: %s", inDtl.getId().getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            });
        }

        if (dto.getOutDtls() != null && !dto.getOutDtls().isEmpty()) {
            Long transactionPoid = savedHdr.getTransactionPoid();
            long detRowIdCounter = 1;
            List<GlChequeCashConvertOutDtlEntity> outDtlEntities = new ArrayList<>();
            
            for (GlChequeCashConvertOutDtlDto outDto : dto.getOutDtls()) {
                if (outDto.getDetRowId() == null) {
                    outDto.setDetRowId(detRowIdCounter++);
                }
                
                GlChequeCashConvertOutDtlEntity outEntity = new GlChequeCashConvertOutDtlEntity();
                GlChequeCashConvertOutDtlKey outDtlKey = new GlChequeCashConvertOutDtlKey();
                outDtlKey.setTransactionPoid(transactionPoid);
                outDtlKey.setDetRowId(outDto.getDetRowId());
                outEntity.setId(outDtlKey);
                outEntity.setPaymentMainPoid(outDto.getPaymentMainPoid());
                outEntity.setAmount(outDto.getAmount());
                outEntity.setRemarks(outDto.getRemarks());
                outEntity.setBankPoid(outDto.getBankPoid());
                outEntity.setChqAcName(outDto.getChqAcName());
                outEntity.setChqAcNo(outDto.getChqAcNo());
                outEntity.setChqCardNo(outDto.getChqCardNo());
                outEntity.setChqDate(outDto.getChqDate());
                outEntity.setSelected(outDto.getSelected());
                outEntity.setVoucherType(outDto.getVoucherType());
                outEntity.setLineType(outDto.getLineType());
                outEntity.setCreatedBy(getCurrentUser());
                outEntity.setCreatedDate(LocalDateTime.now());
                outDtlEntities.add(outEntity);
            }

            List<GlChequeCashConvertOutDtlEntity> savedOutDtls = glChequeCashConvertOutDtlRepository.saveAll(outDtlEntities);
            
            savedOutDtls.forEach(outDtl -> {
                String logDetail = String.format("Row Created on Out Detail with detRowId: %s", outDtl.getId().getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            });
        }

        String docId = UserContext.getDocumentId();
        String docKeyPoid = savedHdr.getTransactionPoid().toString();
        String createdMessage = String.format("Created - - DOC:%s KEY:%s", docId, docKeyPoid);
        GlobalLogSummary headerLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createdMessage);
        globalLogSummaryRepository.save(headerLog);

        return getGlChequeCashConvert(savedHdr.getTransactionPoid());
        } catch (Exception ex) {
            throw new ValidationException(extractTriggerErrorMessage(ex));
        }

    }

    @Override
    @Transactional
    public GlChequeCashConvertHdrDto updateGlChequeCashConvert(Long transactionPoid, GlChequeCashConvertHdrDto dto) {

        try {
        GlChequeCashConvertHdrEntity existingHdr = glChequeCashConvertHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Record not found for transactionPoid: " + transactionPoid));

        // Create a copy of the old entity for logging
        GlChequeCashConvertHdrEntity oldEntity = new GlChequeCashConvertHdrEntity();
        BeanUtils.copyProperties(existingHdr, oldEntity);

        validateTransactionDate(dto.getTransactionDate());
        validateAmounts(dto);
        existingHdr.setPostingNarration(dto.getPostingNarration());
        existingHdr.setCash(dto.getCash());
        existingHdr.setRemarks(dto.getRemarks());
        existingHdr.setChqAcNo(dto.getChqAcNo());
        existingHdr.setChqCardNo(dto.getChqCardNo());
        existingHdr.setRoundingAmt(dto.getRoundingAmt());
        existingHdr.setLastModifiedBy(getCurrentUser());
        existingHdr.setLastModifiedDate(LocalDateTime.now());
        existingHdr.setDeleted(dto.getDeleted() != null ? dto.getDeleted() : "N");

        if (dto.getTransactionDate() != null) {
            existingHdr.setTransactionDate(dto.getTransactionDate());
        }

        GlChequeCashConvertHdrEntity savedHdr = glChequeCashConvertHdrRepository.save(existingHdr);

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<GlobalLogSummary> subTableSummaryLogs = new ArrayList<>();
        List<LogRequestDto<GlChequeCashConvertHdrEntity>> headerLogRequests = new ArrayList<>();

        if (dto.getInDtls() != null && !dto.getInDtls().isEmpty()) {
            updateInDtls(dto.getInDtls(), transactionPoid, docId, docKeyPoid, subTableSummaryLogs);
        }
        if (dto.getOutDtls() != null && !dto.getOutDtls().isEmpty()) {
            updateOutDtls(dto.getOutDtls(), transactionPoid, docId, docKeyPoid, subTableSummaryLogs);
        }

        String modifiedMessage = String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid);
        subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid, modifiedMessage));
        String headerLogDetail = String.format("KeyId = TRANSACTION_POID:%s", docKeyPoid);
        headerLogRequests.add(new LogRequestDto<>(oldEntity, savedHdr, GlChequeCashConvertHdrEntity.class, docId, docKeyPoid, headerLogDetail));

        if (!subTableSummaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(subTableSummaryLogs);
        }
        if (!headerLogRequests.isEmpty()) {
            loggingService.createLogBatch(headerLogRequests);
        }

        return getGlChequeCashConvert(savedHdr.getTransactionPoid());
        } catch (Exception ex) {
            throw new ValidationException(extractTriggerErrorMessage(ex));
        }
    }

    @Override
    public List<GlChequeConversionLoadResponseDto> loadGlChequeConversion(String chequeNumber, String chequeAccNumber, String type) {

        List<GlChequeConversionLoadResponseDto> glChequeConversionLoadResponseDtos = glChequeCashConvertRepository.loadGlChequeConversion(chequeNumber, chequeAccNumber, type);
        glChequeConversionLoadResponseDtos.forEach(c->{
            c.setBankDet(lovService.getDetailsByPoidAndLovName(c.getBankPoid(), "CUSTOMER_BANK_MASTER"));
        });
        return glChequeConversionLoadResponseDtos;
    }

    private String extractTriggerErrorMessage(Exception ex) {

        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        // ----------  PostgreSQL (SQLSTATE) ----------
        if (root instanceof org.postgresql.util.PSQLException pgEx) {
            String sqlState = pgEx.getSQLState();

            if ("P2001".equals(sqlState)) {
                return "Changes allowed only within current Financial Period";
            }
            if ("P2002".equals(sqlState)) {
                return "Transaction date cannot be updated";
            }
            if ("P2003".equals(sqlState)) {
                return "Changes allowed only within current Transaction Period";
            }
        }

        // ----------  Oracle (Error Code) ----------
        if (root instanceof java.sql.SQLException sqlEx) {
            int errorCode = sqlEx.getErrorCode();

            if (errorCode == 20001) {
                return "Changes allowed only within current Financial Period";
            }
            if (errorCode == 20002) {
                return "Transaction date cannot be updated";
            }
            if (errorCode == 20003) {
                return "Changes allowed only within current Transaction Period";
            }
        }

        // ----------  Message fallback (DB agnostic) ----------
        String message = root.getMessage();
        if (message != null) {
            String msg = message.toLowerCase();

            if (msg.contains("financial period")) {
                return "Changes allowed only within current Financial Period";
            }
            if (msg.contains("transaction date") && msg.contains("update")) {
                return "Transaction date cannot be updated";
            }
            if (msg.contains("transaction period")) {
                return "Changes allowed only within current Transaction Period";
            }
        }

        return "Database validation failed: " + (message != null ? message : "Unknown error");
    }

    private void validateTransactionDate(LocalDate transactionDate) {
        if (transactionDate != null && transactionDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date cannot be in future");
        }
    }

    private void updateInDtls(List<GlChequeCashConvertInDtlDto> inDtls, Long transactionPoid, String docId, String docKeyPoid,
                            List<GlobalLogSummary> summaryLogs) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        List<GlChequeCashConvertInDtlEntity> toSave = new ArrayList<>();
        List<GlChequeCashConvertInDtlEntity> newlyCreatedEntities = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlChequeCashConvertInDtlEntity>> logRequests = new ArrayList<>();

        List<GlChequeCashConvertInDtlEntity> existingDetails = glChequeCashConvertInDtlRepository.findByIdTransactionPoid(transactionPoid);
        Map<Long, GlChequeCashConvertInDtlEntity> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(e -> e.getId().getDetRowId(), Function.identity(), (a, b) -> a));

        Long maxDetRowId = existingDetails.stream()
                .map(e -> e.getId().getDetRowId())
                .max(Long::compareTo)
                .orElse(0L);

        for (GlChequeCashConvertInDtlDto inDto : inDtls) {
            String actionType = inDto.getActionType() == null ? "NOCHANGE" : inDto.getActionType().toUpperCase();
            switch (actionType) {
                case "ISCREATED":
                    if (inDto.getDetRowId() == null) {
                        inDto.setDetRowId(++maxDetRowId);
                    }
                    GlChequeCashConvertInDtlEntity newInEntity = buildInDtlEntity(inDto, transactionPoid, currentUser, now);
                    toSave.add(newInEntity);
                    newlyCreatedEntities.add(newInEntity);
                    break;

                case "ISUPDATED":
                    GlChequeCashConvertInDtlEntity existingIn = glChequeCashConvertInDtlRepository
                            .findByIdTransactionPoidAndIdDetRowId(transactionPoid, inDto.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Cheque Cash Convert In Detail", "detRowId", inDto.getDetRowId()));

                    GlChequeCashConvertInDtlEntity oldIn = new GlChequeCashConvertInDtlEntity();
                    BeanUtils.copyProperties(existingIn, oldIn);
                    GlChequeCashConvertInDtlKey oldInKey = new GlChequeCashConvertInDtlKey();
                    oldInKey.setTransactionPoid(existingIn.getId().getTransactionPoid());
                    oldInKey.setDetRowId(existingIn.getId().getDetRowId());
                    oldIn.setId(oldInKey);

                    mapInDtoToEntity(inDto, existingIn, currentUser, now);
                    toSave.add(existingIn);

                    String inLogDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, oldIn.getId().getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldIn, existingIn, GlChequeCashConvertInDtlEntity.class, docId, docKeyPoid, inLogDetail));
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid,
                            String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid)));
                    break;

                case "ISDELETED":
                    toDelete.add(inDto.getDetRowId());
                    GlChequeCashConvertInDtlEntity oldInForDelete = existingMap.get(inDto.getDetRowId());
                    if (oldInForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, bankPoid:%s, amount:%s, remarks:%s",
                                oldInForDelete.getId().getDetRowId(), transactionPoid, oldInForDelete.getBankPoid(), oldInForDelete.getAmount(), oldInForDelete.getRemarks());
                        summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, "Row Deleted " + deletedRecordString));
                    }
                    break;

                case "NOCHANGE":
                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            List<GlChequeCashConvertInDtlEntity> savedEntities = glChequeCashConvertInDtlRepository.saveAll(toSave);
            for (GlChequeCashConvertInDtlEntity newlyCreated : newlyCreatedEntities) {
                GlChequeCashConvertInDtlEntity savedEntity = savedEntities.stream()
                        .filter(s -> s.getId().getTransactionPoid().equals(newlyCreated.getId().getTransactionPoid())
                                && s.getId().getDetRowId().equals(newlyCreated.getId().getDetRowId()))
                        .findFirst().orElse(null);
                if (savedEntity != null && savedEntity.getId().getDetRowId() != null) {
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid,
                            String.format("Row Created on Cheque Cash Convert In Detail with DetRowId: %s", savedEntity.getId().getDetRowId())));
                }
            }
        }
        if (!toDelete.isEmpty()) {
            glChequeCashConvertInDtlRepository.deleteByIdTransactionPoidAndIdDetRowIdIn(transactionPoid, toDelete);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void updateOutDtls(List<GlChequeCashConvertOutDtlDto> outDtls, Long transactionPoid, String docId, String docKeyPoid,
                              List<GlobalLogSummary> summaryLogs) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        List<GlChequeCashConvertOutDtlEntity> toSave = new ArrayList<>();
        List<GlChequeCashConvertOutDtlEntity> newlyCreatedEntities = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlChequeCashConvertOutDtlEntity>> logRequests = new ArrayList<>();

        List<GlChequeCashConvertOutDtlEntity> existingDetails = glChequeCashConvertOutDtlRepository.findByIdTransactionPoid(transactionPoid);
        Map<Long, GlChequeCashConvertOutDtlEntity> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(e -> e.getId().getDetRowId(), Function.identity(), (a, b) -> a));

        Long maxDetRowId = existingDetails.stream()
                .map(e -> e.getId().getDetRowId())
                .max(Long::compareTo)
                .orElse(0L);

        for (GlChequeCashConvertOutDtlDto outDto : outDtls) {
            String actionType = outDto.getActionType() == null ? "NOCHANGE" : outDto.getActionType().toUpperCase();
            switch (actionType) {
                case "ISCREATED":
                    if (outDto.getDetRowId() == null) {
                        outDto.setDetRowId(++maxDetRowId);
                    }
                    GlChequeCashConvertOutDtlEntity newOutEntity = buildOutDtlEntity(outDto, transactionPoid, currentUser, now);
                    toSave.add(newOutEntity);
                    newlyCreatedEntities.add(newOutEntity);
                    break;

                case "ISUPDATED":
                    GlChequeCashConvertOutDtlEntity existingOut = glChequeCashConvertOutDtlRepository
                            .findByIdTransactionPoidAndIdDetRowId(transactionPoid, outDto.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Cheque Cash Convert Out Detail", "detRowId", outDto.getDetRowId()));

                    GlChequeCashConvertOutDtlEntity oldOut = new GlChequeCashConvertOutDtlEntity();
                    BeanUtils.copyProperties(existingOut, oldOut);
                    GlChequeCashConvertOutDtlKey oldOutKey = new GlChequeCashConvertOutDtlKey();
                    oldOutKey.setTransactionPoid(existingOut.getId().getTransactionPoid());
                    oldOutKey.setDetRowId(existingOut.getId().getDetRowId());
                    oldOut.setId(oldOutKey);

                    mapOutDtoToEntity(outDto, existingOut, currentUser, now);
                    toSave.add(existingOut);

                    String outLogDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, oldOut.getId().getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldOut, existingOut, GlChequeCashConvertOutDtlEntity.class, docId, docKeyPoid, outLogDetail));
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid,
                            String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid)));
                    break;

                case "ISDELETED":
                    toDelete.add(outDto.getDetRowId());
                    GlChequeCashConvertOutDtlEntity oldOutForDelete = existingMap.get(outDto.getDetRowId());
                    if (oldOutForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, paymentMainPoid:%s, amount:%s, remarks:%s",
                                oldOutForDelete.getId().getDetRowId(), transactionPoid, oldOutForDelete.getPaymentMainPoid(), oldOutForDelete.getAmount(), oldOutForDelete.getRemarks());
                        summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, "Row Deleted " + deletedRecordString));
                    }
                    break;

                case "NOCHANGE":
                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            List<GlChequeCashConvertOutDtlEntity> savedEntities = glChequeCashConvertOutDtlRepository.saveAll(toSave);
            for (GlChequeCashConvertOutDtlEntity newlyCreated : newlyCreatedEntities) {
                GlChequeCashConvertOutDtlEntity savedEntity = savedEntities.stream()
                        .filter(s -> s.getId().getTransactionPoid().equals(newlyCreated.getId().getTransactionPoid())
                                && s.getId().getDetRowId().equals(newlyCreated.getId().getDetRowId()))
                        .findFirst().orElse(null);
                if (savedEntity != null && savedEntity.getId().getDetRowId() != null) {
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid,
                            String.format("Row Created on Cheque Cash Convert Out Detail with DetRowId: %s", savedEntity.getId().getDetRowId())));
                }
            }
        }
        if (!toDelete.isEmpty()) {
            glChequeCashConvertOutDtlRepository.deleteByIdTransactionPoidAndIdDetRowIdIn(transactionPoid, toDelete);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private GlChequeCashConvertInDtlEntity buildInDtlEntity(GlChequeCashConvertInDtlDto inDto, Long transactionPoid, String currentUser, LocalDateTime now) {
        GlChequeCashConvertInDtlKey key = new GlChequeCashConvertInDtlKey();
        key.setTransactionPoid(transactionPoid);
        key.setDetRowId(inDto.getDetRowId());
        GlChequeCashConvertInDtlEntity entity = new GlChequeCashConvertInDtlEntity();
        entity.setId(key);
        mapInDtoToEntity(inDto, entity, currentUser, now);
        return entity;
    }

    private void mapInDtoToEntity(GlChequeCashConvertInDtlDto inDto, GlChequeCashConvertInDtlEntity entity, String currentUser, LocalDateTime now) {
        entity.setBankPoid(inDto.getBankPoid());
        entity.setChqAcName(inDto.getChqAcName());
        entity.setChqAcNo(inDto.getChqAcNo());
        entity.setChqCardNo(inDto.getChqCardNo());
        entity.setChqDate(inDto.getChqDate());
        entity.setAmount(inDto.getAmount());
        entity.setRemarks(inDto.getRemarks());
        entity.setVoucherType(inDto.getVoucherType());
        entity.setChequeCompanyPoid(inDto.getChequeCompanyPoid());
        entity.setPaymentMainPoid(inDto.getPaymentMainPoid());
        entity.setLineType(inDto.getLineType());
        entity.setPymtType(inDto.getPymtType());
        entity.setTtBankPoid(inDto.getTtBankPoid());
        entity.setTtRef(inDto.getTtRef());
        entity.setCardPoid(inDto.getCardPoid());
        entity.setCardType(inDto.getCardType());
        entity.setCreditCardRef(inDto.getCreditCardRef());
        entity.setCreatedBy(currentUser);
        entity.setCreatedDate(now);
        entity.setLastModifiedBy(currentUser);
        entity.setLastModifiedDate(now);
    }

    private GlChequeCashConvertOutDtlEntity buildOutDtlEntity(GlChequeCashConvertOutDtlDto outDto, Long transactionPoid, String currentUser, LocalDateTime now) {
        GlChequeCashConvertOutDtlKey key = new GlChequeCashConvertOutDtlKey();
        key.setTransactionPoid(transactionPoid);
        key.setDetRowId(outDto.getDetRowId());
        GlChequeCashConvertOutDtlEntity entity = new GlChequeCashConvertOutDtlEntity();
        entity.setId(key);
        mapOutDtoToEntity(outDto, entity, currentUser, now);
        return entity;
    }

    private void mapOutDtoToEntity(GlChequeCashConvertOutDtlDto outDto, GlChequeCashConvertOutDtlEntity entity, String currentUser, LocalDateTime now) {
        entity.setPaymentMainPoid(outDto.getPaymentMainPoid());
        entity.setAmount(outDto.getAmount());
        entity.setRemarks(outDto.getRemarks());
        entity.setBankPoid(outDto.getBankPoid());
        entity.setChqAcName(outDto.getChqAcName());
        entity.setChqAcNo(outDto.getChqAcNo());
        entity.setChqCardNo(outDto.getChqCardNo());
        entity.setChqDate(outDto.getChqDate());
        entity.setSelected(outDto.getSelected());
        entity.setVoucherType(outDto.getVoucherType());
        entity.setLineType(outDto.getLineType());
        entity.setCreatedBy(currentUser);
        entity.setCreatedDate(now);
        entity.setLastModifiedBy(currentUser);
        entity.setLastModifiedDate(now);
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(new Timestamp(System.currentTimeMillis()));
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-110");
        JasperReport mainReport = printService.load("Finance/GL/Cheque_Cash_Conversion.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private void validateAmounts(GlChequeCashConvertHdrDto dto) {

        if (dto.getOutDtls() == null || dto.getOutDtls().isEmpty()) {
            throw new ValidationException("No Detail present for the cheque");
        }

        Long outTotal = dto.getOutDtls().stream()
                .filter(o -> !"N".equalsIgnoreCase(o.getSelected()))
                .map(o -> o.getAmount() == null ? 0L : o.getAmount())
                .reduce(0L, Long::sum);

        if (outTotal == 0) {
            throw new ValidationException("No Detail present for the cheque");
        }

        String type = dto.getType();

        // ================= CHEQUE_TO_CHEQUE =================
        if ("CHEQUE_TO_CHEQUE".equalsIgnoreCase(type)) {

            if (dto.getInDtls() == null || dto.getInDtls().isEmpty()) {
                throw new ValidationException("No Detail present in cheque conversion TO");
            }

            Long inTotal = dto.getInDtls().stream()
                    .map(i -> i.getAmount() == null ? 0L : i.getAmount())
                    .reduce(0L, Long::sum);

            if (!outTotal.equals(inTotal)) {
                throw new ValidationException(
                        "Total cheque amount (" + outTotal +
                                ") is not matched with converted cheque amount (" + inTotal + ")");
            }
        }

        // ================= CHEQUE_TO_BANK =================
        if ("CHEQUE_TO_BANK".equalsIgnoreCase(type)) {

            Long inTotal = dto.getInDtls().stream()
                    .map(i -> i.getAmount() == null ? 0L : i.getAmount())
                    .reduce(0L, Long::sum);

            if (!outTotal.equals(inTotal)) {
                throw new ValidationException(
                        "Total cheque amount (" + outTotal +
                                ") is not matched with converted bank amount (" + inTotal + ")");
            }
        }

        // ================= CHEQUE_TO_CASH =================
        if ("CHEQUE_TO_CASH".equalsIgnoreCase(type)) {

            if (dto.getCash() == null) {
                throw new ValidationException("Please enter Cash amount");
            }

            Long cashAmount = dto.getCash();
            Long rounding = dto.getRoundingAmt() == null ? 0L : dto.getRoundingAmt();

            if (!outTotal.equals(cashAmount + rounding)) {
                throw new ValidationException(
                        "Total cheque amount (" + outTotal +
                                ") is not matched with converted cash amount (" + (cashAmount + rounding) + ")");
            }
        }

        // ================= CASH_TO_CHEQUE =================
        if ("CASH_TO_CHEQUE".equalsIgnoreCase(type)) {

            Long inTotal = dto.getInDtls().stream()
                    .map(i -> i.getAmount() == null ? 0L : i.getAmount())
                    .reduce(0L, Long::sum);

            if (!outTotal.equals(inTotal)) {
                throw new ValidationException(
                        "Cash Amount (" + outTotal +
                                ") and Cheque Amount (" + inTotal + ") are not matching...");
            }
        }

        // ================= Rounding Limit =================
        if (dto.getRoundingAmt() != null && dto.getRoundingAmt() > 99) {
            throw new ValidationException("RoundingAmount is greater than allowed limit");
        }
    }

}





