package com.asg.finance.repository;

import com.asg.finance.entity.GlJournalVoucherHdr;
import com.asg.finance.projection.CurrencyRateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GlJournalVoucherHdrRepository extends JpaRepository<GlJournalVoucherHdr, Long>, JpaSpecificationExecutor<GlJournalVoucherHdr>, JournalVoucherProcRepository {
    Optional<GlJournalVoucherHdr> findByTransactionPoid(Long transactionPoid);
    
    @Query(value = "SELECT c.CURRENCY_CODE as currencyCode, c.RATE_DATE as rateDate, c.RATE as rate " +
            "FROM GLOBAL_CURRENCY_RATES c " +
            "WHERE c.CURRENCY_CODE = :currencyCode " +
            "AND c.RATE_DATE <= :transactionDate " +
            "ORDER BY c.RATE_DATE DESC", nativeQuery = true)
    List<CurrencyRateProjection> findLatestCurrencyRate(
            @Param("currencyCode") String currencyCode,
            @Param("transactionDate") LocalDate transactionDate
    );
}
