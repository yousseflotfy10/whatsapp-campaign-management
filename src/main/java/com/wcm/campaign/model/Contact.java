package com.wcm.campaign.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Column(name = "isactive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "createdat", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "contact_list_member", joinColumns = @JoinColumn(name = "contact_id", referencedColumnName = "contactid"),
            inverseJoinColumns = @JoinColumn(name = "list_id", referencedColumnName = "listid"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"contact_id", "list_id"}))
    private Set<ContactList> contactLists = new HashSet<>();

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

    public Set<ContactList> getContactLists() {
        return contactLists;
    }

    public void setContactLists(Set<ContactList> contactLists) {
        this.contactLists = contactLists;
    }

    public void addContactList(ContactList contactList) {
        this.contactLists.add(contactList);
        contactList.getContacts().add(this);
    }

    public void removeContactList(ContactList contactList) {
        this.contactLists.remove(contactList);
        contactList.getContacts().remove(this);
    }
}