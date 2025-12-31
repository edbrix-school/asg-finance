package com.asg.finance.repository;

import com.asg.finance.entity.ChequeReturnDetail;
import com.asg.finance.entity.ChequeReturnDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChequeReturnDetailRepository extends JpaRepository<ChequeReturnDetail, ChequeReturnDetailId> {
    List<ChequeReturnDetail> findByChequeReturn_TransactionPoid(Long transactionPoid);
    long countById_TransactionPoid(Long transactionPoid);
    void deleteById_TransactionPoid(Long transactionPoid);
}
