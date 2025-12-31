package com.asg.finance.repository;

import com.asg.finance.entity.ArCreditNoteHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArCreditNoteHdrRepository extends JpaRepository<ArCreditNoteHdr, Long> {
    
    @Query("SELECT h FROM ArCreditNoteHdr h WHERE h.transactionPoid = :transactionPoid AND (h.deleted IS NULL OR h.deleted = :deleted)")
    Optional<ArCreditNoteHdr> findByTransactionPoidAndDeleted(@Param("transactionPoid") Long transactionPoid, @Param("deleted") String deleted);
    
    @Query("SELECT h FROM ArCreditNoteHdr h WHERE h.docRef = :docRef AND h.deleted = 'N'")
    Optional<ArCreditNoteHdr> findByDocRef(@Param("docRef") String docRef);
    
    List<ArCreditNoteHdr> findByDeletedOrderByTransactionPoidDesc(String deleted);
}