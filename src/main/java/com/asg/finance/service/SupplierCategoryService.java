package com.asg.finance.service;

import com.asg.finance.dto.SupplierCategoryDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SupplierCategoryService {
     void softDeleteSupplierCategory(Long supplierCategoryPoid, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto);

     SupplierCategoryDto getSupplierCategoryById(Long supplierCategoryPoid);

     SupplierCategoryDto updateSupplierCategory(Long supplierCategoryPoid, SupplierCategoryDto supplierCategoryDto);

    SupplierCategoryDto createSupplierCategory(SupplierCategoryDto supplierCategoryDto);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable);
}
