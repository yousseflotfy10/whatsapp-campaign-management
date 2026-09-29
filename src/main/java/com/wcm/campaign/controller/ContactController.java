package com.wcm.campaign.controller;

import com.wcm.campaign.dto.ContactRequest;
import com.wcm.campaign.dto.ContactListResponse;
import com.wcm.campaign.dto.ContactResponse;
import com.wcm.campaign.model.Contact;
import com.wcm.campaign.service.ContactService;
import com.wcm.campaign.service.RealtimeEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;
    private final RealtimeEventService realtimeEventService;

    public ContactController(
            ContactService contactService,
            RealtimeEventService realtimeEventService
    ) {
        this.contactService = contactService;
        this.realtimeEventService = realtimeEventService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public ResponseEntity<ContactResponse> createContact(@Valid @RequestBody ContactRequest request) {

        Contact contact = contactService.createContact(request);

        realtimeEventService.publishAfterCommit("contacts");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ContactResponse(contact));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public ResponseEntity<com.wcm.campaign.dto.BulkUploadResult> uploadContacts(@RequestParam("file") MultipartFile file) {
        var result = contactService.uploadBulk(file);
        realtimeEventService.publishAfterCommit("contacts");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactResponse>> getAllContacts() {

        List<ContactResponse> contacts = contactService.getAllContacts().stream().map(ContactResponse::new).toList();

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactResponse>> getActiveContacts() {

        List<ContactResponse> contacts = contactService.getActiveContacts().stream().map(ContactResponse::new).toList();

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/lists")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactListResponse>> getActiveListIds() {
        return ResponseEntity.ok(contactService.getActiveListIds());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<ContactResponse> getContactById(@PathVariable Long id) {

        Contact contact = contactService.getContactById(id);

        realtimeEventService.publishAfterCommit("contacts");
        return ResponseEntity.ok(new ContactResponse(contact));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactResponse>> searchContacts(@RequestParam String name) {

        List<ContactResponse> contacts = contactService.searchContacts(name).stream().map(ContactResponse::new).toList();

        return ResponseEntity.ok(contacts);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_UPDATE')")
    public ResponseEntity<ContactResponse> updateContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {

        Contact contact = contactService.updateContact(id, request);

        return ResponseEntity.ok(new ContactResponse(contact));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_DELETE')")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {

        contactService.deleteContact(id);
        realtimeEventService.publishAfterCommit("contacts");

        return ResponseEntity.noContent().build();
    }
}