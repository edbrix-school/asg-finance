package com.asg.finance.repository;

import com.asg.finance.entity.ArDebitNoteHdr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ArDebitNoteHdrRepository extends JpaRepository<ArDebitNoteHdr, Long> {

    @Query("""
            SELECT d FROM ArDebitNoteHdr d 
            WHERE (:partyType IS NULL OR d.partyType = :partyType)
            AND (:refType IS NULL OR d.refType = :refType)
            AND (:voucherType IS NULL OR d.voucherType = :voucherType)
            AND (:fromDate IS NULL OR d.createdDate >= :fromDate)
            AND (:toDate IS NULL OR d.createdDate <= :toDate)
            AND d.deleted = 'N'
            ORDER BY d.createdDate DESC
            """)
    Page<ArDebitNoteHdr> findWithFilters(
            @Param("partyType") String partyType,
            @Param("refType") String refType,
            @Param("voucherType") String voucherType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );
    
    @Query(value = "SELECT AR_DEBIT_NOTE_HDR_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Long getNextSequenceValue();
}