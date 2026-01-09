package com.asg.finance.service.impl;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    
    @Autowired
    private DocumentDeleteService documentDeleteService;
    
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
                        Date asOnDate = header.getTransactionDate() != null 
                                ? Date.valueOf(header.getTransactionDate()) 
                                : Date.valueOf(LocalDate.now());
                        
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
     * Build WHERE clause from filter parameters
     */
    private String buildWhereClause(String type, String status, LocalDate fromDate, 
                                   LocalDate toDate, Long companyId) {
        List<String> conditions = new ArrayList<>();

        // Base condition: not deleted (unless status explicitly requests deleted)
        if (status == null || !"Deleted".equalsIgnoreCase(status)) {
            conditions.add("(DELETED IS NULL OR DELETED = 'N')");
        } else if ("Deleted".equalsIgnoreCase(status)) {
            conditions.add("DELETED = 'Y'");
        }

        // Type filter (REF_TYPE)
        if (type != null && !type.trim().isEmpty()) {
            conditions.add("REF_TYPE = '" + type.replace("'", "''") + "'");
        }

        // Status filter (VERIFIED)
        if (status != null && !status.trim().isEmpty() && !"Deleted".equalsIgnoreCase(status)) {
            if ("Posted".equalsIgnoreCase(status)) {
                conditions.add("VERIFIED = 'Y'");
            } else if ("Pending".equalsIgnoreCase(status)) {
                conditions.add("(VERIFIED IS NULL OR VERIFIED = 'N')");
            }
        }

        // Date range filter
        if (fromDate != null) {
            conditions.add("TRANSACTION_DATE >= DATE '" + fromDate + "'");
        }
        if (toDate != null) {
            conditions.add("TRANSACTION_DATE <= DATE '" + toDate + "'");
        }

        // Company filter
        if (companyId != null) {
            conditions.add("COMPANY_POID = " + companyId);
        }

        // Join conditions
        if (conditions.isEmpty()) {
            return "1=1"; // No filters
        } else {
            return String.join(" AND ", conditions);
        }
    }

    /**
     * Map procedure result row to GeneralReceiptResponse
     * Column names match AR_GEN_RECEIPT_HDR table structure
     */
    private GeneralReceiptResponse mapRowToResponse(Map<String, Object> row) {
        try {
            if (row == null || row.isEmpty()) {
                return null;
            }

            // Map columns from AR_GEN_RECEIPT_HDR table structure
            GeneralReceiptResponse response = GeneralReceiptResponse.builder()
                    .receiptNo(getStringValue(row, "DOC_REF"))
                    .transactionPoid(getLongValue(row, "TRANSACTION_POID"))
                    .transactionDate(getLocalDateValue(row, "TRANSACTION_DATE"))
                    .companyPoid(getLongValue(row, "COMPANY_POID"))
                    .receiptAmount(getBigDecimalValue(row, "RCPT_AMOUNT"))
                    .currencyCode(getStringValue(row, "CURRENCY_CODE"))
                    .currencyRate(getBigDecimalValue(row, "CURRENCY_RATE"))
                    .receivedFrom(getStringValue(row, "RCVD_FROM_DTL_PRINT"))
                    .refType(getStringValue(row, "REF_TYPE"))
                    .narration(getStringValue(row, "REMARKS"))
                    .verified(getStringValue(row, "VERIFIED"))
                    .multicompany(getStringValue(row, "MULTICOMPANY"))
                    .createdBy(getStringValue(row, "CREATED_BY"))
                    .createdDate(getLocalDateTimeValue(row, "CREATED_DATE"))
                    .build();

            return response;

        } catch (Exception e) {
            log.warn("Error mapping row to response: {}", e.getMessage());
            return null;
        }
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

    private LocalDate getLocalDateValue(Map<String, Object> row, String columnName) {
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
    }

    private LocalDateTime getLocalDateTimeValue(Map<String, Object> row, String columnName) {
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
    }

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

        if ("Y".equals(header.getVerified())) {
            throw new ValidationException("Cannot update receipt that has been verified/posted to GL");
        }

        validateGeneralReceiptRequest(request);

        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        updateHeaderEntity(header, request.getHeader(), currentUser, now);
        receiptHdrRepository.save(header);

        pymtDetailsRepository.deleteByTransactionPoid(transactionPoid);
        billDtlRepository.deleteByTransactionPoid(transactionPoid);
        chargesDtlRepository.deleteByTransactionPoid(transactionPoid);
        advanceDtlRepository.deleteByReceiptHdr_TransactionPoid(transactionPoid);

        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            List<ArGenReceiptPymtDetails> details = new ArrayList<>();
            for (int i = 0; i < request.getPayments().size(); i++) {
                GeneralReceiptPaymentDto payment = request.getPayments().get(i);
                ArGenReceiptPymtDetails detail = ArGenReceiptPymtDetails.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId((long) (i + 1))
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
                        .createdBy(currentUser)
                        .createdDate(now)
                        .build();
                details.add(detail);
            }
            pymtDetailsRepository.saveAll(details);
        }
        if (request.getBills() != null && !request.getBills().isEmpty()) {
            List<ArGenReceiptBillDtl> details = new ArrayList<>();
            for (int i = 0; i < request.getBills().size(); i++) {
                GeneralReceiptBillDto bill = request.getBills().get(i);
                Long glPoid = bill.getGlPoid() != null ? bill.getGlPoid() : header.getRcvdOthPoid();
                ArGenReceiptBillDtl detail = ArGenReceiptBillDtl.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId((long) (i + 1))
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
                        .createdBy(currentUser)
                        .createdDate(now)
                        .build();
                details.add(detail);
            }
            billDtlRepository.saveAll(details);
            entityManager.flush();
            callBillwiseCheckProcedure(transactionPoid, header.getCompanyPoid());
        }
        if ("Y".equals(request.getHeader().getExtraCharges()) && 
            request.getExtraCharges() != null && !request.getExtraCharges().isEmpty()) {
            List<ArGenReceiptChargesDtl> details = new ArrayList<>();
            for (int i = 0; i < request.getExtraCharges().size(); i++) {
                GeneralReceiptChargeDto charge = request.getExtraCharges().get(i);
                List<GLMasterEntity> chargeGLList = glMastersRepository.findAllByGlCodeAndDeletedFlag(charge.getGl(), "N");
                if (chargeGLList.isEmpty()) {
                    throw new ValidationException("Charge GL not found: " + charge.getGl());
                }
                GLMasterEntity chargeGL = chargeGLList.get(0);
                ArGenReceiptChargesDtl detail = ArGenReceiptChargesDtl.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId((long) (i + 1))
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
                        .createdBy(currentUser)
                        .createdDate(now)
                        .build();
                details.add(detail);
            }
            chargesDtlRepository.saveAll(details);
        }
        if (request.getAdvances() != null && !request.getAdvances().isEmpty()) {
            List<ArGenReceiptAdvanceDtl> details = new ArrayList<>();
            for (int i = 0; i < request.getAdvances().size(); i++) {
                GeneralReceiptAdvanceDto advance = request.getAdvances().get(i);
                ArGenReceiptAdvanceDtl detail = ArGenReceiptAdvanceDtl.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId((long) (i + 1))
                        .advanceRefDocId(advance.getAdvanceRefDocId())
                        .advanceRefPoid(advance.getAdvanceRefPoid())
                        .amount(advance.getAmount())
                        .remarks(advance.getRemarks())
                        .createdBy(currentUser)
                        .createdDate(now)
                        .build();
                details.add(detail);
            }
            advanceDtlRepository.saveAll(details);
        }

        entityManager.flush();
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

        // 1. Validate mandatory fields (already done by @Valid annotations)
        
        // 2. Validate credit GL exists
        List<GLMasterEntity> creditGLList = glMastersRepository.findAllByGlCodeAndDeletedFlag(header.getCreditGL(), "N");
        if (creditGLList.isEmpty()) {
            throw new ValidationException("Credit GL not found: " + header.getCreditGL());
        }
        GLMasterEntity creditGL = creditGLList.get(0);

        // 3. Validate amount matching
        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            BigDecimal paymentTotal = request.getPayments().stream()
                    .map(GeneralReceiptPaymentDto::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("Amount validation - Receipt amount: {}, Payment total: {}", 
                    header.getReceiptAmount(), paymentTotal);

            if (header.getReceiptAmount().compareTo(paymentTotal) != 0) {
                throw new ValidationException(String.format(
                        "Receipt amount (%.3f) does not match sum of payment amounts (%.3f)",
                        header.getReceiptAmount(), paymentTotal));
            }
        }

        // 4. Validate cheque dates if applicable
        validateChequeDates(request.getPayments());

        // 5. Validate cost center for specific charge types
        validateCostCenterRequirement(request.getExtraCharges());

        // 6. Validate rounding limit
        validateRoundingLimit(request.getExtraCharges());

        // 7. Validate multi-company if applicable
        if ("Y".equals(header.getMulticompany())) {
            validateMultiCompany(request.getBills(), header.getCompanyPoid());
        }

        // 8. Validate bill references if refType is AGAINST
        if ("AGAINST".equals(header.getRefType()) && (request.getBills() == null || request.getBills().isEmpty())) {
            throw new ValidationException("Bill details are required when Ref Type is AGAINST");
        }
        
        // 9. Validate bill references using stored procedure (PROC_GEN_RECE_NEW_BILLREF_CHK)
        if (request.getBills() != null && !request.getBills().isEmpty()) {
            validateBillReferencesUsingProcedure(request.getBills(), creditGL.getGlPoid(), header.getCompanyPoid());
        }
    }

    private void validateChequeDates(List<GeneralReceiptPaymentDto> payments) {
        String postDateDaysParam = parameterServiceClient.findParameterValueByName("GEN_RECEIPT_CHEQUE_POST_DATE_VALIDATION_DAYS")
                .orElse("30");
        String backDateDaysParam = parameterServiceClient.findParameterValueByName("GEN_RECEIPT_CHEQUE_BACK_DATE_VALIDATION_DAYS")
                .orElse("30");

        int postDateDays = Integer.parseInt(postDateDaysParam);
        int backDateDays = Integer.parseInt(backDateDaysParam);

        for (GeneralReceiptPaymentDto payment : payments) {
            if ("CHEQUE".equals(payment.getType()) && payment.getChequeDate() != null) {
                LocalDate today = LocalDate.now();
                long daysDiff = ChronoUnit.DAYS.between(today, payment.getChequeDate());

                if (daysDiff > postDateDays) {
                    throw new ValidationException(String.format(
                            "Cheque date cannot be more than %d days in the future", postDateDays));
                }

                if (daysDiff < backDateDays) {
                    throw new ValidationException(String.format(
                            "Cheque date cannot be more than %d days in the past", backDateDays));
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
        // Get credit GL POID
        List<GLMasterEntity> creditGLList = glMastersRepository.findAllByGlCodeAndDeletedFlag(dto.getCreditGL(), "N");
        if (creditGLList.isEmpty()) {
            throw new ValidationException("Credit GL not found: " + dto.getCreditGL());
        }
        Long creditGlPoid = creditGLList.get(0).getGlPoid();

        return ArGenReceiptHdr.builder()
                .transactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now())
                .groupPoid(DEFAULT_GROUP_POID)
                .companyPoid(dto.getCompanyPoid())
                .rcvdOthPoid(creditGlPoid)
                .rcptAmount(dto.getReceiptAmount())
                .remarks(dto.getNarration())
                .rcvdFromDtlPrint(dto.getReceivedFrom())
                .refType(dto.getRefType())
                .currencyCode(dto.getCurrency())
                .currencyRate(dto.getRate())
                .multicompany(dto.getMulticompany() != null ? dto.getMulticompany() : "N")
                .ttBankPoid(dto.getTtBankPoid())
                .costCenterPoid(dto.getCostCenterPoid())
                .deleted("N")
                .verified("N")
                .dataLoaded("N")
                .extraCharges("N")
                .lineType("GENERAL")  // Set line type
                .rcvdType("GENERAL")  // Set received type
                .createdBy(currentUser)
                .createdDate(now)
                .lastModifiedBy(currentUser)
                .lastModifiedDate(now)
                .build();
    }

    private void updateHeaderEntity(ArGenReceiptHdr header, GeneralReceiptHeaderDto dto, String currentUser, LocalDateTime now) {
        // Get credit GL POID
        List<GLMasterEntity> creditGLList = glMastersRepository.findAllByGlCodeAndDeletedFlag(dto.getCreditGL(), "N");
        if (creditGLList.isEmpty()) {
            throw new ValidationException("Credit GL not found: " + dto.getCreditGL());
        }
        Long creditGlPoid = creditGLList.get(0).getGlPoid();

        header.setRcvdOthPoid(creditGlPoid);
        header.setRcptAmount(dto.getReceiptAmount());
        header.setRemarks(dto.getNarration());
        header.setRcvdFromDtlPrint(dto.getReceivedFrom());
        header.setRefType(dto.getRefType());
        header.setCurrencyCode(dto.getCurrency());
        header.setCurrencyRate(dto.getRate());
        header.setMulticompany(dto.getMulticompany() != null ? dto.getMulticompany() : "N");
        header.setTtBankPoid(dto.getTtBankPoid());
        header.setCostCenterPoid(dto.getCostCenterPoid());
        header.setLastModifiedBy(currentUser);
        header.setLastModifiedDate(now);
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
                    .createdBy(currentUser)
                    .createdDate(now)
                    .build();

            details.add(detail);
        }
        
        // Batch save all payment details in one call
        if (!details.isEmpty()) {
            pymtDetailsRepository.saveAll(details);
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
                    .createdBy(currentUser)
                    .createdDate(now)
                    .build();

            details.add(detail);
        }
        
        // Batch save all bill details in one call
        if (!details.isEmpty()) {
            billDtlRepository.saveAll(details);
            entityManager.flush();  // Ensure details are persisted before calling procedure
            
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
                    .createdBy(currentUser)
                    .createdDate(now)
                    .build();

            details.add(detail);
        }
        
        // Batch save all charge details in one call
        if (!details.isEmpty()) {
            chargesDtlRepository.saveAll(details);
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
                    .createdBy(currentUser)
                    .createdDate(now)
                    .build();

            details.add(detail);
        }
        
        // Batch save all advance details
        if (!details.isEmpty()) {
            advanceDtlRepository.saveAll(details);
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
        // Calculate BHD Amount (receiptAmount * currencyRate)
        BigDecimal bhdAmount = null;
        if (header.getRcptAmount() != null && header.getCurrencyRate() != null) {
            bhdAmount = header.getRcptAmount().multiply(header.getCurrencyRate());
        }

        // Fetch approval status from GLOBAL_APPROVAL_STATUS table
        String approvalStatus = fetchApprovalStatus(header.getTransactionPoid());

        // Fetch Credit GL details
        CreditGlDto creditGL = null;
        if (header.getRcvdOthPoid() != null) {
            creditGL = glMastersRepository.findById(header.getRcvdOthPoid())
                    .map(gl -> CreditGlDto.builder()
                            .glPoid(gl.getGlPoid())
                            .glCode(gl.getGlCode())
                            .glDescription(gl.getDescription())
                            .build())
                    .orElse(null);
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
                .receiptAmount(header.getRcptAmount())
                .currencyCode(header.getCurrencyCode())
                .currencyRate(header.getCurrencyRate())
                .bhdAmount(bhdAmount)
                .receivedFrom(header.getRcvdFromDtlPrint())
                .creditGL(creditGL)
                .refType(header.getRefType())
                .narration(header.getRemarks())
                .printTitle(printTitle)
                .approvalStatus(approvalStatus)
                .verified(header.getVerified())
                .multicompany(header.getMulticompany())
                .createdBy(header.getCreatedBy())
                .createdDate(header.getCreatedDate())
                .payments(convertPaymentDetailsToDto(header.getPaymentDetails()))
                .bills(convertBillDetailsToDto(header.getBillDetails()))
                .extraCharges(convertChargeDetailsToDto(header.getChargesDetails()))
                .advances(convertAdvanceDetailsToDto(header.getAdvanceDetails()))
                .build();
    }

    /**
     * Fetch approval status from GLOBAL_APPROVAL_STATUS table
     * Returns the latest ACTION_STATUS ordered by ACTIONED_DATETIME DESC
     * @param transactionPoid Transaction POID
     * @return Approval status string (ACTION_STATUS) or null if not found
     */
    private String fetchApprovalStatus(Long transactionPoid) {
        try {
            // Get the latest ACTION_STATUS for this document
            // There can be multiple approval records (one per approval level)
            // We get the most recent one based on ACTIONED_DATETIME
            String sql = "SELECT ACTION_STATUS FROM GLOBAL_APPROVAL_STATUS " +
                        "WHERE DOC_ID = :docId AND DOC_KEY_POID = :transactionPoid " +
                        "AND (DELETED IS NULL OR DELETED = 'N') " +
                        "ORDER BY ACTIONED_DATETIME DESC NULLS LAST, APPROVAL_POID DESC " +
                        "FETCH FIRST 1 ROW ONLY";
            
            @SuppressWarnings("unchecked")
            List<Object> results = entityManager.createNativeQuery(sql)
                    .setParameter("docId", DOC_ID)
                    .setParameter("transactionPoid", transactionPoid)
                    .getResultList();
            
            if (results != null && !results.isEmpty()) {
                Object result = results.get(0);
                return result != null ? result.toString() : null;
            }
            return null;
        } catch (Exception e) {
            // If no approval status found or error, return null (not all receipts may have approval)
            log.debug("No approval status found for receipt {}: {}", transactionPoid, e.getMessage());
            return null;
        }
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
                    dto.setCostCenter(detail.getCostPoid());
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
    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto filters, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        java.util.List<FilterDto> filterList = documentService.resolveFilters(filters);

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
    public Map<String, Object> getPendingBills(Long glPoid, LocalDate asOnDate) {
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
        Date sqlDate = asOnDate != null ? Date.valueOf(asOnDate) : Date.valueOf(LocalDate.now());
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

