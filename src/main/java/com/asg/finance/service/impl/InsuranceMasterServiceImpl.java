package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.dto.masters.*;
import com.asg.finance.entity.master.*;
import com.asg.finance.repository.master.InsuranceMasterRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.InsuranceMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
    
    @Autowired
    private LovDataService lovService;

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
        List<FilterDto> filterList = documentService.resolveDateFilters(filters,"TRANSACTION_POID",startDate, endDate);

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
                deleteReasonDto.getDeleteReason(),
                null
        );
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto createInsuranceMaster(InsuranceMasterRequestDto request) {
        try {
            System.out.println("Creating insurance master for policy: " + request.getPolicyNo());

            // Validate policy number format
            if (!request.getPolicyNo().matches("^POL[0-9]{8}$")) {
                throw new ValidationException("Policy number must be in format POL12345678");
            }

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

            // Validate PIC dates (if present)
            if (request.getPicDetails() != null) {
                for (InsurancePicDetailRequestDto pic : request.getPicDetails()) {

                    // Validate PIC from ≤ to
                    if (pic.getFromDate().isAfter(pic.getToDate())) {
                        throw new ValidationException("PIC From Date cannot be greater than PIC To Date");
                    }

                    // Validate PIC within insurance period
                    if (pic.getFromDate().isBefore(request.getFromDate()) ||
                            pic.getToDate().isAfter(request.getExpiryDate())) {

                        throw new ValidationException("PIC dates must be within the insurance policy period");
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
                    .deleted("N")
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build();

            insuranceMaster.setPjRefPoid(null);
            InsuranceMaster saved = insuranceMasterRepository.save(insuranceMaster);
            buildAndSetChildDetails(request, saved);
            InsuranceMaster finalSaved = insuranceMasterRepository.save(saved);
            return mapToResponseDto(finalSaved);
        } catch (Exception e) {
            System.err.println("Error in createInsuranceMaster: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create Insurance Master: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto updateInsuranceMaster(Long insuranceId, InsuranceMasterRequestDto request) {
        // Preserve existing PJ_REF_POID if already linked


        InsuranceMaster existing = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        // Validate unique policy number per company (excluding current record)

        // Validate policy number format
        if (!request.getPolicyNo().matches("^POL[0-9]{8}$")) {
            throw new ValidationException("Policy number must be in format POL12345678");
        }

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

        // Validate PIC dates (if present)
        if (request.getPicDetails() != null) {
            for (InsurancePicDetailRequestDto pic : request.getPicDetails()) {

                // Validate PIC from ≤ to
                if (pic.getFromDate().isAfter(pic.getToDate())) {
                    throw new ValidationException("PIC From Date cannot be greater than PIC To Date");
                }

                // Validate PIC within insurance period
                if (pic.getFromDate().isBefore(request.getFromDate()) ||
                        pic.getToDate().isAfter(request.getExpiryDate())) {

                    throw new ValidationException("PIC dates must be within the insurance policy period");
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
        existing.setInsuranceAmount(request.getInsuranceAmount());
        existing.setPremiumAmount(request.getPremiumAmount());
        existing.setPaymentFrequency(request.getPaymentFrequency());
        existing.setDescription(request.getDescription());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        // Clear existing child details
        existing.getEmployeeDetails().clear();
        existing.getPropertyDetails().clear();
        existing.getPicDetails().clear();

        // Add updated child details
        buildAndSetChildDetails(request, existing);
        if (existing.getPjRefPoid() != null) {
            existing.setPjRefPoid(existing.getPjRefPoid());
        } else {
            existing.setPjRefPoid(null); // don't allow overwriting or random assignment
        }

        InsuranceMaster updated = insuranceMasterRepository.save(existing);
        return mapToResponseDto(updated);
    }

    private void buildAndSetChildDetails(InsuranceMasterRequestDto request, InsuranceMaster savedParent) {
        try {
            if (request.getEmployeeDetails() != null && !request.getEmployeeDetails().isEmpty()) {
                System.out.println("Building " + request.getEmployeeDetails().size() + " employee details");
                savedParent.setEmployeeDetails(buildEmployeeDetails(request.getEmployeeDetails(), savedParent));
            }
            if (request.getPropertyDetails() != null && !request.getPropertyDetails().isEmpty()) {
                System.out.println("Building " + request.getPropertyDetails().size() + " property details");
                savedParent.setPropertyDetails(buildPropertyDetails(request.getPropertyDetails(), savedParent));
            }

            if (request.getPicDetails() != null && !request.getPicDetails().isEmpty()) {
                System.out.println("Building " + request.getPicDetails().size() + " PIC details");
                savedParent.setPicDetails(buildPicDetails(request.getPicDetails(), savedParent));
            }
        } catch (Exception e) {
            System.err.println("Error building child details: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }



    private List<InsuranceEmployeeDetail> buildEmployeeDetails(List<InsuranceEmployeeDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsuranceEmployeeDetail> result = new ArrayList<>();
        long nextDetRowId = 1;
        
        for (InsuranceEmployeeDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            
            result.add(InsuranceEmployeeDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .employeePoid(1L)
                    .amount(dto.getAmount())
                    .remarks(dto.getRemarks())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    private List<InsurancePropertyDetail> buildPropertyDetails(List<InsurancePropertyDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsurancePropertyDetail> result = new ArrayList<>();
        long nextDetRowId = 1;
        
        for (InsurancePropertyDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            
            result.add(InsurancePropertyDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .propertyPoid(1L)
                    .amount(dto.getAmount())
                    .remarks(dto.getRemarks())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    private List<InsurancePicDetail> buildPicDetails(List<InsurancePicDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsurancePicDetail> result = new ArrayList<>();
        long nextDetRowId = 1;
        
        for (InsurancePicDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            DetailsDto roleDetails = getRoleDetails(dto.getRolePoid());
            Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            
            result.add(InsurancePicDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .rolePoid(roleDetails.poid())
                    .contactType(dto.getContactType())
                    .picPerson(dto.getPicPerson())
                    .fromDate(dto.getFromDate())
                    .toDate(dto.getToDate())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    private InsuranceMasterResponseDto mapToResponseDto(InsuranceMaster entity) {
        return InsuranceMasterResponseDto.builder()
                .insurancePoid(entity.getTransactionPoid())
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
                .pjRefPoid(entity.getPjRefPoid())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .employeeDetails(mapEmployeeDetails(entity.getEmployeeDetails()))
                .propertyDetails(mapPropertyDetails(entity.getPropertyDetails()))
                .picDetails(mapPicDetails(entity.getPicDetails()))
                .build();
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
                .map(e -> InsuranceEmployeeDetailResponseDto.builder()
                        .employeeDetailPoid(e.getDetRowId())
                       // .sn(e.getDetRowId().intValue())
                        .amount(e.getAmount())
                        .remarks(e.getRemarks())
                        .build())
                .toList();
    }

    private List<InsurancePropertyDetailResponseDto> mapPropertyDetails(List<InsurancePropertyDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> InsurancePropertyDetailResponseDto.builder()
                        .propertyDetailPoid(e.getDetRowId())
                       // .sn(e.getDetRowId().intValue())
                        .amount(e.getAmount())
                        .remarks(e.getRemarks())
                        .build())
                .toList();
    }



    private List<InsurancePicDetailResponseDto> mapPicDetails(List<InsurancePicDetail> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(e -> InsurancePicDetailResponseDto.builder()
                        .rolePoid(e.getRolePoid())
                        .contactType(e.getContactType())
                        .picPerson(e.getPicPerson())
                        .fromDate(e.getFromDate())
                        .toDate(e.getToDate())
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
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .lastModifiedBy(getCurrentUser())
                            .lastModifiedDate(LocalDateTime.now())
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

    public DetailsDto getRoleDetails(Long rolePoid) {
        RoleDto roleDto = roleServiceClient.findById(rolePoid);

        if (roleDto == null) {
            new ResourceNotFoundException("Global User Role", "userRolePoid", rolePoid);
        }
        return new DetailsDto(
                roleDto.getUserRolePoid(),     // poid
                roleDto.getUserRoleId(),       // code
                roleDto.getUserRoleName(),     // label
                roleDto.getUserRolePoid(),     // value
                roleDto.getUserRoleName(),    // description
                roleDto.getSeqNo()             // seqNo
        );
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto renewInsurance(Long insuranceId, InsuranceMasterRequestDto request) {
        InsuranceMaster existing = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        LocalDate today = LocalDate.now();

        // Validate insurance can be renewed (must be expired or expiring)
        if (existing.getExpiryDate().isAfter(today)) {
            throw new ValidationException("Insurance can only be renewed on or after expiry date");
        }

        // Archive current details to renewal log
        InsuranceRenewalLog renewalLog = InsuranceRenewalLog.builder()
                .transactionPoid(existing.getTransactionPoid())
                .detRowId((long) (existing.getRenewalLogs().size() + 1))
                .renewalDate(LocalDate.now())
                .fromDate(existing.getFromDate())
                .expiryDate(existing.getExpiryDate())
                .insuranceAmount(existing.getInsuranceAmount())
                .premiumAmount(existing.getPremiumAmount())
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(LocalDateTime.now())
                .build();

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
