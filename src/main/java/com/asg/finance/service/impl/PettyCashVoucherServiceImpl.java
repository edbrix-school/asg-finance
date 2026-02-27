package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.ShowPendingBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GLPettyCashItemDtl;
import com.asg.finance.dto.GlPettyCashItemDtlRequestDto;
import com.asg.finance.entity.GlPettyCashChargeDtl;
import com.asg.finance.entity.GlPettyCashPaymentDtl;
import com.asg.finance.entity.GlPettyCashPaymentHdr;
import com.asg.finance.entity.master.ShipChargeEntity;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.PettyCashVoucherService;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PettyCashVoucherServiceImpl implements PettyCashVoucherService {

    private final GlPettyCashPaymentHdrRepository glPettyCashPaymentHdrRepository;
    private final GlPettyCashPaymentDtlRepository glPettyCashPaymentDtlRepository;
    private final GLPettyCashItemDtlRepository glPettyCashItemDtlRepository;
    private final GlPettyCashChargeDtlRepository glPettyCashChargeDtlRepository;

    private final GLMasterRepository glMasterRepository;
    private final StockMasterRepository stockMasterRepository;
    private final ShipChargeRepository shipChargeRepository;
    private final UnitMasterRepository unitMasterRepository;
    private final DocumentSearchService documentService;
    private final TaxMasterRepository taxMasterRepository;


    private final CostCenterBreakupService costCenterBreakupService;
    private final BillwiseBreakupService billwiseBreakupService;
    private final DocumentDeleteService documentDeleteService;

    private final PettyCashLoadByRefTypeRepository pettyCashLoadByRefTypeRepository;
    private final PettyCashPaymentVoucherCustomRepository pettyCashPaymentVoucherCustomRepository;

    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;

    @Override
    @Transactional
    public PettyCashResponseDto createPettyCash(PettyCashCreateRequestDto requestDto, String documentId) {
        StringBuilder result = new StringBuilder();

        try {
            GlPettyCashPaymentHdr header = buildPettyCashHeader(requestDto);


            pettyCashPaymentVoucherCustomRepository.validateGlVouchers(
                    UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                    requestDto.getDocId(), requestDto.getRefType(),
                    requestDto.getFdaRef(), result
            );
            logResult("PROC_GL_VOUCHERS_VALIDATIONS", result);

            StringBuilder glPoidString = new StringBuilder();


            StringBuilder taxInputGlPoid = new StringBuilder();
            pettyCashPaymentVoucherCustomRepository.validateBeforeSave(
                    UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                    requestDto.getDocId(), requestDto.getRefType(),
                    glPoidString.toString(), glPoidString.toString(),
                    "", requestDto.getRefType(),
                    requestDto.getPettyCashGlPoid(), taxInputGlPoid, result
            );
            logResult("PROC_GL_DTL_BEFORE_SAVE_VAL_V2", result);


            List<AdvanceDetailDto> advanceDetails = new ArrayList<>();
            if ("AGAINST_ADVANCE".equalsIgnoreCase(requestDto.getRefType()) &&
                    requestDto.getAdvancePettyCashPoid() != null) {

                pettyCashPaymentVoucherCustomRepository.loadAdvanceDetails(
                        UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                        requestDto.getAmount(), String.valueOf(requestDto.getAdvancePettyCashPoid()),
                        result, advanceDetails
                );
                logResult("PROC_GL_PETTY_ADVANCE_DTLLOAD", result);
            }


            if (requestDto.getFfRef() != null && !requestDto.getFfRef().trim().isEmpty()) {
                pettyCashPaymentVoucherCustomRepository.updateCostFF(
                        UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                        requestDto.getFfRef(), Long.valueOf(requestDto.getFfRef()), result
                );
            }
            if (requestDto.getFdaRef() != null && !requestDto.getFdaRef().trim().isEmpty()) {
                pettyCashPaymentVoucherCustomRepository.updateCostFDA(
                        UserContext.getGroupPoid(), UserContext.getUserPoid(), UserContext.getCompanyPoid(),
                        requestDto.getFdaRef(), Long.valueOf(requestDto.getFdaRef()), result
                );
            }


            GlPettyCashPaymentHdr savedHeader = glPettyCashPaymentHdrRepository.save(header);
            Long hdrPoid = savedHeader.getTransactionPoid();

            // Logging for create operation
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), savedHeader.getTransactionPoid().toString());


            List<GlPettyCashPaymentDtlResponseDto> paymentDtls = new ArrayList<>();
            List<GlPettyCashChargeDtlResponseDto> chargeDtls = new ArrayList<>();
            List<GLPettyCashItemDtlResponseDto> itemDtls = new ArrayList<>();


            String refType = requestDto.getRefType();
            switch (refType.toUpperCase()) {
                case "GENERAL" -> {
                    var savedPaymentDtls = glPettyCashPaymentDtlRepository.saveAll(
                            mapPaymentDtls(requestDto, hdrPoid));
                    paymentDtls = mapPaymentResponse(savedPaymentDtls);
                    
                    // Log child record creation
                    savedPaymentDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Payment Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });

                    // -----------------------------------------
                    // BILLWISE BREAKUP INSERTION FOR EACH GL ROW
                    // -----------------------------------------
                    List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();

                    // Use detRowId from request DTOs directly (frontend will pass all IDs)
                    List<GlPettyCashPaymentDtlRequestDto> requestDtls = requestDto.getGlPettyCashPaymentDtlRequestDtos();
                    for (GlPettyCashPaymentDtlRequestDto dtl : requestDtls) {
                        // Use detRowId from request DTO (frontend passes this)
                        Long mainDetRowId = dtl.getDetRowId();

                        // Check: Billwise rows exist?
                        if (dtl.getBillwiseBreakupList() != null && !dtl.getBillwiseBreakupList().isEmpty()) {

                            for (BillwiseBreakupPopupRequestDto popup : dtl.getBillwiseBreakupList()) {
                                // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    actionTypeStr = "isCreated"; // Default to create if actionType is null/empty
                                }
                                String actionType = actionTypeStr.toUpperCase();
                                // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                                if (!"ISCREATED".equals(actionType)) {
                                    continue;
                                }

                                BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();

                                dto.setGroupPoid(UserContext.getGroupPoid());
                                dto.setCompanyPoid(UserContext.getCompanyPoid());
                                dto.setDocId(documentId);
                                dto.setTransactionPoid(hdrPoid);
                                dto.setGlPoid(dtl.getGlPoid());

                                // GL → Billwise mapping - use detRowId from request (frontend passes this)
                                dto.setMainDetRowId(mainDetRowId);

                                // Use billDetRowId from frontend directly (no sequential generation)
                                dto.setBillDetRowId(popup.getBillDetRowId());

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

                    // Insert final list
                    if (!billwiseList.isEmpty()) {
                        billwiseBreakupService.insertBillwiseBreakup(billwiseList);
                    }

                    // -----------------------------------------
                    // COST CENTER BREAKUP INSERTION FOR EACH GL ROW
                    // -----------------------------------------
                    List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();

                    // Use detRowId from request DTOs directly (frontend will pass all IDs)
                    for (GlPettyCashPaymentDtlRequestDto dtl : requestDtls) {
                        // Use detRowId from request DTO (frontend passes this)
                        Long mainDetRowId = dtl.getDetRowId();

                        if (dtl.getCostCenterBreakupList() != null &&
                                !dtl.getCostCenterBreakupList().isEmpty()) {

                            for (CostCenterBreakupPopupRequestDto popup : dtl.getCostCenterBreakupList()) {
                                // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    actionTypeStr = "isCreated"; // Default to create if actionType is null/empty
                                }
                                String actionType = actionTypeStr.toUpperCase();
                                // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                                if (!"ISCREATED".equals(actionType)) {
                                    continue;
                                }

                                CostCenterBreakupRequestDto dto = new CostCenterBreakupRequestDto();

                                dto.setGroupPoid(UserContext.getGroupPoid());
                                dto.setCompanyPoid(UserContext.getCompanyPoid());
                                dto.setDocId(documentId);
                                dto.setTransactionPoid(hdrPoid);
                                dto.setGlPoid(dtl.getGlPoid());
                                // Mapping GL → Cost center row - use detRowId from request (frontend passes this)
                                dto.setMainDetRowId(mainDetRowId);

                                // Use costDetRowId from frontend directly (no sequential generation)
                                dto.setCostDetRowId(popup.getCostDetRowId());

                                dto.setCostGroup(popup.getCostGroup());
                                dto.setCostPoid(popup.getCostPoid());
                                dto.setAmount(popup.getAmount());
                                dto.setLoginUserPoid(UserContext.getUserPoid());

                                costCenterList.add(dto);
                            }
                        }
                    }

                    // Insert final cost center breakup list
                    if (!costCenterList.isEmpty()) {
                        costCenterBreakupService.saveCostCenterBreakups(costCenterList);
                    }


                }
                case "FF JOBS", "FDA JOBS" -> {
                    var savedChargeDtls = glPettyCashChargeDtlRepository.saveAll(mapChargeDtls(requestDto, hdrPoid));
                    chargeDtls = mapChargeResponse(savedChargeDtls);
                    
                    // Log child record creation
                    savedChargeDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Charge Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                case "MTA RFQ" -> {
                    var savedItemDtls = glPettyCashItemDtlRepository.saveAll(mapItemDtls(requestDto, hdrPoid));
                    itemDtls = mapItemResponse(savedItemDtls);
                    
                    // Log child record creation
                    savedItemDtls.forEach(dtl -> {
                        String logDetail = String.format("Row Created on Item Detail with detRowId: %s", dtl.getDetRowId());
                        loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
                    });
                }
                default -> throw new IllegalArgumentException("Invalid RefType: " + refType);
            }

            // Load billwise and cost center breakup data for response
            if (refType.equalsIgnoreCase("GENERAL") && !paymentDtls.isEmpty()) {
                Long transPoid = savedHeader.getTransactionPoid();
                Long groupPoid = savedHeader.getGroupPoid();
                Long companyPoid = savedHeader.getCompanyPoid();
                Long userPoid = UserContext.getUserPoid();

                GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                        billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transPoid);

                GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                        costCenterBreakupService.loadCostCenterData(documentId, transPoid, groupPoid, companyPoid, userPoid);

                // Populate billwise and cost center breakup in payment details
                for (GlPettyCashPaymentDtlResponseDto dtl : paymentDtls) {
                    Long detRowId = dtl.getDetRowId();
                    if (null != billwiseResponse && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                        dtl.setBillwiseBreakupList(mapToPopupDto(billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                    if (null != costCenterResponse && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                        dtl.setCostCenterBreakupList(mapToCostCenterPopupDto(costCenterResponse.getCostBreakupList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                }
            }

            return mapToResponseDto(savedHeader, paymentDtls, chargeDtls, itemDtls);

        } catch (Exception e) {
            throw new RuntimeException("Error during petty cash creation: " + e.getMessage(), e);
        }
    }

    private GlPettyCashPaymentHdr buildPettyCashHeader(PettyCashRequestBase requestDto) {

        return GlPettyCashPaymentHdr.builder()
                .transactionDate(requestDto.getTransactionDate())
                .groupPoid(UserContext.getGroupPoid())
                .companyPoid(UserContext.getCompanyPoid())
                .currencyCode(requestDto.getCurrencyCode())
                .currencyRate(requestDto.getCurrencyRate())
                .pettyCashGlPoid(requestDto.getPettyCashGlPoid())
                .balance(requestDto.getBalance())
                .amount(requestDto.getAmount())
                .payingTo(requestDto.getPayingTo())
                .narration(requestDto.getNarration())
                .advance(requestDto.getAdvance())
                .refType(requestDto.getRefType())
                .fdaRef(requestDto.getFdaRef())
                .ffRef(requestDto.getFfRef())
                .settledDate(requestDto.getSettledDate())
                .remarks(requestDto.getRemarks())
                .settledTotal(requestDto.getSettledTotal())
                .status(requestDto.getStatus())
                .grandTotal(requestDto.getGrandTotal())
                .mtaRef(requestDto.getMtaRef())
                .multiCompany(requestDto.getMultiCompany())
                .poRef(requestDto.getPoRef())
                .salesQtnRef(requestDto.getSalesQtnRef())
                .crTotal(requestDto.getCrTotal())
                .drTotal(requestDto.getDrTotal())
                .roundingAmount(requestDto.getRoundingAmount())
                .grnSupplierPoid(requestDto.getGrnSupplierPoid())
                .supplierGlPoid(requestDto.getSupplierGlPoid())
                .customerGlPoid(requestDto.getCustomerGlPoid())
                .advancePettyCashPoid(requestDto.getAdvancePettyCashPoid())
                .advanceStatus(requestDto.getAdvanceStatus())
                .advanceAmount(requestDto.getAdvanceAmount())
                .companyDivPoid(requestDto.getCompanyDivPoid())
                .createdBy(getCurrentUser())
                .createdDate(new Date())
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(new Date())
                .build();
    }

    private List<GlPettyCashPaymentDtl> mapPaymentDtls(PettyCashRequestBase requestDto, Long hdrPoid) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GlPettyCashPaymentDtlRequestDto> filteredDtos = Optional.ofNullable(requestDto.getGlPettyCashPaymentDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(dtl -> {
                    String actionTypeStr = dtl.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .collect(Collectors.toList());
        
        final long[] detRowIdCounter = {1};
        return filteredDtos.stream()
                .map(dtl -> {
                    // Auto-generate detRowId if not provided
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) {
                        detRowId = detRowIdCounter[0]++;
                    }
                    
                    return GlPettyCashPaymentDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(detRowId)  // Use auto-generated or provided detRowId
                            .type(dtl.getType())
                            .companyPoid(dtl.getCompanyPoid())
                            .glMaster(dtl.getGlPoid() != null ? GLMaster.builder()
                                    .glPoid(dtl.getGlPoid())
                                    .build() : null)
                            .chargeMaster(dtl.getChargePoid() != null ? ShipChargeEntity.builder()
                                    .chargePoid(dtl.getChargePoid())
                                    .build() : null)
                            .drAmt(dtl.getDrAmt())
                            .crAmt(dtl.getCrAmt())
                            .vatAmount(dtl.getVatAmount())
                            .totalAmount(dtl.getTotalAmount())
                            .vatSupplier(dtl.getVatSupplier())
                            .inputVatNumber(dtl.getInputVatNumber())
                            .supplierInvDate(dtl.getSupplierInvDate())
                            .taxPoid(dtl.getTaxPoid())
                            .taxPercentage(dtl.getTaxPercentage())
                            .vatPartyName(dtl.getVatPartyName())
                            .remarks(dtl.getRemarks())
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .lastModifiedBy(getCurrentUser())
                            .lastModifiedDate(LocalDateTime.now())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<GLPettyCashItemDtl> mapItemDtls(PettyCashRequestBase requestDto, Long hdrPoid) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GlPettyCashItemDtlRequestDto> filteredDtos = Optional.ofNullable(requestDto.getGlPettyCashItemDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(dtl -> {
                    String actionTypeStr = dtl.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .collect(Collectors.toList());
        
        final long[] detRowIdCounter = {1};
        return filteredDtos.stream()
                .map(dtl -> {
                    // Auto-generate detRowId if not provided
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) {
                        detRowId = detRowIdCounter[0]++;
                    }
                    
                    GLPettyCashItemDtl.GLPettyCashItemDtlBuilder builder = GLPettyCashItemDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(detRowId)
                            .poQty(dtl.getPoQty())
                            .dnQty(dtl.getDnQty())
                            .qtyReceived(dtl.getQtyReceived())
                            .price(dtl.getPrice())
                            .discount(dtl.getDiscount())
                            .total(dtl.getTotal())
                            .remarks(dtl.getRemarks())
                            .refDocId(dtl.getRefDocId())
                            .refDocPoid(dtl.getRefDocPoid())
                            .checkAll(dtl.getCheckAll())
                            .refDetRowId(dtl.getRefDetRowId())
                            .vatPartyName(dtl.getVatPartyName())
                            .partyInvNumber(dtl.getPartyInvNumber())
                            .partyInvDate(dtl.getPartyInvDate())
                            .taxPoid(dtl.getTaxPoid())
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .lastModifiedBy(getCurrentUser())
                            .lastModifiedDate(LocalDateTime.now());

                    GLPettyCashItemDtl entity = builder.build();

                    // Set stockPoid and stockUnitPoid directly on the entity
                    entity.setStockPoid(dtl.getStockPoid());
                    entity.setStockUnitPoid(dtl.getStockUnitPoid());

                    return entity;
                })
                .collect(Collectors.toList());
    }

    private List<GlPettyCashChargeDtl> mapChargeDtls(PettyCashRequestBase requestDto, Long hdrPoid) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GlPettyCashChargeDtlRequestDto> filteredDtos = Optional.ofNullable(requestDto.getGlPettyCashChargeDtlRequestDtos())
                .orElse(Collections.emptyList())
                .stream()
                .filter(dtl -> {
                    String actionTypeStr = dtl.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .collect(Collectors.toList());
        
        final long[] detRowIdCounter = {1};
        return filteredDtos.stream()
                .map(dtl -> {
                    // Auto-generate detRowId if not provided
                    Long detRowId = dtl.getDetRowId();
                    if (detRowId == null) {
                        detRowId = detRowIdCounter[0]++;
                    }
                    
                    return GlPettyCashChargeDtl.builder()
                        .transactionPoid(hdrPoid)
                        .detRowId(detRowId)
                        .shipChargeMaster(dtl.getChargePoid() != null ?
                                ShipChargeEntity.builder()
                                        .chargePoid(dtl.getChargePoid())
                                        .build() : null)
                        .chargeAmount(dtl.getChargeAmount())
                        .description(dtl.getDescription())
                        .remarks(dtl.getRemarks())
                        .refDocId(dtl.getRefDocId())
                        .refDocPoid(dtl.getRefDocPoid())
                        .fdaDetRowId(dtl.getFdaDetRowId())
                        .checkAll(dtl.getCheckAll())
                        .pdaAmount(dtl.getPdaAmount())
                        .ffAmount(dtl.getFfAmount())
                        .chargeFrom(dtl.getChargeFrom())
                        .vatPartyName(dtl.getVatPartyName())
                        .partyInvNumber(dtl.getPartyInvNumber())
                        .partyInvDate(dtl.getPartyInvDate())
                        .taxPoid(dtl.getTaxPoid())

                        .createdBy(getCurrentUser())
                        .createdDate(LocalDateTime.now())
                        .lastModifiedBy(getCurrentUser())
                        .lastModifiedDate(LocalDateTime.now())
                        .build();
                })
                .collect(Collectors.toList());
    }

    private PettyCashResponseDto mapToResponseDto(
            GlPettyCashPaymentHdr savedHeader,
            List<GlPettyCashPaymentDtlResponseDto> paymentDtls,
            List<GlPettyCashChargeDtlResponseDto> chargeDtls,
            List<GLPettyCashItemDtlResponseDto> itemDtls) {

        PettyCashResponseDto.PettyCashResponseDtoBuilder builder = PettyCashResponseDto.builder()
                .transactionPoid(savedHeader.getTransactionPoid())
                .docRef(savedHeader.getDocRef())
                .transactionDate(savedHeader.getTransactionDate())
                .groupPoid(savedHeader.getGroupPoid())
                .companyPoid(savedHeader.getCompanyPoid())
                .currencyCode(savedHeader.getCurrencyCode())
                .currencyRate(savedHeader.getCurrencyRate())
                .pettyCashGlPoid(savedHeader.getPettyCashGlPoid())
                .balance(savedHeader.getBalance())
                .amount(savedHeader.getAmount())
                .payingTo(savedHeader.getPayingTo())
                .narration(savedHeader.getNarration())
                .advance(savedHeader.getAdvance())
                .refType(savedHeader.getRefType())
                .fdaRef(savedHeader.getFdaRef())
                .ffRef(savedHeader.getFfRef())
                .settledDate(savedHeader.getSettledDate())
                .remarks(savedHeader.getRemarks())
                .settledTotal(savedHeader.getSettledTotal())
                .createdBy(savedHeader.getCreatedBy())
                .createdDate(savedHeader.getCreatedDate())
                .lastModifiedBy(savedHeader.getLastModifiedBy())
                .lastModifiedDate(savedHeader.getLastModifiedDate())
                .deleted(savedHeader.getDeleted())
                .status(savedHeader.getStatus())
                .grandTotal(savedHeader.getGrandTotal())
                .mtaRef(savedHeader.getMtaRef())
                .multiCompany(savedHeader.getMultiCompany())
                .poRef(savedHeader.getPoRef())
                .salesQtnRef(savedHeader.getSalesQtnRef())
                .crTotal(savedHeader.getCrTotal())
                .drTotal(savedHeader.getDrTotal())
                .roundingAmount(savedHeader.getRoundingAmount())
                .grnSupplierPoid(savedHeader.getGrnSupplierPoid())
                .supplierGlPoid(savedHeader.getSupplierGlPoid())
                .customerGlPoid(savedHeader.getCustomerGlPoid())
                .advancePettyCashPoid(savedHeader.getAdvancePettyCashPoid())
                .advanceStatus(savedHeader.getAdvanceStatus())
                .advanceAmount(savedHeader.getAdvanceAmount())
                .companyDivPoid(savedHeader.getCompanyDivPoid())
                .paymentDtls(paymentDtls)
                .chargeDtls(chargeDtls)
                .itemDtls(itemDtls);

        if (savedHeader.getPettyCashGlPoid() != null) {
            Optional<GLMaster> pettyCashGlPoidOp =
                    glMasterRepository.findByGlPoid(savedHeader.getPettyCashGlPoid());
            if (pettyCashGlPoidOp.isPresent()) {
                GLMaster glMaster = pettyCashGlPoidOp.get();

                DetailsDto detailsDto = new DetailsDto(
                        glMaster.getGlPoid(),
                        glMaster.getGlCode(),
                        glMaster.getGlDescription(),
                        glMaster.getGroupPoid(),
                        glMaster.getGlDescription2(),
                        glMaster.getSeqno()
                );

                builder.pettyCashGlPoidDtl(detailsDto);
            }
        }

        return builder.build();
    }

    private List<GlPettyCashPaymentDtlResponseDto> mapPaymentResponse(List<GlPettyCashPaymentDtl> savedDtls) {
        return savedDtls.stream()
                .map(dtl -> {
                    GlPettyCashPaymentDtlResponseDto.GlPettyCashPaymentDtlResponseDtoBuilder builder =
                            GlPettyCashPaymentDtlResponseDto.builder()
                                    .transactionPoid(dtl.getTransactionPoid())
                                    .detRowId(dtl.getDetRowId())
                                    .type(dtl.getType())
                                    .companyPoid(dtl.getCompanyPoid())
                                    .glPoid(dtl.getGlMaster() != null ? dtl.getGlMaster().getGlPoid() : null)
                                    .chargePoid(dtl.getChargeMaster() != null ? dtl.getChargeMaster().getChargePoid() : null)
                                    .drAmt(dtl.getDrAmt())
                                    .crAmt(dtl.getCrAmt())
                                    .remarks(dtl.getRemarks())
                                    .createdBy(dtl.getCreatedBy())
                                    .createdDate(dtl.getCreatedDate())
                                    .lastModifiedBy(dtl.getLastModifiedBy())
                                    .lastModifiedDate(dtl.getLastModifiedDate())
                                    .vatAmount(dtl.getVatAmount())
                                    .totalAmount(dtl.getTotalAmount())
                                    .vatSupplier(dtl.getVatSupplier())
                                    .inputVatNumber(dtl.getInputVatNumber())
                                    .supplierInvDate(dtl.getSupplierInvDate())
                                    .taxPoid(dtl.getTaxPoid())
                                    .taxPercentage(dtl.getTaxPercentage())
                                    .vatPartyName(dtl.getVatPartyName());

                    if (dtl.getGlMaster() != null && dtl.getGlMaster().getGlPoid() != null) {
                        glMasterRepository.findByGlPoid(dtl.getGlMaster().getGlPoid())
                                .ifPresent(glMaster -> {
                                    DetailsDto glDetails = new DetailsDto(
                                            glMaster.getGlPoid(),
                                            glMaster.getGlCode(),
                                            glMaster.getGlDescription(),
                                            glMaster.getGroupPoid(),
                                            glMaster.getGlDescription2(),
                                            glMaster.getSeqno()
                                    );
                                    builder.glPoidDtl(glDetails);
                                });
                    }

                    if (dtl.getChargeMaster() != null && dtl.getChargeMaster().getChargePoid() != null) {
                        shipChargeRepository.findByChargePoid(dtl.getChargeMaster().getChargePoid())
                                .ifPresent(charge -> {
                                    DetailsDto chargeDetails = new DetailsDto(
                                            charge.getChargePoid(),
                                            charge.getChargeCode(),
                                            charge.getChargeName(),
                                            charge.getGroupPoid(),
                                            charge.getChargeName2(),
                                            charge.getSeqNo()
                                    );
                                    builder.chargePoidDtl(chargeDetails);
                                });
                    }

                    GlPettyCashPaymentDtlResponseDto responseDto = builder.build();

                    // Fetch and set taxPoidDtl if taxPoid exists
                    if (dtl.getTaxPoid() != null) {
                        taxMasterRepository.findByTaxPoid(dtl.getTaxPoid())
                                .ifPresent(tax -> {
                                    DetailsDto taxDetails = new DetailsDto(
                                            tax.getTaxPoid(),
                                            tax.getTaxCode(),
                                            tax.getTaxName(),
                                            tax.getGroupPoid(),
                                            tax.getTaxName2(),
                                            tax.getSeqNo()
                                    );
                                    responseDto.setTaxPoidDtl(taxDetails);
                                });
                    }

                    return responseDto;
                })
                .toList();
    }

    private List<GlPettyCashChargeDtlResponseDto> mapChargeResponse(List<GlPettyCashChargeDtl> savedDtls) {
        return savedDtls.stream()
                .map(dtl -> {
                    GlPettyCashChargeDtlResponseDto.GlPettyCashChargeDtlResponseDtoBuilder builder =
                            GlPettyCashChargeDtlResponseDto.builder()
                                    .transactionPoid(dtl.getTransactionPoid())
                                    .detRowId(dtl.getDetRowId())
                                    .chargePoid(dtl.getShipChargeMaster() != null ? dtl.getShipChargeMaster().getChargePoid() : null)
                                    .chargeAmount(dtl.getChargeAmount())
                                    .description(dtl.getDescription())
                                    .remarks(dtl.getRemarks())
                                    .createdBy(dtl.getCreatedBy())
                                    .createdDate(dtl.getCreatedDate())
                                    .lastModifiedBy(dtl.getLastModifiedBy())
                                    .lastModifiedDate(dtl.getLastModifiedDate())
                                    .refDocId(dtl.getRefDocId())
                                    .refDocPoid(dtl.getRefDocPoid())
                                    .fdaDetRowId(dtl.getFdaDetRowId())
                                    .checkAll(dtl.getCheckAll())
                                    .pdaAmount(dtl.getPdaAmount())
                                    .ffAmount(dtl.getFfAmount())
                                    .chargeFrom(dtl.getChargeFrom())
                                    .vatPartyName(dtl.getVatPartyName())
                                    .partyInvNumber(dtl.getPartyInvNumber())
                                    .partyInvDate(dtl.getPartyInvDate())
                                    .taxPoid(dtl.getTaxPoid());


                    if (dtl.getShipChargeMaster() != null && dtl.getShipChargeMaster().getChargePoid() != null) {
                        shipChargeRepository.findByChargePoid(dtl.getShipChargeMaster().getChargePoid())
                                .ifPresent(charge -> {
                                    DetailsDto chargeDetails = new DetailsDto(
                                            charge.getChargePoid(),
                                            charge.getChargeCode(),
                                            charge.getChargeName(),
                                            charge.getGroupPoid(),
                                            charge.getChargeName2(),
                                            charge.getSeqNo()
                                    );
                                    builder.chargePoidDtl(chargeDetails);
                                });
                    }

                    GlPettyCashChargeDtlResponseDto responseDto = builder.build();

                    // Fetch and set taxPoidDtl if taxPoid exists
                    if (dtl.getTaxPoid() != null) {
                        taxMasterRepository.findByTaxPoid(dtl.getTaxPoid())
                                .ifPresent(tax -> {
                                    DetailsDto taxDetails = new DetailsDto(
                                            tax.getTaxPoid(),
                                            tax.getTaxCode(),
                                            tax.getTaxName(),
                                            tax.getGroupPoid(),
                                            tax.getTaxName2(),
                                            tax.getSeqNo()
                                    );
                                    responseDto.setTaxPoidDtl(taxDetails);
                                });
                    }

                    return responseDto;
                })
                .toList();
    }

    private List<GLPettyCashItemDtlResponseDto> mapItemResponse(List<GLPettyCashItemDtl> savedDtls) {
        return savedDtls.stream()
                .map(dtl -> {
                    GLPettyCashItemDtlResponseDto.GLPettyCashItemDtlResponseDtoBuilder builder =
                            GLPettyCashItemDtlResponseDto.builder()
                                    .transactionPoid(dtl.getTransactionPoid())
                                    .detRowId(dtl.getDetRowId())
                                    .stockPoid(dtl.getStockPoid())
                                    .stockUnitPoid(dtl.getStockUnitPoid())
                                    .poQty(dtl.getPoQty())
                                    .dnQty(dtl.getDnQty())
                                    .qtyReceived(dtl.getQtyReceived())
                                    .price(dtl.getPrice())
                                    .discount(dtl.getDiscount())
                                    .total(dtl.getTotal())
                                    .remarks(dtl.getRemarks())
                                    .createdBy(dtl.getCreatedBy())
                                    .createdDate(dtl.getCreatedDate())
                                    .lastModifiedBy(dtl.getLastModifiedBy())
                                    .lastModifiedDate(dtl.getLastModifiedDate())
                                    .refDocId(dtl.getRefDocId())
                                    .refDocPoid(dtl.getRefDocPoid())
                                    .checkAll(dtl.getCheckAll())
                                    .refDetRowId(dtl.getRefDetRowId())
                                    .vatPartyName(dtl.getVatPartyName())
                                    .partyInvNumber(dtl.getPartyInvNumber())
                                    .partyInvDate(dtl.getPartyInvDate())
                                    .taxPoid(dtl.getTaxPoid());

                    if (dtl.getStockPoid() != null) {
                        stockMasterRepository.findByStockPoid(dtl.getStockPoid())
                                .ifPresent(stock -> {
                                    DetailsDto stockDetails = new DetailsDto(
                                            stock.getStockPoid(),
                                            stock.getStockCode(),
                                            stock.getStockName(),
                                            stock.getGroupPoid(),
                                            stock.getStockDescription(),
                                            stock.getSeqNo()
                                    );
                                    builder.stockPoidDtl(stockDetails);
                                });
                    }

                    if (dtl.getStockUnitPoid() != null) {
                        unitMasterRepository.findByUnitPoid(dtl.getStockUnitPoid())
                                .ifPresent(unit -> {
                                    DetailsDto unitDetails = new DetailsDto(
                                            unit.getUnitPoid(),
                                            unit.getUnitCode(),
                                            unit.getUnitName(),
                                            unit.getGroupPoid(),
                                            unit.getUnitName2(),
                                            unit.getSeqNo()
                                    );
                                    builder.stockUnitPoidDtl(unitDetails);
                                });
                    }

                    GLPettyCashItemDtlResponseDto responseDto = builder.build();

                    // Fetch and set taxPoidDtl if taxPoid exists
                    if (dtl.getTaxPoid() != null) {
                        taxMasterRepository.findByTaxPoid(dtl.getTaxPoid())
                                .ifPresent(tax -> {
                                    DetailsDto taxDetails = new DetailsDto(
                                            tax.getTaxPoid(),
                                            tax.getTaxCode(),
                                            tax.getTaxName(),
                                            tax.getGroupPoid(),
                                            tax.getTaxName2(),
                                            tax.getSeqNo()
                                    );
                                    responseDto.setTaxPoidDtl(taxDetails);
                                });
                    }

                    return responseDto;
                })
                .toList();
    }


    @Transactional
    @Override
    public PettyCashResponseDto updatePettyCash(Long transactionPoid,
                                                PettyCashUpdateRequestDto requestDto, String documentId) {
        try {
            // Step 1: Fetch existing petty cash header
            GlPettyCashPaymentHdr existingHdr = glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new RuntimeException("Petty cash not found with ID: " + transactionPoid));

            // Create copy of old entity for logging
            GlPettyCashPaymentHdr oldEntity = new GlPettyCashPaymentHdr();
            BeanUtils.copyProperties(existingHdr, oldEntity);

            StringBuilder oldRefType = new StringBuilder();
            StringBuilder oldRefPoid = new StringBuilder();
            Long userGroupPoid = UserContext.getGroupPoid();
            Long userCompanyPoid = UserContext.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            //  Step 2: Get old reference type and POID
            pettyCashPaymentVoucherCustomRepository.getOldJobReferences(
                    userGroupPoid,
                    userPoid,
                    userCompanyPoid,
                    requestDto.getDocId(),
                    String.valueOf(transactionPoid),
                    oldRefType,
                    oldRefPoid
            );

            //  Step 3: Validate job before saving (FF/FDA/MTQ)
            StringBuilder validationResult = new StringBuilder();
            pettyCashPaymentVoucherCustomRepository.validateJobBeforeSave(
                    userGroupPoid,
                    userPoid,
                    userCompanyPoid,
                    requestDto.getDocId(),
                    requestDto.getRefType(),
                    requestDto.getFfRef(),
                    validationResult
            );

            if (validationResult.toString().startsWith("ERROR") ||
                    validationResult.toString().startsWith("WARNING")) {
                throw new RuntimeException("Job validation failed: " + validationResult);
            }

            // Step 4: Update parent (header) fields
            updateHeaderFields(existingHdr, requestDto, userPoid);
            GlPettyCashPaymentHdr updatedHdr = glPettyCashPaymentHdrRepository.save(existingHdr);

            // Logging for update operation
            loggingService.logChanges(oldEntity, updatedHdr, GlPettyCashPaymentHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

            //  Step 5: Merge & save child details partially
            String refType = updatedHdr.getRefType().toUpperCase();

            List<GlPettyCashPaymentDtlResponseDto> paymentDtls = new ArrayList<>();
            List<GlPettyCashChargeDtlResponseDto> chargeDtls = new ArrayList<>();
            List<GLPettyCashItemDtlResponseDto> itemDtls = new ArrayList<>();

            switch (refType) {
                case "GENERAL" -> {
                    var existingDtls = glPettyCashPaymentDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergePaymentDtls(existingDtls, requestDto, transactionPoid);
                    glPettyCashPaymentDtlRepository.saveAll(merged);
                    paymentDtls = mapPaymentResponse(merged);

                    // -----------------------------------------
                    // UPDATE BILLWISE BREAKUP ENTRIES
                    // -----------------------------------------
                    List<BillwiseBreakupRequestDto> billwiseList = new ArrayList<>();

                    for (GlPettyCashPaymentDtlRequestDto dtl : requestDto.getGlPettyCashPaymentDtlRequestDtos()) {

                        if (dtl.getBillwiseBreakupList() != null &&
                                !dtl.getBillwiseBreakupList().isEmpty()) {

                            for (BillwiseBreakupPopupRequestDto popup : dtl.getBillwiseBreakupList()) {
                                // Determine actionType based on billDetRowId
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    // If billDetRowId is null or 0, default to "isCreated", otherwise "isUpdated"
                                    if (popup.getBillDetRowId() == null || popup.getBillDetRowId() == 0) {
                                        actionTypeStr = "isCreated";
                                    } else {
                                        actionTypeStr = "isUpdated";
                                    }
                                }
                                String actionType = actionTypeStr.toUpperCase();

                                // Skip "ISDELETED" and "NOCHANGES" - they won't be included in the update
                                if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) {
                                    continue;
                                }

                                BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();

                                dto.setGroupPoid(UserContext.getGroupPoid());
                                dto.setCompanyPoid(UserContext.getCompanyPoid());
                                dto.setDocId(documentId);
                                dto.setTransactionPoid(transactionPoid); // SAME HDR POID
                                dto.setLoginUserPoid(userPoid);
                                dto.setGlPoid(dtl.getGlPoid());

                                // GL → Billwise mapping
                                dto.setMainDetRowId(dtl.getDetRowId());
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
                        billwiseBreakupService.updateBillwiseBreakups(billwiseList, userPoid);
                    }

                    // -----------------------------------------
                    // UPDATE COST CENTER BREAKUP ENTRIES
                    // -----------------------------------------
                    List<CostCenterBreakupRequestDto> costCenterList = new ArrayList<>();


                    for (GlPettyCashPaymentDtlRequestDto dtl : requestDto.getGlPettyCashPaymentDtlRequestDtos()) {
                        if (dtl.getCostCenterBreakupList() != null && !dtl.getCostCenterBreakupList().isEmpty()) {
                            for (CostCenterBreakupPopupRequestDto popup : dtl.getCostCenterBreakupList()) {
                                // Determine actionType based on costDetRowId
                                String actionTypeStr = popup.getActionType();
                                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                                    // If costDetRowId is null or 0, default to "isCreated", otherwise "isUpdated"
                                    if (popup.getCostDetRowId() == null || popup.getCostDetRowId() == 0) {
                                        actionTypeStr = "isCreated";
                                    } else {
                                        actionTypeStr = "isUpdated";
                                    }
                                }
                                String actionType = actionTypeStr.toUpperCase();

                                // Skip "ISDELETED" and "NOCHANGES" - they won't be included in the update
                                if ("ISDELETED".equals(actionType) || "NOCHANGES".equals(actionType)) {
                                    continue;
                                }

                                CostCenterBreakupRequestDto cc = CostCenterBreakupRequestDto.builder()
                                        .groupPoid(UserContext.getGroupPoid())
                                        .companyPoid(UserContext.getCompanyPoid())
                                        .docId(documentId)
                                        .transactionPoid(transactionPoid)
                                        .mainDetRowId(dtl.getDetRowId())
                                        .glPoid(dtl.getGlPoid())
                                        .costDetRowId(popup.getCostDetRowId())
                                        .costGroup(popup.getCostGroup())
                                        .costPoid(popup.getCostPoid())
                                        .amount(popup.getAmount())
                                        .loginUserPoid(userPoid)
                                        .build();

                                costCenterList.add(cc);
                            }
                        }
                    }

                    if (!costCenterList.isEmpty()) {
                        // 2️⃣ Insert new breakup entries
                        costCenterBreakupService.updateCostCenterBreakups(costCenterList, userPoid);
                    }

                }
                case "FF JOBS", "FDA JOBS" -> {
                    var existingDtls = glPettyCashChargeDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergeChargeDtls(existingDtls, requestDto, transactionPoid);
                    glPettyCashChargeDtlRepository.saveAll(merged);
                    chargeDtls = mapChargeResponse(merged);

                    // 6.3 → Update FF cost if FF POID present
                    if (requestDto.getFfRef() != null && !requestDto.getFfRef().trim().isEmpty()) {
                        StringBuilder ffResult = new StringBuilder();
                        pettyCashPaymentVoucherCustomRepository.updateCostFF(
                                userGroupPoid,
                                userCompanyPoid,
                                userPoid,
                                requestDto.getFfRef(),
                                Long.valueOf(requestDto.getFfRef()),
                                ffResult
                        );
                        logResult("PROC_AP_PI_FF_UPDATE_COST", ffResult);
                    }

                    // 6.4 → Update FDA cost if FDA POID present
                    if (requestDto.getFdaRef() != null && !requestDto.getFdaRef().trim().isEmpty()) {
                        StringBuilder fdaResult = new StringBuilder();
                        pettyCashPaymentVoucherCustomRepository.updateCostFDA(
                                userGroupPoid,
                                userCompanyPoid,
                                userPoid,
                                requestDto.getFdaRef(),
                                Long.valueOf(requestDto.getFdaRef()),
                                fdaResult
                        );
                        logResult("PROC_A_PI_FF_UPDATE_COST", fdaResult);
                    }
                }
                case "MTA RFQ" -> {
                    var existingDtls = glPettyCashItemDtlRepository.findByTransactionPoid(transactionPoid);
                    var merged = mergeItemDtls(existingDtls, requestDto, transactionPoid);
                    glPettyCashItemDtlRepository.saveAll(merged);
                    itemDtls = mapItemResponse(merged);
                }
                default -> throw new IllegalArgumentException("Invalid RefType: " + refType);
            }

            //  Step 6: Post-save procedure calls
            StringBuilder result = new StringBuilder();

            // 6.2 → Update RFQ Purchase Price if MTQ RFQ-related
            if (refType.contains("RFQ") || refType.equalsIgnoreCase("MTA RFQ")) {
                StringBuilder rfqResult = new StringBuilder();
                pettyCashPaymentVoucherCustomRepository.updateRfqPurchasePrice(
                        userGroupPoid,
                        userCompanyPoid,
                        userPoid,
                        requestDto.getSalesQtnRef(),
                        rfqResult
                );
                logResult("PROC_RFQ_UPDATE_PURCHASE_PRICE", rfqResult);
            }


            //  Step 7: Load billwise and cost center breakup data for response
            if (refType.equalsIgnoreCase("GENERAL") && !paymentDtls.isEmpty()) {
                Long transPoid = updatedHdr.getTransactionPoid();
                Long groupPoid = updatedHdr.getGroupPoid();
                Long companyPoid = updatedHdr.getCompanyPoid();
                // userPoid is already declared above

                GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                        billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transPoid);

                GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                        costCenterBreakupService.loadCostCenterData(documentId, transPoid, groupPoid, companyPoid, userPoid);

                // Populate billwise and cost center breakup in payment details
                for (GlPettyCashPaymentDtlResponseDto dtl : paymentDtls) {
                    Long detRowId = dtl.getDetRowId();
                    if (null != billwiseResponse && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                        dtl.setBillwiseBreakupList(mapToPopupDto(billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                    if (null != costCenterResponse && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                        dtl.setCostCenterBreakupList(mapToCostCenterPopupDto(costCenterResponse.getCostBreakupList().stream()
                                .filter(x -> x.getMainDetRowId().equals(detRowId))
                                .collect(Collectors.toList())));
                    }
                }
            }

            //  Step 8: Return the final response DTO
            
            return mapToResponseDto(updatedHdr, paymentDtls, chargeDtls, itemDtls);

        } catch (Exception e) {
            throw new RuntimeException("Error during petty cash update: " + e.getMessage(), e);
        }
    }


    private void updateHeaderFields(GlPettyCashPaymentHdr header, PettyCashRequestBase dto, Long loginUserPoid) {
        if (dto.getTransactionDate() != null) header.setTransactionDate(dto.getTransactionDate());
        if (dto.getCurrencyCode() != null) header.setCurrencyCode(dto.getCurrencyCode());
        if (dto.getCurrencyRate() != null) header.setCurrencyRate(dto.getCurrencyRate());
        if (dto.getPettyCashGlPoid() != null) header.setPettyCashGlPoid(dto.getPettyCashGlPoid());
        if (dto.getBalance() != null) header.setBalance(dto.getBalance());
        if (dto.getAmount() != null) header.setAmount(dto.getAmount());
        if (dto.getPayingTo() != null) header.setPayingTo(dto.getPayingTo());
        if (dto.getNarration() != null) header.setNarration(dto.getNarration());
        if (dto.getAdvance() != null) header.setAdvance(dto.getAdvance());
        if (dto.getRefType() != null) header.setRefType(dto.getRefType());
        if (dto.getFdaRef() != null) header.setFdaRef(dto.getFdaRef());
        if (dto.getFfRef() != null) header.setFfRef(dto.getFfRef());
        if (dto.getSettledDate() != null) header.setSettledDate(dto.getSettledDate());
        if (dto.getRemarks() != null) header.setRemarks(dto.getRemarks());
        if (dto.getSettledTotal() != null) header.setSettledTotal(dto.getSettledTotal());
        if (dto.getStatus() != null) header.setStatus(dto.getStatus());
        if (dto.getGrandTotal() != null) header.setGrandTotal(dto.getGrandTotal());
        if (dto.getMtaRef() != null) header.setMtaRef(dto.getMtaRef());
        if (dto.getMultiCompany() != null) header.setMultiCompany(dto.getMultiCompany());
        if (dto.getPoRef() != null) header.setPoRef(dto.getPoRef());
        if (dto.getSalesQtnRef() != null) header.setSalesQtnRef(dto.getSalesQtnRef());
        if (dto.getCrTotal() != null) header.setCrTotal(dto.getCrTotal());
        if (dto.getDrTotal() != null) header.setDrTotal(dto.getDrTotal());
        if (dto.getRoundingAmount() != null) header.setRoundingAmount(dto.getRoundingAmount());
        if (dto.getGrnSupplierPoid() != null) header.setGrnSupplierPoid(dto.getGrnSupplierPoid());
        if (dto.getSupplierGlPoid() != null) header.setSupplierGlPoid(dto.getSupplierGlPoid());
        if (dto.getCustomerGlPoid() != null) header.setCustomerGlPoid(dto.getCustomerGlPoid());
        if (dto.getAdvancePettyCashPoid() != null) header.setAdvancePettyCashPoid(dto.getAdvancePettyCashPoid());
        if (dto.getAdvanceStatus() != null) header.setAdvanceStatus(dto.getAdvanceStatus());
        if (dto.getAdvanceAmount() != null) header.setAdvanceAmount(dto.getAdvanceAmount());
        if (dto.getCompanyDivPoid() != null) header.setCompanyDivPoid(dto.getCompanyDivPoid());

        header.setLastModifiedBy(getCurrentUser());
        header.setLastModifiedDate(new Date());
    }

    private List<GlPettyCashPaymentDtl> mergePaymentDtls(List<GlPettyCashPaymentDtl> existing,
                                                         PettyCashRequestBase requestDto,
                                                         Long hdrPoid) {
        Map<Long, GlPettyCashPaymentDtl> existingMap = existing.stream()
                .collect(Collectors.toMap(GlPettyCashPaymentDtl::getDetRowId, d -> d));

        List<GlPettyCashPaymentDtl> toSave = new ArrayList<>();
        List<GlPettyCashPaymentDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GlPettyCashPaymentDtl>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        for (var dto : Optional.ofNullable(requestDto.getGlPettyCashPaymentDtlRequestDtos())
                .orElse(Collections.emptyList())) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = dto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();

            switch (actionType) {
                case "ISCREATED":
                    // Create new record
                    GlPettyCashPaymentDtl newEntity = new GlPettyCashPaymentDtl();
                    newEntity.setTransactionPoid(hdrPoid);
                    
                    // Auto-generate detRowId if not provided
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        // Find max existing detRowId and increment
                        Long maxDetRowId = existing.stream()
                                .map(GlPettyCashPaymentDtl::getDetRowId)
                                .filter(Objects::nonNull)
                                .max(Long::compareTo)
                                .orElse(0L);
                        detRowId = maxDetRowId + 1;
                    }
                    newEntity.setDetRowId(detRowId);
                    newEntity.setType(dto.getType());
                    newEntity.setCompanyPoid(dto.getCompanyPoid());
                    newEntity.setDrAmt(dto.getDrAmt());
                    newEntity.setCrAmt(dto.getCrAmt());
                    newEntity.setVatAmount(dto.getVatAmount());
                    newEntity.setTotalAmount(dto.getTotalAmount());
                    newEntity.setVatSupplier(dto.getVatSupplier());
                    newEntity.setInputVatNumber(dto.getInputVatNumber());
                    newEntity.setSupplierInvDate(dto.getSupplierInvDate());
                    newEntity.setTaxPoid(dto.getTaxPoid());
                    newEntity.setTaxPercentage(dto.getTaxPercentage());
                    newEntity.setVatPartyName(dto.getVatPartyName());
                    newEntity.setRemarks(dto.getRemarks());
                    newEntity.setCreatedBy(getCurrentUser());
                    newEntity.setCreatedDate(LocalDateTime.now());
                    newEntity.setLastModifiedBy(getCurrentUser());
                    newEntity.setLastModifiedDate(LocalDateTime.now());

                    if (dto.getGlPoid() != null)
                        newEntity.setGlMaster(GLMaster.builder().glPoid(dto.getGlPoid()).build());
                    if (dto.getChargePoid() != null)
                        newEntity.setChargeMaster(ShipChargeEntity.builder().chargePoid(dto.getChargePoid()).build());

                    toSave.add(newEntity);
                    break;

                case "ISUPDATED":
                    // Update existing record
                    GlPettyCashPaymentDtl existingEntity = existingMap.get(dto.getDetRowId());
                    if (existingEntity == null) {
                        throw new RuntimeException("Payment detail not found with detRowId: " + dto.getDetRowId());
                    }
                    
                    // Create copy for logging
                    GlPettyCashPaymentDtl oldEntity = new GlPettyCashPaymentDtl();
                    BeanUtils.copyProperties(existingEntity, oldEntity);
                    
                    existingEntity.setType(dto.getType());
                    existingEntity.setCompanyPoid(dto.getCompanyPoid());
                    existingEntity.setDrAmt(dto.getDrAmt());
                    existingEntity.setCrAmt(dto.getCrAmt());
                    existingEntity.setVatAmount(dto.getVatAmount());
                    existingEntity.setTotalAmount(dto.getTotalAmount());
                    existingEntity.setVatSupplier(dto.getVatSupplier());
                    existingEntity.setInputVatNumber(dto.getInputVatNumber());
                    existingEntity.setSupplierInvDate(dto.getSupplierInvDate());
                    existingEntity.setTaxPoid(dto.getTaxPoid());
                    existingEntity.setTaxPercentage(dto.getTaxPercentage());
                    existingEntity.setVatPartyName(dto.getVatPartyName());
                    existingEntity.setRemarks(dto.getRemarks());
                    existingEntity.setLastModifiedBy(getCurrentUser());
                    existingEntity.setLastModifiedDate(LocalDateTime.now());

                    if (dto.getGlPoid() != null)
                        existingEntity.setGlMaster(GLMaster.builder().glPoid(dto.getGlPoid()).build());
                    if (dto.getChargePoid() != null)
                        existingEntity.setChargeMaster(ShipChargeEntity.builder().chargePoid(dto.getChargePoid()).build());

                    toSave.add(existingEntity);
                    
                    // Add to batch logging
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", hdrPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlPettyCashPaymentDtl.class, documentId, hdrPoid.toString(), logDetail));
                    break;

                case "ISDELETED":
                    // Mark for deletion - find entity and add to delete list
                    GlPettyCashPaymentDtl paymentEntityToDelete = existingMap.get(dto.getDetRowId());
                    if (paymentEntityToDelete != null) {
                        toDelete.add(paymentEntityToDelete);
                        // Log the deletion
                        loggingService.logDelete(paymentEntityToDelete, documentId, hdrPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                    // Keep existing record as-is - add to toSave so it appears in response
                    GlPettyCashPaymentDtl unchangedPaymentEntity = existingMap.get(dto.getDetRowId());
                    if (unchangedPaymentEntity != null) {
                        toSave.add(unchangedPaymentEntity);
                    }
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    GlPettyCashPaymentDtl defaultPaymentEntity = existingMap.get(dto.getDetRowId());
                    if (defaultPaymentEntity != null) {
                        toSave.add(defaultPaymentEntity);
                    }
                    break;
            }
        }

        // Delete records marked for deletion - use deleteAll with entity objects (composite key)
        if (!toDelete.isEmpty()) {
            glPettyCashPaymentDtlRepository.deleteAll(toDelete);
        }
        
        // Save all entities
        List<GlPettyCashPaymentDtl> savedEntities = glPettyCashPaymentDtlRepository.saveAll(toSave);
        
        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        
        // Log creation for new records
        savedEntities.stream()
            .filter(entity -> entity.getCreatedDate() != null && entity.getCreatedDate().isAfter(LocalDateTime.now().minusMinutes(1)))
            .forEach(entity -> {
                String logDetail = String.format("Row Created on Payment Detail with detRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
            });

        return savedEntities;
    }

    private List<GlPettyCashChargeDtl> mergeChargeDtls(List<GlPettyCashChargeDtl> existing,
                                                       PettyCashRequestBase requestDto,
                                                       Long hdrPoid) {
        Map<Long, GlPettyCashChargeDtl> existingMap = existing.stream()
                .collect(Collectors.toMap(GlPettyCashChargeDtl::getDetRowId, d -> d));

        List<GlPettyCashChargeDtl> toSave = new ArrayList<>();
        List<GlPettyCashChargeDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GlPettyCashChargeDtl>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        for (var dto : Optional.ofNullable(requestDto.getGlPettyCashChargeDtlRequestDtos())
                .orElse(Collections.emptyList())) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = dto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();

            switch (actionType) {
                case "ISCREATED":
                    // Create new record
                    GlPettyCashChargeDtl newEntity = new GlPettyCashChargeDtl();
                    newEntity.setTransactionPoid(hdrPoid);
                    
                    // Auto-generate detRowId if not provided
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        // Find max existing detRowId and increment
                        Long maxDetRowId = existing.stream()
                                .map(GlPettyCashChargeDtl::getDetRowId)
                                .filter(Objects::nonNull)
                                .max(Long::compareTo)
                                .orElse(0L);
                        detRowId = maxDetRowId + 1;
                    }
                    newEntity.setDetRowId(detRowId);
                    newEntity.setDescription(dto.getDescription());
                    newEntity.setRemarks(dto.getRemarks());
                    newEntity.setChargeAmount(dto.getChargeAmount());
                    newEntity.setRefDocId(dto.getRefDocId());
                    newEntity.setRefDocPoid(dto.getRefDocPoid());
                    newEntity.setFdaDetRowId(dto.getFdaDetRowId());
                    newEntity.setCheckAll(dto.getCheckAll());
                    newEntity.setPdaAmount(dto.getPdaAmount());
                    newEntity.setFfAmount(dto.getFfAmount());
                    newEntity.setChargeFrom(dto.getChargeFrom());
                    newEntity.setVatPartyName(dto.getVatPartyName());
                    newEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    newEntity.setPartyInvDate(dto.getPartyInvDate());
                    newEntity.setTaxPoid(dto.getTaxPoid());
                    newEntity.setCreatedBy(getCurrentUser());
                    newEntity.setCreatedDate(LocalDateTime.now());
                    newEntity.setLastModifiedBy(getCurrentUser());
                    newEntity.setLastModifiedDate(LocalDateTime.now());

                    if (dto.getChargePoid() != null)
                        newEntity.setShipChargeMaster(ShipChargeEntity.builder().chargePoid(dto.getChargePoid()).build());

                    toSave.add(newEntity);
                    break;

                case "ISUPDATED":
                    // Update existing record
                    GlPettyCashChargeDtl existingEntity = existingMap.get(dto.getDetRowId());
                    if (existingEntity == null) {
                        throw new RuntimeException("Charge detail not found with detRowId: " + dto.getDetRowId());
                    }
                    
                    // Create copy for logging
                    GlPettyCashChargeDtl oldEntity = new GlPettyCashChargeDtl();
                    BeanUtils.copyProperties(existingEntity, oldEntity);
                    
                    existingEntity.setDescription(dto.getDescription());
                    existingEntity.setRemarks(dto.getRemarks());
                    existingEntity.setChargeAmount(dto.getChargeAmount());
                    existingEntity.setRefDocId(dto.getRefDocId());
                    existingEntity.setRefDocPoid(dto.getRefDocPoid());
                    existingEntity.setFdaDetRowId(dto.getFdaDetRowId());
                    existingEntity.setCheckAll(dto.getCheckAll());
                    existingEntity.setPdaAmount(dto.getPdaAmount());
                    existingEntity.setFfAmount(dto.getFfAmount());
                    existingEntity.setChargeFrom(dto.getChargeFrom());
                    existingEntity.setVatPartyName(dto.getVatPartyName());
                    existingEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    existingEntity.setPartyInvDate(dto.getPartyInvDate());
                    existingEntity.setTaxPoid(dto.getTaxPoid());
                    existingEntity.setLastModifiedBy(getCurrentUser());
                    existingEntity.setLastModifiedDate(LocalDateTime.now());

                    if (dto.getChargePoid() != null)
                        existingEntity.setShipChargeMaster(ShipChargeEntity.builder().chargePoid(dto.getChargePoid()).build());

                    toSave.add(existingEntity);
                    
                    // Add to batch logging
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", hdrPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlPettyCashChargeDtl.class, documentId, hdrPoid.toString(), logDetail));
                    break;

                case "ISDELETED":
                    // Mark for deletion - find entity and add to delete list
                    GlPettyCashChargeDtl chargeEntityToDelete = existingMap.get(dto.getDetRowId());
                    if (chargeEntityToDelete != null) {
                        toDelete.add(chargeEntityToDelete);
                        // Log the deletion
                        loggingService.logDelete(chargeEntityToDelete, documentId, hdrPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                    // Keep existing record as-is - add to toSave so it appears in response
                    GlPettyCashChargeDtl unchangedChargeEntity = existingMap.get(dto.getDetRowId());
                    if (unchangedChargeEntity != null) {
                        toSave.add(unchangedChargeEntity);
                    }
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    GlPettyCashChargeDtl defaultChargeEntity = existingMap.get(dto.getDetRowId());
                    if (defaultChargeEntity != null) {
                        toSave.add(defaultChargeEntity);
                    }
                    break;
            }
        }

        // Delete records marked for deletion - use deleteAll with entity objects (composite key)
        if (!toDelete.isEmpty()) {
            glPettyCashChargeDtlRepository.deleteAll(toDelete);
        }
        
        // Save all entities
        List<GlPettyCashChargeDtl> savedEntities = glPettyCashChargeDtlRepository.saveAll(toSave);
        
        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        
        // Log creation for new records
        savedEntities.stream()
            .filter(entity -> entity.getCreatedDate() != null && entity.getCreatedDate().isAfter(LocalDateTime.now().minusMinutes(1)))
            .forEach(entity -> {
                String logDetail = String.format("Row Created on Charge Detail with detRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
            });

        return savedEntities;
    }

    private List<GLPettyCashItemDtl> mergeItemDtls(List<GLPettyCashItemDtl> existing,
                                                   PettyCashRequestBase requestDto,
                                                   Long hdrPoid) {
        Map<Long, GLPettyCashItemDtl> existingMap = existing.stream()
                .collect(Collectors.toMap(GLPettyCashItemDtl::getDetRowId, d -> d));

        List<GLPettyCashItemDtl> toSave = new ArrayList<>();
        List<GLPettyCashItemDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GLPettyCashItemDtl>> logRequests = new ArrayList<>();
        String documentId = UserContext.getDocumentId();

        for (var dto : Optional.ofNullable(requestDto.getGlPettyCashItemDtlRequestDtos())
                .orElse(Collections.emptyList())) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = dto.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();

            switch (actionType) {
                case "ISCREATED":
                    // Create new record
                    GLPettyCashItemDtl newEntity = new GLPettyCashItemDtl();
                    newEntity.setTransactionPoid(hdrPoid);
                    
                    // Auto-generate detRowId if not provided
                    Long detRowId = dto.getDetRowId();
                    if (detRowId == null) {
                        // Find max existing detRowId and increment
                        Long maxDetRowId = existing.stream()
                                .map(GLPettyCashItemDtl::getDetRowId)
                                .filter(Objects::nonNull)
                                .max(Long::compareTo)
                                .orElse(0L);
                        detRowId = maxDetRowId + 1;
                    }
                    newEntity.setDetRowId(detRowId);
                    newEntity.setPoQty(dto.getPoQty());
                    newEntity.setDnQty(dto.getDnQty());
                    newEntity.setQtyReceived(dto.getQtyReceived());
                    newEntity.setPrice(dto.getPrice());
                    newEntity.setDiscount(dto.getDiscount());
                    newEntity.setTotal(dto.getTotal());
                    newEntity.setRemarks(dto.getRemarks());
                    newEntity.setRefDocId(dto.getRefDocId());
                    newEntity.setRefDocPoid(dto.getRefDocPoid());
                    newEntity.setCheckAll(dto.getCheckAll());
                    newEntity.setRefDetRowId(dto.getRefDetRowId());
                    newEntity.setVatPartyName(dto.getVatPartyName());
                    newEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    newEntity.setPartyInvDate(dto.getPartyInvDate());
                    newEntity.setTaxPoid(dto.getTaxPoid());
                    newEntity.setCreatedBy(getCurrentUser());
                    newEntity.setCreatedDate(LocalDateTime.now());
                    newEntity.setLastModifiedBy(getCurrentUser());
                    newEntity.setLastModifiedDate(LocalDateTime.now());

                    newEntity.setStockPoid(dto.getStockPoid());
                    newEntity.setStockUnitPoid(dto.getStockUnitPoid());

                    toSave.add(newEntity);
                    break;

                case "ISUPDATED":
                    // Update existing record
                    GLPettyCashItemDtl existingEntity = existingMap.get(dto.getDetRowId());
                    if (existingEntity == null) {
                        throw new RuntimeException("Item detail not found with detRowId: " + dto.getDetRowId());
                    }
                    
                    // Create copy for logging
                    GLPettyCashItemDtl oldEntity = new GLPettyCashItemDtl();
                    BeanUtils.copyProperties(existingEntity, oldEntity);
                    
                    existingEntity.setPoQty(dto.getPoQty());
                    existingEntity.setDnQty(dto.getDnQty());
                    existingEntity.setQtyReceived(dto.getQtyReceived());
                    existingEntity.setPrice(dto.getPrice());
                    existingEntity.setDiscount(dto.getDiscount());
                    existingEntity.setTotal(dto.getTotal());
                    existingEntity.setRemarks(dto.getRemarks());
                    existingEntity.setRefDocId(dto.getRefDocId());
                    existingEntity.setRefDocPoid(dto.getRefDocPoid());
                    existingEntity.setCheckAll(dto.getCheckAll());
                    existingEntity.setRefDetRowId(dto.getRefDetRowId());
                    existingEntity.setVatPartyName(dto.getVatPartyName());
                    existingEntity.setPartyInvNumber(dto.getPartyInvNumber());
                    existingEntity.setPartyInvDate(dto.getPartyInvDate());
                    existingEntity.setTaxPoid(dto.getTaxPoid());
                    existingEntity.setLastModifiedBy(getCurrentUser());
                    existingEntity.setLastModifiedDate(LocalDateTime.now());

                    existingEntity.setStockPoid(dto.getStockPoid());
                    existingEntity.setStockUnitPoid(dto.getStockUnitPoid());

                    toSave.add(existingEntity);
                    
                    // Add to batch logging
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", hdrPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GLPettyCashItemDtl.class, documentId, hdrPoid.toString(), logDetail));
                    break;

                case "ISDELETED":
                    // Mark for deletion - find entity and add to delete list
                    GLPettyCashItemDtl entityToDelete = existingMap.get(dto.getDetRowId());
                    if (entityToDelete != null) {
                        toDelete.add(entityToDelete);
                        // Log the deletion
                        loggingService.logDelete(entityToDelete, documentId, hdrPoid.toString());
                    }
                    break;

                case "NOCHANGES":
                    // Keep existing record as-is - add to toSave so it appears in response
                    GLPettyCashItemDtl unchangedItemEntity = existingMap.get(dto.getDetRowId());
                    if (unchangedItemEntity != null) {
                        toSave.add(unchangedItemEntity);
                    }
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    GLPettyCashItemDtl defaultItemEntity = existingMap.get(dto.getDetRowId());
                    if (defaultItemEntity != null) {
                        toSave.add(defaultItemEntity);
                    }
                    break;
            }
        }

        // Delete records marked for deletion - use deleteAll with entity objects (composite key)
        if (!toDelete.isEmpty()) {
            glPettyCashItemDtlRepository.deleteAll(toDelete);
        }
        
        // Save all entities
        List<GLPettyCashItemDtl> savedEntities = glPettyCashItemDtlRepository.saveAll(toSave);
        
        // Process batch logging for updates
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        
        // Log creation for new records
        savedEntities.stream()
            .filter(entity -> entity.getCreatedDate() != null && entity.getCreatedDate().isAfter(LocalDateTime.now().minusMinutes(1)))
            .forEach(entity -> {
                String logDetail = String.format("Row Created on Item Detail with detRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(documentId, hdrPoid.toString(), logDetail);
            });

        return savedEntities;
    }


    private void logResult(String procedureName, StringBuilder result) {
        System.out.println("[" + procedureName + "] => " + result);
        result.setLength(0);  /*clear for next procedure call*/
    }

    @Override
    @Transactional(readOnly = true)
    public PettyCashResponseDto findById(Long transactionPoid, String documentId) {

        PettyCashResponseDto response = new PettyCashResponseDto();

        try {

            GlPettyCashPaymentHdr header = glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new EntityNotFoundException("Header not found for TransactionPoid: " + transactionPoid));


            BeanUtils.copyProperties(header, response);
            log.info(" Header retrieved successfully: {}", header.getDocRef());


            List<GlPettyCashPaymentDtl> paymentEntities =
                    glPettyCashPaymentDtlRepository.findByTransactionPoid(header.getTransactionPoid());
            List<GlPettyCashChargeDtl> chargeEntities =
                    glPettyCashChargeDtlRepository.findByTransactionPoid(header.getTransactionPoid());
            List<GLPettyCashItemDtl> itemEntities =
                    glPettyCashItemDtlRepository.findByTransactionPoid(header.getTransactionPoid());

            List<GlPettyCashPaymentDtlResponseDto> paymentDtls = mapPaymentResponse(paymentEntities);

            Long transPoid = header.getTransactionPoid();
            Long groupPoid = header.getGroupPoid();
            Long companyPoid = header.getCompanyPoid();
            Long userPoid = UserContext.getUserPoid();

            GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse =
                    billwiseBreakupService.loadBillwiseBreakup(groupPoid, companyPoid, documentId, transPoid);

            GlVoucherCostCenterBreakupResponseDto costCenterResponse =
                    costCenterBreakupService.loadCostCenterData(documentId, transPoid, groupPoid, companyPoid, userPoid);

            for (GlPettyCashPaymentDtlResponseDto dtl : paymentDtls) {
                Long detRowId = dtl.getDetRowId();
                if (null != billwiseResponse && CollectionUtils.isNotEmpty(billwiseResponse.getLoadBillwiseBreakupResponseDtoList())) {
                    dtl.setBillwiseBreakupList(mapToPopupDto(billwiseResponse.getLoadBillwiseBreakupResponseDtoList().stream().filter(x -> x.getMainDetRowId().equals(detRowId)).collect(Collectors.toList())));
                }
                if (null != costCenterResponse && CollectionUtils.isNotEmpty(costCenterResponse.getCostBreakupList())) {
                    dtl.setCostCenterBreakupList(mapToCostCenterPopupDto(costCenterResponse.getCostBreakupList().stream().filter(x -> x.getMainDetRowId().equals(detRowId)).collect(Collectors.toList())));
                }
            }


            List<GlPettyCashChargeDtlResponseDto> chargeDtls = mapChargeResponse(chargeEntities);
            List<GLPettyCashItemDtlResponseDto> itemDtls = mapItemResponse(itemEntities);

            response.setPaymentDtls(paymentDtls);
            response.setChargeDtls(chargeDtls);
            response.setItemDtls(itemDtls);

            return response;

        } catch (Exception e) {

            throw new RuntimeException("Failed to load Petty Cash details: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deletePettyCashVoucher(Long transactionPoid, String docId, String refType, DeleteReasonDto deleteReasonDto) {
        GlPettyCashPaymentHdr header = glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new EntityNotFoundException("Header not found for TransactionPoid: " + transactionPoid));

        Long userGroupPoid = UserContext.getGroupPoid();
        Long userCompanyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        pettyCashPaymentVoucherCustomRepository.validateVoucherBeforeDelete(
                userGroupPoid, userPoid, userCompanyPoid, docId, refType, refType);
        
        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_PETTY_CASH_PAYMENT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                header.getTransactionDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        );
    }

    @Override
    public Map<String, Object> listPettyCashVoucher(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "REF_TYPE",
                "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    public List<PettyCashFromPoDto> loadPettyCashFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid,
            StringBuilder result
    ) {
        log.info("Loading Petty Cash From PO. RFQ POID: {}", rfqPoid);
        return pettyCashLoadByRefTypeRepository.loadPettyCashFromPo(
                loginGroupPoid,
                loginCompanyPoid,
                loginUserPoid,
                rfqPoid,
                result
        );
    }

    @Override
    public List<PettyCashFromFfDto> loadPettyCashFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            StringBuilder result
    ) {
        log.info("Loading Petty Cash From FF. FF POID: {}", ffPoid);
        return pettyCashLoadByRefTypeRepository.loadPettyCashFromFf(
                loginGroupPoid,
                loginCompanyPoid,
                loginUserPoid,
                ffPoid,
                result
        );
    }

    @Override
    public List<PettyCashFromFdaDto> loadPettyCashFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            StringBuilder result
    ) {
        log.info("Loading Petty Cash From FDA. FDA POID: {}", fdaPoid);
        return pettyCashLoadByRefTypeRepository.loadPettyCashFromFda(
                loginGroupPoid,
                loginCompanyPoid,
                loginUserPoid,
                fdaPoid,
                result
        );
    }

    @Override
    public List<PettyGlBalanceDto> loadPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String documentId,
            Long docKeyPoid,
            String lovName,
            Long lovValue
    ) {
        log.info("Loading Petty Cash GL Balance for DOC_ID: {}, LOV_NAME: {}", documentId, lovName);

        return pettyCashLoadByRefTypeRepository.getPettyGlBalance(
                loginGroupPoid,
                loginCompanyPoid,
                loginUserPoid,
                documentId,
                docKeyPoid,
                lovName,
                lovValue
        );
    }

    private List<BillwiseBreakupPopupRequestDto> mapToPopupDto(List<LoadBillwiseBreakupResponseDto> list) {

        if (list == null) return Collections.emptyList();

        return list.stream().map(src -> {
            BillwiseBreakupPopupRequestDto.BillwiseBreakupPopupRequestDtoBuilder builder =
                    BillwiseBreakupPopupRequestDto.builder()
                            .billDetRowId(src.getBillDetRowId())
                            .billRefType(src.getBillRefType())
                            .billRef(src.getBillRef())
                            .billDueDate(src.getBillDueDate())
                            .billRemarks(src.getBillRemarks())
                            .actionType("noChanges"); // Default actionType for loaded data

            // Set type and amount based on which one has value
            if (src.getDrAmt() != null && src.getDrAmt().compareTo(BigDecimal.ZERO) > 0) {
                builder.type("DR");
                builder.amount(src.getDrAmt());
            } else if (src.getCrAmt() != null && src.getCrAmt().compareTo(BigDecimal.ZERO) > 0) {
                builder.type("CR");
                builder.amount(src.getCrAmt());
            } else {
                // Default to DR if both are zero/null
                builder.type("DR");
                builder.amount(src.getDrAmt() != null ? src.getDrAmt() : BigDecimal.ZERO);
            }

            return builder.build();
        }).collect(Collectors.toList());
    }

    private List<PendingBillwiseBreakupDto> mapToPendingDto(
            List<ShowPendingBillwiseBreakupResponseDto> list) {

        if (list == null) return Collections.emptyList();

        return list.stream().map(src ->
                PendingBillwiseBreakupDto.builder()
                        .billRef(src.getBillRef())
                        .billDueDate(src.getBillDueDate())
                        .remarks(src.getRemarks())
                        .balance(src.getBalance())
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
                        .actionType("noChanges") // Default actionType for loaded data
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-101");
        params.put("SUB_PAYMENT_DTL", printService.load("Finance/GL/PettyCashPaymentDtl_subreport1.jrxml"));
        params.put("SUB_ITEM_DTL_2", printService.load("Finance/GL/PettyCashPaymentItemSubreport2.jrxml"));
        params.put("SUB_ITEM_DTL_1", printService.load("Finance/GL/PettyCashPaymentItemSubreport1.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/PettyCashPayment.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}