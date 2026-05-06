package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.entity.ApPaymentRequestDtl;
import com.asg.finance.entity.ApPaymentRequestDtlId;
import com.asg.finance.entity.ApPaymentRequestHdr;
import com.asg.finance.entity.ApPaymentRequestStockDtl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApPaymentRequestMapper {

    private final LovDataService lovDataService;

    public ApPaymentRequestHdr toEntity(
            ApPaymentRequestHdrRequestDto dto,
            Long transactionPoid
    ) {
        return ApPaymentRequestHdr.builder()
                .transactionPoid(transactionPoid)
                .groupPoid(dto.getGroupPoid())
                .companyPoid(dto.getCompanyPoid())
                .transactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : DateUtil.getCurrentDateInUserTimeZone())
                .refType(dto.getRefType())
                .docReferencePoid(dto.getDocReferencePoid())
                .currencyCode(dto.getCurrencyCode())
                .currencyRate(dto.getCurrencyRate())
                .payeeName(dto.getPayeeName())
                .requestedBy(dto.getRequestedBy())
                .remarks(dto.getRemarks())
                .accResponse(dto.getAccResponse())
                .accResponseCategory(dto.getAccResponseCategory())
                .totalAmount(dto.getTotalAmount())
                .deleted("N")
                .build();
    }

    public ApPaymentRequestHdrResponseDto toResponse(
            ApPaymentRequestHdr hdr,
            List<ApPaymentRequestDtl> details,
            List<ApPaymentRequestStockDtl> stockDetails
    ) {
        return ApPaymentRequestHdrResponseDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .groupPoid(hdr.getGroupPoid())
                .companyPoid(hdr.getCompanyPoid())
                .transactionDate(hdr.getTransactionDate())
                .docRef(hdr.getDocRef())
                .refType(hdr.getRefType())
                .docReferencePoid(hdr.getDocReferencePoid())
                .currencyCode(hdr.getCurrencyCode())
                .currencyRate(hdr.getCurrencyRate())
                .payeeName(hdr.getPayeeName())
                .requestedBy(hdr.getRequestedBy())
                .remarks(hdr.getRemarks())
                .accResponse(hdr.getAccResponse())
                .accResponseCategory(hdr.getAccResponseCategory())
                .totalAmount(hdr.getTotalAmount())
                .deleted(hdr.getDeleted())
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .details(details.stream()
                        .map(val -> toDtlResponse(val, hdr.getRefType()))
                        .toList())
                .stockDetails(stockDetails.stream()
                        .map(this::toStockDtlResponse)
                        .toList())
                .build();
    }

    /* ================= DETAIL ================= */

    public ApPaymentRequestDtl toDtlEntity(
            Long transactionPoid,
            ApPaymentRequestDtlRequestDto dto
    ) {
        return ApPaymentRequestDtl.builder()
                .id(new ApPaymentRequestDtlId(transactionPoid, dto.getDetRowId()))
                .chargePoid(dto.getChargePoid())
                .amount(dto.getAmount())
                .vatPer(dto.getVatPer())
                .vatAmount(dto.getVatAmount())
                .totalAmount(dto.getTotalAmount())
                .remarks(dto.getRemarks())
                .build();
    }

    public ApPaymentRequestStockDtl toStockDtlEntity(
            Long transactionPoid,
            ApPaymentRequestStockDtlRequest dto
    ) {
        return ApPaymentRequestStockDtl.builder()
                .id(new ApPaymentRequestDtlId(transactionPoid, dto.getDetRowId()))
                .stockPoid(dto.getStockPoid())
                .quantity(dto.getQuantity())
                .price(dto.getPrice())
                .discount(dto.getDiscount())
                .baseAmount(dto.getBaseAmount())
                .taxPoid(dto.getTaxPoid())
                .taxPercent(dto.getTaxPercent())
                .taxAmount(dto.getTaxAmount())
                .netSales(dto.getNetSales())
                .build();
    }

    public ApPaymentRequestDtlResponseDto toDtlResponse(
            ApPaymentRequestDtl dtl,
            String refType
    ) {
        return ApPaymentRequestDtlResponseDto.builder()
                .transactionPoid(dtl.getId().getTransactionPoid())
                .detRowId(dtl.getId().getDetRowId())
                .chargePoid(dtl.getChargePoid())
                .chargeLov(getLov(dtl.getChargePoid(), refType.equalsIgnoreCase("FF") ? "CHARGE_MASTER_FF" : "CHARGE_MASTER_FOR_PDA"))
                .amount(dtl.getAmount())
                .vatPer(dtl.getVatPer())
                .vatAmount(dtl.getVatAmount())
                .totalAmount(dtl.getTotalAmount())
                .remarks(dtl.getRemarks())
                .createdBy(dtl.getCreatedBy())
                .createdDate(dtl.getCreatedDate())
                .lastModifiedBy(dtl.getLastModifiedBy())
                .lastModifiedDate(dtl.getLastModifiedDate())
                .build();
    }

    public ApPaymentRequestStockDtlResponse toStockDtlResponse(
            ApPaymentRequestStockDtl dtl
    ) {
        return ApPaymentRequestStockDtlResponse.builder()
                .transactionPoid(dtl.getId().getTransactionPoid())
                .detRowId(dtl.getId().getDetRowId())
                .stockPoid(dtl.getStockPoid())
                .stockLov(getLov(dtl.getStockPoid(), "STOCK_MASTER"))
                .quantity(dtl.getQuantity())
                .price(dtl.getPrice())
                .discount(dtl.getDiscount())
                .baseAmount(dtl.getBaseAmount())
                .taxPoid(dtl.getTaxPoid())
                .taxLov(getLov(dtl.getTaxPoid(), "DR_TAX_MASTER"))
                .taxPercent(dtl.getTaxPercent())
                .taxAmount(dtl.getTaxAmount())
                .netSales(dtl.getNetSales())
                .createdBy(dtl.getCreatedBy())
                .createdDate(dtl.getCreatedDate())
                .lastModifiedBy(dtl.getLastModifiedBy())
                .lastModifiedDate(dtl.getLastModifiedDate())
                .build();
    }

    public List<Map<String, Object>> enrichRecords(
            List<Map<String, Object>> records,
            String primaryKey,
            String primaryLovName,
            String primaryLovField,
            String taxkey,
            String taxLovField
    ) {
        return records.stream()
                .map(record -> {
                    Long primaryPoid = convertToLong(record.get(primaryKey));
                    record.put(primaryLovField, getLov(primaryPoid, primaryLovName));

                    Long taxPoid = convertToLong(record.get(taxkey));
                    record.put(taxLovField, getLov(taxPoid, "DR_TAX_MASTER"));

                    return record;
                })
                .toList();
    }

    private Long convertToLong(Object value) {
        if (value == null) return null;

        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof BigDecimal bd) return bd.longValue();

        return Long.valueOf(value.toString());
    }

    public LovGetListDto getLov(Long poid, String lovName) {
        if (poid == null) return null;
        return lovDataService.getDetailsByPoidAndLovNameFast(poid, lovName);
    }
}
