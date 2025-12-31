package com.asg.finance.repository;

import com.asg.finance.entity.TaxPeriodHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface TaxPeriodHdrRepository extends JpaRepository<TaxPeriodHdr, Long> {
    @Query("SELECT COUNT(t) > 0 FROM TaxPeriodHdr t WHERE t.deleted = 'N' AND " +
           "((t.periodFrom <= :periodTo AND t.periodTo >= :periodFrom))")
    boolean existsOverlappingPeriod(@Param("periodFrom") LocalDate periodFrom, @Param("periodTo") LocalDate periodTo);
    
    @Query("SELECT COUNT(t) > 0 FROM TaxPeriodHdr t WHERE t.deleted = 'N' AND t.transactionPoid != :transactionPoid AND " +
           "((t.periodFrom <= :periodTo AND t.periodTo >= :periodFrom))")
    boolean existsOverlappingPeriodExcluding(@Param("periodFrom") LocalDate periodFrom, @Param("periodTo") LocalDate periodTo, @Param("transactionPoid") Long transactionPoid);
}
