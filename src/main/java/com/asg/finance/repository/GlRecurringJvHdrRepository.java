package com.asg.finance.repository;

import com.asg.finance.entity.GlRecurringJvHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlRecurringJvHdrRepository extends JpaRepository<GlRecurringJvHdr, Long>,
        JpaSpecificationExecutor<GlRecurringJvHdr> {

    Optional<GlRecurringJvHdr> findByTransactionPoid(Long transactionPoid);

    @Query("SELECT h FROM GlRecurringJvHdr h WHERE h.groupPoid = :groupPoid " +
            "AND (:dateFrom IS NULL OR h.transactionDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR h.transactionDate <= :dateTo) " +
            "AND (:docRef IS NULL OR h.docRef LIKE %:docRef%) " +
            "AND (:narration IS NULL OR h.narration LIKE %:narration%) " +
            "AND (:refType IS NULL OR h.refType = :refType) " +
            "AND (:employeeId IS NULL OR h.employeePoid = :employeeId) " +
            "AND (:assetId IS NULL OR h.faPoid = :assetId) " +
            "ORDER BY h.transactionDate DESC, h.transactionPoid DESC")
    List<GlRecurringJvHdr> findWithFilters(
            @Param("groupPoid") Long groupPoid,
            @Param("dateFrom") Timestamp dateFrom,
            @Param("dateTo") Timestamp dateTo,
            @Param("docRef") String docRef,
            @Param("narration") String narration,
            @Param("refType") String refType,
            @Param("employeeId") Long employeeId,
            @Param("assetId") Long assetId);

}

