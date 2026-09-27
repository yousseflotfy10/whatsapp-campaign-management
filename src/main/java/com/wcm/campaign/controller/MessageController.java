package com.wcm.campaign.controller;

import com.wcm.campaign.dto.MessageResponse;
import com.wcm.campaign.model.Message;
import com.wcm.campaign.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getAll() {

        List<MessageResponse> messages = messageService.getAllMessages().stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getById(@PathVariable Long id) {

        Message message = messageService.getMessageById(id);

        return ResponseEntity.ok(new MessageResponse(message));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<MessageResponse>> getByCampaign(@PathVariable Long campaignId) {

        List<MessageResponse> messages = messageService.getMessagesByCampaign(campaignId).stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/contact/{contactId}")
    public ResponseEntity<List<MessageResponse>> getByContact(@PathVariable Long contactId) {

        List<MessageResponse> messages = messageService.getMessagesByContact(contactId).stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MessageResponse>> getByStatus(@PathVariable String status) {

        List<MessageResponse> messages = messageService.getMessagesByStatus(status).stream().map(MessageResponse::new).toList();

        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<MessageResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {

        Message message = messageService.updateStatus(id, status);

        return ResponseEntity.ok(new MessageResponse(message));
    }
}