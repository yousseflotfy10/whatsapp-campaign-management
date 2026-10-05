package com.wcm.campaign.controller;

import com.wcm.campaign.dto.BulkUploadResult;
import com.wcm.campaign.dto.ContactListResponse;
import com.wcm.campaign.dto.ContactRequest;
import com.wcm.campaign.dto.ContactResponse;
import com.wcm.campaign.model.Contact;
import com.wcm.campaign.service.ContactService;
import com.wcm.campaign.service.RealtimeEventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

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

        logger.info("Creating contact");

        Contact contact = contactService.createContact(request);

        realtimeEventService.publishAfterCommit("contacts");

        logger.info("Contact created successfully with id={}", contact.getContactId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new ContactResponse(contact));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public ResponseEntity<BulkUploadResult> uploadContacts(@RequestParam("file") MultipartFile file) {

        logger.info("Uploading contacts file: {}", file.getOriginalFilename());

        BulkUploadResult result = contactService.uploadBulk(file);

        realtimeEventService.publishAfterCommit("contacts");

        if (result.isDuplicate()) {
            logger.warn("Contacts upload rejected because the file is a duplicate: {}", file.getOriginalFilename());
            return ResponseEntity.ok(result);
        }

        logger.info("Contacts file uploaded successfully: {}", file.getOriginalFilename());

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactResponse>> getAllContacts() {

        logger.info("Fetching all contacts");

        List<ContactResponse> contacts = contactService.getAllContacts()
                .stream()
                .map(ContactResponse::new)
                .toList();

        logger.info("Fetched {} contacts", contacts.size());

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactResponse>> getActiveContacts() {

        logger.info("Fetching active contacts");

        List<ContactResponse> contacts = contactService.getActiveContacts()
                .stream()
                .map(ContactResponse::new)
                .toList();

        logger.info("Fetched {} active contacts", contacts.size());

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/lists")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactListResponse>> getContactLists() {

        logger.info("Fetching active contact lists");

        return ResponseEntity.ok(contactService.getActiveListIds());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<ContactResponse> getContactById(@PathVariable Long id) {

        logger.info("Fetching contact with id={}", id);

        Contact contact = contactService.getContactById(id);

        return ResponseEntity.ok(new ContactResponse(contact));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    public ResponseEntity<List<ContactResponse>> searchContacts(@RequestParam String name) {

        logger.info("Searching contacts");

        List<ContactResponse> contacts = contactService.searchContacts(name)
                .stream()
                .map(ContactResponse::new)
                .toList();

        logger.info("Contact search returned {} results", contacts.size());

        return ResponseEntity.ok(contacts);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_UPDATE')")
    public ResponseEntity<ContactResponse> updateContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request
    ) {

        logger.info("Updating contact with id={}", id);

        Contact contact = contactService.updateContact(id, request);

        realtimeEventService.publishAfterCommit("contacts");

        logger.info("Contact updated successfully with id={}", id);

        return ResponseEntity.ok(new ContactResponse(contact));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_DELETE')")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {

        logger.info("Deleting contact with id={}", id);

        contactService.deleteContact(id);

        realtimeEventService.publishAfterCommit("contacts");

        logger.info("Contact deleted successfully with id={}", id);

        return ResponseEntity.noContent().build();
    }
}