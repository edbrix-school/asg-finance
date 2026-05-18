package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.entity.*;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.dto.*;
import com.asg.finance.repository.ArCreditNoteChargeDtlRepository;
import com.asg.finance.repository.ArCreditNoteDtlRepository;
import com.asg.finance.repository.ArCreditNoteHdrRepository;
import com.asg.finance.repository.BankPaymentVoucherSpRepository;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.ChargeLovService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.CreditNoteService;
import com.asg.finance.service.GlPostingService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

import oracle.jdbc.OracleTypes;
import com.asg.common.lib.exception.ValidationException;

@Service
@Slf4j
public class CreditNoteServiceImpl implements CreditNoteService {

    @Autowired
    private ArCreditNoteHdrRepository creditNoteHdrRepository;

    @Autowired
    private ArCreditNoteDtlRepository creditNoteDtlRepository;

    @Autowired
    private ArCreditNoteChargeDtlRepository creditNoteChargeDtlRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private ChargeLovService chargeLovService;

    @Autowired
    private BillwiseBreakupService billwiseBreakupService;

    @Autowired
    private CostCenterBreakupService costCenterBreakupService;

    @Autowired
    private BankPaymentVoucherSpRepository bankPaymentVoucherSpRepository;

    @Autowired
    private GLMasterRepository glMasterRepository;

    @Autowired
    private TaxMasterRepository taxMasterRepository;

    @Autowired
    private DocumentSearchService documentService;

    @Autowired
    private DocumentDeleteService documentDeleteService;

    @Autowired
    private LovDataService lovService;

    @Autowired
    private PrintService printService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private GlobalLogSummaryRepository globalLogSummaryRepository;

    @Autowired
    private GlPostingService glPostingService;

    @Autowired
    private GlobalParameterService globalParameterService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @PerformGlPosting
    public CreditNoteHeaderDto createCreditNote(CreditNoteHeaderDto creditNoteDto) {
        try {
            filterUnselectedCharges(creditNoteDto);
            executeBeforeSaveValidation(creditNoteDto);
            calculateDueDateFromCreditPeriod(creditNoteDto);
            // Save header and flush immediately
            ArCreditNoteHdr header = mapToEntity(creditNoteDto);
            String currentUser = ASGHelperUtils.getCurrentUser();
            LocalDateTime now = LocalDateTime.now();

            header.setGroupPoid(1L);
            header.setCompanyPoid(UserContext.getCompanyPoid());
            header.setTransactionDate(creditNoteDto.getTransactionDate());
            header.setCreatedBy(currentUser);
            header.setCreatedDate(now);
            header.setLastModifiedBy(currentUser);
            header.setLastModifiedDate(now);

            ArCreditNoteHdr savedHeader = creditNoteHdrRepository.saveAndFlush(header);
            entityManager.flush();
            entityManager.refresh(savedHeader);
            creditNoteDto.setDocRef(savedHeader.getDocRef());
            DocumentBeforeSaveBillwiseCostGroups(creditNoteDto);

            ArCreditNoteHdr reloadedHeader = creditNoteHdrRepository.findById(savedHeader.getTransactionPoid())
                    .orElseThrow(() -> new ValidationException("Header not found after insert (trigger modified it)"));

            Long transactionPoid = reloadedHeader.getTransactionPoid();
            if (transactionPoid == null) {
                throw new ValidationException("Database trigger failed to generate TRANSACTION_POID");
            }

            log.info("Credit note header saved with transactionPoid: {}", transactionPoid);

            // Log the header creation first, before any child record processing
            loggingService.createLogSummaryEntry(
                    UserContext.getDocumentId(),
                    savedHeader.getTransactionPoid().toString(),
                    String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedHeader.getDocRef())
            );

            // Save GL details immediately after header to avoid FK constraint issues
            if (creditNoteDto.getGlDetails() != null) {
                log.info("Starting to save GL details for transactionPoid: {}", transactionPoid);
                String issueType = creditNoteDto.getIssueType() != null ? creditNoteDto.getIssueType() : "Y";
                saveGLDetailsWithIssueType(transactionPoid, creditNoteDto.getGlDetails(), issueType);
                entityManager.flush();
                log.info("GL details saved successfully for transactionPoid: {}", transactionPoid);
            }

            try {
                entityManager.flush();
                if (creditNoteDto.getRefType() != null && !"GENERAL".equals(creditNoteDto.getRefType())) {
                    executeReferenceBasedSPs(transactionPoid, creditNoteDto.getRefType(), creditNoteDto);
                }

                if (creditNoteDto.getChargeDetails() != null) {
                    saveChargeDetails(transactionPoid, creditNoteDto.getChargeDetails());
                    entityManager.flush();
                    executeChargeTaxIfChanged(transactionPoid, creditNoteDto);
                }
                //executePostSaveUpdatesAfterCommit(transactionPoid, creditNoteDto);
            } catch (Exception e) {
                log.error("Error in post-save processing for transactionPoid {}: {}", transactionPoid, e.getMessage());
                throw new ValidationException("Post-save processing failed: " + e.getMessage());
            }

            CreditNoteHeaderDto result = mapToDto(reloadedHeader);

            List<ArCreditNoteDtl> glDetails = creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
            List<CreditNoteGLDetailDto> glDetailDtos = glDetails.stream().map(this::mapGLToDto).collect(Collectors.toList());

            saveBillwiseForGl(transactionPoid, creditNoteDto.getGlDetails(), "300-111", false);
            saveCostCenterForGl(transactionPoid, creditNoteDto.getGlDetails(), "300-111", false);
            executePostSaveUpdatesAfterCommit(transactionPoid, creditNoteDto);

            loadBillwiseAndCostCenterBreakup(glDetailDtos, transactionPoid, "300-111");
            result.setGlDetails(glDetailDtos);

            List<ArCreditNoteChargeDtl> chargeDetails = creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
            result.setChargeDetails(chargeDetails.stream().map(charge -> mapChargeToDto(charge, result.getRefType())).collect(Collectors.toList()));

            // Log child record creation after all processing is complete
            List<GlobalLogSummary> detailCreateLogs = buildCreateDetailSummaryLogs(creditNoteDto, transactionPoid);
            if (!detailCreateLogs.isEmpty()) {
                globalLogSummaryRepository.saveAll(detailCreateLogs);
            }

            return result;

        } catch (SQLException e) {
            throw new ValidationException("Database error: " + e.getMessage());
        }
    }

    @Override
    public CreditNoteHeaderDto getCreditNoteById(Long transactionPoid) {
        ArCreditNoteHdr header = creditNoteHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Credit Note", "transactionPoid", transactionPoid));

        CreditNoteHeaderDto dto = mapToDto(header);

        if (header.getRefType() != null) {
            executePrefillSPs(dto, header.getRefType());
        }

        List<ArCreditNoteDtl> glDetails = creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<CreditNoteGLDetailDto> glDetailDtos = glDetails.stream().map(this::mapGLToDto).collect(Collectors.toList());

        loadBillwiseAndCostCenterBreakup(glDetailDtos, transactionPoid, "300-111");
        dto.setGlDetails(glDetailDtos);

        List<ArCreditNoteChargeDtl> chargeDetails = creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        dto.setChargeDetails(chargeDetails.stream().map(charge -> mapChargeToDto(charge, dto.getRefType())).collect(Collectors.toList()));

        return dto;
    }

    @Override
    public Map<String, Object> listCreditNotes(String docId, FilterRequestDto filters, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDateValue, endDateValue);

        RawSearchResult raw = documentService.search(docId, filterList, operator, pageable, isDeleted,
                "LONG_NARRATION",   // label
                "TRANSACTION_POID");    // value

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void validateProcedureResult(String result) {
        if (result != null && result.startsWith("WARNING")) {
            String warningMessage = result.substring(result.indexOf(":") + 1).trim();
            throw new ValidationException(warningMessage);
        }
    }

    @Override
    @Transactional
    @PerformGlPosting
    public CreditNoteHeaderDto updateCreditNote(Long transactionPoid, CreditNoteHeaderDto creditNoteDto) {
        ArCreditNoteHdr existing = creditNoteHdrRepository.findByTransactionPoidAndDeleted(transactionPoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Credit Note", "transactionPoid", transactionPoid));

        try {
            filterUnselectedCharges(creditNoteDto);
            executeBeforeSaveValidation(creditNoteDto);
            DocumentBeforeSaveBillwiseCostGroups(creditNoteDto);
            calculateDueDateFromCreditPeriod(creditNoteDto);

            // Create a copy of the old entity for logging and old-ref tracking
            ArCreditNoteHdr oldEntity = new ArCreditNoteHdr();
            BeanUtils.copyProperties(existing, oldEntity);
            String oldFdaRef = oldEntity.getFdaRef();
            String oldFfRef = oldEntity.getFfRef();

            updateHeaderFromDto(existing, creditNoteDto);
            existing.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
            existing.setLastModifiedDate(LocalDateTime.now());

            existing = creditNoteHdrRepository.saveAndFlush(existing);

            List<GlobalLogSummary> detailSummaryLogs = new ArrayList<>();
            if (creditNoteDto.getGlDetails() != null) {
                updateGLDetailsWithLogging(transactionPoid, creditNoteDto.getGlDetails(), detailSummaryLogs);
            }

            if (creditNoteDto.getChargeDetails() != null) {
                updateChargeDetailsWithLogging(transactionPoid, creditNoteDto.getChargeDetails(), detailSummaryLogs, creditNoteDto);
            }
            saveBillwiseForGl(transactionPoid, creditNoteDto.getGlDetails(), "300-111", true);
            saveCostCenterForGl(transactionPoid, creditNoteDto.getGlDetails(), "300-111", true);
            entityManager.flush();
            executePostSaveUpdates(transactionPoid, creditNoteDto);
            entityManager.flush();  // As We are commenting flush from executePostSaveUpdates so to keep the usecase same in case of Edit, adding here
            // Execute post-commit tax recalculation and reference updates
            executePostCommitTaxUpdates(transactionPoid, creditNoteDto, oldFdaRef, oldFfRef, existing);

            CreditNoteHeaderDto result = mapToDto(existing);
            List<ArCreditNoteDtl> glDetails = creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
            List<CreditNoteGLDetailDto> glDetailDtos = glDetails.stream().map(this::mapGLToDto).collect(Collectors.toList());
            loadBillwiseAndCostCenterBreakup(glDetailDtos, transactionPoid, "300-111");
            result.setGlDetails(glDetailDtos);

            List<ArCreditNoteChargeDtl> chargeDetails = creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
            result.setChargeDetails(chargeDetails.stream().map(charge -> mapChargeToDto(charge, result.getRefType())).collect(Collectors.toList()));

            // Log the update
            loggingService.logChanges(oldEntity, existing, ArCreditNoteHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
            if (!detailSummaryLogs.isEmpty()) {
                globalLogSummaryRepository.saveAll(detailSummaryLogs);
            }
            return result;
        } catch (SQLException e) {
            log.error("Database error updating credit note", e);
            throw new ValidationException("Database error occurred while updating credit note");
        } catch (ValidationException e) {
            log.error("Unexpected error updating credit note", e);
            throw new ValidationException(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating credit note", e);
            throw new ValidationException("Failed to update credit note");
        }
    }

    @Override
    @Transactional
    public void deleteCreditNote(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        try {
            ArCreditNoteHdr existing = creditNoteHdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Credit Note", "transactionPoid", transactionPoid));

            documentDeleteService.deleteDocument(
                    transactionPoid,
                    "AR_CREDIT_NOTE_HDR",
                    "TRANSACTION_POID",
                    deleteReasonDto,
                    existing.getTransactionDate()
            );
        } catch (Exception e) {
            log.error("Error deleting credit note", e);
            throw new ValidationException("Failed to delete credit note: " + e.getMessage());
        }
    }

    @Override
    public List<UniversalChargeDetailDto> getFFInvoiceCharges(Long refNo, Long partyPoid) {
        try {
            return executeFFChargesFetchSP(refNo, partyPoid);
        } catch (ValidationException e) {
            log.error("Validation error fetching FF invoice charges for refNo {}: {}", refNo, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching FF invoice charges for refNo {}: {}", refNo, e.getMessage());
            throw new ValidationException("Failed to fetch FF invoice charges");
        }
    }

    @Override
    public List<UniversalChargeDetailDto> getSHInvoiceCharges(Long refNo, Long partyPoid) {
        try {
            return executeSHChargesFetchSP(refNo, partyPoid);
        } catch (ValidationException e) {
            log.error("Validation error fetching FF invoice charges for refNo {}: {}", refNo, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching SH invoice charges for refNo {}: {}", refNo, e.getMessage());
            throw new ValidationException("Failed to fetch SH invoice charges");
        }
    }

    private void populateRefFields(UniversalChargeDetailDto dto, ResultSet rs) throws SQLException {
        dto.setRefDocId(rs.getString("REF_DOC_ID"));
        dto.setRefDocPoid(rs.getLong("REF_DOC_POID"));
        dto.setFdaDetRowId(rs.getLong("FDA_DET_ROW_ID"));
    }

    private List<UniversalChargeDetailDto> executeFFChargesFetchSP(Long refNo, Long partyPoid) throws SQLException {

        String sql = "BEGIN PROC_AR_CREDIT_NT_FROM_FF_INV(?, ?, ?, ?, ?, ?, ?); END;";
        List<UniversalChargeDetailDto> charges = new ArrayList<>();

        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getGroupPoid());
            cs.setLong(2, UserContext.getCompanyPoid());
            cs.setLong(3, UserContext.getUserPoid());
            cs.setLong(4, refNo); // FF Invoice POID
            cs.setLong(5, partyPoid);
            cs.registerOutParameter(6, Types.VARCHAR);        // P_RESULT
            cs.registerOutParameter(7, OracleTypes.CURSOR);   // OUTDATA
            cs.execute();

            String result = cs.getString(6);
            validateProcedureResult(result);

            try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                while (rs != null && rs.next()) {
                    UniversalChargeDetailDto dto = new UniversalChargeDetailDto();
                    populateRefFields(dto, rs);
                    dto.setChargePoid(rs.getLong("CHARGE_POID"));
                    dto.setChargeAmount(rs.getBigDecimal("INV_AMOUNT"));
                    dto.setChargeCostAmount(rs.getBigDecimal("CHARGE_COST_AMOUNT"));
                    dto.setTaxPoid(rs.getLong("TAX_POID"));
                    dto.setTaxPercentage(rs.getBigDecimal("TAX_PERCENTAGE"));
                    dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
                    dto.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
                    dto.setRemarks(rs.getString("REMARKS"));
                    dto.setIssueInvoice("N");

                    dto.setChargeDet(lovService.getDetailsByPoidAndLovNameFast(dto.getChargePoid(), "CHARGE_MASTER_ALL"));
                    dto.setTaxDet(taxMasterRepository.findByTaxPoid(dto.getTaxPoid())
                            .map(tm -> new LovGetListDto(tm.getTaxPoid(), tm.getTaxCode(), tm.getTaxName(), tm.getTaxPoid(), tm.getTaxName(), tm.getSeqNo(), null))
                            .orElse(null));
                    charges.add(dto);
                }
            }
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
        return charges;
    }

    private List<UniversalChargeDetailDto> executeSHChargesFetchSP(Long refNo, Long partyPoid) throws SQLException {

        String sql = "BEGIN PROC_AR_CREDIT_NT_FROM_SH_INV(?, ?, ?, ?, ?, ?, ?); END;";
        List<UniversalChargeDetailDto> charges = new ArrayList<>();
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);

            cs.setLong(1, UserContext.getGroupPoid());
            cs.setLong(2, UserContext.getCompanyPoid());
            cs.setLong(3, UserContext.getUserPoid());
            cs.setLong(4, refNo);
            cs.setLong(5, partyPoid);
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.registerOutParameter(7, OracleTypes.CURSOR);
            cs.execute();

            String result = cs.getString(6);
            validateProcedureResult(result);

            try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                while (rs != null && rs.next()) {
                    /**
                     * Available Columns in ResultSet
                     * REF_DOC_ID, REF_DOC_POID, FDA_DET_ROW_ID
                     * CHARGE_POID, INV_AMOUNT, TAX_AMOUNT, TAX_POID, TAX_PERCENTAGE, TOTAL_AMOUNT, REMARKS
                     */
                    UniversalChargeDetailDto dto = new UniversalChargeDetailDto();
                    populateRefFields(dto, rs);
                    dto.setChargePoid(rs.getLong("CHARGE_POID"));
                    dto.setChargeAmount(rs.getBigDecimal("INV_AMOUNT"));
                    dto.setChargeCostAmount(rs.getBigDecimal("CHARGE_COST_AMOUNT"));
                    dto.setTaxPoid(rs.getLong("TAX_POID"));
                    dto.setTaxPercentage(rs.getBigDecimal("TAX_PERCENTAGE"));
                    dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
                    dto.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
                    dto.setRemarks(rs.getString("REMARKS"));
                    dto.setIssueInvoice("N");

                    dto.setChargeDet(lovService.getDetailsByPoidAndLovNameFast(dto.getChargePoid(), "CHARGE_MASTER_ALL"));
                    dto.setTaxDet(taxMasterRepository.findByTaxPoid(dto.getTaxPoid())
                            .map(tm -> new LovGetListDto(tm.getTaxPoid(), tm.getTaxCode(), tm.getTaxName(), tm.getTaxPoid(), tm.getTaxName(), tm.getSeqNo(), null))
                            .orElse(null));
                    charges.add(dto);
                }
            }
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
        return charges;
    }

    @Override
    public List<UniversalChargeDetailDto> getDNInvoiceCharges(Long refNo, Long partyPoid) {
        try {
            return executeDNChargesSP(refNo, partyPoid);
        } catch (ValidationException e) {
            log.error("Validation error fetching FF invoice charges for refNo {}: {}", refNo, e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch DN invoice charges");
        }
    }


    @Override
    public List<UniversalChargeDetailDto> getFDADetails(Long fdaRef, Long partyPoid) {
        try {
            return executeFDADetailsSP(fdaRef, partyPoid);
        } catch (ValidationException e) {
            log.error("Validation error fetching FF invoice charges for refNo {}: {}", fdaRef, e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch FDA details");
        }
    }


    private List<UniversalChargeDetailDto> executeDNChargesSP(Long refNo, Long partyPoid) throws SQLException {

        String sql = "BEGIN PROC_AR_CN_CREATE_FROM_DN(?, ?, ?, ?, ?, ?, ?); END;";
        List<UniversalChargeDetailDto> charges = new ArrayList<>();
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);

            cs.setLong(1, UserContext.getGroupPoid());
            cs.setLong(2, UserContext.getCompanyPoid());
            cs.setLong(3, UserContext.getUserPoid());
            cs.setLong(4, refNo);
            cs.setLong(5, partyPoid);
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.registerOutParameter(7, OracleTypes.CURSOR);

            cs.execute();

            String result = cs.getString(6);
            validateProcedureResult(result);

            try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                while (rs != null && rs.next()) {
                    UniversalChargeDetailDto dto = new UniversalChargeDetailDto();
                    populateRefFields(dto, rs);
                    dto.setChargePoid(rs.getLong("CHARGE_POID"));
                    dto.setChargeAmount(rs.getBigDecimal("CHARGE_AMOUNT"));
                    dto.setChargeCostAmount(rs.getBigDecimal("CHARGE_COST_AMOUNT"));
                    dto.setTaxPoid(rs.getLong("TAX_POID"));
                    dto.setTaxPercentage(rs.getBigDecimal("TAX_PERCENTAGE"));
                    dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
                    dto.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
                    dto.setRemarks(rs.getString("REMARKS"));
                    dto.setIssueInvoice("N");
                    dto.setChargeDet(lovService.getDetailsByPoidAndLovNameFast(dto.getChargePoid(), "CHARGE_MASTER_ALL"));
                    dto.setTaxDet(taxMasterRepository.findByTaxPoid(dto.getTaxPoid())
                            .map(tm -> new LovGetListDto(tm.getTaxPoid(), tm.getTaxCode(), tm.getTaxName(), tm.getTaxPoid(), tm.getTaxName(), tm.getSeqNo(), null))
                            .orElse(null));
                    charges.add(dto);
                }
            }
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
        return charges;
    }

    private List<UniversalChargeDetailDto> executeFDADetailsSP(Long fdaRef, Long partyPoid) throws SQLException {

        String sql = "BEGIN PROC_AR_CREDIT_CREATE_FROM_FDA(?, ?, ?, ?, ?, ?, ?); END;";
        List<UniversalChargeDetailDto> charges = new ArrayList<>();
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);

            cs.setLong(1, UserContext.getGroupPoid());
            cs.setLong(2, UserContext.getCompanyPoid());
            cs.setLong(3, UserContext.getUserPoid());
            cs.setString(4, String.valueOf(fdaRef));
            cs.setString(5, String.valueOf(partyPoid));
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.registerOutParameter(7, OracleTypes.CURSOR);

            cs.execute();

            String result = cs.getString(6);
            validateProcedureResult(result);

            try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                while (rs != null && rs.next()) {

                    UniversalChargeDetailDto dto = new UniversalChargeDetailDto();
                    populateRefFields(dto, rs);
                    dto.setChargePoid(rs.getLong("CHARGE_POID"));
                    //dto.setChargeAmount(rs.getBigDecimal("CHARGE_AMOUNT"));
                    dto.setTaxPoid(rs.getLong("TAX_POID"));
                    dto.setTaxPercentage(rs.getBigDecimal("TAX_PERCENTAGE"));
                    //dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
                    //dto.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
                    dto.setRemarks(rs.getString("REMARKS"));
                    dto.setPdaAmount(rs.getBigDecimal("PDA_AMOUNT"));
                    dto.setIssueInvoice("N");
                    charges.add(dto);
                }
            }
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
        return charges;
    }


    private void executeBeforeSaveValidation(CreditNoteHeaderDto dto) throws SQLException {
        validateMandatoryFields(dto);
        validateMultiCompanyFields(dto);
        validateGrandTotalWithCharges(dto);
        validateCreditPeriod(dto);

        // Call GL voucher validation to check reference document status
        executeGLVoucherValidation(dto);

        // Calculate total tax amount from detail rows (matches PageBean: sums TaxAmount per refType)
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        if (dto.getChargeDetails() != null) {
            totalTaxAmount = dto.getChargeDetails().stream()
                    .map(c -> c.getTaxAmount() != null ? c.getTaxAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if (dto.getGlDetails() != null) {
            totalTaxAmount = dto.getGlDetails().stream()
                    .map(gl -> gl.getTaxAmount() != null ? gl.getTaxAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Call stored procedure validation
        String sql = "BEGIN PROC_AR_CN_BEFORE_SAVE_VAL(?, ?, ?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            LocalDate txnDate = dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now();

            cs.setLong(1, groupPoid);
            cs.setLong(2, companyPoid);
            cs.setLong(3, userPoid);
            cs.setString(4, "300-111");
            cs.setLong(5, dto.getTransactionPoid() != null ? dto.getTransactionPoid() : 0);
            cs.setLong(6, dto.getPartyPoid() != null ? dto.getPartyPoid() : 0);
            cs.setBigDecimal(7, totalTaxAmount); // TaxAmount sum (not grandTotal)
            cs.setTimestamp(8, Timestamp.valueOf(txnDate.atStartOfDay()));
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();

            String result = cs.getString(9);
            if (result != null && result.contains("ERROR")) {
                throw new ValidationException(result);
            }
            if (result != null && result.contains("WARNING")) {
                throw new ValidationException(result.substring(result.indexOf(":") + 1).trim());
            }
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void validateMandatoryFields(CreditNoteHeaderDto dto) {
        // Reference-specific validations
        if ("FF_INVOICE".equals(dto.getRefType()) && dto.getFfInvoicePoid() == null) {
            throw new ValidationException("FF Invoice Reference is mandatory for FF_INVOICE type");
        }
        if ("SH_INVOICE".equals(dto.getRefType()) && dto.getShInvoicePoid() == null) {
            throw new ValidationException("SH Invoice Reference is mandatory for SH_INVOICE type");
        }
        if ("DN_INVOICE".equals(dto.getRefType()) && dto.getDnInvoicePoid() == null) {
            throw new ValidationException("DN Invoice Reference is mandatory for DN_INVOICE type");
        }
        if ("FDA".equals(dto.getRefType()) && dto.getDnFdaReference() == null) {
            throw new ValidationException("DN FDA Reference is mandatory for FDA type");
        }

        // Validate that at least one detail row exists (matches PageBean DocumentBeforeSave check)
        String refType = dto.getRefType();
        if ("GENERAL".equals(refType) || "CUSTOM".equals(refType)) {
            if (dto.getGlDetails() == null || dto.getGlDetails().isEmpty()) {
                throw new ValidationException("No Details in this Transaction.");
            }
        } else if ("FDA".equals(refType) || "FF".equals(refType) || "SH_INVOICE".equals(refType)
                || "FF_INVOICE".equals(refType) || "DN_INVOICE".equals(refType) || "VOYAGE".equals(refType)) {
            if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
                throw new ValidationException("No Details in this Transaction.");
            }
        }
    }

    private void validateCreditPeriod(CreditNoteHeaderDto dto) {
        if (dto.getCreditPeriod() == null) return;
        long maxDays = getCreditPeriodValidationDays();
        if (dto.getCreditPeriod() > maxDays) {
            throw new ValidationException("Credit Period is greater than " + maxDays + " days.");
        }
    }

    private long getCreditPeriodValidationDays() {
        try {
            String value = globalParameterService.getParameterValue("CREDIT_PERIOD_VALIDATION_DAYS", "GROUP", "1", "120");
            return Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Failed to get CREDIT_PERIOD_VALIDATION_DAYS parameter: {}", e.getMessage());
        }
        return 120L;
    }

    private void validateMultiCompanyFields(CreditNoteHeaderDto dto) {
        if (Boolean.TRUE.equals(dto.getMultiCompany())) {
            if (dto.getGlDetails() != null) {
                for (var glDetail : dto.getGlDetails()) {
                    if (glDetail.getCompanyPoid() == null) {
                        throw new ValidationException("Company field is mandatory for each GL detail when Multi Company is selected");
                    }
                }
            }
        }
    }

    private void validateTaxFields(CreditNoteHeaderDto dto) {
        if (dto.getGlDetails() != null && !dto.getGlDetails().isEmpty()) {
            for (int i = 0; i < dto.getGlDetails().size(); i++) {
                var glDetail = dto.getGlDetails().get(i);
                if (glDetail.getTaxPoid() == null) {
                    throw new ValidationException("Tax Poid is mandatory for GL Detail row " + (i + 1));
                }
            }
        }

        if (dto.getChargeDetails() != null && !dto.getChargeDetails().isEmpty()) {
            for (int i = 0; i < dto.getChargeDetails().size(); i++) {
                var chargeDetail = dto.getChargeDetails().get(i);
                if (chargeDetail.getTaxPoid() == null) {
                    throw new ValidationException("Tax Poid is mandatory for Charge Detail row " + (i + 1));
                }
            }
        }
    }

    private void validateGrandTotalWithCharges(CreditNoteHeaderDto dto) {

        if (dto.getBhdAmount() == null) return;
        if (dto.getChargeDetails() != null && !dto.getChargeDetails().isEmpty()) {
            BigDecimal totalCharges = BigDecimal.ZERO;
            totalCharges = dto.getChargeDetails().stream()
                    .map(c -> c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (dto.getBhdAmount().compareTo(totalCharges) != 0) {
                throw new ValidationException(
                        "Grand Total is not matching with total charge amount."
                );
            }

        } else if (dto.getGlDetails() != null && !dto.getGlDetails().isEmpty()) {
            BigDecimal totalDebitAmt = BigDecimal.ZERO;
            BigDecimal totalCreditAmt = BigDecimal.ZERO;
            totalDebitAmt = dto.getGlDetails().stream()
                    .map(gl -> gl.getTotalAmount() != null && gl.getType().equals("DR") ? gl.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalCreditAmt = dto.getGlDetails().stream()
                    .map(gl -> gl.getTotalAmount() != null && gl.getType().equals("CR") ? gl.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

//            if (totalDebitAmt.compareTo(totalCreditAmt) != 0) {
//                throw new ValidationException("Total Debit ("+totalDebitAmt+") Amounts and Credit ("+totalCreditAmt+") Amounts are not tallying.");
//            }
            if (dto.getBhdAmount().compareTo(totalDebitAmt) != 0) {
                throw new ValidationException("Paid Amount (" + dto.getBhdAmount() + ") is not matching with total party credit amount(" + totalDebitAmt + ")");
            }
        }
    }

    public DefaultCreditValuesDto getDefaultCreditValues(Long partyPoid, String partyType) {
        try {
            return executeSetDefaultCredit(partyPoid, partyType);
        } catch (SQLException e) {
            throw new ValidationException("Failed to get default credit values: " + e.getMessage());
        }
    }

    @Override
    public Long getPartyGLPoid(Long partyPoid, String partyType) {
        try {
            return executeGetPartyGLPoid(partyPoid, partyType);
        } catch (SQLException e) {
            throw new ValidationException("Failed to get party GL POID: " + e.getMessage());
        }
    }

    private DefaultCreditValuesDto executeSetDefaultCredit(Long partyPoid, String partyType) throws SQLException {
        String lovName = getLovNameForPartyType(partyType);
        String sql = "BEGIN PROC_PI_SET_DEFAULT_CREDIT(?, ?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid); // P_LOGIN_USER_POID
            cs.setString(4, "300-111"); // P_DOC_ID
            cs.setLong(5, 0); // P_DOC_KEY_POID
            cs.setString(6, lovName); // P_LOV_NAME
            cs.setLong(7, partyPoid != null ? partyPoid : 0); // P_LOV_VALUE
            cs.registerOutParameter(8, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();

            DefaultCreditValuesDto.DefaultCreditValuesDtoBuilder builder = DefaultCreditValuesDto.builder();
            Integer creditPeriod = null;

            // Process result to set credit period, currency, TIN number, and bank details
            try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                if (rs != null && rs.next()) {
                    if (rs.getObject("CREDIT_PERIOD") != null) {
                        creditPeriod = rs.getInt("CREDIT_PERIOD");
                        builder.creditPeriod(creditPeriod);
                    }
                    if (rs.getObject("TIN_NUMBER") != null) {
                        builder.tinNumber(rs.getString("TIN_NUMBER"));
                    }
                    if (rs.getObject("CURRENCY_CODE") != null) {
                        builder.currencyCode(rs.getString("CURRENCY_CODE"));
                    }
                    if (rs.getObject("CURRENCY_RATE") != null) {
                        builder.currencyRate(rs.getBigDecimal("CURRENCY_RATE"));
                    }
                }
            }

            // Always calculate due date as per SRS: Due Date = Current Date + Credit Period
            if (creditPeriod != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DAY_OF_MONTH, creditPeriod);
                Timestamp dueDate = new Timestamp(cal.getTimeInMillis());
                builder.dueDate(dueDate);
                log.info("Due date calculated: {} (Current Date + {} days)", dueDate, creditPeriod);
            }

            return builder.build();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private String getLovNameForPartyType(String partyType) {
        switch (partyType != null ? partyType.toUpperCase() : "") {
            case "CUSTOMER":
                return "CUSTOMER_MASTER_FOR_DN";
            case "SUPPLIER":
                return "SUPPLIER_MASTER_FOR_DN";
            case "PRINCIPAL":
                return "PRINCIPAL_MASTER_FOR_DN";
            default:
                return "SUPPLIER_MASTER_FOR_DN";
        }
    }

    /**
     * Calculate due date: DueDate = TransactionDate + CreditPeriod (matches PageBean DocumentBeforeSave).
     * When credit period is null/0, DueDate = TransactionDate (0 days added).
     */
    private void calculateDueDateFromCreditPeriod(CreditNoteHeaderDto dto) {
        LocalDate baseDate = dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now();
        long days = (dto.getCreditPeriod() != null && dto.getCreditPeriod() > 0) ? dto.getCreditPeriod() : 0L;
        dto.setDueDate(baseDate.plusDays(days));
        log.info("Due date calculated: {} (TransactionDate {} + {} days)", dto.getDueDate(), baseDate, days);
    }

    private Long executeGetPartyGLPoid(Long partyPoid, String partyType) throws SQLException {
        String sql = "BEGIN PROC_GL_GET_DR_PARTY_GLPOID(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid); // P_LOGIN_USER_POID
            cs.setLong(4, partyPoid != null ? partyPoid : 0); // P_PARTY_POID
            cs.setString(5, partyType != null ? partyType : "SUPPLIER"); // P_PARTY_TYPE
            cs.registerOutParameter(6, Types.NUMERIC); // P_PARTY_GLPOID OUT
            cs.registerOutParameter(7, Types.VARCHAR); // P_CREDIT_PERIOD OUT
            cs.execute();

            Long partyGLPoid = cs.getLong(6);
            if (partyGLPoid == null || partyGLPoid == 0) {
                throw new ValidationException("Selected Party GL_CODE is not found.");
            }
            return partyGLPoid;
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeReferenceBasedSPs(Long transactionPoid, String refType, CreditNoteHeaderDto dto) throws SQLException {
        Long partyPoid = dto.getPartyPoid();

        switch (refType) {
            case "FF_INVOICE":
                if (dto.getFfInvoicePoid() != null) {
                    executeFFCreateSP(transactionPoid, dto.getFfInvoicePoid());
                }
                break;
            case "SH_INVOICE":
                if (dto.getShInvoicePoid() != null) {
                    executeSHCreateSP(transactionPoid, dto.getShInvoicePoid(), partyPoid);
                }
                break;
            case "DN_INVOICE":
                if (dto.getDnInvoicePoid() != null) {
                    executeDNCreateSP(transactionPoid, dto.getDnInvoicePoid(), partyPoid);
                }
                break;
            case "FDA":
                if (dto.getDnFdaReference() != null) {
                    executeFDACreateSP(transactionPoid, dto.getDnFdaReference(), partyPoid);
                }
                break;
        }
    }

    private void executeFFCreateSP(Long transactionPoid, Long ffInvoicePoid) throws SQLException {
        String sql = "BEGIN PROC_CR_NOTE_CREATE_FROM_FF(?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid); // P_LOGIN_USER_POID
            cs.setLong(4, ffInvoicePoid != null ? ffInvoicePoid : 0); // P_FF_POID
            cs.registerOutParameter(5, Types.VARCHAR); // P_RESULT OUT
            cs.registerOutParameter(6, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeSHCreateSP(Long transactionPoid, Long shInvoicePoid, Long partyPoid) throws SQLException {
        String sql = "BEGIN PROC_AR_CREDIT_NT_FROM_SH_INV(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();
            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid); // P_LOGIN_USER_POID
            cs.setLong(4, shInvoicePoid != null ? shInvoicePoid : 0); // P_INV_POID
            cs.setLong(5, partyPoid != null ? partyPoid : 0); // P_PARTY_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.registerOutParameter(7, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeDNCreateSP(Long transactionPoid, Long dnInvoicePoid, Long partyPoid) throws SQLException {
        String sql = "BEGIN PROC_AR_CN_CREATE_FROM_DN(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid); // P_LOGIN_USER_POID
            cs.setLong(4, dnInvoicePoid != null ? dnInvoicePoid : 0); // P_DN_POID
            cs.setLong(5, partyPoid != null ? partyPoid : 0); // P_PARTY_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.registerOutParameter(7, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeFDACreateSP(Long transactionPoid, String fdaRef, Long partyPoid) throws SQLException {
        String sql = "BEGIN PROC_AR_CREDIT_CREATE_FROM_FDA(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid); // P_LOGIN_USER_POID
            cs.setString(4, fdaRef != null ? fdaRef : "0"); // P_FDA_POID
            cs.setLong(5, partyPoid != null ? partyPoid : 0); // P_PRINCIPAL_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.registerOutParameter(7, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executePrefillSPs(CreditNoteHeaderDto dto, String refType) {
        try {
            if ("FF_INVOICE".equals(refType)) {
                executeFFPrefillSP(dto.getTransactionPoid());
            } else if ("SH_INVOICE".equals(refType)) {
                executeSHPrefillSP(dto.getTransactionPoid());
            } else if ("DN_INVOICE".equals(refType)) {
                executeDNPrefillSP(dto.getTransactionPoid());
            }
        } catch (SQLException e) {
            log.warn("Error executing prefill SP for refType {}: {}", refType, e.getMessage());
        }
    }

    private void executeFFPrefillSP(Long transactionPoid) throws SQLException {
        String sql = "BEGIN PROC_AR_CREDIT_NT_FROM_FF_INV(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, 1); // P_LOGIN_GROUP_POID
            cs.setLong(2, 1); // P_LOGIN_COMPANY_POID
            cs.setLong(3, 1); // P_LOGIN_USER_POID
            cs.setLong(4, 0); // P_INV_POID
            cs.setLong(5, 0); // P_PARTY_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.registerOutParameter(7, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeSHPrefillSP(Long transactionPoid) throws SQLException {
        String sql = "BEGIN PROC_AR_CREDIT_NT_FROM_SH_INV(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, 1); // P_LOGIN_GROUP_POID
            cs.setLong(2, 1); // P_LOGIN_COMPANY_POID
            cs.setLong(3, 1); // P_LOGIN_USER_POID
            cs.setLong(4, 0); // P_INV_POID
            cs.setLong(5, 0); // P_PARTY_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.registerOutParameter(7, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeDNPrefillSP(Long transactionPoid) throws SQLException {
        String sql = "BEGIN PROC_AR_CN_CREATE_FROM_DN(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, 1); // P_LOGIN_GROUP_POID
            cs.setLong(2, 1); // P_LOGIN_COMPANY_POID
            cs.setLong(3, 1); // P_LOGIN_USER_POID
            cs.setString(4, "0"); // P_DN_POID
            cs.setString(5, "0"); // P_PARTY_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.registerOutParameter(7, OracleTypes.CURSOR); // OUTDATA OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    /**
     * Recalculates tax for charge rows that differ from the reference invoice charges.
     * Fetches the reference charges based on refType (FF_INVOICE, SH_INVOICE, DN_INVOICE, FDA),
     * then for each incoming DTO row checks if chargePoid is new or chargeAmount changed vs reference.
     * For refTypes without a reference (GENERAL, CUSTOM, etc.), applies tax only for rows without taxPoid.
     */
    private void executeChargeTaxIfChanged(Long transactionPoid, CreditNoteHeaderDto dto) throws SQLException {
        if (!checkVATApplicability()) return;

        List<UniversalChargeDetailDto> incomingCharges = dto.getChargeDetails();
        if (incomingCharges == null || incomingCharges.isEmpty()) return;

        String refType = dto.getRefType();
        String partyType = dto.getPartyType();
        Long partyPoid = dto.getPartyPoid();

        // Fetch reference charge amounts keyed by chargePoid (null = no reference for this refType)
        Map<Long, BigDecimal> refAmountByChargePoid = fetchReferenceChargeAmounts(refType, dto);

        // Load saved DB rows (after saveChargeDetails, these reflect the latest state)
        List<ArCreditNoteChargeDtl> savedCharges = creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, ArCreditNoteChargeDtl> savedByDetRowId = savedCharges.stream()
                .filter(c -> c.getDetRowId() != null)
                .collect(Collectors.toMap(ArCreditNoteChargeDtl::getDetRowId, c -> c));
        Map<Long, ArCreditNoteChargeDtl> savedByChargePoid = savedCharges.stream()
                .filter(c -> c.getChargePoid() != null)
                .collect(Collectors.toMap(ArCreditNoteChargeDtl::getChargePoid, c -> c, (a, b) -> a));

        for (UniversalChargeDetailDto incoming : incomingCharges) {
            if (incoming.getChargePoid() == null) continue;

            boolean needsRecalc;
            if (refAmountByChargePoid != null) {
                // Reference-based: recalculate if chargePoid not in reference or amount differs
                BigDecimal refAmount = refAmountByChargePoid.get(incoming.getChargePoid());
                BigDecimal incomingAmount = getRefComparableAmount(incoming, refType);
                if (refAmount == null) {
                    // chargePoid not found in reference → new/custom charge → apply tax
                    needsRecalc = true;
                } else {
                    needsRecalc = incomingAmount == null || incomingAmount.compareTo(refAmount) != 0;
                }
            } else {
                // No reference (GENERAL, CUSTOM, etc.) → apply only if taxPoid not already set
                needsRecalc = (incoming.getTaxPoid() == null || incoming.getTaxPoid() == 0);
            }

            if (!needsRecalc) continue;

            // Resolve the saved DB entity to update with the new tax values
            ArCreditNoteChargeDtl charge = incoming.getDetRowId() != null
                    ? savedByDetRowId.get(incoming.getDetRowId())
                    : savedByChargePoid.get(incoming.getChargePoid());

            if (charge != null && charge.getChargeAmount() != null) {
                log.info("Applying changes to tax");
                applyChargeTaxFromSP(charge, partyType, partyPoid);
                entityManager.flush();
            }
        }
    }

    /**
     * Fetches reference charges from the appropriate SP based on refType.
     * Returns null if there is no invoice reference for the given refType.
     */
    private Map<Long, BigDecimal> fetchReferenceChargeAmounts(String refType, CreditNoteHeaderDto dto) throws SQLException {
        if (refType == null) return null;

        List<UniversalChargeDetailDto> refCharges = null;
        switch (refType.toUpperCase()) {
            case "FF_INVOICE":
                if (dto.getFfInvoicePoid() != null)
                    refCharges = executeFFChargesFetchSP(dto.getFfInvoicePoid(), dto.getPartyPoid());
                break;
            case "SH_INVOICE":
                if (dto.getShInvoicePoid() != null)
                    refCharges = executeSHChargesFetchSP(dto.getShInvoicePoid(), dto.getPartyPoid());
                break;
            case "DN_INVOICE":
                if (dto.getDnInvoicePoid() != null)
                    refCharges = executeDNChargesSP(dto.getDnInvoicePoid(), dto.getPartyPoid());
                break;
            case "FDA":
                if (dto.getFdaRefPoid() != null)
                    refCharges = executeFDADetailsSP(dto.getFdaRefPoid(), dto.getPartyPoid());
                break;
            default:
                return null;
        }

        if (refCharges == null) return null;

        Map<Long, BigDecimal> map = new HashMap<>();
        for (UniversalChargeDetailDto ref : refCharges) {
            if (ref.getChargePoid() == null) continue;
            BigDecimal amount = getRefComparableAmount(ref, refType);
            map.put(ref.getChargePoid(), amount);
        }
        return map;
    }

    /**
     * Returns the amount field used for reference comparison.
     * FDA uses pdaAmount (cost amount from FDA); all other invoice types use chargeAmount.
     */
    private BigDecimal getRefComparableAmount(UniversalChargeDetailDto dto, String refType) {
        if ("FDA".equalsIgnoreCase(refType)) return dto.getPdaAmount();
        return dto.getChargeAmount();
    }

    /**
     * Calls PROC_GET_CHARGE_TAX_PER_V3, updates the charge entity's tax fields, and saves it.
     */
    private void applyChargeTaxFromSP(ArCreditNoteChargeDtl charge, String partyType, Long partyPoid) throws SQLException {
        String sql = "BEGIN PROC_GET_CHARGE_TAX_PER_V3(?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getCompanyPoid());
            cs.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            cs.setString(3, partyType != null ? partyType : "SUPPLIER");
            cs.setLong(4, partyPoid != null ? partyPoid : 0);
            cs.setLong(5, charge.getChargePoid());
            cs.registerOutParameter(6, OracleTypes.CURSOR);
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(6)) {
                if (rs != null && rs.next()) {
                    charge.setTaxPercentage(rs.getBigDecimal("PERCENTAGE"));
                    charge.setTaxPoid(rs.getLong("TAX_POID"));
                    if (charge.getTaxPercentage() != null) {
                        BigDecimal taxAmount = charge.getChargeAmount()
                                .multiply(charge.getTaxPercentage().divide(new BigDecimal(100)))
                                .setScale(3, java.math.RoundingMode.HALF_UP);
                        charge.setTaxAmount(taxAmount);
                        charge.setTotalAmount(charge.getChargeAmount().add(taxAmount));
                    }
                } else {
                    charge.setTaxPercentage(BigDecimal.ZERO);
                    charge.setTaxPoid(0L);
                    charge.setTaxAmount(BigDecimal.ZERO);
                    charge.setTotalAmount(charge.getChargeAmount());
                }
                creditNoteChargeDtlRepository.save(charge);
            }
        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (SQLException ignore) {
                }
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private boolean checkVATApplicability() {
        try {
            String value = globalParameterService.getParameterValue("GLOBAL_TAX_APPLICABLE", "TAX", "1", "Y");
            return "Y".equals(value) || "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
        } catch (Exception e) {
            log.warn("Failed to check GLOBAL_TAX_APPLICABLE parameter: {}", e.getMessage());
        }
        return true; // Default to applicable if parameter not found
    }

    private Connection getTransactionalConnection() throws SQLException {
        return DataSourceUtils.getConnection(dataSource);
    }

    private void releaseTransactionalConnection(Connection connection) throws SQLException {
        DataSourceUtils.releaseConnection(connection, dataSource);
    }

    private void executePostSaveUpdatesAfterCommit(Long transactionPoid, CreditNoteHeaderDto dto) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        executePostSaveUpdates(transactionPoid, dto);
                    } catch (SQLException e) {
                        log.error("Error executing post-save updates for transactionPoid {}: {}", transactionPoid, e.getMessage());
                    }
                }
            });
        } else {
            try {
                executePostSaveUpdates(transactionPoid, dto);
            } catch (SQLException e) {
                log.error("Error executing post-save updates for transactionPoid {}: {}", transactionPoid, e.getMessage());
            }
        }
    }

    private void executePostSaveUpdates(Long transactionPoid, CreditNoteHeaderDto dto) throws SQLException {
        String refType = dto.getRefType();

        // Flush pending JPA changes first so stored procedures can see current data within the same transaction
        try {
            entityManager.flush();
        } catch (Exception e) {
            log.warn("Unable to flush entity manager before post-save SP updates: {}", e.getMessage());
        }

        // PROC_DR_CR_BILL_REF_UPDATE is only called for GENERAL type (matches PageBean DocumentAfterSave)
        if ("GENERAL".equals(refType)) {
            executeGLBillRefUpdate(transactionPoid, refType);
        }

        // Update manifest details based on reference type
        if ("FF_INVOICE".equals(refType)) {
            // FF_INVOICE: only update FF invoice details (NOT FF cost — that is for FF type)
            executeFFInvUpdate(transactionPoid, dto.getFfInvoicePoid());
        } else if ("FF".equals(refType)) {
            // FF type: update FF cost via PROC_CR_NOTE_UPDATE_FF_COST
            ArCreditNoteHdr hdr = creditNoteHdrRepository.findById(transactionPoid).orElse(null);
            Long ffPoid = null;
            if (hdr != null && hdr.getFfRef() != null) {
                try {
                    ffPoid = Long.parseLong(hdr.getFfRef());
                } catch (NumberFormatException e) {
                    log.warn("Could not parse ffRef '{}' as Long for transactionPoid {}", hdr.getFfRef(), transactionPoid);
                }
            }
            if (ffPoid != null) {
                executeFFCostUpdate(transactionPoid, ffPoid);
            }
        } else if ("SH_INVOICE".equals(refType)) {
            executeSHInvUpdate(transactionPoid, dto.getShInvoicePoid());
        } else if ("DN_INVOICE".equals(refType) || "DN".equals(refType)) {
            executeDNInvUpdate(transactionPoid, dto.getDnInvoicePoid());
        } else if ("FDA".equals(refType)) {
            executeFDAAmountUpdate(transactionPoid, dto.getDnFdaReference());
        }
        //entityManager.flush(); // Ensure all updates are flushed before tax recalculation
    }

    private void executePostCommitTaxUpdates(Long transactionPoid, CreditNoteHeaderDto creditNoteDto, 
                                              String oldFdaRef, String oldFfRef, ArCreditNoteHdr existing) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executePostCommitTaxAndReferenceUpdates(transactionPoid, creditNoteDto, oldFdaRef, oldFfRef, existing);
                }
            });
        } else {
            // Fallback if no transaction synchronization is active
            executePostCommitTaxAndReferenceUpdates(transactionPoid, creditNoteDto, oldFdaRef, oldFfRef, existing);
        }
    }

    private void executePostCommitTaxAndReferenceUpdates(Long transactionPoid, CreditNoteHeaderDto creditNoteDto,
                                                          String oldFdaRef, String oldFfRef, ArCreditNoteHdr existing) {
        try {
            log.info("Executing executeChargeTaxIfChanged post-commit for transactionPoid: {}", transactionPoid);
            executeChargeTaxIfChanged(transactionPoid, creditNoteDto);

            if ("FDA".equals(creditNoteDto.getRefType())
                    && oldFdaRef != null && !oldFdaRef.equals(creditNoteDto.getDnFdaReference())) {
                executeFDAAmountUpdate(transactionPoid, oldFdaRef);
            }

            // When FF ref changed, also update old FF cost (matches PageBean DocumentAfterSave)
            if ("FF".equals(creditNoteDto.getRefType())
                    && oldFfRef != null && !oldFfRef.equals(existing.getFfRef())) {
                try {
                    Long oldFfPoid = Long.parseLong(oldFfRef);
                    executeFFCostUpdate(transactionPoid, oldFfPoid);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse old ffRef '{}' as Long for transactionPoid {}", oldFfRef, transactionPoid);
                }
            }
        } catch (Exception e) {
            log.error("Error executing post-commit tax and reference updates for transactionPoid {}: {}", transactionPoid, e);
        }
    }

    private void executeGLVoucherValidation(CreditNoteHeaderDto dto) throws SQLException {
        String sql = "BEGIN PROC_GL_VOUCHERS_VALIDATIONS(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, userPoid); // P_LOGIN_USER_POID
            cs.setLong(3, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setString(4, "300-111"); // P_DOC_ID
            cs.setString(5, dto.getRefType() != null ? dto.getRefType() : "GENERAL"); // P_REF_TYPE
            cs.setString(6, getRefPoidForValidation(dto)); // P_REF_POID
            cs.registerOutParameter(7, Types.VARCHAR); // P_RESULT OUT
            cs.execute();

            String result = cs.getString(7);
            if ("CLOSED".equalsIgnoreCase(result) || "CANCELLED".equalsIgnoreCase(result)) {
                throw new ValidationException("Reference document is CLOSED or CANCELLED");
            }
            if (result != null && !"SUCCESS".equalsIgnoreCase(result)) {
                log.info("Reference document status: {}", result);
            }
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private String getRefPoidForValidation(CreditNoteHeaderDto dto) {
        if ("FF_INVOICE".equals(dto.getRefType())) {
            return dto.getFfInvoicePoid() != null ? dto.getFfInvoicePoid().toString() : "0";
        } else if ("SH_INVOICE".equals(dto.getRefType())) {
            return dto.getShInvoicePoid() != null ? dto.getShInvoicePoid().toString() : "0";
        } else if ("DN_INVOICE".equals(dto.getRefType())) {
            return dto.getDnInvoicePoid() != null ? dto.getDnInvoicePoid().toString() : "0";
        } else if ("FDA".equals(dto.getRefType())) {
            return dto.getFdaRefPoid() != null ? dto.getFdaRefPoid().toString() : "0";
        }
        return "0";
    }

    private void executeDeleteValidation(ArCreditNoteHdr header, String refPoid) throws SQLException {
        String sql = "BEGIN PROC_GL_VOUCHERS_VALIDATIONS(?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getGroupPoid()); // P_LOGIN_GROUP_POID
            cs.setLong(2, UserContext.getUserPoid()); // P_LOGIN_USER_POID
            cs.setLong(3, UserContext.getCompanyPoid()); // P_LOGIN_COMPANY_POID
            cs.setString(4, header.getDocRef()); // P_DOC_ID
            cs.setString(5, header.getRefType()); // P_REF_TYPE
            cs.setString(6, refPoid); // P_REF_POID
            cs.registerOutParameter(7, Types.VARCHAR); // P_RESULT OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private String getOldJobPoid(ArCreditNoteHdr entity, String refType) {
        return switch (refType != null ? refType.toUpperCase() : "") {
            case "FDA", "FDA JOBS" -> entity.getFdaRef();
            case "FF", "FF JOBS" -> entity.getFfRef();
            default -> null;
        };
    }

    private void executeSoftDeleteSP(Long transactionPoid) throws SQLException {
        // Update the record to set DELETED='Y' instead of calling stored procedure
        ArCreditNoteHdr header = creditNoteHdrRepository.findByTransactionPoidAndDeleted(transactionPoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Credit Note", "transactionPoid", transactionPoid));

        String refPoid = getOldJobPoid(header, header.getRefType());
        executeDeleteValidation(header, refPoid);

        header.setDeleted("Y");
        header.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        header.setLastModifiedDate(LocalDateTime.now());
        creditNoteHdrRepository.save(header);
    }

    private void executeGLBillRefUpdate(Long transactionPoid, String refType) throws SQLException {
        String sql = "BEGIN PROC_DR_CR_BILL_REF_UPDATE(?, ?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            // Get credit note header to get doc ref and party type
            ArCreditNoteHdr header = creditNoteHdrRepository.findById(transactionPoid).orElse(null);
            String docRef = header != null ? header.getDocRef() : "CN-" + transactionPoid;
            String partyType = header != null ? header.getPartyType() : "SUPPLIER";

            cs.setLong(1, groupPoid); // P_LOGIN_GROUP_POID
            cs.setLong(2, userPoid); // P_LOGIN_USER_POID
            cs.setLong(3, companyPoid); // P_LOGIN_COMPANY_POID
            cs.setLong(4, transactionPoid); // P_TRANSACTION_POID
            cs.setString(5, docRef); // P_DOC_REF
            cs.setString(6, "300-111"); // P_DOC_ID
            cs.setString(7, refType != null ? refType : "GENERAL"); // P_REF_TYPE
            cs.setString(8, partyType); // P_PARTY_TYPE
            cs.execute();

            log.info("GL bill reference updated for transactionPoid: {}", transactionPoid);
        } catch (SQLException e) {
            log.warn("GL bill reference update failed for transactionPoid {}: {}", transactionPoid, e.getMessage());
            // Don't fail the entire transaction for GL bill reference update
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeFFInvUpdate(Long transactionPoid, Long ffInvoicePoid) throws SQLException {
        String sql = "BEGIN PROC_AR_UPDATE_FF_INV_DTLS(?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getGroupPoid()); // P_LOGIN_GROUP_POID
            cs.setLong(2, UserContext.getCompanyPoid()); // P_LOGIN_COMPANY_POID
            cs.setLong(3, UserContext.getUserPoid()); // P_LOGIN_USER_POID
            cs.setLong(4, transactionPoid); // P_CN_POID
            cs.setLong(5, ffInvoicePoid != null ? ffInvoicePoid : 0); // P_FF_INV_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeFFCostUpdate(Long transactionPoid, Long ffInvoicePoid) throws SQLException {
        String sql = "BEGIN PROC_CR_NOTE_UPDATE_FF_COST(?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getGroupPoid()); // P_LOGIN_GROUP_POID
            cs.setLong(2, UserContext.getCompanyPoid()); // P_LOGIN_COMPANY_POID
            cs.setLong(3, UserContext.getUserPoid()); // P_LOGIN_USER_POID
            cs.setLong(4, ffInvoicePoid != null ? ffInvoicePoid : 0); // P_FF_POID
            cs.setLong(5, transactionPoid); // P_CN_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeSHInvUpdate(Long transactionPoid, Long shInvoicePoid) throws SQLException {
        String sql = "BEGIN PROC_AR_UPDATE_SH_INV_DTLS(?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getGroupPoid()); // P_LOGIN_GROUP_POID
            cs.setLong(2, UserContext.getCompanyPoid()); // P_LOGIN_COMPANY_POID
            cs.setLong(3, UserContext.getUserPoid()); // P_LOGIN_USER_POID
            cs.setLong(4, transactionPoid); // P_CN_POID
            cs.setLong(5, shInvoicePoid != null ? shInvoicePoid : 0); // P_SH_INV_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void executeDNInvUpdate(Long transactionPoid, Long dnInvoicePoid) throws SQLException {
        String sql = "BEGIN PROC_AR_UPDATE_DN_INV_DTLS(?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getGroupPoid()); // P_LOGIN_GROUP_POID
            cs.setLong(2, UserContext.getCompanyPoid()); // P_LOGIN_COMPANY_POID
            cs.setLong(3, UserContext.getUserPoid()); // P_LOGIN_USER_POID
            cs.setLong(4, transactionPoid); // P_CN_POID
            cs.setLong(5, dnInvoicePoid != null ? dnInvoicePoid : 0); // P_DN_INV_POID
            cs.registerOutParameter(6, Types.VARCHAR); // P_RESULT OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }


    private void executeFDAAmountUpdate(Long transactionPoid, String fdaRef) throws SQLException {
        if (fdaRef == null) {
            log.warn("FDA ref is null for transactionPoid {}, skipping PROC_AR_UPDATE_FDA_AMOUNT", transactionPoid);
            return;
        }
        String sql = "BEGIN PROC_AR_UPDATE_FDA_AMOUNT(?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getGroupPoid()); // P_LOGIN_GROUP_POID
            cs.setLong(2, UserContext.getCompanyPoid()); // P_LOGIN_COMPANY_POID
            cs.setLong(3, UserContext.getUserPoid()); // P_LOGIN_USER_POID
            cs.setString(4, fdaRef); // P_FDA_POID
            cs.registerOutParameter(5, Types.VARCHAR); // P_RESULT OUT
            cs.execute();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void saveGLDetailsWithIssueType(
            Long transactionPoid,
            List<CreditNoteGLDetailDto> glDetails,
            String issueType
    ) {
        log.info("Saving {} GL rows for txn: {} issueType: {}", glDetails.size(), transactionPoid, issueType);
        creditNoteDtlRepository.deleteByTransactionPoid(transactionPoid);
        long detRowId = 0L;
        for (CreditNoteGLDetailDto glDto : glDetails) {
            if (glDto == null) continue;
            String actionType = glDto.getActionType();
            if (actionType == null || actionType.trim().isEmpty()) {
                actionType = "ISCREATED";
            } else {
                actionType = actionType.trim().toUpperCase();
            }
            if ("NOCHANGES".equals(actionType)) {
                actionType = "NOCHANGE";
            }
            if ("ISUPDATED".equals(actionType) && glDto.getDetRowId() == null) {
                actionType = "ISCREATED";
            }
            if (!"ISCREATED".equals(actionType)) {
                continue;
            }

            Long incomingDetRowId = glDto.getDetRowId();
            if (incomingDetRowId == null) {
                incomingDetRowId = ++detRowId;
                glDto.setDetRowId(incomingDetRowId);
            } else {
                detRowId = Math.max(detRowId, incomingDetRowId);
            }

            if ("N".equalsIgnoreCase(issueType)) {
                // Normal + Reversal (SRS requirement)
                saveNormalGLEntry(transactionPoid, glDto, incomingDetRowId, UserContext.getCompanyPoid());
                //saveReversalGLEntry(transactionPoid, glDto, detRowId++, UserContext.getCompanyPoid());
            } else {
                // Issue Type = YES → Normal + Additional
                saveNormalGLEntry(transactionPoid, glDto, incomingDetRowId, UserContext.getCompanyPoid());
                //saveAdditionalGLEntry(transactionPoid, glDto, detRowId++, UserContext.getCompanyPoid());
            }
        }
    }


    private void saveNormalGLEntry(Long transactionPoid, CreditNoteGLDetailDto glDto, long detRowId, Long companyPoid) {
        ArCreditNoteDtl entity = ArCreditNoteDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .type(glDto.getType())
                .companyPoid(glDto.getCompanyPoid() != null ? glDto.getCompanyPoid() : companyPoid)
                .glPoid(glDto.getGlPoid())
                .drAmt(glDto.getDrAmt())
                .crAmt(glDto.getCrAmt())
                .remarks(glDto.getRemarks())
                .taxPoid(glDto.getTaxPoid())
                .taxPercentage(glDto.getTaxPercentage())
                .taxAmount(glDto.getTaxAmount())
                .totalAmount(glDto.getTotalAmount())
                .build();
        entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        creditNoteDtlRepository.saveAndFlush(entity);
    }


    private void saveReversalGLEntry(Long transactionPoid, CreditNoteGLDetailDto glDto, long detRowId, Long companyPoid) {
        ArCreditNoteDtl entity = ArCreditNoteDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .type(glDto.getType())
                .companyPoid(glDto.getCompanyPoid() != null ? glDto.getCompanyPoid() : companyPoid)
                .glPoid(glDto.getGlPoid())
                .drAmt(glDto.getCrAmt())   // swapped
                .crAmt(glDto.getDrAmt())   // swapped
                .remarks("Reversal: " + (glDto.getRemarks() != null ? glDto.getRemarks() : ""))
                .taxPoid(glDto.getTaxPoid())
                .taxPercentage(glDto.getTaxPercentage())
                .taxAmount(glDto.getTaxAmount())
                .totalAmount(glDto.getTotalAmount())
                .build();
        entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        creditNoteDtlRepository.saveAndFlush(entity);
    }

    private void saveAdditionalGLEntry(Long transactionPoid, CreditNoteGLDetailDto glDto, long detRowId, Long companyPoid) {
        ArCreditNoteDtl entity = ArCreditNoteDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .type(glDto.getType())
                .companyPoid(glDto.getCompanyPoid() != null ? glDto.getCompanyPoid() : companyPoid)
                .glPoid(glDto.getGlPoid())
                .drAmt(glDto.getCrAmt())
                .crAmt(glDto.getDrAmt())
                .remarks("Additional: " + (glDto.getRemarks() != null ? glDto.getRemarks() : ""))
                .taxPoid(glDto.getTaxPoid())
                .taxPercentage(glDto.getTaxPercentage())
                .taxAmount(glDto.getTaxAmount())
                .totalAmount(glDto.getTotalAmount())
                .build();
        entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        creditNoteDtlRepository.saveAndFlush(entity);
    }


    private Long lookupGLPoidFromLov(String glCode, Long companyPoid) {
        String lovName = "GL_MASTER_LEDGERS_CN";
        LovGetListDto lovGetListDto = lovService.getDetailsByCodeAndLovName(glCode, lovName);
        if (lovGetListDto != null) {
            return lovGetListDto.getPoid();
        }
        return null;
    }

    private Long lookupTaxPoidFromLov(String taxCode, Long companyPoid) {
        String lovName = "CREDIT_TAX_CODE"; // SRS LOV NAME
        LovGetListDto lovGetListDto = lovService.getDetailsByCodeAndLovName(taxCode, lovName);
        if (lovGetListDto != null) {
            return lovGetListDto.getPoid();
        }
        return null;
    }

    private void saveChargeDetails(Long transactionPoid, List<UniversalChargeDetailDto> chargeDetails) {
        long detRowId = 0L;
        for (UniversalChargeDetailDto dto : chargeDetails) {
            if (dto == null) continue;
            String actionType = dto.getActionType();
            if (actionType == null || actionType.trim().isEmpty()) {
                actionType = "ISCREATED";
            } else {
                actionType = actionType.trim().toUpperCase();
            }
            if ("NOCHANGES".equals(actionType)) {
                actionType = "NOCHANGE";
            }
            if ("ISUPDATED".equals(actionType) && dto.getDetRowId() == null) {
                actionType = "ISCREATED";
            }
            if (!"ISCREATED".equals(actionType)) {
                continue;
            }

            Long incomingDetRowId = dto.getDetRowId();
            if (incomingDetRowId == null) {
                incomingDetRowId = ++detRowId;
                dto.setDetRowId(incomingDetRowId);
            } else {
                detRowId = Math.max(detRowId, incomingDetRowId);
            }

            ArCreditNoteChargeDtl entity = new ArCreditNoteChargeDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(incomingDetRowId);
            entity.setChargePoid(dto.getChargePoid());
            entity.setChargeAmount(dto.getChargeAmount());
            entity.setChargeCostAmount(dto.getChargeCostAmount());
            entity.setRemarks(dto.getRemarks());
            entity.setTaxPoid(dto.getTaxPoid());
            entity.setTaxPercentage(dto.getTaxPercentage());
            entity.setTaxAmount(dto.getTaxAmount());
            entity.setTotalAmount(dto.getTotalAmount());
            entity.setCheckAll(dto.getSelected() != null && !dto.getSelected().trim().isEmpty()
                    ? dto.getSelected().trim()
                    : "N");
            entity.setIssueInvoice(dto.getIssueInvoice());
            entity.setRefDocId(dto.getRefDocId());
            entity.setRefDocPoid(dto.getRefDocPoid());
            entity.setFdaDetRowId(dto.getFdaDetRowId());
            entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
            entity.setLastModifiedDate(LocalDateTime.now());

            creditNoteChargeDtlRepository.save(entity);
        }
    }

    private List<GlobalLogSummary> buildCreateDetailSummaryLogs(CreditNoteHeaderDto creditNoteDto, Long transactionPoid) {
        List<GlobalLogSummary> summaryLogs = new ArrayList<>();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        if (creditNoteDto.getGlDetails() != null) {
            for (CreditNoteGLDetailDto gl : creditNoteDto.getGlDetails()) {
                if (gl == null) continue;
                String actionType = gl.getActionType();
                if (actionType == null || actionType.trim().isEmpty()) {
                    actionType = "ISCREATED";
                } else {
                    actionType = actionType.trim().toUpperCase();
                }
                if ("NOCHANGES".equals(actionType)) {
                    actionType = "NOCHANGE";
                }
                if ("ISUPDATED".equals(actionType) && gl.getDetRowId() == null) {
                    throw new ValidationException("detRowId is required");
                }
                if (!"ISCREATED".equals(actionType) || gl.getDetRowId() == null) {
                    continue;
                }
                String summaryMessage = String.format("Row Created on Credit Note GL Detail with DetRowId: %s", gl.getDetRowId());
                summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, summaryMessage));
            }
        }

        if (creditNoteDto.getChargeDetails() != null) {
            for (UniversalChargeDetailDto charge : creditNoteDto.getChargeDetails()) {
                if (charge == null) continue;
                String actionType = charge.getActionType();
                if (actionType == null || actionType.trim().isEmpty()) {
                    actionType = "ISCREATED";
                } else {
                    actionType = actionType.trim().toUpperCase();
                }
                if ("NOCHANGES".equals(actionType)) {
                    actionType = "NOCHANGE";
                }
                if ("ISUPDATED".equals(actionType) && charge.getDetRowId() == null) {
                    throw new ValidationException("detRowId is required");
                }
                if (!"ISCREATED".equals(actionType) || charge.getDetRowId() == null) {
                    continue;
                }
                String summaryMessage = String.format("Row Created on Credit Note Charge Detail with DetRowId: %s", charge.getDetRowId());
                summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, summaryMessage));
            }
        }

        return summaryLogs;
    }

    private void updateGLDetailsWithLogging(Long transactionPoid, List<CreditNoteGLDetailDto> glDetails,
                                            List<GlobalLogSummary> summaryLogs) {
        if (glDetails == null || glDetails.isEmpty()) return;

        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<ArCreditNoteDtl> toSave = new ArrayList<>();
        List<ArCreditNoteDtl> newlyCreated = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArCreditNoteDtl>> logRequests = new ArrayList<>();

        List<ArCreditNoteDtl> existingDetails = creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, ArCreditNoteDtl> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(ArCreditNoteDtl::getDetRowId, Function.identity(), (a, b) -> a));

        long nextDetRowId = existingDetails.stream()
                .map(ArCreditNoteDtl::getDetRowId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L);

        for (CreditNoteGLDetailDto dto : glDetails) {
            if (dto == null) continue;
            String actionType = dto.getActionType();
            if (actionType == null || actionType.trim().isEmpty()) {
                actionType = (dto.getDetRowId() == null) ? "ISCREATED" : "ISUPDATED";
            } else {
                actionType = actionType.trim().toUpperCase();
            }
            if ("NOCHANGES".equals(actionType)) {
                actionType = "NOCHANGE";
            }
            if ("ISUPDATED".equals(actionType) && dto.getDetRowId() == null) {
                actionType = "ISCREATED";
            }

            switch (actionType) {
                case "ISCREATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        detRowId = ++nextDetRowId;
                        dto.setDetRowId(detRowId);
                    }
                    if (detRowId > nextDetRowId) {
                        nextDetRowId = detRowId;
                    }

                    ArCreditNoteDtl newEntity = new ArCreditNoteDtl();
                    mapGlDtoToEntity(dto, newEntity, transactionPoid);
                    newEntity.setDetRowId(detRowId);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    toSave.add(newEntity);
                    newlyCreated.add(newEntity);
                    break;
                }
                case "ISUPDATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required");
                    }
                    ArCreditNoteDtl existing = existingMap.get(detRowId);
                    if (existing == null) {
                        Long newDetRowId = detRowId;
                        if (newDetRowId > nextDetRowId) {
                            nextDetRowId = newDetRowId;
                        }
                        ArCreditNoteDtl newEntity = new ArCreditNoteDtl();
                        mapGlDtoToEntity(dto, newEntity, transactionPoid);
                        newEntity.setDetRowId(newDetRowId);
                        newEntity.setCreatedBy(currentUser);
                        newEntity.setCreatedDate(now);
                        newEntity.setLastModifiedBy(currentUser);
                        newEntity.setLastModifiedDate(now);
                        toSave.add(newEntity);
                        newlyCreated.add(newEntity);
                        break;
                    }

                    ArCreditNoteDtl oldEntity = new ArCreditNoteDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapGlDtoToEntity(dto, existing, transactionPoid);
                    existing.setLastModifiedBy(currentUser);
                    existing.setLastModifiedDate(now);
                    toSave.add(existing);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ArCreditNoteDtl.class, docId, docKeyPoid, logDetail));
                    break;
                }
                case "ISDELETED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required");
                    }
                    toDelete.add(detRowId);
                    ArCreditNoteDtl oldEntityForDelete = existingMap.get(detRowId);
                    if (oldEntityForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, glPoid:%s, drAmt:%s, crAmt:%s, remarks:%s",
                                oldEntityForDelete.getDetRowId(), transactionPoid, oldEntityForDelete.getGlPoid(),
                                oldEntityForDelete.getDrAmt(), oldEntityForDelete.getCrAmt(), oldEntityForDelete.getRemarks());
                        String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                        summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage));
                    }
                    break;
                }
                case "NOCHANGE":
                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            creditNoteDtlRepository.saveAll(toSave);
            for (ArCreditNoteDtl newlyCreatedEntity : newlyCreated) {
                if (newlyCreatedEntity.getDetRowId() != null) {
                    String summaryMessage = String.format("Row Created on Credit Note GL Detail with DetRowId: %s", newlyCreatedEntity.getDetRowId());
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, summaryMessage));
                }
            }
        }
        if (!toDelete.isEmpty()) {
            creditNoteDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void updateChargeDetailsWithLogging(Long transactionPoid, List<UniversalChargeDetailDto> chargeDetails,
                                                List<GlobalLogSummary> summaryLogs, CreditNoteHeaderDto creditNoteHdr) {
        if (chargeDetails == null || chargeDetails.isEmpty()) return;

        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<ArCreditNoteChargeDtl> toSave = new ArrayList<>();
        List<ArCreditNoteChargeDtl> newlyCreated = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArCreditNoteChargeDtl>> logRequests = new ArrayList<>();

        List<ArCreditNoteChargeDtl> existingDetails = creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        Set<Long> incomingDetRowIds = chargeDetails.stream()
                .map(UniversalChargeDetailDto::getDetRowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ArCreditNoteChargeDtl> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(ArCreditNoteChargeDtl::getDetRowId, Function.identity(), (a, b) -> a));

        long nextDetRowId = existingDetails.stream()
                .map(ArCreditNoteChargeDtl::getDetRowId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L);

        for (UniversalChargeDetailDto dto : chargeDetails) {
            if (dto == null) continue;
            // 🔥 Treat unselected rows as deleted (Legacy behavior)
            if ("N".equalsIgnoreCase(dto.getSelected())) {
                dto.setActionType("ISDELETED");
            }
            String actionType = dto.getActionType();
            if (actionType == null || actionType.trim().isEmpty()) {
                actionType = (dto.getDetRowId() == null) ? "ISCREATED" : "ISUPDATED";
            } else {
                actionType = actionType.trim().toUpperCase();
            }
            if ("NOCHANGES".equals(actionType)) {
                actionType = "NOCHANGE";
            }
            if ("ISUPDATED".equals(actionType) && dto.getDetRowId() == null) {
                actionType = "ISCREATED";
            }

            switch (actionType) {
                case "ISCREATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        detRowId = ++nextDetRowId;
                        dto.setDetRowId(detRowId);
                    }
                    if (detRowId > nextDetRowId) {
                        nextDetRowId = detRowId;
                    }

                    ArCreditNoteChargeDtl newEntity = new ArCreditNoteChargeDtl();
                    newEntity.setDetRowId(detRowId);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    mapChargeDtoToEntity(dto, newEntity, transactionPoid);
                    toSave.add(newEntity);
                    newlyCreated.add(newEntity);
                    break;
                }
                case "ISUPDATED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required");
                    }
                    ArCreditNoteChargeDtl existing = existingMap.get(detRowId);
                    if (existing == null) {
                        Long newDetRowId = detRowId;
                        if (newDetRowId > nextDetRowId) {
                            nextDetRowId = newDetRowId;
                        }
                        ArCreditNoteChargeDtl newEntity = new ArCreditNoteChargeDtl();
                        newEntity.setDetRowId(newDetRowId);
                        newEntity.setCreatedBy(currentUser);
                        newEntity.setCreatedDate(now);
                        newEntity.setLastModifiedBy(currentUser);
                        newEntity.setLastModifiedDate(now);
                        mapChargeDtoToEntity(dto, newEntity, transactionPoid);
                        toSave.add(newEntity);
                        newlyCreated.add(newEntity);
                        break;
                    }

                    ArCreditNoteChargeDtl oldEntity = new ArCreditNoteChargeDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapChargeDtoToEntity(dto, existing, transactionPoid);
                    existing.setLastModifiedBy(currentUser);
                    existing.setLastModifiedDate(now);
                    toSave.add(existing);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ArCreditNoteChargeDtl.class, docId, docKeyPoid, logDetail));
                    break;
                }
                case "ISDELETED": {
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required");
                    }
                    toDelete.add(detRowId);
                    ArCreditNoteChargeDtl oldEntityForDelete = existingMap.get(detRowId);
                    if (oldEntityForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, chargePoid:%s, chargeAmount:%s, remarks:%s",
                                oldEntityForDelete.getDetRowId(), transactionPoid, oldEntityForDelete.getChargePoid(),
                                oldEntityForDelete.getChargeAmount(), oldEntityForDelete.getRemarks());
                        String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                        summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage));
                    }
                    break;
                }
                case "NOCHANGE":
                default:
                    break;
            }
        }

        // 🔥 Delete rows that exist in DB but not sent from UI
        for (ArCreditNoteChargeDtl existing : existingDetails) {
            if (!incomingDetRowIds.contains(existing.getDetRowId())) {
                toDelete.add(existing.getDetRowId());
            }
        }

        if (!toSave.isEmpty()) {
            creditNoteChargeDtlRepository.saveAll(toSave);
            for (ArCreditNoteChargeDtl newlyCreatedEntity : newlyCreated) {
                if (newlyCreatedEntity.getDetRowId() != null) {
                    String summaryMessage = String.format("Row Created on Credit Note Charge Detail with DetRowId: %s", newlyCreatedEntity.getDetRowId());
                    summaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, summaryMessage));
                }
            }
        }
        if (!toDelete.isEmpty()) {
            creditNoteChargeDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void mapGlDtoToEntity(CreditNoteGLDetailDto dto, ArCreditNoteDtl entity, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setType(dto.getType());
        entity.setCompanyPoid(dto.getCompanyPoid() != null ? dto.getCompanyPoid() : UserContext.getCompanyPoid());
        entity.setGlPoid(dto.getGlPoid());
        entity.setDrAmt(dto.getDrAmt());
        entity.setCrAmt(dto.getCrAmt());
        entity.setRemarks(dto.getRemarks());
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        entity.setTotalAmount(dto.getTotalAmount());
    }

    private void mapChargeDtoToEntity(UniversalChargeDetailDto dto, ArCreditNoteChargeDtl entity, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setChargePoid(dto.getChargePoid());
        entity.setChargeAmount(dto.getChargeAmount());
        entity.setChargeCostAmount(dto.getChargeCostAmount());
        entity.setRemarks(dto.getRemarks());
        entity.setCheckAll(dto.getSelected() != null && !dto.getSelected().trim().isEmpty()
                ? dto.getSelected().trim()
                : "N");
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setIssueInvoice(dto.getIssueInvoice());
        entity.setRefDocId(dto.getRefDocId());
        entity.setRefDocPoid(dto.getRefDocPoid());
        entity.setFdaDetRowId(dto.getFdaDetRowId());
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime( LocalDateTime.now());
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }

    private ArCreditNoteHdr mapToEntity(CreditNoteHeaderDto dto) {
        return ArCreditNoteHdr.builder()
                .transactionDate(dto.getTransactionDate())
                .docRef(null) // Let database trigger generate DOC_REF
                .companyPoid(UserContext.getCompanyPoid())
                .currencyCode(dto.getCurrencyCode() != null ? dto.getCurrencyCode() : "USD")
                .currencyRate(dto.getCurrencyRate())
                .partyType(dto.getPartyType())
                .partyPoid(dto.getPartyPoid())
                .refType(dto.getRefType())
                .postingNarration(dto.getPostingNarration())
                .remarks(dto.getRemarks())
                .grandTotal(dto.getGrandTotal())
                .shInvoicePoid(dto.getShInvoicePoid())
                .dnInvoicePoid(dto.getDnInvoicePoid())
                .ffInvoicePoid(dto.getFfInvoicePoid())
                .fdaRefPoid(dto.getFdaRefPoid())
                .dueDate(dto.getDueDate())
                .creditPeriod(dto.getCreditPeriod() != null ? dto.getCreditPeriod().longValue() : null)
                .tinNumber(dto.getTinNumber())
                .bhdAmount(dto.getBhdAmount())
                .bankPoid(dto.getBankPoid())
                .billRefType(dto.getBillRefType())
                .remarksPrintable(dto.getPrintableRemarks() != null && dto.getPrintableRemarks() ? "Y" : "N")
                .multiCompany(dto.getMultiCompany() ? "Y" : "N")
                .fdaDirect(dto.getFdaDirect() != null && dto.getFdaDirect() ? "Y" : "N")
                .otherCurrAmount(dto.getAmount())
                .deleted("N")
                .build();
    }

    private void updateHeaderFromDto(ArCreditNoteHdr entity, CreditNoteHeaderDto dto) {
        entity.setTransactionDate(dto.getTransactionDate()); // Force current timestamp
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setCurrencyRate(dto.getCurrencyRate());
        entity.setPartyType(dto.getPartyType());
        entity.setPartyPoid(dto.getPartyPoid());
        entity.setRefType(dto.getRefType());
        entity.setPostingNarration(dto.getPostingNarration());
        entity.setRemarks(dto.getRemarks());
        entity.setGrandTotal(dto.getGrandTotal());
        entity.setShInvoicePoid(dto.getShInvoicePoid());
        entity.setDnInvoicePoid(dto.getDnInvoicePoid());
        entity.setFfInvoicePoid(dto.getFfInvoicePoid());
        entity.setFdaRefPoid(dto.getFdaRefPoid());
        entity.setDueDate(dto.getDueDate());
        entity.setCreditPeriod(dto.getCreditPeriod());
        entity.setTinNumber(dto.getTinNumber());
        entity.setBhdAmount(dto.getBhdAmount());
        entity.setBankPoid(dto.getBankPoid());
        entity.setBillRefType(dto.getBillRefType());
        entity.setRemarksPrintable(null != dto.getPrintableRemarks() && dto.getPrintableRemarks() ? "Y" : "N");
        entity.setMultiCompany(null != dto.getMultiCompany() && dto.getMultiCompany() ? "Y" : "N");
        entity.setFdaDirect(null != dto.getFdaDirect() && dto.getFdaDirect() ? "Y" : "N");
        entity.setOtherCurrAmount(dto.getAmount());

    }

    private CreditNoteHeaderDto mapToDto(ArCreditNoteHdr entity) {
        CreditNoteHeaderDto dto = new CreditNoteHeaderDto();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setDocRef(entity.getDocRef());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencyRate(entity.getCurrencyRate());
        dto.setPartyType(entity.getPartyType());
       if (entity.getPartyType() != null) {
           LovGetListDto party = lovService.getDetailsByCodeAndLovName(entity.getPartyType(), "CREDIT_PARTY_TYPE");
            dto.setPartyTypeDet(party);
       }
        dto.setPartyPoid(entity.getPartyPoid());
        if (entity.getPartyPoid() != null) {
            LovGetListDto party = lovService.getDetailsByPoidAndLovName(entity.getPartyPoid(), getLovNameForPartyType(entity.getPartyType()));
            dto.setPartyDet(party);
        }
        dto.setRefType(entity.getRefType());
        dto.setPostingNarration(entity.getPostingNarration());
        dto.setRemarks(entity.getRemarks());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setShInvoicePoid(entity.getShInvoicePoid());
        dto.setDnInvoicePoid(entity.getDnInvoicePoid());
        dto.setFfInvoicePoid(entity.getFfInvoicePoid());
        dto.setFdaRefPoid(entity.getFdaRefPoid());
        dto.setFdaRefDet(
                entity.getFdaRefPoid() != null
                        ? lovService.getDetailsByPoidAndLovNameFast(
                        entity.getFdaRefPoid(),
                        "DN_FDA_REF_FOR_CN"
                )
                        : null
        );
        dto.setDueDate(entity.getDueDate() != null ? entity.getDueDate() : LocalDate.now());
        dto.setCreditPeriod(entity.getCreditPeriod());
        dto.setBillRefType(entity.getBillRefType());
        dto.setPrintableRemarks("Y".equals(entity.getRemarksPrintable()));
        dto.setMultiCompany("Y".equals(entity.getMultiCompany()));
        dto.setFdaDirect("Y".equals(entity.getFdaDirect()));
        dto.setTinNumber(entity.getTinNumber());
        dto.setBhdAmount(entity.getBhdAmount());
        dto.setBankPoid(entity.getBankPoid());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setAmount(entity.getOtherCurrAmount());
        return dto;
    }

    private CreditNoteGLDetailDto mapGLToDto(ArCreditNoteDtl entity) {
        CreditNoteGLDetailDto dto = new CreditNoteGLDetailDto();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setDetRowId(entity.getDetRowId());
        dto.setType(entity.getType());
        dto.setGlPoid(entity.getGlPoid());
        dto.setGlDet(lovService.getDetailsByPoidAndLovNameFast(entity.getGlPoid(), "GL_MASTER_LEDGERS_CN"));
        dto.setDrAmt(entity.getDrAmt());
        dto.setCrAmt(entity.getCrAmt());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxPoid(entity.getTaxPoid());
        dto.setTaxDet(lovService.getDetailsByPoidAndLovNameFast(entity.getTaxPoid(), "CR_TAX_MASTER"));
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setCompanyDet(lovService.getDetailsByPoidAndLovNameFast(entity.getCompanyPoid(), "COMPANY"));
        return dto;
    }

    private UniversalChargeDetailDto mapChargeToDto(ArCreditNoteChargeDtl entity, String refType) {
        UniversalChargeDetailDto dto = new UniversalChargeDetailDto();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setDetRowId(entity.getDetRowId());
        dto.setChargePoid(entity.getChargePoid());
        dto.setChargeDet(chargeLovService.getChargeDet(entity.getChargePoid(), refType));
        dto.setChargeAmount(entity.getChargeAmount());
        dto.setChargeCostAmount(entity.getChargeCostAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxPoid(entity.getTaxPoid());
        dto.setTaxDet(taxMasterRepository.findByTaxPoid(entity.getTaxPoid())
                .map(tm -> new LovGetListDto(tm.getTaxPoid(), tm.getTaxCode(), tm.getTaxName(), tm.getTaxPoid(), tm.getTaxName(), tm.getSeqNo(), null))
                .orElse(null));
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setIssueInvoice(entity.getIssueInvoice());
        dto.setPdaAmount(entity.getPdaAmount());
        dto.setSelected(entity.getCheckAll() != null && !entity.getCheckAll().trim().isEmpty()
                ? entity.getCheckAll().trim()
                : "N");
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setFdaDetRowId(entity.getFdaDetRowId());
        return dto;
    }

    private boolean hasIssueInvoice(List<UniversalChargeDetailDto> chargeDetails) {
        if (chargeDetails == null) return false;
        return chargeDetails.stream().anyMatch(charge -> ("Y".equals(charge.getIssueInvoice())));
    }

    public void saveBillwiseForGl(
            Long transactionPoid,
            List<CreditNoteGLDetailDto> glDetails,
            String docId,
            boolean isUpdate
    ) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }
        List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
        Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
        Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;

        for (CreditNoteGLDetailDto glDto : glDetails) {
            if (glDto == null || glDto.getDetRowId() == null) {
                continue;
            }
            if (!glMasterRepository.existsByGlPoid(glDto.getGlPoid())) {
                throw new ResourceNotFoundException("Gl Master", "glPoid", glDto.getGlPoid());
            }
          /*  if (!taxMasterRepository.existsByTaxPoid(glDto.getTaxPoid())) {
                throw new ResourceNotFoundException("Tax", "taxPoid", glDto.getTaxPoid());
            }*/

            long inital = 1L;
            if (glDto.getBreakupList() != null && !glDto.getBreakupList().isEmpty()) {
                for (BillwiseBreakupPopupRequestDto popup : glDto.getBreakupList()) {
                    BillwiseBreakupRequestDto req = new BillwiseBreakupRequestDto();
                    req.setGroupPoid(groupPoid);
                    req.setCompanyPoid(companyPoid);
                    req.setDocId(docId);
                    req.setTransactionPoid(transactionPoid);

                    Long billDetRowId = popup.getBillDetRowId();
                    if (billDetRowId == null || billDetRowId == 0L) {
                        billDetRowId = inital;
                    }
                    req.setBillDetRowId(billDetRowId);

                    req.setBillRefType(popup.getBillRefType());
                    req.setBillRef(popup.getBillRef());
                    req.setBillDueDate(popup.getBillDueDate());
                    if ("DR".equalsIgnoreCase(popup.getType())) {
                        req.setDrAmt(popup.getAmount());
                        req.setCrAmt(BigDecimal.ZERO);
                    } else {
                        req.setDrAmt(BigDecimal.ZERO);
                        req.setCrAmt(popup.getAmount());
                    }
                    req.setBillRemarks(popup.getBillRemarks());
                    req.setLoginUserPoid(userPoid);
                    req.setMainDetRowId(glDto.getDetRowId());
                    req.setGlCompanyPoid(companyPoid);
                    req.setGlPoid(glDto.getGlPoid());
                    billwiseList.add(req);
                    inital++;
                }
            }
        }

        if (!billwiseList.isEmpty()) {

            if (isUpdate) {
                billwiseBreakupService.updateBillwiseBreakups(billwiseList, userPoid);
            } else {
                billwiseBreakupService.insertBillwiseBreakup(billwiseList);
            }

            log.info("Saved {} billwise breakup entries for transactionPoid: {}", billwiseList.size(), transactionPoid);
        } else if (isUpdate) {
            // No billwise data in request — clean up any previously saved records
            billwiseBreakupService.deleteBillwiseBreakup(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), docId, transactionPoid, userPoid);
        }
    }

    public List<BillwiseBreakupDtoCreditNotePopUp> getBillwiseForGl(Long transactionPoid, Long detRowId, String docId) {
        List<BillwiseBreakupDtoCreditNotePopUp> result = new ArrayList<>();

        try {
            Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
            Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 3L;

            GlVoucherLoadBillwiseBreakupResponseDto response = billwiseBreakupService.loadBillwiseBreakup(
                    groupPoid, companyPoid, docId, transactionPoid);

            if (response != null && response.getLoadBillwiseBreakupResponseDtoList() != null) {
                for (LoadBillwiseBreakupResponseDto item : response.getLoadBillwiseBreakupResponseDtoList()) {
                    if (item.getMainDetRowId() != null && item.getMainDetRowId().equals(detRowId)) {
                        BillwiseBreakupDtoCreditNotePopUp dto = new BillwiseBreakupDtoCreditNotePopUp();
                        dto.setId(item.getBillDetRowId());
                        dto.setCompanyPoid(companyPoid);
                        dto.setBillRefType(item.getBillRefType());
                        dto.setBillRefNo(item.getBillRef());
                        dto.setGlDetRowId(item.getMainDetRowId());
                        dto.setTransactionPoid(transactionPoid);
                        dto.setRemarks(item.getBillRemarks());
                        if (item.getDrAmt() != null && item.getDrAmt().compareTo(BigDecimal.ZERO) > 0) {
                            dto.setAdjustedAmount(item.getDrAmt());
                        } else if (item.getCrAmt() != null && item.getCrAmt().compareTo(BigDecimal.ZERO) > 0) {
                            dto.setAdjustedAmount(item.getCrAmt());
                        }
                        result.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error loading billwise breakup for transactionPoid: {}, detRowId: {}", transactionPoid, detRowId, e);
        }

        return result;
    }

    public void saveCostCenterForGl(Long transactionPoid,
                                    List<CreditNoteGLDetailDto> glDetails,
                                    String docId,
                                    boolean isUpdate
    ) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }

        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();
        Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 3L;
        Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
        long inital = 1L;
        for (CreditNoteGLDetailDto glDto : glDetails) {
            if (glDto == null || glDto.getDetRowId() == null) {
                continue;
            }

            if (glDto.getCostCenterList() != null && !glDto.getCostCenterList().isEmpty()) {
                for (CostCenterBreakupPopupRequestDto popup : glDto.getCostCenterList()) {
                    CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
                    dto.setGroupPoid(groupPoid);
                    dto.setCompanyPoid(companyPoid);
                    dto.setDocId(docId);
                    dto.setTransactionPoid(transactionPoid);
                    dto.setMainDetRowId(glDto.getDetRowId());
                    dto.setGlPoid(glDto.getGlPoid());
                    dto.setCostDetRowId(inital);
                    dto.setCostGroup(popup.getCostGroup());
                    dto.setCostPoid(popup.getCostPoid());
                    dto.setAmount(popup.getAmount());
                    dto.setLoginUserPoid(userPoid);
                    costCenterList.add(dto);
                    inital++;
                }
            }
        }

        if (!costCenterList.isEmpty()) {

            if (isUpdate) {
                costCenterBreakupService.updateCostCenterBreakups(costCenterList, userPoid);
            } else {
                costCenterBreakupService.saveCostCenterBreakups(costCenterList);
            }
            log.info("Saved {} cost center breakup entries for transactionPoid: {}", costCenterList.size(), transactionPoid);
        } else if (isUpdate) {
            // No cost center data in request — clean up any previously saved records
            costCenterBreakupService.deleteCostCenterData(docId, transactionPoid, UserContext.getGroupPoid(), UserContext.getCompanyPoid(), userPoid);
        }
    }

    public List<CostCenterBreakupDto> getCostCenterForGl(Long transactionPoid, Long detRowId, String docId) {
        List<CostCenterBreakupDto> result = new ArrayList<>();

        try {
            Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
            Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 3L;
            Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;

            GlVoucherCostCenterBreakupResponseDto response = costCenterBreakupService.loadCostCenterData(
                    docId, transactionPoid, groupPoid, companyPoid, userPoid);

            if (response != null && response.getCostBreakupList() != null) {
                for (CostCenterBreakupResponseDto item : response.getCostBreakupList()) {
                    if (item.getMainDetRowId() != null && item.getMainDetRowId().equals(detRowId)) {
                        CostCenterBreakupDto dto = new CostCenterBreakupDto();
                        dto.setCompanyPoid(companyPoid);
                        dto.setCostCenterCode(item.getCostPoid());
                        dto.setCostCenterName(item.getDescription());
                        dto.setAmount(item.getAmount() != null ? item.getAmount().doubleValue() : null);
                        dto.setGlDetRowId(item.getMainDetRowId());
                        dto.setTransactionPoid(transactionPoid);
                        dto.setRemarks(item.getDescription());
                        result.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error loading cost center breakup for transactionPoid: {}, detRowId: {}", transactionPoid, detRowId, e);
        }

        return result;
    }

    private void loadBillwiseAndCostCenterBreakup(List<CreditNoteGLDetailDto> glDetailDtos, Long transactionPoid, String docId) {
        if (glDetailDtos == null || glDetailDtos.isEmpty()) {
            return;
        }

        try {
            Long groupPoid = UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
            Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 3L;
            Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;

            GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = billwiseBreakupService.loadBillwiseBreakup(
                    groupPoid, companyPoid, docId, transactionPoid);

            GlVoucherCostCenterBreakupResponseDto costCenterResponse = costCenterBreakupService.loadCostCenterData(
                    docId, transactionPoid, groupPoid, companyPoid, userPoid);

            for (CreditNoteGLDetailDto glDto : glDetailDtos) {
                if (glDto.getDetRowId() == null) {
                    continue;
                }

                if (billwiseResponse != null && billwiseResponse.getLoadBillwiseBreakupResponseDtoList() != null) {
                    List<BillwiseBreakupPopupRequestDto> billwiseList = billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                            .filter(item -> item.getMainDetRowId() != null && item.getMainDetRowId().equals(glDto.getDetRowId()))
                            .map(item -> {
                                BillwiseBreakupPopupRequestDto popupDto = new BillwiseBreakupPopupRequestDto();
                                popupDto.setBillDetRowId(item.getBillDetRowId());
                                popupDto.setBillRefType(item.getBillRefType());
                                popupDto.setBillRef(item.getBillRef());
                                popupDto.setBillDueDate(item.getBillDueDate());
                                if (item.getDrAmt() != null && item.getDrAmt().compareTo(BigDecimal.ZERO) > 0) {
                                    popupDto.setType("DR");
                                    popupDto.setAmount(item.getDrAmt());
                                } else if (item.getCrAmt() != null && item.getCrAmt().compareTo(BigDecimal.ZERO) > 0) {
                                    popupDto.setType("CR");
                                    popupDto.setAmount(item.getCrAmt());
                                }
                                popupDto.setBillRemarks(item.getBillRemarks());
                                return popupDto;
                            })
                            .collect(Collectors.toList());
                    glDto.setBreakupList(billwiseList);
                }

                if (costCenterResponse != null && costCenterResponse.getCostBreakupList() != null) {
                    List<CostCenterBreakupPopupRequestDto> costCenterList = costCenterResponse.getCostBreakupList().stream()
                            .filter(item -> item.getMainDetRowId() != null && item.getMainDetRowId().equals(glDto.getDetRowId()))
                            .map(item -> {
                                CostCenterBreakupPopupRequestDto popupDto = new CostCenterBreakupPopupRequestDto();
                                popupDto.setCostDetRowId(item.getCostDetRowId());
                                popupDto.setCostGroup(item.getCostGroup());
                                popupDto.setCostPoid(item.getCostPoid());
                                popupDto.setAmount(item.getAmount());
                                if (StringUtils.isNotEmpty(item.getCostPoid()) && StringUtils.isNotEmpty(item.getCostGroup())) {
                                    try {
                                        LovGetListDto codeDet  = lovService.getDetailsByCodeAndLovName(item.getCostPoid(), item.getCostGroup());
                                        popupDto.setCostCenterDetails(codeDet);
                                        if (codeDet.getPoid() == null) {
                                            LovGetListDto det = lovService.getDetailsByPoidAndLovName(Long.valueOf(item.getCostPoid()), item.getCostGroup());
                                            popupDto.setCostCenterDetails(det);
                                            popupDto.setCostPoid(det.getCode());
                                        }
                                    } catch (NumberFormatException e) {
                                        popupDto.setCostCenterDetails(lovService.getDetailsByCodeAndLovName(item.getCostPoid(), item.getCostGroup()));
                                    }
                                }
                                return popupDto;
                            })
                            .collect(Collectors.toList());
                    glDto.setCostCenterList(costCenterList);
                }
            }
        } catch (Exception e) {
            log.error("Error loading breakup data for transactionPoid: {}", transactionPoid, e);
        }
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-111");
        params.put("SUB_CREDIT_DTL_1", printService.load("Finance/AR/CreditNoteDtl_subreport1.jrxml"));
        params.put("SUB_CREDIT_DTL_VAT", printService.load("Finance/AR/CreditNoteDtlSubreportVAT2019.jrxml"));
        params.put("SUB_CHARGE_1", printService.load("Finance/AR/CreditNoteChargeSubreport1.jrxml"));
        params.put("SUB_CHARGE_VAT", printService.load("Finance/AR/CrdeitNoteChargeSubreportVAT2019.jrxml"));
        params.put("SUB_ITEM", printService.load("Finance/AR/CrdeitNoteItemSubreport.jrxml"));
        JasperReport mainReport = printService.load("Finance/AR/CreditNote.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public FdaRefResponseDto getFdaRefForCreditNote(String lovName, Long lovValue) {
        try {
            return executeFdaRefProcedure(lovName, lovValue);
        } catch (SQLException e) {
            log.error("Error fetching FDA reference for lovName: {}, lovValue: {}", lovName, lovValue, e);
            throw new ValidationException("Failed to fetch FDA reference: " + e.getMessage());
        }
    }

    private FdaRefResponseDto executeFdaRefProcedure(String lovName, Long lovValue) throws SQLException {
        String sql = "BEGIN PROC_AR_CN_SET_FDAREF(?, ?, ?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);

            cs.setLong(1, UserContext.getGroupPoid());
            cs.setLong(2, UserContext.getCompanyPoid());
            cs.setLong(3, UserContext.getUserPoid());
            cs.setString(4, UserContext.getDocumentId());
            cs.setNull(5, java.sql.Types.NUMERIC);
            cs.setString(6, lovName);
            cs.setLong(7, lovValue != null ? lovValue : 0);
            cs.registerOutParameter(8, OracleTypes.CURSOR);

            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                if (rs != null && rs.next()) {
                    Long fdaRefPoid = rs.getLong("FDA_REF_POID");
                    return FdaRefResponseDto.builder()
                            .fdaRefPoid(fdaRefPoid)
                            .fdaRefDet(lovService.getDetailsByPoidAndLovNameFast(fdaRefPoid, "DN_FDA_REF_FOR_CN"))
                            .build();
                }
            }

            return FdaRefResponseDto.builder().build();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

    private void DocumentBeforeSaveBillwiseCostGroups(CreditNoteHeaderDto dto) {
        BigDecimal documentTotal = firstNonNull(dto.getBhdAmount(), dto.getGrandTotal());
        if (documentTotal == null) {
            throw new ValidationException("Total Amount is not found...");
        }
        if (dto.getPartyPoid() == null) {
            throw new ValidationException("Party is not found...");
        }
        if (StringUtils.isBlank(dto.getRefType())) {
            throw new ValidationException("Ref Type is not found...");
        }

        String refType = dto.getRefType();

        // GENERAL logic 
        if ("GENERAL".equalsIgnoreCase(refType)) {
            Long partyGl = getPartyGLPoid(dto.getPartyPoid(), dto.getPartyType());
            if (partyGl == null) {
                throw new ValidationException("Selected Party GL_CODE is not found...");
            }

            List<CreditNoteGLDetailDto> effectiveGlDetails = getEffectiveGlDetailsForBillwise(dto);
            BigDecimal totalDrAmt = sumByTypeForBillwise(effectiveGlDetails, "DR");
            BigDecimal totalCrAmt = sumByTypeForBillwise(effectiveGlDetails, "CR");

            if (totalDrAmt.compareTo(BigDecimal.ZERO) == 0) {
                throw new ValidationException("No Debit Entries Entered...");
            }

            boolean partyGlFound = effectiveGlDetails.stream()
                    .anyMatch(gl -> Objects.equals(gl.getGlPoid(), partyGl));

            if (!partyGlFound) {
                BigDecimal balancingAmount = totalDrAmt.subtract(totalCrAmt);
                if (balancingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    balancingAmount = documentTotal;
                }

                Long nextDetRowId = dto.getGlDetails().stream()
                        .map(CreditNoteGLDetailDto::getDetRowId)
                        .filter(Objects::nonNull)
                        .max(Long::compareTo)
                        .orElse(0L) + 1;

                CreditNoteGLDetailDto balancingRow = new CreditNoteGLDetailDto();
                balancingRow.setType("CR");
                balancingRow.setCompanyPoid(UserContext.getCompanyPoid());
                balancingRow.setGlPoid(partyGl);
                balancingRow.setDrAmt(BigDecimal.ZERO);
                balancingRow.setCrAmt(balancingAmount);
                balancingRow.setTotalAmount(balancingAmount);
                balancingRow.setRemarks("Auto Balance Entry");
                balancingRow.setActionType("isCreated");
                balancingRow.setDetRowId(nextDetRowId);

                if (isBillwiseApplicableForCn(partyGl)) {
                    balancingRow.setBreakupList(List.of(createDefaultBillwiseBreakupForCn(dto, balancingAmount)));
                }

                if (dto.getGlDetails() == null) {
                    dto.setGlDetails(new ArrayList<>());
                }
                dto.getGlDetails().add(balancingRow);

                effectiveGlDetails = getEffectiveGlDetailsForBillwise(dto);
                totalDrAmt = sumByTypeForBillwise(effectiveGlDetails, "DR");
                totalCrAmt = sumByTypeForBillwise(effectiveGlDetails, "CR");
            }

            if (totalCrAmt.compareTo(documentTotal) != 0) {
                throw new ValidationException(
                        "Amount (" + documentTotal + ") is not matching with party credit amount(" + totalCrAmt + ")"
                );
            }

            if (totalDrAmt.compareTo(totalCrAmt) != 0) {
                throw new ValidationException(
                        "Total Debits and Credits not tallying. Dr> " + totalDrAmt + " Cr> " + totalCrAmt
                );
            }
            return;
        }

        // CUSTOM logic 
        if ("CUSTOM".equalsIgnoreCase(refType)) {
            List<CreditNoteGLDetailDto> effectiveGlDetails = getEffectiveGlDetailsForBillwise(dto);
            BigDecimal totalDrAmt = sumByTypeForBillwise(effectiveGlDetails, "DR");
            BigDecimal totalCrAmt = sumByTypeForBillwise(effectiveGlDetails, "CR");

            if (totalDrAmt.compareTo(BigDecimal.ZERO) == 0) {
                throw new ValidationException("WARNING : No debit entries entered.");
            }
            if (totalCrAmt.compareTo(BigDecimal.ZERO) == 0) {
                throw new ValidationException("WARNING : No credit entries entered.");
            }
            if (totalCrAmt.compareTo(totalDrAmt) != 0) {
                throw new ValidationException(
                        "Total Debit (" + totalDrAmt + ") Amounts and Credit (" + totalCrAmt + ") Amounts are not tallying."
                );
            }
            if (totalDrAmt.compareTo(documentTotal) != 0) {
                throw new ValidationException(
                        "Paid Amount (" + documentTotal + ") is not matching with total party credit amount(" + totalDrAmt + ")"
                );
            }
        }
    }

    private List<CreditNoteGLDetailDto> getEffectiveGlDetailsForBillwise(CreditNoteHeaderDto dto) {
        if (dto.getGlDetails() == null) {
            return new ArrayList<>();
        }
        return dto.getGlDetails().stream()
                .filter(Objects::nonNull)
                .filter(gl -> !"ISDELETED".equalsIgnoreCase(normalizeActionTypeForBillwise(gl.getActionType())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private BigDecimal sumByTypeForBillwise(List<CreditNoteGLDetailDto> glDetails, String type) {
        return glDetails.stream()
                .filter(gl -> type.equalsIgnoreCase(gl.getType()))
                .map(this::resolveLineAmountForBillwise)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveLineAmountForBillwise(CreditNoteGLDetailDto glDetail) {
        if ("DR".equalsIgnoreCase(glDetail.getType())) {
            return firstNonNull(glDetail.getTotalAmount(), glDetail.getDrAmt(), BigDecimal.ZERO);
        }
        if ("CR".equalsIgnoreCase(glDetail.getType())) {
            return firstNonNull(glDetail.getTotalAmount(), glDetail.getCrAmt(),BigDecimal.ZERO);
        }
        return firstNonNull(glDetail.getTotalAmount(), BigDecimal.ZERO);
    }

    private String normalizeActionTypeForBillwise(String actionType) {
        return actionType == null ? "" : actionType.trim().toUpperCase();
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean isBillwiseApplicableForCn(Long glPoid) {
        if (glPoid == null) {
            return false;
        }
        return glMasterRepository
                .findByGlPoid(glPoid)
                .map(gl -> "Y".equalsIgnoreCase(gl.getBillwise()))
                .orElse(false);
    }

    private BillwiseBreakupPopupRequestDto createDefaultBillwiseBreakupForCn(CreditNoteHeaderDto dto, BigDecimal amount) {
        BillwiseBreakupPopupRequestDto billwise = new BillwiseBreakupPopupRequestDto();
        billwise.setBillDueDate(dto.getDueDate());
        billwise.setBillRefType("NEW");
        billwise.setBillRef(StringUtils.defaultIfBlank(dto.getDocRef(), "CN-TEMP"));
        billwise.setBillDueDate(resolveDueDateForBillwise(dto));
        billwise.setType("CR");
        billwise.setAmount(amount);
        billwise.setBillRemarks(dto.getPostingNarration());
        billwise.setActionType("isCreated");
        return billwise;
    }

    private LocalDate resolveDueDateForBillwise(CreditNoteHeaderDto dto) {
        if (dto.getDueDate() != null) {
            return dto.getDueDate();
        }
        if (dto.getCreditPeriod() != null && dto.getCreditPeriod() > 0) {
            return LocalDate.now().plusDays(dto.getCreditPeriod());
        }
        return LocalDate.now();
    }

    private void filterUnselectedCharges(CreditNoteHeaderDto dto) {
        if (dto.getChargeDetails() == null) return;

        List<UniversalChargeDetailDto> filtered =
                dto.getChargeDetails()
                        .stream()
                        .filter(c -> "Y".equalsIgnoreCase(c.getSelected()))
                        .collect(Collectors.toList());

        dto.setChargeDetails(filtered);
    }

    @Override
    public ChargeTaxDataDto getChargeTaxData(String partyType, Long partyPoid, Long chargePoid) {
        try {
            return executeGetChargeTaxData(partyType, partyPoid, chargePoid);
        } catch (SQLException e) {
            log.error("Error fetching charge tax data for partyType: {}, partyPoid: {}, chargePoid: {}", 
                    partyType, partyPoid, chargePoid, e);
            throw new ValidationException("Failed to fetch charge tax data: " + e.getMessage());
        }
    }

    private ChargeTaxDataDto executeGetChargeTaxData(String partyType, Long partyPoid, Long chargePoid) throws SQLException {
        String sql = "BEGIN PROC_GET_CHARGE_TAX_PER_V3(?, ?, ?, ?, ?, ?); END;";
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = getTransactionalConnection();
            cs = conn.prepareCall(sql);
            cs.setLong(1, UserContext.getCompanyPoid());
            cs.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            cs.setString(3, partyType);
            cs.setLong(4, partyPoid);
            cs.setLong(5, chargePoid);
            cs.registerOutParameter(6, OracleTypes.CURSOR);
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(6)) {
                if (rs != null && rs.next()) {
                    return ChargeTaxDataDto.builder()
                            .taxPoid(rs.getLong("TAX_POID"))
                            .taxDet(taxMasterRepository.findByTaxPoid(rs.getLong("TAX_POID"))
                                    .map(tm -> new LovGetListDto(tm.getTaxPoid(), tm.getTaxCode(), tm.getTaxName(), tm.getTaxPoid(), tm.getTaxName(), tm.getSeqNo(), null))
                                    .orElse(null))
                            .percentage(rs.getBigDecimal("PERCENTAGE"))
                            .build();
                }
            }

            return ChargeTaxDataDto.builder()
                    .taxPoid(0L)
                    .percentage(BigDecimal.ZERO)
                    .build();
        } finally {
            if (cs != null) {
                try { cs.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                releaseTransactionalConnection(conn);
            }
        }
    }

}
