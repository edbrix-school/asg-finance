package com.asg.finance.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.finance.entity.GlExpenseReallocationDtl;

@Repository
public interface GlExpenseReallocationDtlRepository
		extends JpaRepository<GlExpenseReallocationDtl, GlExpenseReallocationDtl.CompositeKey> {

	List<GlExpenseReallocationDtl> findByTransactionPoid(Long transactionPoid);
	
	Optional<GlExpenseReallocationDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

	@Query(value = "SELECT COALESCE(SUM(d.SH), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalShByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.FF), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalFfByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.FFS), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalFfsByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.FFP), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalFfpByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.PROPERTIES), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalPropertiesByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.MTA), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalMtaByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.PDA), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalPdaByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.ADMIN), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getTotalAdminByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	@Query(value = "SELECT COALESCE(SUM(d.TOTAL), 0) FROM GL_EXPENSE_REALLOCATION_DTL d WHERE D.TRANSACTION_POID = :transactionPoid", nativeQuery = true)
	BigDecimal getGrandTotalByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

	void deleteByTransactionPoid(Long transactionPoid);

	@Query("SELECT COALESCE(MAX(u.id.detRowId), 0) + 1 FROM GlExpenseReallocationDtl u WHERE u.id.transactionPoid = :transactionPoid")
	Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);
}
