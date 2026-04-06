package com.asg.finance.pettycashvoucher.service.impl;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.entity.GLPettyCashItemDtl;
import com.asg.finance.entity.GlPettyCashChargeDtl;
import com.asg.finance.entity.GlPettyCashPaymentDtl;
import com.asg.finance.entity.GlPettyCashPaymentGrnDtl;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.impl.PettyCashVoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("PettyCashVoucherServiceImpl Unit Tests")
class PettyCashVoucherServiceImplTest {

    @Mock private GlPettyCashPaymentHdrRepository glPettyCashPaymentHdrRepository;
    @Mock private GlPettyCashPaymentDtlRepository glPettyCashPaymentDtlRepository;
    @Mock private GLPettyCashItemDtlRepository glPettyCashItemDtlRepository;
    @Mock private GlPettyCashChargeDtlRepository glPettyCashChargeDtlRepository;
    @Mock private GlPettyCashPaymentGrnDtlRepository glPettyCashPaymentGrnDtlRepository;
    @Mock private com.asg.finance.repository.GLMasterRepository glMasterRepository;
    @Mock private com.asg.finance.repository.StockMasterRepository stockMasterRepository;
    @Mock private ShipChargeRepository shipChargeRepository;
    @Mock private com.asg.finance.repository.UnitMasterRepository unitMasterRepository;
    @Mock private com.asg.common.lib.service.DocumentSearchService documentService;
    @Mock private com.asg.finance.repository.TaxMasterRepository taxMasterRepository;
    @Mock private com.asg.common.lib.service.LovDataService lovService;
    @Mock private CostCenterBreakupService costCenterBreakupService;
    @Mock private BillwiseBreakupService billwiseBreakupService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private PettyCashLoadByRefTypeRepository pettyCashLoadByRefTypeRepository;
    @Mock private PettyCashPaymentVoucherCustomRepository pettyCashPaymentVoucherCustomRepository;
    @Mock private com.asg.finance.repository.SupplierMasterRepository supplierMasterRepository;
    @Mock private com.asg.finance.repository.AdvancePettyCashHdrRepository advancePettyCashHdrRepository;
    @Mock private com.asg.finance.repository.AssetLocationMasterRepository assetLocationMasterRepository;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private LoggingService loggingService;
    @Mock private GlobalParameterService globalParameterService;

    @InjectMocks
    private PettyCashVoucherServiceImpl service;

    // -------------------------------------------------------------------
    // Helper to build a minimal PettyCashCreateRequestDto
    // -------------------------------------------------------------------

    private PettyCashCreateRequestDto buildDto(String refType) {
        return PettyCashCreateRequestDto.builder()
                .refType(refType)
                .amount(BigDecimal.TEN)
                .roundingAmount(BigDecimal.ZERO)
                .build();
    }

    // ===================================================================
    // 1. resolveVoucherRefByType
    // ===================================================================
    @Nested
    @DisplayName("resolveVoucherRefByType")
    class ResolveVoucherRefByTypeTests {

        @Test
        @DisplayName("returns fdaRef for FDA JOBS")
        void fdaJobs_returnsFdaRef() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("FDA JOBS").fdaRef("FDA-001").ffRef("FF-001").build();
            String result = invokeResolveVoucherRef("FDA JOBS", dto);
            assertEquals("FDA-001", result);
        }

        @Test
        @DisplayName("returns ffRef for FF JOBS")
        void ffJobs_returnsFfRef() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("FF JOBS").fdaRef("FDA-001").ffRef("FF-001").build();
            String result = invokeResolveVoucherRef("FF JOBS", dto);
            assertEquals("FF-001", result);
        }

        @Test
        @DisplayName("returns salesQtnRef for MTA RFQ")
        void mtaRfq_returnsSalesQtnRef() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("MTA RFQ").salesQtnRef("RFQ-123").build();
            String result = invokeResolveVoucherRef("MTA RFQ", dto);
            assertEquals("RFQ-123", result);
        }

        @Test
        @DisplayName("returns poRef for GENERAL PO")
        void generalPo_returnsPoRef() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("GENERAL PO").poRef("PO-456").build();
            String result = invokeResolveVoucherRef("GENERAL PO", dto);
            assertEquals("PO-456", result);
        }

        @Test
        @DisplayName("returns poRef for GRN_JOBS when poRef is present")
        void grnJobs_returnsPoRefWhenPresent() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("GRN_JOBS").poRef("PO-789").grnSupplierPoid(999L).build();
            String result = invokeResolveVoucherRef("GRN_JOBS", dto);
            assertEquals("PO-789", result);
        }

        @Test
        @DisplayName("returns grnSupplierPoid as string for GRN_JOBS when poRef is blank")
        void grnJobs_returnsGrnSupplierPoidWhenPoRefBlank() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("GRN_JOBS").poRef(null).grnSupplierPoid(999L).build();
            String result = invokeResolveVoucherRef("GRN_JOBS", dto);
            assertEquals("999", result);
        }

        @Test
        @DisplayName("returns null for GENERAL refType")
        void general_returnsNull() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("GENERAL").build();
            String result = invokeResolveVoucherRef("GENERAL", dto);
            assertNull(result);
        }

        private String invokeResolveVoucherRef(String refType, PettyCashRequestBase dto) {
            return (String) ReflectionTestUtils.invokeMethod(service, "resolveVoucherRefByType", refType, dto);
        }
    }

    // ===================================================================
    // 2. buildGlPoidStringWithoutTax and buildGlPoidStringAll
    // ===================================================================
    @Nested
    @DisplayName("GL poid string builders")
    class GlPoidStringBuilderTests {

        @Test
        @DisplayName("buildGlPoidStringWithoutTax excludes rows with taxPoid and deleted rows")
        void withoutTax_excludesTaxRowsAndDeleted() {
            List<GlPettyCashPaymentDtlRequestDto> dtls = Arrays.asList(
                    GlPettyCashPaymentDtlRequestDto.builder().glPoid(100L).taxPoid(null).actionType("isCreated").build(),
                    GlPettyCashPaymentDtlRequestDto.builder().glPoid(200L).taxPoid(5L).actionType("isCreated").build(),
                    GlPettyCashPaymentDtlRequestDto.builder().glPoid(300L).taxPoid(null).actionType("isDeleted").build()
            );
            String result = (String) ReflectionTestUtils.invokeMethod(service, "buildGlPoidStringWithoutTax", dtls);
            assertEquals("100", result);
        }

        @Test
        @DisplayName("buildGlPoidStringAll includes all non-deleted rows with glPoid")
        void all_includesNonDeletedRows() {
            List<GlPettyCashPaymentDtlRequestDto> dtls = Arrays.asList(
                    GlPettyCashPaymentDtlRequestDto.builder().glPoid(100L).actionType("isCreated").build(),
                    GlPettyCashPaymentDtlRequestDto.builder().glPoid(200L).taxPoid(5L).actionType("isCreated").build(),
                    GlPettyCashPaymentDtlRequestDto.builder().glPoid(300L).actionType("isDeleted").build()
            );
            String result = (String) ReflectionTestUtils.invokeMethod(service, "buildGlPoidStringAll", dtls);
            assertEquals("100,200", result);
        }

        @Test
        @DisplayName("returns empty string for null input")
        void nullInput_returnsEmpty() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "buildGlPoidStringWithoutTax", (Object) null);
            assertEquals("", result);
        }
    }

    // ===================================================================
    // 3. checkAll filtering — validateAmountVsChargeTotal
    // ===================================================================
    @Nested
    @DisplayName("checkAll filtering in validateAmountVsChargeTotal")
    class CheckAllChargeFilterTests {

        @Test
        @DisplayName("rows with checkAll=N are excluded from total computation")
        void checkAllN_rowsExcluded() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("50"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashChargeDtlRequestDtos(Arrays.asList(
                            GlPettyCashChargeDtlRequestDto.builder().chargeAmount(new BigDecimal("50")).checkAll("Y").actionType("isCreated").build(),
                            GlPettyCashChargeDtlRequestDto.builder().chargeAmount(new BigDecimal("999")).checkAll("N").actionType("isCreated").build()
                    ))
                    .build();
            // Should not throw — checkAll=N row is excluded, remaining 50 matches amount 50
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsChargeTotal", dto));
        }
    }

    // ===================================================================
    // 4. validateAmountVsChargeTotal
    // ===================================================================
    @Nested
    @DisplayName("validateAmountVsChargeTotal")
    class ValidateAmountVsChargeTotalTests {

        @Test
        @DisplayName("throws ValidationException when charge list is empty")
        void emptyList_throwsValidation() {
            PettyCashCreateRequestDto dto = buildDto("FF JOBS");
            dto.setGlPettyCashChargeDtlRequestDtos(Collections.emptyList());
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsChargeTotal", dto));
        }

        @Test
        @DisplayName("throws ValidationException when amount does not match charge total")
        void amountMismatch_throwsValidation() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashChargeDtlRequestDtos(List.of(
                            GlPettyCashChargeDtlRequestDto.builder().chargeAmount(new BigDecimal("80")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsChargeTotal", dto));
        }

        @Test
        @DisplayName("passes validation when amount equals charge total")
        void correctAmount_passes() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashChargeDtlRequestDtos(List.of(
                            GlPettyCashChargeDtlRequestDto.builder().chargeAmount(new BigDecimal("100")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsChargeTotal", dto));
        }

        @Test
        @DisplayName("passes validation when amount equals charge total plus rounding")
        void amountEqualsChargeAndRounding_passes() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100.50"))
                    .roundingAmount(new BigDecimal("0.50"))
                    .glPettyCashChargeDtlRequestDtos(List.of(
                            GlPettyCashChargeDtlRequestDto.builder().chargeAmount(new BigDecimal("100")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsChargeTotal", dto));
        }

        @Test
        @DisplayName("deleted rows are excluded from total computation")
        void deletedRows_excluded() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashChargeDtlRequestDtos(Arrays.asList(
                            GlPettyCashChargeDtlRequestDto.builder().chargeAmount(new BigDecimal("100")).checkAll("Y").actionType("isCreated").build(),
                            GlPettyCashChargeDtlRequestDto.builder().chargeAmount(new BigDecimal("999")).checkAll("Y").actionType("isDeleted").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsChargeTotal", dto));
        }
    }

    // ===================================================================
    // 5. validateAmountVsItemTotal
    // ===================================================================
    @Nested
    @DisplayName("validateAmountVsItemTotal")
    class ValidateAmountVsItemTotalTests {

        @Test
        @DisplayName("throws ValidationException when item list is empty")
        void emptyList_throwsValidation() {
            PettyCashCreateRequestDto dto = buildDto("MTA RFQ");
            dto.setGlPettyCashItemDtlRequestDtos(Collections.emptyList());
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsItemTotal", dto));
        }

        @Test
        @DisplayName("throws ValidationException when amount does not match item total")
        void amountMismatch_throwsValidation() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashItemDtlRequestDtos(List.of(
                            GlPettyCashItemDtlRequestDto.builder().total(new BigDecimal("80")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsItemTotal", dto));
        }

        @Test
        @DisplayName("passes validation when amount equals item total")
        void correctAmount_passes() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashItemDtlRequestDtos(List.of(
                            GlPettyCashItemDtlRequestDto.builder().total(new BigDecimal("100")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsItemTotal", dto));
        }

        @Test
        @DisplayName("rows with checkAll=N are excluded from total computation")
        void checkAllN_excluded() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("50"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashItemDtlRequestDtos(Arrays.asList(
                            GlPettyCashItemDtlRequestDto.builder().total(new BigDecimal("50")).checkAll("Y").actionType("isCreated").build(),
                            GlPettyCashItemDtlRequestDto.builder().total(new BigDecimal("999")).checkAll("N").actionType("isCreated").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsItemTotal", dto));
        }
    }

    // ===================================================================
    // 6. resolveChargeFrom
    // ===================================================================
    @Nested
    @DisplayName("resolveChargeFrom")
    class ResolveChargeFromTests {

        @Test
        @DisplayName("returns FF for FF JOBS when existing is null")
        void ffJobs_nullExisting_returnsFF() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", null, "FF JOBS");
            assertEquals("FF", result);
        }

        @Test
        @DisplayName("returns FDA for FDA JOBS when existing is null")
        void fdaJobs_nullExisting_returnsFDA() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", null, "FDA JOBS");
            assertEquals("FDA", result);
        }

        @Test
        @DisplayName("returns existing value even for FF JOBS when already set")
        void ffJobs_existingValue_returnsExisting() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", "CUSTOM_FROM", "FF JOBS");
            assertEquals("CUSTOM_FROM", result);
        }

        @Test
        @DisplayName("returns null for GENERAL refType with null existing")
        void general_nullExisting_returnsNull() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", null, "GENERAL");
            assertNull(result);
        }

        @Test
        @DisplayName("returns existing value for GENERAL refType when set")
        void general_withExisting_returnsExisting() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", "SOME_FROM", "GENERAL");
            assertEquals("SOME_FROM", result);
        }
    }

    // ===================================================================
    // 7. validateAmountVsGrnTotal
    // ===================================================================
    @Nested
    @DisplayName("validateAmountVsGrnTotal")
    class ValidateAmountVsGrnTotalTests {

        @Test
        @DisplayName("throws ValidationException when GRN list is empty")
        void emptyList_throwsValidation() {
            PettyCashCreateRequestDto dto = buildDto("GRN_JOBS");
            dto.setGlPettyCashGrnDtlRequestDtos(Collections.emptyList());
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsGrnTotal", dto));
        }

        @Test
        @DisplayName("throws ValidationException when amount does not match GRN total")
        void amountMismatch_throwsValidation() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashGrnDtlRequestDtos(List.of(
                            GlPettyCashPaymentGrnDtlRequestDto.builder().amount(new BigDecimal("80")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsGrnTotal", dto));
        }

        @Test
        @DisplayName("passes when amount equals GRN total")
        void correctAmount_passes() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashGrnDtlRequestDtos(List.of(
                            GlPettyCashPaymentGrnDtlRequestDto.builder().amount(new BigDecimal("100")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsGrnTotal", dto));
        }

        @Test
        @DisplayName("passes when amount equals GRN total plus rounding")
        void amountEqualsGrnAndRounding_passes() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100.25"))
                    .roundingAmount(new BigDecimal("0.25"))
                    .glPettyCashGrnDtlRequestDtos(List.of(
                            GlPettyCashPaymentGrnDtlRequestDto.builder().amount(new BigDecimal("100")).checkAll("Y").actionType("isCreated").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsGrnTotal", dto));
        }

        @Test
        @DisplayName("rows with checkAll=N are excluded from total computation")
        void checkAllN_excluded() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("50"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashGrnDtlRequestDtos(Arrays.asList(
                            GlPettyCashPaymentGrnDtlRequestDto.builder().amount(new BigDecimal("50")).checkAll("Y").actionType("isCreated").build(),
                            GlPettyCashPaymentGrnDtlRequestDto.builder().amount(new BigDecimal("999")).checkAll("N").actionType("isCreated").build()
                    ))
                    .build();
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsGrnTotal", dto));
        }
    }

    // ===================================================================
    // 8. chargeFrom defaulting via resolveChargeFrom (public effect)
    // ===================================================================
    @Nested
    @DisplayName("chargeFrom defaulting")
    class ChargeFromDefaultingTests {

        @Test
        @DisplayName("null chargeFrom is defaulted to FF for FF JOBS refType")
        void nullChargeFrom_ffJobs_defaultedToFF() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", null, "FF JOBS");
            assertEquals("FF", result);
        }

        @Test
        @DisplayName("null chargeFrom is defaulted to FDA for FDA JOBS refType")
        void nullChargeFrom_fdaJobs_defaultedToFDA() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", null, "FDA JOBS");
            assertEquals("FDA", result);
        }

        @Test
        @DisplayName("empty chargeFrom is defaulted to FF for FF JOBS refType")
        void emptyChargeFrom_ffJobs_defaultedToFF() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", "", "FF JOBS");
            assertEquals("FF", result);
        }

        @Test
        @DisplayName("blank chargeFrom is defaulted to FF for FF JOBS refType")
        void blankChargeFrom_ffJobs_defaultedToFF() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveChargeFrom", "  ", "FF JOBS");
            assertEquals("FF", result);
        }
    }

    // ===================================================================
    // 9. Mandatory detail validation failures per refType
    // ===================================================================
    @Nested
    @DisplayName("Mandatory detail empty failures")
    class MandatoryDetailValidationTests {

        @Test
        @DisplayName("getActivePaymentDtls returns empty list when all rows are deleted")
        void allDeleted_returnsEmpty() {
            List<GlPettyCashPaymentDtlRequestDto> dtls = Arrays.asList(
                    GlPettyCashPaymentDtlRequestDto.builder().actionType("isDeleted").glPoid(100L).build(),
                    GlPettyCashPaymentDtlRequestDto.builder().actionType("ISDELETED").glPoid(200L).build()
            );
            @SuppressWarnings("unchecked")
            List<GlPettyCashPaymentDtlRequestDto> result =
                    (List<GlPettyCashPaymentDtlRequestDto>) ReflectionTestUtils.invokeMethod(service, "getActivePaymentDtls", dtls);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getActivePaymentDtls returns non-deleted rows")
        void someActive_returnsActiveOnly() {
            List<GlPettyCashPaymentDtlRequestDto> dtls = Arrays.asList(
                    GlPettyCashPaymentDtlRequestDto.builder().actionType("isCreated").glPoid(100L).build(),
                    GlPettyCashPaymentDtlRequestDto.builder().actionType("isDeleted").glPoid(200L).build()
            );
            @SuppressWarnings("unchecked")
            List<GlPettyCashPaymentDtlRequestDto> result =
                    (List<GlPettyCashPaymentDtlRequestDto>) ReflectionTestUtils.invokeMethod(service, "getActivePaymentDtls", dtls);
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(100L, result.get(0).getGlPoid());
        }

        @Test
        @DisplayName("getActivePaymentDtls returns null-action rows as active")
        void nullAction_treatedAsActive() {
            List<GlPettyCashPaymentDtlRequestDto> dtls = List.of(
                    GlPettyCashPaymentDtlRequestDto.builder().actionType(null).glPoid(100L).build()
            );
            @SuppressWarnings("unchecked")
            List<GlPettyCashPaymentDtlRequestDto> result =
                    (List<GlPettyCashPaymentDtlRequestDto>) ReflectionTestUtils.invokeMethod(service, "getActivePaymentDtls", dtls);
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("validateAmountVsChargeTotal throws when charge list is null")
        void nullChargeList_throwsValidation() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashChargeDtlRequestDtos(null)
                    .build();
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsChargeTotal", dto));
        }

        @Test
        @DisplayName("validateAmountVsItemTotal throws when item list is null")
        void nullItemList_throwsValidation() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashItemDtlRequestDtos(null)
                    .build();
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsItemTotal", dto));
        }

        @Test
        @DisplayName("validateAmountVsGrnTotal throws when GRN list is null")
        void nullGrnList_throwsValidation() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .amount(new BigDecimal("100"))
                    .roundingAmount(BigDecimal.ZERO)
                    .glPettyCashGrnDtlRequestDtos(null)
                    .build();
            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateAmountVsGrnTotal", dto));
        }

        @Test
        @DisplayName("isPaymentDtlRefType returns true for GENERAL")
        void isPaymentDtlRefType_general() {
            Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(service, "isPaymentDtlRefType", "GENERAL");
            assertTrue(result);
        }

        @Test
        @DisplayName("isPaymentDtlRefType returns true for CUSTOM")
        void isPaymentDtlRefType_custom() {
            Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(service, "isPaymentDtlRefType", "CUSTOM");
            assertTrue(result);
        }

        @Test
        @DisplayName("isPaymentDtlRefType returns true for SUPPLIER")
        void isPaymentDtlRefType_supplier() {
            Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(service, "isPaymentDtlRefType", "SUPPLIER");
            assertTrue(result);
        }

        @Test
        @DisplayName("isPaymentDtlRefType returns true for CUSTOMER")
        void isPaymentDtlRefType_customer() {
            Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(service, "isPaymentDtlRefType", "CUSTOMER");
            assertTrue(result);
        }

        @Test
        @DisplayName("isPaymentDtlRefType returns false for FF JOBS")
        void isPaymentDtlRefType_ffJobs_false() {
            Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(service, "isPaymentDtlRefType", "FF JOBS");
            assertFalse(result);
        }

        @Test
        @DisplayName("isPaymentDtlRefType returns false for GRN_JOBS")
        void isPaymentDtlRefType_grnJobs_false() {
            Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(service, "isPaymentDtlRefType", "GRN_JOBS");
            assertFalse(result);
        }
    }

    // ===================================================================
    // 10. validateJobBeforeSave called on create for FDA/FF/MTA RFQ
    // ===================================================================
    @Nested
    @DisplayName("resolveRefPoidByType for create-path job validation")
    class ResolveRefPoidForJobValidationTests {

        @Test
        @DisplayName("resolveRefPoidByType returns fdaRef for FDA JOBS")
        void fdaJobs_returnsFdaRef() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("FDA JOBS").fdaRef("FDA-001").build();
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveRefPoidByType", dto, "FDA JOBS");
            assertEquals("FDA-001", result);
        }

        @Test
        @DisplayName("resolveRefPoidByType returns ffRef for FF JOBS")
        void ffJobs_returnsffRef() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("FF JOBS").ffRef("FF-002").build();
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveRefPoidByType", dto, "FF JOBS");
            assertEquals("FF-002", result);
        }

        @Test
        @DisplayName("resolveRefPoidByType returns salesQtnRef for MTA RFQ")
        void mtaRfq_returnsSalesQtnRef() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("MTA RFQ").salesQtnRef("RFQ-999").build();
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveRefPoidByType", dto, "MTA RFQ");
            assertEquals("RFQ-999", result);
        }

        @Test
        @DisplayName("resolveRefPoidByType returns null for GENERAL (no job proc)")
        void general_returnsNull() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .refType("GENERAL").build();
            String result = (String) ReflectionTestUtils.invokeMethod(service, "resolveRefPoidByType", dto, "GENERAL");
            assertNull(result);
        }
    }

    // ===================================================================
    // 11. safeStatus helper
    // ===================================================================
    @Nested
    @DisplayName("safeStatus")
    class SafeStatusTests {

        @Test
        @DisplayName("returns empty string when status is null")
        void nullStatus_returnsEmpty() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "safeStatus", (Object) null);
            assertEquals("", result);
        }

        @Test
        @DisplayName("returns the status value when non-null")
        void nonNull_returnsValue() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "safeStatus", "ADVANCE");
            assertEquals("ADVANCE", result);
        }
    }

    // ===================================================================
    // 12. validateBeforeSave arg 9 = status (safeStatus produces correct value)
    // ===================================================================
    @Nested
    @DisplayName("validateBeforeSave partyType arg alignment")
    class ValidateBeforeSavePartyTypeArgTests {

        @Test
        @DisplayName("safeStatus returns empty string for null — ensures arg 9 is never null")
        void safeStatus_nullSafe() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "safeStatus", (Object) null);
            assertEquals("", result);
        }

        @Test
        @DisplayName("safeStatus passes through AGAINST_ADVANCE status intact")
        void safeStatus_againstAdvance() {
            String result = (String) ReflectionTestUtils.invokeMethod(service, "safeStatus", "AGAINST_ADVANCE");
            assertEquals("AGAINST_ADVANCE", result);
        }
    }

    // ===================================================================
    // 13. validateCashBalance
    // ===================================================================
    @Nested
    @DisplayName("CashBalanceValidationTests")
    class CashBalanceValidationTests {

        @Test
        @DisplayName("amountExceedsBalance_throws")
        void amountExceedsBalance_throws() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .pettyCashGlPoid(10L)
                    .amount(new BigDecimal("500"))
                    .build();
            when(pettyCashLoadByRefTypeRepository.getPettyGlBalance(
                    any(), any(), any(), any(), eq(10L), isNull(), eq(0L)))
                    .thenReturn(PettyRefTypeResponse.<PettyGlBalanceDto>builder()
                            .responseList(List.of(PettyGlBalanceDto.builder().balance(new BigDecimal("100")).build()))
                            .build());

            assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "validateCashBalance", dto, "DOC-001"));
        }

        @Test
        @DisplayName("amountWithinBalance_passes")
        void amountWithinBalance_passes() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .pettyCashGlPoid(10L)
                    .amount(new BigDecimal("50"))
                    .build();
            when(pettyCashLoadByRefTypeRepository.getPettyGlBalance(
                    any(), any(), any(), any(), eq(10L), isNull(), eq(0L)))
                    .thenReturn(PettyRefTypeResponse.<PettyGlBalanceDto>builder()
                            .responseList(List.of(PettyGlBalanceDto.builder().balance(new BigDecimal("100")).build()))
                            .build());

            assertDoesNotThrow(
                    () -> ReflectionTestUtils.invokeMethod(service, "validateCashBalance", dto, "DOC-001"));
        }

        @Test
        @DisplayName("nullBalance_passes")
        void nullBalance_passes() {
            PettyCashCreateRequestDto dto = PettyCashCreateRequestDto.builder()
                    .pettyCashGlPoid(10L)
                    .amount(new BigDecimal("500"))
                    .build();
            when(pettyCashLoadByRefTypeRepository.getPettyGlBalance(
                    any(), any(), any(), any(), eq(10L), isNull(), eq(0L)))
                    .thenReturn(PettyRefTypeResponse.<PettyGlBalanceDto>builder()
                            .responseList(List.of(PettyGlBalanceDto.builder().balance(null).build()))
                            .build());

            assertDoesNotThrow(
                    () -> ReflectionTestUtils.invokeMethod(service, "validateCashBalance", dto, "DOC-001"));
        }
    }

    // ===================================================================
    // 14. getAllowedRefTypes
    // ===================================================================
    @Nested
    @DisplayName("RefTypeFilteringTests")
    class RefTypeFilteringTests {

        @Test
        @DisplayName("returnsFilteredList_forUser")
        void returnsFilteredList_forUser() {
            when(pettyCashPaymentVoucherCustomRepository.getRefTypeWhereClause(1L))
                    .thenReturn("'GENERAL','FF JOBS','FDA JOBS'");

            List<String> result = service.getAllowedRefTypes(1L);

            assertEquals(List.of("GENERAL", "FF JOBS", "FDA JOBS"), result);
        }

        @Test
        @DisplayName("handlesEmptyResult")
        void handlesEmptyResult() {
            when(pettyCashPaymentVoucherCustomRepository.getRefTypeWhereClause(2L))
                    .thenReturn("");

            List<String> result = service.getAllowedRefTypes(2L);

            assertTrue(result.isEmpty());
        }
    }

    // ===================================================================
    // 15. loadPettyCashFromGrn
    // ===================================================================
    @Nested
    @DisplayName("LoadFromGrnTests")
    class LoadFromGrnTests {

        @Test
        @DisplayName("validSupplier_returnsGrnRows")
        void validSupplier_returnsGrnRows() {
            List<PettyCashFromGrnDto> expected = List.of(
                    PettyCashFromGrnDto.builder().transactionPoid(1L).grandTotal(new BigDecimal("200")).build()
            );
            when(pettyCashLoadByRefTypeRepository.loadPettyCashFromGrn(1L, 2L, 3L, "2024-01-01", "123"))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGrnDto>builder()
                            .responseList(expected).build());

            PettyRefTypeResponse<PettyCashFromGrnDto> result = service.loadPettyCashFromGrn(1L, 2L, 3L, "2024-01-01", "123");

            assertEquals(1, result.getResponseList().size());
            assertEquals(new BigDecimal("200"), result.getResponseList().get(0).getGrandTotal());
        }

        @Test
        @DisplayName("emptyResult_returnsEmptyList")
        void emptyResult_returnsEmptyList() {
            when(pettyCashLoadByRefTypeRepository.loadPettyCashFromGrn(1L, 2L, 3L, "2024-01-01", "999"))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGrnDto>builder()
                            .responseList(Collections.emptyList()).build());

            PettyRefTypeResponse<PettyCashFromGrnDto> result = service.loadPettyCashFromGrn(1L, 2L, 3L, "2024-01-01", "999");

            assertTrue(result.getResponseList().isEmpty());
        }

        @Test
        @DisplayName("errorStatus_throwsException")
        void errorStatus_throwsException() {
            when(pettyCashLoadByRefTypeRepository.loadPettyCashFromGrn(1L, 2L, 3L, "2024-01-01", "bad"))
                    .thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class,
                    () -> service.loadPettyCashFromGrn(1L, 2L, 3L, "2024-01-01", "bad"));
        }
    }

    // ===================================================================
    // 16. loadPettyCashFromCompletedPo
    // ===================================================================
    @Nested
    @DisplayName("LoadFromCompletedPoTests")
    class LoadFromCompletedPoTests {

        @Test
        @DisplayName("validPo_returnsItemRows")
        void validPo_returnsItemRows() {
            List<PettyCashFromGenrlPoDto> expected = List.of(
                    PettyCashFromGenrlPoDto.builder().stockPoid(10L).total(new BigDecimal("500")).build()
            );
            when(pettyCashLoadByRefTypeRepository.loadPettyCashFromCompletedPo(1L, 2L, 3L, "PO-456"))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGenrlPoDto>builder()
                            .responseList(expected).build());

            PettyRefTypeResponse<PettyCashFromGenrlPoDto> result = service.loadPettyCashFromCompletedPo(1L, 2L, 3L, "PO-456");

            assertEquals(1, result.getResponseList().size());
            assertEquals(new BigDecimal("500"), result.getResponseList().get(0).getTotal());
        }

        @Test
        @DisplayName("emptyResult_returnsEmptyList")
        void emptyResult_returnsEmptyList() {
            when(pettyCashLoadByRefTypeRepository.loadPettyCashFromCompletedPo(1L, 2L, 3L, "PO-000"))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGenrlPoDto>builder()
                            .responseList(Collections.emptyList()).build());

            PettyRefTypeResponse<PettyCashFromGenrlPoDto> result = service.loadPettyCashFromCompletedPo(1L, 2L, 3L, "PO-000");

            assertTrue(result.getResponseList().isEmpty());
        }

        @Test
        @DisplayName("errorStatus_throwsException")
        void errorStatus_throwsException() {
            when(pettyCashLoadByRefTypeRepository.loadPettyCashFromCompletedPo(1L, 2L, 3L, "BAD"))
                    .thenThrow(new RuntimeException("Proc failed"));

            assertThrows(RuntimeException.class,
                    () -> service.loadPettyCashFromCompletedPo(1L, 2L, 3L, "BAD"));
        }
    }

    // ===================================================================
    // 17. getPettyCashGlobalParams
    // ===================================================================
    @Nested
    @DisplayName("GlobalParamsTests")
    class GlobalParamsTests {

        private void stubParam(String name, String value) {
            when(globalParameterService.getParameterValue(eq(name), anyString(), anyString(), anyString()))
                    .thenReturn(value);
        }

        @Test
        @DisplayName("allParamsPresent_mapsCorrectly")
        void allParamsPresent_mapsCorrectly() {
            stubParam("PETTY_CASH_DEFAULT_PAYING_TO", "Petty Cash Fund");
            stubParam("DEFAULT_PETTY_CASH_REF_TYPE", "GENERAL");
            stubParam("PETTY_CASH_GL_VAT_RELATED_FIELDS", "TRUE");
            stubParam("ROUNDING_LIMIT", "5.00");
            stubParam("PETTY_CASH_VAT_AMOUNT_LIMIT", "1000.00");
            stubParam("INPUT_TAX_VARIANCE_LIMIT", "10.00");
            stubParam("MTA_PETTY_CASH_GL_CODE", "PC-MTA");
            stubParam("PETTY_CASH_ADVANCE_LEDGER", "2001");
            stubParam("PETTY_CASH_LEDGER", "2002");
            stubParam("PETTY_CASH_ADV_REFND_APPR_SUBMN", "Y");

            PettyCashGlobalParamsDto result = service.getPettyCashGlobalParams(null);

            assertEquals("Petty Cash Fund", result.getDefaultPayingTo());
            assertEquals("GENERAL", result.getDefaultRefType());
            assertTrue(result.isVatRelatedFieldsVisible());
            assertEquals(new BigDecimal("5.00"), result.getRoundingLimit());
            assertEquals(new BigDecimal("1000.00"), result.getVatAmountLimit());
            assertEquals(new BigDecimal("10.00"), result.getInputTaxVarianceLimit());
            assertEquals("PC-MTA", result.getMtaPettyCashGlCode());
            assertEquals(2001L, result.getAdvanceLedgerGlPoid());
            assertEquals(2002L, result.getPettyCashLedgerGlPoid());
            assertTrue(result.isAdvRefundAutoApproval());
        }

        @Test
        @DisplayName("vatRelatedFieldsFalse_whenNotTrue")
        void vatRelatedFieldsFalse_whenNotTrue() {
            stubParam("PETTY_CASH_DEFAULT_PAYING_TO", "");
            stubParam("DEFAULT_PETTY_CASH_REF_TYPE", "GENERAL");
            stubParam("PETTY_CASH_GL_VAT_RELATED_FIELDS", "FALSE");
            stubParam("ROUNDING_LIMIT", "0");
            stubParam("PETTY_CASH_VAT_AMOUNT_LIMIT", "0");
            stubParam("INPUT_TAX_VARIANCE_LIMIT", "0");
            stubParam("MTA_PETTY_CASH_GL_CODE", "");
            stubParam("PETTY_CASH_ADVANCE_LEDGER", "0");
            stubParam("PETTY_CASH_LEDGER", "0");
            stubParam("PETTY_CASH_ADV_REFND_APPR_SUBMN", "N");

            PettyCashGlobalParamsDto result = service.getPettyCashGlobalParams(null);

            assertFalse(result.isVatRelatedFieldsVisible());
            assertFalse(result.isAdvRefundAutoApproval());
        }

        @Test
        @DisplayName("emptyLedgerValues_returnNullLongs")
        void emptyLedgerValues_returnNullLongs() {
            stubParam("PETTY_CASH_DEFAULT_PAYING_TO", "");
            stubParam("DEFAULT_PETTY_CASH_REF_TYPE", "");
            stubParam("PETTY_CASH_GL_VAT_RELATED_FIELDS", "");
            stubParam("ROUNDING_LIMIT", "0");
            stubParam("PETTY_CASH_VAT_AMOUNT_LIMIT", "0");
            stubParam("INPUT_TAX_VARIANCE_LIMIT", "0");
            stubParam("MTA_PETTY_CASH_GL_CODE", "");
            stubParam("PETTY_CASH_ADVANCE_LEDGER", "");
            stubParam("PETTY_CASH_LEDGER", "");
            stubParam("PETTY_CASH_ADV_REFND_APPR_SUBMN", "");

            PettyCashGlobalParamsDto result = service.getPettyCashGlobalParams(null);

            assertNull(result.getAdvanceLedgerGlPoid());
            assertNull(result.getPettyCashLedgerGlPoid());
        }

        @Test
        @DisplayName("withPettyCashGlPoid_usesGlPoidAsLedgerKey")
        void withPettyCashGlPoid_usesGlPoidAsLedgerKey() {
            stubParam("PETTY_CASH_DEFAULT_PAYING_TO", "");
            stubParam("DEFAULT_PETTY_CASH_REF_TYPE", "");
            stubParam("PETTY_CASH_GL_VAT_RELATED_FIELDS", "FALSE");
            stubParam("ROUNDING_LIMIT", "0");
            stubParam("PETTY_CASH_VAT_AMOUNT_LIMIT", "0");
            stubParam("INPUT_TAX_VARIANCE_LIMIT", "0");
            stubParam("MTA_PETTY_CASH_GL_CODE", "");
            stubParam("PETTY_CASH_ADV_REFND_APPR_SUBMN", "N");

            when(globalParameterService.getParameterValue(
                    eq("PETTY_CASH_ADVANCE_LEDGER"), anyString(), eq("5001"), anyString()))
                    .thenReturn("3001");
            when(globalParameterService.getParameterValue(
                    eq("PETTY_CASH_LEDGER"), anyString(), eq("5001"), anyString()))
                    .thenReturn("3002");

            PettyCashGlobalParamsDto result = service.getPettyCashGlobalParams(5001L);

            assertEquals(3001L, result.getAdvanceLedgerGlPoid());
            assertEquals(3002L, result.getPettyCashLedgerGlPoid());
        }
    }

    // =========================================================================
    // FF / FDA Ref LOV Enrichment Tests
    // =========================================================================
    @Nested
    @DisplayName("Header LOV enrichment in mapToResponseDto")
    class RefLovEnrichmentTests {

        private LovGetListDto ffLov() {
            LovGetListDto lov = new LovGetListDto();
            lov.setPoid(7001L);
            lov.setCode("FF-2024-00789");
            lov.setLabel("FF Job — Sea Freight Singapore");
            lov.setValue(7001L);
            lov.setDescription("Sea Freight Singapore");
            lov.setSeqNo(1);
            return lov;
        }

        private LovGetListDto fdaLov() {
            LovGetListDto lov = new LovGetListDto();
            lov.setPoid(8001L);
            lov.setCode("FDA-2024-00456");
            lov.setLabel("FDA — Final Delivery KL");
            lov.setValue(8001L);
            lov.setDescription("Final Delivery KL");
            lov.setSeqNo(2);
            return lov;
        }

        private LovGetListDto mtaLov() {
            LovGetListDto lov = new LovGetListDto();
            lov.setPoid(9001L);
            lov.setCode("RFQ-2024-00111");
            lov.setLabel("MTA RFQ — Port Klang");
            lov.setValue(9001L);
            lov.setDescription("Port Klang");
            lov.setSeqNo(3);
            return lov;
        }

        private com.asg.finance.entity.GlPettyCashPaymentHdr emptyHdr() {
            com.asg.finance.entity.GlPettyCashPaymentHdr hdr =
                    mock(com.asg.finance.entity.GlPettyCashPaymentHdr.class);
            when(hdr.getFfRef()).thenReturn(null);
            when(hdr.getFdaRef()).thenReturn(null);
            when(hdr.getSalesQtnRef()).thenReturn(null);
            when(hdr.getMtaRef()).thenReturn(null);
            when(hdr.getPettyCashGlPoid()).thenReturn(null);
            when(hdr.getGrnSupplierPoid()).thenReturn(null);
            when(hdr.getSupplierGlPoid()).thenReturn(null);
            when(hdr.getCustomerGlPoid()).thenReturn(null);
            when(hdr.getAdvancePettyCashPoid()).thenReturn(null);
            return hdr;
        }

        @Test
        @DisplayName("ffRef numeric poid populates ffRefDtl via FF_JOBS_FOR_COST_BOOKING")
        void ffRefNumericPoid_populatesDetails() {
            com.asg.finance.entity.GlPettyCashPaymentHdr hdr = emptyHdr();
            when(hdr.getFfRef()).thenReturn("7001");

            when(lovService.getDetailsByPoidAndLovName(7001L, "FF_JOBS_FOR_COST_BOOKING"))
                    .thenReturn(ffLov());

            PettyCashResponseDto result = (PettyCashResponseDto) ReflectionTestUtils.invokeMethod(
                    service, "mapToResponseDto", hdr,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());

            assertNotNull(result.getFfRefDtl());
            assertEquals(7001L, result.getFfRefDtl().poid());
            assertEquals("FF-2024-00789", result.getFfRefDtl().code());
            assertEquals("FF Job — Sea Freight Singapore", result.getFfRefDtl().label());
        }

        @Test
        @DisplayName("fdaRef numeric poid populates fdaRefDtl via PROCESS_FDA_IN_PI")
        void fdaRefNumericPoid_populatesDetails() {
            com.asg.finance.entity.GlPettyCashPaymentHdr hdr = emptyHdr();
            when(hdr.getFdaRef()).thenReturn("8001");

            when(lovService.getDetailsByPoidAndLovName(8001L, "PROCESS_FDA_IN_PI"))
                    .thenReturn(fdaLov());

            PettyCashResponseDto result = (PettyCashResponseDto) ReflectionTestUtils.invokeMethod(
                    service, "mapToResponseDto", hdr,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());

            assertNotNull(result.getFdaRefDtl());
            assertEquals(8001L, result.getFdaRefDtl().poid());
            assertEquals("FDA-2024-00456", result.getFdaRefDtl().code());
            assertEquals("FDA — Final Delivery KL", result.getFdaRefDtl().label());
        }

        @Test
        @DisplayName("salesQtnRef numeric poid populates salesQtnRefDtl via PETTY_MTA_BASED_RFQ")
        void salesQtnRefNumericPoid_populatesDetails() {
            com.asg.finance.entity.GlPettyCashPaymentHdr hdr = emptyHdr();
            when(hdr.getSalesQtnRef()).thenReturn("9001");

            when(lovService.getDetailsByPoidAndLovName(9001L, "PETTY_MTA_BASED_RFQ"))
                    .thenReturn(mtaLov());

            PettyCashResponseDto result = (PettyCashResponseDto) ReflectionTestUtils.invokeMethod(
                    service, "mapToResponseDto", hdr,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());

            assertNotNull(result.getSalesQtnRefDtl());
            assertEquals(9001L, result.getSalesQtnRefDtl().poid());
            assertEquals("RFQ-2024-00111", result.getSalesQtnRefDtl().code());
            assertEquals("MTA RFQ — Port Klang", result.getSalesQtnRefDtl().label());
        }

        @Test
        @DisplayName("non-numeric ffRef skips enrichment — no exception thrown")
        void ffRefNonNumeric_skipsEnrichment() {
            com.asg.finance.entity.GlPettyCashPaymentHdr hdr = emptyHdr();
            when(hdr.getFfRef()).thenReturn("FF-DOC-STRING");

            PettyCashResponseDto result = (PettyCashResponseDto) ReflectionTestUtils.invokeMethod(
                    service, "mapToResponseDto", hdr,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());

            assertNull(result.getFfRefDtl());
        }

        @Test
        @DisplayName("null refs — all details remain null")
        void nullRefs_noEnrichment() {
            com.asg.finance.entity.GlPettyCashPaymentHdr hdr = emptyHdr();

            PettyCashResponseDto result = (PettyCashResponseDto) ReflectionTestUtils.invokeMethod(
                    service, "mapToResponseDto", hdr,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());

            assertNull(result.getFfRefDtl());
            assertNull(result.getFdaRefDtl());
            assertNull(result.getSalesQtnRefDtl());
        }
    }

    // =========================================================================
    // Child table LOV enrichment tests
    // =========================================================================
    @Nested
    @DisplayName("Child table LOV enrichment")
    class ChildTableLovEnrichmentTests {

        private LovGetListDto lov(Long poid, String code, String label) {
            LovGetListDto l = new LovGetListDto();
            l.setPoid(poid); l.setCode(code); l.setLabel(label);
            l.setValue(poid); l.setDescription(label); l.setSeqNo(1);
            return l;
        }

        // ── Gap 1: companyPoidDtl in GL Detail ──────────────────────────────

        @Test
        @DisplayName("mapPaymentResponse: companyPoid populates companyPoidDtl via COMPANY LOV")
        void paymentDtl_companyPoid_populatesDetails() {
            com.asg.finance.entity.GlPettyCashPaymentDtl dtl =
                    mock(com.asg.finance.entity.GlPettyCashPaymentDtl.class);
            when(dtl.getCompanyPoid()).thenReturn(2001L);
            when(dtl.getGlMaster()).thenReturn(null);
            when(dtl.getChargeMaster()).thenReturn(null);
            when(dtl.getTaxPoid()).thenReturn(null);
            when(dtl.getVatSupplier()).thenReturn(null);

            when(lovService.getDetailsByPoidAndLovName(2001L, "COMPANY"))
                    .thenReturn(lov(2001L, "COMP-A", "Company Alpha"));

            List<GlPettyCashPaymentDtlResponseDto> result =
                    (List<GlPettyCashPaymentDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapPaymentResponse", List.of(dtl));

            assertNotNull(result);
            DetailsDto companyDtl = result.get(0).getCompanyPoidDtl();
            assertNotNull(companyDtl);
            assertEquals(2001L, companyDtl.poid());
            assertEquals("COMP-A", companyDtl.code());
            assertEquals("Company Alpha", companyDtl.label());
        }

        @Test
        @DisplayName("mapPaymentResponse: null companyPoid leaves companyPoidDtl null")
        void paymentDtl_nullCompanyPoid_noEnrichment() {
            com.asg.finance.entity.GlPettyCashPaymentDtl dtl =
                    mock(com.asg.finance.entity.GlPettyCashPaymentDtl.class);
            when(dtl.getCompanyPoid()).thenReturn(null);
            when(dtl.getGlMaster()).thenReturn(null);
            when(dtl.getChargeMaster()).thenReturn(null);
            when(dtl.getTaxPoid()).thenReturn(null);
            when(dtl.getVatSupplier()).thenReturn(null);

            List<GlPettyCashPaymentDtlResponseDto> result =
                    (List<GlPettyCashPaymentDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapPaymentResponse", List.of(dtl));

            assertNull(result.get(0).getCompanyPoidDtl());
        }

        // ── Gap 2: refDocPoidDtl in Charge Detail (FF only) ─────────────────

        @Test
        @DisplayName("mapChargeResponse: FF chargeFrom + refDocPoid populates refDocPoidDtl via FF_JOBNO")
        void chargeDtl_ffChargeFrom_refDocPoidEnriched() {
            com.asg.finance.entity.GlPettyCashChargeDtl dtl =
                    mock(com.asg.finance.entity.GlPettyCashChargeDtl.class);
            when(dtl.getChargeFrom()).thenReturn("FF");
            when(dtl.getRefDocPoid()).thenReturn(9001L);
            when(dtl.getChargePoid()).thenReturn(null);
            when(dtl.getTaxPoid()).thenReturn(null);

            when(lovService.getDetailsByPoidAndLovName(9001L, "FF_JOBNO"))
                    .thenReturn(lov(9001L, "FF-2024-00789", "Singapore Sea Freight"));

            List<GlPettyCashChargeDtlResponseDto> result =
                    (List<GlPettyCashChargeDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapChargeResponse", List.of(dtl));

            DetailsDto refDtl = result.get(0).getRefDocPoidDtl();
            assertNotNull(refDtl);
            assertEquals(9001L, refDtl.poid());
            assertEquals("FF-2024-00789", refDtl.code());
            assertEquals("Singapore Sea Freight", refDtl.label());
        }

        @Test
        @DisplayName("mapChargeResponse: FDA chargeFrom skips refDocPoidDtl enrichment")
        void chargeDtl_fdaChargeFrom_refDocPoidNotEnriched() {
            com.asg.finance.entity.GlPettyCashChargeDtl dtl =
                    mock(com.asg.finance.entity.GlPettyCashChargeDtl.class);
            when(dtl.getChargeFrom()).thenReturn("FDA");
            when(dtl.getRefDocPoid()).thenReturn(9002L);
            when(dtl.getChargePoid()).thenReturn(null);
            when(dtl.getTaxPoid()).thenReturn(null);

            List<GlPettyCashChargeDtlResponseDto> result =
                    (List<GlPettyCashChargeDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapChargeResponse", List.of(dtl));

            assertNull(result.get(0).getRefDocPoidDtl());
        }

        @Test
        @DisplayName("mapChargeResponse: FF chargeFrom with null refDocPoid leaves refDocPoidDtl null")
        void chargeDtl_ffChargeFrom_nullRefDocPoid_noEnrichment() {
            com.asg.finance.entity.GlPettyCashChargeDtl dtl =
                    mock(com.asg.finance.entity.GlPettyCashChargeDtl.class);
            when(dtl.getChargeFrom()).thenReturn("FF");
            when(dtl.getRefDocPoid()).thenReturn(null);
            when(dtl.getChargePoid()).thenReturn(null);
            when(dtl.getTaxPoid()).thenReturn(null);

            List<GlPettyCashChargeDtlResponseDto> result =
                    (List<GlPettyCashChargeDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapChargeResponse", List.of(dtl));

            assertNull(result.get(0).getRefDocPoidDtl());
        }

    }

    // =========================================================================
    // Merge helpers and broader response mapping coverage
    // =========================================================================
    @Nested
    @DisplayName("Merge helpers and response mapping coverage")
    class MergeAndMappingCoverageTests {

        private LovGetListDto lov(Long poid, String code, String label) {
            LovGetListDto lov = new LovGetListDto();
            lov.setPoid(poid);
            lov.setCode(code);
            lov.setLabel(label);
            lov.setValue(poid);
            lov.setDescription(label);
            lov.setSeqNo(1);
            return lov;
        }

        private GlPettyCashPaymentDtl paymentEntity(long detRowId, String remarks) {
            return GlPettyCashPaymentDtl.builder()
                    .transactionPoid(100L)
                    .detRowId(detRowId)
                    .remarks(remarks)
                    .build();
        }

        private GlPettyCashChargeDtl chargeEntity(long detRowId, String remarks) {
            return GlPettyCashChargeDtl.builder()
                    .transactionPoid(200L)
                    .detRowId(detRowId)
                    .remarks(remarks)
                    .build();
        }

        private GLPettyCashItemDtl itemEntity(long detRowId, String remarks) {
            return GLPettyCashItemDtl.builder()
                    .transactionPoid(300L)
                    .detRowId(detRowId)
                    .remarks(remarks)
                    .build();
        }

        private GlPettyCashPaymentGrnDtl grnEntity(long detRowId, BigDecimal amount) {
            return GlPettyCashPaymentGrnDtl.builder()
                    .transactionPoid(400L)
                    .detRowId(detRowId)
                    .amount(amount)
                    .build();
        }

        @Test
        @DisplayName("filterGrnCheckAll excludes rows flagged with checkAll=N")
        void filterGrnCheckAll_excludesNRows() {
            List<GlPettyCashPaymentGrnDtlRequestDto> request = List.of(
                    GlPettyCashPaymentGrnDtlRequestDto.builder().detRowId(1L).checkAll("Y").build(),
                    GlPettyCashPaymentGrnDtlRequestDto.builder().detRowId(2L).checkAll("N").build(),
                    GlPettyCashPaymentGrnDtlRequestDto.builder().detRowId(3L).checkAll(null).build()
            );

            @SuppressWarnings("unchecked")
            List<GlPettyCashPaymentGrnDtlRequestDto> filtered =
                    (List<GlPettyCashPaymentGrnDtlRequestDto>) ReflectionTestUtils.invokeMethod(
                            service, "filterGrnCheckAll", request);

            assertEquals(2, filtered.size());
            assertEquals(1L, filtered.get(0).getDetRowId());
            assertEquals(3L, filtered.get(1).getDetRowId());
        }

        @Test
        @DisplayName("mapGrnDtls skips deleted rows and auto-generates missing detRowId values")
        void mapGrnDtls_autoGeneratesRowIds() {
            List<GlPettyCashPaymentGrnDtlRequestDto> request = List.of(
                    GlPettyCashPaymentGrnDtlRequestDto.builder()
                            .detRowId(null)
                            .grnPoid(9001L)
                            .amount(new BigDecimal("10"))
                            .actionType("isCreated")
                            .build(),
                    GlPettyCashPaymentGrnDtlRequestDto.builder()
                            .detRowId(7L)
                            .grnPoid(9002L)
                            .amount(new BigDecimal("11"))
                            .actionType("isDeleted")
                            .build()
            );

            @SuppressWarnings("unchecked")
            List<GlPettyCashPaymentGrnDtl> result =
                    (List<GlPettyCashPaymentGrnDtl>) ReflectionTestUtils.invokeMethod(
                            service, "mapGrnDtls", request, 77L);

            assertEquals(1, result.size());
            assertEquals(77L, result.get(0).getTransactionPoid());
            assertEquals(1L, result.get(0).getDetRowId());
            assertEquals(9001L, result.get(0).getGrnPoid());
        }

        @Test
        @DisplayName("mergePaymentDtls handles create, update, delete and noChanges rows")
        void mergePaymentDtls_handlesAllActions() {
            List<GlPettyCashPaymentDtl> existing = List.of(
                    paymentEntity(1L, "old-1"),
                    paymentEntity(2L, "old-2"),
                    paymentEntity(3L, "old-3")
            );

            List<GlPettyCashPaymentDtlRequestDto> request = List.of(
                    GlPettyCashPaymentDtlRequestDto.builder()
                            .actionType("isCreated")
                            .glPoid(101L)
                            .chargePoid(201L)
                            .type("NEW")
                            .remarks("created")
                            .build(),
                    GlPettyCashPaymentDtlRequestDto.builder()
                            .actionType("isUpdated")
                            .detRowId(1L)
                            .type("UPDATED")
                            .remarks("updated")
                            .build(),
                    GlPettyCashPaymentDtlRequestDto.builder()
                            .actionType("isDeleted")
                            .detRowId(2L)
                            .build(),
                    GlPettyCashPaymentDtlRequestDto.builder()
                            .actionType(null)
                            .detRowId(3L)
                            .build()
            );

            when(glPettyCashPaymentDtlRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            @SuppressWarnings("unchecked")
            List<GlPettyCashPaymentDtl> result =
                    (List<GlPettyCashPaymentDtl>) ReflectionTestUtils.invokeMethod(
                            service, "mergePaymentDtls", existing, request, 500L);

            assertEquals(3, result.size());
            assertEquals("UPDATED", result.stream()
                    .filter(row -> Long.valueOf(1L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow().getType());
            GlPettyCashPaymentDtl created = result.stream()
                    .filter(row -> Long.valueOf(4L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow();
            assertEquals("NEW", created.getType());
            assertNotNull(created.getGlMaster());
            assertNotNull(created.getChargeMaster());

            verify(glPettyCashPaymentDtlRepository).deleteAll(anyList());
            verify(loggingService).logDelete(any(), any(), any());
            verify(loggingService).createLogBatch(any());
            verify(loggingService).createLogSummaryEntry(
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any());
        }

        @Test
        @DisplayName("mergeChargeDtls handles create, update, delete and noChanges rows")
        void mergeChargeDtls_handlesAllActions() {
            List<GlPettyCashChargeDtl> existing = List.of(
                    chargeEntity(1L, "old-1"),
                    chargeEntity(2L, "old-2"),
                    chargeEntity(3L, "old-3")
            );

            PettyCashCreateRequestDto request = PettyCashCreateRequestDto.builder()
                    .refType("FF JOBS")
                    .glPettyCashChargeDtlRequestDtos(List.of(
                            GlPettyCashChargeDtlRequestDto.builder()
                                    .actionType("isCreated")
                                    .chargeAmount(new BigDecimal("10"))
                                    .remarks("created")
                                    .build(),
                            GlPettyCashChargeDtlRequestDto.builder()
                                    .actionType("isUpdated")
                                    .detRowId(1L)
                                    .chargeAmount(new BigDecimal("20"))
                                    .remarks("updated")
                                    .build(),
                            GlPettyCashChargeDtlRequestDto.builder()
                                    .actionType("isDeleted")
                                    .detRowId(2L)
                                    .build(),
                            GlPettyCashChargeDtlRequestDto.builder()
                                    .actionType(null)
                                    .detRowId(3L)
                                    .build()
                    ))
                    .build();

            when(glPettyCashChargeDtlRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            @SuppressWarnings("unchecked")
            List<GlPettyCashChargeDtl> result =
                    (List<GlPettyCashChargeDtl>) ReflectionTestUtils.invokeMethod(
                            service, "mergeChargeDtls", existing, request, 600L);

            assertEquals(3, result.size());
            assertEquals(new BigDecimal("20"), result.stream()
                    .filter(row -> Long.valueOf(1L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow().getChargeAmount());
            GlPettyCashChargeDtl created = result.stream()
                    .filter(row -> Long.valueOf(4L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow();
            assertEquals("FF", created.getChargeFrom());
            assertEquals("created", created.getRemarks());

            verify(glPettyCashChargeDtlRepository).deleteAll(anyList());
            verify(loggingService).logDelete(any(), any(), any());
            verify(loggingService).createLogBatch(any());
            verify(loggingService).createLogSummaryEntry(
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any());
        }

        @Test
        @DisplayName("mergeItemDtls handles create, update, delete and noChanges rows")
        void mergeItemDtls_handlesAllActions() {
            List<GLPettyCashItemDtl> existing = List.of(
                    itemEntity(1L, "old-1"),
                    itemEntity(2L, "old-2"),
                    itemEntity(3L, "old-3")
            );

            PettyCashCreateRequestDto request = PettyCashCreateRequestDto.builder()
                    .refType("GENERAL PO")
                    .glPettyCashItemDtlRequestDtos(List.of(
                            GlPettyCashItemDtlRequestDto.builder()
                                    .actionType("isCreated")
                                    .stockPoid(501L)
                                    .stockUnitPoid(601L)
                                    .total(new BigDecimal("10"))
                                    .remarks("created")
                                    .build(),
                            GlPettyCashItemDtlRequestDto.builder()
                                    .actionType("isUpdated")
                                    .detRowId(1L)
                                    .total(new BigDecimal("20"))
                                    .remarks("updated")
                                    .build(),
                            GlPettyCashItemDtlRequestDto.builder()
                                    .actionType("isDeleted")
                                    .detRowId(2L)
                                    .build(),
                            GlPettyCashItemDtlRequestDto.builder()
                                    .actionType(null)
                                    .detRowId(3L)
                                    .build()
                    ))
                    .build();

            when(glPettyCashItemDtlRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            @SuppressWarnings("unchecked")
            List<GLPettyCashItemDtl> result =
                    (List<GLPettyCashItemDtl>) ReflectionTestUtils.invokeMethod(
                            service, "mergeItemDtls", existing, request, 700L);

            assertEquals(3, result.size());
            assertEquals(new BigDecimal("20"), result.stream()
                    .filter(row -> Long.valueOf(1L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow().getTotal());
            GLPettyCashItemDtl created = result.stream()
                    .filter(row -> Long.valueOf(4L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow();
            assertEquals(501L, created.getStockPoid());
            assertEquals(601L, created.getStockUnitPoid());

            verify(glPettyCashItemDtlRepository).deleteAll(anyList());
            verify(loggingService).logDelete(any(), any(), any());
            verify(loggingService).createLogBatch(any());
            verify(loggingService).createLogSummaryEntry(
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any());
        }

        @Test
        @DisplayName("mergeGrnDtls handles create, update, delete and noChanges rows")
        void mergeGrnDtls_handlesAllActions() {
            List<GlPettyCashPaymentGrnDtl> existing = List.of(
                    grnEntity(1L, new BigDecimal("1")),
                    grnEntity(2L, new BigDecimal("2")),
                    grnEntity(3L, new BigDecimal("3"))
            );

            PettyCashCreateRequestDto request = PettyCashCreateRequestDto.builder()
                    .refType("GRN_JOBS")
                    .glPettyCashGrnDtlRequestDtos(List.of(
                            GlPettyCashPaymentGrnDtlRequestDto.builder()
                                    .actionType("isCreated")
                                    .grnPoid(801L)
                                    .amount(new BigDecimal("10"))
                                    .build(),
                            GlPettyCashPaymentGrnDtlRequestDto.builder()
                                    .actionType("isUpdated")
                                    .detRowId(1L)
                                    .grnPoid(802L)
                                    .amount(new BigDecimal("20"))
                                    .build(),
                            GlPettyCashPaymentGrnDtlRequestDto.builder()
                                    .actionType("isDeleted")
                                    .detRowId(2L)
                                    .build(),
                            GlPettyCashPaymentGrnDtlRequestDto.builder()
                                    .actionType(null)
                                    .detRowId(3L)
                                    .build()
                    ))
                    .build();

            when(glPettyCashPaymentGrnDtlRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            @SuppressWarnings("unchecked")
            List<GlPettyCashPaymentGrnDtl> result =
                    (List<GlPettyCashPaymentGrnDtl>) ReflectionTestUtils.invokeMethod(
                            service, "mergeGrnDtls", existing, request, 800L);

            assertEquals(3, result.size());
            assertEquals(new BigDecimal("20"), result.stream()
                    .filter(row -> Long.valueOf(1L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow().getAmount());
            GlPettyCashPaymentGrnDtl created = result.stream()
                    .filter(row -> Long.valueOf(4L).equals(row.getDetRowId()))
                    .findFirst().orElseThrow();
            assertEquals(801L, created.getGrnPoid());
            assertEquals(new BigDecimal("10"), created.getAmount());

            verify(glPettyCashPaymentGrnDtlRepository).deleteAll(anyList());
            verify(loggingService).logDelete(any(), any(), any());
            verify(loggingService).createLogBatch(any());
            verify(loggingService).createLogSummaryEntry(
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any(),
                    org.mockito.ArgumentMatchers.<String>any());
        }

        @Test
        @DisplayName("mapToResponseDto populates header-level linked details")
        void mapToResponseDto_populatesHeaderLinkedDetails() {
            com.asg.finance.entity.GlPettyCashPaymentHdr hdr =
                    mock(com.asg.finance.entity.GlPettyCashPaymentHdr.class);
            when(hdr.getTransactionPoid()).thenReturn(900L);
            when(hdr.getDocRef()).thenReturn("DOC-900");
            when(hdr.getTransactionDate()).thenReturn(LocalDate.of(2025, 1, 15));
            when(hdr.getGroupPoid()).thenReturn(10L);
            when(hdr.getCompanyPoid()).thenReturn(20L);
            when(hdr.getCurrencyCode()).thenReturn("AED");
            when(hdr.getCurrencyRate()).thenReturn(new BigDecimal("3.672"));
            when(hdr.getPettyCashGlPoid()).thenReturn(101L);
            when(hdr.getGrnSupplierPoid()).thenReturn(202L);
            when(hdr.getSupplierGlPoid()).thenReturn(303L);
            when(hdr.getCustomerGlPoid()).thenReturn(404L);
            when(hdr.getAdvancePettyCashPoid()).thenReturn(505L);
            when(hdr.getFfRef()).thenReturn(null);
            when(hdr.getFdaRef()).thenReturn(null);
            when(hdr.getSalesQtnRef()).thenReturn(null);

            GLMaster pettyCashGl = GLMaster.builder()
                    .glPoid(101L)
                    .glCode("GL-101")
                    .glDescription("Petty cash ledger")
                    .groupPoid(10L)
                    .glDescription2("PC")
                    .seqno(1)
                    .build();
            GLMaster supplierGl = GLMaster.builder()
                    .glPoid(303L)
                    .glCode("GL-303")
                    .glDescription("Supplier ledger")
                    .groupPoid(10L)
                    .glDescription2("SUP")
                    .seqno(2)
                    .build();
            GLMaster customerGl = GLMaster.builder()
                    .glPoid(404L)
                    .glCode("GL-404")
                    .glDescription("Customer ledger")
                    .groupPoid(10L)
                    .glDescription2("CUS")
                    .seqno(3)
                    .build();

            com.asg.finance.entity.SupplierMasterEntity supplier =
                    mock(com.asg.finance.entity.SupplierMasterEntity.class);
            when(supplier.getSupplierPoid()).thenReturn(202L);
            when(supplier.getSupplierCode()).thenReturn("SUP-202");
            when(supplier.getSupplierName()).thenReturn("Supplier");
            when(supplier.getGroupPoid()).thenReturn(10L);
            when(supplier.getSupplierName2()).thenReturn("S-2");
            when(supplier.getSeqNo()).thenReturn(6L);

            com.asg.finance.entity.AdvancePettyCashHdr advance =
                    mock(com.asg.finance.entity.AdvancePettyCashHdr.class);
            when(advance.getTransactionPoid()).thenReturn(505L);
            when(advance.getDocRef()).thenReturn("ADV-505");
            when(advance.getGroupPoid()).thenReturn(10L);

            when(glMasterRepository.findByGlPoid(101L)).thenReturn(java.util.Optional.of(pettyCashGl));
            when(glMasterRepository.findByGlPoid(303L)).thenReturn(java.util.Optional.of(supplierGl));
            when(glMasterRepository.findByGlPoid(404L)).thenReturn(java.util.Optional.of(customerGl));
            when(supplierMasterRepository.findBySupplierPoid(202L)).thenReturn(supplier);
            when(advancePettyCashHdrRepository.findByTransactionPoid(505L))
                    .thenReturn(java.util.Optional.of(advance));

            PettyCashResponseDto response = (PettyCashResponseDto) ReflectionTestUtils.invokeMethod(
                    service, "mapToResponseDto", hdr,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

            assertEquals("GL-101", response.getPettyCashGlPoidDtl().code());
            assertEquals("SUP-202", response.getGrnSupplierPoidDtl().code());
            assertEquals("GL-303", response.getSupplierGlPoidDtl().code());
            assertEquals("GL-404", response.getCustomerGlPoidDtl().code());
            assertEquals("ADV-505", response.getAdvancePettyCashPoidDtl().code());
        }

        @Test
        @DisplayName("mapPaymentResponse populates GL, charge, tax, VAT supplier and company lookups")
        void mapPaymentResponse_populatesAllLookups() {
            GlPettyCashPaymentDtl dtl = GlPettyCashPaymentDtl.builder()
                    .transactionPoid(111L)
                    .detRowId(1L)
                    .companyPoid(88L)
                    .drAmt(new BigDecimal("10"))
                    .crAmt(BigDecimal.ZERO)
                    .vatAmount(new BigDecimal("1.500"))
                    .totalAmount(new BigDecimal("11.500"))
                    .vatSupplier(66L)
                    .inputVatNumber("VAT-1")
                    .supplierInvDate(LocalDate.of(2025, 1, 10))
                    .taxPoid(55L)
                    .taxPercentage(new BigDecimal("15"))
                    .vatPartyName("Party")
                    .build();
            dtl.setGlMaster(GLMaster.builder().glPoid(101L).build());
            dtl.setChargeMaster(com.asg.finance.entity.master.ShipChargeEntity.builder().chargePoid(201L).build());

            GLMaster gl = GLMaster.builder()
                    .glPoid(101L)
                    .glCode("GL-101")
                    .glDescription("GL")
                    .groupPoid(10L)
                    .glDescription2("L1")
                    .seqno(1)
                    .build();
            com.asg.finance.entity.master.ShipChargeEntity charge =
                    com.asg.finance.entity.master.ShipChargeEntity.builder()
                            .chargePoid(201L)
                            .chargeCode("CH-201")
                            .chargeName("Charge")
                            .groupPoid(10L)
                            .chargeName2("C1")
                            .seqNo(2)
                            .build();
            com.asg.finance.entity.TaxMaster tax = mock(com.asg.finance.entity.TaxMaster.class);
            when(tax.getTaxPoid()).thenReturn(55L);
            when(tax.getTaxCode()).thenReturn("TAX-55");
            when(tax.getTaxName()).thenReturn("VAT");
            when(tax.getGroupPoid()).thenReturn(10L);
            when(tax.getTaxName2()).thenReturn("VAT-2");
            when(tax.getSeqNo()).thenReturn(3);

            com.asg.finance.entity.SupplierMasterEntity supplier =
                    mock(com.asg.finance.entity.SupplierMasterEntity.class);
            when(supplier.getSupplierPoid()).thenReturn(66L);
            when(supplier.getSupplierCode()).thenReturn("SUP-66");
            when(supplier.getSupplierName()).thenReturn("Supplier");
            when(supplier.getGroupPoid()).thenReturn(10L);
            when(supplier.getSupplierName2()).thenReturn("S-2");
            when(supplier.getSeqNo()).thenReturn(4L);

            when(glMasterRepository.findByGlPoid(101L)).thenReturn(java.util.Optional.of(gl));
            when(shipChargeRepository.findByChargePoid(201L)).thenReturn(java.util.Optional.of(charge));
            when(taxMasterRepository.findByTaxPoid(55L)).thenReturn(java.util.Optional.of(tax));
            when(supplierMasterRepository.findBySupplierPoid(66L)).thenReturn(supplier);
            when(lovService.getDetailsByPoidAndLovName(88L, "COMPANY"))
                    .thenReturn(lov(88L, "COMP-88", "Company 88"));

            List<GlPettyCashPaymentDtlResponseDto> result =
                    (List<GlPettyCashPaymentDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapPaymentResponse", List.of(dtl));

            assertEquals("GL-101", result.get(0).getGlPoidDtl().code());
            assertEquals("CH-201", result.get(0).getChargePoidDtl().code());
            assertEquals("TAX-55", result.get(0).getTaxPoidDtl().code());
            assertEquals("SUP-66", result.get(0).getVatSupplierDtl().code());
            assertEquals("COMP-88", result.get(0).getCompanyPoidDtl().code());
        }

        @Test
        @DisplayName("mapChargeResponse populates charge, tax and FF job lookups")
        void mapChargeResponse_populatesAllLookups() {
            GlPettyCashChargeDtl dtl = GlPettyCashChargeDtl.builder()
                    .transactionPoid(222L)
                    .detRowId(1L)
                    .chargePoid(201L)
                    .chargeAmount(new BigDecimal("10"))
                    .description("Charge")
                    .refDocPoid(301L)
                    .chargeFrom("FF")
                    .taxPoid(55L)
                    .build();

            com.asg.finance.entity.master.ShipChargeEntity charge =
                    com.asg.finance.entity.master.ShipChargeEntity.builder()
                            .chargePoid(201L)
                            .chargeCode("CH-201")
                            .chargeName("Charge")
                            .groupPoid(10L)
                            .chargeName2("C1")
                            .seqNo(2)
                            .build();
            com.asg.finance.entity.TaxMaster tax = mock(com.asg.finance.entity.TaxMaster.class);
            when(tax.getTaxPoid()).thenReturn(55L);
            when(tax.getTaxCode()).thenReturn("TAX-55");
            when(tax.getTaxName()).thenReturn("VAT");
            when(tax.getGroupPoid()).thenReturn(10L);
            when(tax.getTaxName2()).thenReturn("VAT-2");
            when(tax.getSeqNo()).thenReturn(3);

            when(shipChargeRepository.findByChargePoid(201L)).thenReturn(java.util.Optional.of(charge));
            when(taxMasterRepository.findByTaxPoid(55L)).thenReturn(java.util.Optional.of(tax));
            when(lovService.getDetailsByPoidAndLovName(301L, "FF_JOBNO"))
                    .thenReturn(lov(301L, "FF-301", "FF Job 301"));

            List<GlPettyCashChargeDtlResponseDto> result =
                    (List<GlPettyCashChargeDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapChargeResponse", List.of(dtl));

            assertEquals("CH-201", result.get(0).getChargePoidDtl().code());
            assertEquals("TAX-55", result.get(0).getTaxPoidDtl().code());
            assertEquals("FF-301", result.get(0).getRefDocPoidDtl().code());
        }

        @Test
        @DisplayName("mapItemResponse populates stock, stock unit and tax lookups")
        void mapItemResponse_populatesAllLookups() {
            GLPettyCashItemDtl dtl = GLPettyCashItemDtl.builder()
                    .transactionPoid(333L)
                    .detRowId(1L)
                    .stockPoid(401L)
                    .stockUnitPoid(402L)
                    .poQty(new BigDecimal("1"))
                    .dnQty(new BigDecimal("2"))
                    .qtyReceived(new BigDecimal("3"))
                    .price(new BigDecimal("4"))
                    .discount(new BigDecimal("5"))
                    .total(new BigDecimal("6"))
                    .refDocPoid(7L)
                    .checkAll("Y")
                    .refDetRowId(8L)
                    .taxPoid(55L)
                    .build();

            com.asg.finance.entity.StockMasterEntity stock =
                    mock(com.asg.finance.entity.StockMasterEntity.class);
            when(stock.getStockPoid()).thenReturn(401L);
            when(stock.getStockCode()).thenReturn("STK-401");
            when(stock.getStockName()).thenReturn("Stock");
            when(stock.getGroupPoid()).thenReturn(10L);
            when(stock.getStockDescription()).thenReturn("Stock Desc");
            when(stock.getSeqNo()).thenReturn(1);

            com.asg.finance.entity.master.UnitMaster unit =
                    mock(com.asg.finance.entity.master.UnitMaster.class);
            when(unit.getUnitPoid()).thenReturn(402L);
            when(unit.getUnitCode()).thenReturn("UNT-402");
            when(unit.getUnitName()).thenReturn("Unit");
            when(unit.getGroupPoid()).thenReturn(10L);
            when(unit.getUnitName2()).thenReturn("Unit 2");
            when(unit.getSeqNo()).thenReturn(2);

            com.asg.finance.entity.TaxMaster tax = mock(com.asg.finance.entity.TaxMaster.class);
            when(tax.getTaxPoid()).thenReturn(55L);
            when(tax.getTaxCode()).thenReturn("TAX-55");
            when(tax.getTaxName()).thenReturn("VAT");
            when(tax.getGroupPoid()).thenReturn(10L);
            when(tax.getTaxName2()).thenReturn("VAT-2");
            when(tax.getSeqNo()).thenReturn(3);

            when(stockMasterRepository.findByStockPoid(401L)).thenReturn(java.util.Optional.of(stock));
            when(unitMasterRepository.findByUnitPoid(402L)).thenReturn(java.util.Optional.of(unit));
            when(taxMasterRepository.findByTaxPoid(55L)).thenReturn(java.util.Optional.of(tax));

            List<GLPettyCashItemDtlResponseDto> result =
                    (List<GLPettyCashItemDtlResponseDto>) ReflectionTestUtils.invokeMethod(
                            service, "mapItemResponse", List.of(dtl));

            assertEquals("STK-401", result.get(0).getStockPoidDtl().code());
            assertEquals("UNT-402", result.get(0).getStockUnitPoidDtl().code());
            assertEquals("TAX-55", result.get(0).getTaxPoidDtl().code());
        }

        @Test
        @DisplayName("validateTaxAndVatRules rejects VAT rows missing supplier details")
        void validateTaxAndVatRules_rejectsMissingVatSupplier() {
            when(globalParameterService.getParameterValue(
                    eq("PETTY_CASH_VAT_AMOUNT_LIMIT"), anyString(), anyString(), anyString()))
                    .thenReturn("100");
            when(globalParameterService.getParameterValue(
                    eq("INPUT_TAX_VARIANCE_LIMIT"), anyString(), anyString(), anyString()))
                    .thenReturn("0");

            PettyCashCreateRequestDto request = PettyCashCreateRequestDto.builder()
                    .refType("GENERAL")
                    .glPettyCashPaymentDtlRequestDtos(List.of(
                            GlPettyCashPaymentDtlRequestDto.builder()
                                    .actionType("isCreated")
                                    .drAmt(new BigDecimal("10"))
                                    .vatAmount(new BigDecimal("1"))
                                    .build()
                    ))
                    .build();

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> ReflectionTestUtils.invokeMethod(
                            service, "validateTaxAndVatRules", request, "123"));

            assertTrue(ex.getMessage().contains("VAT supplier not found"));
        }
    }

}
