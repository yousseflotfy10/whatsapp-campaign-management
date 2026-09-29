package com.wcm.campaign.controller;

import com.wcm.campaign.dto.MessageResponse;
import com.wcm.campaign.model.Message;
import com.wcm.campaign.service.MessageService;
import com.wcm.campaign.service.RealtimeEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final RealtimeEventService realtimeEventService;

    public MessageController(
            MessageService messageService,
            RealtimeEventService realtimeEventService
    ) {
        this.messageService = messageService;
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<List<MessageResponse>> getAll() {

        List<MessageResponse> messages = messageService.getAllMessages().stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<MessageResponse> getById(@PathVariable Long id) {

        Message message = messageService.getMessageById(id);

        return ResponseEntity.ok(new MessageResponse(message));
    }

    @GetMapping("/campaign/{campaignId}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<List<MessageResponse>> getByCampaign(@PathVariable Long campaignId) {

        List<MessageResponse> messages = messageService.getMessagesByCampaign(campaignId).stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/contact/{contactId}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<List<MessageResponse>> getByContact(@PathVariable Long contactId) {

        List<MessageResponse> messages = messageService.getMessagesByContact(contactId).stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<List<MessageResponse>> getByStatus(@PathVariable String status) {

        List<MessageResponse> messages = messageService.getMessagesByStatus(status).stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MESSAGE_SEND')")
    public ResponseEntity<MessageResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {

        Message message = messageService.updateStatus(id, status);
        realtimeEventService.publishAfterCommit("messages");

        return ResponseEntity.ok(new MessageResponse(message));
    }
}