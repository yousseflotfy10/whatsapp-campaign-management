package com.wcm.campaign.dto;

import com.wcm.campaign.model.Contact;

import java.util.List;

public class ContactResponse {

    private Long contactId;
    private String contactName;
    private String contactNumbers;
    private Boolean active;
    private List<ContactListResponse> contactLists;

    public ContactResponse() {
    }

    public ContactResponse(Contact contact) {
        this.contactId = contact.getContactId();
        this.contactName = contact.getContactName();
        this.contactNumbers = contact.getContactNumbers();
        this.active = contact.getIsActive();
        this.contactLists = contact.getContactLists().stream()
                .map(contactList -> new ContactListResponse(contactList.getListId(), contactList.getListName()))
                .toList();
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<ContactListResponse> getContactLists() {
        return contactLists;
    }

    public void setContactLists(List<ContactListResponse> contactLists) {
        this.contactLists = contactLists;
    }
}
