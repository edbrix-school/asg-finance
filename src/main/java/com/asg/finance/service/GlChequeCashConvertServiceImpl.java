package com.asg.finance.service;


import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.GlChequeCashConvertHdrDto;
import com.asg.finance.dto.GlChequeCashConvertInDtlDto;
import com.asg.finance.dto.GlChequeCashConvertOutDtlDto;
import com.asg.finance.dto.GlChequeConversionLoadResponseDto;
import com.asg.finance.entity.GlChequeCashConvertHdrEntity;
import com.asg.finance.entity.GlChequeCashConvertInDtlEntity;
import com.asg.finance.entity.GlChequeCashConvertOutDtlEntity;
import com.asg.finance.entity.key.GlChequeCashConvertInDtlKey;
import com.asg.finance.entity.key.GlChequeCashConvertOutDtlKey;
import com.asg.finance.repository.GlChequeCashConvertHdrRepository;
import com.asg.finance.repository.GlChequeCashConvertInDtlRepository;
import com.asg.finance.repository.GlChequeCashConvertOutDtlRepository;
import com.asg.finance.repository.GlChequeCashConvertRepository;
import com.asg.common.lib.utility.PaginationUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.asg.common.lib.exception.ValidationException;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

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
    public void softDeleteByTransactionPoid(Long transactionPoid) {

        GlChequeCashConvertHdrEntity entity = glChequeCashConvertHdrRepository.findById(transactionPoid).orElseThrow(() -> new ResourceNotFoundException("ChequeAndCashConvert", "transactionPoid", transactionPoid));
        entity.setDeleted("Y");
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        glChequeCashConvertHdrRepository.save(entity);

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
        hdrEntity.setTransactionDate(dto.getTransactionDate());

        hdrEntity.setGroupPoid(dto.getGroupPoid());
        hdrEntity.setCompanyPoid(dto.getCompanyPoid());
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
            List<GlChequeCashConvertInDtlEntity> inDtlEntities = dto.getInDtls().stream().map(inDto -> {
                GlChequeCashConvertInDtlEntity inEntity = new GlChequeCashConvertInDtlEntity();
                GlChequeCashConvertInDtlKey key = new GlChequeCashConvertInDtlKey();
                Long transactionPoid = savedHdr.getTransactionPoid();
                Long maxDetRowId = glChequeCashConvertInDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
                log.info("maxDetRowId : {}", maxDetRowId);
                key.setTransactionPoid(transactionPoid);
                key.setDetRowId(maxDetRowId);
                inEntity.setId(key);
                log.info("key: {}", key);
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
                return inEntity;
            }).collect(Collectors.toList());

            glChequeCashConvertInDtlRepository.saveAll(inDtlEntities);
        }

        if (dto.getOutDtls() != null && !dto.getOutDtls().isEmpty()) {
            List<GlChequeCashConvertOutDtlEntity> outDtlEntities = dto.getOutDtls().stream().map(outDto -> {
                GlChequeCashConvertOutDtlEntity outEntity = new GlChequeCashConvertOutDtlEntity();
                GlChequeCashConvertOutDtlKey outDtlKey = new GlChequeCashConvertOutDtlKey();
                Long transactionPoid = savedHdr.getTransactionPoid();
                log.info("TransactionPoid : {}", transactionPoid);
                outDtlKey.setTransactionPoid(transactionPoid);
                Long maxDetRowId = glChequeCashConvertOutDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
                log.info(" out det RowId : {}", maxDetRowId);
                outDtlKey.setDetRowId(maxDetRowId);
                outEntity.setId(outDtlKey);
                log.info("out key: {}", outDtlKey);
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
                return outEntity;
            }).collect(Collectors.toList());

            glChequeCashConvertOutDtlRepository.saveAll(outDtlEntities);
        }

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

        validateTransactionDate(dto.getTransactionDate());
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

        if (dto.getInDtls() != null && !dto.getInDtls().isEmpty()) {
            AtomicLong nextInDetRowId = new AtomicLong(glChequeCashConvertInDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid));
            List<GlChequeCashConvertInDtlEntity> inDtlEntities = dto.getInDtls().stream().map(inDto -> {
                GlChequeCashConvertInDtlEntity inEntity = new GlChequeCashConvertInDtlEntity();
                GlChequeCashConvertInDtlKey key = new GlChequeCashConvertInDtlKey();
                key.setTransactionPoid(transactionPoid);
                Long detRowId = inDto.getDetRowId();
                if (detRowId == null || detRowId <= 0) {
                    detRowId = nextInDetRowId.getAndIncrement();
                }
                key.setDetRowId(detRowId);
                inEntity.setId(key);
                inEntity.setBankPoid(inDto.getBankPoid());
                inEntity.setChqAcName(inDto.getChqAcName());
                inEntity.setChqCardNo(inDto.getChqCardNo());
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
                return inEntity;
            }).collect(Collectors.toList());

            glChequeCashConvertInDtlRepository.saveAll(inDtlEntities);
        }

        if (dto.getOutDtls() != null && !dto.getOutDtls().isEmpty()) {
            AtomicLong nextOutDetRowId = new AtomicLong(glChequeCashConvertOutDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid));
            List<GlChequeCashConvertOutDtlEntity> outDtlEntities = dto.getOutDtls().stream().map(outDto -> {
                GlChequeCashConvertOutDtlEntity outEntity = new GlChequeCashConvertOutDtlEntity();
                GlChequeCashConvertOutDtlKey outDtlKey = new GlChequeCashConvertOutDtlKey();
                outDtlKey.setTransactionPoid(transactionPoid);
                Long detRowId = outDto.getDetRowId();
                if (detRowId == null || detRowId <= 0) {
                    detRowId = nextOutDetRowId.getAndIncrement();
                }
                outDtlKey.setDetRowId(detRowId);
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
                return outEntity;
            }).collect(Collectors.toList());

            glChequeCashConvertOutDtlRepository.saveAll(outDtlEntities);
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
}





