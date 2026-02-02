package com.asg.finance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.asg.finance.entity.GlExpenseReallocationXlDtl;

@Repository
public interface GlExpenseReallocationXlDtlRepository
		extends JpaRepository<GlExpenseReallocationXlDtl, GlExpenseReallocationXlDtl.CompositeKey> {

	List<GlExpenseReallocationXlDtl> findByTransactionPoid(Long transactionPoid);

	void deleteByTransactionPoid(Long transactionPoid);

	@Query("SELECT COALESCE(MAX(u.id.detRowId), 0) + 1 FROM GlExpenseReallocationXlDtl u WHERE u.id.transactionPoid = :transactionPoid")
	Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);
}
