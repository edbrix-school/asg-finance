package com.asg.finance.repository;

import com.asg.finance.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByTransactionPoid(Long transactionPoid);
    
    Optional<PurchaseOrderItem> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
    
    @Modifying
    @Query("DELETE FROM PurchaseOrderItem p WHERE p.transactionPoid = :transactionPoid AND p.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIdIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

}
