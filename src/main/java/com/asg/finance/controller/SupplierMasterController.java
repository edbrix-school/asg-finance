package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.dto.AddressDetailsDTO;
import com.asg.common.lib.dto.AddressTypeMapDTO;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.GlobalLedgerDto;
import com.asg.finance.dto.SupplierImportRequestDto;
import com.asg.finance.dto.SupplierImportResponseDto;
import com.asg.finance.dto.SupplierMasterDto;
import com.asg.finance.service.SupplierMasterService;
import com.asg.finance.validator.MainGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.*;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/supplier")
public class SupplierMasterController {

    private final SupplierMasterService supplierMasterService;

    private final Validator validator;

    private static final Map<String, Class<?>> ADDRESS_GROUPS = Map.of(
            "MAIN", MainGroup.class

//            uncomment if needed in future
//            "FINANCE", FinanceGroup.class
//            "SALES", SalesGroup.class,
//            "OPERATIONS", OperationsGroup.class,
//            "INVOICE", InvoiceGroup.class,
//            "DELIVERY_ORDER", DeliveryOrderGroup.class,
//            "CARGO_ARRIVAL_NOTICE", CargoArrivalNoticeGroup.class,
//            "SHIP_CHANDLING", ShipChandlingGroup.class,
//            "CLAIM_UAC", ClaimUacGroup.class,
//            "CAN", CanGroup.class
    );


    @Operation(
            summary = "Get Supplier Master by ID",
            description = "Retrieves detailed information about a specific supplier including payment, management, service, and question details",
            parameters = {
                    @Parameter(
                            name = "supplierPoid",
                            description = "Unique identifier of the supplier",
                            required = true,
                            example = "149",
                            in = ParameterIn.PATH
                    ),
                    @Parameter(
                            name = "documentId",
                            description = "Document identifier for tracking",
                            required = true,
                            example = "200-001",
                            in = ParameterIn.QUERY
                    ),
                    @Parameter(
                            name = "actionRequested",
                            description = "Action to be performed on the supplier data",
                            required = true,
                            example = "view",
                            schema = @Schema(allowableValues = {"view"})
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved supplier details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SupplierMasterDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{supplierPoid}")
    public ResponseEntity<?> getSupplierMaster(@PathVariable Long supplierPoid) {
        SupplierMasterDto result = supplierMasterService.getSupplierMaster(supplierPoid);
        return success("Supplier Master Records fetched successfully", result);
    }

    @Operation(
            summary = "Delete a supplier master record",
            description = "Marks a supplier as deleted and removes all related data. This is a soft delete operation.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Supplier master record deleted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class,
                                            example = "{\"success\": true, \"message\": \"Supplier Master Record deleted successfully\"}"))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier not found with the given ID",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{supplierPoid}")
    public ResponseEntity<?> deleteSupplierMaster(
            @Parameter(description = "ID of the supplier to be deleted", required = true, example = "149")
            @PathVariable Long supplierPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        supplierMasterService.deleteSupplierMaster(supplierPoid, deleteReasonDto);
        return success("Supplier Master Record deleted successfully");
    }

    @Operation(
            summary = "Update Supplier Master",
            description = "Updates an existing supplier master record with new information including payment, management, service, and question details",
            parameters = {
                    @Parameter(
                            name = "supplierPoid",
                            description = "Unique identifier of the supplier to update",
                            required = true,
                            example = "1682391",
                            in = ParameterIn.PATH
                    ),
                    @Parameter(
                            name = "documentId",
                            description = "Document identifier for tracking",
                            required = true,
                            example = "200-001",
                            in = ParameterIn.QUERY
                    ),
                    @Parameter(
                            name = "actionRequested",
                            description = "Action to be performed on the supplier data",
                            required = true,
                            example = "EDIT",
                            schema = @Schema(allowableValues = {"EDIT"})
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Supplier master updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SupplierMasterDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Supplier not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{supplierPoid}")
    public ResponseEntity<?> updateSupplierMaster(
            @PathVariable Long supplierPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated supplier master data",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SupplierMasterDto.class),
                            examples = @ExampleObject(
                                    name = "Update Supplier Example",
                                    value = """
                                            {
                                                 "groupPoid": 1,
                                                 "supplierName": "ABC Trading Co.42",
                                                 "supplierName2": "ABC Trading LLC42",
                                                 "supplierType": "Local",
                                                 "supplierCategoryPoid": 146,
                                                 "countryPoid": 180,
                                                 "creditLimit": 50000,
                                                 "creditPeriod": 30,
                                                 "crNo": "CR123456",
                                                 "contactPerson": "John Doe",
                                                 "active": "Y",
                                                 "seqNo": 1,
                                                 "generalRemarks": "Preferred supplier for office equipment.1",
                                                 "deleted": "N",
                                                 "tempPaymentName": "Temp Supplier",
                                                 "currencyCode": "USD",
                                                 "currencyRate": 1,
                                                 "rateExpiryDate": "2023-12-31",
                                                 "glPoid": 19760,
                                                 "defaultWeightSelectionMethod": "AUTO",
                                                 "productInfo": "Office supplies and electronics1",
                                                 "tinNumber": "TIN789456123",
                                                 "taxSlab": "5%",
                                                 "exemptionReason": null,
                                                 "taxRegisteredDate": "2020-01-01",
                                                 "purchaserPoid": 2864,
                                                 "grnCreditGl": 7001,
                                                 "auditedYear": "2022-12-31",
                                                 "auditingFirm": "AuditPro LLP",
                                                 "isoCertification": "ISO9001",
                                                 "profileUpdated": "Y",
                                                 "profileVatCrMismatch": "N",
                                                 "addressPoid": 103596,
                                                 "addressName": "Test address2",
                                                 "addressTypeMap": {
                                                     "MAIN": [
                                                         {
                                                             "addressType": "MAIN",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "FINANCE": [
                                                         {
                                                             "addressType": "FINANCE",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "SALES": [
                                                         {
                                                             "addressType": "SALES",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "OPERATIONS": [
                                                         {
                                                             "addressType": "OPERATIONS",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "INVOICE": [
                                                         {
                                                             "addressType": "INVOICE",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "DELIVERY_ORDER": [
                                                         {
                                                             "addressType": "DELIVERY_ORDER",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "CARGO_ARRIVAL_NOTICE": [
                                                         {
                                                             "addressType": "CARGO_ARRIVAL_NOTICE",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "SHIP_CHANDLING": [
                                                         {
                                                             "addressType": "SHIP_CHANDLING",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "CLAIM_UAC_CONTACT_MASTER": [],
                                                     "CAN": []
                                                 },
                                                 "paymentDtl": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "bank": "First National Bank5",
                                                         "swiftCode": "FNBNUS33XXX",
                                                         "accountNumber": "1234567890",
                                                         "remarks": "Primary payment method5",
                                                         "type": "Wire",
                                                         "beneficiaryName": "ABC Trading LLC",
                                                         "address": "P.O. Box 1234, Dubai, UAE",
                                                         "bankAddress": "Bank Street, NY, USA",
                                                         "bankSwiftCode": "FNBNUS33XXX",
                                                         "iban": "AE070331234567890123456",
                                                         "intermediaryBank": "Intermediary Bank Ltd",
                                                         "beneficiaryId": "S834",
                                                         "intermediaryAcct": "INT123456789",
                                                         "intermediaryOth": "Handle with care",
                                                         "specialInstruction": "Urgent processing",
                                                         "intermediaryCountryPoid": 101000,
                                                         "beneficiaryCountry": 971,
                                                         "active": "Y",
                                                         "defaults": "Y",
                                                         "actionType": "isUpdated"
                                                     }
                                                 ],
                                                 "managementDtl": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "name": "Jane Smith5",
                                                         "designation": "Procurement Head5",
                                                         "mobile": 971555123456,
                                                         "email": "jane.smith@example.com",
                                                         "remarks": "Decision maker",
                                                         "telephone1": "+971-4-1234567",
                                                         "telephone": 97141234567,
                                                         "managementMobile": "+971555123456",
                                                         "managementTelephone": "+97141234567",
                                                         "actionType": "isUpdated"
                                                     }
                                                 ],
                                                 "questionaries": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "questionaries": "Is the supplier ISO certified?5",
                                                         "answers": "Yes",
                                                         "remarks": "ISO 9001:2015",
                                                         "actionType": "isUpdated"
                                                     },
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 2,
                                                         "questionaries": "Is the supplier VAT registered?5",
                                                         "answers": "Yes",
                                                         "remarks": "VAT123456",
                                                         "actionType": "isUpdated"
                                                     }
                                                 ],
                                                 "serviceDtl": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "servicePoid": 12,
                                                         "remarks": "Handles logistics5",
                                                         "actionType": "isUpdated"
                                                     },
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 2,
                                                         "servicePoid": 13,
                                                         "remarks": "Warehousing service5",
                                                         "actionType": "isUpdated"
                                                     }
                                                 ]
                                             }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SupplierMasterDto supplierMasterDto) {

        if (supplierMasterDto.getAddressPoid() == null) {
            Map<String, String> errors = new LinkedHashMap<>();
            AddressTypeMapDTO addressMap = supplierMasterDto.getAddressTypeMap();

            ADDRESS_GROUPS.forEach((tabName, groupClass) -> {
                List<AddressDetailsDTO> list = getListForTab(addressMap, tabName);
                validateGroup(tabName, list, groupClass, errors);
            });

            if (!errors.isEmpty()) {
                return error("Validation error occurred", 400, errors);
            }
        }

        SupplierMasterDto result = supplierMasterService.updateSupplierMaster(supplierPoid, supplierMasterDto);
        return success("Supplier Master updated successfully", result);
    }

    @Operation(
            summary = "Create Supplier Master",
            description = "Creates a new supplier master record with payment, management, service, and question details",
            parameters = {
                    @Parameter(
                            name = "documentId",
                            description = "Document identifier for tracking",
                            required = true,
                            example = "200-001",
                            in = ParameterIn.QUERY
                    ),
                    @Parameter(
                            name = "actionRequested",
                            description = "Action to be performed on the supplier data",
                            required = true,
                            example = "CREATE",
                            schema = @Schema(allowableValues = {"CREATE"})
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Supplier master created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SupplierMasterDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Supplier already exists",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createSupplier(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Supplier master data to create",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SupplierMasterDto.class),
                            examples = @ExampleObject(
                                    name = "Complete Supplier Example",
                                    value = """
                                            {
                                                 "groupPoid": 1,
                                                 "supplierName": "ABC Trading Co.42",
                                                 "supplierName2": "ABC Trading LLC42",
                                                 "supplierType": "Local",
                                                 "supplierCategoryPoid": 146,
                                                 "countryPoid": 180,
                                                 "creditLimit": 50000,
                                                 "creditPeriod": 30,
                                                 "crNo": "CR123456",
                                                 "contactPerson": "John Doe",
                                                 "active": "Y",
                                                 "seqNo": 1,
                                                 "generalRemarks": "Preferred supplier for office equipment.1",
                                                 "deleted": "N",
                                                 "tempPaymentName": "Temp Supplier",
                                                 "currencyCode": "USD",
                                                 "currencyRate": 1,
                                                 "rateExpiryDate": "2023-12-31",
                                                 "glPoid": 19760,
                                                 "defaultWeightSelectionMethod": "AUTO",
                                                 "productInfo": "Office supplies and electronics1",
                                                 "tinNumber": "TIN789456123",
                                                 "taxSlab": "5%",
                                                 "exemptionReason": null,
                                                 "taxRegisteredDate": "2020-01-01",
                                                 "purchaserPoid": 2864,
                                                 "grnCreditGl": 7001,
                                                 "auditedYear": "2022-12-31",
                                                 "auditingFirm": "AuditPro LLP",
                                                 "isoCertification": "ISO9001",
                                                 "profileUpdated": "Y",
                                                 "profileVatCrMismatch": "N",
                                                 "addressPoid": 103596,
                                                 "addressName": "Test address2",
                                                 "addressTypeMap": {
                                                     "MAIN": [
                                                         {
                                                             "addressType": "MAIN",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "FINANCE": [
                                                         {
                                                             "addressType": "FINANCE",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "SALES": [
                                                         {
                                                             "addressType": "SALES",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "OPERATIONS": [
                                                         {
                                                             "addressType": "OPERATIONS",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "INVOICE": [
                                                         {
                                                             "addressType": "INVOICE",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "DELIVERY_ORDER": [
                                                         {
                                                             "addressType": "DELIVERY_ORDER",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "CARGO_ARRIVAL_NOTICE": [
                                                         {
                                                             "addressType": "CARGO_ARRIVAL_NOTICE",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "SHIP_CHANDLING": [
                                                         {
                                                             "addressType": "SHIP_CHANDLING",
                                                             "contactPerson": "test",
                                                             "designation": null,
                                                             "offTel1": null,
                                                             "offTel2": null,
                                                             "mobile": "97312321313",
                                                             "fax": null,
                                                             "email": [
                                                                 "test@gmail.com"
                                                             ],
                                                             "website": "website",
                                                             "poBox": null,
                                                             "offNo": null,
                                                             "bldg": null,
                                                             "road": null,
                                                             "area": null,
                                                             "city": null,
                                                             "state": [],
                                                             "landMark": null,
                                                             "verified": null,
                                                             "verifiedBy": null,
                                                             "verifiedDate": null,
                                                             "whatsappNo": null,
                                                             "linkedIn": null,
                                                             "instagram": null,
                                                             "facebook": null,
                                                             "actionType": "isUpdated"
                                                         }
                                                     ],
                                                     "CLAIM_UAC_CONTACT_MASTER": [],
                                                     "CAN": []
                                                 },
                                                 "paymentDtl": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "bank": "First National Bank5",
                                                         "swiftCode": "FNBNUS33XXX",
                                                         "accountNumber": "1234567890",
                                                         "remarks": "Primary payment method5",
                                                         "type": "Wire",
                                                         "beneficiaryName": "ABC Trading LLC",
                                                         "address": "P.O. Box 1234, Dubai, UAE",
                                                         "bankAddress": "Bank Street, NY, USA",
                                                         "bankSwiftCode": "FNBNUS33XXX",
                                                         "iban": "AE070331234567890123456",
                                                         "intermediaryBank": "Intermediary Bank Ltd",
                                                         "beneficiaryId": "S834",
                                                         "intermediaryAcct": "INT123456789",
                                                         "intermediaryOth": "Handle with care",
                                                         "specialInstruction": "Urgent processing",
                                                         "intermediaryCountryPoid": 101000,
                                                         "beneficiaryCountry": 971,
                                                         "active": "Y",
                                                         "defaults": "Y",
                                                         "actionType": "isCreated"
                                                     }
                                                 ],
                                                 "managementDtl": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "name": "Jane Smith5",
                                                         "designation": "Procurement Head5",
                                                         "mobile": 971555123456,
                                                         "email": "jane.smith@example.com",
                                                         "remarks": "Decision maker",
                                                         "telephone1": "+971-4-1234567",
                                                         "telephone": 97141234567,
                                                         "managementMobile": "+971555123456",
                                                         "managementTelephone": "+97141234567",
                                                         "actionType": "isCreated"
                                                     }
                                                 ],
                                                 "questionaries": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "questionaries": "Is the supplier ISO certified?5",
                                                         "answers": "Yes",
                                                         "remarks": "ISO 9001:2015",
                                                         "actionType": "isCreated"
                                                     },
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 2,
                                                         "questionaries": "Is the supplier VAT registered?5",
                                                         "answers": "Yes",
                                                         "remarks": "VAT123456",
                                                         "actionType": "isCreated"
                                                     }
                                                 ],
                                                 "serviceDtl": [
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 1,
                                                         "servicePoid": 12,
                                                         "remarks": "Handles logistics5",
                                                         "actionType": "isCreated"
                                                     },
                                                     {
                                                         "supplierPoid": 1682391,
                                                         "detRowId": 2,
                                                         "servicePoid": 13,
                                                         "remarks": "Warehousing service5",
                                                         "actionType": "isCreated"
                                                     }
                                                 ]
                                             }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SupplierMasterDto supplierDto) {

        if (supplierDto.getAddressPoid() == null) {
            Map<String, String> errors = new LinkedHashMap<>();
            AddressTypeMapDTO addressMap = supplierDto.getAddressTypeMap();

            ADDRESS_GROUPS.forEach((tabName, groupClass) -> {
                List<AddressDetailsDTO> list = getListForTab(addressMap, tabName);
                validateGroup(tabName, list, groupClass, errors);
            });

            if (!errors.isEmpty()) {
                return error("Validation error occurred", 400, errors);
            }
        }

        SupplierMasterDto result = supplierMasterService.createSupplierMaster(supplierDto);
        return success("Supplier Master created successfully", result);
    }

    @Operation(
            summary = "Create GL Ledger for Supplier",
            description = "Creates a GL ledger for the specified supplier using PROC_GL_MASTER_CREATE procedure",
            responses = {
                    @ApiResponse(responseCode = "201", description = "GL ledger created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"SUCCESS\", \"message\": \"GL created successfully\", \"glPoid\": 12001}"))),
                    @ApiResponse(responseCode = "200", description = "Supplier already mapped to GL",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ALREADY_EXISTS\", \"message\": \"Supplier already mapped to GL\", \"glPoid\": 12001}"))),
                    @ApiResponse(responseCode = "409", description = "GL Code already exists",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ERROR\", \"message\": \"GL Code Already exist\"}"))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ERROR\", \"message\": \"PROC_GL_MASTER_CREATE failed: Check Global fix variables}")))
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{supplierPoid}/create-ledger")
    public ResponseEntity<?> createLedger(
            @Parameter(description = "Supplier ID", required = true, example = "149")
            @PathVariable Long supplierPoid,
            @Valid @RequestBody GlobalLedgerDto request) {

        Long glPoid = supplierMasterService.createLedger(supplierPoid, request);

        return success("GL created successfully", Map.of("glPoid", glPoid));
    }


    @Operation(
            summary = "Get paginated list of supplier masters with filtering",
            description = "Retrieves a paginated list of supplier masters with support for filtering and searching"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved supplier master list",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters",
                    content = @Content(mediaType = "application/json")
            )
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject
            @Parameter(
                    description = "Pagination and sorting configuration",
                    example = "page=0&size=10&sort=supplierName,asc"
            )
            Pageable pageable,
            @RequestBody(required = false)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Filter criteria for supplier master records",
                    required = false,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Search by Supplier Name",
                                    value = """
                                            {
                                                 "operator":"AND",
                                                 "isDeleted":"Y",
                                                 "filters":[
                                                {
                                                "searchField":"SUPPLIER_CODE",
                                                "searchValue":"T**88"
                                                },
                                                {
                                                "searchField":"SUPPLIER_POID",
                                                 "searchValue":"149"
                                                 }
                                                 ]
                                            }"""
                            )
                    )
            )
            FilterRequestDto filters
    ) {
        Map<String, Object> supplierMaster = supplierMasterService.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, pageable);
        return success("Supplier Master list fetched successfully", supplierMaster);
    }

    @Operation(
            summary = "Upload Excel file for bulk supplier import",
            description = "Accepts a multipart Excel file and stores rows into TEMP_SUPPLIER_MSTR_IMPORT_TML. Requires supplierPoid to update an existing supplier.",
            parameters = {
                    @Parameter(
                            name = "supplierPoid",
                            description = "Supplier POID of the existing supplier to update (required)",
                            required = true,
                            example = "149",
                            in = ParameterIn.QUERY
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/import")
    public ResponseEntity<?> importSuppliers(@RequestParam("file") MultipartFile file,
                                             @Parameter(description = "Supplier POID of the existing supplier to update", required = true)
                                             @RequestParam(required = false) Long supplierPoid) {
        Long transactionPoid = supplierMasterService.importSuppliersFromExcel(file, supplierPoid);
        return success("Excel file uploaded successfully", Map.of("transactionPoid", transactionPoid));
    }

    @Operation(
            summary = "Process imported supplier data",
            description = "Triggers stored procedure to validate and update/create suppliers from imported data"
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/import/process")
    public ResponseEntity<?> processImportedSuppliers(@Valid @RequestBody SupplierImportRequestDto request) {
        SupplierImportResponseDto result = supplierMasterService.processImportedSuppliers(request);
        return success("Supplier import processed successfully", result);
    }

    @SuppressWarnings("unchecked")
    private List<AddressDetailsDTO> getListForTab(AddressTypeMapDTO dto, String tabName) {
        try {
            Field field = AddressTypeMapDTO.class.getDeclaredField(tabName);
            field.setAccessible(true);
            return (List<AddressDetailsDTO>) field.get(dto);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private <T> void validateGroup(String tabName, List<AddressDetailsDTO> list, Class<?> group, Map<String, String> errors) {
        if (list != null) {
            for (AddressDetailsDTO dto : list) {
                Set<ConstraintViolation<AddressDetailsDTO>> violations = validator.validate(dto, group);
                for (ConstraintViolation<AddressDetailsDTO> v : violations) {
                    String fieldKey = tabName + "." + v.getPropertyPath().toString();
                    errors.put(fieldKey, v.getMessage());
                }
            }
        }
    }

}
