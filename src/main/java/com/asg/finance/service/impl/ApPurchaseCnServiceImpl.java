package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import com.asg.finance.exception.DataAccessException;
import com.asg.finance.entity.ApPurchaseCnChargeDtl;
import com.asg.finance.entity.ApPurchaseCnGlDtl;
import com.asg.finance.entity.ApPurchaseCnHdr;
import com.asg.finance.entity.ApPurchaseCnItemDtl;
import com.asg.finance.repository.ApPurchaseCnChargeDtlRepository;
import com.asg.finance.repository.ApPurchaseCnGlDtlRepository;
import com.asg.finance.repository.ApPurchaseCnHdrRepository;
import com.asg.finance.repository.ApPurchaseCnItemDtlRepository;
import com.asg.finance.repository.ApPurchaseCnProcRepository;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.finance.service.ApPurchaseCnService;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApPurchaseCnServiceImpl implements ApPurchaseCnService {

    private static final String DEFAULT_YES_NO_VALUE = "Y";
    private static final String DEFAULT_NO_VALUE = "N";
    private static final long DEFAULT_USER_CONTEXT_POID = 1L;
    private static final String DOC_ID_AP_PURCHASE_CN = "200-103";
    private static final String DOC_ID_AP_PURCHASE_CN_GL = "200-107";
    private static final String ENTITY_SUPPLIER_CREDIT_NOTE = "Supplier Credit Note";
    private static final String FIELD_TRANSACTION_POID = "transactionPoid";
    private static final String TABLE_AP_PURCHASE_CN_HDR = "AP_PURCHASE_CN_HDR";
    private static final String COLUMN_TRANSACTION_POID = "TRANSACTION_POID";
    // LOV Constants - Updated to match SRS specification
    private static final String LOV_COMPANY = "COMPANY";
    private static final String LOV_GL_MASTER_LEDGERS_PJ = "GL_MASTER_LEDGERS_PJ";
    private static final String LOV_ACC_TYPE_SHORT = "ACC_TYPE_SHORT";
    private static final String LOV_INPUT_TAX_MASTER = "INPUT_TAX_MASTER";
    private static final String LOV_PJ_GL_INPUT_TAX = "PJ_GL_INPUT_TAX";
    private static final String LOV_FDA_CHARGE_MASTER_PJ = "FDA_CHARGE_MASTER_PJ";
    private static final String LOV_FF_CHARGE_MASTER_PJ = "FF_CHARGE_MASTER_PJ";
    private static final String LOV_FF_JOBNO = "FF_JOBNO";
    private static final String LOV_STOCK_MASTER = "STOCK_MASTER";
    private static final String LOV_STOCK_UNIT = "STOCK_UNIT";
    private static final String LOV_SUPPLIER_MASTER_FOR_PJ_CN = "SUPPLIER_MASTER_FOR_PJ_CN";
    private static final String LOV_PRINCIPAL_MASTER_FOR_PJ_CN = "PRINCIPAL_MASTER_FOR_PJ_CN";
    private static final String PARTY_TYPE_SUPPLIER = "SUPPLIER";
    private static final String FILTER_TRANSACTION_DATE = "TRANSACTION_DATE";
    private static final String FILTER_LONG_NARRATION = "LONG_NARRATION";
    private static final String PARAM_PJ_REF_TYPE = "pjRefType";
    private static final String PARAM_LINE_ITEMS = "lineItems";
    private static final String FIELD_COMPANY_POID = "COMPANY_POID";
    private static final String FIELD_GL_POID = "GL_POID";
    private static final String FIELD_TYPE = "TYPE";
    private static final String FIELD_TAX_POID = "TAX_POID";
    private static final String FIELD_CHARGE_POID = "CHARGE_POID";
    private static final String FIELD_REF_DOC_POID = "REF_DOC_POID";
    private static final String FIELD_STOCK_POID = "STOCK_POID";
    private static final String FIELD_STOCK_UNIT_POID = "STOCK_UNIT_POID";
    private static final String FIELD_COMPANY_DET = "companyDet";
    private static final String FIELD_GL_DET = "glDet";
    private static final String FIELD_TYPE_DET = "typeDet";
    private static final String FIELD_TAX_DET = "taxDet";
    private static final String FIELD_CHARGE_DET = "chargeDet";
    private static final String FIELD_REF_DOC_DET = "refDocDet";
    private static final String FIELD_STOCK_DET = "stockDet";
    private static final String FIELD_STOCK_UNIT_DET = "stockUnitDet";
    private static final String FIELD_BILL_WISE_BREAK_UP_LIST = "BILL_WISE_BREAK_UP_LIST";
    private static final String FIELD_COST_CENTER_BREAK_UP_LIST = "COST_CENTER_BREAK_UP_LIST";
    private static final String LOG_ROW_CREATED_ITEM = "Row Created on Supplier Credit Note Item Detail with detRowId: ";
    private static final String LOG_ROW_CREATED_CHARGE = "Row Created on Supplier Credit Note Charge Detail with detRowId: ";
    private static final String LOG_ROW_CREATED_GL = "Row Created on Supplier Credit Note GL Detail with detRowId: ";
    private static final String ERROR_REF_TYPE_REQUIRED = "Reference type is required";
    private static final String ERROR_GL_DETAIL_REQUIRED_PREFIX = "At least one GL detail is required for reference type: ";
    private static final String ERROR_CHARGE_DETAIL_REQUIRED_PREFIX = "At least one charge detail is required for reference type: ";
    private static final String ERROR_ITEM_DETAIL_REQUIRED_PREFIX = "At least one item detail is required for reference type: ";
    private static final String ERROR_PJ_FF_FDA_CHARGE_REQUIRED =
            "At least one charge detail is required for PJ Type 'FF Jobs or FDA jobs'";
    private static final String ERROR_PJ_GENERAL_GL_REQUIRED =
            "At least one charge detail is required for PJ Type 'GENERAL PO Jobs'";
    private static final String REF_TYPE_GENERAL = "GENERAL";
    private static final String REF_TYPE_GENERAL_PO = "GENERAL_PO";
    private static final String REF_TYPE_FF = "FF";
    private static final String REF_TYPE_FF_JOB = "FF_JOB";
    private static final String REF_TYPE_FDA = "FDA";
    private static final String REF_TYPE_MTA_PO = "MTA_PO";
    private static final String REF_TYPE_PJ_REVERSAL = "PJ_REVERSAL";
    private static final String ERROR_CHARGE_DETAIL_REQUIRED_FOR_FF =
            ERROR_CHARGE_DETAIL_REQUIRED_PREFIX + REF_TYPE_FF;
    private static final String ACTION_TYPE_NO_CHANGE = "nochange";
    private static final String ACTION_TYPE_IS_DELETED = "isdeleted";
    private static final String ACTION_TYPE_IS_UPDATED = "isupdated";
    private static final String ACTION_TYPE_IS_CREATED = "iscreated";
    private static final String BILLWISE_TYPE_DR = "DR";
    private static final String BILLWISE_TYPE_CR = "CR";
    private static final String BILLWISE_DISPLAY_DR = "DR";
    private static final String BILLWISE_DISPLAY_CR = "CR";
    private static final String LOG_KEY_ID_FORMAT = "KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s";
    private static final String LOG_MESSAGE_CREATING = "Creating supplier credit note for partyType: {}, refType: {}";
    private static final String LOG_MESSAGE_UPDATING = "Updating supplier credit note with transactionPoid: {}";
    private static final String LOG_MESSAGE_DELETING = "Deleting supplier credit note with transactionPoid: {}";
    private static final String LOG_MESSAGE_FETCHING = "Fetching supplier credit note with transactionPoid: {}";
    private static final String LOG_MESSAGE_SAVED_HEADER = "Supplier credit note header saved with transactionPoid: {}";
    private static final String LOG_MESSAGE_CREATED_SUCCESS = "Supplier credit note created successfully with transactionPoid: {}";
    private static final String LOG_MESSAGE_UPDATED_SUCCESS = "Supplier credit note updated successfully with transactionPoid: {}";
    private static final String LOG_MESSAGE_DELETED_SUCCESS = "Supplier credit note deleted successfully with transactionPoid: {}";
    private static final String LOG_MESSAGE_FETCHED_SUCCESS = "Successfully fetched supplier credit note with {} items, {} charges, {} GL entries";
    private static final String LOG_MESSAGE_HEADER_UPDATED = "Supplier credit note header updated successfully";
    private static final String LOG_MESSAGE_VALIDATION = "Performing validation for partyPoid: {}";
    private static final String LOG_MESSAGE_PROCESSING_ACTIONS = "Processing {} item actions for transactionPoid: {}";
    private static final String LOG_MESSAGE_PROCESSING_GL_ACTIONS = "Processing {} GL actions for transactionPoid: {}";
    private static final String LOG_MESSAGE_PROCESSING_ITEM = "Processing item detRowId: {} with actionType: {} (normalized: {})";
    private static final String LOG_MESSAGE_PROCESSING_GL = "Processing GL detRowId: {} with actionType: {} (normalized: {})";
    private static final String LOG_MESSAGE_NO_ACTION = "No action taken for GL detRowId: {} with actionType: {}";
    private static final String LOG_MESSAGE_UPDATING_GL = "Updating GL detail - transactionPoid: {}, detRowId: {}, actionType: {}";
    private static final String LOG_MESSAGE_ADDING_GL_LOG = "Adding GL log request for transactionPoid: {}, detRowId: {}";
    private static final String LOG_MESSAGE_ADDED_GL_LOG = "Added GL log request - transactionPoid: {}, detRowId: {}, logDetail: {}";
    private static final String LOG_MESSAGE_CREATING_BATCH = "Creating batch logs - Items: {}, Charges: {}, GL: {}";
    private static final String LOG_MESSAGE_PROCESSING_ITEM_LOGS = "Processing {} item log requests";
    private static final String LOG_MESSAGE_PROCESSING_CHARGE_LOGS = "Processing {} charge log requests";
    private static final String LOG_MESSAGE_PROCESSING_GL_LOGS = "Processing {} GL log requests";
    private static final String LOG_MESSAGE_PROCESSED_SUCCESS = "Successfully processed {} {} log requests";
    private static final String LOG_MESSAGE_SAVING_DETAILS = "Saving details for transactionPoid: {}";
    private static final String LOG_MESSAGE_SAVED_DETAILS = "Saved {} {} details for transactionPoid: {}";
    private static final String LOG_MESSAGE_SAVING_BILLWISE = "Saving billwise breakup for {} GL details, transactionPoid: {}";
    private static final String LOG_MESSAGE_SAVED_BILLWISE = "Saved {} billwise breakup entries for transactionPoid: {}";
    private static final String LOG_MESSAGE_SAVED_COST_CENTER = "Saved {} cost center breakup entries for transactionPoid: {}";
    private static final String LOG_MESSAGE_FETCHING_PJ_REF = "Fetching PJ reference details for pjPoid: {}";
    private static final String LOG_MESSAGE_FETCHED_PJ_REF = "Successfully fetched PJ reference details for pjPoid: {}";
    private static final String LOG_MESSAGE_FETCHING_PARTY = "Fetching party details for partyType: {}, partyPoid: {}";
    private static final String LOG_MESSAGE_FETCHED_PARTY = "Successfully fetched party details for partyType: {}, partyPoid: {}";
    private static final String LOG_MESSAGE_GENERATING_PDF = "Generating PDF for transactionPoid: {}";
    private static final String LOG_MESSAGE_CREATING_ENTITY = "Creating entity with groupPoid: {}, companyPoid: {}";
    private static final String LOG_MESSAGE_FAILED_BREAKUP = "Failed to load breakup lists for transactionPoid: {}, detRowId: {}: {}";
    private static final String LOG_MESSAGE_UNKNOWN_ACTION = "Unknown action type '{}', defaulting to no change";
    private static final String LOG_MESSAGE_UNKNOWN_REF_TYPE_MAPPING = "Unknown reference type for line item mapping: {}";
    private static final String LOG_MESSAGE_OLD_ENTITY_NULL = "Old GL entity is null, skipping log request for transactionPoid: {}, detRowId: {}";
    private static final String LOG_MESSAGE_OLD_ENTITY_NOT_FOUND = "Old GL entity not found for transactionPoid: {}, detRowId: {}";
    private static final String LOG_MESSAGE_EXISTING_ENTITY_NOT_FOUND = "Existing {} entity not found for transactionPoid: {}, detRowId: {}";
    private static final String LOG_MESSAGE_USER_CONTEXT_NULL = "UserContext.{}() returned null, using default value: {}";
    private static final String LOG_MESSAGE_USER_CONTEXT_NULL_UPDATE = "UserContext.{}() returned null during update, using default value: {}";
    private static final String ERROR_MESSAGE_CREATING = "Error creating supplier credit note: {}";
    private static final String ERROR_MESSAGE_UPDATING = "Error updating supplier credit note with transactionPoid {}: {}";
    private static final String ERROR_MESSAGE_DELETING = "Error deleting supplier credit note with transactionPoid {}: {}";
    private static final String ERROR_MESSAGE_FETCHING = "Error fetching supplier credit note with transactionPoid {}: {}";
    private static final String ERROR_MESSAGE_FETCHING_PJ_REF = "Error fetching PJ reference details for pjPoid {}: {}";
    private static final String ERROR_MESSAGE_FETCHING_PARTY = "Error fetching party details for partyType {}, partyPoid {}: {}";
    private static final String ERROR_MESSAGE_PROCESSING_LOGS = "Error processing {} log requests: {}";
    private static final String ERROR_MESSAGE_CREATING_PDF = "Error creating simple PDF: {}";
    private static final String ERROR_MESSAGE_FAILED_CREATE = "Failed to create supplier credit note: ";
    private static final String ERROR_MESSAGE_FAILED_UPDATE = "Failed to update supplier credit note: ";
    private static final String ERROR_MESSAGE_FAILED_DELETE = "Failed to delete supplier credit note: ";
    private static final String ERROR_MESSAGE_FAILED_FETCH = "Failed to fetch supplier credit note: ";
    private static final String ERROR_MESSAGE_FAILED_PJ_REF = "Failed to fetch PJ reference details: ";
    private static final String ERROR_MESSAGE_FAILED_PARTY = "Failed to fetch party details: ";
    private static final String ENTITY_TYPE_ITEM = "Item";
    private static final String ENTITY_TYPE_CHARGE = "Charge";
    private static final String ENTITY_TYPE_GL = "GL";
    private static final String METHOD_GET_GROUP_POID = "getGroupPoid";
    private static final String METHOD_GET_COMPANY_POID = "getCompanyPoid";
    private static final String ENTITY_GL_MASTER = "Gl Master";
    private static final String ENTITY_TAX = "Tax";
    private static final String FIELD_GL_POID_LOWER = "glPoid";
    private static final String FIELD_TAX_POID_LOWER = "taxPoid";
    private static final String ACTION_ISCREATED = "iscreated";
    private static final String ACTION_ISUPDATED = "isupdated";
    private static final String ACTION_ISDELETED = "isdeleted";
    private static final String ACTION_NOCHANGE = "nochange";
    private static final String VALIDATION_UNKNOWN_REF_TYPE = "Unknown reference type: {}, skipping validation";

    private record BreakupContext(
            Long transactionPoid,
            String docId,
            Long groupPoid,
            Long companyPoid,
            Long userPoid) {
    }

    private final ApPurchaseCnHdrRepository hdrRepository;
    private final ApPurchaseCnItemDtlRepository itemDtlRepository;
    private final ApPurchaseCnChargeDtlRepository chargeDtlRepository;
    private final ApPurchaseCnGlDtlRepository glDtlRepository;
    private final ApPurchaseCnProcRepository procRepository;
    private final BillwiseBreakupService billwiseBreakupService;
    private final CostCenterBreakupService costCenterBreakupService;
    private final GLMasterRepository glMasterRepository;
    private final TaxMasterRepository taxMasterRepository;
    private final PrintService printService;
    private final DataSource dataSource;
    private final DocumentSearchService documentSearchService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final LovDataService lovService;

    @Override
    @Transactional
    public ApPurchaseCnHdrDto create(ApPurchaseCnHdrDto dto) {
        log.info(LOG_MESSAGE_CREATING, dto.getPartyType(), dto.getRefType());
        
        try {
            // Before save validation
            Long partyPoid = PARTY_TYPE_SUPPLIER.equals(dto.getPartyType()) ? dto.getSupplierPoid() : dto.getPrincipalPoid();
            log.debug(LOG_MESSAGE_VALIDATION, partyPoid);
            procRepository.beforeSaveValidation(dto.getPartyType(), partyPoid, dto.getRefType(), dto.getPjReversalRef());
            
            ApPurchaseCnHdr hdr = mapToEntity(dto);
            hdr.setCreatedBy(UserContext.getUserId());
            hdr.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
            hdr.setDeleted(DEFAULT_NO_VALUE);
            
            ApPurchaseCnHdr savedHdr = hdrRepository.save(hdr);
            log.info(LOG_MESSAGE_SAVED_HEADER, savedHdr.getTransactionPoid());
            
            saveDetails(savedHdr.getTransactionPoid(), dto);
            log.info(LOG_MESSAGE_CREATED_SUCCESS, savedHdr.getTransactionPoid());
            
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), 
                    savedHdr.getTransactionPoid().toString());
            
            return fetchById(savedHdr.getTransactionPoid());
        } catch (Exception e) {
            log.error(ERROR_MESSAGE_CREATING, e.getMessage(), e);
            throw new DataAccessException(ERROR_MESSAGE_FAILED_CREATE + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApPurchaseCnHdrDto getById(Long transactionPoid) {
        return fetchById(transactionPoid);
    }

    private ApPurchaseCnHdrDto fetchById(Long transactionPoid) {
        log.info(LOG_MESSAGE_FETCHING, transactionPoid);
        
        try {
            ApPurchaseCnHdr hdr = hdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException(ENTITY_SUPPLIER_CREDIT_NOTE, FIELD_TRANSACTION_POID, transactionPoid));
            
            ApPurchaseCnHdrDto dto = mapToDto(hdr);
            
            dto.setItemDetails(itemDtlRepository.findByTransactionPoid(transactionPoid).stream()
                    .map(this::mapItemToDto).toList());
            dto.setChargeDetails(chargeDtlRepository.findByTransactionPoid(transactionPoid).stream()
                    .map(this::mapChargeToDto).toList());
            dto.setGlDetails(glDtlRepository.findByTransactionPoid(transactionPoid).stream()
                    .map(this::mapGlToDto).toList());
            
            log.info(LOG_MESSAGE_FETCHED_SUCCESS, 
                    dto.getItemDetails().size(), dto.getChargeDetails().size(), dto.getGlDetails().size());
            
            return dto;
        } catch (Exception e) {
            log.error(ERROR_MESSAGE_FETCHING, transactionPoid, e.getMessage(), e);
            throw new DataAccessException(ERROR_MESSAGE_FAILED_FETCH + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ApPurchaseCnHdrDto update(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        log.info(LOG_MESSAGE_UPDATING, transactionPoid);
        
        try {
            ApPurchaseCnHdr existing = hdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException(ENTITY_SUPPLIER_CREDIT_NOTE, FIELD_TRANSACTION_POID, transactionPoid));

            ApPurchaseCnHdr oldEntity = new ApPurchaseCnHdr();
            BeanUtils.copyProperties(existing, oldEntity);
            updateEntityFromDto(existing, dto);
            existing.setLastModifiedBy(UserContext.getUserId());
            existing.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
            
            hdrRepository.save(existing);
            log.info(LOG_MESSAGE_HEADER_UPDATED);
            
            updateDetailsByActionType(transactionPoid, dto);
            log.info(LOG_MESSAGE_UPDATED_SUCCESS, transactionPoid);
            loggingService.logChanges(oldEntity, existing, ApPurchaseCnHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, COLUMN_TRANSACTION_POID);
            return fetchById(transactionPoid);


        } catch (Exception e) {
            log.error(ERROR_MESSAGE_UPDATING, transactionPoid, e.getMessage(), e);
            throw new DataAccessException(ERROR_MESSAGE_FAILED_UPDATE + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info(LOG_MESSAGE_DELETING, transactionPoid);
        
        try {
            ApPurchaseCnHdr hdr = hdrRepository.findById(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException(ENTITY_SUPPLIER_CREDIT_NOTE, FIELD_TRANSACTION_POID, transactionPoid));

            documentDeleteService.deleteDocument(
                    transactionPoid,
                    TABLE_AP_PURCHASE_CN_HDR,
                    COLUMN_TRANSACTION_POID,
                    deleteReasonDto,
                    hdr.getTransactionDate()
            );

            log.info(LOG_MESSAGE_DELETED_SUCCESS, transactionPoid);
        } catch (Exception e) {
            log.error(ERROR_MESSAGE_DELETING, transactionPoid, e.getMessage(), e);
            throw new DataAccessException(ERROR_MESSAGE_FAILED_DELETE + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> list(String documentId, FilterRequestDto filters, 
                                     LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, FILTER_TRANSACTION_DATE, startDate, endDate);

        RawSearchResult raw = documentSearchService.search(documentId, filterList, operator, pageable, isDeleted,
                FILTER_LONG_NARRATION,
                COLUMN_TRANSACTION_POID);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public Map<String, Object> getPjRefDetails(Long pjPoid) {
        log.info(LOG_MESSAGE_FETCHING_PJ_REF, pjPoid);
        
        try {
            Map<String, Object> result = procRepository.getPjRefDetails(pjPoid);
            GlVoucherLoadBillwiseBreakupResponseDto blResponse = billwiseBreakupService.loadBillwiseBreakup(
                    UserContext.getGroupPoid(), UserContext.getCompanyPoid(), DOC_ID_AP_PURCHASE_CN, pjPoid);
            GlVoucherCostCenterBreakupResponseDto cCResponse = costCenterBreakupService.loadCostCenterData(
                    DOC_ID_AP_PURCHASE_CN, pjPoid, UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid());
            mapBillwiseAndCostCenterBreakup(result, blResponse, cCResponse);
            log.info(LOG_MESSAGE_FETCHED_PJ_REF, pjPoid);
            return result;
        } catch (Exception e) {
            log.error(ERROR_MESSAGE_FETCHING_PJ_REF, pjPoid, e.getMessage(), e);
            throw new DataAccessException(ERROR_MESSAGE_FAILED_PJ_REF + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getPartyDetails(String partyType, Long partyPoid) {
        log.info(LOG_MESSAGE_FETCHING_PARTY, partyType, partyPoid);
        
        try {
            Map<String, Object> result = procRepository.getPartyDetails(partyType, partyPoid);
            log.info(LOG_MESSAGE_FETCHED_PARTY, partyType, partyPoid);
            return result;
        } catch (Exception e) {
            log.error(ERROR_MESSAGE_FETCHING_PARTY, partyType, partyPoid, e.getMessage(), e);
            throw new DataAccessException(ERROR_MESSAGE_FAILED_PARTY + e.getMessage(), e);
        }
    }

    private String normalizeActionType(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) {
            return ACTION_TYPE_NO_CHANGE;
        }
        
        String normalized = actionType.trim().toLowerCase();
        
        // Handle various case formats for action types
        switch (normalized) {
            case ACTION_ISCREATED -> {
                return ACTION_TYPE_IS_CREATED;
            }
            case ACTION_ISUPDATED -> {
                return ACTION_TYPE_IS_UPDATED;
            }
            case ACTION_ISDELETED -> {
                return ACTION_TYPE_IS_DELETED;
            }
            case ACTION_NOCHANGE -> {
                return ACTION_TYPE_NO_CHANGE;
            }
            default -> {
                log.warn(LOG_MESSAGE_UNKNOWN_ACTION, actionType);
                return ACTION_TYPE_NO_CHANGE;
            }
        }
    }

    private Long getCurrentGroupPoid() {
        return Optional.ofNullable(UserContext.getGroupPoid()).orElse(DEFAULT_USER_CONTEXT_POID);
    }

    private Long getCurrentCompanyPoid() {
        return Optional.ofNullable(UserContext.getCompanyPoid()).orElse(DEFAULT_USER_CONTEXT_POID);
    }

    private Long getCurrentUserPoid() {
        return Optional.ofNullable(UserContext.getUserPoid()).orElse(DEFAULT_USER_CONTEXT_POID);
    }

    private void updateDetailsByActionType(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        validateRefTypeRequirements(dto);
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<LogRequestDto<ApPurchaseCnItemDtl>> itemLogRequests = new ArrayList<>();
        List<LogRequestDto<ApPurchaseCnChargeDtl>> chargeLogRequests = new ArrayList<>();
        List<LogRequestDto<ApPurchaseCnGlDtl>> glLogRequests = new ArrayList<>();

        processItemActions(transactionPoid, dto.getItemDetails(), docId, docKeyPoid, itemLogRequests);
        processChargeActions(transactionPoid, dto.getChargeDetails(), docId, docKeyPoid, chargeLogRequests);
        processGlActions(transactionPoid, dto.getGlDetails(), docId, docKeyPoid, glLogRequests);
        createBatchLogs(itemLogRequests, chargeLogRequests, glLogRequests);
    }

    private void processItemActions(Long transactionPoid,
                                    List<ApPurchaseCnItemDtlDto> itemDetails,
                                    String docId,
                                    String docKeyPoid,
                                    List<LogRequestDto<ApPurchaseCnItemDtl>> itemLogRequests) {
        if (itemDetails == null) {
            return;
        }

        log.debug(LOG_MESSAGE_PROCESSING_ACTIONS, itemDetails.size(), transactionPoid);
        for (ApPurchaseCnItemDtlDto itemDto : itemDetails) {
            String actionType = normalizeActionType(itemDto.getActionType());
            log.debug(LOG_MESSAGE_PROCESSING_ITEM, 
                    itemDto.getDetRowId(), itemDto.getActionType(), actionType);
            
            if (ACTION_TYPE_IS_DELETED.equals(actionType) && itemDto.getDetRowId() != null) {
                deleteItemDetail(transactionPoid, itemDto, docId, docKeyPoid);
            } else if (ACTION_TYPE_IS_UPDATED.equals(actionType) && itemDto.getDetRowId() != null) {
                updateItemDetail(transactionPoid, itemDto, docId, docKeyPoid, itemLogRequests);
            } else if (ACTION_TYPE_IS_CREATED.equals(actionType)) {
                createItemDetail(transactionPoid, itemDto, docId, docKeyPoid);
            }
        }
    }

    private void processChargeActions(Long transactionPoid,
                                      List<ApPurchaseCnChargeDtlDto> chargeDetails,
                                      String docId,
                                      String docKeyPoid,
                                      List<LogRequestDto<ApPurchaseCnChargeDtl>> chargeLogRequests) {
        if (chargeDetails == null) {
            return;
        }

        for (ApPurchaseCnChargeDtlDto chargeDto : chargeDetails) {
            String actionType = normalizeActionType(chargeDto.getActionType());
            if (ACTION_TYPE_IS_DELETED.equals(actionType) && chargeDto.getDetRowId() != null) {
                deleteChargeDetail(transactionPoid, chargeDto, docId, docKeyPoid);
            } else if (ACTION_TYPE_IS_UPDATED.equals(actionType) && chargeDto.getDetRowId() != null) {
                updateChargeDetail(transactionPoid, chargeDto, docId, docKeyPoid, chargeLogRequests);
            } else if (ACTION_TYPE_IS_CREATED.equals(actionType)) {
                createChargeDetail(transactionPoid, chargeDto, docId, docKeyPoid);
            }
        }
    }

    private void processGlActions(Long transactionPoid,
                                  List<ApPurchaseCnGlDtlDto> glDetails,
                                  String docId,
                                  String docKeyPoid,
                                  List<LogRequestDto<ApPurchaseCnGlDtl>> glLogRequests) {
        if (glDetails == null) {
            return;
        }

        log.debug(LOG_MESSAGE_PROCESSING_GL_ACTIONS, glDetails.size(), transactionPoid);
        for (ApPurchaseCnGlDtlDto glDto : glDetails) {
            String actionType = normalizeActionType(glDto.getActionType());
            log.debug(LOG_MESSAGE_PROCESSING_GL, 
                    glDto.getDetRowId(), glDto.getActionType(), actionType);
            
            if (ACTION_TYPE_IS_DELETED.equals(actionType) && glDto.getDetRowId() != null) {
                deleteGlDetail(transactionPoid, glDto, docId, docKeyPoid);
            } else if (ACTION_TYPE_IS_UPDATED.equals(actionType) && glDto.getDetRowId() != null) {
                updateGlDetail(transactionPoid, glDto, docId, docKeyPoid, glLogRequests);
            } else if (ACTION_TYPE_IS_CREATED.equals(actionType)) {
                createGlDetail(transactionPoid, glDto, docId, docKeyPoid);
            } else {
                log.debug(LOG_MESSAGE_NO_ACTION, glDto.getDetRowId(), actionType);
            }
        }

        List<ApPurchaseCnGlDtlDto> activeGlDetails = glDetails.stream()
                .filter(g -> !ACTION_TYPE_IS_DELETED.equals(normalizeActionType(g.getActionType())))
                .toList();
        saveBillwiseForGl(transactionPoid, activeGlDetails, docId, true);
        saveCostCenterForGl(transactionPoid, activeGlDetails, docId, true);
    }

    private void deleteItemDetail(Long transactionPoid, ApPurchaseCnItemDtlDto itemDto, String docId, String docKeyPoid) {
        ApPurchaseCnItemDtl existingItem = itemDtlRepository.findById(
                new com.asg.finance.entity.key.ApPurchaseCnItemDtlKey(transactionPoid, itemDto.getDetRowId()))
                .orElse(null);
        
        if (existingItem != null) {
            // Log detailed deletion for audit trail
            String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, itemDto.getDetRowId());
            loggingService.logChanges(existingItem, null, ApPurchaseCnItemDtl.class, docId, docKeyPoid, LogDetailsEnum.DELETED, logDetail);
            
            itemDtlRepository.deleteById(new com.asg.finance.entity.key.ApPurchaseCnItemDtlKey(transactionPoid, itemDto.getDetRowId()));
            loggingService.logDelete(existingItem, docId, docKeyPoid);
        }
    }

    private void updateItemDetail(Long transactionPoid,
                                  ApPurchaseCnItemDtlDto itemDto,
                                  String docId,
                                  String docKeyPoid,
                                  List<LogRequestDto<ApPurchaseCnItemDtl>> itemLogRequests) {
        ApPurchaseCnItemDtl existingItem = itemDtlRepository.findById(
                new com.asg.finance.entity.key.ApPurchaseCnItemDtlKey(transactionPoid, itemDto.getDetRowId())).orElse(null);
        
        if (existingItem == null) {
            log.warn(LOG_MESSAGE_EXISTING_ENTITY_NOT_FOUND, ENTITY_TYPE_ITEM, transactionPoid, itemDto.getDetRowId());
            return;
        }
        
        // Create a copy of the old entity for logging
        ApPurchaseCnItemDtl oldItem = new ApPurchaseCnItemDtl();
        BeanUtils.copyProperties(existingItem, oldItem);
        
        // Update the existing entity with new values
        ApPurchaseCnItemDtl updatedItem = mapItemToEntity(itemDto);
        updatedItem.setTransactionPoid(transactionPoid);
        updatedItem.setDetRowId(itemDto.getDetRowId());
        // Preserve audit fields from existing entity
        updatedItem.setCreatedBy(existingItem.getCreatedBy());
        updatedItem.setCreatedDate(existingItem.getCreatedDate());
        updatedItem.setLastModifiedBy(UserContext.getUserId());
        updatedItem.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        ApPurchaseCnItemDtl savedItem = itemDtlRepository.save(updatedItem);
        addItemLogRequest(transactionPoid, itemDto.getDetRowId(), docId, docKeyPoid, itemLogRequests, oldItem, savedItem);
    }

    private void createItemDetail(Long transactionPoid, ApPurchaseCnItemDtlDto itemDto, String docId, String docKeyPoid) {
        ApPurchaseCnItemDtl item = mapItemToEntity(itemDto);
        item.setTransactionPoid(transactionPoid);
        item.setDetRowId(getNextItemRowId(transactionPoid));
        item.setCreatedBy(UserContext.getUserId());
        item.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        ApPurchaseCnItemDtl savedItem = itemDtlRepository.save(item);
        
        // Log summary entry for creation
        loggingService.createLogSummaryEntry(docId, docKeyPoid, LOG_ROW_CREATED_ITEM + savedItem.getDetRowId());
    }

    private void deleteChargeDetail(Long transactionPoid, ApPurchaseCnChargeDtlDto chargeDto, String docId, String docKeyPoid) {
        ApPurchaseCnChargeDtl existingCharge = chargeDtlRepository.findById(
                new com.asg.finance.entity.key.ApPurchaseCnChargeDtlKey(transactionPoid, chargeDto.getDetRowId()))
                .orElse(null);
        
        if (existingCharge != null) {
            // Log detailed deletion for audit trail
            String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, chargeDto.getDetRowId());
            loggingService.logChanges(existingCharge, null, ApPurchaseCnChargeDtl.class, docId, docKeyPoid, LogDetailsEnum.DELETED, logDetail);
            
            chargeDtlRepository.deleteById(new com.asg.finance.entity.key.ApPurchaseCnChargeDtlKey(transactionPoid, chargeDto.getDetRowId()));
            loggingService.logDelete(existingCharge, docId, docKeyPoid);
        }
    }

    private void updateChargeDetail(Long transactionPoid,
                                    ApPurchaseCnChargeDtlDto chargeDto,
                                    String docId,
                                    String docKeyPoid,
                                    List<LogRequestDto<ApPurchaseCnChargeDtl>> chargeLogRequests) {
        ApPurchaseCnChargeDtl existingCharge = chargeDtlRepository.findById(
                new com.asg.finance.entity.key.ApPurchaseCnChargeDtlKey(transactionPoid, chargeDto.getDetRowId())).orElse(null);
        
        if (existingCharge == null) {
            log.warn(LOG_MESSAGE_EXISTING_ENTITY_NOT_FOUND, ENTITY_TYPE_CHARGE, transactionPoid, chargeDto.getDetRowId());
            return;
        }
        
        // Create a copy of the old entity for logging
        ApPurchaseCnChargeDtl oldCharge = new ApPurchaseCnChargeDtl();
        BeanUtils.copyProperties(existingCharge, oldCharge);
        
        // Update the existing entity with new values
        ApPurchaseCnChargeDtl updatedCharge = mapChargeToEntity(chargeDto);
        updatedCharge.setTransactionPoid(transactionPoid);
        updatedCharge.setDetRowId(chargeDto.getDetRowId());
        // Preserve audit fields from existing entity
        updatedCharge.setCreatedBy(existingCharge.getCreatedBy());
        updatedCharge.setCreatedDate(existingCharge.getCreatedDate());
        updatedCharge.setLastModifiedBy(UserContext.getUserId());
        updatedCharge.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        ApPurchaseCnChargeDtl savedCharge = chargeDtlRepository.save(updatedCharge);
        addChargeLogRequest(transactionPoid, chargeDto.getDetRowId(), docId, docKeyPoid, chargeLogRequests, oldCharge, savedCharge);
    }

    private void createChargeDetail(Long transactionPoid, ApPurchaseCnChargeDtlDto chargeDto, String docId, String docKeyPoid) {
        ApPurchaseCnChargeDtl charge = mapChargeToEntity(chargeDto);
        charge.setTransactionPoid(transactionPoid);
        charge.setDetRowId(chargeDto.getDetRowId() != null ? chargeDto.getDetRowId() : getNextChargeRowId(transactionPoid));
        charge.setCreatedBy(UserContext.getUserId());
        charge.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        ApPurchaseCnChargeDtl savedCharge = chargeDtlRepository.save(charge);
        
        // Log summary entry for creation
        loggingService.createLogSummaryEntry(docId, docKeyPoid, LOG_ROW_CREATED_CHARGE + savedCharge.getDetRowId());
    }

    private void deleteGlDetail(Long transactionPoid, ApPurchaseCnGlDtlDto glDto, String docId, String docKeyPoid) {
        ApPurchaseCnGlDtl existingGl = glDtlRepository.findById(
                new com.asg.finance.entity.key.ApPurchaseCnGlDtlKey(transactionPoid, glDto.getDetRowId()))
                .orElse(null);
        
        if (existingGl != null) {
            // Log detailed deletion for audit trail
            String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, glDto.getDetRowId());
            loggingService.logChanges(existingGl, null, ApPurchaseCnGlDtl.class, docId, docKeyPoid, LogDetailsEnum.DELETED, logDetail);
            
            glDtlRepository.deleteById(new com.asg.finance.entity.key.ApPurchaseCnGlDtlKey(transactionPoid, glDto.getDetRowId()));
            loggingService.logDelete(existingGl, docId, docKeyPoid);
        }
    }

    private void updateGlDetail(Long transactionPoid,
                                ApPurchaseCnGlDtlDto glDto,
                                String docId,
                                String docKeyPoid,
                                List<LogRequestDto<ApPurchaseCnGlDtl>> glLogRequests) {
        log.debug(LOG_MESSAGE_UPDATING_GL, 
                transactionPoid, glDto.getDetRowId(), glDto.getActionType());
        
        ApPurchaseCnGlDtl existingGl = glDtlRepository.findById(
                new com.asg.finance.entity.key.ApPurchaseCnGlDtlKey(transactionPoid, glDto.getDetRowId())).orElse(null);
        
        if (existingGl == null) {
            log.warn(LOG_MESSAGE_OLD_ENTITY_NOT_FOUND, transactionPoid, glDto.getDetRowId());
            return;
        }
        
        // Create a copy of the old entity for logging
        ApPurchaseCnGlDtl oldGl = new ApPurchaseCnGlDtl();
        BeanUtils.copyProperties(existingGl, oldGl);
        
        // Update the existing entity with new values
        ApPurchaseCnGlDtl updatedGl = mapGlToEntity(glDto);
        updatedGl.setTransactionPoid(transactionPoid);
        updatedGl.setDetRowId(glDto.getDetRowId());
        // Preserve audit fields from existing entity
        updatedGl.setCreatedBy(existingGl.getCreatedBy());
        updatedGl.setCreatedDate(existingGl.getCreatedDate());
        updatedGl.setLastModifiedBy(UserContext.getUserId());
        updatedGl.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        ApPurchaseCnGlDtl savedGl = glDtlRepository.save(updatedGl);
        glDto.setDetRowId(savedGl.getDetRowId());
        
        log.debug(LOG_MESSAGE_ADDING_GL_LOG, transactionPoid, glDto.getDetRowId());
        addGlLogRequest(transactionPoid, glDto.getDetRowId(), docId, docKeyPoid, glLogRequests, oldGl, savedGl);
    }

    private void createGlDetail(Long transactionPoid, ApPurchaseCnGlDtlDto glDto, String docId, String docKeyPoid) {
        ApPurchaseCnGlDtl gl = mapGlToEntity(glDto);
        gl.setTransactionPoid(transactionPoid);
        gl.setDetRowId(glDto.getDetRowId() != null ? glDto.getDetRowId() : getNextGlRowId(transactionPoid));
        gl.setCreatedBy(UserContext.getUserId());
        gl.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        ApPurchaseCnGlDtl savedGl = glDtlRepository.save(gl);
        glDto.setDetRowId(savedGl.getDetRowId());
        
        loggingService.createLogSummaryEntry(docId, docKeyPoid, LOG_ROW_CREATED_GL + savedGl.getDetRowId());
    }

    private void addItemLogRequest(Long transactionPoid,
                                   Long detRowId,
                                   String docId,
                                   String docKeyPoid,
                                   List<LogRequestDto<ApPurchaseCnItemDtl>> itemLogRequests,
                                   ApPurchaseCnItemDtl oldItem,
                                   ApPurchaseCnItemDtl item) {
        if (oldItem == null) {
            return;
        }
        String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId);
        itemLogRequests.add(new LogRequestDto<>(oldItem, item, ApPurchaseCnItemDtl.class, docId, docKeyPoid, logDetail));
    }

    private void addChargeLogRequest(Long transactionPoid,
                                     Long detRowId,
                                     String docId,
                                     String docKeyPoid,
                                     List<LogRequestDto<ApPurchaseCnChargeDtl>> chargeLogRequests,
                                     ApPurchaseCnChargeDtl oldCharge,
                                     ApPurchaseCnChargeDtl charge) {
        if (oldCharge == null) {
            return;
        }
        String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId);
        chargeLogRequests.add(new LogRequestDto<>(oldCharge, charge, ApPurchaseCnChargeDtl.class, docId, docKeyPoid, logDetail));
    }

    private void addGlLogRequest(Long transactionPoid,
                                 Long detRowId,
                                 String docId,
                                 String docKeyPoid,
                                 List<LogRequestDto<ApPurchaseCnGlDtl>> glLogRequests,
                                 ApPurchaseCnGlDtl oldGl,
                                 ApPurchaseCnGlDtl gl) {
        if (oldGl == null) {
            log.warn(LOG_MESSAGE_OLD_ENTITY_NULL, transactionPoid, detRowId);
            return;
        }
        String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId);
        LogRequestDto<ApPurchaseCnGlDtl> logRequest = new LogRequestDto<>(oldGl, gl, ApPurchaseCnGlDtl.class, docId, docKeyPoid, logDetail);
        glLogRequests.add(logRequest);
        log.debug(LOG_MESSAGE_ADDED_GL_LOG, transactionPoid, detRowId, logDetail);
    }

    private void createBatchLogs(List<LogRequestDto<ApPurchaseCnItemDtl>> itemLogRequests,
                                 List<LogRequestDto<ApPurchaseCnChargeDtl>> chargeLogRequests,
                                 List<LogRequestDto<ApPurchaseCnGlDtl>> glLogRequests) {
        log.debug(LOG_MESSAGE_CREATING_BATCH, 
                itemLogRequests.size(), chargeLogRequests.size(), glLogRequests.size());
        
        if (!itemLogRequests.isEmpty()) {
            log.debug(LOG_MESSAGE_PROCESSING_ITEM_LOGS, itemLogRequests.size());
            try {
                loggingService.createLogBatch(itemLogRequests);
                log.info(LOG_MESSAGE_PROCESSED_SUCCESS, itemLogRequests.size(), ENTITY_TYPE_ITEM);
            } catch (Exception e) {
                log.error(ERROR_MESSAGE_PROCESSING_LOGS, ENTITY_TYPE_ITEM, e.getMessage(), e);
            }
        }
        if (!chargeLogRequests.isEmpty()) {
            log.debug(LOG_MESSAGE_PROCESSING_CHARGE_LOGS, chargeLogRequests.size());
            try {
                loggingService.createLogBatch(chargeLogRequests);
                log.info(LOG_MESSAGE_PROCESSED_SUCCESS, chargeLogRequests.size(), ENTITY_TYPE_CHARGE);
            } catch (Exception e) {
                log.error(ERROR_MESSAGE_PROCESSING_LOGS, ENTITY_TYPE_CHARGE, e.getMessage(), e);
            }
        }
        if (!glLogRequests.isEmpty()) {
            log.debug(LOG_MESSAGE_PROCESSING_GL_LOGS, glLogRequests.size());
            try {
                loggingService.createLogBatch(glLogRequests);
                log.info(LOG_MESSAGE_PROCESSED_SUCCESS, glLogRequests.size(), ENTITY_TYPE_GL);
            } catch (Exception e) {
                log.error(ERROR_MESSAGE_PROCESSING_LOGS, ENTITY_TYPE_GL, e.getMessage(), e);
            }
        }
    }

    private Long getNextItemRowId(Long transactionPoid) {
        return itemDtlRepository.findByTransactionPoid(transactionPoid).stream()
            .map(ApPurchaseCnItemDtl::getDetRowId).max(Long::compareTo).orElse(0L) + 1;
    }

    private Long getNextChargeRowId(Long transactionPoid) {
        return chargeDtlRepository.findByTransactionPoid(transactionPoid).stream()
            .map(ApPurchaseCnChargeDtl::getDetRowId).max(Long::compareTo).orElse(0L) + 1;
    }

    private Long getNextGlRowId(Long transactionPoid) {
        return glDtlRepository.findByTransactionPoid(transactionPoid).stream()
            .map(ApPurchaseCnGlDtl::getDetRowId).max(Long::compareTo).orElse(0L) + 1;
    }

    private void saveDetails(Long transactionPoid, ApPurchaseCnHdrDto dto) {
        log.info(LOG_MESSAGE_SAVING_DETAILS, transactionPoid);
        
        validateRefTypeRequirements(dto);
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        saveItemDetails(transactionPoid, dto.getItemDetails(), docId, docKeyPoid);
        saveChargeDetails(transactionPoid, dto.getChargeDetails(), docId, docKeyPoid);
        saveGlDetails(transactionPoid, dto.getGlDetails(), docId, docKeyPoid);
    }
    
    private void saveItemDetails(Long transactionPoid, List<ApPurchaseCnItemDtlDto> itemDetails, String docId, String docKeyPoid) {
        if (itemDetails == null) {
            return;
        }
        
        long rowId = 1;
        for (ApPurchaseCnItemDtlDto itemDto : itemDetails) {
            ApPurchaseCnItemDtl item = mapItemToEntity(itemDto);
            item.setTransactionPoid(transactionPoid);
            item.setDetRowId(rowId++);
            item.setCreatedBy(UserContext.getUserId());
            item.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
            ApPurchaseCnItemDtl savedItem = itemDtlRepository.save(item);
            
            loggingService.createLogSummaryEntry(docId, docKeyPoid, LOG_ROW_CREATED_ITEM + savedItem.getDetRowId());
        }
        log.info(LOG_MESSAGE_SAVED_DETAILS, itemDetails.size(), ENTITY_TYPE_ITEM, transactionPoid);
    }
    
    private void saveChargeDetails(Long transactionPoid, List<ApPurchaseCnChargeDtlDto> chargeDetails, String docId, String docKeyPoid) {
        if (chargeDetails == null) {
            return;
        }
        
        long rowId = 1;
        for (ApPurchaseCnChargeDtlDto chargeDto : chargeDetails) {
            ApPurchaseCnChargeDtl charge = mapChargeToEntity(chargeDto);
            charge.setTransactionPoid(transactionPoid);
            charge.setDetRowId(rowId++);
            charge.setCreatedBy(UserContext.getUserId());
            charge.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
            ApPurchaseCnChargeDtl savedCharge = chargeDtlRepository.save(charge);
            
            loggingService.createLogSummaryEntry(docId, docKeyPoid, LOG_ROW_CREATED_CHARGE + savedCharge.getDetRowId());
        }
        log.info(LOG_MESSAGE_SAVED_DETAILS, chargeDetails.size(), ENTITY_TYPE_CHARGE, transactionPoid);
    }
    
    private void saveGlDetails(Long transactionPoid, List<ApPurchaseCnGlDtlDto> glDetails, String docId, String docKeyPoid) {
        if (glDetails == null) {
            return;
        }
        
        saveGlEntities(transactionPoid, glDetails, docId, docKeyPoid);
        log.info(LOG_MESSAGE_SAVED_DETAILS, glDetails.size(), ENTITY_TYPE_GL, transactionPoid);
        
        saveBillwiseForGl(transactionPoid, glDetails, docId, false);
        saveCostCenterForGl(transactionPoid, glDetails, docId, false);
    }
    
    private void saveGlEntities(Long transactionPoid, List<ApPurchaseCnGlDtlDto> glDetails, String docId, String docKeyPoid) {
        long rowId = 1;
        for (ApPurchaseCnGlDtlDto glDto : glDetails) {
            ApPurchaseCnGlDtl gl = mapGlToEntity(glDto);
            gl.setTransactionPoid(transactionPoid);
            gl.setDetRowId(rowId++);
            gl.setCreatedBy(UserContext.getUserId());
            gl.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
            ApPurchaseCnGlDtl savedGl = glDtlRepository.save(gl);
            
            updateGlDtoWithSavedId(glDto, savedGl, gl);
            loggingService.createLogSummaryEntry(docId, docKeyPoid, LOG_ROW_CREATED_GL + getGlDetRowId(savedGl, gl));
        }
    }
    
    private void updateGlDtoWithSavedId(ApPurchaseCnGlDtlDto glDto, ApPurchaseCnGlDtl savedGl, ApPurchaseCnGlDtl gl) {
        if (savedGl != null) {
            glDto.setDetRowId(savedGl.getDetRowId());
        } else {
            glDto.setDetRowId(gl.getDetRowId());
        }
    }
    
    private Long getGlDetRowId(ApPurchaseCnGlDtl savedGl, ApPurchaseCnGlDtl gl) {
        return savedGl != null ? savedGl.getDetRowId() : gl.getDetRowId();
    }
    
    private void validateRefTypeRequirements(ApPurchaseCnHdrDto dto) {
        String refType = dto.getRefType();
        if (refType == null) {
            throw new ValidationException(ERROR_REF_TYPE_REQUIRED);
        }
        
        switch (refType.toUpperCase()) {
            case REF_TYPE_GENERAL -> validateGeneralRefType(dto);
            case REF_TYPE_FF, REF_TYPE_FF_JOB -> validateFfRefType(dto);
            case REF_TYPE_FDA -> validateFdaRefType(dto);
            case REF_TYPE_PJ_REVERSAL -> validatePjReversalRefType(dto);
            default -> log.warn(VALIDATION_UNKNOWN_REF_TYPE, refType);
        }
    }

    private void validateGeneralRefType(ApPurchaseCnHdrDto dto) {
        if (CollectionUtils.isEmpty(dto.getGlDetails())) {
            throw new ValidationException(ERROR_GL_DETAIL_REQUIRED_PREFIX + REF_TYPE_GENERAL);
        }
    }

    private void validateFfRefType(ApPurchaseCnHdrDto dto) {
        if (CollectionUtils.isEmpty(dto.getChargeDetails())) {
            throw new ValidationException(ERROR_CHARGE_DETAIL_REQUIRED_FOR_FF);
        }
    }

    private void validateFdaRefType(ApPurchaseCnHdrDto dto) {
        if (CollectionUtils.isEmpty(dto.getChargeDetails())) {
            throw new ValidationException(ERROR_CHARGE_DETAIL_REQUIRED_PREFIX + REF_TYPE_FDA);
        }
    }

    private void validatePjReversalRefType(ApPurchaseCnHdrDto dto) {
        if (requiresChargeDetailsForPjReversal(dto)) {
            validatePjReversalChargeDetails(dto);
            return;
        }
        if (requiresGlDetailsForPjReversal(dto)) {
            validatePjReversalGlDetails(dto);
            return;
        }
        validatePjReversalItemDetails(dto);
    }

    private boolean requiresChargeDetailsForPjReversal(ApPurchaseCnHdrDto dto) {
        return REF_TYPE_FF.equalsIgnoreCase(dto.getPjReversalRefType())
                || REF_TYPE_FDA.equalsIgnoreCase(dto.getPjReversalRefType())
                || REF_TYPE_FF_JOB.equalsIgnoreCase(dto.getPjReversalRefType());
    }

    private boolean requiresGlDetailsForPjReversal(ApPurchaseCnHdrDto dto) {
        return REF_TYPE_GENERAL_PO.equalsIgnoreCase(dto.getPjReversalRefType())
                || REF_TYPE_GENERAL.equalsIgnoreCase(dto.getPjReversalRefType());
    }

    private void validatePjReversalChargeDetails(ApPurchaseCnHdrDto dto) {
        if (CollectionUtils.isEmpty(dto.getChargeDetails())) {
            throw new ValidationException(ERROR_PJ_FF_FDA_CHARGE_REQUIRED);
        }
    }

    private void validatePjReversalGlDetails(ApPurchaseCnHdrDto dto) {
        if (CollectionUtils.isEmpty(dto.getGlDetails())) {
            throw new ValidationException(ERROR_PJ_GENERAL_GL_REQUIRED);
        }
    }

    private void validatePjReversalItemDetails(ApPurchaseCnHdrDto dto) {
        if (CollectionUtils.isEmpty(dto.getItemDetails())) {
            throw new ValidationException(ERROR_ITEM_DETAIL_REQUIRED_PREFIX + REF_TYPE_PJ_REVERSAL);
        }
    }

    private void saveBillwiseForGl(Long transactionPoid, List<ApPurchaseCnGlDtlDto> glDetails, String docId, boolean isUpdate) {
        if (glDetails == null || glDetails.isEmpty()) {
            return;
        }
        
        log.info(LOG_MESSAGE_SAVING_BILLWISE, glDetails.size(), transactionPoid);
        
        List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();
        Long groupPoid = getCurrentGroupPoid();
        Long companyPoid = getCurrentCompanyPoid();
        Long userPoid = getCurrentUserPoid();

        for (ApPurchaseCnGlDtlDto glDto : glDetails) {
            if (shouldSkipBillwise(glDto)) {
                continue;
            }
            validateBillwiseReferences(glDto);
            billwiseList.addAll(buildBillwiseRequests(transactionPoid, docId, groupPoid, companyPoid, userPoid, glDto));
        }

        if (!billwiseList.isEmpty()) {
            if (isUpdate) {
                billwiseBreakupService.updateBillwiseBreakups(billwiseList, userPoid);
            } else {
                billwiseBreakupService.insertBillwiseBreakup(billwiseList);
            }
            log.info(LOG_MESSAGE_SAVED_BILLWISE, billwiseList.size(), transactionPoid);
        }
    }

    private boolean shouldSkipBillwise(ApPurchaseCnGlDtlDto glDto) {
        return glDto == null || glDto.getDetRowId() == null;
    }

    private void validateBillwiseReferences(ApPurchaseCnGlDtlDto glDto) {
        if (!glMasterRepository.existsByGlPoid(glDto.getGlPoid())) {
            throw new ResourceNotFoundException(ENTITY_GL_MASTER, FIELD_GL_POID_LOWER, glDto.getGlPoid());
        }
        if (glDto.getTaxPoid() != null && !taxMasterRepository.existsByTaxPoid(glDto.getTaxPoid())) {
            throw new ResourceNotFoundException(ENTITY_TAX, FIELD_TAX_POID_LOWER, glDto.getTaxPoid());
        }
    }

    private List<BillwiseBreakupRequestDto> buildBillwiseRequests(
            Long transactionPoid,
            String docId,
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            ApPurchaseCnGlDtlDto glDto) {
        if (CollectionUtils.isEmpty(glDto.getBreakupList())) {
            return Collections.emptyList();
        }

        BreakupContext context = new BreakupContext(transactionPoid, docId, groupPoid, companyPoid, userPoid);
        List<BillwiseBreakupRequestDto> requests = new ArrayList<>();
        long initialRowId = 1L;
        for (BillwiseBreakupPopupRequestDto popup : glDto.getBreakupList()) {
            requests.add(buildBillwiseRequest(context, glDto, popup, initialRowId));
            initialRowId++;
        }
        return requests;
    }

    private BillwiseBreakupRequestDto buildBillwiseRequest(
            BreakupContext context,
            ApPurchaseCnGlDtlDto glDto,
            BillwiseBreakupPopupRequestDto popup,
            long billDetRowId) {
        BillwiseBreakupRequestDto req = new BillwiseBreakupRequestDto();
        req.setGroupPoid(context.groupPoid());
        req.setCompanyPoid(context.companyPoid());
        req.setDocId(context.docId());
        req.setTransactionPoid(context.transactionPoid());
        req.setBillDetRowId(billDetRowId);
        req.setBillRefType(popup.getBillRefType());
        req.setBillRef(popup.getBillRef());
        req.setBillDueDate(popup.getBillDueDate());
        setBillwiseAmounts(req, popup);
        req.setBillRemarks(popup.getBillRemarks());
        req.setLoginUserPoid(context.userPoid());
        req.setMainDetRowId(glDto.getDetRowId());
        req.setGlCompanyPoid(context.companyPoid());
        req.setGlPoid(glDto.getGlPoid());
        return req;
    }

    private void setBillwiseAmounts(BillwiseBreakupRequestDto req, BillwiseBreakupPopupRequestDto popup) {
        if (BILLWISE_TYPE_DR.equalsIgnoreCase(popup.getType())) {
            req.setDrAmt(popup.getAmount());
            req.setCrAmt(BigDecimal.ZERO);
        } else {
            req.setDrAmt(BigDecimal.ZERO);
            req.setCrAmt(popup.getAmount());
        }
    }

    private void saveCostCenterForGl(Long transactionPoid,
                                     List<ApPurchaseCnGlDtlDto> glDetails,
                                     String docId,
                                     boolean isUpdate) {
        if (CollectionUtils.isEmpty(glDetails)) {
            return;
        }

        List<CostCenterBreakupRequestDto> costCenterList = buildCostCenterList(transactionPoid, docId, glDetails);

        if (!costCenterList.isEmpty()) {
            saveCostCenterBreakups(costCenterList, isUpdate);
            log.info(LOG_MESSAGE_SAVED_COST_CENTER, costCenterList.size(), transactionPoid);
        }
    }
    
    private List<CostCenterBreakupRequestDto> buildCostCenterList(Long transactionPoid, String docId, List<ApPurchaseCnGlDtlDto> glDetails) {
        List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();
        Long groupPoid = getCurrentGroupPoid();
        Long companyPoid = getCurrentCompanyPoid();
        Long userPoid = getCurrentUserPoid();

        long initialRowId = 1L;
        for (ApPurchaseCnGlDtlDto glDto : glDetails) {
            if (shouldSkipCostCenter(glDto)) {
                continue;
            }

            List<CostCenterBreakupRequestDto> requests = buildCostCenterRequests(
                    transactionPoid, docId, groupPoid, companyPoid, userPoid, glDto, initialRowId);
            costCenterList.addAll(requests);
            initialRowId += requests.size();
        }
        return costCenterList;
    }
    
    private void saveCostCenterBreakups(List<CostCenterBreakupRequestDto> costCenterList, boolean isUpdate) {
        if (isUpdate) {
            costCenterBreakupService.updateCostCenterBreakups(costCenterList, getCurrentUserPoid());
        } else {
            costCenterBreakupService.saveCostCenterBreakups(costCenterList);
        }
    }

    private boolean shouldSkipCostCenter(ApPurchaseCnGlDtlDto glDto) {
        return glDto == null || glDto.getDetRowId() == null || CollectionUtils.isEmpty(glDto.getCostCenterList());
    }

    private List<CostCenterBreakupRequestDto> buildCostCenterRequests(
            Long transactionPoid,
            String docId,
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            ApPurchaseCnGlDtlDto glDto,
            long initialRowId) {
        BreakupContext context = new BreakupContext(transactionPoid, docId, groupPoid, companyPoid, userPoid);
        List<CostCenterBreakupRequestDto> requests = new ArrayList<>();
        long currentRowId = initialRowId;

        for (CostCenterBreakupPopupRequestDto popup : glDto.getCostCenterList()) {
            requests.add(buildCostCenterRequest(context, glDto, popup, currentRowId));
            currentRowId++;
        }
        return requests;
    }

    private CostCenterBreakupRequestDto buildCostCenterRequest(
            BreakupContext context,
            ApPurchaseCnGlDtlDto glDto,
            CostCenterBreakupPopupRequestDto popup,
            long costDetRowId) {
        CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();
        dto.setGroupPoid(context.groupPoid());
        dto.setCompanyPoid(context.companyPoid());
        dto.setDocId(context.docId());
        dto.setTransactionPoid(context.transactionPoid());
        dto.setMainDetRowId(glDto.getDetRowId());
        dto.setGlPoid(glDto.getGlPoid());
        dto.setCostDetRowId(costDetRowId);
        dto.setCostGroup(popup.getCostGroup());
        dto.setCostPoid(popup.getCostPoid());
        dto.setAmount(popup.getAmount());
        dto.setLoginUserPoid(context.userPoid());
        return dto;
    }

    @Override
    public byte[] print(Long transactionPoid) {
        log.info(LOG_MESSAGE_GENERATING_PDF, transactionPoid);
        try {
            Map<String, Object> params = printService.buildBaseParams(transactionPoid, DOC_ID_AP_PURCHASE_CN);
            params.put("SUBREPORT_GL", printService.load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml"));
            params.put("SUBREPORT_CHARGE", printService.load("Finance/AP/PurchaseInvoiceChargeSubReport.jrxml"));
            JasperReport mainReport = printService.load("Finance/AP/PurchaseInvoiceReport_2.jrxml");
            return printService.fillReportToPdf(mainReport, params, dataSource);
        } catch (Exception e) {
            log.error(ERROR_MESSAGE_CREATING_PDF, e.getMessage(), e);
            throw new DataAccessException("Failed to generate PDF for transaction: " + transactionPoid, e);
        }
    }

    private ApPurchaseCnHdr mapToEntity(ApPurchaseCnHdrDto dto) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        // Fallback to default values if UserContext returns null
        if (groupPoid == null) {
            groupPoid = DEFAULT_USER_CONTEXT_POID;
            log.warn(LOG_MESSAGE_USER_CONTEXT_NULL, METHOD_GET_GROUP_POID, groupPoid);
        }
        if (companyPoid == null) {
            companyPoid = DEFAULT_USER_CONTEXT_POID;
            log.warn(LOG_MESSAGE_USER_CONTEXT_NULL, METHOD_GET_COMPANY_POID, companyPoid);
        }
        
        log.info(LOG_MESSAGE_CREATING_ENTITY, groupPoid, companyPoid);
        
        return ApPurchaseCnHdr.builder()
                .transactionDate(dto.getTransactionDate())
                .docRef(dto.getDocRef())
                .poRef(dto.getPoRef())
                .groupPoid(groupPoid)
                .companyPoid(companyPoid)
                .currencyCode(dto.getCurrencyCode())
                .currencyRate(dto.getCurrencyRate())
                .supplierPoid(dto.getSupplierPoid())
                .subTotal(dto.getSubTotal())
                .discount(dto.getDiscount())
                .grandTotal(dto.getGrandTotal())
                .remarks(dto.getRemarks())
                .itemTotal(dto.getItemTotal())
                .chargeTotal(dto.getChargeTotal())
                .glTotal(dto.getGlTotal())
                .type(dto.getType())
                .description(dto.getDescription())
                .creditPeriod(dto.getCreditPeriod())
                .dueDate(dto.getDueDate())
                .refType(dto.getRefType())
                .narration(dto.getNarration())
                .supplierCnDate(dto.getSupplierCnDate())
                .supplierCnNo(dto.getSupplierCnNo())
                .supplierCnRemark(dto.getSupplierCnRemark())
                .multiCompany(Boolean.TRUE.equals(dto.getMultiCompany()) ? DEFAULT_YES_NO_VALUE : DEFAULT_NO_VALUE)
                .bhdAmount(dto.getCurrencyRate().multiply(dto.getSupplierCnAmount()))
                .supplierCnAmount(dto.getSupplierCnAmount())
                .roundingAmount(dto.getRoundingAmount())
                .partyType(dto.getPartyType())
                .partyTinNumber(dto.getPartyTinNumber())
                .fdaCoveringRef(dto.getFdaCoveringRef())
                .pjReversalRef(dto.getPjReversalRef())
                .pjReversalRefType(dto.getPjReversalRefType())
                .pjReversalRefDetails(dto.getPjReversalRefDetails())
                .ffRef(dto.getFfRef())
                .build();
    }

    private void updateEntityFromDto(ApPurchaseCnHdr entity, ApPurchaseCnHdrDto dto) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        // Fallback to default values if UserContext returns null
        if (groupPoid == null) {
            groupPoid = DEFAULT_USER_CONTEXT_POID;
            log.warn(LOG_MESSAGE_USER_CONTEXT_NULL_UPDATE, METHOD_GET_GROUP_POID, groupPoid);
        }
        if (companyPoid == null) {
            companyPoid = DEFAULT_USER_CONTEXT_POID;
            log.warn(LOG_MESSAGE_USER_CONTEXT_NULL_UPDATE, METHOD_GET_COMPANY_POID, companyPoid);
        }
        
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setPoRef(dto.getPoRef());
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setCurrencyRate(dto.getCurrencyRate());
        entity.setSupplierPoid(dto.getSupplierPoid());
        entity.setSubTotal(dto.getSubTotal());
        entity.setDiscount(dto.getDiscount());
        entity.setGrandTotal(dto.getGrandTotal());
        entity.setRemarks(dto.getRemarks());
        entity.setItemTotal(dto.getItemTotal());
        entity.setChargeTotal(dto.getChargeTotal());
        entity.setGlTotal(dto.getGlTotal());
        entity.setType(dto.getType());
        entity.setDescription(dto.getDescription());
        entity.setCreditPeriod(dto.getCreditPeriod());
        entity.setDueDate(dto.getDueDate());
        entity.setRefType(dto.getRefType());
        entity.setNarration(dto.getNarration());
        entity.setSupplierCnDate(dto.getSupplierCnDate());
        entity.setSupplierCnNo(dto.getSupplierCnNo());
        entity.setSupplierCnRemark(dto.getSupplierCnRemark());
        entity.setMultiCompany(Boolean.TRUE.equals(dto.getMultiCompany()) ? DEFAULT_YES_NO_VALUE : DEFAULT_NO_VALUE);
        entity.setBhdAmount(dto.getCurrencyRate().multiply(dto.getSupplierCnAmount()));
        entity.setSupplierCnAmount(dto.getSupplierCnAmount());
        entity.setRoundingAmount(dto.getRoundingAmount());
        entity.setPartyType(dto.getPartyType());
        entity.setPartyTinNumber(dto.getPartyTinNumber());
        entity.setFdaCoveringRef(dto.getFdaCoveringRef());
        entity.setPjReversalRef(dto.getPjReversalRef());
        entity.setPjReversalRefType(dto.getPjReversalRefType());
        entity.setPjReversalRefDetails(dto.getPjReversalRefDetails());
        entity.setFfRef(dto.getFfRef());
    }

    private ApPurchaseCnHdrDto mapToDto(ApPurchaseCnHdr entity) {
        ApPurchaseCnHdrDto dto = new ApPurchaseCnHdrDto();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setDocRef(entity.getDocRef());
        dto.setPoRef(entity.getPoRef());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencyRate(entity.getCurrencyRate());
        dto.setSupplierPoid(entity.getSupplierPoid());
        dto.setSubTotal(entity.getSubTotal());
        dto.setDiscount(entity.getDiscount());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setRemarks(entity.getRemarks());
        dto.setItemTotal(entity.getItemTotal());
        dto.setChargeTotal(entity.getChargeTotal());
        dto.setGlTotal(entity.getGlTotal());
        dto.setType(entity.getType());
        dto.setDescription(entity.getDescription());
        dto.setCreditPeriod(entity.getCreditPeriod());
        dto.setDueDate(entity.getDueDate());
        dto.setRefType(entity.getRefType());
        dto.setNarration(entity.getNarration());
        dto.setSupplierCnDate(entity.getSupplierCnDate());
        dto.setSupplierCnNo(entity.getSupplierCnNo());
        dto.setSupplierCnRemark(entity.getSupplierCnRemark());
        dto.setMultiCompany(DEFAULT_YES_NO_VALUE.equals(entity.getMultiCompany()));
        dto.setBhdAmount(entity.getBhdAmount());
        dto.setSupplierCnAmount(entity.getSupplierCnAmount());
        dto.setRoundingAmount(entity.getRoundingAmount());
        dto.setPartyType(entity.getPartyType());
        dto.setPartyTinNumber(entity.getPartyTinNumber());
        dto.setFdaCoveringRef(entity.getFdaCoveringRef());
        dto.setPjReversalRef(entity.getPjReversalRef());
        dto.setPjReversalRefType(entity.getPjReversalRefType());
        dto.setPjReversalRefDetails(entity.getPjReversalRefDetails());
        dto.setFfRef(entity.getFfRef());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        
        // Set party details based on party type
        if (PARTY_TYPE_SUPPLIER.equals(entity.getPartyType()) && entity.getSupplierPoid() != null) {
            dto.setPartyDet(lovService.getDetailsByPoidAndLovName(entity.getSupplierPoid(), LOV_SUPPLIER_MASTER_FOR_PJ_CN));
        }
        
        return dto;
    }

    private ApPurchaseCnItemDtl mapItemToEntity(ApPurchaseCnItemDtlDto dto) {
        return ApPurchaseCnItemDtl.builder()
                .stockPoid(dto.getStockPoid())
                .stockUnitPoid(dto.getStockUnitPoid())
                .quantity(dto.getQuantity())
                .price(dto.getPrice())
                .discount(dto.getDiscount())
                .total(dto.getTotal())
                .remarks(dto.getRemarks())
                .refDocId(dto.getRefDocId())
                .refDocPoid(dto.getRefDocPoid())
                .checkAll(dto.getCheckAll())
                .refDetRowId(dto.getRefDetRowId())
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .amount(dto.getAmount())
                .baseAmount(dto.getBaseAmount())
                .build();
    }

    private ApPurchaseCnItemDtlDto mapItemToDto(ApPurchaseCnItemDtl entity) {
        ApPurchaseCnItemDtlDto dto = new ApPurchaseCnItemDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setStockPoid(entity.getStockPoid());
        if (entity.getStockPoid() != null) {
            dto.setStockDet(lovService.getDetailsByPoidAndLovName(entity.getStockPoid(), LOV_STOCK_MASTER));
        }
        dto.setStockUnitPoid(entity.getStockUnitPoid());
        if (entity.getStockUnitPoid() != null) {
            dto.setStockUnitDet(lovService.getDetailsByPoidAndLovName(entity.getStockUnitPoid(), LOV_STOCK_UNIT));
        }
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setDiscount(entity.getDiscount());
        dto.setTotal(entity.getTotal());
        dto.setRemarks(entity.getRemarks());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setCheckAll(entity.getCheckAll());
        dto.setRefDetRowId(entity.getRefDetRowId());
        dto.setTaxPoid(entity.getTaxPoid());
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), LOV_INPUT_TAX_MASTER));
        }
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setAmount(entity.getAmount());
        dto.setBaseAmount(entity.getBaseAmount());
        
        return dto;
    }

    private ApPurchaseCnChargeDtl mapChargeToEntity(ApPurchaseCnChargeDtlDto dto) {
        return ApPurchaseCnChargeDtl.builder()
                .chargePoid(dto.getChargePoid())
                .chargeAmount(dto.getChargeAmount())
                .description(dto.getDescription())
                .remarks(dto.getRemarks())
                .refDocId(dto.getRefDocId())
                .refDocPoid(dto.getRefDocPoid())
                .refDetRowId(dto.getRefDetRowId())
                .checkAll(dto.getCheckAll())
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .chargeBaseAmount(dto.getChargeBaseAmount())
                .chargeFrom(dto.getChargeFrom())
                .supplierPoidFf(dto.getSupplierPoidFf())
                .build();
    }

    private ApPurchaseCnChargeDtlDto mapChargeToDto(ApPurchaseCnChargeDtl entity) {
        ApPurchaseCnChargeDtlDto dto = new ApPurchaseCnChargeDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setChargePoid(entity.getChargePoid());
        if (entity.getChargePoid() != null) {
            // Use appropriate LOV based on charge context - FF vs FDA
            // This should ideally be determined by the refType context, but for now using a generic LOV
            // TODO: Implement dynamic LOV selection based on refType (FF_CHARGE_MASTER_PJ vs FDA_CHARGE_MASTER_PJ)
            dto.setChargeDet(lovService.getDetailsByPoidAndLovName(entity.getChargePoid(), LOV_FF_CHARGE_MASTER_PJ));
        }
        dto.setChargeAmount(entity.getChargeAmount());
        dto.setDescription(entity.getDescription());
        dto.setRemarks(entity.getRemarks());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        if (entity.getRefDocPoid() != null) {
            // Use appropriate LOV based on refDocId - this might need refinement based on business logic
            dto.setRefDocDet(lovService.getDetailsByPoidAndLovName(entity.getRefDocPoid(), LOV_FF_JOBNO));
        }
        dto.setRefDetRowId(entity.getRefDetRowId());
        dto.setCheckAll(entity.getCheckAll());
        dto.setTaxPoid(entity.getTaxPoid());
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), LOV_INPUT_TAX_MASTER));
        }
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setChargeBaseAmount(entity.getChargeBaseAmount());
        dto.setChargeFrom(entity.getChargeFrom());
        dto.setSupplierPoidFf(entity.getSupplierPoidFf());
        
        return dto;
    }

    private ApPurchaseCnGlDtl mapGlToEntity(ApPurchaseCnGlDtlDto dto) {
        return ApPurchaseCnGlDtl.builder()
                .type(dto.getType())
                .companyPoid(dto.getCompanyPoid())
                .glPoid(dto.getGlPoid())
                .drAmount(dto.getDrAmount())
                .crAmount(dto.getCrAmount())
                .refDocId(dto.getRefDocId())
                .refDocPoid(dto.getRefDocPoid())
                .description(dto.getDescription())
                .remarks(dto.getRemarks())
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .totalAmount(dto.getTotalAmount())
                .build();
    }

    private ApPurchaseCnGlDtlDto mapGlToDto(ApPurchaseCnGlDtl entity) {
        ApPurchaseCnGlDtlDto dto = new ApPurchaseCnGlDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setType(entity.getType());
        dto.setCompanyPoid(entity.getCompanyPoid());
        if (entity.getCompanyPoid() != null) {
            dto.setCompanyDet(lovService.getDetailsByPoidAndLovName(entity.getCompanyPoid(), LOV_COMPANY));
        }
        dto.setGlPoid(entity.getGlPoid());
        if (entity.getGlPoid() != null) {
            dto.setGlDet(lovService.getDetailsByPoidAndLovName(entity.getGlPoid(), LOV_GL_MASTER_LEDGERS_PJ));
        }
        if (entity.getType() != null) {
            dto.setTypeDet(lovService.getDetailsByCodeAndLovName(entity.getType(), LOV_ACC_TYPE_SHORT));
        }
        dto.setDrAmount(entity.getDrAmount());
        dto.setCrAmount(entity.getCrAmount());
        dto.setRefDocId(entity.getRefDocId());
        dto.setRefDocPoid(entity.getRefDocPoid());
        dto.setGlDescription(entity.getDescription());
        dto.setDescription(entity.getDescription());
        dto.setRemarks(entity.getRemarks());
        dto.setTaxPoid(entity.getTaxPoid());
        if (entity.getTaxPoid() != null) {
            dto.setTaxDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), LOV_PJ_GL_INPUT_TAX));
        }
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        
        // Load breakup lists
        loadBreakupLists(entity.getTransactionPoid(), entity.getDetRowId(), dto);
        
        return dto;
    }
    
    private void loadBreakupLists(Long transactionPoid, Long detRowId, ApPurchaseCnGlDtlDto dto) {
        try {
            dto.setBreakupList(loadBillwisePopupList(transactionPoid, detRowId));
            dto.setCostCenterList(loadCostCenterPopupList(transactionPoid, detRowId));
        } catch (Exception e) {
            log.warn(LOG_MESSAGE_FAILED_BREAKUP, 
                transactionPoid, detRowId, e.getMessage());
            dto.setBreakupList(Collections.emptyList());
            dto.setCostCenterList(Collections.emptyList());
        }
    }

    private List<BillwiseBreakupPopupRequestDto> loadBillwisePopupList(Long transactionPoid, Long detRowId) {
        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = billwiseBreakupService.loadBillwiseBreakup(
                getCurrentGroupPoid(),
                getCurrentCompanyPoid(),
                DOC_ID_AP_PURCHASE_CN_GL,
                transactionPoid
        );

        if (billwiseResponse == null || billwiseResponse.getLoadBillwiseBreakupResponseDtoList() == null) {
            return Collections.emptyList();
        }

        return billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                .filter(b -> detRowId.equals(b.getMainDetRowId()))
                .map(this::mapBillwisePopup)
                .toList();
    }

    private BillwiseBreakupPopupRequestDto mapBillwisePopup(
            com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto billwiseDto) {
        BillwiseBreakupPopupRequestDto popup = new BillwiseBreakupPopupRequestDto();
        popup.setBillDetRowId(billwiseDto.getBillDetRowId());
        popup.setBillRefType(billwiseDto.getBillRefType());
        popup.setBillRef(billwiseDto.getBillRef());
        popup.setBillDueDate(billwiseDto.getBillDueDate());

        BigDecimal drAmt = billwiseDto.getDrAmt() != null ? billwiseDto.getDrAmt() : BigDecimal.ZERO;
        BigDecimal crAmt = billwiseDto.getCrAmt() != null ? billwiseDto.getCrAmt() : BigDecimal.ZERO;
        popup.setType(drAmt.compareTo(BigDecimal.ZERO) > 0 ? BILLWISE_TYPE_DR : BILLWISE_TYPE_CR);
        popup.setAmount(drAmt.compareTo(BigDecimal.ZERO) > 0 ? drAmt : crAmt);
        popup.setBillRemarks(billwiseDto.getBillRemarks());
        return popup;
    }

    private List<CostCenterBreakupPopupRequestDto> loadCostCenterPopupList(Long transactionPoid, Long detRowId) {
        GlVoucherCostCenterBreakupResponseDto costCenterResponse = costCenterBreakupService.loadCostCenterData(
                DOC_ID_AP_PURCHASE_CN_GL,
                transactionPoid,
                getCurrentGroupPoid(),
                getCurrentCompanyPoid(),
                getCurrentUserPoid()
        );

        if (costCenterResponse == null || costCenterResponse.getCostBreakupList() == null) {
            return Collections.emptyList();
        }

        return costCenterResponse.getCostBreakupList().stream()
                .filter(c -> detRowId.equals(c.getMainDetRowId()))
                .map(this::mapCostCenterPopup)
                .toList();
    }

    private CostCenterBreakupPopupRequestDto mapCostCenterPopup(CostCenterBreakupResponseDto costCenterDto) {
        CostCenterBreakupPopupRequestDto popup = new CostCenterBreakupPopupRequestDto();
        popup.setCostDetRowId(costCenterDto.getCostDetRowId());
        popup.setCostGroup(costCenterDto.getCostGroup());
        popup.setCostPoid(costCenterDto.getCostPoid());
        popup.setAmount(costCenterDto.getAmount());

        if (StringUtils.isNotEmpty(costCenterDto.getCostPoid()) && StringUtils.isNotEmpty(costCenterDto.getCostGroup())) {
            popup.setCostCenterDetails(
                    lovService.getDetailsByPoidAndLovName(Long.valueOf(costCenterDto.getCostPoid()), costCenterDto.getCostGroup()));
        }

        return popup;
    }

    private void mapBillwiseAndCostCenterBreakup(
            Map<String, Object> params,
            GlVoucherLoadBillwiseBreakupResponseDto billResponse,
            GlVoucherCostCenterBreakupResponseDto costResponse
    ) {
        String refType = (String) params.get(PARAM_PJ_REF_TYPE);
        List<Map<String, Object>> lineItems = getLineItems(params);

        if (CollectionUtils.isEmpty(lineItems)) {
            return;
        }

        mapLineItemsByRefType(refType, lineItems, billResponse, costResponse);
    }
    
    private void mapLineItemsByRefType(String refType, 
                                      List<Map<String, Object>> lineItems,
                                      GlVoucherLoadBillwiseBreakupResponseDto billResponse,
                                      GlVoucherCostCenterBreakupResponseDto costResponse) {
        switch (refType) {
            case REF_TYPE_GENERAL -> mapGeneralLineItems(lineItems, billResponse, costResponse);
            case REF_TYPE_FDA -> mapFdaLineItems(lineItems);
            case REF_TYPE_FF, REF_TYPE_FF_JOB -> mapFfLineItems(lineItems);
            case REF_TYPE_MTA_PO -> mapMtaPoLineItems(lineItems);
            default -> log.warn(LOG_MESSAGE_UNKNOWN_REF_TYPE_MAPPING, refType);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getLineItems(Map<String, Object> params) {
        return (List<Map<String, Object>>) params.get(PARAM_LINE_ITEMS);
    }

    private void mapGeneralLineItems(List<Map<String, Object>> lineItems,
                                     GlVoucherLoadBillwiseBreakupResponseDto billResponse,
                                     GlVoucherCostCenterBreakupResponseDto costResponse) {
        for (Map<String, Object> lineItem : lineItems) {
            mapGeneralLineItem(lineItem, billResponse, costResponse);
        }
    }
    
    private void mapGeneralLineItem(Map<String, Object> lineItem,
                                   GlVoucherLoadBillwiseBreakupResponseDto billResponse,
                                   GlVoucherCostCenterBreakupResponseDto costResponse) {
        Long companyPoid = getLongValue(lineItem, FIELD_COMPANY_POID);
        Long glPoid = getLongValue(lineItem, FIELD_GL_POID);

        lineItem.put(FIELD_COMPANY_DET, lovService.getDetailsByPoidAndLovName(companyPoid, LOV_COMPANY));
        lineItem.put(FIELD_GL_DET, lovService.getDetailsByPoidAndLovName(glPoid, LOV_GL_MASTER_LEDGERS_PJ));
        putTypeDetail(lineItem);
        putTaxDetail(lineItem, LOV_PJ_GL_INPUT_TAX);
        lineItem.put(FIELD_BILL_WISE_BREAK_UP_LIST, filterBillwiseBreakup(billResponse, glPoid));
        lineItem.put(FIELD_COST_CENTER_BREAK_UP_LIST, filterCostCenterBreakup(costResponse, glPoid));
    }

    private void mapFdaLineItems(List<Map<String, Object>> lineItems) {
        for (Map<String, Object> lineItem : lineItems) {
            putDetailByPoid(lineItem, FIELD_CHARGE_POID, FIELD_CHARGE_DET, LOV_FDA_CHARGE_MASTER_PJ);
            putTaxDetail(lineItem, LOV_INPUT_TAX_MASTER);
        }
    }

    private void mapFfLineItems(List<Map<String, Object>> lineItems) {
        for (Map<String, Object> lineItem : lineItems) {
            putDetailByPoid(lineItem, FIELD_CHARGE_POID, FIELD_CHARGE_DET, LOV_FF_CHARGE_MASTER_PJ);
            putTaxDetail(lineItem, LOV_INPUT_TAX_MASTER);
            putDetailByPoid(lineItem, FIELD_REF_DOC_POID, FIELD_REF_DOC_DET, LOV_FF_JOBNO);
        }
    }

    private void mapMtaPoLineItems(List<Map<String, Object>> lineItems) {
        for (Map<String, Object> lineItem : lineItems) {
            putDetailByPoid(lineItem, FIELD_STOCK_POID, FIELD_STOCK_DET, LOV_STOCK_MASTER);
            putDetailByPoid(lineItem, FIELD_STOCK_UNIT_POID, FIELD_STOCK_UNIT_DET, LOV_STOCK_UNIT);
            putTaxDetail(lineItem, LOV_INPUT_TAX_MASTER);
        }
    }

    private void putTypeDetail(Map<String, Object> lineItem) {
        String type = (String) lineItem.get(FIELD_TYPE);
        if (type != null) {
            lineItem.put(FIELD_TYPE_DET, lovService.getDetailsByCodeAndLovName(type, LOV_ACC_TYPE_SHORT));
        }
    }

    private void putTaxDetail(Map<String, Object> lineItem, String lovName) {
        Object taxPoidObj = lineItem.get(FIELD_TAX_POID);
        if (taxPoidObj != null) {
            Long taxPoid = ((Number) taxPoidObj).longValue();
            lineItem.put(FIELD_TAX_DET, lovService.getDetailsByPoidAndLovName(taxPoid, lovName));
        }
    }

    private void putDetailByPoid(Map<String, Object> lineItem, String sourceField, String targetField, String lovName) {
        Object poidObj = lineItem.get(sourceField);
        if (poidObj != null) {
            Long poid = ((Number) poidObj).longValue();
            lineItem.put(targetField, lovService.getDetailsByPoidAndLovName(poid, lovName));
        }
    }

    private Long getLongValue(Map<String, Object> lineItem, String fieldName) {
        return ((Number) lineItem.get(fieldName)).longValue();
    }

    private List<CostCenterBreakupPopupRequestDto> filterCostCenterBreakup(
            GlVoucherCostCenterBreakupResponseDto response, Long detRowId) {

        if (response == null || response.getCostBreakupList() == null) {
            return Collections.emptyList();
        }

        return response.getCostBreakupList().stream()
                .filter(cc -> cc.getGlPoid().equals(detRowId))
                .map(cc -> {
                    CostCenterBreakupPopupRequestDto dto = new CostCenterBreakupPopupRequestDto();
                    dto.setCostDetRowId(cc.getCostDetRowId());
                    dto.setCostGroup(cc.getCostGroup());
                    dto.setCostPoid(cc.getCostPoid());
                    dto.setAmount(cc.getAmount());
                    return dto;
                })
                .toList();
    }

    private List<BillwiseBreakupPopupRequestDto> filterBillwiseBreakup(
            GlVoucherLoadBillwiseBreakupResponseDto response, Long detRowId) {

        if (response == null || response.getLoadBillwiseBreakupResponseDtoList() == null) {
            return Collections.emptyList();
        }

        return response.getLoadBillwiseBreakupResponseDtoList().stream()
                .filter(bw -> bw.getGlPoid().equals(detRowId))
                .map(bw -> {
                    BillwiseBreakupPopupRequestDto dto = new BillwiseBreakupPopupRequestDto();
                    dto.setBillDetRowId(bw.getBillDetRowId());
                    dto.setBillRefType(bw.getBillRefType());
                    dto.setBillRef(bw.getBillRef());
                    dto.setBillDueDate(bw.getBillDueDate());
                    dto.setAmount(bw.getDrAmt() != null ? bw.getDrAmt() : bw.getCrAmt());
                    dto.setType(bw.getDrAmt() != null && bw.getDrAmt().compareTo(BigDecimal.ZERO) > 0
                            ? BILLWISE_DISPLAY_DR : BILLWISE_DISPLAY_CR);
                    dto.setBillRemarks(bw.getBillRemarks());
                    return dto;
                })
                .toList();
    }
}
