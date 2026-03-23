package com.asg.finance.dto;


import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequest {

    private LocalDate transactionDate;
    private String docRef;
    private String currencyCode;
    private Double currencyRate;
    private LocalDate expectedDate;
    @NotNull(message = "Supplier is mandatory")
    private Long supplierPoid;

    @NotNull(message = "Payment terms are mandatory")
    private String paymentTerms;
    private String modeOfTransport;
    @Size(max = 100, message = "Delivery terms cannot exceed 100 characters")
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
    private String deliveryAddress;

    private Long salesQtnPoid;
    private String descriptionPrintYn;

    private Long salesInvPoid;
    private String salesInvDocRef;

    @NotNull(message = "Reference type is mandatory")
    private String refType;
    private String multiCompany;
    private String voucherNarration;

    private Long pjPoid;
    private LocalDate validityDate;

    private Long termsPoid;
    private Long purchaseRequestPoid;
    private Long printDivPoid;

    private Long grnPoid;
    private String grnRef;
    private LocalDate grnDate;

    private LocalDate shipmentMonth;

    private Double discountPercentage;
    private Double itemDiscountTotal;
    private Double itemDiscountTotalPercentage;

    // NEW: Child Request DTOs
    private List<PurchaseOrderItemRequestDto> items;

}
