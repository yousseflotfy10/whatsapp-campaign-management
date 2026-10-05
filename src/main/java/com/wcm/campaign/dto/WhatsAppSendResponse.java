package com.wcm.campaign.dto;

import java.util.List;

public class WhatsAppSendResponse {

    private String messagingProduct;

    private List<MessageResponse> messages;

    public String getMessagingProduct() {
        return messagingProduct;
    }

    public void setMessagingProduct(
            String messagingProduct
    ) {
        this.messagingProduct =
                messagingProduct;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(
            List<MessageResponse> messages
    ) {
        this.messages = messages;
    }

    public static class MessageResponse {

        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}