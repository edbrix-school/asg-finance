package com.asg.finance.service;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.client.CompanyServiceClient;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlExpenseReallocationDtl;
import com.asg.finance.entity.GlExpenseReallocationHdr;
import com.asg.finance.entity.GlExpenseReallocationXlDtl;
import com.asg.finance.repository.*;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseReallocationServiceImpl implements ExpenseReallocationService {

    private final GlExpenseReallocationHdrRepository hdrRepository;
    private final GlExpenseReallocationDtlRepository dtlRepository;
    private final GlExpenseReallocationXlDtlRepository xlDtlRepository;
    private final ExpenseReallocationStoredProcedure storedProcedureHelper;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final CompanyServiceClient companyServiceClient;
    private final CostCenterRepository costCenterRepository;
    private final LovDataService lovService;
    private final LoggingService loggingService;
    private final LovDataService lovDataService;

    private static final String TRANSACTION_POID = "TRANSACTION_POID";
    private static final String COMPANYCODE = "Company Code";

    @Override
    @Transactional
    public ExpenseReallocationResponse createExpenseReallocation(CreateExpenseReallocationRequest request,
                                                                 Long groupPoid, Long companyPoid, String userId) {

        log.info("createExpenseReallocation started for groupPoid={} userId={}", groupPoid, userId);

        validateMandatoryFields(request);
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new RuntimeException("At least one detail line is required");
        }

        GlExpenseReallocationHdr header = GlExpenseReallocationHdr.builder()
                .transactionDate(DateUtil.getCurrentDateInUserTimeZone()).groupPoid(groupPoid).companyPoid(companyPoid)
                .expenseGroupGl(request.getExpenseGroupGlId()).fromCompany(request.getFromCompanyId())
                .fromDate(request.getFromDate()).toDate(request.getToDate()).allocationType(request.getAllocationType())
                .costPoid(request.getCostCode()).remarks(request.getRemarks())
                .deleted("N").reportGeneration("N")
                .narration(request.getNarration()).build();

        final GlExpenseReallocationHdr savedHdr = hdrRepository.save(header);
        final Long hdrPoid = savedHdr.getTransactionPoid();

        xlDtlRepository.deleteByTransactionPoid(hdrPoid);
        AtomicLong xlDtlDetRowIdSeq = new AtomicLong(1);

        List<GlExpenseReallocationXlDtl> xlDtlEntities = mapXlDtoToEnity(mapXlDetailtoEntity(request.getDetails(), hdrPoid, xlDtlDetRowIdSeq));

        xlDtlRepository.saveAll(xlDtlEntities);

        // Log child record creation for XL details
        xlDtlEntities.forEach(xlDetail -> {
            String logDetail = String.format("Row Created on Expense Reallocation XL Detail with detRowId: %s", xlDetail.getDetRowId());
            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), hdrPoid.toString(), logDetail);
        });
        log.info("createExpenseReallocation completed for transactionPoid={}", savedHdr.getTransactionPoid());
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), hdrPoid.toString());
        return buildResponse(savedHdr);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseReallocationResponse getExpenseReallocationById(Long transactionPoid) {
        log.info("getExpenseReallocationById started for transactionPoid={} groupPoid={}", transactionPoid);

        GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
                        transactionPoid));

        ExpenseReallocationResponse response = buildResponse(header);

        log.info("getExpenseReallocationById completed for transactionPoid={}", transactionPoid);
        return response;
    }

    @Override
    @Transactional
    public ExpenseReallocationResponse updateExpenseReallocation(Long transactionPoid,
                                                                 UpdateExpenseReallocationRequest request, Long groupPoid, String userId) {
        log.info("updateExpenseReallocation started for transactionPoid={} groupPoid={} userId={}", transactionPoid,
                groupPoid, userId);

        GlExpenseReallocationHdr existingHeader = hdrRepository
                .findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
                        transactionPoid));

        GlExpenseReallocationHdr header = new GlExpenseReallocationHdr();
        BeanUtils.copyProperties(existingHeader, header);

        if ("Y".equals(header.getDeleted())) {
            throw new RuntimeException("Cannot update deleted expense reallocation");
        }

        validateMandatoryFieldsForUpdate(request);

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new RuntimeException("At least one detail line is required");
        }

        validateDetailLines(request.getDetails());

        header.setTransactionDate(request.getTransactionDate());
        header.setCompanyPoid(request.getCompanyPoid());
        header.setNarration(request.getNarration());
        header.setExpenseGroupGl(request.getExpenseGroupGlId());
        header.setFromCompany(request.getFromCompanyId());
        header.setFromDate(request.getFromDate());
        header.setToDate(request.getToDate());
        header.setAllocationType(request.getAllocationType());
        header.setCostPoid(request.getCostCode());
        header.setRemarks(request.getRemarks());

        GlExpenseReallocationHdr savedHeader = hdrRepository.save(header);

        // Process details based on actionType
        processXlDetails(transactionPoid, mapXlDetailtoEntity(request.getDetails(), transactionPoid, null), userId);

        log.info("updateExpenseReallocation completed for transactionPoid={}", transactionPoid);
        String key = header.getTransactionPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(existingHeader, header, GlExpenseReallocationHdr.class, docId, key,
                LogDetailsEnum.MODIFIED, "SUPPLIER_POID");
        return buildResponse(savedHeader);
    }

    @Override
    @Transactional
    public void deleteExpenseReallocation(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("deleteExpenseReallocation started for transactionPoid={}", transactionPoid);

        hdrRepository.findByTransactionPoid(transactionPoid).orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
                transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_EXPENSE_REALLOCATION_HDR",
                TRANSACTION_POID,
                deleteReasonDto,
                null
        );

        log.info("deleteExpenseReallocation completed for transactionPoid={}", transactionPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listOfRecordsAndGenericSearch(String documentId, FilterRequestDto filters,
                                                             LocalDate startDate, LocalDate endDate, Pageable pageable) {

        log.info("getExpenseReallocations started for docId={}", documentId);

        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate,
                endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted, "NARRATION",
                TRANSACTION_POID);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        log.info("getExpenseReallocations completed for docId={} count={}", documentId, page.getNumber());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public Map<String, String> createJv(Long transactionPoid, Long groupPoid, Long companyPoid, Long userId) {
        log.info("createJv started for transactionPoid={} groupPoid={} companyPoid={} userId={}", transactionPoid,
                groupPoid, companyPoid, userId);

        GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
                        transactionPoid));

        if (header.getJvPoid() != null) {
            throw new RuntimeException("JV already created for this expense reallocation");
        }

        ValidateAllocationResponse validation = validateAllocation(transactionPoid, groupPoid);
        if (!validation.getValid()) {
            throw new RuntimeException("Allocations are not valid: " + String.join(", ", validation.getErrors()));
        }

        LocalDate toDate = header.getToDate();
        Long expenseGroupGL = header.getExpenseGroupGl();
        String costPoid = header.getCostPoid();

        return storedProcedureHelper.createJv(groupPoid, userId, companyPoid, transactionPoid,
                expenseGroupGL, toDate, costPoid);
    }

    private int getMergedColumnSpan(Sheet sheet, int rowIndex, int colIndex) {

        for (CellRangeAddress region : sheet.getMergedRegions()) {

            if (region.isInRange(rowIndex, colIndex)) {
                return region.getLastColumn() - region.getFirstColumn() + 1;
            }
        }
        return 1; // Not merged
    }

    @Override
    public List<Map<String, Object>> processExpenseAllocationExcel(MultipartFile file) {

        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = new ArrayList<>();

            Row headerRow1 = sheet.getRow(1);
            Row headerRow2 = sheet.getRow(2);

            if (headerRow1 == null || headerRow2 == null) {
                throw new ValidationException("Invalid template: Header rows missing");
            }

            int colIndex = 0;
            int span = getMergedColumnSpan(sheet, 1, 1);

            for (int i = 0; i < span + 1; i++) {

                String costCenterCode = StringUtils.trimToNull(
                        getStringCell(headerRow2.getCell(colIndex++))
                );

                if (costCenterCode == null || costCenterCode.isBlank()) {
                    headers.add(null); // mark as skip column
                    continue;
                }

                if (COMPANYCODE.equalsIgnoreCase(costCenterCode) ||
                        "TOTAL".equalsIgnoreCase(costCenterCode)) {

                    headers.add(costCenterCode);
                    continue;
                }

                boolean exists = costCenterRepository.existsByCostCenterCode(costCenterCode);

                if (!exists) {
                    throw new ValidationException("Invalid Cost Center Code: " + costCenterCode);
                }

                headers.add(costCenterCode);
            }

            for (int r = 3; r <= sheet.getLastRowNum(); r++) {

                Row row = sheet.getRow(r);
                if (row == null) continue;

                String companyCode = StringUtils.trimToNull(getStringCell(row.getCell(0)));

                if (companyCode == null || "Totals".equalsIgnoreCase(companyCode)) {
                    continue;
                }

                Map<String, Object> rowMap = new HashMap<>();
                BigDecimal rowTotal = BigDecimal.ZERO;

                for (int c = 0; c < headers.size(); c++) {

                    String key = headers.get(c);
                    Cell cell = row.getCell(c);

                    if (key == null) {
                        continue;
                    }

                    if (COMPANYCODE.equalsIgnoreCase(key)) {

                        String companyCodeValue = StringUtils.trimToNull(getStringCell(cell));

                        if (companyCodeValue == null) {
                            throw new ValidationException(
                                    "Company Code cannot be empty at row " + (r + 1)
                            );
                        }

                        LovGetListDto dto = mapLovDetails(companyCodeValue, "COMPANY", false);

                        if (dto == null || dto.getPoid() == null) {
                            throw new ValidationException("Invalid Company Code Value: " + companyCodeValue);
                        }
                        rowMap.put("company", dto.getPoid());
                        rowMap.put("companyName", dto.getDescription());
                        rowMap.put("companyCode", companyCodeValue);
                        continue;
                    }

                    BigDecimal value = getDecimal(cell);
                    value = value != null ? value : BigDecimal.ZERO;

                    rowMap.put(key, value);

                    if (!"TOTAL".equalsIgnoreCase(key)) {
                        rowTotal = rowTotal.add(value);
                    }
                }

                Cell totalCell = row.getCell(headers.size() - 1);
                BigDecimal excelTotal = getDecimal(totalCell);
                excelTotal = excelTotal != null ? excelTotal : BigDecimal.ZERO;

                if (rowTotal.compareTo(excelTotal) != 0) {
                    throw new RuntimeException(
                            "Invalid Total at row " + (r + 1) +
                                    ". Expected: " + rowTotal + " Found: " + excelTotal
                    );
                }

                grandTotal = grandTotal.add(rowTotal);
                result.add(rowMap);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Excel", e);
        }

        if (grandTotal.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException(
                    "Total allocation must be 100%, found: " + grandTotal
            );
        }

        return result;
    }

    @Override
    public byte[] exportExpenseAllocationExcel() {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Expense Allocation");

        // Title Style
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setFontName("Aptos Narrow Bold");
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);

        // Subtitle Style
        CellStyle subtitleStyle = workbook.createCellStyle();
        Font subtitleFont = workbook.createFont();
        subtitleFont.setBold(true);
        subtitleStyle.setFont(subtitleFont);
        subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Header Style (Bold + Border)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        applyBorders(headerStyle, BorderStyle.MEDIUM);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Footer Style (Italic)
        CellStyle footerStyle = workbook.createCellStyle();
        Font footerFont = workbook.createFont();
        footerFont.setItalic(true);
        footerStyle.setFont(footerFont);
        footerStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Data Cell Style (Border)
        CellStyle dataStyle = workbook.createCellStyle();
        Font dataFont = workbook.createFont();
        dataFont.setBold(true);
        dataStyle.setFont(dataFont);
        applyBorders(dataStyle, BorderStyle.MEDIUM);
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);

        // TITLE ROW
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Expense Allocation Template");
        titleCell.setCellStyle(titleStyle);

        // SUBTITLE ROW
        Row subtitleRow = sheet.createRow(1);
        Cell subtitleCell = subtitleRow.createCell(1);
        subtitleCell.setCellValue("Overhead Cost Centre Code (as per the ERP System)");
        subtitleCell.setCellStyle(subtitleStyle);

        // HEADER ROW
        Row headerRow = sheet.createRow(2);
        List<String> costCenterKeys = List.of("SH_", "FF_", "PROPERTIES_", "MTA_", "ADMIN_");
        List<String> headers = new ArrayList<>();
        headers.add(COMPANYCODE);
        headers.addAll(costCenterKeys); // dynamic allocation columns
        headers.add(null);
        headers.add(null);
        headers.add(null);
        headers.add("TOTAL");

        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, headers.size() - 1));

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            if (headers.get(i) != null) {
                cell.setCellValue(headers.get(i));
            }
            cell.setCellStyle(headerStyle);
        }

        Object[][] data = {{"ASG", "", "", "", "", "", "", "", ""},
                {"NSA1", "", "", "", "", "", "", "", ""},
                {"DSA", "", "", "", "", "", "", "", ""},
                {"FSL", "", "", "", "", "", "", "", ""}};

        int rowIdx = 3;

        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowIdx++);
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = row.createCell(col);
                String header = headers.get(col);

                if ("TOTAL".equals(header)) {
                    cell.setCellStyle(dataStyle);
                    cell.setCellFormula(String.format("SUM(B%d:I%d)",
                            row.getRowNum() + 1,
                            row.getRowNum() + 1));
                } else if (header == null) {
                    cell.setBlank();
                    cell.setCellStyle(dataStyle);
                } else {
                    Object value = col < rowData.length ? rowData[col] : null;

                    if (value instanceof Number) {
                        double num = ((Number) value).doubleValue();
                        cell.setCellValue(num); // includes 0
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue(0); // numeric default
                    }
                    cell.setCellStyle(headerStyle);
                }
            }
        }
        Row footerRow = sheet.createRow(0);
        Cell footerTitle = footerRow.createCell(0);
        footerTitle.setCellValue("Expense Allocation Template");
        footerTitle.setCellStyle(titleStyle);

        Row finalRow = sheet.createRow(rowIdx + 2);
        Cell cell = finalRow.createCell(headers.size() - 3);
        cell.setCellValue("This should be always 100%");
        cell.setCellStyle(footerStyle);

        Cell cell1 = finalRow.createCell(headers.size() - 1);
        cell1.setCellStyle(footerStyle);
        cell1.setCellFormula("SUM(J4:J9)");

        Font grantTotal = workbook.createFont();
        grantTotal.setBold(true);
        CellStyle grantTotalStyle = workbook.createCellStyle();
        applyBorders(grantTotalStyle, BorderStyle.THICK);
        grantTotalStyle.setAlignment(HorizontalAlignment.RIGHT);
        grantTotalStyle.setFont(grantTotal);

        cell1.setCellStyle(grantTotalStyle);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    private void applyBorders(CellStyle style, BorderStyle border) {
        style.setBorderTop(border);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
    }

    @Override
    @Transactional
    public String generateReport(Long transactionPoid, Long groupPoid, String userId) {
        log.info("generateReport started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

        GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
                        transactionPoid));

        LocalDate toDate = header.getToDate();
        Long expenseGroupGL = header.getExpenseGroupGl();
        Long companyPoid = header.getCompanyPoid();
        String costPoid = header.getCostPoid();

        storedProcedureHelper.generateReport(companyPoid, toDate, expenseGroupGL, transactionPoid, costPoid);
        return "Report generated successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateAllocationResponse validateAllocation(Long transactionPoid, Long groupPoid) {

        log.info("validateAllocation started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

        hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid", transactionPoid));

        List<GlExpenseReallocationXlDtl> details = xlDtlRepository
                .findByTransactionPoid(transactionPoid)
                .orElse(Collections.emptyList());

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        List<ExpenseReallocationDetailResponseDto> xlDetails = Collections.emptyList();

        if (details.isEmpty()) {
            errors.add("At least one detail line is required");
        } else {

            xlDetails = modifyXlDetail(details);

            Set<Long> companySet = new HashSet<>();

            for (ExpenseReallocationDetailResponseDto detail : xlDetails) {
                if (detail.getCompany() != null && !companySet.add(detail.getCompany())) {
                    errors.add("Duplicate company found: " + detail.getCompany());
                }
            }
        }

        Map<String, BigDecimal> totals = xlDetails.isEmpty()
                ? Collections.emptyMap()
                : calculateTotals(xlDetails);

        ValidateAllocationResponse response = new ValidateAllocationResponse();
        response.setValid(errors.isEmpty());
        response.setErrors(errors);
        response.setWarnings(warnings);
        response.setTotals(totals);

        log.info("validateAllocation completed for transactionPoid={} valid={}", transactionPoid, response.getValid());

        return response;
    }

    @Override
    public ComputeTotalsResponse computeTotals(ComputeTotalsRequest request) {
        log.info("computeTotals started");

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new RuntimeException("Details are required");
        }

        BigDecimal totalSh = BigDecimal.ZERO;
        BigDecimal totalFf = BigDecimal.ZERO;
        BigDecimal totalFfs = BigDecimal.ZERO;
        BigDecimal totalFfp = BigDecimal.ZERO;
        BigDecimal totalProperties = BigDecimal.ZERO;
        BigDecimal totalMta = BigDecimal.ZERO;
        BigDecimal totalPda = BigDecimal.ZERO;
        BigDecimal totalAdmin = BigDecimal.ZERO;

        for (ComputeTotalsRequest.AllocationDetail detail : request.getDetails()) {
            if (detail.getSh() != null)
                totalSh = totalSh.add(detail.getSh());
            if (detail.getFf() != null)
                totalFf = totalFf.add(detail.getFf());
            if (detail.getFfs() != null)
                totalFfs = totalFfs.add(detail.getFfs());
            if (detail.getFfp() != null)
                totalFfp = totalFfp.add(detail.getFfp());
            if (detail.getProperties() != null)
                totalProperties = totalProperties.add(detail.getProperties());
            if (detail.getMta() != null)
                totalMta = totalMta.add(detail.getMta());
            if (detail.getPda() != null)
                totalPda = totalPda.add(detail.getPda());
            if (detail.getAdmin() != null)
                totalAdmin = totalAdmin.add(detail.getAdmin());
        }

        BigDecimal grandTotal = totalSh.add(totalFf).add(totalFfs).add(totalFfp).add(totalProperties).add(totalMta)
                .add(totalPda).add(totalAdmin);

        ComputeTotalsResponse.AllocationTotals totals = new ComputeTotalsResponse.AllocationTotals();
        totals.setTotalSh(totalSh.setScale(3, RoundingMode.HALF_UP));
        totals.setTotalFf(totalFf.setScale(3, RoundingMode.HALF_UP));
        totals.setTotalFfs(totalFfs.setScale(3, RoundingMode.HALF_UP));
        totals.setTotalFfp(totalFfp.setScale(3, RoundingMode.HALF_UP));
        totals.setTotalProperties(totalProperties.setScale(3, RoundingMode.HALF_UP));
        totals.setTotalMta(totalMta.setScale(3, RoundingMode.HALF_UP));
        totals.setTotalPda(totalPda.setScale(3, RoundingMode.HALF_UP));
        totals.setTotalAdmin(totalAdmin.setScale(3, RoundingMode.HALF_UP));

        ComputeTotalsResponse response = new ComputeTotalsResponse();
        response.setTotals(totals);
        response.setGrandTotal(grandTotal.setScale(3, RoundingMode.HALF_UP));

        log.info("computeTotals completed");
        return response;
    }

    @Override
    public ExpenseReallocationConfigResponse getConfig() {
        log.info("getConfig started");

        ExpenseReallocationConfigResponse response = new ExpenseReallocationConfigResponse();
        response.setGlPosting(false);
        response.setAllowEditAfterJvCreation(false);
        response.setAllowDeleteAfterJvCreation(false);
        response.setScale(3);

        List<String> allocationColumns = allocationKeys();

        response.setAllocationColumns(allocationColumns);

        log.info("getConfig completed");
        return response;
    }

    private void validateMandatoryFields(CreateExpenseReallocationRequest request) {
        if (request.getExpenseGroupGlId() == null) {
            throw new ValidationException("Expense Group GL is required");
        }
        if (request.getFromCompanyId() == null) {
            throw new ValidationException("From Company is required");
        }
    }

    private void validateMandatoryFieldsForUpdate(UpdateExpenseReallocationRequest request) {
        if (request.getTransactionDate() == null) {
            throw new ValidationException("Transaction Date is required");
        }
        if (request.getCompanyPoid() == null) {
            throw new ValidationException("Company POID is required");
        }
        if (request.getExpenseGroupGlId() == null) {
            throw new ValidationException("Expense Group GL is required");
        }
        if (request.getFromCompanyId() == null) {
            throw new ValidationException("From Company is required");
        }
    }

    private void validateDetailLines(List<ExpenseReallocationXlDetailRequest> details) {
        Set<Long> companySet = new HashSet<>();
        for (ExpenseReallocationXlDetailRequest detail : details) {
            if (detail.getCompany() == null) {
                throw new ValidationException("Company is required for all detail lines");
            }
            if (!companySet.add(detail.getCompany())) {
                throw new IllegalStateException("Duplicate company found: " + detail.getCompany());
            }
            Map<String, BigDecimal> map = Optional.ofNullable(detail.getCostCenterMap())
                    .orElse(new HashMap<>());

            BigDecimal totalValue = map.get("TOTAL");

            if (totalValue == null) {

                BigDecimal total = map.entrySet().stream()
                        .filter(e -> !e.getKey().equalsIgnoreCase("TOTAL"))
                        .map(e -> e.getValue() != null ? e.getValue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                map.put("TOTAL", total.setScale(3, RoundingMode.HALF_UP));
            }
        }
    }

    private ExpenseReallocationResponse buildResponse(GlExpenseReallocationHdr header) {
        ExpenseReallocationResponse response = new ExpenseReallocationResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setTransactionDate(header.getTransactionDate());
        response.setGroupPoid(header.getGroupPoid());

        response.setDocRef(header.getDocRef());
        response.setNarration(header.getNarration());

        response.setJvPoid(header.getJvPoid());
        response.setJvRef(header.getJvRef());
        response.setRemarks(header.getRemarks());
        response.setFromDate(header.getFromDate());
        response.setToDate(header.getToDate());
        response.setCostPoid(header.getCostPoid());
        response.setCostCode(header.getCostPoid());
        response.setReportGeneration(header.getReportGeneration());
        response.setAllocationType(header.getAllocationType());
        response.setCreatedBy(header.getCreatedBy());
        response.setCreatedDate(header.getCreatedDate());
        response.setLastmodifiedBy(header.getLastModifiedBy());
        response.setLastmodifiedDate(header.getLastModifiedDate());
        response.setDeleted(header.getDeleted());
        response.setGlPosting(false);

        response.setCompanyPoid(header.getCompanyPoid());

        Set<Long> companyPoids = new HashSet<>();
        Map<Long, LovGetListDto> companyCache = new HashMap<>();

        companyPoids.add(header.getCompanyPoid());
        companyPoids.add(header.getFromCompany());

        List<ExpenseReallocationDetailResponseDto> details;

        List<GlExpenseReallocationXlDtl> xlDetails =
                xlDtlRepository.findByTransactionPoid(header.getTransactionPoid())
                        .orElse(new ArrayList<>());

        if (!xlDetails.isEmpty()) {

            List<ExpenseReallocationDetailResponseDto> xlDtos = modifyXlDetail(xlDetails);

            companyPoids.addAll(
                    xlDtos.stream()
                            .map(ExpenseReallocationDetailResponseDto::getCompany)
                            .toList()
            );

            loadCompanyCache(companyPoids, companyCache);

            details = xlDtos.stream()
                    .map(val -> {
                        LovGetListDto company = companyCache.get(val.getCompany());
                        val.setCompanyName(company.getDescription());
                        return val;
                    })
                    .toList();

        } else {

            List<GlExpenseReallocationDtl> oldDetails =
                    dtlRepository.findByTransactionPoid(header.getTransactionPoid())
                            .orElse(new ArrayList<>());

            companyPoids.addAll(
                    oldDetails.stream()
                            .map(GlExpenseReallocationDtl::getCompany)
                            .toList()
            );

            loadCompanyCache(companyPoids, companyCache);

            details = oldDetails.stream()
                    .map(val -> {
                        ExpenseReallocationDetailResponseDto res = convertDetailToResponse(val);

                        LovGetListDto company = companyCache.get(val.getCompany());

                        res.setCompanyName(
                                val.getCompanyName() != null
                                        ? val.getCompanyName()
                                        : company.getDescription()
                        );
                        res.setCompanyCode(company.getCode());

                        return res;
                    })
                    .toList();
        }

        loadCompanyCache(companyPoids, companyCache);

        response.setCompanyName(companyCache.get(header.getCompanyPoid()).getDescription());
        response.setFromCompanyId(header.getFromCompany());
        response.setFromCompanyName(companyCache.get(header.getFromCompany()).getDescription());

        setExpenseGroupInfo(response, header.getExpenseGroupGl());

        response.setDetails(details);
        response.setDetailTotals(calculateTotals(details));

        return response;
    }

    private void loadCompanyCache(Set<Long> companyPoids,
                                  Map<Long, LovGetListDto> cache) {

        companyPoids.forEach(poid -> {
            cache.computeIfAbsent(poid, this::getCompanyInfo);
        });
    }

    private List<ExpenseReallocationDetailResponseDto> modifyXlDetail(List<GlExpenseReallocationXlDtl> xlDetails) {
        Map<String, ExpenseReallocationDetailResponseDto> groupedMap = new LinkedHashMap<>();

        for (GlExpenseReallocationXlDtl curr : xlDetails) {
            String key = curr.getCompanyCode();

            ExpenseReallocationDetailResponseDto dto =
                    groupedMap.computeIfAbsent(key, k -> {
                        ExpenseReallocationDetailResponseDto newDto =
                                new ExpenseReallocationDetailResponseDto();

                        newDto.setTransactionPoid(curr.getTransactionPoid());
                        newDto.setCompany(curr.getCompany());
                        newDto.setCompanyCode(curr.getCompanyCode());
                        newDto.setDetRowId(curr.getDetRowId());

                        return newDto;
                    });

            String costCenter = curr.getCostCentre() != null
                    ? curr.getCostCentre().toUpperCase().trim()
                    : null;

            if (costCenter != null && curr.getPercent() != null) {
                dto.getCostCenterMap().put(costCenter, curr.getPercent());
                BigDecimal existingTotal = dto.getCostCenterMap()
                        .getOrDefault("TOTAL", BigDecimal.ZERO);

                dto.getCostCenterMap().put(
                        "TOTAL",
                        existingTotal.add(curr.getPercent() != null ? curr.getPercent() : BigDecimal.ZERO)
                );
            }

        }

        return new ArrayList<>(groupedMap.values());
    }

    private LovGetListDto getCompanyInfo(Long companyPoid) {
        return getLov(companyPoid, "COMPANY");
    }

    private void setExpenseGroupInfo(ExpenseReallocationResponse response, Long expenseGroupGl) {
        if (expenseGroupGl == null) return;

        response.setExpenseGroupGlId(expenseGroupGl);


        LovGetListDto gl = mapLovDetails(expenseGroupGl, "GL_MASTER_GROUPS", true);
        if (gl != null) {
            response.setExpenseGroupGlDtl(gl);
        }
    }

    private LovGetListDto mapLovDetails(Object value, String lovName, boolean isPoid) {
        if (value == null) return null;

        try {
            if (isPoid) {
                Long poid = value instanceof Long ? (Long) value : Long.valueOf(value.toString());
                return lovService.getDetailsByPoidAndLovName(poid, lovName);
            } else {
                String code = value.toString();
                return lovService.getDetailsByCodeAndLovName(code, lovName);
            }
        } catch (Exception e) {
            log.warn("Failed to map LOV details for value: {}, lovName: {}, isPoid: {}", value, lovName, isPoid, e);
            return null;
        }
    }

    private Map<String, BigDecimal> calculateTotals(List<ExpenseReallocationDetailResponseDto> details) {

        Map<String, BigDecimal> result = new HashMap<>();

        for (ExpenseReallocationDetailResponseDto detail : details) {
            Map<String, BigDecimal> costCenterMap = detail.getCostCenterMap();
            if (costCenterMap == null) continue;
            for (Map.Entry<String, BigDecimal> entry : costCenterMap.entrySet()) {
                String key = entry.getKey();
                BigDecimal value = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
                String finalKey = "TOTAL".equalsIgnoreCase(key)
                        ? "grandTOTAL"
                        : "total" + key;
                result.merge(finalKey, value, BigDecimal::add);
            }
        }

        return result;
    }

    private ExpenseReallocationDetailResponseDto convertDetailToResponse(GlExpenseReallocationDtl detail) {

        ExpenseReallocationDetailResponseDto response =
                new ExpenseReallocationDetailResponseDto();

        response.setTransactionPoid(detail.getTransactionPoid());
        response.setDetRowId(detail.getDetRowId());
        Long companyPoid = detail.getCompany();
        response.setCompany(companyPoid);

        Map<String, BigDecimal> costCenterMap = new HashMap<>();

        for (Field field : GlExpenseReallocationDtl.class.getDeclaredFields()) {

            if (field.getType().equals(BigDecimal.class)) {

                field.setAccessible(true);

                try {
                    BigDecimal value = (BigDecimal) field.get(detail);

                    String key = field.getName().toUpperCase();
                    costCenterMap.put(key, value);

                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Error accessing field: " + field.getName(), e);
                }
            }
        }

        response.setCostCenterMap(costCenterMap);

        return response;
    }

    private BigDecimal getDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;

        try {
            switch (cell.getCellType()) {

                case NUMERIC, FORMULA:
                    return BigDecimal.valueOf(cell.getNumericCellValue());

                case STRING:
                    String value = cell.getStringCellValue();
                    if (value == null || value.trim().isEmpty()) {
                        return BigDecimal.ZERO;
                    }
                    return new BigDecimal(value.trim());

                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String getStringCell(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {

                case STRING:
                    return cell.getStringCellValue().trim();

                case NUMERIC:
                    double num = cell.getNumericCellValue();
                    return (num == (long) num)
                            ? String.valueOf((long) num)
                            : String.valueOf(num);

                case FORMULA:
                    return cell.getStringCellValue().trim();

                default:
                    return null;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> allocationKeys() {
        return Arrays.stream(GlExpenseReallocationDtl.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class)).map(Column::name)
                .filter(name -> !List.of(TRANSACTION_POID, "DET_ROW_ID", "COMPANY", "COMPANY_NAME", "TOTAL",
                        "REMARKS", "CREATED_BY", "CREATED_DATE", "LASTMODIFIED_BY", "LASTMODIFIED_DATE").contains(name))
                .toList();
    }

    private void processXlDetails(Long transactionPoid, List<ExpenseReallocationProcessedXlDetail> xlDetails, String userId) {
        List<LogRequestDto<GlExpenseReallocationXlDtl>> logRequests = new ArrayList<>();
        String docId = UserContext.getDocumentId();

        if (xlDetails.stream()
                .anyMatch(val -> "isCreated".equalsIgnoreCase(val.getActionType())))
            xlDtlRepository.deleteByTransactionPoid(transactionPoid);
        // Auto-generate detRowId for new records
        Long maxDetRowId = xlDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        AtomicLong detRowIdSeq = new AtomicLong(maxDetRowId != null ? maxDetRowId + 1 : 1);

        for (ExpenseReallocationProcessedXlDetail xlDetail : xlDetails) {
            String actionType = xlDetail.getActionType() != null ? xlDetail.getActionType().toUpperCase() : "ISCREATED";

            switch (actionType) {
                case "ISCREATED" -> {
                    // Auto-generate detRowId for new records
                    Long newDetRowId = detRowIdSeq.getAndIncrement();
                    xlDetail.setDetRowId(newDetRowId); // Set back to DTO

                    GlExpenseReallocationXlDtl entity = GlExpenseReallocationXlDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(newDetRowId) // Use auto-generated ID
                            .company(xlDetail.getCompany())
                            .companyCode(xlDetail.getCompanyCode())
                            .costCentre(xlDetail.getCostCentre())
                            .percent(xlDetail.getPercent())
                            .remarks(xlDetail.getRemarks())
                            .build();
                    xlDtlRepository.save(entity);
                    String logDetail = String.format("Row Created on Expense Reallocation XL Detail with detRowId: %s", entity.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
                }
                case "ISUPDATED" -> {
                    validateDetRowID(xlDetail.getDetRowId(), "xlDetail");
                    GlExpenseReallocationXlDtl existing = xlDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, xlDetail.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation XL Detail", "detRowId", xlDetail.getDetRowId()));

                    GlExpenseReallocationXlDtl oldEntity = new GlExpenseReallocationXlDtl();
                    BeanUtils.copyProperties(existing, oldEntity);

                    existing.setCompany(xlDetail.getCompany());
                    existing.setCompanyCode(xlDetail.getCompanyCode());
                    existing.setCostCentre(xlDetail.getCostCentre());
                    existing.setPercent(xlDetail.getPercent());
                    existing.setRemarks(xlDetail.getRemarks());
                    xlDtlRepository.save(existing);

                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, xlDetail.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, GlExpenseReallocationXlDtl.class, docId, transactionPoid.toString(), logDetailForUpdate));
                }
                case "ISDELETED" -> {
                    validateDetRowID(xlDetail.getDetRowId(), "xlDetail");
                    xlDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, xlDetail.getDetRowId())
                            .ifPresent(entity -> {
                                xlDtlRepository.delete(entity);
                                loggingService.logDelete(xlDetail, docId, transactionPoid.toString());
                            });
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void validateDetRowID(Long detRowId, String position) {
        Optional.ofNullable(detRowId).orElseThrow(() -> new ValidationException(String.format("Validation Error on %s RowId is Required", position)));
    }

    private List<ExpenseReallocationProcessedXlDetail> mapXlDetailtoEntity(List<ExpenseReallocationXlDetailRequest> request, Long transactionPoid, AtomicLong detRowId) {
        return request.stream()
                .flatMap(val -> {

                            Long detRowValue = detRowId != null ? (Long) detRowId.getAndIncrement() : val.getDetRowId();

                            return val.getCostCenterMap().entrySet().stream()
                                    .map(entry -> {
                                        if (!entry.getKey().equalsIgnoreCase("TOTAL")) {
                                            ExpenseReallocationProcessedXlDetail detail = new ExpenseReallocationProcessedXlDetail();

                                            detail.setCompany(val.getCompany());
                                            detail.setTransactionPoid(transactionPoid);
                                            detail.setCompanyCode(val.getCompanyCode());
                                            detail.setDetRowId(detRowValue);
                                            detail.setCostCentre(entry.getKey());
                                            detail.setPercent(entry.getValue());
                                            detail.setRemarks(val.getRemarks());
                                            detail.setActionType(val.getActionType());

                                            return detail;
                                        }
                                        return null;
                                    }).filter(Objects::nonNull);
                        }
                )
                .toList();
    }

    private List<GlExpenseReallocationXlDtl> mapXlDtoToEnity(List<ExpenseReallocationProcessedXlDetail> dto) {
        return dto.stream().map(val -> GlExpenseReallocationXlDtl.builder()
                .transactionPoid(val.getTransactionPoid())
                .percent(val.getPercent())
                .remarks(val.getRemarks())
                .detRowId(val.getDetRowId())
                .company(val.getCompany())
                .companyCode(val.getCompanyCode())
                .costCentre(val.getCostCentre())
                .build()).toList();
    }

    private LovGetListDto getLov(Long poid, String lovName) {
        return lovDataService.getDetailsByPoidAndLovNameFast(poid, lovName);
    }
}
