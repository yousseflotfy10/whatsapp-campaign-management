package com.wcm.campaign.controller;

import com.wcm.campaign.dto.ContactRequest;
import com.wcm.campaign.dto.ContactResponse;
import com.wcm.campaign.model.Contact;
import com.wcm.campaign.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> createContact(@Valid @RequestBody ContactRequest request) {

        Contact contact = contactService.createContact(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ContactResponse(contact));
    }

    @GetMapping
    public ResponseEntity<List<ContactResponse>> getAllContacts() {

        List<ContactResponse> contacts = contactService.getAllContacts().stream().map(ContactResponse::new).toList();

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ContactResponse>> getActiveContacts() {

        List<ContactResponse> contacts = contactService.getActiveContacts().stream().map(ContactResponse::new).toList();

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getContactById(@PathVariable Long id) {

        Contact contact = contactService.getContactById(id);

        return ResponseEntity.ok(new ContactResponse(contact));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ContactResponse>> searchContacts(@RequestParam String name) {

        List<ContactResponse> contacts = contactService.searchContacts(name).stream().map(ContactResponse::new).toList();

        return ResponseEntity.ok(contacts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> updateContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {

        Contact contact = contactService.updateContact(id, request);

        return ResponseEntity.ok(new ContactResponse(contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {

        contactService.deleteContact(id);

        return ResponseEntity.noContent().build();
    }
}