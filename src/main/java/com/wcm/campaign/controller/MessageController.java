package com.wcm.campaign.controller;

import com.wcm.campaign.dto.MessageResponse;
import com.wcm.campaign.model.Message;
import com.wcm.campaign.service.MessageService;
import com.wcm.campaign.service.RealtimeEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

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

        logger.info("Fetching all messages");

        List<MessageResponse> messages = messageService.getAllMessages().stream().map(MessageResponse::new).toList();

        logger.info("Fetched {} messages", messages.size());

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<MessageResponse> getById(@PathVariable Long id) {

        logger.info("Fetching message with id={}", id);

        Message message = messageService.getMessageById(id);

        return ResponseEntity.ok(new MessageResponse(message));
    }

    @GetMapping("/campaign/{campaignId}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<List<MessageResponse>> getByCampaign(@PathVariable Long campaignId) {

        logger.info("Fetching messages for campaign id={}", campaignId);

        List<MessageResponse> messages = messageService.getMessagesByCampaign(campaignId).stream().map(MessageResponse::new).toList();

        logger.info("Fetched {} messages for campaign id={}", messages.size(), campaignId);

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/contact/{contactId}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<List<MessageResponse>> getByContact(@PathVariable Long contactId) {

        logger.info("Fetching messages for contact id={}", contactId);

        List<MessageResponse> messages = messageService.getMessagesByContact(contactId).stream().map(MessageResponse::new).toList();

        logger.info("Fetched {} messages for contact id={}", messages.size(), contactId);

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('MESSAGE_VIEW')")
    public ResponseEntity<List<MessageResponse>> getByStatus(@PathVariable String status) {

        logger.info("Fetching messages with status={}", status);

        List<MessageResponse> messages = messageService.getMessagesByStatus(status).stream().map(MessageResponse::new).toList();

        logger.info("Fetched {} messages with status={}", messages.size(), status);

        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MESSAGE_SEND')")
    public ResponseEntity<MessageResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {

        logger.info("Updating message status for message id={}", id);

        Message message = messageService.updateStatus(id, status);
        realtimeEventService.publishAfterCommit("messages");

        logger.info("Message status updated successfully for message id={}", id);

        return ResponseEntity.ok(new MessageResponse(message));
    }
}