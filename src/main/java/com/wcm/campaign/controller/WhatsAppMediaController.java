package com.wcm.campaign.controller;

import com.wcm.campaign.service.WhatsAppMediaUploadResult;
import com.wcm.campaign.service.WhatsAppProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppMediaController {

    private final WhatsAppProvider whatsappProvider;

    public WhatsAppMediaController(WhatsAppProvider whatsappProvider) {
        this.whatsappProvider = whatsappProvider;
    }

    @PostMapping("/media")
    @PreAuthorize("hasAuthority('MESSAGE_CREATE')")
    public ResponseEntity<WhatsAppMediaUploadResult> uploadMedia(
            @RequestParam("file") MultipartFile file
    ) throws java.io.IOException {
        WhatsAppMediaUploadResult result = whatsappProvider.uploadMedia(
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType()
        );
        return result.success()
                ? ResponseEntity.status(HttpStatus.CREATED).body(result)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
    }
}
