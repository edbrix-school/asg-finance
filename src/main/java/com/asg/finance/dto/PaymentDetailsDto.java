package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymentDetailsDto {
    @JsonAlias({"id", "detRowId"})  // Accept both "id" and "detRowId" from JSON
    private Long detRowId;
    
    private Long glPoid;
    private String type;
    private String beneficiaryName;
    private String address;
    private String bank;
    private String bankAddress;
    private Long beneficiaryCountry;  // for request input
    private CountryInfoDto beneficiaryCountryDetails;  // for response output
    private String swiftCode;
    private String accountNumber;
    private String iban;
    private String specialInstruction;
    private String intermediaryBank;
    private String intermediaryAcct;
    private String intermediaryOth;
    private String intermediarySwiftCode;
    private Long intermediaryCountryPoid;
    private CountryInfoDto intermediaryCountryDetails;  // for response output
    private String active;
    private String actionType;

    /**
     * Custom setter to handle both boolean and String values for active field
     * This allows the API to accept boolean values from JSON and convert them to "Y"/"N"
     */
    @JsonProperty("active")
    public void setActive(Object activeValue) {
        if (activeValue == null) {
            this.active = "Y"; // Default to active
        } else if (activeValue instanceof Boolean) {
            this.active = ((Boolean) activeValue) ? "Y" : "N";
        } else if (activeValue instanceof String) {
            String str = ((String) activeValue).trim().toUpperCase();
            if ("TRUE".equals(str) || "Y".equals(str) || "YES".equals(str) || "1".equals(str)) {
                this.active = "Y";
            } else if ("FALSE".equals(str) || "N".equals(str) || "NO".equals(str) || "0".equals(str)) {
                this.active = "N";
            } else {
                this.active = str; // Keep as is if already "Y" or "N"
            }
        } else {
            this.active = "Y"; // Default
        }
    }
}

