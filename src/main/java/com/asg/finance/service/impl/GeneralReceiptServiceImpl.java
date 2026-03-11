package com.asg.finance.service.impl;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.*;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.repository.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.service.GeneralReceiptService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralReceiptServiceImpl implements GeneralReceiptService {

    private static final Long DEFAULT_GROUP_POID = 1L;
    private static final String DOC_ID = "300-105";
    
    private final ArGenReceiptHdrRepository receiptHdrRepository;
    private final ArGenReceiptPymtDetailsRepository pymtDetailsRepository;
    private final ArGenReceiptBillDtlRepository billDtlRepository;
    private final ArGenReceiptChargesDtlRepository chargesDtlRepository;
    private final ArGenReceiptAdvanceDtlRepository advanceDtlRepository;
    private final GLMastersRepository glMastersRepository;
    private final ParameterServiceClient parameterServiceClient;
    private final GeneralReceiptProcedureRepository procedureRepository;
    private final EntityManager entityManager;
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    
    private final DocumentDeleteService documentDeleteService;
    private final ApprovalService approvalService;

    
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public GeneralReceiptResponse createGeneralReceipt(GeneralReceiptRequest request) {
        // Step 1: Get self-reference to enable proxy interception for @Transactional
        GeneralReceiptServiceImpl self = applicationContext.getBean(GeneralReceiptServiceImpl.class);
        
        // Step 2: Save receipt data in a new transaction that commits immediately
        ArGenReceiptHdr header = self.saveReceiptData(request);
        
        log.info("Receipt transaction committed. Data is now visible in database.");
        
        // Step 3: Call GL posting procedure OUTSIDE any transaction
        // The procedure can now see the committed data
        return completeReceiptCreation(header);
    }
    
    /**
     * Save receipt data in a NEW transaction that commits immediately when method completes.
     * REQUIRES_NEW ensures this transaction is independent of any calling context.
     * 
     * This method is public to allow Spring AOP to intercept it and create a new transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArGenReceiptHdr saveReceiptData(GeneralReceiptRequest request) {
        log.info("Creating general receipt for company: {}", request.getHeader().getCompanyPoid());

        // 1. Validate request
        validateGeneralReceiptRequest(request);

        // 2. Get current user
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        // 3. Create and save header entity
        ArGenReceiptHdr header = buildHeaderEntity(request.getHeader(), currentUser, now);

        try {
            header = receiptHdrRepository.save(header);
            entityManager.flush();
            entityManager.refresh(header); // Get trigger-generated DOC_REF
            
            log.info("Created receipt header with DOC_REF: {} and TRANSACTION_POID: {}", 
                    header.getDocRef(), header.getTransactionPoid());
            
            // Check for duplicates after creation (in case of race condition)
            if (header.getDocRef() != null) {
                Long duplicateCount = receiptHdrRepository.countByDocRef(header.getDocRef());
                if (duplicateCount > 1) {
                    log.error("Duplicate DOC_REF detected after creation: {} (count: {})", header.getDocRef(), duplicateCount);
                    // This is a critical issue - log it but don't fail the transaction
                    // The database trigger should prevent this, but if it happens, we continue
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to save receipt header: {}", e.getMessage(), e);
            
            // Handle specific database trigger errors
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("ORA-20001")) {
                    throw new ValidationException("Changes allowed only within current Financial Period.");
                } else if (errorMessage.contains("ORA-20002")) {
                    throw new ValidationException("Transaction date is outside the current transaction period.");
                } else if (errorMessage.contains("ORA-00001") || errorMessage.contains("unique constraint")) {
                    throw new ValidationException("Duplicate receipt reference. Please try again.");
                } else if (errorMessage.contains("Query did not return a unique result")) {
                    throw new ValidationException("Duplicate receipt detected. Please contact system administrator.");
                }
            }
            throw new ValidationException("Failed to save receipt: " + e.getMessage());
        }

        // 6. Save details (payment, bill, charge, advance)
        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            savePaymentDetails(header, request.getPayments(), currentUser, now, true);
        }
        if (request.getBills() != null && !request.getBills().isEmpty()) {
            saveBillDetails(header, request.getBills(), currentUser, now, true);
        }
        if ("Y".equals(request.getHeader().getExtraCharges()) && 
            request.getExtraCharges() != null && !request.getExtraCharges().isEmpty()) {
            saveChargeDetails(header, request.getExtraCharges(), currentUser, now, true);
        }
        if (request.getAdvances() != null && !request.getAdvances().isEmpty()) {
            saveAdvanceDetails(header, request.getAdvances(), currentUser, now, true);
        }

        // 7. Flush all changes to commit the receipt data
        entityManager.flush();
        
        // Log the creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), header.getTransactionPoid().toString());
        
        log.info("Successfully created general receipt: {}", header.getDocRef());
        
        // Return the header so GL posting can be done in a separate transaction
        return header;
    }
    
    /**
     * Complete receipt creation by calling GL posting/approval in a separate transaction
     * This ensures receipt data is committed even if GL posting fails
     * 
     * NOTE: This method does NOT have @Transactional so that it calls the procedure
     * OUTSIDE of any transaction, allowing the procedure to see the committed data
     */
    public GeneralReceiptResponse completeReceiptCreation(ArGenReceiptHdr header) {
        GeneralReceiptResponse response = getGeneralReceiptByTransactionPoid(header.getTransactionPoid());
        response.setMessage("General Receipt created successfully");
        try {
            log.info("Receipt data committed. Now calling GL posting procedure...");
            // Process GL posting or approval - called OUTSIDE any transaction
            // so the Oracle procedure can see the committed data
            // processGLPostingOrApproval(header);
        } catch (Exception e) {
            log.error("GL posting failed for receipt {}, but receipt data is saved", header.getDocRef(), e);
        }
        return response;
    }

    /**
     * Enrich receipt data by calling additional procedures based on conditions
     * Procedures called:
     * 1. PROC_GEN_REC_BILLWISE_PENDING → fetch pending bills (optional, for selection purposes)
     * 2. PROC_AR_GEN_RCP_FETCH_CUST_AC → fetch customer bank details for cheque mode
     * 3. PROC_AR_GEN_RCPT_FTCH_GLBAL → load billwise balance details when Ref Type = AGAINST
     */
    private void enrichReceiptData(ArGenReceiptHdr header) {
        try {
            // 1. Fetch pending bills if needed (for GENERAL receipts)
            // This is typically used for selection purposes when creating receipts
            // Can be called if we have GL POID from bills
            if (header.getBillDetails() != null && !header.getBillDetails().isEmpty()) {
                // Get GL POID from first bill (typically all bills have same GL for a receipt)
                Long glPoid = header.getBillDetails().get(0).getGlPoid();
                if (glPoid != null) {
                    try {
                        LocalDate asOnDate = header.getTransactionDate() != null 
                                ? header.getTransactionDate() 
                                : LocalDate.now();
                        
                        List<Object[]> pendingBills = procedureRepository.fetchPendingBills(
                                DEFAULT_GROUP_POID, header.getCompanyPoid(), glPoid, asOnDate);
                        log.debug("Fetched {} pending bills for GL: {}", pendingBills.size(), glPoid);
                        // TODO: Map pending bills data if needed in response
                    } catch (Exception e) {
                        log.warn("Failed to fetch pending bills: {}", e.getMessage());
                    }
                }
            }

            // 2. Fetch customer bank details for cheque mode payments
            // Procedure uses RCVD_OTH_POID from header to find last cheque payment details
            if (header.getPaymentDetails() != null && !header.getPaymentDetails().isEmpty()) {
                boolean hasChequePayment = header.getPaymentDetails().stream()
                        .anyMatch(p -> "CHEQUE".equalsIgnoreCase(p.getPymtType()));
                
                if (hasChequePayment && header.getRcvdOthPoid() != null) {
                    try {
                        List<Object[]> bankDetails = procedureRepository.fetchCustomerBankDetails(
                                header.getRcvdOthPoid());
                        log.debug("Fetched {} customer bank details for cheque payment (RCVD_OTH_POID: {})", 
                                bankDetails.size(), header.getRcvdOthPoid());
                        // TODO: Map bank details to payment if needed in response
                        // Returns: ACCOUNT_NO, ACCOUNT_NAME, BANK_POID
                    } catch (Exception e) {
                        log.warn("Failed to fetch customer bank details for RCVD_OTH_POID {}: {}", 
                                header.getRcvdOthPoid(), e.getMessage());
                    }
                }
            }

            // 3. Load billwise balance details when Ref Type = AGAINST
            // Called for each bill to get balance, account type, due date, etc.
            if ("AGAINST".equalsIgnoreCase(header.getRefType()) 
                    && header.getBillDetails() != null && !header.getBillDetails().isEmpty()) {
                for (ArGenReceiptBillDtl bill : header.getBillDetails()) {
                    if (bill.getGlPoid() != null && bill.getBillRefno() != null) {
                        try {
                            List<Object[]> billwiseDetails = procedureRepository.fetchBillwiseDetails(
                                    bill.getGlPoid(), bill.getBillRefno());
                            if (!billwiseDetails.isEmpty()) {
                                log.debug("Fetched billwise details for bill {}: {}", 
                                        bill.getBillRefno(), billwiseDetails.size());
                                // TODO: Map billwise details to bill if needed in response
                                // Returns: BALANCE, ACTYPE (CREDIT/DEBIT), BILL_DUE_DATE, CHECK_ALL, REMARKS
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch billwise details for bill {}: {}", 
                                    bill.getBillRefno(), e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error enriching receipt data", e);
            // Don't fail the request if enrichment fails - these are enrichment calls
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GeneralReceiptResponse getGeneralReceiptByTransactionPoid(Long transactionPoid) {
        log.info("Fetching general receipt by TransactionPoid: {}", transactionPoid);

        try {
            // Fetch receipt header
            ArGenReceiptHdr header = receiptHdrRepository.findByTransactionPoidWithDetails(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("General Receipt", "transactionPoid", transactionPoid));

            // Fetch child collections separately to avoid MultipleBagFetchException
            loadReceiptDetails(header);

            // Call additional procedures based on conditions to enrich data
            enrichReceiptData(header);

            return buildResponse(header);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Query did not return a unique result")) {
                log.error("Duplicate records found for TRANSACTION_POID: {}. This should not happen as it's a primary key.", transactionPoid);
                throw new ValidationException("Data integrity issue detected. Please contact system administrator.");
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GeneralReceiptResponse getGeneralReceiptByDocRef(String docRef) {
        log.info("Fetching general receipt by DocRef: {}", docRef);

        // Check for duplicates first
        Long duplicateCount = receiptHdrRepository.countByDocRef(docRef);
        if (duplicateCount > 1) {
            log.warn("Found {} duplicate receipts with DOC_REF: {}. Using the latest one.", duplicateCount, docRef);
            // Get all duplicates and use the latest one
            List<ArGenReceiptHdr> duplicates = receiptHdrRepository.findAllByDocRef(docRef);
            if (!duplicates.isEmpty()) {
                ArGenReceiptHdr header = duplicates.get(0); // Already ordered by TRANSACTION_POID DESC
                loadReceiptDetails(header);
                enrichReceiptData(header);
                return buildResponse(header);
            }
        }

        // Fetch receipt header
        ArGenReceiptHdr header = receiptHdrRepository.findByDocRefWithDetails(docRef)
                .orElseThrow(() -> new ResourceNotFoundException("General Receipt", "docRef", docRef));

        // Fetch child collections separately to avoid MultipleBagFetchException
        loadReceiptDetails(header);

        // Call additional procedures based on conditions to enrich data
        enrichReceiptData(header);

        return buildResponse(header);
    }

    /**
     * Load all child collections separately to avoid MultipleBagFetchException
     * Fetches: payments, bills, charges, advances
     */
    private void loadReceiptDetails(ArGenReceiptHdr header) {
        Long transactionPoid = header.getTransactionPoid();
        
        // Fetch each collection separately using repository methods
        List<ArGenReceiptPymtDetails> payments = pymtDetailsRepository.findByReceiptHdr_TransactionPoid(transactionPoid);
        List<ArGenReceiptBillDtl> bills = billDtlRepository.findByReceiptHdr_TransactionPoid(transactionPoid);
        List<ArGenReceiptChargesDtl> charges = chargesDtlRepository.findByReceiptHdr_TransactionPoid(transactionPoid);
        List<ArGenReceiptAdvanceDtl> advances = advanceDtlRepository.findByReceiptHdr_TransactionPoid(transactionPoid);
        
        // Set collections on header entity
        header.setPaymentDetails(payments);
        header.setBillDetails(bills);
        header.setChargesDetails(charges);
        header.setAdvanceDetails(advances);
    }

    /**
     * Helper methods to extract values from row Map
     */
    private String getStringValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;
        return value.toString();
    }

    private Long getLongValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

 /*   private LocalDate getLocalDateValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof Date) return ((Date) value).toLocalDate();
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime().toLocalDate();
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }*/

  /*  private LocalDateTime getLocalDateTimeValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate().atStartOfDay();
        if (value instanceof Date) return ((Date) value).toLocalDate().atStartOfDay();
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }*/

    @Override
    public GeneralReceiptResponse updateGeneralReceipt(Long transactionPoid, GeneralReceiptRequest request) {
        GeneralReceiptServiceImpl self = applicationContext.getBean(GeneralReceiptServiceImpl.class);
        self.updateReceiptData(transactionPoid, request);
        GeneralReceiptResponse response = getGeneralReceiptByTransactionPoid(transactionPoid);
        response.setMessage("Receipt updated successfully");
        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateReceiptData(Long transactionPoid, GeneralReceiptRequest request) {
        log.info("Updating general receipt: {}", transactionPoid);

        ArGenReceiptHdr header = receiptHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("General Receipt", "transactionPoid", transactionPoid));

        // Create a copy of the old entity for logging
        ArGenReceiptHdr oldEntity = new ArGenReceiptHdr();
        BeanUtils.copyProperties(header, oldEntity);

        if ("Y".equals(header.getVerified())) {
            throw new ValidationException("Cannot update receipt that has been verified/posted to GL");
        }

        final String approvalStatus = approvalService.getApprovalStatus(UserContext.getDocumentId(), header.getTransactionPoid());

        if ("APPROVED".equals(approvalStatus)) {
            throw new ValidationException("Cannot update receipt that has been approved");
        }

        validateGeneralReceiptRequest(request);
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        updateHeaderEntity(header, request.getHeader(), currentUser, now);
        receiptHdrRepository.save(header);

        // Update child records using actionType pattern
        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            updatePaymentDetails(request.getPayments(), transactionPoid);
        }
        if (request.getBills() != null && !request.getBills().isEmpty()) {
            updateBillDetails(request.getBills(), transactionPoid);
        }
        if ("Y".equals(request.getHeader().getExtraCharges()) && 
            request.getExtraCharges() != null && !request.getExtraCharges().isEmpty()) {
            updateChargeDetails(request.getExtraCharges(), transactionPoid);
        }
        if (request.getAdvances() != null && !request.getAdvances().isEmpty()) {
            updateAdvanceDetails(request.getAdvances(), transactionPoid);
        }

        entityManager.flush();
        
        // Log the update
        loggingService.logChanges(oldEntity, header, ArGenReceiptHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
    }

    private void updatePaymentDetails(List<GeneralReceiptPaymentDto> payments, Long transactionPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<ArGenReceiptPymtDetails> toSave = new ArrayList<>();
        List<ArGenReceiptPymtDetails> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArGenReceiptPymtDetails>> logRequests = new ArrayList<>();
        
        // Delete first
        for (GeneralReceiptPaymentDto payment : payments) {
            String action = payment.getActionType() != null ? payment.getActionType().toUpperCase() : "NOCHANGE";
            if ("ISDELETED".equals(action) && payment.getDetRowId() != null) {
                toDelete.add(payment.getDetRowId());
                loggingService.logDelete(payment, docId, docKeyPoid);
            }
        }
        
        if (!toDelete.isEmpty()) {
            pymtDetailsRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
            entityManager.flush();
        }
        
        Long maxDetRowId = pymtDetailsRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (maxDetRowId == null) maxDetRowId = 0L;
        
        for (GeneralReceiptPaymentDto payment : payments) {
            String action = payment.getActionType() != null ? payment.getActionType().toUpperCase() : "NOCHANGE";
            if ("ISDELETED".equals(action)) continue;
            
            if ("ISCREATED".equals(action)) {
                toSave.add(ArGenReceiptPymtDetails.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId(++maxDetRowId)
                        .pymtType(payment.getType())
                        .amount(payment.getAmount())
                        .chqCardno(payment.getChequeNo())
                        .chqDate(payment.getChequeDate())
                        .bankPoid(payment.getBankPoid())
                        .accountPoid(payment.getAccountPoid())
                        .accountName(payment.getAccountName())
                        .accountNo(payment.getAccountNumber())
                        .ttBankPoid(payment.getTtBankPoid())
                        .ttRef(payment.getTtRef())
                        .creditCardRef(payment.getCreditCardRef())
                        .cardType(payment.getCardType())
                        .cardPoid(payment.getCardPoid())
                        .build());
            } else if ("ISUPDATED".equals(action)) {
                Optional<ArGenReceiptPymtDetails> existingPaymentOpt = pymtDetailsRepository
                        .findByTransactionPoidAndDetRowId(transactionPoid, payment.getDetRowId());
                
                if (existingPaymentOpt.isPresent()) {
                    ArGenReceiptPymtDetails existingPayment = existingPaymentOpt.get();
                    ArGenReceiptPymtDetails oldPayment = new ArGenReceiptPymtDetails();
                    BeanUtils.copyProperties(existingPayment, oldPayment);
                    
                    existingPayment.setPymtType(payment.getType());
                    existingPayment.setAmount(payment.getAmount());
                    existingPayment.setChqCardno(payment.getChequeNo());
                    existingPayment.setChqDate(payment.getChequeDate());
                    existingPayment.setBankPoid(payment.getBankPoid());
                    existingPayment.setAccountPoid(payment.getAccountPoid());
                    existingPayment.setAccountName(payment.getAccountName());
                    existingPayment.setAccountNo(payment.getAccountNumber());
                    existingPayment.setTtBankPoid(payment.getTtBankPoid());
                    existingPayment.setTtRef(payment.getTtRef());
                    existingPayment.setCreditCardRef(payment.getCreditCardRef());
                    existingPayment.setCardType(payment.getCardType());
                    existingPayment.setCardPoid(payment.getCardPoid());
                    existingPayment.setLastModifiedBy(currentUser);
                    existingPayment.setLastModifiedDate(now);
                    toUpdate.add(existingPayment);
                    
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, payment.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldPayment, existingPayment, ArGenReceiptPymtDetails.class, docId, docKeyPoid, logDetailForUpdate));
                } else {
                    toSave.add(ArGenReceiptPymtDetails.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(++maxDetRowId)
                            .pymtType(payment.getType())
                            .amount(payment.getAmount())
                            .chqCardno(payment.getChequeNo())
                            .chqDate(payment.getChequeDate())
                            .bankPoid(payment.getBankPoid())
                            .accountPoid(payment.getAccountPoid())
                            .accountName(payment.getAccountName())
                            .accountNo(payment.getAccountNumber())
                            .ttBankPoid(payment.getTtBankPoid())
                            .ttRef(payment.getTtRef())
                            .creditCardRef(payment.getCreditCardRef())
                            .cardType(payment.getCardType())
                            .cardPoid(payment.getCardPoid())
                            .build());
                }
            }
        }
        
        if (!toSave.isEmpty()) {
            pymtDetailsRepository.saveAll(toSave);
            toSave.forEach(e -> {
                String logDetail = String.format("Row Created on Payment with detRowId: %s", e.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            });
        }

        if (!toUpdate.isEmpty()) {
            pymtDetailsRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
    }

    private void updateBillDetails(List<GeneralReceiptBillDto> bills, Long transactionPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<ArGenReceiptBillDtl> toSave = new ArrayList<>();
        List<ArGenReceiptBillDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArGenReceiptBillDtl>> logRequests = new ArrayList<>();
        
        ArGenReceiptHdr header = receiptHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("General Receipt", "transactionPoid", transactionPoid));
        
        for (GeneralReceiptBillDto bill : bills) {
            String action = bill.getActionType() != null ? bill.getActionType().toUpperCase() : "ISCREATED";
            switch (action) {
                case "ISCREATED":
                    Long glPoid = bill.getGlPoid() != null ? bill.getGlPoid() : header.getRcvdOthPoid();
                    toSave.add(ArGenReceiptBillDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(bill.getDetRowId())
                            .glPoid(glPoid)
                            .billRefType(bill.getBillRefType() != null ? bill.getBillRefType() : header.getRefType())
                            .billRefno(bill.getBillReference())
                            .billDueDate(bill.getBillDueDate())
                            .description(bill.getDescription())
                            .amount(bill.getAmount())
                            .crDrType(bill.getDrCr())
                            .glCompanyPoid(bill.getGlCompanyPoid() != null ? bill.getGlCompanyPoid() : header.getCompanyPoid())
                            .remarks(bill.getRemarks())
                            .checkall("N")
                            .build());
                    break;
                    
                case "ISUPDATED":
                    ArGenReceiptBillDtl existingBill = billDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, bill.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Bill not found", "detRowId", bill.getDetRowId()));
                    
                    ArGenReceiptBillDtl oldBill = new ArGenReceiptBillDtl();
                    BeanUtils.copyProperties(existingBill, oldBill);
                    
                    Long updatedGlPoid = bill.getGlPoid() != null ? bill.getGlPoid() : header.getRcvdOthPoid();
                    existingBill.setGlPoid(updatedGlPoid);
                    existingBill.setBillRefType(bill.getBillRefType() != null ? bill.getBillRefType() : header.getRefType());
                    existingBill.setBillRefno(bill.getBillReference());
                    existingBill.setBillDueDate(bill.getBillDueDate());
                    existingBill.setDescription(bill.getDescription());
                    existingBill.setAmount(bill.getAmount());
                    existingBill.setCrDrType(bill.getDrCr());
                    existingBill.setGlCompanyPoid(bill.getGlCompanyPoid() != null ? bill.getGlCompanyPoid() : header.getCompanyPoid());
                    existingBill.setRemarks(bill.getRemarks());
                    existingBill.setLastModifiedBy(currentUser);
                    existingBill.setLastModifiedDate(now);
                    toUpdate.add(existingBill);
                    
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, bill.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldBill, existingBill, ArGenReceiptBillDtl.class, docId, docKeyPoid, logDetailForUpdate));
                    break;
                    
                case "ISDELETED":
                    toDelete.add(bill.getDetRowId());
                    loggingService.logDelete(bill, docId, docKeyPoid);
                    break;
            }
        }
        
        if (!toSave.isEmpty()) {
            billDtlRepository.saveAll(toSave);
            toSave.forEach(e -> {
                String logDetail = String.format("Row Created on Bill with detRowId: %s", e.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            });
        }

        if (!toUpdate.isEmpty()) {
            billDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (!toDelete.isEmpty()) {
            billDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
        
        if (!toSave.isEmpty() || !toUpdate.isEmpty()) {
            entityManager.flush();
            callBillwiseCheckProcedure(transactionPoid, header.getCompanyPoid());
        }
    }

    private void updateChargeDetails(List<GeneralReceiptChargeDto> charges, Long transactionPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<ArGenReceiptChargesDtl> toSave = new ArrayList<>();
        List<ArGenReceiptChargesDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArGenReceiptChargesDtl>> logRequests = new ArrayList<>();
        
        ArGenReceiptHdr header = receiptHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("General Receipt", "transactionPoid", transactionPoid));
        
        for (GeneralReceiptChargeDto charge : charges) {
            String action = charge.getActionType() != null ? charge.getActionType().toUpperCase() : "ISCREATED";
            switch (action) {
                case "ISCREATED":
                    List<GLMasterEntity> chargeGLList = glMastersRepository.findAllByGlCodeAndDeletedFlag(charge.getGl(), "N");
                    if (chargeGLList.isEmpty()) {
                        throw new ValidationException("Charge GL not found: " + charge.getGl());
                    }
                    GLMasterEntity chargeGL = chargeGLList.get(0);
                    
                    toSave.add(ArGenReceiptChargesDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(charge.getDetRowId())
                            .chargeType(charge.getChargeType())
                            .glPoid(chargeGL.getGlPoid())
                            .amount(charge.getAmount())
                            .bhdAmount(charge.getAmount().multiply(header.getCurrencyRate()))
                            .taxPoid(charge.getTaxPoid())
                            .taxPercentage(charge.getTaxPercent())
                            .taxAmount(charge.getTaxAmount())
                            .totalAmount(charge.getTotalAmount())
                            .costPoid(charge.getCostCenter())
                            .remarks(charge.getRemarks())
                            .build());
                    break;
                    
                case "ISUPDATED":
                    ArGenReceiptChargesDtl existingCharge = chargesDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, charge.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Charge not found", "detRowId", charge.getDetRowId()));
                    
                    ArGenReceiptChargesDtl oldCharge = new ArGenReceiptChargesDtl();
                    BeanUtils.copyProperties(existingCharge, oldCharge);
                    
                    List<GLMasterEntity> updatedChargeGLList = glMastersRepository.findAllByGlCodeAndDeletedFlag(charge.getGl(), "N");
                    if (updatedChargeGLList.isEmpty()) {
                        throw new ValidationException("Charge GL not found: " + charge.getGl());
                    }
                    GLMasterEntity updatedChargeGL = updatedChargeGLList.get(0);
                    
                    existingCharge.setChargeType(charge.getChargeType());
                    existingCharge.setGlPoid(updatedChargeGL.getGlPoid());
                    existingCharge.setAmount(charge.getAmount());
                    existingCharge.setBhdAmount(charge.getAmount().multiply(header.getCurrencyRate()));
                    existingCharge.setTaxPoid(charge.getTaxPoid());
                    existingCharge.setTaxPercentage(charge.getTaxPercent());
                    existingCharge.setTaxAmount(charge.getTaxAmount());
                    existingCharge.setTotalAmount(charge.getTotalAmount());
                    existingCharge.setCostPoid(charge.getCostCenter());
                    existingCharge.setRemarks(charge.getRemarks());
                    existingCharge.setLastModifiedBy(currentUser);
                    existingCharge.setLastModifiedDate(now);
                    toUpdate.add(existingCharge);
                    
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, charge.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldCharge, existingCharge, ArGenReceiptChargesDtl.class, docId, docKeyPoid, logDetailForUpdate));
                    break;
                    
                case "ISDELETED":
                    toDelete.add(charge.getDetRowId());
                    loggingService.logDelete(charge, docId, docKeyPoid);
                    break;
            }
        }
        
        if (!toSave.isEmpty()) {
            chargesDtlRepository.saveAll(toSave);
            toSave.forEach(e -> {
                String logDetail = String.format("Row Created on Charge with detRowId: %s", e.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            });
        }

        if (!toUpdate.isEmpty()) {
            chargesDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (!toDelete.isEmpty()) {
            chargesDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
    }

    private void updateAdvanceDetails(List<GeneralReceiptAdvanceDto> advances, Long transactionPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<ArGenReceiptAdvanceDtl> toSave = new ArrayList<>();
        List<ArGenReceiptAdvanceDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArGenReceiptAdvanceDtl>> logRequests = new ArrayList<>();
        
        for (GeneralReceiptAdvanceDto advance : advances) {
            String action = advance.getActionType() != null ? advance.getActionType().toUpperCase() : "ISCREATED";
            switch (action) {
                case "ISCREATED":
                    toSave.add(ArGenReceiptAdvanceDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(advance.getDetRowId())
                            .advanceRefDocId(advance.getAdvanceRefDocId())
                            .advanceRefPoid(advance.getAdvanceRefPoid())
                            .amount(advance.getAmount())
                            .remarks(advance.getRemarks())
                            .build());
                    break;
                    
                case "ISUPDATED":
                    ArGenReceiptAdvanceDtl existingAdvance = advanceDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, advance.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Advance not found", "detRowId", advance.getDetRowId()));
                    
                    ArGenReceiptAdvanceDtl oldAdvance = new ArGenReceiptAdvanceDtl();
                    BeanUtils.copyProperties(existingAdvance, oldAdvance);
                    
                    existingAdvance.setAdvanceRefDocId(advance.getAdvanceRefDocId());
                    existingAdvance.setAdvanceRefPoid(advance.getAdvanceRefPoid());
                    existingAdvance.setAmount(advance.getAmount());
                    existingAdvance.setRemarks(advance.getRemarks());
                    existingAdvance.setLastModifiedBy(currentUser);
                    existingAdvance.setLastModifiedDate(now);
                    toUpdate.add(existingAdvance);
                    
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, advance.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldAdvance, existingAdvance, ArGenReceiptAdvanceDtl.class, docId, docKeyPoid, logDetailForUpdate));
                    break;
                    
                case "ISDELETED":
                    toDelete.add(advance.getDetRowId());
                    loggingService.logDelete(advance, docId, docKeyPoid);
                    break;
            }
        }
        
        if (!toSave.isEmpty()) {
            advanceDtlRepository.saveAll(toSave);
            toSave.forEach(e -> {
                String logDetail = String.format("Row Created on Advance with detRowId: %s", e.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            });
        }

        if (!toUpdate.isEmpty()) {
            advanceDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (!toDelete.isEmpty()) {
            advanceDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
    }

    @Override
    @Transactional
    public void deleteGeneralReceipt(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting general receipt: {}", transactionPoid);
        ArGenReceiptHdr header = receiptHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("General Receipt", "transactionPoid", transactionPoid));
        if ("Y".equalsIgnoreCase(header.getDeleted())) {
            throw new ValidationException("Receipt is already deleted");
        }
        if ("Y".equalsIgnoreCase(header.getVerified())) {
            throw new ValidationException("Cannot delete receipt that has been verified/posted to GL");
        }
        documentDeleteService.deleteDocument(
                transactionPoid,
                "AR_GEN_RECEIPT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                header.getTransactionDate()
        );
        log.info("Successfully deleted general receipt: {}", header.getDocRef());
    }

    /**
     * Check for downstream linkages in GL_LEDGER table
     * Returns count of GL_LEDGER records linked to this receipt
     */
    private Long checkDownstreamLinkages(Long transactionPoid) {
        try {
            String sql = "SELECT COUNT(*) FROM GL_LEDGER WHERE DOC_ID = :docId AND TRANSACTION_POID = :transactionPoid";
            Object result = entityManager.createNativeQuery(sql)
                    .setParameter("docId", DOC_ID)
                    .setParameter("transactionPoid", transactionPoid)
                    .getSingleResult();
            
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
            return 0L;
        } catch (Exception e) {
            log.warn("Error checking downstream linkages for receipt {}: {}", transactionPoid, e.getMessage());
            // If we can't check, assume no linkages to allow delete (fail-safe)
            return 0L;
        }
    }

    // ===== PRIVATE HELPER METHODS =====
    
    private void validateGeneralReceiptRequest(GeneralReceiptRequest request) {
        GeneralReceiptHeaderDto header = request.getHeader();

        
        List<GeneralReceiptBillDto> activeBills = new ArrayList<>();
        if (request.getBills() != null) {
            activeBills = request.getBills().stream()
                    .filter(bill -> bill.getActionType() == null ||
                            !"ISDELETED".equalsIgnoreCase(bill.getActionType()))
                    .collect(Collectors.toList());
        }

        List<GeneralReceiptChargeDto> activeCharges = new ArrayList<>();
        if (request.getExtraCharges() != null) {
            activeCharges = request.getExtraCharges().stream()
                    .filter(charge -> charge.getActionType() == null ||
                            !"ISDELETED".equalsIgnoreCase(charge.getActionType()))
                    .collect(Collectors.toList());
        }

        Long creditGlPoid;
        try {
            creditGlPoid = Long.parseLong(header.getCreditGL());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Credit GL not found: " + header.getCreditGL());
        }

        GLMasterEntity creditGL = glMastersRepository.findById(creditGlPoid)
                .orElseThrow(() -> new ValidationException("Credit GL not found: " + header.getCreditGL()));
        if ("Y".equalsIgnoreCase(creditGL.getDeletedFlag())) {
            throw new ValidationException("Credit GL not found: " + header.getCreditGL());
        }

        String billwiseFlag = getGlBillwiseYn(creditGL.getGlPoid());
        boolean isBillwiseEnabled = "Y".equalsIgnoreCase(billwiseFlag);

        if (isBillwiseEnabled) {
            BigDecimal billTotal = BigDecimal.ZERO;
            billTotal = activeBills.stream()
                    .map(GeneralReceiptBillDto::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (billTotal.compareTo(BigDecimal.ZERO) == 0) {
                throw new ValidationException("Zero values found in Billwise total Amount... , please check");
            }
        }

        if (!activeBills.isEmpty()) {
            for (GeneralReceiptBillDto bill : activeBills) {
                if (bill.getBillDueDate() == null) {
                    throw new ValidationException("Due date is required for all bills");
                }
            }
        }
        
        // 4. Validate FDA advance amount matches BHD amount
        if ("FDA_ADVANCE".equals(header.getRefType())) {
            if (request.getAdvances() == null || request.getAdvances().isEmpty()) {
                throw new ValidationException("FDA advance details are required when Ref Type is FDA_ADVANCE");
            }

            BigDecimal advanceTotal = request.getAdvances().stream()
                    .map(GeneralReceiptAdvanceDto::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal bhdAmount = header.getBhdAmount() != null ? header.getBhdAmount() : header.getReceiptAmount().multiply(header.getRate());
            
            if (bhdAmount.compareTo(advanceTotal) != 0) {
                throw new ValidationException("BHD amount doesn't match with FDA Amount");
            }
        }

        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            BigDecimal paymentTotal = request.getPayments().stream()
                    .filter(payment -> {
                        String action = payment.getActionType();
                        return action == null || !action.toUpperCase().equals("ISDELETED");
                    })
                    .map(GeneralReceiptPaymentDto::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal bhdAmount = header.getBhdAmount() != null ? header.getBhdAmount() : header.getReceiptAmount().multiply(header.getRate());

            log.debug("Amount validation - BHD amount: {}, Payment total: {}",
                    bhdAmount, paymentTotal);

            if (bhdAmount.compareTo(paymentTotal) != 0) {
                throw new ValidationException(String.format(
                        "Total BHD amount (%.3f) does not match sum of payment amounts (%.3f)",
                        bhdAmount, paymentTotal));
            }
        }

        BigDecimal amountToCompare = "BHD".equalsIgnoreCase(header.getCurrency())
                ? header.getReceiptAmount()
                : (header.getBhdAmount() != null ? header.getBhdAmount() : header.getReceiptAmount().multiply(header.getRate()));
        validateBillAmountMatchesReceiptAmount(activeBills, activeCharges, amountToCompare);

        // 8. Validate cheque dates if applicable
        validateChequeDates(request.getPayments());

        // 9. Validate cost center for specific charge types
        validateCostCenterRequirement(activeCharges);
        
        // 10. Validate rounding limit
        validateRoundingLimit(activeCharges);

        // 11. Validate multi-company if applicable
        if ("Y".equals(header.getMulticompany())) {
            validateMultiCompany(activeBills, header.getCompanyPoid());
        }
        
        // 12. Validate bill references if refType is AGAINST
        if ("AGAINST".equals(header.getRefType()) && activeBills.isEmpty()) {
            throw new ValidationException("Bill details are required when Ref Type is AGAINST");
        }

        // 13. Validate bill references using stored procedure (PROC_GEN_RECE_NEW_BILLREF_CHK)
        if (!activeBills.isEmpty()) {
            validateBillReferencesUsingProcedure(activeBills, creditGL.getGlPoid(), header.getCompanyPoid());
        }
    }

    private void validateBillAmountMatchesReceiptAmount(List<GeneralReceiptBillDto> bills, 
                                                         List<GeneralReceiptChargeDto> charges, 
                                                         BigDecimal amountToCompare) {
        if (bills == null || bills.isEmpty()) {
            return;
        }

        BigDecimal billTotal = bills.stream()
                .map(GeneralReceiptBillDto::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal chargeTotal = BigDecimal.ZERO;
        if (charges != null && !charges.isEmpty()) {
            chargeTotal = charges.stream()
                    .map(GeneralReceiptChargeDto::getTotalAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal expectedTotal = billTotal.add(chargeTotal);

        if (amountToCompare.compareTo(expectedTotal) != 0) {
            throw new ValidationException(String.format(
                    "Receipt amount (%.3f) does not match bill amount (%.3f) ,please check",
                    amountToCompare, billTotal, chargeTotal, expectedTotal));
        }
    }

    private void validateChequeDates(List<GeneralReceiptPaymentDto> payments) {
        String postDateDaysParam = parameterServiceClient.findParameterValueByName("GEN_RECEIPT_CHEQUE_POST_DATE_VALIDATION_DAYS")
                .orElse("30");
        String backDateDaysParam = parameterServiceClient.findParameterValueByName("GEN_RECEIPT_CHEQUE_BACK_DATE_VALIDATION_DAYS")
                .orElse("30");

        int postDateDays = Integer.parseInt(postDateDaysParam);
        int backDateDays = Integer.parseInt(backDateDaysParam);
        int postLimit = Math.abs(postDateDays);
        int backLimit = Math.abs(backDateDays);

        for (GeneralReceiptPaymentDto payment : payments) {
            if ("CHEQUE".equals(payment.getType()) && payment.getChequeDate() != null) {
                LocalDate today = LocalDate.now();
                long daysDiff = ChronoUnit.DAYS.between(today, payment.getChequeDate());

                if (daysDiff > postLimit) {
                    throw new ValidationException(String.format(
                            "Cheque date cannot be more than %d days in the future", postLimit));
                }

                if (daysDiff < -backLimit) {
                    throw new ValidationException(String.format(
                            "Cheque date cannot be more than %d days in the past", backLimit));
                }
            }
        }
    }

    private void validateCostCenterRequirement(List<GeneralReceiptChargeDto> charges) {
        if (charges == null || charges.isEmpty()) {
            return;
        }

        String costCenterApplicable = parameterServiceClient.findParameterValueByName("COST_CENTER_APPLICABLE")
                .orElse("N");

        if ("Y".equals(costCenterApplicable)) {
            for (GeneralReceiptChargeDto charge : charges) {
                if (isChargeTypeThatRequiresCostCenter(charge.getChargeType()) && 
                    (charge.getCostCenter() == null || charge.getCostCenter().isBlank())) {
                    throw new ValidationException(String.format(
                            "Cost center is required for charge type: %s", charge.getChargeType()));
                }
            }
        }
    }

    private boolean isChargeTypeThatRequiresCostCenter(String chargeType) {
        return "BANK_CHARGES".equals(chargeType) || 
               "ROUND_OFF".equals(chargeType) || 
               "EXCHANGE_GAIN_LOSS".equals(chargeType);
    }

    private void validateRoundingLimit(List<GeneralReceiptChargeDto> charges) {
        if (charges == null || charges.isEmpty()) {
            return;
        }

        String roundingLimitParam = parameterServiceClient.findParameterValueByName("ROUNDING_LIMIT")
                .orElse("1.000");
        BigDecimal roundingLimit = new BigDecimal(roundingLimitParam);

        for (GeneralReceiptChargeDto charge : charges) {
            if ("ROUND_OFF".equals(charge.getChargeType()) && 
                charge.getAmount().abs().compareTo(roundingLimit) > 0) {
                throw new ValidationException(String.format(
                        "Round off amount (%.3f) exceeds the limit (%.3f)",
                        charge.getAmount(), roundingLimit));
            }
        }
    }

    private void validateMultiCompany(List<GeneralReceiptBillDto> bills, Long mainCompanyPoid) {
        if (bills == null || bills.isEmpty()) {
            return;
        }

        boolean hasMultipleCompanies = bills.stream()
                .map(GeneralReceiptBillDto::getGlCompanyPoid)
                .filter(poid -> poid != null && !poid.equals(mainCompanyPoid))
                .findAny()
                .isPresent();

        if (!hasMultipleCompanies) {
            throw new ValidationException("Multi-company flag is set but all bills belong to the same company");
        }
    }

    private ArGenReceiptHdr buildHeaderEntity(GeneralReceiptHeaderDto dto, String currentUser, LocalDateTime now) {
        Long creditGlPoid;
        try {
            creditGlPoid = Long.parseLong(dto.getCreditGL());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Credit GL not found: " + dto.getCreditGL());
        }

        BigDecimal receiptAmount = dto.getReceiptAmount();
        BigDecimal invoiceAmount = dto.getInvoiceAmount();
        
        // Apply legacy logic: receipt amount = invoice amount for all currencies
        if (invoiceAmount != null) {
            receiptAmount = invoiceAmount;
        }

        return ArGenReceiptHdr.builder()
                .transactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now())
                .groupPoid(DEFAULT_GROUP_POID)
                .companyPoid(dto.getCompanyPoid())
                .rcvdOthPoid(creditGlPoid)
                .rcptAmount(receiptAmount)
                .invoiceAmount(invoiceAmount)
                .remarks(dto.getNarration())
                .rcvdFromDtlPrint(dto.getReceivedFrom())
                .refType(dto.getRefType())
                .currencyCode(dto.getCurrency())
                .currencyRate(dto.getRate())
                .multicompany(dto.getMulticompany() != null ? dto.getMulticompany() : "N")
                .ttBankPoid(dto.getTtBankPoid())
                .costCenterPoid(dto.getCostCenterPoid())
                .printDocCompId(dto.getPrintDocCompId())
                .deleted("N")
                .verified("N")
                .dataLoaded("N")
                .extraCharges(dto.getExtraCharges() != null ? dto.getExtraCharges() : "N")
                .lineType("GENERAL")
                .rcvdType("GENERAL")
                .build();
    }

    private void updateHeaderEntity(ArGenReceiptHdr header, GeneralReceiptHeaderDto dto, String currentUser, LocalDateTime now) {
        Long creditGlPoid;
        try {
            creditGlPoid = Long.parseLong(dto.getCreditGL());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Credit GL not found: " + dto.getCreditGL());
        }

        if (dto.getTransactionDate() != null) {
            header.setTransactionDate(dto.getTransactionDate());
        } else {
            header.setTransactionDate(LocalDate.now());
        }

        BigDecimal receiptAmount = dto.getReceiptAmount();
        BigDecimal invoiceAmount = dto.getInvoiceAmount();
        
        // Apply legacy logic: receipt amount = invoice amount for all currencies
        if (invoiceAmount != null) {
            receiptAmount = invoiceAmount;
        }

        header.setRcvdOthPoid(creditGlPoid);
        header.setRcptAmount(receiptAmount);
        header.setInvoiceAmount(invoiceAmount);
        header.setRemarks(dto.getNarration());
        header.setRcvdFromDtlPrint(dto.getReceivedFrom());
        header.setRefType(dto.getRefType());
        header.setCurrencyCode(dto.getCurrency());
        header.setCurrencyRate(dto.getRate());
        header.setMulticompany(dto.getMulticompany() != null ? dto.getMulticompany() : "N");
        header.setExtraCharges(dto.getExtraCharges() != null ? dto.getExtraCharges() : "N");
        header.setTtBankPoid(dto.getTtBankPoid());
        header.setCostCenterPoid(dto.getCostCenterPoid());
        header.setLastModifiedBy(currentUser);
        header.setLastModifiedDate(now);
        header.setPrintDocCompId(dto.getPrintDocCompId());
    }

    private void savePaymentDetails(ArGenReceiptHdr header, List<GeneralReceiptPaymentDto> payments, 
                                    String currentUser, LocalDateTime now, boolean freshInsert) {
        if (payments == null || payments.isEmpty()) {
            return;
        }
        
        log.info("Saving payment details for header POID: {}", header.getTransactionPoid());
        
        // Double-check header exists before saving details
        Long headerCount = ((Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM AR_GEN_RECEIPT_HDR WHERE TRANSACTION_POID = ?")
            .setParameter(1, header.getTransactionPoid())
            .getSingleResult()).longValue();

//        if (headerCount == 0) {
//            throw new ValidationException("Header not found when saving payment details - POID: " + header.getTransactionPoid());
//        }
        log.info("Header verification - count: {}", headerCount);
        
        List<ArGenReceiptPymtDetails> details = new ArrayList<>();

        // 🔥 DET ROW ID GENERATION LOGIC
        // ------------------------------
        long startIndex = freshInsert
                ? 1
                : (pymtDetailsRepository.countByTransactionPoid(header.getTransactionPoid()) + 1);

        long i = startIndex;
        
        for (GeneralReceiptPaymentDto payment : payments) {
            Long detId = freshInsert
                    ? i++
                    : (payment.getDetRowId() != null ? payment.getDetRowId() : i++);

            payment.setDetRowId(detId);
            ArGenReceiptPymtDetails detail = ArGenReceiptPymtDetails.builder()
                    .transactionPoid(header.getTransactionPoid())  // Set parent transaction POID
                    .detRowId(detId)
                    .pymtType(payment.getType())
                    .amount(payment.getAmount())
                    .chqCardno(payment.getChequeNo())
                    .chqDate(payment.getChequeDate())
                    .bankPoid(payment.getBankPoid())
                    .accountPoid(payment.getAccountPoid())
                    .accountName(payment.getAccountName())
                    .accountNo(payment.getAccountNumber())
                    .ttBankPoid(payment.getTtBankPoid())
                    .ttRef(payment.getTtRef())
                    .creditCardRef(payment.getCreditCardRef())
                    .cardType(payment.getCardType())
                    .cardPoid(payment.getCardPoid())
                    .build();

            details.add(detail);
        }
        
        // Batch save all payment details in one call
        if (!details.isEmpty()) {
            List<ArGenReceiptPymtDetails> savedDetails = pymtDetailsRepository.saveAll(details);
            
            // Log each payment detail creation
            savedDetails.forEach(paymentDetail -> {
                String logDetail = String.format("Row Created on Payment with detRowId: %s", paymentDetail.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), header.getTransactionPoid().toString(), logDetail);
            });
        }
    }

    private void saveBillDetails(ArGenReceiptHdr header, List<GeneralReceiptBillDto> bills, 
                                String currentUser, LocalDateTime now, boolean freshInsert) {
        if (bills == null || bills.isEmpty()) {
            return;
        }

        // DET ROW ID GENERATION LOGIC
        // -----------------------------
        long startIndex = freshInsert
                ? 1
                : (billDtlRepository.countByTransactionPoid(header.getTransactionPoid()) + 1);

        long i = startIndex;
        
        List<ArGenReceiptBillDtl> details = new ArrayList<>();
        
        for (GeneralReceiptBillDto bill : bills) {

            // Same logic you use everywhere
            Long detId = freshInsert
                    ? i++
                    : (bill.getDetRowId() != null ? bill.getDetRowId() : i++);

            // Set back into DTO (needed for next updates)
            bill.setDetRowId(detId);

            // Get GL POID from header's credit GL if not provided
            Long glPoid = bill.getGlPoid();
            if (glPoid == null) {
                glPoid = header.getRcvdOthPoid();
            }

            ArGenReceiptBillDtl detail = ArGenReceiptBillDtl.builder()
                    .transactionPoid(header.getTransactionPoid())  // Set parent transaction POID
                    .detRowId(detId)
                    .glPoid(glPoid)
                    .billRefType(bill.getBillRefType() != null ? bill.getBillRefType() : header.getRefType())
                    .billRefno(bill.getBillReference())
                    .billDueDate(bill.getBillDueDate())
                    .description(bill.getDescription())
                    .amount(bill.getAmount())
                    .crDrType(bill.getDrCr())
                    .glCompanyPoid(bill.getGlCompanyPoid() != null ? bill.getGlCompanyPoid() : header.getCompanyPoid())
                    .remarks(bill.getRemarks())
                    .checkall("N")
                    .build();

            details.add(detail);
        }
        
        // Batch save all bill details in one call
        if (!details.isEmpty()) {
            List<ArGenReceiptBillDtl> savedDetails = billDtlRepository.saveAll(details);
            entityManager.flush();  // Ensure details are persisted before calling procedure
            
            // Log each bill detail creation
            savedDetails.forEach(billDetail -> {
                String logDetail = String.format("Row Created on Bill with detRowId: %s", billDetail.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), header.getTransactionPoid().toString(), logDetail);
            });
            
            // Call PROC_GEN_RECEIPT_BILLWISE_CHK to format bill references (trim at pipe delimiter)
            callBillwiseCheckProcedure(header.getTransactionPoid(), header.getCompanyPoid());
        }
    }

    private void saveChargeDetails(ArGenReceiptHdr header, List<GeneralReceiptChargeDto> charges, 
                                   String currentUser, LocalDateTime now, boolean freshInsert) {
        if (charges == null || charges.isEmpty()) {
            return;
        }

        // DET ROW ID GENERATION (same pattern)
        // -----------------------------------
        long startIndex = freshInsert
                ? 1
                : (chargesDtlRepository.countByTransactionPoid(header.getTransactionPoid()) + 1);

        long i = startIndex;
        
        List<ArGenReceiptChargesDtl> details = new ArrayList<>();
        
        for (GeneralReceiptChargeDto charge : charges) {

            // 🔥 DET ROW ID LOGIC (copy-paste from all other create methods)
            Long detId = freshInsert
                    ? i++
                    : (charge.getDetRowId() != null ? charge.getDetRowId() : i++);

            // Set back to DTO (needed for future update calls)
            charge.setDetRowId(detId);

            // Get GL POID
            List<GLMasterEntity> chargeGLList = glMastersRepository.findAllByGlCodeAndDeletedFlag(charge.getGl(), "N");
            if (chargeGLList.isEmpty()) {
                throw new ValidationException("Charge GL not found: " + charge.getGl());
            }
            GLMasterEntity chargeGL = chargeGLList.get(0);

            // Calculate BHD equivalent (amount * currency rate)
            BigDecimal bhdEquivalent = charge.getAmount().multiply(header.getCurrencyRate());
            
            ArGenReceiptChargesDtl detail = ArGenReceiptChargesDtl.builder()
                    .transactionPoid(header.getTransactionPoid())  // Set parent transaction POID
                    .detRowId(detId)
                    .chargeType(charge.getChargeType())
                    .glPoid(chargeGL.getGlPoid())
                    .amount(charge.getAmount())
                    .bhdAmount(bhdEquivalent)  // Calculate BHD equivalent using currency rate
                    .taxPoid(charge.getTaxPoid())
                    .taxPercentage(charge.getTaxPercent())
                    .taxAmount(charge.getTaxAmount())
                    .totalAmount(charge.getTotalAmount())
                    .costPoid(charge.getCostCenter())
                    .remarks(charge.getRemarks())
                    .build();

            details.add(detail);
        }
        
        // Batch save all charge details in one call
        if (!details.isEmpty()) {
            List<ArGenReceiptChargesDtl> savedDetails = chargesDtlRepository.saveAll(details);
            
            // Log each charge detail creation
            savedDetails.forEach(chargeDetail -> {
                String logDetail = String.format("Row Created on Charge with detRowId: %s", chargeDetail.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), header.getTransactionPoid().toString(), logDetail);
            });
        }

        // Set extra charges flag
        header.setExtraCharges("Y");
    }

    private void saveAdvanceDetails(ArGenReceiptHdr header, List<GeneralReceiptAdvanceDto> advances, 
                                    String currentUser, LocalDateTime now, boolean freshInsert) {
        if (advances == null || advances.isEmpty()) {
            return;
        }

        // DET ROW ID GENERATION (same pattern everywhere)
        // ------------------------------------------------
        long startIndex = freshInsert
                ? 1
                : (advanceDtlRepository.countByTransactionPoid(header.getTransactionPoid()) + 1);

        long i = startIndex;
        
        List<ArGenReceiptAdvanceDtl> details = new ArrayList<>();
        
        for (GeneralReceiptAdvanceDto advance : advances) {
            // 🔥 DET ROW ID LOGIC
            Long detId = freshInsert
                    ? i++
                    : (advance.getDetRowId() != null ? advance.getDetRowId() : i++);

            // put back into DTO for future update usage
            advance.setDetRowId(detId);
            ArGenReceiptAdvanceDtl detail = ArGenReceiptAdvanceDtl.builder()
                    .transactionPoid(header.getTransactionPoid())
                    .detRowId(detId)
                    .advanceRefDocId(advance.getAdvanceRefDocId())
                    .advanceRefPoid(advance.getAdvanceRefPoid())
                    .amount(advance.getAmount())
                    .remarks(advance.getRemarks())
                    .build();

            details.add(detail);
        }
        
        // Batch save all advance details
        if (!details.isEmpty()) {
            List<ArGenReceiptAdvanceDtl> savedDetails = advanceDtlRepository.saveAll(details);
            
            // Log each advance detail creation
            savedDetails.forEach(advanceDetail -> {
                String logDetail = String.format("Row Created on Advance with detRowId: %s", advanceDetail.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), header.getTransactionPoid().toString(), logDetail);
            });
        }
    }


    private String extractNumericFromDocRef(String docRef) {
        // Extract numeric part from DOC_REF (e.g., "ASGGEN234830" -> "234830")
        if (docRef == null || docRef.isEmpty()) {
            throw new ValidationException("Document reference is null or empty");
        }
        
        // Find the last sequence of digits
        String numericPart = docRef.replaceAll("\\D", "");
        if (numericPart.isEmpty()) {
            throw new ValidationException("No numeric part found in document reference: " + docRef);
        }
        
        return numericPart;
    }

    /**
     * Validate bill references using stored procedure
     * Validates whether bill references already exist (NEW) or exist for settlement (AGAINST)
     */
    private void validateBillReferencesUsingProcedure(List<GeneralReceiptBillDto> bills, Long glPoid, Long companyPoid) {
        try {
            Long userPoid = UserContext.getUserPoid();
            if (userPoid == null) {
                userPoid = 1L; // Default user POID
            }

            for (GeneralReceiptBillDto bill : bills) {
                String billRefType = bill.getBillRefType() != null ? bill.getBillRefType() : "AGAINST";
                
                log.debug("Validating bill reference: {} with type: {}", bill.getBillReference(), billRefType);
                
                String result = procedureRepository.validateBillReference(
                        companyPoid, userPoid, glPoid, bill.getBillReference(), null, billRefType);
                
                if (result != null && (result.toUpperCase().contains("WARNING") || result.toUpperCase().contains("ERROR"))) {
                    log.error("Bill reference validation failed: {}", result);
                    throw new ValidationException(result);
                }
                
                log.debug("Bill reference validation successful for: {}", bill.getBillReference());
            }
            
        } catch (Exception e) {
            log.error("Error validating bill references", e);
            if (e instanceof ValidationException) {
                throw e;
            }
            throw new ValidationException("Bill reference validation failed: " + e.getMessage());
        }
    }

    /**
     * Format bill references using stored procedure
     * Trims bill references at pipe delimiter (e.g., "INV-001|Info" -> "INV-001")
     */
    private void callBillwiseCheckProcedure(Long transactionPoid, Long companyPoid) {
        try {
            Long userPoid = UserContext.getUserPoid();
            if (userPoid == null) {
                userPoid = 1L; // Default user POID
            }

            procedureRepository.formatBillReferences(companyPoid, userPoid, transactionPoid);
            
        } catch (Exception e) {
            log.error("Error calling PROC_GEN_RECEIPT_BILLWISE_CHK", e);
            throw new ValidationException("Bill reference formatting failed: " + e.getMessage());
        }
    }

    // Removed native SQL helpers; using repository-based saves instead

    private String processGLPostingOrApproval(ArGenReceiptHdr header) {
        // Check if approval is required
        String approvalSubmission = parameterServiceClient.findParameterValueByName("GENERAL_RECEIPT_APPROVAL_SUBMISSION")
                .orElse("DIRECT_POST");

        if ("APPROVAL".equals(approvalSubmission)) {
            // Submit for approval
            return submitForApproval(header);
        } else {
            // Direct GL posting
            return callGLPostingProcedure(header);
        }
    }

    private String callGLPostingProcedure(ArGenReceiptHdr header) {
        try {
            log.info("Calling GL posting procedure for receipt: {}", header.getDocRef());

            // Extract numeric part from DOC_REF (e.g., "ASGGEN234830" -> 234830)
            String docRefNumeric = extractNumericFromDocRef(header.getDocRef());
            Long docRefNumber = Long.parseLong(docRefNumeric);

            Long userPoid = UserContext.getUserPoid();
            if (userPoid == null) {
                throw new ValidationException("User context not available - User POID is null");
            }

            String status = procedureRepository.callGLPostingProcedure(
                    header.getCompanyPoid(), userPoid, header.getTransactionPoid(), docRefNumber);
            
            if (status != null && status.toUpperCase().contains("ERROR")) {
                throw new ValidationException("GL Posting failed: " + status);
            }

            log.info("GL posting successful for receipt: {}", header.getDocRef());
            return status != null ? status : "GL posting successful";

        } catch (Exception e) {
            log.error("Error calling GL posting procedure", e);
            if (e instanceof ValidationException) {
                throw e;
            }
            throw new ValidationException("GL Posting failed: " + e.getMessage());
        }
    }

    private String submitForApproval(ArGenReceiptHdr header) {
        try {
            log.info("Submitting receipt for approval: {}", header.getDocRef());

            // Extract numeric part from DOC_REF for approval procedure
            String docRefNumeric = extractNumericFromDocRef(header.getDocRef());
            Long docRefNumber = Long.parseLong(docRefNumeric);

            Long userPoid = UserContext.getUserPoid();
            if (userPoid == null) {
                throw new ValidationException("User context not available - User POID is null");
            }

            String actionResult = procedureRepository.callApprovalProcedure(
                    header.getCompanyPoid(), userPoid, header.getTransactionPoid(), 
                    docRefNumber, header.getTransactionDate());
            
            if (actionResult != null && actionResult.toUpperCase().contains("ERROR")) {
                throw new ValidationException("Approval submission failed: " + actionResult);
            }

            log.info("Receipt submitted for approval successfully: {}", header.getDocRef());
            return "Receipt submitted for approval successfully";

        } catch (Exception e) {
            log.error("Error submitting for approval", e);
            if (e instanceof ValidationException) {
                throw e;
            }
            throw new ValidationException("Approval submission failed: " + e.getMessage());
        }
    }

    private GeneralReceiptResponse buildResponse(ArGenReceiptHdr header) {

        BigDecimal currencyRate = null;
        if (header.getCurrencyRate() != null) {
            currencyRate = header.getCurrencyRate().setScale(3, RoundingMode.HALF_UP);
        }

        // Calculate BHD Amount (receiptAmount * currencyRate)
        BigDecimal bhdAmount = null;
        if (header.getRcptAmount() != null && header.getCurrencyRate() != null) {
            bhdAmount = header.getRcptAmount().multiply(header.getCurrencyRate());
        }

        // For response: receipt amount should equal invoice amount
        BigDecimal responseReceiptAmount = header.getInvoiceAmount();
        if (responseReceiptAmount == null) {
            responseReceiptAmount = header.getRcptAmount();
        }

        // Fetch approval status from GLOBAL_APPROVAL_STATUS table
        final String approvalStatus = approvalService.getApprovalStatus(UserContext.getDocumentId(), header.getTransactionPoid());

        // Fetch Credit GL details
        CreditGlDto creditGL = null;
        if (header.getRcvdOthPoid() != null) {
            Optional<GLMasterEntity> glOptional = glMastersRepository.findById(header.getRcvdOthPoid());
            if (glOptional.isPresent()) {
                GLMasterEntity gl = glOptional.get();
                String glDescription = gl.getDescription();
                try {
//                    LovGetListDto lovDetails = lovService.getDetailsByPoidAndLovName(gl.getGlPoid(), "GEN_RECEIPT_CREDIT_GL");
//                    if (lovDetails != null && lovDetails.getDescription() != null) {
//                        glDescription = lovDetails.getDescription();
//                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch LOV description for GL poid {}: {}", gl.getGlPoid(), e.getMessage());
                }
                creditGL = CreditGlDto.builder()
                        .glPoid(gl.getGlPoid())
                        .glCode(gl.getGlCode())
                        .glDescription(glDescription)
                        .build();
            }
        }

        // Fetch Print Title (Company Name) from LOV
        String printTitle = null;
        if (header.getPrintDocCompId() != null) {
            try {
                LovGetListDto companyLov = lovService.getDetailsByPoidAndLovName(header.getPrintDocCompId(), "COMPANY");
                if (companyLov != null && companyLov.getDescription() != null) {
                    printTitle = companyLov.getDescription();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch company name for printDocCompId {}: {}", header.getPrintDocCompId(), e.getMessage());
            }
        }

        return GeneralReceiptResponse.builder()
                .status("SUCCESS")
                .message("Receipt fetched successfully")
                .receiptNo(header.getDocRef())
                .transactionPoid(header.getTransactionPoid())
                .transactionDate(header.getTransactionDate())
                .companyPoid(header.getCompanyPoid())
                .receiptAmount(responseReceiptAmount)
                .invoiceAmount(header.getInvoiceAmount())
                .currencyCode(header.getCurrencyCode())
                .currencyRate(currencyRate)
                .bhdAmount(bhdAmount)
                .receivedFrom(header.getRcvdFromDtlPrint())
                .creditGL(creditGL)
                .refType(header.getRefType())
                .docRef(header.getDocRef())
                .narration(header.getRemarks())
                .printTitle(printTitle)
                .approvalStatus(approvalStatus)
                .verified(header.getVerified())
                .multicompany(header.getMulticompany())
                .extraChargesFlag(header.getExtraCharges())
                .createdBy(header.getCreatedBy())
                .createdDate(header.getCreatedDate())
                .payments(convertPaymentDetailsToDto(header.getPaymentDetails()))
                .bills(convertBillDetailsToDto(header.getBillDetails()))
                .extraCharges(convertChargeDetailsToDto(header.getChargesDetails()))
                .advances(convertAdvanceDetailsToDto(header.getAdvanceDetails()))
                .printDocCompId(header.getPrintDocCompId())
                .build();
    }

    private List<GeneralReceiptPaymentDto> convertPaymentDetailsToDto(List<ArGenReceiptPymtDetails> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .map(detail -> {
                    GeneralReceiptPaymentDto dto = new GeneralReceiptPaymentDto();
                    dto.setDetRowId(detail.getDetRowId());
                    dto.setType(detail.getPymtType());
                    dto.setAmount(detail.getAmount());
                    dto.setChequeNo(detail.getChqCardno());
                    dto.setChequeDate(detail.getChqDate());
                    dto.setBankPoid(detail.getBankPoid());
                    if (detail.getBankPoid() != null) {
                        try {
                            LovGetListDto bankLov = lovService.getDetailsByPoidAndLovName(detail.getBankPoid(), "ARCUSTBANKRCPT");
                            if (bankLov != null && bankLov.getDescription() != null) {
                                dto.setBank(bankLov.getDescription());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch bank name for bankPoid {}: {}", detail.getBankPoid(), e.getMessage());
                        }
                    }
                    dto.setAccountNumber(detail.getAccountNo());
                    dto.setAccountName(detail.getAccountName());
                    dto.setAccountPoid(detail.getAccountPoid());
                    dto.setTtBankPoid(detail.getTtBankPoid());
                    dto.setTtRef(detail.getTtRef());
                    dto.setCreditCardRef(detail.getCreditCardRef());
                    dto.setCardType(detail.getCardType());
                    dto.setCardPoid(detail.getCardPoid());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<GeneralReceiptBillDto> convertBillDetailsToDto(List<ArGenReceiptBillDtl> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .map(detail -> {
                    GeneralReceiptBillDto dto = new GeneralReceiptBillDto();
                    dto.setDetRowId(detail.getDetRowId());
                    dto.setBillReference(detail.getBillRefno());
                    dto.setAmount(detail.getAmount());
                    dto.setDrCr(detail.getCrDrType());
                    dto.setRemarks(detail.getRemarks());
                    dto.setBillRefType(detail.getBillRefType());
                    dto.setBillDueDate(detail.getBillDueDate());
                    dto.setGlPoid(detail.getGlPoid());
                    dto.setGlCompanyPoid(detail.getGlCompanyPoid());
                    dto.setDescription(detail.getDescription());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<GeneralReceiptChargeDto> convertChargeDetailsToDto(List<ArGenReceiptChargesDtl> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .map(detail -> {
                    GeneralReceiptChargeDto dto = new GeneralReceiptChargeDto();
                    dto.setDetRowId(detail.getDetRowId());
                    dto.setChargeType(detail.getChargeType());
                    // Get GL code from GL POID
                    String glCode = glMastersRepository.findById(detail.getGlPoid())
                            .map(GLMasterEntity::getGlCode)
                            .orElse(null);
                    dto.setGl(glCode);
                    dto.setAmount(detail.getAmount());
                    dto.setBhdAmount(detail.getBhdAmount());
                    dto.setTaxPoid(detail.getTaxPoid());
                    dto.setTaxPercent(detail.getTaxPercentage());
                    dto.setTaxAmount(detail.getTaxAmount());
                    dto.setTotalAmount(detail.getTotalAmount());
                    // Get cost center details from cost POID using LOV service
                    if (detail.getCostPoid() != null && !detail.getCostPoid().isBlank()) {
                        try {
                            Long costPoid = Long.parseLong(detail.getCostPoid());
                            LovGetListDto costCenterLov = lovService.getDetailsByPoidAndLovName(costPoid, "AR_GEN_REC_COST_CENTER");
                            if (costCenterLov != null) {
                                dto.setCostCenter(costCenterLov.getCode());
                                dto.setCostCenterDetails(costCenterLov);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch cost center for costPoid {}: {}", detail.getCostPoid(), e.getMessage());
                        }
                    }
                    dto.setRemarks(detail.getRemarks());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<GeneralReceiptAdvanceDto> convertAdvanceDetailsToDto(List<ArGenReceiptAdvanceDtl> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .map(detail -> {
                    GeneralReceiptAdvanceDto dto = new GeneralReceiptAdvanceDto();
                    dto.setAdvanceRefDocId(detail.getAdvanceRefDocId());
                    dto.setAdvanceRefPoid(detail.getAdvanceRefPoid());
                    dto.setAmount(detail.getAmount());
                    dto.setRemarks(detail.getRemarks());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String getCurrentUser() {
        return ASGHelperUtils.getCurrentUser();
    }

    @Override
    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(
                docId,
                filterList,
                operator,
                pageable,
                isDeleted,
                "TRANSACTION_POID",
                "DOC_REF"
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public String getChargeGLAccount(String chargeType) {
        try {
            Long userPoid = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
            Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
            log.info("Fetching GL account for charge type: {} with userPoid: {}, companyPoid: {}", chargeType, userPoid, companyPoid);
            String glPoid = procedureRepository.fetchChargeGLAccount(DEFAULT_GROUP_POID, companyPoid, userPoid, chargeType);
            log.info("GL account fetched: {}", glPoid);
            return glPoid;
        } catch (Exception e) {
            log.error("Error fetching charge GL account for type {}: {}", chargeType, e.getMessage(), e);
            throw new ValidationException("Failed to fetch GL account for charge type: " + chargeType + ". Error: " + e.getMessage());
        }
    }

    @Override
    public String getGlBillwiseYn(Long glPoid) {
        try {
            log.info("Fetching billwise flag for GL: {}", glPoid);
            String result = procedureRepository.fetchGLBillwiseFlag(glPoid);

            if (result == null || result.trim().isEmpty()) {
                return "N";
            }

            return result.trim();
        } catch (Exception e) {
            log.error("Error fetching billwise flag for GL {}: {}", glPoid, e.getMessage(), e);
            throw new ValidationException("Failed to fetch billwise flag for GL: " + glPoid + ". Error: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPendingBills(Long glPoid, LocalDate asOnDate) {
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
        LocalDate sqlDate = asOnDate != null ? asOnDate : LocalDate.now();
        List<Object[]> results = procedureRepository.fetchPendingBills(DEFAULT_GROUP_POID, companyPoid, glPoid, sqlDate);
        return Map.of("pendingBills", results);
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-105");
        params.put("BILL_SUBREPORT",  printService.load("Finance/AR/GEN_RECEIPT_BILL_subreport1.jrxml"));
        params.put("CHARGES_SUBREPORT",  printService.load("Finance/AR/GEN_RECEIPT_CHARGES_subreport1.jrxml"));
        params.put("PAYMENT_SUBREPORT",  printService.load("Finance/AR/GEN_RECEIPT_PAYMENT_DETAIL_subreport1.jrxml"));
        JasperReport mainReport = printService.load("Finance/AR/GEN_RECEIPT.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
}

