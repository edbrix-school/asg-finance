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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

    private static final String TRANSACTION_POID = "TRANSACTION_POID";
    private static final String COMPANYCODE = "Company Code";

    @Override
    @Transactional
    public ExpenseReallocationResponse createExpenseReallocation(CreateExpenseReallocationRequest request,
                                                                 Long groupPoid, Long companyPoid, String userId) {

        log.info("createExpenseReallocation started for groupPoid={} userId={}", groupPoid, userId);

        validateMandatoryFields(request);

        GlExpenseReallocationHdr header = GlExpenseReallocationHdr.builder()
                .transactionDate(DateUtil.getCurrentDateInUserTimeZone()).groupPoid(groupPoid).companyPoid(companyPoid)
                .expenseGroupGl(request.getExpenseGroupGlId()).fromCompany(request.getFromCompanyId())
                .fromDate(request.getFromDate()).toDate(request.getToDate()).allocationType(request.getAllocationType())
                .costPoid(request.getCostPoid()).remarks(request.getRemarks())
                .deleted("N").reportGeneration("N").build();

        final GlExpenseReallocationHdr savedHdr = hdrRepository.save(header);
        final Long hdrPoid = savedHdr.getTransactionPoid();
        Long maxDtlDetRowId = dtlRepository.findMaxDetRowIdByTransactionPoid(hdrPoid);
        AtomicLong dtlDetRowIdSeq = new AtomicLong(maxDtlDetRowId != null ? maxDtlDetRowId + 1 : 1);
        List<GlExpenseReallocationDtl> dtlEntities = request.getDetails().stream()
                .map(dto -> GlExpenseReallocationDtl.builder().transactionPoid(hdrPoid)
                        .detRowId(dtlDetRowIdSeq.getAndIncrement()).company(dto.getCompany())
                        .companyName(dto.getCompanyName()).sh(dto.getSh()).ff(dto.getFf()).ffs(dto.getFfs())
                        .ffp(dto.getFfp()).properties(dto.getProperties()).mta(dto.getMta()).pda(dto.getPda())
                        .admin(dto.getAdmin()).total(dto.getTotal()).remarks(dto.getRemarks()).build())
                .toList();

        dtlRepository.saveAll(dtlEntities);

        // Log child record creation for details
        dtlEntities.forEach(detail -> {
            String logDetail = String.format("Row Created on Expense Reallocation Detail with detRowId: %s", detail.getDetRowId());
            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), hdrPoid.toString(), logDetail);
        });

        Long maxXlDetRowId = xlDtlRepository.findMaxDetRowIdByTransactionPoid(hdrPoid);
        AtomicLong xlDtlDetRowIdSeq = new AtomicLong(maxXlDetRowId != null ? maxXlDetRowId + 1 : 1);

        List<GlExpenseReallocationXlDtl> xlDtlEntities = request.getXlDetails().stream()
                .map(dto -> GlExpenseReallocationXlDtl.builder().transactionPoid(hdrPoid)
                        .detRowId(xlDtlDetRowIdSeq.getAndIncrement()).company(dto.getCompany())
                        .companyCode(dto.getCompanyCode()).costCentre(dto.getCostCentre()).percent(dto.getPercent())
                        .remarks(dto.getRemarks()).build())
                .toList();

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
    public ExpenseReallocationResponse getExpenseReallocationById(Long transactionPoid, Long groupPoid) {
        log.info("getExpenseReallocationById started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

        GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
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
            throw new RuntimeException("Cannot update soft-deleted expense reallocation");
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
        header.setCostPoid(request.getCostPoid());
        header.setRemarks(request.getRemarks());

        GlExpenseReallocationHdr savedHeader = hdrRepository.save(header);

        // Process details based on actionType
        processDetails(transactionPoid, request.getDetails(), userId);

        // Process XL details based on actionType
        if (request.getXlDetails() != null && !request.getXlDetails().isEmpty()) {
            processXlDetails(transactionPoid, request.getXlDetails(), userId);
        }

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
                    throw new ValidationException("Please check Cost center or Company code and upload again");
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
                            throw new ValidationException("Please check Cost center or Company code and upload again");
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
        titleFont.setFontName("Aptos Narrow");
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
        applyBorders(headerStyle);
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
        applyBorders(dataStyle);
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
        List<String> headers = new ArrayList<>();
        headers.add(COMPANYCODE);
        headers.addAll(allocationKeys()); // dynamic allocation columns
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


        Object[][] data = {{"ASG", 10, 15, 5, 5, 10, 0, 0, 0, "", "", ""},
                {"NSA", 15, 8, 0, 0, 0, 0, 0, 0, "", "", ""}, {"DSA", 10, 10, 0, 0, 0, 0, 0, 0, "", "", ""},
                {"FSL", 5, 7, 0, 0, 0, 0, 0, 0, "", "", ""}};

        int rowIdx = 3;

        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowIdx++);
            double total = 0;

            for (int col = 0; col < headers.size(); col++) {
                Cell cell = row.createCell(col);
                String header = headers.get(col);

                if ("TOTAL".equals(header)) {
                    cell.setCellValue(total);
                    cell.setCellStyle(dataStyle);
                } else if (header == null) {
                    cell.setBlank();
                } else {
                    Object value = col < rowData.length ? rowData[col] : null;

                    if (value instanceof Number) {
                        double num = ((Number) value).doubleValue();
                        cell.setCellValue(num); // includes 0
                        total += num;
                    } else if (value != null && !value.toString().isEmpty()) {
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

        Row finalRow = sheet.createRow(rowIdx++);
        Cell cell = finalRow.createCell(headers.size() - 1);
        cell.setCellValue("This should be always 100%");
        cell.setCellStyle(footerStyle);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THICK);
        style.setBorderRight(BorderStyle.THICK);
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

        hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid).orElseThrow(
                () -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid", transactionPoid));

        List<GlExpenseReallocationDtl> details = dtlRepository.findByTransactionPoid(transactionPoid).orElse(null);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (details == null || details.isEmpty()) {
            errors.add("At least one detail line is required");
        } else {
            Set<Long> companySet = new HashSet<>();
            for (GlExpenseReallocationDtl detail : details) {
                if (detail.getCompany() != null) {
                    if (companySet.contains(detail.getCompany())) {
                        errors.add("Duplicate company found: " + detail.getCompany());
                    }
                    companySet.add(detail.getCompany());
                }
            }
        }

        ExpenseReallocationResponse.DetailTotals totals = calculateTotals(transactionPoid);

        ValidateAllocationResponse response = new ValidateAllocationResponse();
        response.setValid(errors.isEmpty());
        response.setErrors(errors);
        response.setWarnings(warnings);

        ValidateAllocationResponse.AllocationTotals allocationTotals = new ValidateAllocationResponse.AllocationTotals();
        allocationTotals.setTotalSh(totals.getTotalSh());
        allocationTotals.setTotalFf(totals.getTotalFf());
        allocationTotals.setTotalFfs(totals.getTotalFfs());
        allocationTotals.setTotalFfp(totals.getTotalFfp());
        allocationTotals.setTotalProperties(totals.getTotalProperties());
        allocationTotals.setTotalMta(totals.getTotalMta());
        allocationTotals.setTotalPda(totals.getTotalPda());
        allocationTotals.setTotalAdmin(totals.getTotalAdmin());
        allocationTotals.setGrandTotal(totals.getGrandTotal());
        response.setTotals(allocationTotals);

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

    private void validateDetailLines(List<ExpenseReallocationDetailRequest> details) {
        Set<Long> companySet = new HashSet<>();
        for (ExpenseReallocationDetailRequest detail : details) {
            if (detail.getCompany() == null) {
                throw new ValidationException("Company is required for all detail lines");
            }
            if (companySet.contains(detail.getCompany())) {
                throw new IllegalStateException("Duplicate company found: " + detail.getCompany());
            }
            companySet.add(detail.getCompany());

            if (detail.getTotal() == null) {
                BigDecimal total = BigDecimal.ZERO;
                if (detail.getSh() != null)
                    total = total.add(detail.getSh());
                if (detail.getFf() != null)
                    total = total.add(detail.getFf());
                if (detail.getFfs() != null)
                    total = total.add(detail.getFfs());
                if (detail.getFfp() != null)
                    total = total.add(detail.getFfp());
                if (detail.getProperties() != null)
                    total = total.add(detail.getProperties());
                if (detail.getMta() != null)
                    total = total.add(detail.getMta());
                if (detail.getPda() != null)
                    total = total.add(detail.getPda());
                if (detail.getAdmin() != null)
                    total = total.add(detail.getAdmin());
                detail.setTotal(total.setScale(3, RoundingMode.HALF_UP));
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
        response.setReportGeneration(header.getReportGeneration());
        response.setAllocationType(header.getAllocationType());
        response.setCreatedBy(header.getCreatedBy());
        response.setCreatedDate(header.getCreatedDate());
        response.setLastmodifiedBy(header.getLastModifiedBy());
        response.setLastmodifiedDate(header.getLastModifiedDate());
        response.setDeleted(header.getDeleted());
        response.setGlPosting(false);

        response.setCompanyPoid(header.getCompanyPoid());
        response.setCompanyName(getCompanyInfo(header.getCompanyPoid()));

        response.setFromCompanyId(header.getFromCompany());
        response.setFromCompanyName(getCompanyInfo(header.getFromCompany()));

        setExpenseGroupInfo(response, header.getExpenseGroupGl());

        List<GlExpenseReallocationDtl> details = dtlRepository.findByTransactionPoid(header.getTransactionPoid()).orElse(new ArrayList<GlExpenseReallocationDtl>());
        response.setDetails(details.stream().map(this::convertDetailToResponse).collect(Collectors.toList()));

        ExpenseReallocationResponse.DetailTotals detailTotals = calculateTotals(header.getTransactionPoid());
        response.setDetailTotals(detailTotals);

        List<GlExpenseReallocationXlDtl> xlDetails = xlDtlRepository.findByTransactionPoid(header.getTransactionPoid()).orElse(new ArrayList<GlExpenseReallocationXlDtl>());
        response.setXlDetails(xlDetails.stream().map(this::convertXlDetailToResponse).collect(Collectors.toList()));

        return response;
    }

    private String getCompanyInfo(Long companyPoid) {
        if (companyPoid == null) return null;

        CompanyDto company = companyServiceClient.findById(companyPoid);
        if (company != null) {
            return company.getCompanyName();
        }
        return null;
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

    private ExpenseReallocationResponse.DetailTotals calculateTotals(Long transactionPoid) {
        ExpenseReallocationResponse.DetailTotals totals = new ExpenseReallocationResponse.DetailTotals();
        totals.setTotalSh(dtlRepository.getTotalShByTransactionPoid(transactionPoid));
        totals.setTotalFf(dtlRepository.getTotalFfByTransactionPoid(transactionPoid));
        totals.setTotalFfs(dtlRepository.getTotalFfsByTransactionPoid(transactionPoid));
        totals.setTotalFfp(dtlRepository.getTotalFfpByTransactionPoid(transactionPoid));
        totals.setTotalProperties(dtlRepository.getTotalPropertiesByTransactionPoid(transactionPoid));
        totals.setTotalMta(dtlRepository.getTotalMtaByTransactionPoid(transactionPoid));
        totals.setTotalPda(dtlRepository.getTotalPdaByTransactionPoid(transactionPoid));
        totals.setTotalAdmin(dtlRepository.getTotalAdminByTransactionPoid(transactionPoid));
        totals.setGrandTotal(dtlRepository.getGrandTotalByTransactionPoid(transactionPoid));
        return totals;
    }

    private ExpenseReallocationDetailResponse convertDetailToResponse(GlExpenseReallocationDtl detail) {
        ExpenseReallocationDetailResponse response = new ExpenseReallocationDetailResponse();
        response.setDetRowId(detail.getDetRowId());
        response.setCompany(detail.getCompany());
        response.setCompanyName(detail.getCompanyName());
        response.setSh(detail.getSh());
        response.setFf(detail.getFf());
        response.setFfs(detail.getFfs());
        response.setFfp(detail.getFfp());
        response.setProperties(detail.getProperties());
        response.setMta(detail.getMta());
        response.setPda(detail.getPda());
        response.setAdmin(detail.getAdmin());
        response.setTotal(detail.getTotal());
        response.setRemarks(detail.getRemarks());
        return response;
    }

    private ExpenseReallocationXlDetailResponse convertXlDetailToResponse(GlExpenseReallocationXlDtl xlDetail) {
        ExpenseReallocationXlDetailResponse response = new ExpenseReallocationXlDetailResponse();
        response.setDetRowId(xlDetail.getDetRowId());
        response.setCompany(xlDetail.getCompany());
        response.setCompanyCode(xlDetail.getCompanyCode());
        response.setCostCentre(xlDetail.getCostCentre());
        response.setPercent(xlDetail.getPercent());
        response.setRemarks(xlDetail.getRemarks());
        String companyName = getCompanyInfo(xlDetail.getCompany());
        response.setCompanyName(companyName);
        return response;
    }

    private BigDecimal getDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;

        try {
            switch (cell.getCellType()) {

                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());

                case STRING:
                    String value = cell.getStringCellValue();
                    if (value == null || value.trim().isEmpty()) {
                        return BigDecimal.ZERO;
                    }
                    return new BigDecimal(value.trim());

                case FORMULA:
                    return BigDecimal.valueOf(cell.getNumericCellValue());

                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
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

    private void processDetails(Long transactionPoid, List<ExpenseReallocationDetailRequest> details, String userId) {
        List<LogRequestDto<GlExpenseReallocationDtl>> logRequests = new ArrayList<>();
        String docId = UserContext.getDocumentId();

        // Auto-generate detRowId for new records
        Long maxDetRowId = dtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        AtomicLong detRowIdSeq = new AtomicLong(maxDetRowId != null ? maxDetRowId + 1 : 1);

        for (ExpenseReallocationDetailRequest detail : details) {
            String actionType = detail.getActionType() != null ? detail.getActionType().toUpperCase() : "ISCREATED";

            switch (actionType) {
                case "ISCREATED" -> {
                    // Auto-generate detRowId for new records
                    Long newDetRowId = detRowIdSeq.getAndIncrement();
                    detail.setDetRowId(newDetRowId); // Set back to DTO

                    GlExpenseReallocationDtl entity = GlExpenseReallocationDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(newDetRowId) // Use auto-generated ID
                            .company(detail.getCompany())
                            .companyName(detail.getCompanyName())
                            .sh(detail.getSh())
                            .ff(detail.getFf())
                            .ffs(detail.getFfs())
                            .ffp(detail.getFfp())
                            .properties(detail.getProperties())
                            .mta(detail.getMta())
                            .pda(detail.getPda())
                            .admin(detail.getAdmin())
                            .total(detail.getTotal())
                            .remarks(detail.getRemarks())
                            .build();
                    dtlRepository.save(entity);
                    String logDetail = String.format("Row Created on Expense Reallocation Detail with detRowId: %s", entity.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
                }
                case "ISUPDATED" -> {
                    GlExpenseReallocationDtl existing = dtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, detail.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation Detail", "detRowId", detail.getDetRowId()));

                    GlExpenseReallocationDtl oldEntity = new GlExpenseReallocationDtl();
                    BeanUtils.copyProperties(existing, oldEntity);

                    existing.setCompany(detail.getCompany());
                    existing.setCompanyName(detail.getCompanyName());
                    existing.setSh(detail.getSh());
                    existing.setFf(detail.getFf());
                    existing.setFfs(detail.getFfs());
                    existing.setFfp(detail.getFfp());
                    existing.setProperties(detail.getProperties());
                    existing.setMta(detail.getMta());
                    existing.setPda(detail.getPda());
                    existing.setAdmin(detail.getAdmin());
                    existing.setTotal(detail.getTotal());
                    existing.setRemarks(detail.getRemarks());
                    dtlRepository.save(existing);

                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detail.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, GlExpenseReallocationDtl.class, docId, transactionPoid.toString(), logDetailForUpdate));
                }
                case "ISDELETED" -> {
                    dtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, detail.getDetRowId())
                            .ifPresent(entity -> {
                                dtlRepository.delete(entity);
                                loggingService.logDelete(detail, docId, transactionPoid.toString());
                            });
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void processXlDetails(Long transactionPoid, List<ExpenseReallocationXlDetailRequest> xlDetails, String userId) {
        List<LogRequestDto<GlExpenseReallocationXlDtl>> logRequests = new ArrayList<>();
        String docId = UserContext.getDocumentId();

        // Auto-generate detRowId for new records
        Long maxDetRowId = xlDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        AtomicLong detRowIdSeq = new AtomicLong(maxDetRowId != null ? maxDetRowId + 1 : 1);

        for (ExpenseReallocationXlDetailRequest xlDetail : xlDetails) {
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
}
