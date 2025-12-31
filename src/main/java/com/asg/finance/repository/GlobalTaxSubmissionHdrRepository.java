package com.asg.finance.repository;

import com.asg.finance.entity.GlobalTaxSubmissionHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalTaxSubmissionHdrRepository extends JpaRepository<GlobalTaxSubmissionHdr, Long>,
        JpaSpecificationExecutor<GlobalTaxSubmissionHdr> {

    Optional<GlobalTaxSubmissionHdr> findByTransactionPoid(Long transactionPoid);

    Optional<GlobalTaxSubmissionHdr> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    List<GlobalTaxSubmissionHdr> findByGroupPoid(Long groupPoid);

    @Query("SELECT h FROM GlobalTaxSubmissionHdr h WHERE h.groupPoid = :groupPoid " +
            "AND (h.deleted IS NULL OR h.deleted = 'N') " +
            "AND (:dateFrom IS NULL OR h.transactionDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR h.transactionDate <= :dateTo) " +
            "AND (:docRef IS NULL OR h.docRef LIKE CONCAT('%', :docRef, '%')) " +
            "AND (:periodFrom IS NULL OR h.periodFrom >= :periodFrom) " +
            "AND (:periodTo IS NULL OR h.periodTo <= :periodTo) " +
            "AND (:companyId IS NULL OR h.companyPoid = :companyId) " +
            "ORDER BY h.transactionDate DESC, h.transactionPoid DESC")
    List<GlobalTaxSubmissionHdr> findWithFilters(
            @Param("groupPoid") Long groupPoid,
            @Param("dateFrom") Timestamp dateFrom,
            @Param("dateTo") Timestamp dateTo,
            @Param("docRef") String docRef,
            @Param("periodFrom") Timestamp periodFrom,
            @Param("periodTo") Timestamp periodTo,
            @Param("companyId") Long companyId);

    @Query("SELECT h FROM GlobalTaxSubmissionHdr h WHERE h.companyPoid = :companyId " +
            "AND h.groupPoid = :groupPoid " +
            "AND (h.deleted IS NULL OR h.deleted = 'N') " +
            "AND ((h.periodFrom <= :periodTo AND h.periodTo >= :periodFrom))")
    List<GlobalTaxSubmissionHdr> findOverlappingPeriods(
            @Param("companyId") Long companyId,
            @Param("groupPoid") Long groupPoid,
            @Param("periodFrom") Timestamp periodFrom,
            @Param("periodTo") Timestamp periodTo);

    @Query("SELECT h FROM GlobalTaxSubmissionHdr h WHERE h.companyPoid = :companyId " +
            "AND h.groupPoid = :groupPoid " +
            "AND h.transactionPoid != :excludeTransactionPoid " +
            "AND (h.deleted IS NULL OR h.deleted = 'N') " +
            "AND ((h.periodFrom <= :periodTo AND h.periodTo >= :periodFrom))")
    List<GlobalTaxSubmissionHdr> findOverlappingPeriodsExcluding(
            @Param("companyId") Long companyId,
            @Param("groupPoid") Long groupPoid,
            @Param("periodFrom") Timestamp periodFrom,
            @Param("periodTo") Timestamp periodTo,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid);
}

