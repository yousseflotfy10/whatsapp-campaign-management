package com.wcm.campaign.dto;

public class BulkUploadResult {

    private String listHash;
    private int totalRows;
    private int savedContacts;
    private boolean duplicate;
    private String message;

    public BulkUploadResult() {
    }

    public BulkUploadResult(
            String listHash,
            int totalRows,
            int savedContacts,
            boolean duplicate,
            String message) {

        this.listHash = listHash;
        this.totalRows = totalRows;
        this.savedContacts = savedContacts;
        this.duplicate = duplicate;
        this.message = message;
    }

    public String getListHash() {
        return listHash;
    }

    public void setListHash(String listHash) {
        this.listHash = listHash;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSavedContacts() {
        return savedContacts;
    }

    public void setSavedContacts(int savedContacts) {
        this.savedContacts = savedContacts;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}