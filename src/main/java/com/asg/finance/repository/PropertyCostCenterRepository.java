package com.asg.finance.repository;

import com.asg.finance.entity.PropertyCostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyCostCenterRepository extends JpaRepository<PropertyCostCenter, Long> {

    Optional<PropertyCostCenter> findByPropertyCostCenterPoid(Long poid);

    Optional<PropertyCostCenter> findByPropertyCostCenterPoidAndDeleted(Long poid, String deleted);

    // Corrected method: Find by 'propertyCostCenterCode' field
    Optional<PropertyCostCenter> findByPropertyCostCenterCode(String costCenterCode);
    
    Optional<PropertyCostCenter> findByPropertyCostCenterNameAndDeleted(String propertyCostCenterName, String deleted);

    boolean existsByParentPropertyPoidAndDeleted(Long parentPropertyPoid, String deleted);
    
    /**
     * Find main groups (records with no parent)
     * These are the top-level Property Cost Centers
     */
    @Query("SELECT p FROM PropertyCostCenter p WHERE p.parentPropertyPoid IS NULL " +
           "AND (:includeDeleted = true OR p.deleted = 'N') " +
           "AND (:groupPoid IS NULL OR p.companyPoid = :groupPoid) " +
           "ORDER BY COALESCE(p.seqNo, 999999), p.propertyCostCenterCode")
    List<PropertyCostCenter> findMainGroups(
            @Param("includeDeleted") Boolean includeDeleted,
            @Param("groupPoid") Long groupPoid);
    
    /**
     * Find direct children of a specific parent
     * Returns only immediate children, not the complete hierarchy
     */
    @Query("SELECT p FROM PropertyCostCenter p WHERE p.parentPropertyPoid = :parentPoid " +
           "AND (:includeDeleted = true OR p.deleted = 'N') " +
           "AND (:groupPoid IS NULL OR p.companyPoid = :groupPoid) " +
           "ORDER BY COALESCE(p.seqNo, 999999), p.propertyCostCenterCode")
    List<PropertyCostCenter> findDirectChildren(
            @Param("parentPoid") Long parentPoid,
            @Param("includeDeleted") Boolean includeDeleted,
            @Param("groupPoid") Long groupPoid);

    /**
     * Count active main groups (no parent)
     */
    @Query("SELECT COUNT(p) FROM PropertyCostCenter p WHERE p.parentPropertyPoid IS NULL AND p.deleted = 'N'")
    long countMainGroups();

    /**
     * Count active direct children of a specific parent
     */
    @Query("SELECT COUNT(p) FROM PropertyCostCenter p WHERE p.parentPropertyPoid = :parentPoid AND p.deleted = 'N'")
    long countActiveDirectChildren(@Param("parentPoid") Long parentPoid);

    /**
     * Count direct children of a specific parent
     */
    @Query("SELECT COUNT(p) FROM PropertyCostCenter p WHERE p.parentPropertyPoid = :parentPoid AND p.deleted = 'N'")
    long countDirectChildren(@Param("parentPoid") Long parentPoid);
}
