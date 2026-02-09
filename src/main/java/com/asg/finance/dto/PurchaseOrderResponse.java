package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private Double subTotal;
    private Double discount;
    private Double expenseBySupplier;
    private Double grandTotal;

    private String remarks;
    private Long rfqPoid;
    private String poStatus;

    private Double itemTotal;
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

    private Double discountPercentage;
    private Double itemDiscountTotal;
    private Double itemDiscountTotalPercentage;

    private String createdBy;
    private LocalDateTime createdDate;

    // Child List
    private List<PurchaseOrderItemResponseDto> items;
}
