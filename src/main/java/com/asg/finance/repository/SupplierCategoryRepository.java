package com.asg.finance.repository;

import com.asg.finance.entity.SupplierCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategoryEntity, Long> {

    SupplierCategoryEntity findBySupplierCategoryPoid(Long supplierCategoryPoid);

    List<SupplierCategoryEntity> findAllByActiveAndDeleted(String active, String deleted);

    @Query("SELECT s FROM SupplierCategoryEntity s WHERE s.supplierCategoryPoid = :supplierCategoryPoid AND s.deleted = 'N'")
    Optional<SupplierCategoryEntity> findActiveById(@Param("supplierCategoryPoid") Long supplierCategoryPoid);

    @Modifying
    @Transactional
    @Query("UPDATE SupplierCategoryEntity s " +
            "SET s.active = 'N', s.deleted = 'Y', s.lastModifiedBy = :modifiedBy, s.lastModifiedDate = CURRENT_TIMESTAMP " +
            "WHERE s.supplierCategoryPoid = :supplierCategoryPoid")
    int softDelete(@Param("supplierCategoryPoid") Long supplierCategoryPoid, @Param("modifiedBy") String modifiedBy);

    boolean existsBySupplierCategoryNameAndSupplierCategoryPoidNot(String supplierCategoryName, Long supplierCategoryPoid);

    boolean existsBySupplierCategoryPoid(Long supplierCategoryPoid);

    boolean existsBySupplierCategoryCode(String supplierCategoryCode);

    boolean existsBySupplierCategoryName(String supplierCategoryName);
}
