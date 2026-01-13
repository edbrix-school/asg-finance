package com.asg.finance.repository;

import com.asg.finance.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long>, JpaSpecificationExecutor<CostCenter> {
    boolean existsByCostCenterCode(String costCenterCode);
    boolean existsByCostCenterDescription(String costCenterDescription);
    CostCenter findByCostCenterPoid(Long costCenterPoid);
    CostCenter findByParentCostCenterPoid(Long parentCostCenterPoid);
    boolean existsByCostCenterPoid(Long costCenterPoid);
    boolean existsByCostCenterCodeAndCostCenterPoidNot(String code, Long poid);

    boolean existsByCostCenterDescriptionAndCostCenterPoidNot(String description, Long poid);

    boolean existsByParentCostCenterPoidAndDeleted(Long parentCostCenterPoid, String deleted);

    /**
     * Find main groups (records with no parent)
     * These are the top-level cost centers
     */
    @Query("SELECT c FROM CostCenter c WHERE c.parentCostCenterPoid IS NULL " +
           "AND (:includeDeleted = true OR c.deleted != 'Y' OR c.deleted IS NULL) " +
           "AND (:groupPoid IS NULL OR c.groupPoid = :groupPoid) " +
           "ORDER BY COALESCE(c.seqNo, 999999), c.costCenterCode")
    List<CostCenter> findMainGroups(
            @Param("includeDeleted") Boolean includeDeleted,
            @Param("groupPoid") Long groupPoid);

    /**
     * Find direct children of a specific parent
     * Returns only immediate children, not the complete hierarchy
     */
    @Query("SELECT c FROM CostCenter c WHERE c.parentCostCenterPoid = :parentPoid " +
           "AND (:includeDeleted = true OR c.deleted != 'Y' OR c.deleted IS NULL) " +
           "AND (:groupPoid IS NULL OR c.groupPoid = :groupPoid) " +
           "ORDER BY COALESCE(c.seqNo, 999999), c.costCenterCode")
    List<CostCenter> findDirectChildren(
            @Param("parentPoid") Long parentPoid,
            @Param("includeDeleted") Boolean includeDeleted,
            @Param("groupPoid") Long groupPoid);

}
