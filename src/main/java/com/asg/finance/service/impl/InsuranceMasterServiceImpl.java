package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.dto.masters.*;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.entity.master.*;
import com.asg.finance.repository.GlobalLogDetailsRepository;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.finance.repository.master.HrEmployeeMasterRepository;
import com.asg.finance.repository.master.InsuranceMasterRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.InsuranceMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class InsuranceMasterServiceImpl implements InsuranceMasterService {

    private final InsuranceMasterRepository insuranceMasterRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final RoleServiceClient roleServiceClient;
    private final LoggingService loggingService;
    private final GlobalLogSummaryRepository globalLogSummaryRepository;
    private final HrEmployeeMasterRepository hrEmployeeMasterRepository;
    private final LovDataService lovService;

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    public InsuranceMasterResponseDto getInsuranceMasterById(Long insuranceId) {
        InsuranceMaster insuranceMaster = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));
        return mapToResponseDto(insuranceMaster);
    }

    @Override
    public Map<String, Object> listInsuranceMasters(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters,"FROM_DATE",startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "POLICY_NO",
                "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public void softDeleteInsuranceMaster(Long insuranceId, DeleteReasonDto deleteReasonDto) {
        InsuranceMaster insuranceMaster = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        if (insuranceMasterRepository.hasPjReference(insuranceId)) {
            throw new ValidationException("Cannot delete — Insurance linked with Purchase Journal Reference");
        }
        
        documentDeleteService.deleteDocument(
                insuranceId,
                "GLOBAL_INSURANCE_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                insuranceMaster.getCreatedDate() != null ? insuranceMaster.getCreatedDate().toLocalDate() : null
        );
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto createInsuranceMaster(InsuranceMasterRequestDto request) {
        try {
            System.out.println("Creating insurance master for policy: " + request.getPolicyNo());

            // Validate insurance type against LOV
            List<String> validInsuranceTypes = Arrays.asList(
                "VEHICLE_INSURANCE", "MEDICAL_INSURANCE", "PROPERTY_INSURANCE", 
                "TRAVEL_INSURANCE", "PROJECTS_INSURANCE", "RO_RO_INSURANCE", 
                "EQUIPMENT_INSURANCE", "CUSTOMS_CLEARANCE_INSURANCE", "IT_INSURANCE", 
                "LIFE_INSURANCE", "CYBER_SECURITY_INSURANCE", "PROFESSIONAL_INDEMNITY_INSURANCE", 
                "FORWARDING_LOGISTICS_INSURANCE"
            );
            if (!validInsuranceTypes.contains(request.getInsuranceType())) {
                throw new ValidationException("Invalid insurance type. Must be one of: " + String.join(", ", validInsuranceTypes));
            }

            // Validate insurance category
            if (!request.getCategory().matches("^(GROUP|COMPANY|INDIVIDUAL)$")) {
                throw new ValidationException("Insurance category must be GROUP, COMPANY, or INDIVIDUAL");
            }

            // Validate unique policy number per company
            if (insuranceMasterRepository.existsByPolicyNoAndCompanyPoid(request.getPolicyNo(), 1L)) {
                throw new ValidationException("Policy Number must be unique per company");
            }

            // Validate header dates
            if (request.getFromDate() == null || request.getExpiryDate() == null) {
                throw new ValidationException("From Date and Expiry Date cannot be null");
            }

            if (request.getFromDate().isAfter(request.getExpiryDate())) {
                throw new ValidationException("From Date cannot be greater than Expiry Date");
            }

            // Validate Employee Details
            if (request.getEmployeeDetails() != null) {
                for (InsuranceEmployeeDetailRequestDto emp : request.getEmployeeDetails()) {
                    String action = normalizeAction(emp.getActionType());
                    if ("ISDELETED".equals(action)) continue;
                    
                    if (emp.getEmployeePoid() == null || emp.getEmployeePoid() <= 0) {
                        throw new ValidationException("Employee is mandatory in Employee Details");
                    }
                }
            }

            // Validate Property Details
            if (request.getPropertyDetails() != null) {
                for (InsurancePropertyDetailRequestDto prop : request.getPropertyDetails()) {
                    String action = normalizeAction(prop.getActionType());
                    if ("ISDELETED".equals(action)) continue;
                    
                    if (prop.getPropertyPoid() == null || prop.getPropertyPoid() <= 0) {
                        throw new ValidationException("Property is mandatory in Property Details");
                    }
                }
            }

            // Validate PIC dates (if present)
            if (request.getPicDetails() != null) {
                for (InsurancePicDetailRequestDto pic : request.getPicDetails()) {
                    String action = normalizeAction(pic.getActionType());
                    if ("ISDELETED".equals(action)) continue;
                    
                    // Validate Role is provided
                    if (pic.getRolePoid() == null || pic.getRolePoid() <= 0) {
                        throw new ValidationException("Role is mandatory in PIC Details");
                    }

                    // Validate PIC from ≤ to (only if toDate is provided)
                    if (pic.getFromDate() != null && pic.getToDate() != null && pic.getFromDate().isAfter(pic.getToDate())) {
                        throw new ValidationException("PIC From Date cannot be greater than PIC To Date");
                    }
                }
            }

            InsuranceMaster insuranceMaster = InsuranceMaster.builder()
                    .groupPoid(1L)
                    .companyPoid(1L)
                    .insuranceType(request.getInsuranceType())
                    .insuranceCategory(request.getCategory())
                    .policyNo(request.getPolicyNo())
                    .insuranceProvider(request.getInsuranceProvider())
                    .fromDate(request.getFromDate())
                    .expiryDate(request.getExpiryDate())
                    .currencyPoid(request.getCurrency())
                    .exchangeRate(request.getRate())
                    .insuranceAmount(request.getInsuranceAmount())
                    .premiumAmount(request.getPremiumAmount())
                    .paymentFrequency(request.getPaymentFrequency())
                    .oneTime("N")
                    .description(request.getDescription())
                    .faPoid(request.getFaPoid())
                    .deleted("N")
                    .build();

            InsuranceMaster saved = insuranceMasterRepository.save(insuranceMaster);
            buildAndSetChildDetails(request, saved);
            InsuranceMaster finalSaved = insuranceMasterRepository.save(saved);
            
            String docId = UserContext.getDocumentId();
            String docKeyPoid = finalSaved.getTransactionPoid().toString();
            LocalDateTime now = LocalDateTime.now();
            
            // Log header creation
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, docKeyPoid);
            
            // Log grid row creations
            List<GlobalLogSummary> gridLogs = new ArrayList<>();
            if (finalSaved.getEmployeeDetails() != null) {
                for (InsuranceEmployeeDetail detail : finalSaved.getEmployeeDetails()) {
                    String msg = String.format("Row Created on Insurance Employee Detail with DetRowId: %s", detail.getDetRowId());
                    gridLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, msg, now));
                }
            }
            if (finalSaved.getPropertyDetails() != null) {
                for (InsurancePropertyDetail detail : finalSaved.getPropertyDetails()) {
                    String msg = String.format("Row Created on Insurance Property Detail with DetRowId: %s", detail.getDetRowId());
                    gridLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, msg, now));
                }
            }
            if (finalSaved.getPicDetails() != null) {
                for (InsurancePicDetail detail : finalSaved.getPicDetails()) {
                    String msg = String.format("Row Created on Insurance PIC Detail with DetRowId: %s", detail.getDetRowId());
                    gridLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, msg, now));
                }
            }
            if (!gridLogs.isEmpty()) {
                globalLogSummaryRepository.saveAll(gridLogs);
            }
            
            return mapToResponseDto(finalSaved);
        } catch (Exception e) {
            System.err.println("Error in createInsuranceMaster: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto updateInsuranceMaster(Long insuranceId, InsuranceMasterRequestDto request) {
        InsuranceMaster existing = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        // Create copy of old entity for logging
        InsuranceMaster oldEntity = new InsuranceMaster();
        BeanUtils.copyProperties(existing, oldEntity);
        
        // Snapshot child details for logging
        List<InsuranceEmployeeDetail> oldEmployeeDetails = snapshotEmployeeDetails(existing.getEmployeeDetails());
        List<InsurancePropertyDetail> oldPropertyDetails = snapshotPropertyDetails(existing.getPropertyDetails());
        List<InsurancePicDetail> oldPicDetails = snapshotPicDetails(existing.getPicDetails());

        // Validate insurance type against LOV
        List<String> validInsuranceTypes = Arrays.asList(
            "VEHICLE_INSURANCE", "MEDICAL_INSURANCE", "PROPERTY_INSURANCE", 
            "TRAVEL_INSURANCE", "PROJECTS_INSURANCE", "RO_RO_INSURANCE", 
            "EQUIPMENT_INSURANCE", "CUSTOMS_CLEARANCE_INSURANCE", "IT_INSURANCE", 
            "LIFE_INSURANCE", "CYBER_SECURITY_INSURANCE", "PROFESSIONAL_INDEMNITY_INSURANCE", 
            "FORWARDING_LOGISTICS_INSURANCE"
        );
        if (!validInsuranceTypes.contains(request.getInsuranceType())) {
            throw new ValidationException("Invalid insurance type. Must be one of: " + String.join(", ", validInsuranceTypes));
        }

        // Validate insurance category
        if (!request.getCategory().matches("^(GROUP|COMPANY|INDIVIDUAL)$")) {
            throw new ValidationException("Insurance category must be GROUP, COMPANY, or INDIVIDUAL");
        }

        // Validate unique policy number (excluding current record)
        if (insuranceMasterRepository.existsByPolicyNoAndDeletedAndTransactionPoidNot(
                request.getPolicyNo(), "N", insuranceId)) {
            throw new ValidationException("Policy Number must be unique");

        }

        // Validate header dates
        if (request.getFromDate() == null || request.getExpiryDate() == null) {
            throw new ValidationException("From Date and Expiry Date cannot be null");
        }

        if (request.getFromDate().isAfter(request.getExpiryDate())) {
            throw new ValidationException("From Date cannot be greater than Expiry Date");
        }

        // Validate Employee Details
        if (request.getEmployeeDetails() != null) {
            for (InsuranceEmployeeDetailRequestDto emp : request.getEmployeeDetails()) {
                String action = normalizeAction(emp.getActionType());
                if ("ISDELETED".equals(action)) continue;
                
                if (emp.getEmployeePoid() == null || emp.getEmployeePoid() <= 0) {
                    throw new ValidationException("Employee is mandatory in Employee Details");
                }
            }
        }

        // Validate Property Details
        if (request.getPropertyDetails() != null) {
            for (InsurancePropertyDetailRequestDto prop : request.getPropertyDetails()) {
                String action = normalizeAction(prop.getActionType());
                if ("ISDELETED".equals(action)) continue;
                
                if (prop.getPropertyPoid() == null || prop.getPropertyPoid() <= 0) {
                    throw new ValidationException("Property is mandatory in Property Details");
                }
            }
        }

        // Validate PIC dates (if present)
        if (request.getPicDetails() != null) {
            for (InsurancePicDetailRequestDto pic : request.getPicDetails()) {
                String action = normalizeAction(pic.getActionType());
                if ("ISDELETED".equals(action)) continue;
                
                // Validate Role is provided
                if (pic.getRolePoid() == null || pic.getRolePoid() <= 0) {
                    throw new ValidationException("Role is mandatory in PIC Details");
                }

                // Validate PIC from ≤ to (only if toDate is provided)
                if (pic.getFromDate() != null && pic.getToDate() != null && pic.getFromDate().isAfter(pic.getToDate())) {
                    throw new ValidationException("PIC From Date cannot be greater than PIC To Date");
                }
            }
        }



        // Update fields (preserve pjRefPoid if it exists)
        existing.setInsuranceType(request.getInsuranceType());
        existing.setInsuranceCategory(request.getCategory());
        existing.setPolicyNo(request.getPolicyNo());
        existing.setInsuranceProvider(request.getInsuranceProvider());
        existing.setFromDate(request.getFromDate());
        existing.setExpiryDate(request.getExpiryDate());
        existing.setCurrencyPoid(request.getCurrency());
        existing.setExchangeRate(request.getRate());
        existing.setInsuranceAmount(request.getInsuranceAmount());
        existing.setPremiumAmount(request.getPremiumAmount());
        existing.setPaymentFrequency(request.getPaymentFrequency());
        existing.setDescription(request.getDescription());
        existing.setFaPoid(request.getFaPoid());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        // Update child details properly
        existing.getEmployeeDetails().removeIf(e -> true);
        existing.getPropertyDetails().removeIf(e -> true);
        existing.getPicDetails().removeIf(e -> true);

        buildAndSetChildDetails(request, existing);
        if (existing.getPjRefPoid() != null) {
            existing.setPjRefPoid(existing.getPjRefPoid());
        } else {
            existing.setPjRefPoid(null);
        }

        InsuranceMaster updated = insuranceMasterRepository.save(existing);
        
        String docId = UserContext.getDocumentId();
        String docKeyPoid = insuranceId.toString();
        
        // Log header changes
        loggingService.logChanges(oldEntity, updated, InsuranceMaster.class, docId, docKeyPoid, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        
        // Log child record changes using batch logging
        List<LogRequestDto<InsuranceEmployeeDetail>> empLogRequests = new ArrayList<>();
        List<LogRequestDto<InsurancePropertyDetail>> propLogRequests = new ArrayList<>();
        List<LogRequestDto<InsurancePicDetail>> picLogRequests = new ArrayList<>();
        
        // Process Employee Details
        if (request.getEmployeeDetails() != null) {
            int newEmpIndex = 0;
            for (InsuranceEmployeeDetailRequestDto dto : request.getEmployeeDetails()) {
                String action = normalizeAction(dto.getActionType());
                if ("ISDELETED".equals(action)) {
                    loggingService.logDelete(dto, docId, docKeyPoid);
                } else if ("ISCREATED".equals(action)) {
                    InsuranceEmployeeDetail newE = findNewEmployeeByIndex(updated.getEmployeeDetails(), oldEmployeeDetails, newEmpIndex++);
                    if (newE != null) {
                        String msg = String.format("Row Created on Insurance Employee Detail with detRowId: %s", newE.getDetRowId());
                        loggingService.createLogSummaryEntry(docId, docKeyPoid, msg);
                    }
                } else if ("ISUPDATED".equals(action)) {
                    InsuranceEmployeeDetail oldE = findOldEmployeeByDetRowId(oldEmployeeDetails, dto.getDetRowId());
                    InsuranceEmployeeDetail newE = findOldEmployeeByDetRowId(updated.getEmployeeDetails(), dto.getDetRowId());
                    if (oldE != null && newE != null) {
                        String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, dto.getDetRowId());
                        empLogRequests.add(new LogRequestDto<>(oldE, newE, InsuranceEmployeeDetail.class, docId, docKeyPoid, logDetail));
                    }
                }
            }
        }
        
        // Process Property Details
        if (request.getPropertyDetails() != null) {
            int newPropIndex = 0;
            for (InsurancePropertyDetailRequestDto dto : request.getPropertyDetails()) {
                String action = normalizeAction(dto.getActionType());
                if ("ISDELETED".equals(action)) {
                    loggingService.logDelete(dto, docId, docKeyPoid);
                } else if ("ISCREATED".equals(action)) {
                    InsurancePropertyDetail newP = findNewPropertyByIndex(updated.getPropertyDetails(), oldPropertyDetails, newPropIndex++);
                    if (newP != null) {
                        String msg = String.format("Row Created on Insurance Property Detail with detRowId: %s", newP.getDetRowId());
                        loggingService.createLogSummaryEntry(docId, docKeyPoid, msg);
                    }
                } else if ("ISUPDATED".equals(action)) {
                    InsurancePropertyDetail oldP = findOldPropertyByDetRowId(oldPropertyDetails, dto.getDetRowId());
                    InsurancePropertyDetail newP = findOldPropertyByDetRowId(updated.getPropertyDetails(), dto.getDetRowId());
                    if (oldP != null && newP != null) {
                        String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, dto.getDetRowId());
                        propLogRequests.add(new LogRequestDto<>(oldP, newP, InsurancePropertyDetail.class, docId, docKeyPoid, logDetail));
                    }
                }
            }
        }
        
        // Process PIC Details
        if (request.getPicDetails() != null) {
            int newPicIndex = 0;
            for (InsurancePicDetailRequestDto dto : request.getPicDetails()) {
                String action = normalizeAction(dto.getActionType());
                if ("ISDELETED".equals(action)) {
                    loggingService.logDelete(dto, docId, docKeyPoid);
                } else if ("ISCREATED".equals(action)) {
                    InsurancePicDetail newP = findNewPicByIndex(updated.getPicDetails(), oldPicDetails, newPicIndex++);
                    if (newP != null) {
                        String msg = String.format("Row Created on Insurance PIC Detail with detRowId: %s", newP.getDetRowId());
                        loggingService.createLogSummaryEntry(docId, docKeyPoid, msg);
                    }
                } else if ("ISUPDATED".equals(action)) {
                    InsurancePicDetail oldP = findOldPicByDetRowId(oldPicDetails, dto.getDetRowId());
                    InsurancePicDetail newP = findOldPicByDetRowId(updated.getPicDetails(), dto.getDetRowId());
                    if (oldP != null && newP != null) {
                        String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, dto.getDetRowId());
                        picLogRequests.add(new LogRequestDto<>(oldP, newP, InsurancePicDetail.class, docId, docKeyPoid, logDetail));
                    }
                }
            }
        }
        
        // Batch process all update logs
        if (!empLogRequests.isEmpty()) {
            loggingService.createLogBatch(empLogRequests);
        }
        if (!propLogRequests.isEmpty()) {
            loggingService.createLogBatch(propLogRequests);
        }
        if (!picLogRequests.isEmpty()) {
            loggingService.createLogBatch(picLogRequests);
        }
        
        return mapToResponseDto(updated);
    }

    private void buildAndSetChildDetails(InsuranceMasterRequestDto request, InsuranceMaster savedParent) {
        if (request.getEmployeeDetails() != null && !request.getEmployeeDetails().isEmpty()) {
            List<InsuranceEmployeeDetail> newDetails = buildEmployeeDetails(request.getEmployeeDetails(), savedParent);
            newDetails.forEach(d -> {
                d.setTransactionPoid(savedParent.getTransactionPoid());
                d.setInsuranceMaster(savedParent);
            });
            if (savedParent.getEmployeeDetails() == null) {
                savedParent.setEmployeeDetails(new ArrayList<>());
            }
            savedParent.getEmployeeDetails().addAll(newDetails);
        }
        if (request.getPropertyDetails() != null && !request.getPropertyDetails().isEmpty()) {
            List<InsurancePropertyDetail> newDetails = buildPropertyDetails(request.getPropertyDetails(), savedParent);
            newDetails.forEach(d -> {
                d.setTransactionPoid(savedParent.getTransactionPoid());
                d.setInsuranceMaster(savedParent);
            });
            if (savedParent.getPropertyDetails() == null) {
                savedParent.setPropertyDetails(new ArrayList<>());
            }
            savedParent.getPropertyDetails().addAll(newDetails);
        }
        if (request.getPicDetails() != null && !request.getPicDetails().isEmpty()) {
            List<InsurancePicDetail> newDetails = buildPicDetails(request.getPicDetails(), savedParent);
            newDetails.forEach(d -> {
                d.setTransactionPoid(savedParent.getTransactionPoid());
                d.setInsuranceMaster(savedParent);
            });
            if (savedParent.getPicDetails() == null) {
                savedParent.setPicDetails(new ArrayList<>());
            }
            savedParent.getPicDetails().addAll(newDetails);
        }
    }



    private List<InsuranceEmployeeDetail> buildEmployeeDetails(List<InsuranceEmployeeDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsuranceEmployeeDetail> result = new ArrayList<>();
        
        // Find max detRowId from existing records AND incoming DTOs
        long maxFromExisting = parent.getEmployeeDetails() != null ? 
            parent.getEmployeeDetails().stream()
                .mapToLong(InsuranceEmployeeDetail::getDetRowId)
                .max()
                .orElse(0L) : 0L;
        long maxFromDtos = dtos.stream()
            .filter(dto -> dto.getDetRowId() != null)
            .mapToLong(InsuranceEmployeeDetailRequestDto::getDetRowId)
            .max()
            .orElse(0L);
        long maxDetRowId = Math.max(maxFromExisting, maxFromDtos);
        long nextDetRowId = maxDetRowId + 1;
        
        for (InsuranceEmployeeDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            // Skip empty rows
            if (dto.getEmployeePoid() == null && dto.getAmount() == null) {
                continue;
            }
            
            Long detRowId;
            if ("ISCREATED".equals(action)) {
                detRowId = nextDetRowId++;
            } else if ("ISUPDATED".equals(action)) {
                detRowId = dto.getDetRowId();
            } else {
                detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            }
            
            result.add(InsuranceEmployeeDetail.builder()
                    .detRowId(detRowId)
                    .employeePoid(dto.getEmployeePoid())
                    .amount(dto.getAmount())
                    .remarks(dto.getRemarks())
                    .build());
        }
        return result;
    }

    private List<InsurancePropertyDetail> buildPropertyDetails(List<InsurancePropertyDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsurancePropertyDetail> result = new ArrayList<>();
        
        // Find max detRowId from existing records AND incoming DTOs
        long maxFromExisting = parent.getPropertyDetails() != null ? 
            parent.getPropertyDetails().stream()
                .mapToLong(InsurancePropertyDetail::getDetRowId)
                .max()
                .orElse(0L) : 0L;
        long maxFromDtos = dtos.stream()
            .filter(dto -> dto.getDetRowId() != null)
            .mapToLong(InsurancePropertyDetailRequestDto::getDetRowId)
            .max()
            .orElse(0L);
        long maxDetRowId = Math.max(maxFromExisting, maxFromDtos);
        long nextDetRowId = maxDetRowId + 1;
        
        for (InsurancePropertyDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            // Skip empty rows
            if (dto.getPropertyPoid() == null && dto.getAmount() == null) {
                continue;
            }
            
            Long detRowId;
            if ("ISCREATED".equals(action)) {
                detRowId = nextDetRowId++;
            } else if ("ISUPDATED".equals(action)) {
                detRowId = dto.getDetRowId();
            } else {
                detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            }
            
            result.add(InsurancePropertyDetail.builder()
                    .detRowId(detRowId)
                    .propertyPoid(dto.getPropertyPoid())
                    .amount(dto.getAmount())
                    .remarks(dto.getRemarks())
                    .build());
        }
        return result;
    }

    private List<InsurancePicDetail> buildPicDetails(List<InsurancePicDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsurancePicDetail> result = new ArrayList<>();
        
        // Find max detRowId from existing records AND incoming DTOs
        long maxFromExisting = parent.getPicDetails() != null ? 
            parent.getPicDetails().stream()
                .mapToLong(InsurancePicDetail::getDetRowId)
                .max()
                .orElse(0L) : 0L;
        long maxFromDtos = dtos.stream()
            .filter(dto -> dto.getDetRowId() != null)
            .mapToLong(InsurancePicDetailRequestDto::getDetRowId)
            .max()
            .orElse(0L);
        long maxDetRowId = Math.max(maxFromExisting, maxFromDtos);
        long nextDetRowId = maxDetRowId + 1;
        
        for (InsurancePicDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            if ("ISDELETED".equals(action)) continue;
            
            // Skip empty rows
            if (dto.getRolePoid() == null && dto.getPicPersonPoid() == null && 
                dto.getFromDate() == null && dto.getToDate() == null) {
                continue;
            }
            
            Long detRowId;
            if ("ISCREATED".equals(action)) {
                detRowId = nextDetRowId++;
            } else if ("ISUPDATED".equals(action)) {
                detRowId = dto.getDetRowId();
            } else {
                detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            }
            
            result.add(InsurancePicDetail.builder()
                    .detRowId(detRowId)
                    .rolePoid(dto.getRolePoid())
                    .contactType(dto.getContactType())
                    .picPersonPoid(dto.getPicPersonPoid())
                    .fromDate(dto.getFromDate())
                    .toDate(dto.getToDate())
                    .build());
        }
        return result;
    }

    private InsuranceMasterResponseDto mapToResponseDto(InsuranceMaster entity) {
        InsuranceMasterResponseDto dto = InsuranceMasterResponseDto.builder()
                .insurancePoid(entity.getTransactionPoid())
                .docRef(entity.getDocRef())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .insuranceType(entity.getInsuranceType())
                .insuranceCategory(entity.getInsuranceCategory())
                .policyNo(entity.getPolicyNo())
                .insuranceProvider(entity.getInsuranceProvider())
                .fromDate(entity.getFromDate())
                .expiryDate(entity.getExpiryDate())
                .status(calculateStatus(entity))
                .currency(entity.getCurrencyPoid())
                .rate(entity.getExchangeRate())
                .insuranceAmount(entity.getInsuranceAmount())
                .premiumAmount(entity.getPremiumAmount())
                .paymentFrequency(entity.getPaymentFrequency())
                .oneTime(entity.getOneTime())
                .description(entity.getDescription())
                .faPoid(entity.getFaPoid())
                .pjRefPoid(entity.getPjRefPoid())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .employeeDetails(mapEmployeeDetails(entity.getEmployeeDetails()))
                .propertyDetails(mapPropertyDetails(entity.getPropertyDetails()))
                .picDetails(mapPicDetails(entity.getPicDetails()))
                .renewalLogs(mapRenewalLogs(entity.getRenewalLogs()))
                .build();
        
        // Populate header LOV details
        if (entity.getInsuranceType() != null) {
            dto.setInsuranceTypeDetails(lovService.getDetailsByCodeAndLovName(entity.getInsuranceType(), "INSURANCE_TYPE"));
        }
        if (entity.getInsuranceCategory() != null) {
            dto.setInsuranceCategoryDetails(lovService.getDetailsByCodeAndLovName(entity.getInsuranceCategory(), "INSURANCE_CATEGORY"));
        }
        if (entity.getInsuranceProvider() != null) {
            dto.setInsuranceProviderDetails(lovService.getDetailsByCodeAndLovName(entity.getInsuranceProvider(), "INSURANCE_SUPPLIER_MASTER"));
        }
        if (entity.getCurrencyPoid() != null) {
            dto.setCurrencyDetails(lovService.getDetailsByPoidAndLovName(entity.getCurrencyPoid(), "CURRENCY"));
        }
        if (entity.getPaymentFrequency() != null) {
            dto.setPaymentFrequencyDetails(lovService.getDetailsByCodeAndLovName(entity.getPaymentFrequency(), "INSURANCE_PAYMENT_TYPE"));
        }
        
        return dto;
    }

    private String calculateStatus(InsuranceMaster entity) {
        if (entity.getExpiryDate() == null) return "Active";

        LocalDate today = LocalDate.now();
        if (entity.getRenewalLogs() != null && !entity.getRenewalLogs().isEmpty()) {
            return "Renewed";
        }
        if (entity.getExpiryDate().isBefore(today)) {
            return "Expired";
        }
        return "Active";
    }

    private List<InsuranceEmployeeDetailResponseDto> mapEmployeeDetails(List<InsuranceEmployeeDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> {
                    LovGetListDto employeeLov = null;
                    if (e.getEmployeePoid() != null) {
                        try {
                            HrEmployeeMaster employee = hrEmployeeMasterRepository.findByEmployeePoid(e.getEmployeePoid()).orElse(null);
                            if (employee != null) {
                                employeeLov = new LovGetListDto(
                                    employee.getEmployeePoid(),
                                    employee.getEmployeeCode(),
                                    employee.getEmployeeName(),
                                    employee.getEmployeePoid(),
                                    employee.getEmployeeName(),
                                    null,
                                    null
                                );
                            }
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                    return InsuranceEmployeeDetailResponseDto.builder()
                            .detRowId(e.getDetRowId())
                            .employee(employeeLov)
                            .amount(e.getAmount())
                            .remarks(e.getRemarks())
                            .build();
                })
                .toList();
    }

    private List<InsurancePropertyDetailResponseDto> mapPropertyDetails(List<InsurancePropertyDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> {
                    LovGetListDto propertyLov = null;
                    if (e.getPropertyPoid() != null) {
                        propertyLov = getLovByPoid(e.getPropertyPoid(), "INSURANCE_PROPERTY_MASTER");
                    }
                    return InsurancePropertyDetailResponseDto.builder()
                            .detRowId(e.getDetRowId())
                            .property(propertyLov)
                            .amount(e.getAmount())
                            .remarks(e.getRemarks())
                            .build();
                })
                .toList();
    }



    private List<InsurancePicDetailResponseDto> mapPicDetails(List<InsurancePicDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> {
                    DetailsDto roleDetails = null;
                    if (e.getRolePoid() != null) {
                        try {
                            roleDetails = getRoleDetails(e.getRolePoid());
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                    LovGetListDto contactTypeLov = null;
                    if (e.getContactType() != null) {
                        contactTypeLov = getLovByCode(e.getContactType(), "INSURANCE_CONTACT_TYPE");
                    }
                    LovGetListDto picPersonLov = null;
                    if (e.getPicPersonPoid() != null) {
                        picPersonLov = getLovByPoid(e.getPicPersonPoid(), "INSURANCE_PROPERTY_PIC");
                    }
                    return InsurancePicDetailResponseDto.builder()
                            .detRowId(e.getDetRowId())
                            .role(roleDetails)
                            .contactType(e.getContactType())
                            .contactTypeDetails(contactTypeLov)
                            .picPerson(picPersonLov)
                            .fromDate(e.getFromDate())
                            .toDate(e.getToDate())
                            .build();
                })
                .toList();
    }

    private List<InsuranceRenewalLogResponseDto> mapRenewalLogs(List<InsuranceRenewalLog> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> InsuranceRenewalLogResponseDto.builder()
                        .detRowId(e.getDetRowId())
                        .renewalDate(e.getRenewalDate())
                        .fromDate(e.getFromDate())
                        .expiryDate(e.getExpiryDate())
                        .insuranceAmount(e.getInsuranceAmount())
                        .premiumAmount(e.getPremiumAmount())
                        .createdBy(e.getCreatedBy())
                        .lastModifiedBy(e.getLastModifiedBy())
                        .build())
                .toList();
    }

    private List<InsuranceRenewalLog> buildRenewalLogs(List<InsuranceRenewalLogRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();

        LocalDate today = LocalDate.now();

        return IntStream.range(0, dtos.size())
                .mapToObj(i -> {
                    InsuranceRenewalLogRequestDto dto = dtos.get(i);

                    // Validate renewal date is not in future
                    if (dto.getRenewalDate().isAfter(today)) {
                        throw new ValidationException("Renewal date cannot be in the future");
                    }

                    return InsuranceRenewalLog.builder()
                            .transactionPoid(parent.getTransactionPoid())
                            .detRowId((long) (i + 1))
                            .renewalDate(dto.getRenewalDate())
                            .fromDate(dto.getFromDate())
                            .expiryDate(dto.getExpiryDate())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String normalizeAction(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) return "ISCREATED";
        return switch (actionType.trim().toUpperCase()) {
            case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
            case "ISUPDATED", "UPDATED" -> "ISUPDATED";
            case "ISDELETED", "DELETED" -> "ISDELETED";
            default -> "NOCHANGES";
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DetailsDto validateAndGetRoleDetails(Long rolePoid) {
        try {
            RoleDto roleDto = roleServiceClient.findById(rolePoid);
            if (roleDto == null) {
                throw new ValidationException("Please select a valid User Role in PIC Details");
            }
            return new DetailsDto(
                    roleDto.getUserRolePoid(),
                    roleDto.getUserRoleId(),
                    roleDto.getUserRoleName(),
                    roleDto.getUserRolePoid(),
                    roleDto.getUserRoleName(),
                    roleDto.getSeqNo()
            );
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch role details: " + e.getMessage());
        }
    }

    public DetailsDto getRoleDetails(Long rolePoid) {
        try {
            RoleDto roleDto = roleServiceClient.findById(rolePoid);
            if (roleDto == null) return null;
            return new DetailsDto(
                    roleDto.getUserRolePoid(),
                    roleDto.getUserRoleId(),
                    roleDto.getUserRoleName(),
                    roleDto.getUserRolePoid(),
                    roleDto.getUserRoleName(),
                    roleDto.getSeqNo()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private LovGetListDto getLovByPoid(Long poid, String lovName) {
        if (poid == null) return null;
        try {
            return lovService.getDetailsByPoidAndLovName(poid, lovName);
        } catch (Exception e) {
            return null;
        }
    }

    private LovGetListDto getLovByCode(String code, String lovName) {
        if (code == null) return null;
        try {
            return lovService.getDetailsByCodeAndLovName(code, lovName);
        } catch (Exception e) {
            return null;
        }
    }

    private String getEmployeeLabel(Long employeePoid) {
        if (employeePoid == null) return null;
        try {
            return hrEmployeeMasterRepository.findByEmployeePoid(employeePoid)
                    .map(HrEmployeeMaster::getEmployeeName)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String getPropertyLabel(Long propertyPoid) {
        if (propertyPoid == null) return null;
        try {
            // Property PIC is from LOV table - fetch using document service or return poid as string
            // Since we don't have a direct repository, return the poid for now
            // Frontend should already have the label from the LOV dropdown
            return "Property-" + propertyPoid;
        } catch (Exception e) {
            return null;
        }
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage, LocalDateTime logDateTime) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(logDateTime != null ? logDateTime : LocalDateTime.now());
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }

    private InsuranceEmployeeDetail findOldEmployeeByDetRowId(List<InsuranceEmployeeDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(e -> detRowId.equals(e.getDetRowId())).findFirst().orElse(null);
    }
    private InsurancePropertyDetail findOldPropertyByDetRowId(List<InsurancePropertyDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(p -> detRowId.equals(p.getDetRowId())).findFirst().orElse(null);
    }
    private InsurancePicDetail findOldPicByDetRowId(List<InsurancePicDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(p -> detRowId.equals(p.getDetRowId())).findFirst().orElse(null);
    }
    private InsuranceVehicleDetail findOldVehicleByDetRowId(List<InsuranceVehicleDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(v -> detRowId.equals(v.getDetRowId())).findFirst().orElse(null);
    }

    private InsuranceEmployeeDetail findNewEmployeeByIndex(List<InsuranceEmployeeDetail> newList, List<InsuranceEmployeeDetail> oldList, int index) {
        if (newList == null) return null;
        Set<Long> oldDetRowIds = oldList != null ? oldList.stream().map(InsuranceEmployeeDetail::getDetRowId).collect(Collectors.toSet()) : new HashSet<>();
        List<InsuranceEmployeeDetail> newRecords = newList.stream().filter(e -> !oldDetRowIds.contains(e.getDetRowId())).toList();
        return index < newRecords.size() ? newRecords.get(index) : null;
    }

    private InsurancePropertyDetail findNewPropertyByIndex(List<InsurancePropertyDetail> newList, List<InsurancePropertyDetail> oldList, int index) {
        if (newList == null) return null;
        Set<Long> oldDetRowIds = oldList != null ? oldList.stream().map(InsurancePropertyDetail::getDetRowId).collect(Collectors.toSet()) : new HashSet<>();
        List<InsurancePropertyDetail> newRecords = newList.stream().filter(p -> !oldDetRowIds.contains(p.getDetRowId())).toList();
        return index < newRecords.size() ? newRecords.get(index) : null;
    }

    private InsurancePicDetail findNewPicByIndex(List<InsurancePicDetail> newList, List<InsurancePicDetail> oldList, int index) {
        if (newList == null) return null;
        Set<Long> oldDetRowIds = oldList != null ? oldList.stream().map(InsurancePicDetail::getDetRowId).collect(Collectors.toSet()) : new HashSet<>();
        List<InsurancePicDetail> newRecords = newList.stream().filter(p -> !oldDetRowIds.contains(p.getDetRowId())).toList();
        return index < newRecords.size() ? newRecords.get(index) : null;
    }

    private List<InsuranceEmployeeDetail> snapshotEmployeeDetails(List<InsuranceEmployeeDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsuranceEmployeeDetail> snapshots = new ArrayList<>();
        for (InsuranceEmployeeDetail e : list) {
            InsuranceEmployeeDetail s = new InsuranceEmployeeDetail();
            BeanUtils.copyProperties(e, s);
            s.setTransactionPoid(e.getTransactionPoid());
            s.setDetRowId(e.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }
    private List<InsurancePropertyDetail> snapshotPropertyDetails(List<InsurancePropertyDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsurancePropertyDetail> snapshots = new ArrayList<>();
        for (InsurancePropertyDetail p : list) {
            InsurancePropertyDetail s = new InsurancePropertyDetail();
            BeanUtils.copyProperties(p, s);
            s.setTransactionPoid(p.getTransactionPoid());
            s.setDetRowId(p.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }
    private List<InsurancePicDetail> snapshotPicDetails(List<InsurancePicDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsurancePicDetail> snapshots = new ArrayList<>();
        for (InsurancePicDetail p : list) {
            InsurancePicDetail s = new InsurancePicDetail();
            BeanUtils.copyProperties(p, s);
            s.setTransactionPoid(p.getTransactionPoid());
            s.setDetRowId(p.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }
    private List<InsuranceVehicleDetail> snapshotVehicleDetails(List<InsuranceVehicleDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsuranceVehicleDetail> snapshots = new ArrayList<>();
        for (InsuranceVehicleDetail v : list) {
            InsuranceVehicleDetail s = new InsuranceVehicleDetail();
            BeanUtils.copyProperties(v, s);
            s.setTransactionPoid(v.getTransactionPoid());
            s.setDetRowId(v.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }




    private InsuranceMaster copyHeaderOnlyForLog(InsuranceMaster source) {
        InsuranceMaster copy = new InsuranceMaster();
        BeanUtils.copyProperties(source, copy);
        copy.setEmployeeDetails(null);
        copy.setPropertyDetails(null);
        copy.setPicDetails(null);
        copy.setVehicleDetails(null);
        copy.setInsuranceDetails(null);
        copy.setRenewalLogs(null);
        return copy;
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto renewInsurance(Long insuranceId, InsuranceMasterRequestDto request) {
        InsuranceMaster existing = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        // Scenario 2: Check if already renewed
        if (existing.getRenewalLogs() != null && !existing.getRenewalLogs().isEmpty()) {
            throw new ValidationException("Details are already added to the renewal history");
        }

        // User confirmed - proceed with renewal
        InsuranceRenewalLog renewalLog = InsuranceRenewalLog.builder()
                .transactionPoid(existing.getTransactionPoid())
                .detRowId(1L)
                .renewalDate(LocalDate.now())
                .fromDate(existing.getFromDate())
                .expiryDate(existing.getExpiryDate())
                .insuranceAmount(existing.getInsuranceAmount())
                .premiumAmount(existing.getPremiumAmount())
                .build();

        if (existing.getRenewalLogs() == null) {
            existing.setRenewalLogs(new ArrayList<>());
        }
        existing.getRenewalLogs().add(renewalLog);

        // Update with new renewal details
        existing.setFromDate(request.getFromDate());
        existing.setExpiryDate(request.getExpiryDate());
        existing.setInsuranceAmount(request.getInsuranceAmount());
        existing.setPremiumAmount(request.getPremiumAmount());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        InsuranceMaster renewed = insuranceMasterRepository.save(existing);
        return mapToResponseDto(renewed);
    }
}
