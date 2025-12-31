package com.asg.finance.repository;

import com.asg.finance.entity.ChequeReturnGlDetail;
import com.asg.finance.entity.ChequeReturnGlDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChequeReturnGlDetailRepository extends JpaRepository<ChequeReturnGlDetail, ChequeReturnGlDetailId> {
    List<ChequeReturnGlDetail> findByChequeReturn_TransactionPoid(Long transactionPoid);
    long countById_TransactionPoid(Long transactionPoid);
    void deleteById_TransactionPoid(Long transactionPoid);
}
