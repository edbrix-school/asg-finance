package com.asg.finance.repository;

import com.asg.finance.entity.SupplierMasterEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierMasterRepository extends JpaRepository<SupplierMasterEntity, Long> {
    SupplierMasterEntity findBySupplierPoid(Long supplierPoid);

    boolean existsBySupplierCodeAndGroupPoidAndSupplierPoidNot(String code, Long groupPoid, Long supplierPoid);

    boolean existsBySupplierCodeAndGroupPoid(String code, Long groupPoid);

    boolean existsBySupplierNameIgnoreCaseAndSupplierPoidNot(String supplierName, Long supplierPoid);

    boolean existsBySupplierNameIgnoreCaseAndGroupPoidAndSupplierPoidNot(String normalizedName, Long groupPoid, Long supplierPoid);

    boolean existsBySupplierNameIgnoreCaseAndGroupPoid(String normalizedName, Long groupPoid);

    boolean existsBySupplierCodeAndGroupPoidAndCountryPoid(String supplierCode, Long groupPoid, Long countryPoid);

    boolean existsBySupplierCodeAndGroupPoidAndCountryPoidAndSupplierNameIgnoreCaseAndSupplierPoidNot(String supplierCode, Long groupPoid, Long countryPoid, String supplierName, Long supplierPoid);

}
