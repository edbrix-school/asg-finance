package com.asg.finance.repository;


import com.asg.finance.entity.GlPettyCashPaymentHdr;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface GlPettyCashPaymentHdrRepository extends JpaRepository<GlPettyCashPaymentHdr, Long> {
    Optional<GlPettyCashPaymentHdr> findByTransactionPoid(Long transactionPoid);

}
