package com.asg.finance.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.dto.*;
import com.asg.finance.entity.PdcBatchExcelUploadTemp;
import com.asg.finance.entity.PdcChqBatchDtlEntity;
import com.asg.finance.entity.PdcChqBatchHdrEntity;
import com.asg.finance.repository.PdcBatchCreationRepository;
import com.asg.finance.repository.PdcBatchExcelUploadTempRepository;
import com.asg.finance.repository.PdcChqBatchDtlRepository;
import com.asg.finance.repository.PdcChqBatchHdrRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.PdcChqBatchService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdcChqBatchServiceImpl implements PdcChqBatchService {

    private final PdcChqBatchHdrRepository hdrRepo;
    private final PdcChqBatchDtlRepository dtlRepo;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final PdcBatchCreationRepository pdcBatchCreationRepository;
    private final PdcBatchExcelUploadTempRepository tempRepo;
    private final LoggingService loggingService;

    @Transactional
    public PdcChqBatchHdrResponseDto createBatch(PdcChqBatchHdrRequestDto dto) {

        validateSrsBusinessRules(dto);

        PdcChqBatchHdrEntity hdr = mapHeaderDtoToEntity(dto);
        hdr = hdrRepo.save(hdr); // trigger generates docRef + poid

        Long transactionPoid = hdr.getTransactionPoid();

        List<PdcChqBatchDtlResponseDto> dtlResponses =
                saveDetailRows(dto.getChequeDetails(), transactionPoid);

        // Log the creation
        String key = transactionPoid.toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, "400-113", key);

        return mapHeaderEntityToResponseDto(hdr, dtlResponses);
    }

    @Override
    @Transactional
    public PdcChqBatchHdrResponseDto updateBatch(Long transactionPoid, PdcChqBatchHdrRequestDto dto) {

        validateSrsBusinessRules(dto);

        PdcChqBatchHdrEntity hdr = hdrRepo.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("PDC Batch not found: " + transactionPoid));

        // Create a copy of the existing entity for logging
        PdcChqBatchHdrEntity oldEntity = new PdcChqBatchHdrEntity();
        BeanUtils.copyProperties(hdr, oldEntity);

        hdr.setTransactionDate(dto.getTransactionDate());
        hdr.setGroupPoid(dto.getGroupPoid());
        hdr.setCompanyPoid(dto.getCompanyPoid());
        hdr.setPayGlPoid(dto.getPayGlPoid());
        hdr.setPayingTo(dto.getPayingTo());
        hdr.setPayingType(dto.getPayingType());
        hdr.setDivisionCode(dto.getDivisionCode());
        hdr.setBankPoid(dto.getBankPoid());
        hdr.setChqStartNo(dto.getChqStartNo());
        hdr.setChqStartDate(dto.getChqStartDate());
        hdr.setChqAmount(dto.getChqAmount());
        hdr.setNoOfChqs(dto.getNoOfChqs());
        hdr.setTotalAmount(dto.getChqAmount() * dto.getNoOfChqs());
        hdr.setNarration(dto.getNarration());
        hdr.setBillType(dto.getBillType());
        hdr.setBillRef(dto.getBillRef());
        hdr.setCostGroup(dto.getCostGroup());
        hdr.setCostPoid(dto.getCostPoid());
        hdr.setPrePrinted(dto.getPrePrinted());
        hdr.setAccountPayee(dto.getAccountPayee());
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());
        hdrRepo.save(hdr);

        dtlRepo.deleteByTransactionPoid(transactionPoid);
        List<PdcChqBatchDtlResponseDto> dtls =
                saveDetailRows(dto.getChequeDetails(), transactionPoid);

        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, hdr, PdcChqBatchHdrEntity.class, 
                "400-113", key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return mapHeaderEntityToResponseDto(hdr, dtls);
    }

    @Override
    @Transactional()
    public PdcChqBatchHdrResponseDto findById(Long transactionPoid) {

        PdcChqBatchHdrEntity hdr = hdrRepo.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("PDC Batch not found: " + transactionPoid));

        List<PdcChqBatchDtlEntity> dtlEntities =
                dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(transactionPoid);

        List<PdcChqBatchDtlResponseDto> dtls = dtlEntities.stream()
                .map(this::mapDtlEntityToResponseDto)
                .collect(Collectors.toList());

        return mapHeaderEntityToResponseDto(hdr, dtls);
    }

    @Override
    @Transactional
    public void deletePdcBatch(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        PdcChqBatchHdrEntity hdr = hdrRepo.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("PDC Batch not found: " + transactionPoid));
        
        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_PDC_CHQ_BATCH_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                hdr.getTransactionDate().toLocalDate()
        );
    }

    private void validateSrsBusinessRules(PdcChqBatchHdrRequestDto dto) {

        if ("Y".equalsIgnoreCase(dto.getPrePrinted())) {
            if (dto.getChqStartNo() == null || dto.getChqStartNo().trim().length() != 6) {
                throw new IllegalArgumentException("Cheque Start No must be 6 digits when Manual Cheque is selected.");
            }
        }

        double totalDr = dto.getChequeDetails().stream()
                .mapToDouble(d ->
                        (d.getDrAmt1() != null ? d.getDrAmt1() : 0.0)
                                + (d.getDrAmt2() != null ? d.getDrAmt2() : 0.0)
                                + (d.getDrAmt3() != null ? d.getDrAmt3() : 0.0)
                ).sum();

        double totalCr = dto.getChequeDetails().stream()
                .mapToDouble(d -> d.getCrAmt() != null ? d.getCrAmt() : 0.0)
                .sum();

        if (Math.abs(totalDr - totalCr) > 0.001) {
            throw new IllegalArgumentException("Debit and Credit total must be equal.");
        }
    }

    private PdcChqBatchHdrEntity mapHeaderDtoToEntity(PdcChqBatchHdrRequestDto dto) {

        return PdcChqBatchHdrEntity.builder()
                .transactionDate(dto.getTransactionDate())
                .groupPoid(dto.getGroupPoid())
                .companyPoid(dto.getCompanyPoid())
                .payGlPoid(dto.getPayGlPoid())
                .payingTo(dto.getPayingTo())
                .payingType(dto.getPayingType())
                .divisionCode(dto.getDivisionCode())
                .bankPoid(dto.getBankPoid())
                .chqStartNo(dto.getChqStartNo())
                .chqStartDate(dto.getChqStartDate())
                .chqAmount(dto.getChqAmount())
                .noOfChqs(dto.getNoOfChqs())
                .totalAmount(dto.getChqAmount() * dto.getNoOfChqs())
                .narration(dto.getNarration())
                .billType(dto.getBillType())
                .billRef(dto.getBillRef())
                .costGroup(dto.getCostGroup())
                .costPoid(dto.getCostPoid())
                .prePrinted(dto.getPrePrinted())
                .confidentialRemarks(dto.getConfidentialRemarks())
                .accountPayee(dto.getAccountPayee())
                .deleted("N")
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .build();
    }

    private List<PdcChqBatchDtlResponseDto> saveDetailRows(
            List<PdcChqBatchDtlRequestDto> dtos,
            Long transactionPoid) {

        String docId = "400-113";
        List<PdcChqBatchDtlEntity> existingList = dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(transactionPoid);
        Map<Long, PdcChqBatchDtlEntity> existingMap = existingList.stream()
                .collect(Collectors.toMap(PdcChqBatchDtlEntity::getDetRowId, d -> d));
        
        Long[] maxDetRowId = {existingList.stream()
                .map(PdcChqBatchDtlEntity::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L)};

        List<PdcChqBatchDtlResponseDto> responseList = new ArrayList<>();
        List<LogRequestDto<PdcChqBatchDtlEntity>> logRequests = new ArrayList<>();

        for (PdcChqBatchDtlRequestDto dto : dtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : "ISCREATED";
            
            switch (action) {
                case "ISCREATED":
                    Long detRowId = ++maxDetRowId[0];
                    PdcChqBatchDtlEntity newEntity = mapDtlDtoToEntity(dto, transactionPoid, detRowId);
                    newEntity = dtlRepo.save(newEntity);
                    responseList.add(mapDtlEntityToResponseDto(newEntity));
                    String logDetail = String.format("Row Created on PDC Cheque Batch Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
                    break;
                    
                case "ISUPDATED":
                    PdcChqBatchDtlEntity existing = existingMap.get(dto.getDetRowId());
                    if (existing == null) {
                        throw new ResourceNotFoundException("PDC Batch Detail", "detRowId", dto.getDetRowId());
                    }
                    PdcChqBatchDtlEntity oldEntity = new PdcChqBatchDtlEntity();
                    BeanUtils.copyProperties(existing, oldEntity);
                    updateDetailEntity(existing, dto);
                    existing = dtlRepo.save(existing);
                    responseList.add(mapDtlEntityToResponseDto(existing));
                    String logDetailUpdate = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", transactionPoid, dto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, PdcChqBatchDtlEntity.class, docId, transactionPoid.toString(), logDetailUpdate));
                    break;
                    
                case "ISDELETED":
                    if (dto.getDetRowId() != null) {
                        dtlRepo.deleteByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId());
                        loggingService.logDelete(dto, docId, transactionPoid.toString());
                    }
                    break;
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }

        return responseList;
    }

    private void updateDetailEntity(PdcChqBatchDtlEntity entity, PdcChqBatchDtlRequestDto dto) {
        entity.setPdcChqDate(dto.getPdcChqDate());
        entity.setChqNumber(dto.getChqNumber());
        entity.setChqAmount(dto.getChqAmount());
        entity.setRemarks(dto.getRemarks());
        entity.setBankPaymentPoid(dto.getBankPaymentPoid());
        entity.setBankPaymentRef(dto.getBankPaymentRef());
        entity.setNarration(dto.getNarration());
        entity.setBillRef(dto.getBillRef());
        entity.setCostPoid(dto.getCostPoid());
        entity.setDrGlPoid1(dto.getDrGlPoid1());
        entity.setDrAmt1(dto.getDrAmt1());
        entity.setDrGlPoid2(dto.getDrGlPoid2());
        entity.setDrAmt2(dto.getDrAmt2());
        entity.setDrGlPoid3(dto.getDrGlPoid3());
        entity.setDrAmt3(dto.getDrAmt3());
        entity.setCrGlPoid(dto.getCrGlPoid());
        entity.setCrAmt(dto.getCrAmt());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private PdcChqBatchDtlEntity mapDtlDtoToEntity(
            PdcChqBatchDtlRequestDto dto,
            Long transactionPoid,
            Long detRowId) {

        return PdcChqBatchDtlEntity.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .pdcChqDate(dto.getPdcChqDate())
                .chqNumber(dto.getChqNumber())
                .chqAmount(dto.getChqAmount())
                .remarks(dto.getRemarks())
                .bankPaymentPoid(dto.getBankPaymentPoid())
                .bankPaymentRef(dto.getBankPaymentRef())
                .narration(dto.getNarration())
                .billRef(dto.getBillRef())
                .costPoid(dto.getCostPoid())
                .drGlPoid1(dto.getDrGlPoid1())
                .drAmt1(dto.getDrAmt1())
                .drGlPoid2(dto.getDrGlPoid2())
                .drAmt2(dto.getDrAmt2())
                .drGlPoid3(dto.getDrGlPoid3())
                .drAmt3(dto.getDrAmt3())
                .crGlPoid(dto.getCrGlPoid())
                .crAmt(dto.getCrAmt())
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(LocalDateTime.now())
                .build();
    }

    private PdcChqBatchDtlResponseDto mapDtlEntityToResponseDto(PdcChqBatchDtlEntity e) {

        return PdcChqBatchDtlResponseDto.builder()
                .transactionPoid(e.getTransactionPoid())
                .detRowId(e.getDetRowId())
                .pdcChqDate(e.getPdcChqDate())
                .chqNumber(e.getChqNumber())
                .chqAmount(e.getChqAmount())
                .remarks(e.getRemarks())
                .bankPaymentPoid(e.getBankPaymentPoid())
                .bankPaymentRef(e.getBankPaymentRef())
                .narration(e.getNarration())
                .billRef(e.getBillRef())
                .costPoid(e.getCostPoid())
                .drGlPoid1(e.getDrGlPoid1())
                .drAmt1(e.getDrAmt1())
                .drGlPoid2(e.getDrGlPoid2())
                .drAmt2(e.getDrAmt2())
                .drGlPoid3(e.getDrGlPoid3())
                .drAmt3(e.getDrAmt3())
                .crGlPoid(e.getCrGlPoid())
                .crAmt(e.getCrAmt())
                .createdBy(e.getCreatedBy())
                .createdDate(e.getCreatedDate())
                .build();
    }

    private PdcChqBatchHdrResponseDto mapHeaderEntityToResponseDto(
            PdcChqBatchHdrEntity hdr,
            List<PdcChqBatchDtlResponseDto> dtlList) {

        return PdcChqBatchHdrResponseDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .transactionDate(hdr.getTransactionDate())
                .docRef(hdr.getDocRef())
                .bankPoid(hdr.getBankPoid())
                .payGlPoid(hdr.getPayGlPoid())
                .payingTo(hdr.getPayingTo())
                .narration(hdr.getNarration())
                .prePrinted(hdr.getPrePrinted())
                .accountPayee(hdr.getAccountPayee())
                .chqStartNo(hdr.getChqStartNo())
                .chqStartDate(hdr.getChqStartDate())
                .billType(hdr.getBillType())
                .chqAmount(hdr.getChqAmount())
                .noOfChqs(hdr.getNoOfChqs())
                .totalAmount(hdr.getTotalAmount())
                .confidentialRemarks(hdr.getConfidentialRemarks())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .deleted(hdr.getDeleted())
                .billRef(hdr.getBillRef())
                .chequeDetails(dtlList)
                .createdDate(hdr.getCreatedDate())
                .createdBy(hdr.getCreatedBy())
                .build();
    }

    @Override
    public Map<String, Object> listPdcBatchCreation(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "REF_TYPE",
                "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    public PayGlBreakupCheckResponseDto validatePayGl(Long payGlPoid) {

        Long group = UserContext.getGroupPoid();
        Long company = UserContext.getCompanyPoid();
        Long user = UserContext.getUserPoid();

        return pdcBatchCreationRepository.checkPayGlBreakup(group, company, user, payGlPoid);
    }

    public PdcBatchCreationProcResponse processBatch(PdcBatchCreationProcRequest request) {
        boolean exists = hdrRepo.existsByTransactionPoid(request.getTransactionPoid());
        if (!exists) {
            throw new ResourceNotFoundException(
                    "PDC Batch not found for: ",
                    "TransactionPoid",
                    request.getTransactionPoid()
            );
        }
        return pdcBatchCreationRepository.runBatchCreation(request);
    }

    public PdcBatchCreationProcResponse runBankPostingProcedure(PdcBankPostingProcRequest request) {
        boolean exists = hdrRepo.existsByTransactionPoid(request.getTransactionPoid());
        if (!exists) {
            throw new ResourceNotFoundException(
                    "PDC Batch not found for: ",
                    "TransactionPoid",
                    request.getTransactionPoid()
            );
        }
        return pdcBatchCreationRepository.runBankPosting(request);
    }

    public PdcBatchCreationProcResponse createBatchFromExcel(PdcBatchCreationExcelProcRequest request) {
        boolean exists = hdrRepo.existsByTransactionPoid(request.getTransactionPoid());
        if (!exists) {
            throw new ResourceNotFoundException(
                    "PDC Batch not found for: ",
                     "TransactionPoid",
                    request.getTransactionPoid()
            );
        }

        return pdcBatchCreationRepository.runBatchCreationXL(request);
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    public String uploadExcel(MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            return "File is empty";
        }

        List<PdcBatchExcelUploadTemp> tempList = new ArrayList<>();
        Set<String> chequeNumbersInExcel = new HashSet<>();

        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet sheet = workbook.getSheetAt(0);

        int rowIndex = 0;

        for (Row row : sheet) {

            // Skip header row
            if (rowIndex == 0) {
                rowIndex++;
                continue;
            }

            String chequeNumber = getString(row.getCell(1));

            if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
                throw new RuntimeException("Cheque number is missing at row " + (rowIndex + 1));
            }

            if (!chequeNumbersInExcel.add(chequeNumber)) {
                throw new RuntimeException("Duplicate cheque number in Excel: " + chequeNumber);
            }

            PdcBatchExcelUploadTemp temp = new PdcBatchExcelUploadTemp();

            temp.setChequeDate(getString(row.getCell(0)));
            temp.setChequeNumber(getString(row.getCell(1)));
            temp.setChequeAmount(getString(row.getCell(2)));
            temp.setDrAmt(getString(row.getCell(3)));
            temp.setDrAmt2(getString(row.getCell(4)));

            tempList.add(temp);
            rowIndex++;
        }

        workbook.close();

        tempRepo.deleteAll(); // clear old data
        tempRepo.saveAll(tempList);

        return "Excel uploaded successfully";
    }

    private String getString(Cell cell) {
        return cell == null ? null : cell.toString().trim();
    }

}
