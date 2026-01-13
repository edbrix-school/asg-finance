package com.asg.finance.service.impl;

import com.asg.common.lib.dto.response.AddressMasterResponse;
import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.client.AddressMasterServiceClient;
import com.asg.common.lib.dto.*;
import com.asg.common.lib.entity.CurrencyEntity;
import com.asg.common.lib.repository.GroupRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.entity.key.SupplierMasterMangementDtlKey;
import com.asg.finance.entity.key.SupplierMasterQstnDtlKey;
import com.asg.finance.entity.key.SupplierMasterServiceDtlKey;
import com.asg.finance.entity.key.SupplierPaymentDetailId;
import com.asg.finance.entity.master.HrEmployeeMaster;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.HrEmployeeMasterRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.SupplierMasterService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierMasterServiceImpl implements SupplierMasterService {

    private final GroupRepository groupRepository;
    private final SalesCustomerMasterRepository salesCustomerMasterRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final SupplierMasterPaymentDtlRepository supplierMasterPaymentDtlRepository;
    private final SupplierMasterMangementDtlRepository supplierMasterMangementDtlRepository;
    private final SupplierMasterServiceDtlRepository supplierMasterServiceDtlRepository;
    private final SupplierMasterQstnRepository supplierMasterQstnDtlRepository;
    private final SupplierServicesMasterRepository supplierServicesMasterRepository;
    private final GLMasterRepository glMasterRepository;
    private final HrEmployeeMasterRepository hrEmployeeMasterRepository;
    private final DocumentSearchService documentService;
    private final JdbcTemplate jdbcTemplate;
    private final AddressMasterServiceClient addressMasterServiceClient;
    private final LovDataService lovDataService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public SupplierMasterDto getSupplierMaster(Long supplierPoid) {

        SupplierMasterEntity supplierMasterEntity = supplierMasterRepository.findBySupplierPoid(supplierPoid);
        if (supplierMasterEntity == null) {
            throw new ResourceNotFoundException("Supplier Master", "supplierPoid", supplierPoid);
        }
        SupplierMasterDto supplierMasterDto = new SupplierMasterDto();
        BeanUtils.copyProperties(supplierMasterEntity, supplierMasterDto);

        if (supplierMasterEntity.getCurrencyCode() != null) {
            LovGetListDto lovGetListDto = lovDataService.getLovItemByCodeFast(supplierMasterDto.getCurrencyCode(), "CURRENCY");
            if (lovGetListDto.getPoid() != null) {
                CurrencyLightDto currencyDto = new CurrencyLightDto();
                currencyDto.setCurrencyPoid(lovGetListDto.getPoid());
                currencyDto.setCurrencyCode(lovGetListDto.getCode());
                currencyDto.setCurrencyName(lovGetListDto.getLabel());
                supplierMasterDto.setCurrency(currencyDto);
            }
        }
        if (supplierMasterEntity.getSupplierCategoryPoid() != null)
            supplierMasterDto.setSupplierCategory(toSupplierCategoryDto(supplierCategoryRepository.findBySupplierCategoryPoid(supplierMasterEntity.getSupplierCategoryPoid())));
        if (supplierMasterEntity.getCountryPoid() != null) {

            LovGetListDto lovGetListDto = lovDataService.getDetailsByPoidAndLovName(supplierMasterEntity.getCountryPoid(), "COUNTRY");
            if (lovGetListDto.getCode() != null) {
                CountryDto countryDto = new CountryDto();
                countryDto.setCountryPoid(lovGetListDto.getPoid());
                countryDto.setCountryCode(lovGetListDto.getCode());
                countryDto.setCountryName(lovGetListDto.getCode());
                supplierMasterDto.setCountry(countryDto);
            }
        }
        if (supplierMasterEntity.getGlPoid() != null)
            supplierMasterDto.setGlMaster(toGLMasterDto(glMasterRepository.findByGlPoid(supplierMasterEntity.getGlPoid())));
        if (supplierMasterEntity.getCustomerPoid() != null)
            supplierMasterDto.setCustomer(toSalesCustomerMasterDto(salesCustomerMasterRepository.findByCustomerPoid(supplierMasterEntity.getCustomerPoid())));
        if (supplierMasterEntity.getPurchaserPoid() != null)
            supplierMasterDto.setPurchaser(toHrEmployeeDto(hrEmployeeMasterRepository.findByEmployeePoid(supplierMasterEntity.getPurchaserPoid())));
        if (supplierMasterDto.getAddressPoid() != null) {
            AddressMasterResponse addressMaster = addressMasterServiceClient.findById(supplierMasterDto.getAddressPoid());
            if (addressMaster != null) {
                supplierMasterDto.setAddressName(addressMaster.getAddressName());
                AddressMasterLightDto addressDto = new AddressMasterLightDto(
                        addressMaster.getAddressMasterPoid(),
                        addressMaster.getAddressName(),
                        addressMaster.getCountryName(),
                        addressMaster.getAddressName(),
                        addressMaster.getAddressMasterPoid(),
                        addressMaster.getActive()
                );
                supplierMasterDto.setAddress(addressDto);
                supplierMasterDto.setAddressTypeMap(addressMaster.getAddressTypeMap());
            }
        }

        supplierMasterDto.setPaymentDtl(supplierMasterPaymentDtlRepository.findBySupplierPoid(supplierPoid).stream().map(this::mapSupplierMasterPaymentDtlEntityToDto).toList());
        supplierMasterDto.setManagementDtl(supplierMasterMangementDtlRepository.findBySupplierPoid(supplierPoid).stream().map(this::mapSupplierMasterManagementDtlEntityToDto).toList());
        supplierMasterDto.setServiceDtl(supplierMasterServiceDtlRepository.findBySupplierPoid(supplierPoid).stream().map(this::mapServiceDtlEntityToDto).toList());
        supplierMasterDto.setQuestionaries(supplierMasterQstnDtlRepository.findBySupplierPoid(supplierPoid).stream().map(this::mapSupplierMasterQstnDtlEntityToDto).toList());
        return supplierMasterDto;
    }

    @Override
    @Transactional
    public void deleteSupplierMaster(Long supplierPoid, DeleteReasonDto deleteReasonDto) {
        SupplierMasterEntity supplierMasterEntity = supplierMasterRepository.findBySupplierPoid(supplierPoid);
        documentDeleteService.deleteDocument(
                supplierPoid,
                "AP_SUPPLIER_MASTER",
                "SUPPLIER_POID",
                deleteReasonDto,
                supplierMasterEntity.getCreatedDate()
        );
    }

    @Override
    @Transactional
    public SupplierMasterDto updateSupplierMaster(Long supplierPoid, SupplierMasterDto supplierMasterDto) {
        SupplierMasterEntity existingEntity = supplierMasterRepository.findBySupplierPoid(supplierPoid);
        if (existingEntity == null) {
            throw new ResourceNotFoundException("Supplier Master", "supplierPoid", supplierPoid);
        }

        // Create a copy of the existing entity for logging
        SupplierMasterEntity oldEntity = new SupplierMasterEntity();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        boolean existsByGroupPoid = groupRepository.existsByGroupPoid(UserContext.getGroupPoid());

        if (!existsByGroupPoid) {
            throw new ResourceNotFoundException("Group", "groupPoid", UserContext.getGroupPoid());
        }

        boolean existsBySupplierCategoryPoid = supplierCategoryRepository.existsBySupplierCategoryPoid(supplierMasterDto.getSupplierCategoryPoid());

        if (!existsBySupplierCategoryPoid) {
            throw new ResourceNotFoundException("Supplier Category", "supplierCategoryPoid", supplierMasterDto.getSupplierCategoryPoid());
        }

        if (supplierMasterDto.getCountryPoid() != null) {
            LovGetListDto lovGetListDto = lovDataService.getDetailsByPoidAndLovName(supplierMasterDto.getCountryPoid(), "COUNTRY");
            if (lovGetListDto.getCode() == null) {
                throw new ResourceNotFoundException("Country", "countryPoid", supplierMasterDto.getCountryPoid());
            }
        }

        if (supplierMasterDto.getPurchaserPoid() != null) {
            boolean existsByEmployeePoid = hrEmployeeMasterRepository.existsByEmployeePoid(supplierMasterDto.getPurchaserPoid());

            if (!existsByEmployeePoid) {
                throw new ResourceNotFoundException("Purchaser", "purchaserPoid", supplierMasterDto.getPurchaserPoid());
            }
        }

        if (supplierMasterDto.getCustomerPoid() != null) {
            boolean existsByCustomerPoid = salesCustomerMasterRepository.existsByCustomerPoid(supplierMasterDto.getCustomerPoid());

            if (!existsByCustomerPoid) {
                throw new ResourceNotFoundException("Customer", "customerPoid", supplierMasterDto.getCustomerPoid());
            }
        }

        String normalizedName = supplierMasterDto.getSupplierName().trim().toLowerCase();

        boolean existsBySupplierNameAndSupplierPoidNot = supplierMasterRepository.existsBySupplierNameIgnoreCaseAndSupplierPoidNot(normalizedName, supplierPoid);

        if (existsBySupplierNameAndSupplierPoidNot) {
            throw new ResourceAlreadyExistsException("Supplier Name", supplierMasterDto.getSupplierName());
        }

        boolean existsBySupplierCodeAndGroupPoidAndSupplierPoidNot = supplierMasterRepository.existsBySupplierCodeAndGroupPoidAndSupplierPoidNot(supplierMasterDto.getSupplierCode(), UserContext.getGroupPoid(), supplierPoid);

        if (existsBySupplierCodeAndGroupPoidAndSupplierPoidNot) {
            throw new ResourceAlreadyExistsException("Supplier Code", supplierMasterDto.getSupplierCode());
        }

        boolean existsBySupplierNameIgnoreCaseAndGroupPoidAndSupplierPoidNot = supplierMasterRepository.existsBySupplierNameIgnoreCaseAndGroupPoidAndSupplierPoidNot(normalizedName, UserContext.getGroupPoid(), supplierPoid);

        if (existsBySupplierNameIgnoreCaseAndGroupPoidAndSupplierPoidNot) {
            throw new ResourceAlreadyExistsException("Supplier Name", supplierMasterDto.getSupplierName());
        }

        boolean exists = supplierMasterRepository.existsBySupplierCodeAndGroupPoidAndCountryPoidAndSupplierNameIgnoreCaseAndSupplierPoidNot(supplierMasterDto.getSupplierCode(), UserContext.getGroupPoid(), supplierMasterDto.getCountryPoid(), supplierMasterDto.getSupplierName(), supplierMasterDto.getSupplierPoid());

        if (exists) {
            String key = "Supplier code + Group + Country + Name";
            String value = String.format("%s / %s / %s / %s", supplierMasterDto.getSupplierCode(), UserContext.getGroupPoid(), supplierMasterDto.getCountryPoid(), supplierMasterDto.getSupplierName());
            throw new ResourceAlreadyExistsException(key, value);
        }

        if (supplierMasterDto.getAddressPoid() == null && StringUtils.isNotBlank(supplierMasterDto.getAddressName())) {
            AddressMasterUpsertDto addressRequest = new AddressMasterUpsertDto();
            addressRequest.setAddressName(supplierMasterDto.getAddressName());
            addressRequest.setAddressName2(supplierMasterDto.getAddressName());
            addressRequest.setGroupPoid(UserContext.getGroupPoid());
            addressRequest.setCountryId(supplierMasterDto.getCountryPoid());
            addressRequest.setActive("Y");
            addressRequest.setSeqno(supplierMasterDto.getSeqNo());
            addressRequest.setAddressTypeMap(supplierMasterDto.getAddressTypeMap());
            AddressMasterResponse addressResponse = addressMasterServiceClient.upsert(addressRequest);
            supplierMasterDto.setAddressPoid(addressResponse.getAddressMasterPoid());
        } else if (supplierMasterDto.getAddressPoid() != null && supplierMasterDto.getAddressTypeMap() != null) {
            AddressMasterUpsertDto addressRequest = new AddressMasterUpsertDto();
            addressRequest.setAddressMasterPoid(supplierMasterDto.getAddressPoid());
            addressRequest.setAddressName(supplierMasterDto.getAddressName());
            addressRequest.setGroupPoid(UserContext.getGroupPoid());
            addressRequest.setCountryId(supplierMasterDto.getCountryPoid());
            addressRequest.setActive("Y");
            addressRequest.setSeqno(supplierMasterDto.getSeqNo());
            addressRequest.setAddressTypeMap(supplierMasterDto.getAddressTypeMap());
            addressMasterServiceClient.upsert(addressRequest);
        }

        BeanUtils.copyProperties(supplierMasterDto, existingEntity, "supplierPoid", "supplierCode", "createdBy", "createdDate");
        existingEntity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        existingEntity.setLastModifiedDate(LocalDate.now());
        SupplierMasterEntity updatedEntity = supplierMasterRepository.save(existingEntity);

        if (supplierMasterDto.getPaymentDtl() != null && !supplierMasterDto.getPaymentDtl().isEmpty()) {
            processPaymentDtl(supplierPoid, supplierMasterDto.getPaymentDtl());
        }

        if (supplierMasterDto.getManagementDtl() != null && !supplierMasterDto.getManagementDtl().isEmpty()) {
            processManagementDtl(supplierPoid, supplierMasterDto.getManagementDtl());
        }

        if (supplierMasterDto.getServiceDtl() != null && !supplierMasterDto.getServiceDtl().isEmpty()) {
            processServiceDtl(supplierPoid, supplierMasterDto.getServiceDtl());
        }

        if (supplierMasterDto.getQuestionaries() != null && !supplierMasterDto.getQuestionaries().isEmpty()) {
            processQuestionaries(supplierPoid, supplierMasterDto.getQuestionaries());
        }

        callSupplierValidationProcedure(UserContext.getGroupPoid(),
                supplierMasterDto.getCustomerPoid(),
                UserContext.getUserPoid(),
                "Y",
                supplierMasterDto.getSupplierPoid());
        
        // Log the update
        String key = updatedEntity.getSupplierPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, updatedEntity, SupplierMasterEntity.class, 
                docId, key, LogDetailsEnum.MODIFIED, "SUPPLIER_POID");
        
        return getSupplierMaster(supplierPoid);
    }

    @Override
    @Transactional
    public Long createLedger(Long supplierPoid, GlobalLedgerDto request) {
        SupplierMasterEntity supplier = supplierMasterRepository.findBySupplierPoid(supplierPoid);
        if (supplier == null) {
            throw new ResourceNotFoundException("Supplier", "supplierPoid", supplierPoid);
        }

        // If supplier already has GL_POID, return existing GL (no duplicate creation)
        if (supplier.getGlPoid() != null) {
            throw new ResourceAlreadyExistsException("Gl poid", supplier.getGlPoid().toString());
        }

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_MASTER_CREATE");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CODE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DESC", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_GL_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_NEW_GL_POID", Long.class, ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", supplier.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", supplier.getCustomerPoid() != null ? supplier.getCustomerPoid() : supplier.getGroupPoid());
        query.setParameter("P_LOGIN_USER", request.getRequestedBy());
        query.setParameter("P_CODE", supplier.getSupplierCode());
        query.setParameter("P_DESC", supplier.getSupplierName());
        query.setParameter("P_GL_TYPE", request.getGlType());

        query.execute();

        String status = (String) query.getOutputParameterValue("P_STATUS");
        Long newGlPoid = (Long) query.getOutputParameterValue("P_NEW_GL_POID");

        if (status != null && status.startsWith("SUCCESS")) {
            // Update supplier with new GL_POID and audit fields
            supplier.setGlPoid(newGlPoid);
            supplier.setLastModifiedBy(request.getRequestedBy());
            supplier.setLastModifiedDate(LocalDate.now());
            supplierMasterRepository.save(supplier);
            return newGlPoid;
        } else if (status != null && status.contains("GL Code Already exist")) {
            throw new ResourceAlreadyExistsException("GL Code", supplier.getSupplierCode());
        } else {
            throw new AsgException(status != null ? status : "PROC_GL_MASTER_CREATE failed: Check Global fix variables");
        }
    }

    @Override
    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "SUPPLIER_CODE",
                "SUPPLIER_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public Long importSuppliersFromExcel(MultipartFile file, Long supplierPoid) {
        try {
            // Validate that supplierPoid is provided
            if (supplierPoid == null) {
                throw new ValidationException("supplierPoid is required for updating existing suppliers. Please provide the supplier POID of the supplier you want to update.");
            }

            // Use supplierPoid as TRANSACTION_POID so the stored procedure can find the supplier
            Long transactionPoid = supplierPoid;

            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            String sql = "INSERT INTO TEMP_SUPPLIER_MSTR_IMPORT_TML " +
                    "(TRANSACTION_POID , COLUMN_1, COLUMN_2, COLUMN_3, COLUMN_4, COLUMN_5, COLUMN_6 ) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    jdbcTemplate.update(sql,
                            transactionPoid,  // ← IMPORTANT - This is now the supplier POID
                            getCellValue(row.getCell(0)),
                            getCellValue(row.getCell(1)),
                            getCellValue(row.getCell(2)),
                            getCellValue(row.getCell(3)),
                            getCellValue(row.getCell(4)),
                            getCellValue(row.getCell(5))
                    );
                }
            }

            workbook.close();
            return transactionPoid;

        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel file: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public SupplierImportResponseDto processImportedSuppliers(SupplierImportRequestDto request) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AP_SUPPLIER_EXCEL_PROCESS")
                .registerStoredProcedureParameter("groupPoid", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("companyPoid", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("userPoid", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("transactionPoid", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("status", String.class, ParameterMode.OUT)
                .setParameter("groupPoid", request.getGroupPoid())
                .setParameter("companyPoid", request.getCompanyPoid())
                .setParameter("userPoid", request.getUserPoid())
                .setParameter("transactionPoid", request.getTransactionPoid());

        query.execute();
        String status = (String) query.getOutputParameterValue("status");

        SupplierImportResponseDto response = new SupplierImportResponseDto();
        response.setStatus(status);
        response.setProcessedRows(0); // proc does not return row level details
        response.setFailedRows(0);
        response.setErrors(List.of());

        return response;
    }

    @Transactional
    public SupplierMasterDto createSupplierMaster(SupplierMasterDto supplierMasterDto) {

        boolean existsByGroupPoid = groupRepository.existsByGroupPoid(UserContext.getGroupPoid());

        if (!existsByGroupPoid) {
            throw new ResourceNotFoundException("Group", "groupPoid", UserContext.getGroupPoid());
        }

        boolean existsBySupplierCategoryPoid = supplierCategoryRepository.existsBySupplierCategoryPoid(supplierMasterDto.getSupplierCategoryPoid());

        if (!existsBySupplierCategoryPoid) {
            throw new ResourceNotFoundException("Supplier Category", "supplierCategoryPoid", supplierMasterDto.getSupplierCategoryPoid());
        }

        if (supplierMasterDto.getCountryPoid() != null) {
            LovGetListDto lovGetListDto = lovDataService.getDetailsByPoidAndLovName(supplierMasterDto.getCountryPoid(), "COUNTRY");
            if (lovGetListDto.getCode() == null) {
                throw new ResourceNotFoundException("Country", "countryPoid", supplierMasterDto.getCountryPoid());
            }
        }

        if (supplierMasterDto.getPurchaserPoid() != null) {
            boolean existsByEmployeePoid = hrEmployeeMasterRepository.existsByEmployeePoid(supplierMasterDto.getPurchaserPoid());

            if (!existsByEmployeePoid) {
                throw new ResourceNotFoundException("Purchaser", "purchaserPoid", supplierMasterDto.getPurchaserPoid());
            }
        }

        if (supplierMasterDto.getCustomerPoid() != null) {
            boolean existsByCustomerPoid = salesCustomerMasterRepository.existsByCustomerPoid(supplierMasterDto.getCustomerPoid());

            if (!existsByCustomerPoid) {
                throw new ResourceNotFoundException("Customer", "customerPoid", supplierMasterDto.getCustomerPoid());
            }
        }

        boolean existsBySupplierCodeAndGroupPoid = supplierMasterRepository.existsBySupplierCodeAndGroupPoid(supplierMasterDto.getSupplierCode(), UserContext.getGroupPoid());

        if (existsBySupplierCodeAndGroupPoid) {
            throw new ResourceAlreadyExistsException("Supplier code", supplierMasterDto.getSupplierCode());
        }

        boolean existsBySupplierCodeAndGroupPoidAndCountryPoid = supplierMasterRepository.existsBySupplierCodeAndGroupPoidAndCountryPoid(supplierMasterDto.getSupplierCode(), UserContext.getGroupPoid(), supplierMasterDto.getCountryPoid());

        String errorKeyForCodeGroupCountry = "Supplier code + Group + Country";
        String errorValueForCodeGroupCountry = String.format("%s / %s / %s", supplierMasterDto.getSupplierCode(), UserContext.getGroupPoid(), supplierMasterDto.getCountryPoid());

        if (existsBySupplierCodeAndGroupPoidAndCountryPoid) {
            throw new ResourceAlreadyExistsException(errorKeyForCodeGroupCountry, errorValueForCodeGroupCountry);
        }

        String normalizedName = supplierMasterDto.getSupplierName().trim().toLowerCase();

        boolean existsByNormalizedSupplierName = supplierMasterRepository.existsBySupplierNameIgnoreCaseAndGroupPoid(normalizedName, UserContext.getGroupPoid());

        if (existsByNormalizedSupplierName) {
            throw new ResourceAlreadyExistsException("Supplier Name", supplierMasterDto.getSupplierName());
        }

        if (supplierMasterDto.getAddressPoid() == null && StringUtils.isNotBlank(supplierMasterDto.getAddressName())) {
            AddressMasterUpsertDto addressRequest = new AddressMasterUpsertDto();
            addressRequest.setAddressName(supplierMasterDto.getAddressName());
            addressRequest.setAddressName2(supplierMasterDto.getAddressName());
            addressRequest.setGroupPoid(UserContext.getGroupPoid());
            addressRequest.setCountryId(supplierMasterDto.getCountryPoid());
            addressRequest.setActive("Y");
            addressRequest.setSeqno(supplierMasterDto.getSeqNo());
            addressRequest.setAddressTypeMap(supplierMasterDto.getAddressTypeMap());
            AddressMasterResponse addressResponse = addressMasterServiceClient.upsert(addressRequest);
            supplierMasterDto.setAddressPoid(addressResponse.getAddressMasterPoid());
        } else if (supplierMasterDto.getAddressPoid() != null && supplierMasterDto.getAddressTypeMap() != null) {
            AddressMasterUpsertDto addressRequest = new AddressMasterUpsertDto();
            addressRequest.setAddressMasterPoid(supplierMasterDto.getAddressPoid());
            addressRequest.setAddressName(supplierMasterDto.getAddressName());
            addressRequest.setGroupPoid(UserContext.getGroupPoid());
            addressRequest.setCountryId(supplierMasterDto.getCountryPoid());
            addressRequest.setActive("Y");
            addressRequest.setSeqno(supplierMasterDto.getSeqNo());
            addressRequest.setAddressTypeMap(supplierMasterDto.getAddressTypeMap());
            addressMasterServiceClient.upsert(addressRequest);
        }

        SupplierMasterEntity entity = mapToEntity(supplierMasterDto);
        SupplierMasterEntity savedEntity = supplierMasterRepository.save(entity); // save parent first

        entityManager.flush();   // ensure INSERT actually runs
        entityManager.refresh(savedEntity); // reload with trigger-updated values

        // For POST, only process isCreated actions (or treat as isCreated if actionType not provided)
        if (supplierMasterDto.getPaymentDtl() != null && !supplierMasterDto.getPaymentDtl().isEmpty()) {
            List<SupplierMasterPaymentDtlDto> createdPayments = supplierMasterDto.getPaymentDtl().stream()
                    .filter(p -> {
                        String action = StringUtils.isBlank(p.getActionType()) ? "isCreated" : p.getActionType();
                        return "isCreated".equalsIgnoreCase(action);
                    })
                    .toList();
            if (!createdPayments.isEmpty()) {
                processPaymentDtl(savedEntity.getSupplierPoid(), createdPayments);
            }
        }

        if (supplierMasterDto.getManagementDtl() != null && !supplierMasterDto.getManagementDtl().isEmpty()) {
            List<SupplierMasterManagementDtlDto> createdMgmt = supplierMasterDto.getManagementDtl().stream()
                    .filter(m -> {
                        String action = StringUtils.isBlank(m.getActionType()) ? "isCreated" : m.getActionType();
                        return "isCreated".equalsIgnoreCase(action);
                    })
                    .toList();
            if (!createdMgmt.isEmpty()) {
                processManagementDtl(savedEntity.getSupplierPoid(), createdMgmt);
            }
        }

        if (supplierMasterDto.getQuestionaries() != null && !supplierMasterDto.getQuestionaries().isEmpty()) {
            List<SupplierMasterQstnDtlDto> createdQstn = supplierMasterDto.getQuestionaries().stream()
                    .filter(q -> {
                        String action = StringUtils.isBlank(q.getActionType()) ? "isCreated" : q.getActionType();
                        return "isCreated".equalsIgnoreCase(action);
                    })
                    .toList();
            if (!createdQstn.isEmpty()) {
                processQuestionaries(savedEntity.getSupplierPoid(), createdQstn);
            }
        }

        if (supplierMasterDto.getServiceDtl() != null && !supplierMasterDto.getServiceDtl().isEmpty()) {
            List<SupplierMasterServiceDtlDto> createdService = supplierMasterDto.getServiceDtl().stream()
                    .filter(s -> {
                        String action = StringUtils.isBlank(s.getActionType()) ? "isCreated" : s.getActionType();
                        return "isCreated".equalsIgnoreCase(action);
                    })
                    .toList();
            if (!createdService.isEmpty()) {
                processServiceDtl(savedEntity.getSupplierPoid(), createdService);
            }
        }

        callSupplierValidationProcedure(UserContext.getGroupPoid(), supplierMasterDto.getCustomerPoid(), UserContext.getUserPoid(), "Y", supplierMasterDto.getSupplierPoid());

        // Log the creation
        String key = savedEntity.getSupplierPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        return getSupplierMaster(savedEntity.getSupplierPoid());
    }

    private GLMasterResponseDto toGLMasterDto(Optional<GLMaster> optionalGLMaster) {
        return optionalGLMaster
                .map(entity -> {
                    GLMasterResponseDto dto = new GLMasterResponseDto();
                    dto.setGlPoid(entity.getGlPoid());
                    dto.setGlCode(entity.getGlCode());
                    dto.setDescription(entity.getGlDescription());
                    dto.setDescription2(entity.getGlDescription2());
                    dto.setType(entity.getGlType());
                    dto.setSubOf(entity.getGroupGlPoid());
                    dto.setAccountType(entity.getGlAcType());
                    dto.setControlAcType(entity.getControlAcNature());
                    dto.setCostGroup(entity.getCostGroup());
                    dto.setBillWise("Y".equalsIgnoreCase(entity.getBillwise()));
                    dto.setPrepaymentLedger("Y".equalsIgnoreCase(entity.getPrepaymentLedger()));
                    dto.setInterCompany("Y".equalsIgnoreCase(entity.getInterCompanyAc()));
                    dto.setInterCompanyId(entity.getInterCompanyPoid());
                    dto.setRemarks(entity.getRemarks());
                    dto.setSeqNo(entity.getSeqno());
                    dto.setActive("Y".equalsIgnoreCase(entity.getActive()));
                    dto.setDeleted("Y".equalsIgnoreCase(entity.getDeleted()));
                    return dto;
                })
                .orElse(null);
    }

    private SupplierCategoryDto toSupplierCategoryDto(SupplierCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        SupplierCategoryDto dto = new SupplierCategoryDto();
        dto.setSupplierCategoryPoid(entity.getSupplierCategoryPoid());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setSupplierCategoryCode(entity.getSupplierCategoryCode());
        dto.setSupplierCategoryName(entity.getSupplierCategoryName());
        dto.setSupplierCategoryName2(entity.getSupplierCategoryName2());
        dto.setActive(entity.getActive());

        if (entity.getSequenceNumber() != null) {
            dto.setSeqNo(String.valueOf(entity.getSequenceNumber()));
        }

        dto.setGeneralRemarks(entity.getGeneralRemarks());
        dto.setDeleted(entity.getDeleted());

        return dto;
    }

    private SalesCustomerMasterDto toSalesCustomerMasterDto(SalesCustomerMaster entity) {
        if (entity == null) {
            return null;
        }
        SalesCustomerMasterDto dto = new SalesCustomerMasterDto();
        dto.setCustomerPoid(entity.getCustomerPoid());
        dto.setCustomerCode(entity.getCustomerCode());
        dto.setCustomerName(entity.getCustomerName());
        dto.setCustomerName2(entity.getCustomerName2());
        dto.setCustomerCategoryPoid(entity.getCustomerCategoryPoid());
        dto.setAddressPoid(entity.getAddressPoid());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCreditLimit(entity.getCreditLimit());
        dto.setCreditPeriod(entity.getCreditPeriod());
        dto.setSalesmanPoid(entity.getSalesmanPoid());
        dto.setCrRegno(entity.getCrRegno());
        dto.setContractExpiry(entity.getContractExpiry());
        dto.setContractExpiryReason(entity.getContractExpiryReason());
        dto.setBlockedCustomer(entity.getBlockedCustomer());
        dto.setBlockedReason(entity.getBlockedReason());
        dto.setPaymentTerms(entity.getPaymentTerms());
        dto.setDeliveryTerms(entity.getDeliveryTerms());
        dto.setAccountName(entity.getAccountName());
        dto.setBankAc(entity.getBankAc());
        dto.setSwiftCode(entity.getSwiftCode());
        dto.setIban(entity.getIban());
        dto.setAccountNo(entity.getAccountNo());
        dto.setGlAcct(entity.getGlAcct());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setCreditType(entity.getCreditType());
        dto.setActive(entity.getActive());
        dto.setCustomerType(entity.getCustomerType());
        dto.setCurrencyRate(entity.getCurrencyRate());
        dto.setRateExpiryDate(entity.getRateExpiryDate());
        dto.setDeleted(entity.getDeleted());

        return dto;
    }

    private CurrencyLightDto toCurrencyDto(Optional<CurrencyEntity> optionalCurrencyEntity) {
        return optionalCurrencyEntity
                .map(entity -> {
                    CurrencyLightDto dto = new CurrencyLightDto();
                    dto.setCurrencyPoid(entity.getCurrencyPoid());
                    dto.setCurrencyCode(entity.getCurrencyCode());
                    dto.setCurrencyName(entity.getCurrencyName());
                    return dto;
                })
                .orElse(null);
    }

    private HrEmployeeDto toHrEmployeeDto(Optional<HrEmployeeMaster> optionalHrEmployeeMaster) {
        return optionalHrEmployeeMaster
                .map(entity -> {
                    HrEmployeeDto dto = new HrEmployeeDto();
                    dto.setEmployeePoid(entity.getEmployeePoid());
                    dto.setGroupPoid(entity.getGroupPoid());
                    dto.setEmployeeCode(entity.getEmployeeCode());
                    dto.setEmployeeName(entity.getEmployeeName());
                    dto.setEmployeeName2(entity.getEmployeeName2());
                    dto.setBaseGroup(entity.getBaseGroup());
                    dto.setLocationPoid(entity.getLocationPoid());
                    dto.setGender(entity.getGender());
                    dto.setMaritalStatus(entity.getMaritalStatus());
                    dto.setJoinDate(entity.getJoinDate());
                    dto.setDateOfBirth(entity.getDateOfBirth());
                    dto.setPresentAddress(entity.getPresentAddress());
                    dto.setPermanentAddress(entity.getPermanentAddress());
                    dto.setPostalAddress(entity.getPostalAddress());
                    dto.setHomeCountryPhone(entity.getHomeCountryPhone());
                    dto.setMobile(entity.getMobile());
                    dto.setPersonalEmail(entity.getPersonalEmail());
                    dto.setBusinessEmail(entity.getBusinessEmail());
                    dto.setBloodGroup(entity.getBloodGroup());
                    dto.setEmergencyContactPerson(entity.getEmergencyContactPerson());
                    dto.setEmergencyContactNo(entity.getEmergencyContactNo());
                    dto.setServiceStartDate(entity.getServiceStartDate());
                    dto.setServiceType(entity.getServiceType());
                    dto.setContractStart(entity.getContractStart());
                    dto.setContractEnd(entity.getContractEnd());
                    dto.setProbation(entity.getProbation());
                    dto.setNoticePeriod(entity.getNoticePeriod());
                    dto.setLoginUserPoid(entity.getLoginUserPoid());
                    dto.setJobDescription(entity.getJobDescription());
                    dto.setTicketPeriod(entity.getTicketPeriod());
                    dto.setNoOfTickets(entity.getNoOfTickets());
                    dto.setAccessCardIssued(entity.getAccessCardIssued());
                    dto.setDiscontinued(entity.getDiscontinued());
                    dto.setDiscontinuedDate(entity.getDiscontinuedDate());
                    dto.setReason(entity.getReason());
                    dto.setBasicSalary(entity.getBasicSalary());
                    dto.setNetSalary(entity.getNetSalary());
                    dto.setCurrencyPoid(entity.getCurrencyPoid());
                    dto.setPaymentMethod(entity.getPaymentMethod());
                    dto.setBankPoid(entity.getBankPoid());
                    dto.setAccountNo(entity.getAccountNo());
                    dto.setIban(entity.getIban());
                    dto.setRegisteredSalary(entity.getRegisteredSalary());
                    dto.setLastIncrementDate(entity.getLastIncrementDate());
                    dto.setNextIncrementDate(entity.getNextIncrementDate());
                    dto.setHoldSalary(entity.getHoldSalary());
                    dto.setHoldReason(entity.getHoldReason());
                    dto.setPassportNo(entity.getPassportNo());
                    dto.setPlaceOfIssue(entity.getPlaceOfIssue());
                    dto.setPassportPossessedBy(entity.getPassportPossessedBy());
                    dto.setGosiNo(entity.getGosiNo());
                    dto.setGosiReturnAmount(entity.getGosiReturnAmount());
                    dto.setCprNo(entity.getCprNo());
                    dto.setCprExpiryDate(entity.getCprExpiryDate());
                    dto.setCprOccupation(entity.getCprOccupation());
                    dto.setMedicalInsurance(entity.getMedicalInsurance());
                    dto.setMedicalInsuranceExpiryDate(entity.getMedicalInsuranceExpiryDate());
                    dto.setRpNo(entity.getRpNo());
                    dto.setRpStartDate(entity.getRpStartDate());
                    dto.setRpExpiryDate(entity.getRpExpiryDate());
                    dto.setActive(entity.getActive());
                    dto.setCreatedBy(entity.getCreatedBy());
                    dto.setCreatedDate(entity.getCreatedDate());
                    dto.setLastModifiedBy(entity.getLastModifiedBy());
                    dto.setLastModifiedDate(entity.getLastModifiedDate());
                    dto.setEmployeeBiomatrixId(entity.getEmployeeBiomatrixId());
                    dto.setOtApplicable(entity.getOtApplicable());
                    dto.setInsuranceNominee(entity.getInsuranceNominee());
                    dto.setInsuranceNomineeRelation(entity.getInsuranceNomineeRelation());
                    dto.setShiftPoid(entity.getShiftPoid());
                    dto.setCurrencyCode(entity.getCurrencyCode());
                    dto.setDeleted(entity.getDeleted());
                    dto.setDrivingLicExp(entity.getDrivingLicExp());
                    dto.setNomineeContactDtl(entity.getNomineeContactDtl());
                    dto.setDisplayName(entity.getDisplayName());
                    return dto;
                })
                .orElse(null);
    }

    private SupplierServicesMasterDto toSupplierServicesMasterDto(SupplierServicesMasterEntity entity) {
        if (entity == null) {
            return null;
        }
        SupplierServicesMasterDto dto = new SupplierServicesMasterDto();
        dto.setServicePoid(entity.getServicePoid());
        dto.setServiceName(entity.getServiceName());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setDeleted(entity.getDeleted());

        return dto;
    }

    private SupplierMasterPaymentDtlDto mapSupplierMasterPaymentDtlEntityToDto(SupplierMasterPaymentDtlEntity supplierMasterPaymentDtlEntity) {
        SupplierMasterPaymentDtlDto supplierMasterPaymentDtlDto = new SupplierMasterPaymentDtlDto();
        supplierMasterPaymentDtlDto.setSupplierPoid(supplierMasterPaymentDtlEntity.getId().getSupplierPoid());
        supplierMasterPaymentDtlDto.setDetRowId(supplierMasterPaymentDtlEntity.getId().getDetRowId());
        BeanUtils.copyProperties(supplierMasterPaymentDtlEntity, supplierMasterPaymentDtlDto);

        if (supplierMasterPaymentDtlEntity.getIntermediaryCountryPoid() != null) {
            LovGetListDto lovGetListDto = lovDataService.getDetailsByPoidAndLovName(supplierMasterPaymentDtlEntity.getIntermediaryCountryPoid(), "COUNTRY");
            if (lovGetListDto != null && lovGetListDto.getCode() != null) {
                supplierMasterPaymentDtlDto.setIntermediaryCountryDetail(lovGetListDto);
            }
        }
        if (supplierMasterPaymentDtlEntity.getBeneficiaryCountry() != null) {
            LovGetListDto lovGetListDto = lovDataService.getDetailsByPoidAndLovName(supplierMasterPaymentDtlEntity.getBeneficiaryCountry(), "COUNTRY");
            if (lovGetListDto != null && lovGetListDto.getCode() != null) {
                supplierMasterPaymentDtlDto.setBeneficiaryCountryDetail(lovGetListDto);
            }
        }

        return supplierMasterPaymentDtlDto;
    }

    private SupplierMasterManagementDtlDto mapSupplierMasterManagementDtlEntityToDto(SupplierMasterManagementDtlEntity supplierMasterManagementDtlEntity) {
        SupplierMasterManagementDtlDto supplierMasterManagementDtlDto = new SupplierMasterManagementDtlDto();
        supplierMasterManagementDtlDto.setSupplierPoid(supplierMasterManagementDtlEntity.getId().getSupplierPoid());
        supplierMasterManagementDtlDto.setDetRowId(supplierMasterManagementDtlEntity.getId().getDetRowId());
        BeanUtils.copyProperties(supplierMasterManagementDtlEntity, supplierMasterManagementDtlDto);
        return supplierMasterManagementDtlDto;
    }

    private SupplierMasterServiceDtlDto mapServiceDtlEntityToDto(SupplierMasterServiceDtlEntity serviceDtlEntity) {
        if (serviceDtlEntity == null) {
            return null;
        }

        SupplierMasterServiceDtlDto supplierMasterServiceDtlDto = new SupplierMasterServiceDtlDto();

        if (serviceDtlEntity.getId() != null) {
            supplierMasterServiceDtlDto.setSupplierPoid(serviceDtlEntity.getId().getSupplierPoid());
            supplierMasterServiceDtlDto.setDetRowId(serviceDtlEntity.getId().getDetRowId());
        }

        BeanUtils.copyProperties(serviceDtlEntity, supplierMasterServiceDtlDto);
        if (serviceDtlEntity.getServicePoid() != null)
            supplierMasterServiceDtlDto.setService(toSupplierServicesMasterDto(supplierServicesMasterRepository.findByServicePoid(serviceDtlEntity.getServicePoid())));
        return supplierMasterServiceDtlDto;
    }

    private SupplierMasterQstnDtlDto mapSupplierMasterQstnDtlEntityToDto(SupplierMasterQstnDtlEntity supplierMasterQstnDtlEntity) {
        SupplierMasterQstnDtlDto supplierMasterQstnDto = new SupplierMasterQstnDtlDto();
        supplierMasterQstnDto.setSupplierPoid(supplierMasterQstnDtlEntity.getId().getSupplierPoid());
        supplierMasterQstnDto.setDetRowId(supplierMasterQstnDtlEntity.getId().getDetRowId());
        BeanUtils.copyProperties(supplierMasterQstnDtlEntity, supplierMasterQstnDto);
        return supplierMasterQstnDto;
    }

    private void processPaymentDtl(Long supplierPoid, List<SupplierMasterPaymentDtlDto> paymentDtlList) {
        List<SupplierMasterPaymentDtlEntity> entitiesToDelete = new ArrayList<>();
        List<SupplierMasterPaymentDtlEntity> entitiesToSave = new ArrayList<>();
        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        for (SupplierMasterPaymentDtlDto paymentDto : paymentDtlList) {
            String actionType = StringUtils.isBlank(paymentDto.getActionType()) ? null : paymentDto.getActionType();

            // Handle backward compatibility: if actionType is null, determine from detRowId
            if (actionType == null) {
                actionType = (paymentDto.getDetRowId() == null) ? "isCreated" : "isUpdated";
            }

            switch (actionType.toLowerCase()) {
                case "isdeleted" -> {
                    if (paymentDto.getDetRowId() != null) {
                        SupplierMasterPaymentDtlEntity entity = supplierMasterPaymentDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, paymentDto.getDetRowId());
                        if (entity != null) {
                            entitiesToDelete.add(entity);
                        }
                    }
                }
                case "nochange" -> {
                    // Skip processing
                    continue;
                }
                case "iscreated" -> {
                    SupplierMasterPaymentDtlEntity newEntity = new SupplierMasterPaymentDtlEntity();
                    BeanUtils.copyProperties(paymentDto, newEntity);
                    SupplierPaymentDetailId id = new SupplierPaymentDetailId();
                    id.setSupplierPoid(supplierPoid);
                    id.setDetRowId(getNextDetRowIdForPaymentDtl(supplierPoid));
                    newEntity.setId(id);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    entitiesToSave.add(newEntity);
                }
                case "isupdated" -> {
                    if (paymentDto.getDetRowId() != null) {
                        SupplierMasterPaymentDtlEntity entity = supplierMasterPaymentDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, paymentDto.getDetRowId());
                        if (entity != null) {
                            BeanUtils.copyProperties(paymentDto, entity);
                            entity.setLastModifiedBy(currentUser);
                            entity.setLastModifiedDate(now);
                            entitiesToSave.add(entity);
                        } else {
                            // Entity not found, treat as create
                            SupplierMasterPaymentDtlEntity newEntity = new SupplierMasterPaymentDtlEntity();
                            BeanUtils.copyProperties(paymentDto, newEntity);
                            SupplierPaymentDetailId id = new SupplierPaymentDetailId();
                            id.setSupplierPoid(supplierPoid);
                            id.setDetRowId(paymentDto.getDetRowId());
                            newEntity.setId(id);
                            newEntity.setCreatedBy(currentUser);
                            newEntity.setCreatedDate(now);
                            newEntity.setLastModifiedBy(currentUser);
                            newEntity.setLastModifiedDate(now);
                            entitiesToSave.add(newEntity);
                        }
                    }
                }
            }
        }

        // Batch delete operations
        if (!entitiesToDelete.isEmpty()) {
            supplierMasterPaymentDtlRepository.deleteAll(entitiesToDelete);
        }

        // Batch save operations
        if (!entitiesToSave.isEmpty()) {
            supplierMasterPaymentDtlRepository.saveAll(entitiesToSave);
        }
    }

    private void processManagementDtl(Long supplierPoid, List<SupplierMasterManagementDtlDto> managementDtlList) {
        List<SupplierMasterManagementDtlEntity> entitiesToDelete = new ArrayList<>();
        List<SupplierMasterManagementDtlEntity> entitiesToSave = new ArrayList<>();
        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        for (SupplierMasterManagementDtlDto managementDto : managementDtlList) {
            String actionType = StringUtils.isBlank(managementDto.getActionType()) ? null : managementDto.getActionType();

            // Handle backward compatibility: if actionType is null, determine from detRowId
            if (actionType == null) {
                actionType = (managementDto.getDetRowId() == null) ? "isCreated" : "isUpdated";
            }

            switch (actionType.toLowerCase()) {
                case "isdeleted" -> {
                    if (managementDto.getDetRowId() != null) {
                        SupplierMasterManagementDtlEntity entity = supplierMasterMangementDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, managementDto.getDetRowId());
                        if (entity != null) {
                            entitiesToDelete.add(entity);
                        }
                    }
                }
                case "nochange" -> {
                    // Skip processing
                    continue;
                }
                case "iscreated" -> {
                    SupplierMasterManagementDtlEntity newEntity = new SupplierMasterManagementDtlEntity();
                    BeanUtils.copyProperties(managementDto, newEntity);
                    SupplierMasterMangementDtlKey id = new SupplierMasterMangementDtlKey();
                    id.setSupplierPoid(supplierPoid);
                    id.setDetRowId(getNextDetRowIdForManagementDtl(supplierPoid));
                    newEntity.setId(id);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    entitiesToSave.add(newEntity);
                }
                case "isupdated" -> {
                    if (managementDto.getDetRowId() != null) {
                        SupplierMasterManagementDtlEntity entity = supplierMasterMangementDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, managementDto.getDetRowId());
                        if (entity != null) {
                            BeanUtils.copyProperties(managementDto, entity);
                            entity.setLastModifiedBy(currentUser);
                            entity.setLastModifiedDate(now);
                            entitiesToSave.add(entity);
                        } else {
                            // Entity not found, treat as create
                            SupplierMasterManagementDtlEntity newEntity = new SupplierMasterManagementDtlEntity();
                            BeanUtils.copyProperties(managementDto, newEntity);
                            SupplierMasterMangementDtlKey id = new SupplierMasterMangementDtlKey();
                            id.setSupplierPoid(supplierPoid);
                            id.setDetRowId(managementDto.getDetRowId());
                            newEntity.setId(id);
                            newEntity.setCreatedBy(currentUser);
                            newEntity.setCreatedDate(now);
                            newEntity.setLastModifiedBy(currentUser);
                            newEntity.setLastModifiedDate(now);
                            entitiesToSave.add(newEntity);
                        }
                    }
                }
            }
        }

        // Batch delete operations
        if (!entitiesToDelete.isEmpty()) {
            supplierMasterMangementDtlRepository.deleteAll(entitiesToDelete);
        }

        // Batch save operations
        if (!entitiesToSave.isEmpty()) {
            supplierMasterMangementDtlRepository.saveAll(entitiesToSave);
        }
    }

    private void processServiceDtl(Long supplierPoid, List<SupplierMasterServiceDtlDto> serviceDtlList) {
        List<SupplierMasterServiceDtlEntity> entitiesToDelete = new ArrayList<>();
        List<SupplierMasterServiceDtlEntity> entitiesToSave = new ArrayList<>();
        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        for (SupplierMasterServiceDtlDto serviceDto : serviceDtlList) {
            // Validate servicePoid exists
            boolean existsByServicePoid = supplierServicesMasterRepository.existsByServicePoid(serviceDto.getServicePoid());
            if (!existsByServicePoid) {
                throw new ResourceNotFoundException("Service", "servicePoid", serviceDto.getServicePoid());
            }

            String actionType = StringUtils.isBlank(serviceDto.getActionType()) ? null : serviceDto.getActionType();

            // Handle backward compatibility: if actionType is null, determine from detRowId
            if (actionType == null) {
                actionType = (serviceDto.getDetRowId() == null) ? "isCreated" : "isUpdated";
            }

            switch (actionType.toLowerCase()) {
                case "isdeleted" -> {
                    if (serviceDto.getDetRowId() != null) {
                        SupplierMasterServiceDtlEntity entity = supplierMasterServiceDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, serviceDto.getDetRowId());
                        if (entity != null) {
                            entitiesToDelete.add(entity);
                        }
                    }
                }
                case "nochange" -> {
                    // Skip processing
                    continue;
                }
                case "iscreated" -> {
                    SupplierMasterServiceDtlEntity newEntity = new SupplierMasterServiceDtlEntity();
                    BeanUtils.copyProperties(serviceDto, newEntity);
                    SupplierMasterServiceDtlKey id = new SupplierMasterServiceDtlKey();
                    id.setSupplierPoid(supplierPoid);
                    id.setDetRowId(getNextDetRowIdForServiceDtl(supplierPoid));
                    newEntity.setId(id);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    entitiesToSave.add(newEntity);
                }
                case "isupdated" -> {
                    if (serviceDto.getDetRowId() != null) {
                        SupplierMasterServiceDtlEntity entity = supplierMasterServiceDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, serviceDto.getDetRowId());
                        if (entity != null) {
                            BeanUtils.copyProperties(serviceDto, entity);
                            entity.setLastModifiedBy(currentUser);
                            entity.setLastModifiedDate(now);
                            entitiesToSave.add(entity);
                        } else {
                            // Entity not found, treat as create
                            SupplierMasterServiceDtlEntity newEntity = new SupplierMasterServiceDtlEntity();
                            BeanUtils.copyProperties(serviceDto, newEntity);
                            SupplierMasterServiceDtlKey id = new SupplierMasterServiceDtlKey();
                            id.setSupplierPoid(supplierPoid);
                            id.setDetRowId(serviceDto.getDetRowId());
                            newEntity.setId(id);
                            newEntity.setCreatedBy(currentUser);
                            newEntity.setCreatedDate(now);
                            newEntity.setLastModifiedBy(currentUser);
                            newEntity.setLastModifiedDate(now);
                            entitiesToSave.add(newEntity);
                        }
                    }
                }
            }
        }

        // Batch delete operations
        if (!entitiesToDelete.isEmpty()) {
            supplierMasterServiceDtlRepository.deleteAll(entitiesToDelete);
        }

        // Batch save operations
        if (!entitiesToSave.isEmpty()) {
            supplierMasterServiceDtlRepository.saveAll(entitiesToSave);
        }
    }

    private void processQuestionaries(Long supplierPoid, List<SupplierMasterQstnDtlDto> questionariesList) {
        List<SupplierMasterQstnDtlEntity> entitiesToDelete = new ArrayList<>();
        List<SupplierMasterQstnDtlEntity> entitiesToSave = new ArrayList<>();
        String currentUser = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        for (SupplierMasterQstnDtlDto qstnDto : questionariesList) {
            String actionType = StringUtils.isBlank(qstnDto.getActionType()) ? null : qstnDto.getActionType();

            // Handle backward compatibility: if actionType is null, determine from detRowId
            if (actionType == null) {
                actionType = (qstnDto.getDetRowId() == null) ? "isCreated" : "isUpdated";
            }

            switch (actionType.toLowerCase()) {
                case "isdeleted" -> {
                    if (qstnDto.getDetRowId() != null) {
                        SupplierMasterQstnDtlEntity entity = supplierMasterQstnDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, qstnDto.getDetRowId());
                        if (entity != null) {
                            entitiesToDelete.add(entity);
                        }
                    }
                }
                case "nochange" -> {
                    // Skip processing
                    continue;
                }
                case "iscreated" -> {
                    SupplierMasterQstnDtlEntity newEntity = new SupplierMasterQstnDtlEntity();
                    BeanUtils.copyProperties(qstnDto, newEntity);
                    SupplierMasterQstnDtlKey id = new SupplierMasterQstnDtlKey();
                    id.setSupplierPoid(supplierPoid);
                    id.setDetRowId(getNextDetRowIdForQstnDtl(supplierPoid));
                    newEntity.setId(id);
                    newEntity.setCreatedBy(currentUser);
                    newEntity.setCreatedDate(now);
                    newEntity.setLastModifiedBy(currentUser);
                    newEntity.setLastModifiedDate(now);
                    entitiesToSave.add(newEntity);
                }
                case "isupdated" -> {
                    if (qstnDto.getDetRowId() != null) {
                        SupplierMasterQstnDtlEntity entity = supplierMasterQstnDtlRepository.findBySupplierPoidAndDetRowId(supplierPoid, qstnDto.getDetRowId());
                        if (entity != null) {
                            BeanUtils.copyProperties(qstnDto, entity);
                            entity.setLastModifiedBy(currentUser);
                            entity.setLastModifiedDate(now);
                            entitiesToSave.add(entity);
                        } else {
                            // Entity not found, treat as create
                            SupplierMasterQstnDtlEntity newEntity = new SupplierMasterQstnDtlEntity();
                            BeanUtils.copyProperties(qstnDto, newEntity);
                            SupplierMasterQstnDtlKey id = new SupplierMasterQstnDtlKey();
                            id.setSupplierPoid(supplierPoid);
                            id.setDetRowId(qstnDto.getDetRowId());
                            newEntity.setId(id);
                            newEntity.setCreatedBy(currentUser);
                            newEntity.setCreatedDate(now);
                            newEntity.setLastModifiedBy(currentUser);
                            newEntity.setLastModifiedDate(now);
                            entitiesToSave.add(newEntity);
                        }
                    }
                }
            }
        }

        // Batch delete operations
        if (!entitiesToDelete.isEmpty()) {
            supplierMasterQstnDtlRepository.deleteAll(entitiesToDelete);
        }

        // Batch save operations
        if (!entitiesToSave.isEmpty()) {
            supplierMasterQstnDtlRepository.saveAll(entitiesToSave);
        }
    }
    private void callSupplierValidationProcedure(Long groupPoid, Long companyPoid, Long userPoid, String active, Long supplierPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AP_SUPPLIER_VALIDATION")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_ACTIVE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_SUPPLIER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT)
                .setParameter("P_LOGIN_GROUP_POID", groupPoid)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_ACTIVE", active)
                .setParameter("P_SUPPLIER_POID", supplierPoid);

        query.execute();

        String status = (String) query.getOutputParameterValue("P_RESULT");

        if (!"SUCCESS...".equalsIgnoreCase(status)) {
            throw new RuntimeException("Supplier validation failed: " + status);
        }
    }

    private SupplierMasterEntity mapToEntity(SupplierMasterDto dto) {
        SupplierMasterEntity entity = new SupplierMasterEntity();

        entity.setSupplierPoid(dto.getSupplierPoid());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setSupplierName(dto.getSupplierName());
        entity.setSupplierName2(dto.getSupplierName2());
        entity.setSupplierType(dto.getSupplierType());
        entity.setSupplierCategoryPoid(dto.getSupplierCategoryPoid());
        entity.setCountryPoid(dto.getCountryPoid());
        entity.setCreditLimit(dto.getCreditLimit());
        entity.setCreditPeriod(dto.getCreditPeriod());
        entity.setCrNo(dto.getCrNo());
        entity.setContactPerson(dto.getContactPerson());
        entity.setAddressPoid(dto.getAddressPoid());
        entity.setActive(StringUtils.isBlank(dto.getActive()) ? "Y" : dto.getActive());
        entity.setSeqNo(dto.getSeqNo());
        entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
        entity.setCreatedDate(LocalDate.now());
        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDate.now());
        entity.setGeneralRemarks(dto.getGeneralRemarks());
        entity.setDeleted(StringUtils.isBlank(dto.getDeleted()) ? "N" : dto.getDeleted());
        entity.setTempPaymentName(dto.getTempPaymentName());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setCurrencyRate(dto.getCurrencyRate());
        entity.setRateExpiryDate(dto.getRateExpiryDate());
        entity.setGlPoid(dto.getGlPoid());
        entity.setDefaultWeightSelectionMethod(dto.getDefaultWeightSelectionMethod());
        entity.setProductInfo(dto.getProductInfo());
        entity.setTinNumber(dto.getTinNumber());
        entity.setTaxSlab(dto.getTaxSlab());
        entity.setExemptionReason(dto.getExemptionReason());
        entity.setTaxRegisteredDate(dto.getTaxRegisteredDate());
        entity.setPurchaserPoid(dto.getPurchaserPoid());
        entity.setGrnCreditGl(dto.getGrnCreditGl());
        entity.setAuditedYear(dto.getAuditedYear());
        entity.setAuditingFirm(dto.getAuditingFirm());
        entity.setIsoCertification(dto.getIsoCertification());
        entity.setProfileUpdated(dto.getProfileUpdated());
        entity.setProfileVatCrMismatch(dto.getProfileVatCrMismatch());
        entity.setCustomerPoid(dto.getCustomerPoid());

        return entity;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    public Long getNextDetRowIdForPaymentDtl(Long supplierPoid) {
        return Optional.ofNullable(supplierMasterPaymentDtlRepository.findMaxDetRowIdBySupplierPoid(supplierPoid)).map(id -> id + 1).orElse(1L);
    }

    public Long getNextDetRowIdForManagementDtl(Long supplierPoid) {
        return Optional.ofNullable(supplierMasterMangementDtlRepository.findMaxDetRowIdBySupplierPoid(supplierPoid)).map(id -> id + 1).orElse(1L);
    }

    public Long getNextDetRowIdForServiceDtl(Long supplierPoid) {
        return Optional.ofNullable(supplierMasterServiceDtlRepository.findMaxDetRowIdBySupplierPoid(supplierPoid)).map(id -> id + 1).orElse(1L);
    }

    public Long getNextDetRowIdForQstnDtl(Long supplierPoid) {
        return Optional.ofNullable(supplierMasterQstnDtlRepository.findMaxDetRowIdBySupplierPoid(supplierPoid)).map(id -> id + 1).orElse(1L);
    }
}
