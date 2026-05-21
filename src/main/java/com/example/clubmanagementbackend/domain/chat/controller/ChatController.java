package com.example.clubmanagementbackend.domain.chat.controller;

import com.example.clubmanagementbackend.domain.chat.dto.ChatMessageResponse;
import com.example.clubmanagementbackend.domain.chat.dto.ChatRoomResponse;
import com.example.clubmanagementbackend.domain.chat.dto.SendChatMessageRequest;
import com.example.clubmanagementbackend.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"}, allowCredentials = "true")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRooms(
            @RequestParam(value = "memberId", required = false) String memberId,
            @RequestParam(value = "userId", required = false) String userId) {
        return ResponseEntity.ok(chatService.getRooms(memberId, userId));
    }

    @GetMapping("/units/{unitId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable("unitId") String unitId,
            @RequestParam(value = "memberId", required = false) String memberId,
            @RequestParam(value = "userId", required = false) String userId) {
        return ResponseEntity.ok(chatService.getMessages(unitId, memberId, userId));
    }

    @PostMapping("/units/{unitId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("unitId") String unitId,
            @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(unitId, request));
    }
}
