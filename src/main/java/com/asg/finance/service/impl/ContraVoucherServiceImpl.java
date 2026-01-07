package com.asg.finance.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.entity.Company;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.ContraVoucherDetailRequest;
import com.asg.finance.dto.ContraVoucherRequest;
import com.asg.finance.dto.ContraVoucherDetailResponse;
import com.asg.finance.dto.ContraVoucherFullResponse;
import com.asg.finance.dto.ContraVoucherResponse;
import com.asg.finance.dto.PoidDetailsDto;
import com.asg.finance.entity.GlContraVoucherDtl;
import com.asg.finance.entity.GlContraVoucherHdr;
import com.asg.finance.repository.GlContraVoucherDtlRepository;
import com.asg.finance.repository.GlContraVoucherHdrRepository;
import com.asg.common.lib.security.util.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContraVoucherServiceImpl implements ContraVoucherService {

    private final GlContraVoucherHdrRepository hdrRepository;
    private final GlContraVoucherDtlRepository dtlRepository;
    private final GLMasterRepository glMasterRepository;
    private final DataSource dataSource;
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final PrintService printService;

    @Override
    public Map<String, Object> listContraVouchers(String docId, FilterRequestDto request, Pageable pageable, LocalDate periodFrom, LocalDate periodTo) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        
        FilterRequestDto safeRequest = request;
        if (request != null && request.filters() == null) {
            safeRequest = new FilterRequestDto(
                request.operator(),
                request.isDeleted(),
                new ArrayList<>()
            );
        }
        
        List<FilterDto> filters = documentService.resolveDateFilters(safeRequest, "TRANSACTION_DATE", periodFrom, periodTo);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "TRANSACTION_DATE",
                "TRANSACTION_POID");
      
        List<Map<String, Object>> records = raw.records() != null ? raw.records() : new ArrayList<>();
        Page<Map<String, Object>> page = new PageImpl<>(records, pageable, raw.totalRecords());

        Map<String, String> displayFields = raw.displayFields() != null ? raw.displayFields() : Map.of();
        return PaginationUtil.wrapPage(page, displayFields);
    }

    private boolean applyFilters(GlContraVoucherHdr header, List<FilterDto> filters, String operator) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        List<Boolean> results = new ArrayList<>();
        
        for (FilterDto filter : filters) {
            String searchField = filter.searchField() != null ? filter.searchField().toUpperCase() : "";
            String searchValue = filter.searchValue();
            
            if (searchValue == null || searchValue.trim().isEmpty()) {
                continue;
            }

            boolean matches = false;
            
            switch (searchField) {
                case "DOCREF":
                case "DOC_REF":
                    matches = header.getDocRef() != null && 
                             header.getDocRef().toUpperCase().contains(searchValue.toUpperCase());
                    break;
                    
                case "NARRATION":
                case "POSTING_NARRATION":
                    matches = header.getPostingNarration() != null && 
                             header.getPostingNarration().toUpperCase().contains(searchValue.toUpperCase());
                    break;
                    
                case "COMPANY_ID":
                case "COMPANY_POID":
                    try {
                        Long companyId = Long.parseLong(searchValue);
                        matches = header.getCompanyPoid() != null && header.getCompanyPoid().equals(companyId);
                    } catch (NumberFormatException e) {
                        matches = false;
                    }
                    break;
                    
                case "CREDIT_GL":
                    try {
                        Long creditGl = Long.parseLong(searchValue);
                        matches = header.getCreditGl() != null && header.getCreditGl().equals(creditGl);
                    } catch (NumberFormatException e) {
                        matches = false;
                    }
                    break;
                    
                case "DEBIT_GL":
                    try {
                        Long debitGl = Long.parseLong(searchValue);
                        matches = header.getDebitGl() != null && header.getDebitGl().equals(debitGl);
                    } catch (NumberFormatException e) {
                        matches = false;
                    }
                    break;
                    
                case "CURRENCY_CODE":
                case "CURRENCY":
                    matches = header.getCurrencyCode() != null && 
                             header.getCurrencyCode().equalsIgnoreCase(searchValue);
                    break;
                    
                case "GLOBALSEARCH":
                    // Search across multiple fields
                    String searchUpper = searchValue.toUpperCase();
                    matches = (header.getDocRef() != null && header.getDocRef().toUpperCase().contains(searchUpper)) ||
                             (header.getPostingNarration() != null && header.getPostingNarration().toUpperCase().contains(searchUpper)) ||
                             (header.getCurrencyCode() != null && header.getCurrencyCode().toUpperCase().contains(searchUpper)) ||
                             (header.getCreatedBy() != null && header.getCreatedBy().toUpperCase().contains(searchUpper));
                    break;
                    
                default:
                    matches = false;
                    break;
            }
            
            results.add(matches);
        }
        
        if (results.isEmpty()) {
            return true;
        }
        
        // Apply operator
        if ("OR".equalsIgnoreCase(operator)) {
            return results.stream().anyMatch(b -> b);
        } else {
            // AND (default)
            return results.stream().allMatch(b -> b);
        }
    }

    @Override
    @Transactional
    public ContraVoucherFullResponse createContraVoucher(ContraVoucherRequest request) {
        log.info("createContraVoucher started");

        // Create header
        GlContraVoucherHdr header = new GlContraVoucherHdr();
        header.setCreditGl(request.getCreditGl());
        header.setDebitGl(request.getDebitGl());
        header.setCurrencyCode(request.getCurrencyCode());
        header.setCurrencyRate(request.getCurrencyRate());
        header.setAmount(request.getAmount());
        header.setBhdAmount(request.getBhdAmount());
        header.setPostingNarration(request.getPostingNarration());
        header.setChequeNo(request.getChequeNo());
        header.setManual(request.getManual());
        header.setChequeDate(request.getChequeDate());
        header.setMultiCompany(request.getMultiCompany());
        
        // Auto-generate docRef if not provided
        header.setGroupPoid(request.getGroupPoid() != null ? request.getGroupPoid() : ASGHelperUtils.getGroupId());
        if (request.getDocRef() == null || request.getDocRef().trim().isEmpty()) {
            Long maxDocRefNum = hdrRepository.findMaxDocRefNumber(header.getGroupPoid());
            Long nextDocRefNum = (maxDocRefNum != null ? maxDocRefNum : 10000L) + 1;
            header.setDocRef("ASG" + nextDocRefNum);
            log.info("Auto-generated docRef: {}", header.getDocRef());
        } else {
            header.setDocRef(request.getDocRef());
        }
        
        // Set transactionDate to LocalDate.now()
        header.setTransactionDate(LocalDate.now());
        header.setDeleted(request.getDeleted() != null ? request.getDeleted() : "N");
        header.setOldJvno(request.getOldJvno());
        header.setRemarks(request.getRemarks());
        header.setCompanyPoid(request.getCompanyPoid());
        header.setDrTotal(request.getDrTotal());
        header.setCrTotal(request.getCrTotal());
        header.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : ASGHelperUtils.getCurrentUser());

        GlContraVoucherHdr savedHeader = hdrRepository.save(header);

        // Process details from request
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            Long nextDetRowId = 1L;
            for (ContraVoucherDetailRequest detailRequest : request.getDetails()) {
                if ("isCreated".equalsIgnoreCase(detailRequest.getActionType())) {
                    GlContraVoucherDtl detail = new GlContraVoucherDtl();
                    detail.setTransactionPoid(savedHeader.getTransactionPoid());
                    // Always auto-generate sequential detRowId starting from 1 during create
                    detail.setDetRowId(nextDetRowId++);
                    detail.setType(detailRequest.getType());
                    detail.setCompanyPoid(detailRequest.getCompanyPoid() != null ? detailRequest.getCompanyPoid() : savedHeader.getCompanyPoid());
                    detail.setGlPoid(detailRequest.getGlPoid());
                    detail.setDrAmt(detailRequest.getDrAmt());
                    detail.setCrAmt(detailRequest.getCrAmt());
                    detail.setRemarks(detailRequest.getRemarks());
                    detail.setCreatedBy(ASGHelperUtils.getCurrentUser());
                    dtlRepository.save(detail);
                }
            }
        }

        log.info("createContraVoucher completed for transactionPoid={}", savedHeader.getTransactionPoid());
        return convertToFullResponse(savedHeader);
    }

    @Override
    @Transactional
    public ContraVoucherFullResponse updateContraVoucher(ContraVoucherRequest request) {
        log.info("updateContraVoucher started for transactionPoid={}", request.getTransactionPoid());

        if (request.getTransactionPoid() == null) {
            throw new IllegalArgumentException("transactionPoid is required for update operation");
        }

        // Update header
        GlContraVoucherHdr header = hdrRepository.findByTransactionPoid(request.getTransactionPoid())
                .orElseThrow(() -> new RuntimeException("Contra voucher not found with transactionPoid: " + request.getTransactionPoid()));

        header.setCreditGl(request.getCreditGl());
        header.setDebitGl(request.getDebitGl());
        header.setCurrencyCode(request.getCurrencyCode());
        header.setCurrencyRate(request.getCurrencyRate());
        header.setAmount(request.getAmount());
        header.setBhdAmount(request.getBhdAmount());
        header.setPostingNarration(request.getPostingNarration());
        header.setChequeNo(request.getChequeNo());
        header.setManual(request.getManual());
        header.setChequeDate(request.getChequeDate());
        header.setMultiCompany(request.getMultiCompany());
        header.setDocRef(request.getDocRef());
        if (request.getTransactionDate() != null) {
            header.setTransactionDate(request.getTransactionDate());
        }
        if (request.getDeleted() != null) {
            header.setDeleted(request.getDeleted());
        }
        header.setOldJvno(request.getOldJvno());
        header.setRemarks(request.getRemarks());
        header.setCompanyPoid(request.getCompanyPoid());
        header.setDrTotal(request.getDrTotal());
        header.setCrTotal(request.getCrTotal());

        GlContraVoucherHdr savedHeader = hdrRepository.save(header);

        // Process details based on actionType
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            List<GlContraVoucherDtl> existingDetails = dtlRepository.findByTransactionPoid(savedHeader.getTransactionPoid());
            Long maxDetRowId = existingDetails.stream()
                    .map(GlContraVoucherDtl::getDetRowId)
                    .max(Long::compareTo)
                    .orElse(0L);

            Long nextDetRowId = maxDetRowId + 1;

            for (ContraVoucherDetailRequest detailRequest : request.getDetails()) {
                String actionType = detailRequest.getActionType();
                
                if ("isCreated".equalsIgnoreCase(actionType)) {
                    // Create new detail - always auto-generate sequential detRowId starting from next available
                    GlContraVoucherDtl detail = new GlContraVoucherDtl();
                    detail.setTransactionPoid(savedHeader.getTransactionPoid());
                    detail.setDetRowId(nextDetRowId++);
                    detail.setType(detailRequest.getType());
                    detail.setCompanyPoid(detailRequest.getCompanyPoid() != null ? detailRequest.getCompanyPoid() : savedHeader.getCompanyPoid());
                    detail.setGlPoid(detailRequest.getGlPoid());
                    detail.setDrAmt(detailRequest.getDrAmt());
                    detail.setCrAmt(detailRequest.getCrAmt());
                    detail.setRemarks(detailRequest.getRemarks());
                    detail.setCreatedBy(ASGHelperUtils.getCurrentUser());
                    dtlRepository.save(detail);
                } else if ("isUpdated".equalsIgnoreCase(actionType)) {
                    // Update existing detail with request data
                    if (detailRequest.getDetRowId() == null) {
                        throw new IllegalArgumentException("detRowId is required for update operation");
                    }
                    GlContraVoucherDtl detail = dtlRepository
                            .findByTransactionPoidAndDetRowId(savedHeader.getTransactionPoid(), detailRequest.getDetRowId())
                            .orElseThrow(() -> new RuntimeException("Detail not found with detRowId: " + detailRequest.getDetRowId()));
                    
                    // Update with request values
                    if (detailRequest.getType() != null) {
                        detail.setType(detailRequest.getType());
                    }
                    if (detailRequest.getCompanyPoid() != null) {
                        detail.setCompanyPoid(detailRequest.getCompanyPoid());
                    }
                    if (detailRequest.getGlPoid() != null) {
                        detail.setGlPoid(detailRequest.getGlPoid());
                    }
                    if (detailRequest.getDrAmt() != null) {
                        detail.setDrAmt(detailRequest.getDrAmt());
                    }
                    if (detailRequest.getCrAmt() != null) {
                        detail.setCrAmt(detailRequest.getCrAmt());
                    }
                    if (detailRequest.getRemarks() != null) {
                        detail.setRemarks(detailRequest.getRemarks());
                    }
                    dtlRepository.save(detail);
                } else if ("isDeleted".equalsIgnoreCase(actionType)) {
                    // Delete detail
                    if (detailRequest.getDetRowId() == null) {
                        throw new IllegalArgumentException("detRowId is required for delete operation");
                    }
                    dtlRepository.findByTransactionPoidAndDetRowId(savedHeader.getTransactionPoid(), detailRequest.getDetRowId())
                            .ifPresent(dtlRepository::delete);
                }
                // noChange/noChanges - do nothing
            }
        }

        log.info("updateContraVoucher completed for transactionPoid={}", savedHeader.getTransactionPoid());
        return convertToFullResponse(savedHeader);
    }

    @Override
    @Transactional(readOnly = true)
    public ContraVoucherFullResponse getContraVoucherById(Long transactionPoid) {
        log.info("getContraVoucherById started for transactionPoid={}", transactionPoid);

        GlContraVoucherHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Contra voucher not found with transactionPoid: " + transactionPoid));

        log.info("getContraVoucherById completed for transactionPoid={}", transactionPoid);
        return convertToFullResponse(header);
    }

    @Override
    @Transactional
    public void deleteContraVoucher(Long transactionPoid) {
        log.info("deleteContraVoucher started for transactionPoid={}", transactionPoid);

        GlContraVoucherHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Contra voucher not found with transactionPoid: " + transactionPoid));

        // Soft delete - set deleted flag
        header.setDeleted("Y");
        hdrRepository.save(header);

        // Optionally delete details or mark them as deleted
        // For now, we'll keep details but you can add logic to delete them if needed

        log.info("deleteContraVoucher completed for transactionPoid={}", transactionPoid);
    }

    @Override
    public String checkGlNature(Long creditGlId) {
        log.info("checkGlNature started for creditGlId={}", creditGlId);

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        String sqlProc = "BEGIN PROC_GL_CONTRA_CHK_GL_NATURE(?,?,?,?,?); END;";
        
        CallableStatement statement = null;
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            statement = conn.prepareCall(sqlProc);

            statement.setLong(1, groupPoid);
            statement.setLong(2, companyPoid);
            statement.setLong(3, userPoid);
            statement.setLong(4, creditGlId);
            statement.registerOutParameter(5, Types.VARCHAR);

            statement.executeUpdate();

            String result = statement.getString(5);
            log.info("PROC_GL_CONTRA_CHK_GL_NATURE returned: {} for creditGlId={}", result, creditGlId);

            String finalResult = result != null ? result : "FALSE";
            log.info("checkGlNature completed for creditGlId={}, result={}", creditGlId, finalResult);
            return finalResult;

        } catch (SQLException e) {
            log.error("Error calling PROC_GL_CONTRA_CHK_GL_NATURE for creditGlId={}", creditGlId, e);
            throw new RuntimeException("Error checking GL nature: " + e.getMessage(), e);
        } finally {
            try {
                if (statement != null) statement.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                log.error("Error closing connection/statement", e);
            }
        }
    }


    private ContraVoucherResponse convertToResponse(GlContraVoucherHdr header) {
        ContraVoucherResponse response = new ContraVoucherResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setTransactionDate(header.getTransactionDate());
        response.setDocRef(header.getDocRef());
        response.setPostingNarration(header.getPostingNarration());
        response.setCreatedBy(header.getCreatedBy());
        response.setDeleted(header.getDeleted());
        response.setCreatedDate(header.getCreatedDate());
        return response;
    }

    private ContraVoucherFullResponse convertToFullResponse(GlContraVoucherHdr header) {
        ContraVoucherFullResponse response = new ContraVoucherFullResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setTransactionDate(header.getTransactionDate());
        response.setDocRef(header.getDocRef());
        response.setPostingNarration(header.getPostingNarration());
        response.setDeleted(header.getDeleted());
        response.setCompanyPoid(header.getCompanyPoid());
        response.setCurrencyCode(header.getCurrencyCode());
        response.setCurrencyRate(header.getCurrencyRate());
        response.setAmount(header.getAmount());
        response.setBhdAmount(header.getBhdAmount());
        response.setDrTotal(header.getDrTotal());
        response.setCrTotal(header.getCrTotal());
        response.setApprovalStatus(null); // ApprovalStatus column doesn't exist in database
        response.setCreatedBy(header.getCreatedBy());
        response.setCreatedDate(header.getCreatedDate());
        response.setCreditGl(header.getCreditGl());
        response.setCreditGlDetails(getGlDetails(header.getCreditGl()));
        response.setDebitGl(header.getDebitGl());
        response.setDebitGlDetails(getGlDetails(header.getDebitGl()));
        response.setCompanyPoidDetails(getCompanyDetails(header.getCompanyPoid()));
        response.setChequeNo(header.getChequeNo());
        response.setManual(header.getManual());
        response.setChequeDate(header.getChequeDate());
        response.setMultiCompany(header.getMultiCompany());
        response.setRemarks(header.getRemarks());

        // Fetch and convert details
        List<GlContraVoucherDtl> details = dtlRepository.findByTransactionPoid(header.getTransactionPoid());
        if (details != null && !details.isEmpty()) {
            List<ContraVoucherDetailResponse> detailResponses = details.stream()
                    .map(this::convertDetailToResponse)
                    .collect(Collectors.toList());
            response.setDetails(detailResponses);
        }

        return response;
    }

    private ContraVoucherDetailResponse convertDetailToResponse(GlContraVoucherDtl detail) {
        ContraVoucherDetailResponse response = new ContraVoucherDetailResponse();
        response.setDetRowId(detail.getDetRowId());
        response.setType(detail.getType());
        response.setCompanyPoid(detail.getCompanyPoid());
        response.setCompanyPoidDetails(getCompanyDetails(detail.getCompanyPoid()));
        response.setGlPoid(detail.getGlPoid());
        response.setGlPoidDetails(getGlDetails(detail.getGlPoid()));
        response.setDrAmt(detail.getDrAmt());
        response.setCrAmt(detail.getCrAmt());
        response.setRemarks(detail.getRemarks());
        return response;
    }

    /**
     * Fetches GL Master details (POID, CODE, DESCRIPTION) for a given GL POID
     */
    private PoidDetailsDto getGlDetails(Long glPoid) {
        if (glPoid == null) {
            return null;
        }
        try {
            Optional<GLMaster> glMaster = glMasterRepository.findByGlPoid(glPoid);
            if (glMaster.isPresent()) {
                GLMaster gl = glMaster.get();
                PoidDetailsDto details = new PoidDetailsDto();
                details.setPoid(gl.getGlPoid());
                details.setCode(gl.getGlCode());
                details.setDescription(gl.getGlDescription());
                return details;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GL Master details for glPoid={}: {}", glPoid, e.getMessage());
        }
        return null;
    }

    /**
     * Fetches Company details (POID, CODE, DESCRIPTION) for a given Company POID
     */
    private PoidDetailsDto getCompanyDetails(Long companyPoid) {
        if (companyPoid == null) {
            return null;
        }
        try {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovNameFast(companyPoid, "COMPANY");
            if (lovGetListDto != null) {
                PoidDetailsDto details = new PoidDetailsDto();
                details.setPoid(lovGetListDto.getPoid());
                details.setCode(lovGetListDto.getCode());
                details.setDescription(lovGetListDto.getDescription());
                return details;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Company details for companyPoid={}: {}", companyPoid, e.getMessage());
        }
        return null;
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-103");
        params.put("SUB_DETAIL", printService.load("Finance/GL/ContraVoucherReportDtlSubreport1.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/ContraVoucherReport1.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}

