package com.wcm.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactRequest {

    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @NotBlank(message = "Contact numbers are required")
    private String contactNumbers;

    private String listId;
    private String listName;
    private java.util.List<String> sourceListIds;
    private Boolean createNewList;

    public ContactRequest() {
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

    public java.util.List<String> getSourceListIds() {
        return sourceListIds;
    }

    public void setSourceListIds(java.util.List<String> sourceListIds) {
        this.sourceListIds = sourceListIds;
    }

    public Boolean getCreateNewList() {
        return createNewList;
    }

    public void setCreateNewList(Boolean createNewList) {
        this.createNewList = createNewList;
    }
}