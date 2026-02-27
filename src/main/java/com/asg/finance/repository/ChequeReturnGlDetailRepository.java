package com.asg.finance.repository;

import com.asg.finance.entity.ChequeReturnGlDetail;
import com.asg.finance.entity.ChequeReturnGlDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChequeReturnGlDetailRepository extends JpaRepository<ChequeReturnGlDetail, ChequeReturnGlDetailId> {
    List<ChequeReturnGlDetail> findByTransactionPoid(Long transactionPoid);
    long countByTransactionPoid(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
    Optional<ChequeReturnGlDetail> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

}
