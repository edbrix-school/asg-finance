package com.asg.finance.repository;

import com.asg.finance.entity.GLMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GLMastersRepository extends JpaRepository<GLMasterEntity, Long> {
    boolean existsByGlCode(String glCode);
    // maybe method to check uniqueness excluding current record
    boolean existsByGlCodeAndGlPoidNot(String glCode, Long glPoid);
    
    /**
     * Find GL Master by GL Code (active records only)
     * Returns list ordered by GL_POID DESC to handle duplicates
     */
    @Query("SELECT g FROM GLMasterEntity g WHERE g.glCode = :glCode AND (g.deletedFlag IS NULL OR g.deletedFlag = :deletedFlag) ORDER BY g.glPoid DESC")
    List<GLMasterEntity> findAllByGlCodeAndDeletedFlag(@Param("glCode") String glCode, @Param("deletedFlag") String deletedFlag);

    boolean existsBySubOfAndDeletedFlag(Long subOf, String deletedFlag);

    /**
     * Find main groups (records with no parent)
     * These are the top-level categories like ASSETS, LIABILITIES, etc.
     */
    @Query("SELECT g FROM GLMasterEntity g WHERE g.subOf IS NULL " +
           "AND (:includeDeleted = true OR g.deletedFlag = 'N') " +
           "AND (:groupPoid IS NULL OR g.groupPoid = :groupPoid) " +
           "ORDER BY g.glCode")
    List<GLMasterEntity> findMainGroups(
            @Param("includeDeleted") Boolean includeDeleted,
            @Param("groupPoid") Long groupPoid);

    /**
     * Count children for multiple parent IDs
     */
    @Query("SELECT c.subOf, COUNT(c) FROM GLMasterEntity c " +
           "WHERE c.subOf IN :parentIds " +
           "AND c.deletedFlag = 'N' " +
           "GROUP BY c.subOf")
    List<Object[]> countChildrenByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * Find direct children of a specific parent
     * Returns only immediate children, not the complete hierarchy
     */
    @Query("SELECT g FROM GLMasterEntity g WHERE g.subOf = :parentPoid " +
           "AND (:includeDeleted = true OR g.deletedFlag = 'N') " +
           "AND (:groupPoid IS NULL OR g.groupPoid = :groupPoid) " +
           "ORDER BY g.glCode")
    List<GLMasterEntity> findDirectChildren(
            @Param("parentPoid") Long parentPoid,
            @Param("includeDeleted") Boolean includeDeleted,
            @Param("groupPoid") Long groupPoid);

    List<GLMasterEntity> findByGlPoidIn(List<Long> glPoids);

    @Query("SELECT COUNT(g) FROM GLMasterEntity g " +
           "WHERE g.type = 'LEDGER' " +
           "AND (g.deletedFlag IS NULL OR g.deletedFlag = 'N') " +
           "AND (:groupPoid IS NULL OR g.groupPoid = :groupPoid)")
    Long countActiveLedgers(@Param("groupPoid") Long groupPoid);

}