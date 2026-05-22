package com.blue.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String model;
    private String created_at;
    private ChatMessage message;
    private boolean done;
}
