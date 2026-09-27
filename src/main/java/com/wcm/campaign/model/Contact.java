package com.wcm.campaign.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contactid")
    private Long contactId;

    @Column(name = "contactname", length = 100)
    private String contactName;

    @Column(name = "contactnumbers", nullable = false, columnDefinition = "TEXT")
    private String contactNumbers;

    @Column(name = "listid", columnDefinition = "TEXT")
    private String listId;

    @Column(name = "listhash", columnDefinition = "TEXT")
    private String listHash;

    @Column(name = "isactive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "createdat", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    public Contact() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getListHash() {
        return listHash;
    }

    public void setListHash(String listHash) {
        this.listHash = listHash;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}