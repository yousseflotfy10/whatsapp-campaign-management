
package com.wcm.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CampaignContactRequest {

    @NotBlank(message = "Contact name is required")
    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @NotBlank(message = "Contact phone number is required")
    private String contactNumbers;

    public CampaignContactRequest() {
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactNumbers() {
        return contactNumbers;
    }

    public void setContactNumbers(String contactNumbers) {
        this.contactNumbers = contactNumbers;
    }
}
