package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {

    private Long transactionPoid;
    private LocalDate transactionDate;

    private Long groupPoid;
    private String docRef;
    private Long companyPoid;

    private String currencyCode;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double currencyRate;

    private LocalDate expectedDate;
    private Long supplierPoid;
    private LovGetListDto supplierDetails;

    private String paymentTerms;
    private LovGetListDto paymentTermsDetails;
    private String modeOfTransport;
    private String deliveryTerms;
    private String freightForwarder;

    private String shippingMark;
    private Long billingAddressPoid;
    private Long deliveryAddressPoid;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double subTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double discount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double expenseBySupplier;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double grandTotal;

    private String remarks;
    private Long rfqPoid;
    private String poStatus;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double itemTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double chargeTotal;
    private String type;

    private String description;
    private String deliveryMethod;
    private LovGetListDto deliveryMethodDetails;
    private String deliveryAddress;

    private Long salesQtnPoid;
    private String descriptionPrintYn;

    private Long salesInvPoid;
    private String salesInvDocRef;

    private String refType;
    private LovGetListDto refTypeDetails;
    private String multiCompany;
    private String voucherNarration;

    private Long pjPoid;
    private LocalDate validityDate;
    private Long termsPoid;

    private Long purchaseRequestPoid;
    private Long printDivPoid;
    private LovGetListDto printDivDetails;

    private Long grnPoid;
    private String grnRef;
    private LocalDate grnDate;

    private LocalDate shipmentMonth;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double discountPercentage;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double itemDiscountTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double itemDiscountTotalPercentage;

    // Child List
    private List<PurchaseOrderItemResponseDto> items;
}
