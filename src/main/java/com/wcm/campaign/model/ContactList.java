package com.wcm.campaign.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contact_lists", uniqueConstraints = {@UniqueConstraint(name = "uk_contact_list_hash", columnNames = "listhash")})
public class ContactList {

    @Id
    @Column(name = "listid", length = 64)
    private String listId;

    @Column(name = "listname", nullable = false, length = 255)
    private String listName;

    @Column(name = "listhash", nullable = false, unique = true, length = 64)
    private String listHash;

    @Column(name = "createdat", nullable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "contactLists", fetch = FetchType.LAZY)
    private Set<Contact> contacts = new HashSet<>();

    public ContactList() {
    }

    public ContactList(String listId, String listName, String listHash) {
        this.listId = listId;
        this.listName = listName;
        this.listHash = listHash;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public String getListHash() {
        return listHash;
    }

    public void setListHash(String listHash) {
        this.listHash = listHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }
}