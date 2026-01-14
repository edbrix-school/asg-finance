package com.asg.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.ComputeTotalsRequest;
import com.asg.finance.dto.ComputeTotalsResponse;
import com.asg.finance.dto.CreateExpenseReallocationRequest;
import com.asg.finance.dto.ExpenseReallocationConfigResponse;
import com.asg.finance.dto.ExpenseReallocationDetailRequest;
import com.asg.finance.dto.ExpenseReallocationDetailResponse;
import com.asg.finance.dto.ExpenseReallocationResponse;
import com.asg.finance.dto.ExpenseReallocationXlDetailResponse;
import com.asg.finance.dto.UpdateExpenseReallocationRequest;
import com.asg.finance.dto.ValidateAllocationResponse;
import com.asg.finance.entity.GlExpenseReallocationDtl;
import com.asg.finance.entity.GlExpenseReallocationHdr;
import com.asg.finance.entity.GlExpenseReallocationXlDtl;
import com.asg.finance.entity.SupplierMasterEntity;
import com.asg.finance.repository.ExpenseReallocationStoredProcedure;
import com.asg.finance.repository.GlExpenseReallocationDtlRepository;
import com.asg.finance.repository.GlExpenseReallocationHdrRepository;
import com.asg.finance.repository.GlExpenseReallocationXlDtlRepository;

import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseReallocationServiceImpl implements ExpenseReallocationService {

	private final GlExpenseReallocationHdrRepository hdrRepository;
	private final GlExpenseReallocationDtlRepository dtlRepository;
	private final GlExpenseReallocationXlDtlRepository xlDtlRepository;
	private final ExpenseReallocationStoredProcedure storedProcedureHelper;
	private final DocumentSearchService documentService;
	private final LoggingService loggingService;

	@Override
	@Transactional
	public ExpenseReallocationResponse createExpenseReallocation(CreateExpenseReallocationRequest request,
			Long groupPoid, Long companyPoid, String userId) {

		log.info("createExpenseReallocation started for groupPoid={} userId={}", groupPoid, userId);

		validateMandatoryFields(request);

		GlExpenseReallocationHdr header = GlExpenseReallocationHdr.builder()
				.transactionDate(request.getTransactionDate()).groupPoid(groupPoid)
				.companyPoid(request.getCompanyPoid() != null ? request.getCompanyPoid() : companyPoid)
				.expenseGroupGl(request.getExpenseGroupGlId()).fromCompany(request.getFromCompanyId())
				.fromDate(request.getFromDate()).toDate(request.getToDate()).allocationType(request.getAllocationType())
				.costPoid(request.getCostPoid()).remarks(request.getRemarks()).createdBy(userId)
				.createdDate(Timestamp.valueOf(LocalDateTime.now())).lastmodifiedBy(userId)
				.lastmodifiedDate(Timestamp.valueOf(LocalDateTime.now())).deleted("N").reportGeneration("N").build();

		final GlExpenseReallocationHdr savedHdr = hdrRepository.save(header);
		final Long hdrPoid = savedHdr.getTransactionPoid();
		Long maxDtlDetRowId = dtlRepository.findMaxDetRowIdByTransactionPoid(hdrPoid);
		AtomicLong dtlDetRowIdSeq = new AtomicLong(maxDtlDetRowId != null ? maxDtlDetRowId + 1 : 1);
		List<GlExpenseReallocationDtl> dtlEntities = request.getDetails().stream()
				.map(dto -> GlExpenseReallocationDtl.builder().transactionPoid(hdrPoid)
						.detRowId(dtlDetRowIdSeq.getAndIncrement()).company(dto.getCompany())
						.companyName(dto.getCompanyName()).sh(dto.getSh()).ff(dto.getFf()).ffs(dto.getFfs())
						.ffp(dto.getFfp()).properties(dto.getProperties()).mta(dto.getMta()).pda(dto.getPda())
						.admin(dto.getAdmin()).total(dto.getTotal()).remarks(dto.getRemarks()).createdBy(userId)
						.createdDate(Timestamp.valueOf(LocalDateTime.now())).lastmodifiedBy(userId)
						.lastmodifiedDate(Timestamp.valueOf(LocalDateTime.now())).build())
				.toList();

		dtlRepository.saveAll(dtlEntities);

		Long maxXlDetRowId = xlDtlRepository.findMaxDetRowIdByTransactionPoid(hdrPoid);
		AtomicLong xlDtlDetRowIdSeq = new AtomicLong(maxXlDetRowId != null ? maxXlDetRowId + 1 : 1);

		List<GlExpenseReallocationXlDtl> xlDtlEntities = request.getXlDetails().stream()
				.map(dto -> GlExpenseReallocationXlDtl.builder().transactionPoid(hdrPoid)
						.detRowId(xlDtlDetRowIdSeq.getAndIncrement()).company(dto.getCompany())
						.companyCode(dto.getCompanyCode()).costCentre(dto.getCostCentre()).percent(dto.getPercent())
						.remarks(dto.getRemarks()).createdBy(userId).createdDate(Timestamp.valueOf(LocalDateTime.now()))
						.lastmodifiedBy(userId).lastmodifiedDate(Timestamp.valueOf(LocalDateTime.now())).build())
				.toList();

		xlDtlRepository.saveAll(xlDtlEntities);
		log.info("createExpenseReallocation completed for transactionPoid={}", savedHdr.getTransactionPoid());
		loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), hdrPoid.toString());
		return buildResponse(savedHdr);
	}

	@Override
	@Transactional(readOnly = true)
	public ExpenseReallocationResponse getExpenseReallocationById(Long transactionPoid, Long groupPoid) {
		log.info("getExpenseReallocationById started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

		GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
						transactionPoid));

		ExpenseReallocationResponse response = buildResponse(header);

		log.info("getExpenseReallocationById completed for transactionPoid={}", transactionPoid);
		return response;
	}

	@Override
	@Transactional
	public ExpenseReallocationResponse updateExpenseReallocation(Long transactionPoid,
			UpdateExpenseReallocationRequest request, Long groupPoid, String userId) {
		log.info("updateExpenseReallocation started for transactionPoid={} groupPoid={} userId={}", transactionPoid,
				groupPoid, userId);

		GlExpenseReallocationHdr existingHeader = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
						transactionPoid));
		
		GlExpenseReallocationHdr header=new GlExpenseReallocationHdr();
		BeanUtils.copyProperties(existingHeader, header);
		

		if (header.getJvPoid() != null) {
			throw new RuntimeException("Cannot update expense reallocation that has JV created");
		}

		if ("Y".equals(header.getDeleted())) {
			throw new RuntimeException("Cannot update soft-deleted expense reallocation");
		}

		validateMandatoryFieldsForUpdate(request);

		if (request.getDetails() == null || request.getDetails().isEmpty()) {
			throw new RuntimeException("At least one detail line is required");
		}

		validateDetailLines(request.getDetails());

		header.setTransactionDate(request.getTransactionDate());
		header.setCompanyPoid(request.getCompanyPoid());
		header.setNarration(request.getNarration());
		header.setExpenseGroupGl(request.getExpenseGroupGlId());
		header.setFromCompany(request.getFromCompanyId());
		header.setFromDate(request.getFromDate());
		header.setToDate(request.getToDate());
		header.setAllocationType(request.getAllocationType());
		header.setCostPoid(request.getCostPoid());
		header.setRemarks(request.getRemarks());
		header.setLastmodifiedBy(userId);

		GlExpenseReallocationHdr savedHeader = hdrRepository.save(header);

		dtlRepository.deleteByTransactionPoid(transactionPoid);
		List<GlExpenseReallocationDtl> detailEntities = request.getDetails().stream()
				.map(dto -> GlExpenseReallocationDtl.builder().transactionPoid(transactionPoid)
						.detRowId(dto.getDetRowId() != null ? dto.getDetRowId() : 1).company(dto.getCompany())
						.companyName(dto.getCompanyName()).sh(dto.getSh()).ff(dto.getFf()).ffs(dto.getFfs())
						.ffp(dto.getFfp()).properties(dto.getProperties()).mta(dto.getMta()).pda(dto.getPda())
						.admin(dto.getAdmin()).total(dto.getTotal()).remarks(dto.getRemarks()).createdBy(userId)
						.createdDate(Timestamp.valueOf(LocalDateTime.now())).lastmodifiedBy(userId)
						.lastmodifiedDate(Timestamp.valueOf(LocalDateTime.now())).build())
				.toList();

		dtlRepository.saveAll(detailEntities);

		xlDtlRepository.deleteByTransactionPoid(transactionPoid);
		if (request.getXlDetails() != null && !request.getXlDetails().isEmpty()) {
			List<GlExpenseReallocationXlDtl> xlDetailEntities = request.getXlDetails().stream()
					.map(dto -> GlExpenseReallocationXlDtl.builder().transactionPoid(transactionPoid)
							.detRowId(dto.getDetRowId() != null ? dto.getDetRowId() : 1).company(dto.getCompany())
							.companyCode(dto.getCompanyCode()).costCentre(dto.getCostCentre()).percent(dto.getPercent())
							.remarks(dto.getRemarks()).createdBy(userId)
							.createdDate(Timestamp.valueOf(LocalDateTime.now())).lastmodifiedBy(userId)
							.lastmodifiedDate(Timestamp.valueOf(LocalDateTime.now())).build())
					.toList();

			xlDtlRepository.saveAll(xlDetailEntities);
		}

		log.info("updateExpenseReallocation completed for transactionPoid={}", transactionPoid);
		 String key = header.getTransactionPoid().toString();
	        String docId = UserContext.getDocumentId();
		loggingService.logChanges(existingHeader, header, GlExpenseReallocationHdr.class, 
                docId, key, LogDetailsEnum.MODIFIED, "SUPPLIER_POID");
		return buildResponse(savedHeader);
	}

	@Override
	@Transactional
	public void deleteExpenseReallocation(Long transactionPoid, Long groupPoid) {
		log.info("deleteExpenseReallocation started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

		GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
						transactionPoid));

		if (header.getJvPoid() != null) {
			throw new RuntimeException("Cannot delete expense reallocation that has JV created");
		}

		header.setDeleted("Y");
		hdrRepository.save(header);

		log.info("deleteExpenseReallocation completed for transactionPoid={}", transactionPoid);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> listOfRecordsAndGenericSearch(String documentId, FilterRequestDto filters,
			LocalDate startDate, LocalDate endDate, Pageable pageable) {

		log.info("getExpenseReallocations started for docId={}", documentId);

		String operator = documentService.resolveOperator(filters);
		String isDeleted = documentService.resolveIsDeleted(filters);
		List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate,
				endDate);

		RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted, "NARRATION",
				"TRANSACTION_POID");

		Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
		log.info("getExpenseReallocations completed for docId={} count={}", documentId, page.getNumber());
		return PaginationUtil.wrapPage(page, raw.displayFields());
	}

	@Override
	@Transactional
	public Map<String, String> createJv(Long transactionPoid, Long groupPoid, Long companyPoid, Long userId) {
		log.info("createJv started for transactionPoid={} groupPoid={} companyPoid={} userId={}", transactionPoid,
				groupPoid, companyPoid, userId);

		GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
						transactionPoid));

		if (header.getJvPoid() != null) {
			throw new RuntimeException("JV already created for this expense reallocation");
		}

		ValidateAllocationResponse validation = validateAllocation(transactionPoid, groupPoid);
		if (!validation.getValid()) {
			throw new RuntimeException("Allocations are not valid: " + String.join(", ", validation.getErrors()));
		}

		Date toDate = header.getToDate();
		Long expenseGroupGL = header.getExpenseGroupGl();
		String costPoid = header.getCostPoid();

		Map<String, String> result = storedProcedureHelper.createJv(groupPoid, userId, companyPoid, transactionPoid,
				expenseGroupGL, toDate, costPoid);

		return result;
	}

	@Override
	@Transactional
	public String generateReport(Long transactionPoid, Long groupPoid, String userId) {
		log.info("generateReport started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

		GlExpenseReallocationHdr header = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid",
						transactionPoid));

		Date toDate = header.getToDate();
		Long expenseGroupGL = header.getExpenseGroupGl();
		Long companyPoid = header.getCompanyPoid();
		String costPoid = header.getCostPoid();

		storedProcedureHelper.generateReport(companyPoid, toDate, expenseGroupGL, transactionPoid, costPoid);
		return "Report generated successfully";
	}

	@Override
	@Transactional(readOnly = true)
	public ValidateAllocationResponse validateAllocation(Long transactionPoid, Long groupPoid) {
		log.info("validateAllocation started for transactionPoid={} groupPoid={}", transactionPoid, groupPoid);

		hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid).orElseThrow(
				() -> new ResourceNotFoundException("Expense Reallocation", "transactionPoid", transactionPoid));

		List<GlExpenseReallocationDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		if (details == null || details.isEmpty()) {
			errors.add("At least one detail line is required");
		} else {
			Set<Long> companySet = new HashSet<>();
			for (GlExpenseReallocationDtl detail : details) {
				if (detail.getCompany() != null) {
					if (companySet.contains(detail.getCompany())) {
						errors.add("Duplicate company found: " + detail.getCompany());
					}
					companySet.add(detail.getCompany());
				}
			}
		}

		ExpenseReallocationResponse.DetailTotals totals = calculateTotals(transactionPoid);

		ValidateAllocationResponse response = new ValidateAllocationResponse();
		response.setValid(errors.isEmpty());
		response.setErrors(errors);
		response.setWarnings(warnings);

		ValidateAllocationResponse.AllocationTotals allocationTotals = new ValidateAllocationResponse.AllocationTotals();
		allocationTotals.setTotalSh(totals.getTotalSh());
		allocationTotals.setTotalFf(totals.getTotalFf());
		allocationTotals.setTotalFfs(totals.getTotalFfs());
		allocationTotals.setTotalFfp(totals.getTotalFfp());
		allocationTotals.setTotalProperties(totals.getTotalProperties());
		allocationTotals.setTotalMta(totals.getTotalMta());
		allocationTotals.setTotalPda(totals.getTotalPda());
		allocationTotals.setTotalAdmin(totals.getTotalAdmin());
		allocationTotals.setGrandTotal(totals.getGrandTotal());
		response.setTotals(allocationTotals);

		log.info("validateAllocation completed for transactionPoid={} valid={}", transactionPoid, response.getValid());
		return response;
	}

	@Override
	public ComputeTotalsResponse computeTotals(ComputeTotalsRequest request) {
		log.info("computeTotals started");

		if (request.getDetails() == null || request.getDetails().isEmpty()) {
			throw new RuntimeException("Details are required");
		}

		BigDecimal totalSh = BigDecimal.ZERO;
		BigDecimal totalFf = BigDecimal.ZERO;
		BigDecimal totalFfs = BigDecimal.ZERO;
		BigDecimal totalFfp = BigDecimal.ZERO;
		BigDecimal totalProperties = BigDecimal.ZERO;
		BigDecimal totalMta = BigDecimal.ZERO;
		BigDecimal totalPda = BigDecimal.ZERO;
		BigDecimal totalAdmin = BigDecimal.ZERO;

		for (ComputeTotalsRequest.AllocationDetail detail : request.getDetails()) {
			if (detail.getSh() != null)
				totalSh = totalSh.add(detail.getSh());
			if (detail.getFf() != null)
				totalFf = totalFf.add(detail.getFf());
			if (detail.getFfs() != null)
				totalFfs = totalFfs.add(detail.getFfs());
			if (detail.getFfp() != null)
				totalFfp = totalFfp.add(detail.getFfp());
			if (detail.getProperties() != null)
				totalProperties = totalProperties.add(detail.getProperties());
			if (detail.getMta() != null)
				totalMta = totalMta.add(detail.getMta());
			if (detail.getPda() != null)
				totalPda = totalPda.add(detail.getPda());
			if (detail.getAdmin() != null)
				totalAdmin = totalAdmin.add(detail.getAdmin());
		}

		BigDecimal grandTotal = totalSh.add(totalFf).add(totalFfs).add(totalFfp).add(totalProperties).add(totalMta)
				.add(totalPda).add(totalAdmin);

		ComputeTotalsResponse.AllocationTotals totals = new ComputeTotalsResponse.AllocationTotals();
		totals.setTotalSh(totalSh.setScale(3, RoundingMode.HALF_UP));
		totals.setTotalFf(totalFf.setScale(3, RoundingMode.HALF_UP));
		totals.setTotalFfs(totalFfs.setScale(3, RoundingMode.HALF_UP));
		totals.setTotalFfp(totalFfp.setScale(3, RoundingMode.HALF_UP));
		totals.setTotalProperties(totalProperties.setScale(3, RoundingMode.HALF_UP));
		totals.setTotalMta(totalMta.setScale(3, RoundingMode.HALF_UP));
		totals.setTotalPda(totalPda.setScale(3, RoundingMode.HALF_UP));
		totals.setTotalAdmin(totalAdmin.setScale(3, RoundingMode.HALF_UP));

		ComputeTotalsResponse response = new ComputeTotalsResponse();
		response.setTotals(totals);
		response.setGrandTotal(grandTotal.setScale(3, RoundingMode.HALF_UP));

		log.info("computeTotals completed");
		return response;
	}

	@Override
	public ExpenseReallocationConfigResponse getConfig() {
		log.info("getConfig started");

		ExpenseReallocationConfigResponse response = new ExpenseReallocationConfigResponse();
		response.setGlPosting(false);
		response.setAllowEditAfterJvCreation(false);
		response.setAllowDeleteAfterJvCreation(false);
		response.setScale(3);

		List<String> allocationColumns = Arrays.stream(GlExpenseReallocationDtl.class.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(Column.class))
				.map(field -> field.getAnnotation(Column.class)).map(Column::name)
				.filter(name -> !List.of("TRANSACTION_POID", "DET_ROW_ID", "COMPANY", "COMPANY_NAME", "TOTAL",
						"REMARKS", "CREATED_BY", "CREATED_DATE", "LASTMODIFIED_BY", "LASTMODIFIED_DATE").contains(name))
				.toList();

		response.setAllocationColumns(allocationColumns);

		log.info("getConfig completed");
		return response;
	}

	private void validateMandatoryFields(CreateExpenseReallocationRequest request) {
		if (request.getTransactionDate() == null) {
			throw new RuntimeException("Transaction Date is required");
		}
		if (request.getCompanyPoid() == null) {
			throw new RuntimeException("Company POID is required");
		}
		if (request.getExpenseGroupGlId() == null) {
			throw new RuntimeException("Expense Group GL is required");
		}
		if (request.getFromCompanyId() == null) {
			throw new RuntimeException("From Company is required");
		}
	}

	private void validateMandatoryFieldsForUpdate(UpdateExpenseReallocationRequest request) {
		if (request.getTransactionDate() == null) {
			throw new RuntimeException("Transaction Date is required");
		}
		if (request.getCompanyPoid() == null) {
			throw new RuntimeException("Company POID is required");
		}
		if (request.getExpenseGroupGlId() == null) {
			throw new RuntimeException("Expense Group GL is required");
		}
		if (request.getFromCompanyId() == null) {
			throw new RuntimeException("From Company is required");
		}
	}

	private void validateDetailLines(List<ExpenseReallocationDetailRequest> details) {
		Set<Long> companySet = new HashSet<>();
		for (ExpenseReallocationDetailRequest detail : details) {
			if (detail.getCompany() == null) {
				throw new RuntimeException("Company is required for all detail lines");
			}
			if (companySet.contains(detail.getCompany())) {
				throw new RuntimeException("Duplicate company found: " + detail.getCompany());
			}
			companySet.add(detail.getCompany());

			if (detail.getTotal() == null) {
				BigDecimal total = BigDecimal.ZERO;
				if (detail.getSh() != null)
					total = total.add(detail.getSh());
				if (detail.getFf() != null)
					total = total.add(detail.getFf());
				if (detail.getFfs() != null)
					total = total.add(detail.getFfs());
				if (detail.getFfp() != null)
					total = total.add(detail.getFfp());
				if (detail.getProperties() != null)
					total = total.add(detail.getProperties());
				if (detail.getMta() != null)
					total = total.add(detail.getMta());
				if (detail.getPda() != null)
					total = total.add(detail.getPda());
				if (detail.getAdmin() != null)
					total = total.add(detail.getAdmin());
				detail.setTotal(total.setScale(3, RoundingMode.HALF_UP));
			}
		}
	}

	private ExpenseReallocationResponse buildResponse(GlExpenseReallocationHdr header) {
		ExpenseReallocationResponse response = new ExpenseReallocationResponse();
		response.setTransactionPoid(header.getTransactionPoid());
		response.setTransactionDate(header.getTransactionDate());
		response.setGroupPoid(header.getGroupPoid());
		response.setCompanyPoid(header.getCompanyPoid());
		response.setCompanyName(null);
		response.setDocRef(header.getDocRef());
		response.setNarration(header.getNarration());
		response.setExpenseGroupGlId(header.getExpenseGroupGl());
		response.setExpenseGroupGlName(null);
		response.setFromCompanyId(header.getFromCompany());
		response.setFromCompanyName(null);
		response.setJvPoid(header.getJvPoid());
		response.setJvRef(header.getJvRef());
		response.setRemarks(header.getRemarks());
		response.setFromDate(header.getFromDate());
		response.setToDate(header.getToDate());
		response.setCostPoid(header.getCostPoid());
		response.setReportGeneration(header.getReportGeneration());
		response.setAllocationType(header.getAllocationType());
		response.setCreatedBy(header.getCreatedBy());
		response.setCreatedDate(header.getCreatedDate());
		response.setLastmodifiedBy(header.getLastmodifiedBy());
		response.setLastmodifiedDate(header.getLastmodifiedDate());
		response.setDeleted(header.getDeleted());
		response.setGlPosting(false);

		List<GlExpenseReallocationDtl> details = dtlRepository.findByTransactionPoid(header.getTransactionPoid());
		response.setDetails(details.stream().map(this::convertDetailToResponse).collect(Collectors.toList()));

		ExpenseReallocationResponse.DetailTotals detailTotals = calculateTotals(header.getTransactionPoid());
		response.setDetailTotals(detailTotals);

		List<GlExpenseReallocationXlDtl> xlDetails = xlDtlRepository.findByTransactionPoid(header.getTransactionPoid());
		response.setXlDetails(xlDetails.stream().map(this::convertXlDetailToResponse).collect(Collectors.toList()));

		return response;
	}

	private ExpenseReallocationResponse.DetailTotals calculateTotals(Long transactionPoid) {
		ExpenseReallocationResponse.DetailTotals totals = new ExpenseReallocationResponse.DetailTotals();
		totals.setTotalSh(dtlRepository.getTotalShByTransactionPoid(transactionPoid));
		totals.setTotalFf(dtlRepository.getTotalFfByTransactionPoid(transactionPoid));
		totals.setTotalFfs(dtlRepository.getTotalFfsByTransactionPoid(transactionPoid));
		totals.setTotalFfp(dtlRepository.getTotalFfpByTransactionPoid(transactionPoid));
		totals.setTotalProperties(dtlRepository.getTotalPropertiesByTransactionPoid(transactionPoid));
		totals.setTotalMta(dtlRepository.getTotalMtaByTransactionPoid(transactionPoid));
		totals.setTotalPda(dtlRepository.getTotalPdaByTransactionPoid(transactionPoid));
		totals.setTotalAdmin(dtlRepository.getTotalAdminByTransactionPoid(transactionPoid));
		totals.setGrandTotal(dtlRepository.getGrandTotalByTransactionPoid(transactionPoid));
		return totals;
	}

	private ExpenseReallocationDetailResponse convertDetailToResponse(GlExpenseReallocationDtl detail) {
		ExpenseReallocationDetailResponse response = new ExpenseReallocationDetailResponse();
		response.setDetRowId(detail.getDetRowId());
		response.setCompany(detail.getCompany());
		response.setCompanyName(detail.getCompanyName());
		response.setSh(detail.getSh());
		response.setFf(detail.getFf());
		response.setFfs(detail.getFfs());
		response.setFfp(detail.getFfp());
		response.setProperties(detail.getProperties());
		response.setMta(detail.getMta());
		response.setPda(detail.getPda());
		response.setAdmin(detail.getAdmin());
		response.setTotal(detail.getTotal());
		response.setRemarks(detail.getRemarks());
		return response;
	}

	private ExpenseReallocationXlDetailResponse convertXlDetailToResponse(GlExpenseReallocationXlDtl xlDetail) {
		ExpenseReallocationXlDetailResponse response = new ExpenseReallocationXlDetailResponse();
		response.setDetRowId(xlDetail.getDetRowId());
		response.setCompany(xlDetail.getCompany());
		response.setCompanyCode(xlDetail.getCompanyCode());
		response.setCompanyName(null);
		response.setCostCentre(xlDetail.getCostCentre());
		response.setPercent(xlDetail.getPercent());
		response.setRemarks(xlDetail.getRemarks());
		return response;
	}
}
