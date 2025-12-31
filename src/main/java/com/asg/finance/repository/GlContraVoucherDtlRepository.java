package com.asg.finance.repository;

import com.asg.finance.entity.GlContraVoucherDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlContraVoucherDtlRepository extends JpaRepository<GlContraVoucherDtl, GlContraVoucherDtl.CompositeKey>,
        JpaSpecificationExecutor<GlContraVoucherDtl> {

    List<GlContraVoucherDtl> findByTransactionPoid(Long transactionPoid);

    List<GlContraVoucherDtl> findByTransactionPoidIn(List<Long> transactionPoids);

    Optional<GlContraVoucherDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    @Query("SELECT COALESCE(SUM(d.drAmt), 0) FROM GlContraVoucherDtl d WHERE d.transactionPoid = :transactionPoid")
    BigDecimal getDrTotalByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COALESCE(SUM(d.crAmt), 0) FROM GlContraVoucherDtl d WHERE d.transactionPoid = :transactionPoid")
    BigDecimal getCrTotalByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);
}

