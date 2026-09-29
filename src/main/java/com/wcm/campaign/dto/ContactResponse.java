package com.wcm.campaign.dto;

import com.wcm.campaign.model.Contact;

public class ContactResponse {

    private Long contactId;
    private String contactName;
    private String contactNumbers;
    private String listId;
    private String listName;
    private Boolean active;

    public ContactResponse() {}

    public ContactResponse(Contact contact) {
        this.contactId = contact.getContactId();
        this.contactName = contact.getContactName();
        this.contactNumbers = contact.getContactNumbers();
        this.listId = contact.getListId();
        this.listName = contact.getListName();
        this.active = contact.getIsActive();
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

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}