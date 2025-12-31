package com.asg.finance.repository;

import com.asg.finance.entity.APRequestForQtnHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface APRequestForQtnHdrRepository extends JpaRepository<APRequestForQtnHdr, Long> {
    boolean existsByTransactionPoid(Long transactionPoid);
}
