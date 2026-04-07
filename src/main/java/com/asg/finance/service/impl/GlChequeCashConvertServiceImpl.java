package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.dto.GlChequeCashConvertHdrDto;
import com.asg.finance.dto.GlChequeCashConvertInDtlDto;
import com.asg.finance.dto.GlChequeCashConvertOutDtlDto;
import com.asg.finance.dto.GlChequeCashConvertValidateEditResponseDto;
import com.asg.finance.dto.GlChequeConversionLoadResponseDto;
import com.asg.finance.entity.GlChequeCashConvertHdrEntity;
import com.asg.finance.entity.GlChequeCashConvertInDtlEntity;
import com.asg.finance.entity.GlChequeCashConvertOutDtlEntity;
import com.asg.finance.entity.key.GlChequeCashConvertInDtlKey;
import com.asg.finance.entity.key.GlChequeCashConvertOutDtlKey;
import com.asg.finance.repository.GlChequeCashConvertHdrRepository;
import com.asg.finance.repository.GlChequeCashConvertInDtlRepository;
import com.asg.finance.repository.GlChequeCashConvertOutDtlRepository;
import com.asg.finance.repository.ChequeCashConvertCustomRepository;
import com.asg.finance.repository.GlChequeCashConvertRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.GlChequeCashConvertService;
import com.asg.finance.service.GlPostingService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.asg.common.lib.service.GlobalParameterService;

import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.asg.finance.event.GlChequeCashConvertSaveEvent;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final GlobalParameterService globalParameterService;
    private final GlPostingService glPostingService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Override
    public GlChequeCashConvertValidateEditResponseDto validateForEdit(Long transactionPoid) {
        glChequeCashConvertHdrRepository.findById(transactionPoid)
                .orElseThrow(
                        () -> new ResourceNotFoundException("GlChequeCashConvert", "transactionPoid", transactionPoid));
        validateStatusForEdit(transactionPoid);
        return new GlChequeCashConvertValidateEditResponseDto(true, "Record is eligible for edit.");
    }

    @Override
    public GlChequeCashConvertHdrDto getGlChequeCashConvert(Long transactionPoid) {
        GlChequeCashConvertHdrEntity glChequeCashConvertHdrEntity = glChequeCashConvertHdrRepository
                .findByTransactionPoid(transactionPoid);
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
            inDtlDto.setChequeCompanyDet(
                    lovService.getDetailsByPoidAndLovName(inDtl.getChequeCompanyPoid(), "COMPANY"));
            inDtlDto.setPaymentMainDet(
                    lovService.getDetailsByPoidAndLovName(inDtl.getPaymentMainPoid(), "PAYMENT_MAIN"));
            inDtlDto.setTtBankDet(lovService.getDetailsByPoidAndLovName(inDtl.getTtBankPoid(), "CCC_BANK_MASTER_COMPANY_WISE"));
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
            outDtlDto.setPaymentMainDet(
                    lovService.getDetailsByPoidAndLovName(outDtl.getPaymentMainPoid(), "PAYMENT_MAIN"));
            return outDtlDto;
        }).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void softDeleteByTransactionPoid(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlChequeCashConvertHdrEntity existing = glChequeCashConvertHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("ChequeAndCashConvert", "transactionPoid",
                        transactionPoid));

        // Use DocumentDeleteService for consistent soft delete handling
        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_CHEQUE_CASH_CONVERT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                existing.getTransactionDate());
    }

    public Map<String, Object> listOfRecordsAndGenericSearch(String documentId, FilterRequestDto filters,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {

        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate,
                endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "TRANSACTION_POID",
                "DOC_REF");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    @PerformGlPosting
    public GlChequeCashConvertHdrDto createGlChequeCashConvert(GlChequeCashConvertHdrDto dto) {

        GlChequeCashConvertHdrEntity hdrEntity = new GlChequeCashConvertHdrEntity();

        // Validate transaction date and trigger rules at application level (mirrors
        // GL_CHEQUE_CASH_CONVERT_HDR_GTTRG)
        if (dto.getTransactionDate() == null) {
            throw new ValidationException("Transaction date is required and must be within the open financial period.");
        }
        validateTransactionDate(dto.getTransactionDate());
        Long companyPoid = dto.getCompanyPoid() != null ? dto.getCompanyPoid() : UserContext.getCompanyPoid();
        validateTriggerRulesForCreate(companyPoid, dto.getTransactionDate());
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

        GlChequeCashConvertHdrEntity savedHdr = glChequeCashConvertHdrRepository.save(hdrEntity);
        entityManager.flush();

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
        //loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, docKeyPoid);
        loggingService.createLogSummaryEntry("400-110", docKeyPoid, String.format("%s %s", LogDetailsEnum.CREATED, savedHdr.getDocRef()));
        eventPublisher.publishEvent(new GlChequeCashConvertSaveEvent(
                this,
                savedHdr,
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                getCurrentUser()
        ));

        return getGlChequeCashConvert(savedHdr.getTransactionPoid());
    }

    @Override
    @Transactional
    @PerformGlPosting
    public GlChequeCashConvertHdrDto updateGlChequeCashConvert(Long transactionPoid, GlChequeCashConvertHdrDto dto) {

        validateStatusForEdit(transactionPoid);
        GlChequeCashConvertHdrEntity existingHdr = glChequeCashConvertHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Record not found for transactionPoid: " + transactionPoid));

        // Create a copy of the old entity for logging
        GlChequeCashConvertHdrEntity oldEntity = new GlChequeCashConvertHdrEntity();
        BeanUtils.copyProperties(existingHdr, oldEntity);

        validateTransactionDate(dto.getTransactionDate());
        Long companyPoid = existingHdr.getCompanyPoid();
        LocalDate oldTransactionDate = existingHdr.getTransactionDate();
        LocalDate newTransactionDate = dto.getTransactionDate() != null ? dto.getTransactionDate() : oldTransactionDate;
        validateTriggerRulesForUpdate(companyPoid, oldTransactionDate, newTransactionDate);
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
        List<LogRequestDto<GlChequeCashConvertHdrEntity>> headerLogRequests = new ArrayList<>();

        if (dto.getInDtls() != null && !dto.getInDtls().isEmpty()) {
            updateInDtls(dto.getInDtls(), transactionPoid, docId, docKeyPoid);
        }
        if (dto.getOutDtls() != null && !dto.getOutDtls().isEmpty()) {
            updateOutDtls(dto.getOutDtls(), transactionPoid, docId, docKeyPoid);
        }

        String headerLogDetail = String.format("KeyId = TRANSACTION_POID:%s", docKeyPoid);
        headerLogRequests.add(new LogRequestDto<>(oldEntity, savedHdr, GlChequeCashConvertHdrEntity.class, docId, docKeyPoid, headerLogDetail));

        loggingService.createLogBatch(headerLogRequests);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid);

        glChequeCashConvertInDtlRepository.flush();
        glChequeCashConvertOutDtlRepository.flush();

        eventPublisher.publishEvent(new GlChequeCashConvertSaveEvent(
                this,
                savedHdr,
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                getCurrentUser()
        ));

        return getGlChequeCashConvert(savedHdr.getTransactionPoid());
    }

    /**
     * Validates trigger rules before INSERT (mirrors GL_CHEQUE_CASH_CONVERT_HDR_GTTRG):
     * - Financial year check for transaction date.
     */
    private void validateTriggerRulesForCreate(Long companyPoid, LocalDate transactionDate) {
        if (companyPoid == null || transactionDate == null) {
            return;
        }
        if (!glChequeCashConvertRepository.isFinancialYearValid(companyPoid, transactionDate)) {
            throw new ValidationException("Changes allowed only within current Financial Period. Please set transaction date within the open financial period.");
        }
    }

    /**
     * Validates trigger rules before UPDATE (mirrors GL_CHEQUE_CASH_CONVERT_HDR_GTTRG):
     * - Financial year check for new transaction date.
     * - If date is changing: transaction period check for new date.
     * - Transaction period check for old transaction date (current period for edit).
     */
    private void validateTriggerRulesForUpdate(Long companyPoid, LocalDate oldTransactionDate, LocalDate newTransactionDate) {
        if (companyPoid == null) {
            return;
        }
        if (newTransactionDate != null && !glChequeCashConvertRepository.isFinancialYearValid(companyPoid, newTransactionDate)) {
            throw new ValidationException("Changes allowed only within current Financial Period. Please set transaction date within the open financial period.");
        }
        if (oldTransactionDate != null && !oldTransactionDate.equals(newTransactionDate)) {
            if (newTransactionDate != null && !glChequeCashConvertRepository.isTransactionYearValid(companyPoid, newTransactionDate)) {
                throw new ValidationException("Transaction date cannot be updated. The new date is outside the current transaction period.");
            }
        }
        if (oldTransactionDate != null && !glChequeCashConvertRepository.isTransactionYearValid(companyPoid, oldTransactionDate)) {
            throw new ValidationException("Changes allowed only within current Transaction Period. This record's transaction date is outside the editable period.");
        }
    }

    @Override
    public List<GlChequeConversionLoadResponseDto> loadGlChequeConversion(String chequeNumber, String chequeAccNumber, String type) {
        if (StringUtils.isBlank(type)) {
            throw new ValidationException("Type is required for load (e.g. CHEQUE_TO_CHEQUE, CHEQUE_TO_CASH, CASH_TO_CHEQUE, CHEQUE_TO_IMCOCHEQUE, CHEQUE_TO_BANK)");
        }
        List<GlChequeConversionLoadResponseDto> glChequeConversionLoadResponseDtos = glChequeCashConvertRepository.loadGlChequeConversion(chequeNumber, chequeAccNumber, type.trim());
        glChequeConversionLoadResponseDtos.forEach(c -> {
            c.setBankDet(lovService.getDetailsByPoidAndLovName(c.getBankPoid(), "CUSTOMER_BANK_MASTER"));
        });
        return glChequeConversionLoadResponseDtos;
    }

    private void validateTransactionDate(LocalDate transactionDate) {
        if (transactionDate != null && transactionDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date cannot be in future");
        }
    }

    private void updateInDtls(List<GlChequeCashConvertInDtlDto> inDtls, Long transactionPoid, String docId, String docKeyPoid) {
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
                    break;

                case "ISDELETED":
                    toDelete.add(inDto.getDetRowId());
                    GlChequeCashConvertInDtlEntity oldInForDelete = existingMap.get(inDto.getDetRowId());
                    if (oldInForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, bankPoid:%s, amount:%s, remarks:%s",
                                oldInForDelete.getId().getDetRowId(), transactionPoid, oldInForDelete.getBankPoid(), oldInForDelete.getAmount(), oldInForDelete.getRemarks());
                        String logDetail = "Row Deleted " + deletedRecordString;
                        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
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
                    String logDetail = String.format("Row Created on Cheque Cash Convert In Detail with DetRowId: %s", savedEntity.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
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

    private void updateOutDtls(List<GlChequeCashConvertOutDtlDto> outDtls, Long transactionPoid, String docId, String docKeyPoid) {
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
                    break;

                case "ISDELETED":
                    toDelete.add(outDto.getDetRowId());
                    GlChequeCashConvertOutDtlEntity oldOutForDelete = existingMap.get(outDto.getDetRowId());
                    if (oldOutForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, paymentMainPoid:%s, amount:%s, remarks:%s",
                                oldOutForDelete.getId().getDetRowId(), transactionPoid, oldOutForDelete.getPaymentMainPoid(), oldOutForDelete.getAmount(), oldOutForDelete.getRemarks());
                        String logDetail = "Row Deleted " + deletedRecordString;
                        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
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
                    String logDetail = String.format("Row Created on Cheque Cash Convert Out Detail with DetRowId: %s", savedEntity.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
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

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-110");
        JasperReport mainReport = printService.load("Finance/GL/Cheque_Cash_Conversion.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    /**
     * Validates request against rules from legacy ChequeCashConversionBean.DocumentBeforeSave().
     * Uses StringUtils.isBlank() for string checks. Type can be numeric ("1"-"5") or legacy string (e.g. CHEQUE_TO_CHEQUE).
     */
    private void validateAmounts(GlChequeCashConvertHdrDto dto) {
        if (StringUtils.isBlank(dto.getType())) {
            throw new ValidationException("Cheque Conversion Type is required");
        }
        String type = dto.getType().trim();

        if (dto.getOutDtls() == null || dto.getOutDtls().isEmpty()) {
            throw new ValidationException("No Detail present for the cheque");
        }

        List<GlChequeCashConvertOutDtlDto> selectedOut = dto.getOutDtls().stream()
                .filter(o -> o.getSelected() != null && !"N".equalsIgnoreCase(o.getSelected().trim()))
                .toList();

        if (selectedOut.isEmpty()) {
            throw new ValidationException("No Detail present for the cheque");
        }

        BigDecimal outTotal = selectedOut.stream()
                .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Voucher type must be same across all selected OUT rows (legacy: only compare when both non-null)
        String outVoucherType = null;
        Set<String> distinctVoucherTypes = selectedOut.stream()
                .map(o -> o.getVoucherType() == null ? null : o.getVoucherType().trim())
                .filter(StringUtils::isNotBlank)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        if (distinctVoucherTypes.size() > 1) {
            throw new ValidationException("Voucher type should be same from 'FROM' table..");
        }
        outVoucherType = selectedOut.stream()
                .map(o -> o.getVoucherType() == null ? null : o.getVoucherType().trim())
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);

        boolean isChequeToCheque = "1".equals(type) || "CHEQUE_TO_CHEQUE".equalsIgnoreCase(type);
        boolean isChequeToCash = "2".equals(type) || "CHEQUE_TO_CASH".equalsIgnoreCase(type);
        boolean isCashToCheque = "3".equals(type) || "CASH_TO_CHEQUE".equalsIgnoreCase(type);
        boolean isChequeToImcoCheque = "4".equals(type) || "CHEQUE_TO_IMCOCHEQUE".equalsIgnoreCase(type);
        boolean isChequeToBank = "5".equals(type) || "CHEQUE_TO_BANK".equalsIgnoreCase(type);

        if (!isChequeToCheque && !isChequeToCash && !isCashToCheque && !isChequeToImcoCheque && !isChequeToBank) {
            throw new ValidationException("Invalid cheque conversion type. Use 1-5 or CHEQUE_TO_CHEQUE, CHEQUE_TO_CASH, CASH_TO_CHEQUE, CHEQUE_TO_IMCOCHEQUE, CHEQUE_TO_BANK");
        }

        if (isChequeToCheque) {
            if (dto.getInDtls() == null || dto.getInDtls().isEmpty()) {
                throw new ValidationException("No Detail present in cheque conversion TO");
            }
            for (GlChequeCashConvertInDtlDto in : dto.getInDtls()) {
                String inVt = in.getVoucherType() == null ? null : in.getVoucherType().trim();
                if (StringUtils.isBlank(inVt) || StringUtils.isBlank(outVoucherType)) {
                    throw new ValidationException("Voucher Type should be same..");
                }
                if (!inVt.equalsIgnoreCase(outVoucherType)) {
                    throw new ValidationException("Voucher Type should be same..");
                }
            }
            BigDecimal inTotal = dto.getInDtls().stream()
                    .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (inTotal.compareTo(outTotal) != 0) {
                throw new ValidationException(
                        "Total cheque amount (" + outTotal + ") is not matched with converted cheque amount (" + inTotal + ")");
            }
        }

        if (isChequeToBank) {
            if (dto.getInDtls() == null || dto.getInDtls().isEmpty()) {
                throw new ValidationException("No Detail present in cheque conversion TO");
            }
            BigDecimal inTotal = dto.getInDtls().stream()
                    .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (inTotal.compareTo(outTotal) != 0) {
                throw new ValidationException(
                        "Total cheque amount (" + outTotal + ") is not matched with converted bank amount (" + inTotal + ")");
            }
        }

        if (isChequeToImcoCheque) {
            if (dto.getInDtls() == null || dto.getInDtls().isEmpty()) {
                throw new ValidationException("No Detail present in cheque conversion TO");
            }
            for (GlChequeCashConvertInDtlDto in : dto.getInDtls()) {
                String inVt = in.getVoucherType() == null ? null : in.getVoucherType().trim();
                if (StringUtils.isBlank(inVt) && StringUtils.isBlank(outVoucherType)) {
                    throw new ValidationException("Voucher Type should be empty");
                }
                if (StringUtils.isNotBlank(inVt) && !"IMCOCHEQUE".equalsIgnoreCase(inVt)) {
                    throw new ValidationException("Voucher Type should be IMCOCHEQUE");
                }
            }
            BigDecimal inTotal = dto.getInDtls().stream()
                    .map(i -> Optional.ofNullable(i.getAmount()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (inTotal.compareTo(outTotal) != 0) {
                throw new ValidationException(
                        "Total cheque amount (" + outTotal + ") is not matched with converted cheque amount (" + inTotal + ")");
            }
        }

        if (isChequeToCash) {
            if (dto.getCash() == null) {
                throw new ValidationException("Please enter Cash amount");
            }
            BigDecimal cashAmount = dto.getCash();
            BigDecimal rounding = Optional.ofNullable(dto.getRoundingAmt()).orElse(BigDecimal.ZERO);
            BigDecimal totalCash = cashAmount.add(rounding);
            if (outTotal.compareTo(totalCash) != 0) {
                throw new ValidationException(
                        "Total cheque amount (" + outTotal + ") is not matched with converted cash amount (" + totalCash + ")");
            }
        }

        if (isCashToCheque) {

            if (dto.getInDtls() == null || dto.getInDtls().isEmpty()) {
                throw new ValidationException("No Detail present in cheque conversion TO");
            }

            BigDecimal chequeTotal = dto.getInDtls().stream()
                    .map(i -> Optional.ofNullable(i.getAmount()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cashReceived = Optional.ofNullable(dto.getCash()).orElse(BigDecimal.ZERO);

            BigDecimal calculated = chequeTotal.add(cashReceived);

            BigDecimal difference = outTotal.subtract(calculated).abs();

            BigDecimal roundingLimit = getRoundingLimitParam();

            if (difference.compareTo(roundingLimit) > 0) {
                throw new ValidationException(
                        "Cash Amount (" + outTotal + ") and Cheque Amount (" + chequeTotal + ") are not matching...");
            }

            dto.setRoundingAmt(difference);
        }

        validateChequeDatesForInDetails(dto.getInDtls());
        validateRoundingAmount(dto.getRoundingAmt());
    }

    private BigDecimal getRoundingLimitParam() {
        String val = globalParameterService.getParameterValue("ROUNDING_LIMIT", "GROUP", "1", "0.099");
        try {
            return new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            return new BigDecimal("0.099");
        }
    }

    private void validateRoundingAmount(BigDecimal roundingAmt) {
        if (roundingAmt == null) {
            return;
        }
        BigDecimal limit = getRoundingLimitParam();
        if (roundingAmt.compareTo(limit) > 0) {
            throw new ValidationException("RoundingAmount is greater than " + limit);
        }
    }

    /**
     * Legacy: CHEQUE_DATE_VALIDATION_DAYS (e.g. -30). Cheque date must not be earlier than (today + param days).
     * NoOfDays = (ChqDate - Sysdate) in days; if NoOfDays < ValidateDays then error.
     */
    private void validateChequeDatesForInDetails(List<GlChequeCashConvertInDtlDto> inDtls) {
        if (inDtls == null || inDtls.isEmpty()) {
            return;
        }
        String validateDaysStr = globalParameterService.getParameterValue("CHEQUE_DATE_VALIDATION_DAYS", "GROUP", "1", "1");
        BigDecimal validateDaysBd;
        try {
            validateDaysBd = new BigDecimal(validateDaysStr.trim());
        } catch (NumberFormatException e) {
            validateDaysBd = BigDecimal.ONE;
        }
        LocalDate today = LocalDate.now();
        for (GlChequeCashConvertInDtlDto in : inDtls) {
            if (in.getChqDate() == null) {
                continue;
            }
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, in.getChqDate());
            if (new BigDecimal(daysDiff).compareTo(validateDaysBd) < 0) {
                throw new ValidationException("Cheque Date should be less than " + validateDaysBd.abs() + " days...");
            }
        }
    }

    private void validateStatusForEdit(Long transactionPoid) {

        String status = glChequeCashConvertRepository.checkChequeConvertStatus(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                transactionPoid,
                UserContext.getDocumentId(),
                UserContext.getUserPoid(),
                getCurrentUser()
        );

        if (status != null) {

            if (status.contains("ERROR")) {
                throw new ValidationException("Some error occured in PROC_CHEQUE_CONVERT_STATUS_CHK : " + status);
            }

            if (status.contains("INFO")) {
                throw new ValidationException("Cheque/cash is not in PENDING status, not allowed to edit..");
            }
        }
    }

}





