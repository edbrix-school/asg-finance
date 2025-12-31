package com.asg.finance.repository;

import com.asg.finance.entity.GlContraVoucherHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlContraVoucherHdrRepository extends JpaRepository<GlContraVoucherHdr, Long>,
        JpaSpecificationExecutor<GlContraVoucherHdr> {

    Optional<GlContraVoucherHdr> findByTransactionPoid(Long transactionPoid);

    Optional<GlContraVoucherHdr> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    List<GlContraVoucherHdr> findByGroupPoid(Long groupPoid);

    @Query("SELECT h FROM GlContraVoucherHdr h WHERE h.groupPoid = :groupPoid " +
            "AND (:dateFrom IS NULL OR h.transactionDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR h.transactionDate <= :dateTo) " +
            "AND (:docRef IS NULL OR UPPER(h.docRef) LIKE UPPER(CONCAT('%', :docRef, '%'))) " +
            "AND (:narration IS NULL OR UPPER(h.postingNarration) LIKE UPPER(CONCAT('%', :narration, '%'))) " +
            "AND (:companyId IS NULL OR h.companyPoid = :companyId) " +
            "AND (:currencyCode IS NULL OR h.currencyCode = :currencyCode) " +
            "AND (:creditGl IS NULL OR h.creditGl = :creditGl) " +
            "AND (:debitGl IS NULL OR h.debitGl = :debitGl) " +
            "ORDER BY h.transactionDate DESC, h.transactionPoid DESC")
    List<GlContraVoucherHdr> findWithFilters(
            @Param("groupPoid") Long groupPoid,
            @Param("dateFrom") Timestamp dateFrom,
            @Param("dateTo") Timestamp dateTo,
            @Param("docRef") String docRef,
            @Param("narration") String narration,
            @Param("companyId") Long companyId,
            @Param("currencyCode") String currencyCode,
            @Param("creditGl") Long creditGl,
            @Param("debitGl") Long debitGl);

    @Query(value = """
            SELECT COALESCE(MAX(TO_NUMBER(REGEXP_SUBSTR(DOC_REF, '[0-9]+'))), 0)
            FROM GL_CONTRA_VOUCHER_HDR
            WHERE GROUP_POID = :groupPoid
            AND DOC_REF LIKE 'ASG%'
            """, nativeQuery = true)
    Long findMaxDocRefNumber(@Param("groupPoid") Long groupPoid);
}

