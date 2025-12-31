package com.asg.finance.repository;

import com.asg.finance.entity.GLPaymentVoucherDtlGLEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankPaymentVoucherDetailsRepository extends JpaRepository<GLPaymentVoucherDtlGLEntity, Long> {

    List<GLPaymentVoucherDtlGLEntity> findByTransactionPoid(Long transactionPoid);

    @Modifying
    @Query("DELETE FROM GLPaymentVoucherDtlGLEntity e WHERE e.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}
