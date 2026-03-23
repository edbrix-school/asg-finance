package com.asg.finance.dto;

import com.asg.finance.entity.ApPaymentRequestDtl;
import com.asg.finance.entity.ApPaymentRequestDtlId;
import com.asg.finance.entity.ApPaymentRequestHdr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.finance.utility.DateTimeHandler.convertDate;

public class ApPaymentRequestMapper {


    public static ApPaymentRequestHdr toEntity(
            ApPaymentRequestHdrRequestDto dto,
            Long transactionPoid
    ) {
        return ApPaymentRequestHdr.builder()
                .transactionPoid(transactionPoid)
                .groupPoid(dto.getGroupPoid())
                .companyPoid(dto.getCompanyPoid())
                .transactionDate(convertDate(LocalDateTime.from(dto.getTransactionDate())))
                .docRef(dto.getDocRef())
                .refType(dto.getRefType())
                .docReferencePoid(dto.getDocReferencePoid())
                .currencyCode(dto.getCurrencyCode())
                .currencyRate(dto.getCurrencyRate())
                .payeePoid(dto.getPayeePoid())
                .requestedBy(dto.getRequestedBy())
                .remarks(dto.getRemarks())
                .totalAmount(dto.getTotalAmount())
                .deleted("N")
                .build();
    }

    public static ApPaymentRequestHdrResponseDto toResponse(
            ApPaymentRequestHdr hdr,
            List<ApPaymentRequestDtl> details
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
                .payeePoid(hdr.getPayeePoid())
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
                        .map(ApPaymentRequestMapper::toDtlResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    /* ================= DETAIL ================= */

    public static ApPaymentRequestDtl toDtlEntity(
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

    public static ApPaymentRequestDtlResponseDto toDtlResponse(
            ApPaymentRequestDtl dtl
    ) {
        return ApPaymentRequestDtlResponseDto.builder()
                .transactionPoid(dtl.getId().getTransactionPoid())
                .detRowId(dtl.getId().getDetRowId())
                .chargePoid(dtl.getChargePoid())
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
}
