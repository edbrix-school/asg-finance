package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;


import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.SupplierCategoryDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.SupplierCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/supplier-category")
@RequiredArgsConstructor
public class SupplierCategoryController {

    private final SupplierCategoryService supplierCategoriesService;

    @Operation(
            summary = "Soft delete a supplier category",
            description = "Marks a supplier category as deleted by setting its active status to 'N' and deleted status to 'Y'",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Supplier category successfully marked as deleted",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SupplierCategoryDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier category not found with the given ID"
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{categoryPoid}")
    public ResponseEntity<?> softDeleteSupplierCategory(
            @Parameter(description = "ID of the supplier category to be soft deleted", example = "121")
            @PathVariable("categoryPoid") Long supplierCategoryPoid) {

        SupplierCategoryDto supplierCategoryDto = supplierCategoriesService.softDeleteSupplierCategory(supplierCategoryPoid);
        return success("Supplier Category deleted successfully", supplierCategoryDto);
    }


    @Operation(
            summary = "Get supplier category by ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved supplier category details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SupplierCategoryDto.class,
                                            example = "{\n" +
                                                    "  \"supplierCategoryPoid\": 121,\n" +
                                                    "  \"supplierCategoryName\": \"Electronics\",\n" +
                                                    "  \"supplierCategoryCode\": \"ELEC\",\n" +
                                                    "  \"active\": \"Y\",\n" +
                                                    "  \"deleted\": \"N\"\n" +
                                                    "}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier category not found with the given ID"
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{supplierCategoryPoid}")
    public ResponseEntity<?> getSupplierCategoryById(
            @Parameter(description = "ID of the supplier category to retrieve", example = "121")
            @PathVariable("supplierCategoryPoid") Long supplierCategoryPoid) {

        SupplierCategoryDto supplierCategoryDto = supplierCategoriesService.getSupplierCategoryById(supplierCategoryPoid);
        return success("Supplier Category retrieved successfully", supplierCategoryDto);
    }

    @Operation(
            summary = "Update supplier category",
            description = "Updates supplier category name, name2, sequence number, and active flag. Supplier category code cannot be changed.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Supplier category successfully updated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SupplierCategoryDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier category not found with the given ID"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    schema = @Schema(implementation = SupplierCategoryDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Update Supplier Category Example",
                                    value = """
                                            {
                                              "supplierCategoryName": "Consultancy Services",
                                              "supplierCategoryName2": "Advisory",
                                              "seqNo": 15,
                                              "active": "Y"
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{supCatPoid}")
    public ResponseEntity<?> updateSupplierCategory(
            @Parameter(description = "ID of the supplier category to update", example = "144")
            @PathVariable("supCatPoid") Long supCatPoid,

            @Valid @RequestBody SupplierCategoryDto supplierCategoryDto) {

        SupplierCategoryDto updatedSupplierCategory = supplierCategoriesService.updateSupplierCategory(supCatPoid, supplierCategoryDto);
        return success("Supplier Category updated successfully", updatedSupplierCategory);
    }

    @Operation(
            summary = "Create a new supplier category",
            description = "Creates a new supplier category. The supplier category code is auto-generated by a database trigger and will be returned in the response.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Supplier category successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SupplierCategoryDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input data or validation error"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Supplier category with the same name already exists"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    schema = @Schema(implementation = SupplierCategoryDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Create Supplier Category Example",
                                    value = """
                                            {
                                              "groupPoid": 1,
                                              "supplierCategoryName": "Construction Materials",
                                              "supplierCategoryName2": "Building Supplies",
                                              "seqNo": "10",
                                              "active": "Y",
                                              "generalRemarks": "Suppliers for construction and building materials",
                                              "deleted": "N"
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createSupplierCategory(
            @Valid @RequestBody SupplierCategoryDto supplierCategoryDto) {

        SupplierCategoryDto createdSupplierCategory = supplierCategoriesService.createSupplierCategory(supplierCategoryDto);

        return success("Supplier Category created successfully", createdSupplierCategory);
    }

    @Operation(
            summary = "List Supplier Categories with Search and Sort",
            description = "Search and sort supplier categories with flexible filtering. " +
                    "Valid `searchField` values: GLOBALSEARCH or (SUPPLIER_CATEGORY_NAME, SUPPLIER_CATEGORY_CODE). " +
                    "Sorting defaults to SUPPLIER_CATEGORY_CODE in ascending order."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (SUPPLIER_CATEGORY_NAME, SUPPLIER_CATEGORY_CODE).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR".
                      4. isDeleted when 'N' or null, will search and return non-deleted records, 'Y' will check and return deleted records.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      - { "searchField": "GLOBALSEARCH", "searchValue": "electronics" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      - { "searchField": "SUPPLIER_CATEGORY_NAME", "searchValue": "Electronics" }
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      - { "searchField": "SUPPLIER_CATEGORY_NAME", "searchValue": "Electronics|Furniture" }
                    
                    - #### Multiple Different Fields:
                      Combine multiple search conditions.
                      - { "searchField": "SUPPLIER_CATEGORY_NAME", "searchValue": "Electronics" }
                      - "operator" : "AND"
                    
                    - ### Sorting:
                      Defaults to SUPPLIER_CATEGORY_CODE ascending.
                      To override, use `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      - sort=SUPPLIER_CATEGORY_NAME,ASC
                      - sort=SUPPLIER_CATEGORY_CODE,DESC
                    """,
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FilterDto.class)),
                    examples = {
                            @ExampleObject(
                                    name = "Supplier Category Filters",
                                    value = """
                                            {
                                                                "operator": "AND",
                                                                  "isDeleted": "N",
                                                                  "filters": [
                                                                    {
                                                                      "searchField": "SUPPLIER_CATEGORY_CODE",
                                                                      "searchValue": "SC002"
                                                                    },
                                                                    {
                                                                      "searchField": "SUPPLIER_CATEGORY_NAME",
                                                                      "searchValue": "MARINE SEVICE VENDERS"
                                                                    }
                                                                  ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters
    ) {

        Map<String, Object> supplierCategories = supplierCategoriesService.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, pageable);

        return success("Supplier Category list fetched successfully", supplierCategories);

    }


}
