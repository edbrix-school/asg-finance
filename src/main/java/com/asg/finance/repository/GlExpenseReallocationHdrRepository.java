package com.asg.finance.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.finance.entity.GlExpenseReallocationHdr;

@Repository
public interface GlExpenseReallocationHdrRepository extends JpaRepository<GlExpenseReallocationHdr, Long> {

	Optional<GlExpenseReallocationHdr> findByTransactionPoid(Long transactionPoid);

	Optional<GlExpenseReallocationHdr> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

	List<GlExpenseReallocationHdr> findByGroupPoid(Long groupPoid);

	@Query(value = """
			    SELECT *
			    FROM GL_EXPENSE_REALLOCATION_HDR h
			    WHERE h.GROUP_POID = :groupPoid
			      AND (h.DELETED IS NULL OR h.DELETED = 'N')
			      AND (:dateFrom IS NULL OR h.TRANSACTION_DATE >= :dateFrom)
			      AND (:dateTo IS NULL OR h.TRANSACTION_DATE <= :dateTo)
			      AND (:docRef IS NULL OR h.DOC_REF LIKE '%' || :docRef || '%')
			      AND (:narration IS NULL OR h.NARRATION LIKE '%' || :narration || '%')
			      AND (:expenseGroupGlId IS NULL OR h.EXPENSE_GROUP_GL = :expenseGroupGlId)
			      AND (:fromCompanyId IS NULL OR h.FROM_COMPANY = :fromCompanyId)
			      AND (:companyId IS NULL OR h.COMPANY_POID = :companyId)
			      AND (:jvRef IS NULL OR h.JV_REF LIKE '%' || :jvRef || '%')
			      AND (:allocationType IS NULL OR h.ALLOCATION_TYPE = :allocationType)
			    ORDER BY h.TRANSACTION_DATE DESC, h.TRANSACTION_POID DESC
			""", nativeQuery = true)
	List<GlExpenseReallocationHdr> findWithFilters(@Param("groupPoid") Long groupPoid,
			@Param("dateFrom") Timestamp dateFrom, @Param("dateTo") Timestamp dateTo, @Param("docRef") String docRef,
			@Param("narration") String narration, @Param("expenseGroupGlId") Long expenseGroupGlId,
			@Param("fromCompanyId") Long fromCompanyId, @Param("companyId") Long companyId,
			@Param("jvRef") String jvRef, @Param("allocationType") String allocationType);
}
