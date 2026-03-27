package com.asg.finance.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.*;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
}
