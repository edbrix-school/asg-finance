package com.asg.finance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.asg.finance.entity.GlExpenseReallocationXlDtl;

@Repository
public interface GlExpenseReallocationXlDtlRepository
		extends JpaRepository<GlExpenseReallocationXlDtl, GlExpenseReallocationXlDtl.CompositeKey> {

	Optional<List<GlExpenseReallocationXlDtl>> findByTransactionPoid(Long transactionPoid);
	
	Optional<GlExpenseReallocationXlDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

	void deleteByTransactionPoid(Long transactionPoid);

	@Query("SELECT COALESCE(MAX(u.detRowId), 0) FROM GlExpenseReallocationXlDtl u WHERE u.transactionPoid = :transactionPoid")
	Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);
}
