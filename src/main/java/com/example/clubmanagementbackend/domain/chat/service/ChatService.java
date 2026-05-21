package com.example.clubmanagementbackend.domain.chat.service;

import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.chat.dto.ChatMessageResponse;
import com.example.clubmanagementbackend.domain.chat.dto.ChatRoomResponse;
import com.example.clubmanagementbackend.domain.chat.dto.SendChatMessageRequest;
import com.example.clubmanagementbackend.domain.chat.entity.ChatMessage;
import com.example.clubmanagementbackend.domain.chat.repository.ChatMessageRepository;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ClubUnitRepository clubUnitRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final MemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(String memberId, String userId) {
        log.info("Fetching chat rooms for memberId: {}, userId: {}", memberId, userId);
        
        ResolvedUser user = resolveUser(memberId, userId);
        Role role = user.getRole();
        String resolvedMemberId = user.getMemberId();

        List<ClubUnit> accessibleUnits = new ArrayList<>();

        if (role == Role.CLUB_LEADER || role == Role.ADMIN) {
            accessibleUnits = clubUnitRepository.findAll().stream()
                    .filter(u -> u.getStatus() == Status.ACTIVE)
                    .collect(Collectors.toList());
        } else {
            if (resolvedMemberId != null) {
                List<MemberUnit> memberUnits = memberUnitRepository.findByMemberId(resolvedMemberId).stream()
                        .filter(mu -> mu.getStatus() == Status.ACTIVE)
                        .collect(Collectors.toList());
                for (MemberUnit mu : memberUnits) {
                    clubUnitRepository.findById(mu.getUnitId())
                            .filter(u -> u.getStatus() == Status.ACTIVE)
                            .ifPresent(accessibleUnits::add);
                }
            }
        }

        List<ChatRoomResponse> rooms = new ArrayList<>();
        for (ClubUnit unit : accessibleUnits) {
            String lastMessageContent = "";
            java.time.LocalDateTime lastMessageTime = null;

            Optional<ChatMessage> lastMsgOpt = chatMessageRepository.findFirstByUnitIdOrderByCreatedAtDesc(unit.getUnitId());
            if (lastMsgOpt.isPresent()) {
                lastMessageContent = lastMsgOpt.get().getContent();
                lastMessageTime = lastMsgOpt.get().getCreatedAt();
            }

            rooms.add(ChatRoomResponse.builder()
                    .unitId(unit.getUnitId())
                    .unitName(unit.getUnitName())
                    .unitType(unit.getType() != null ? unit.getType().name().toLowerCase() : null)
                    .lastMessage(lastMessageContent)
                    .lastMessageAt(lastMessageTime)
                    .unreadCount(0)
                    .build());
        }

        return rooms;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(String unitId, String memberId, String userId) {
        log.info("Fetching message history for unitId: {}, memberId: {}, userId: {}", unitId, memberId, userId);
        
        ClubUnit unit = clubUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

        ResolvedUser user = resolveUser(memberId, userId);
        checkAccess(user, unitId);

        List<ChatMessage> messages = chatMessageRepository.findByUnitIdOrderByCreatedAtAsc(unitId);
        
        String unitTypeStr = unit.getType() != null ? unit.getType().name().toLowerCase() : null;
        
        return messages.stream()
                .map(msg -> ChatMessageResponse.builder()
                        .messageId(msg.getId())
                        .unitId(msg.getUnitId())
                        .unitName(unit.getUnitName())
                        .unitType(unitTypeStr)
                        .senderMemberId(msg.getSenderMemberId())
                        .senderUserId(msg.getSenderUserId())
                        .senderName(msg.getSenderName())
                        .senderRole(msg.getSenderRole())
                        .content(msg.getContent())
                        .messageType(msg.getMessageType())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(String unitId, SendChatMessageRequest request) {
        log.info("Sending chat message to unitId: {}, senderMemberId: {}", unitId, request.getSenderMemberId());

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        ClubUnit unit = clubUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

        ResolvedUser user = resolveUser(request.getSenderMemberId(), request.getSenderUserId());
        checkAccess(user, unitId);

        Member senderMember = memberRepository.findById(user.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender member not found"));

        ChatMessage message = ChatMessage.builder()
                .unitId(unitId)
                .senderMemberId(user.getMemberId())
                .senderUserId(user.getUserId())
                .senderName(senderMember.getFullName())
                .senderRole(user.getRole() != null ? user.getRole().name() : "MEMBER")
                .content(request.getContent().trim())
                .messageType("TEXT")
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("Saved chat message with ID: {}", savedMessage.getId());

        String unitTypeStr = unit.getType() != null ? unit.getType().name().toLowerCase() : null;

        ChatMessageResponse response = ChatMessageResponse.builder()
                .messageId(savedMessage.getId())
                .unitId(savedMessage.getUnitId())
                .unitName(unit.getUnitName())
                .unitType(unitTypeStr)
                .senderMemberId(savedMessage.getSenderMemberId())
                .senderUserId(savedMessage.getSenderUserId())
                .senderName(savedMessage.getSenderName())
                .senderRole(savedMessage.getSenderRole())
                .content(savedMessage.getContent())
                .messageType(savedMessage.getMessageType())
                .createdAt(savedMessage.getCreatedAt())
                .build();

        try {
            messagingTemplate.convertAndSend("/topic/chat/units/" + unitId, response);
        } catch (Exception e) {
            log.error("Failed to push chat message via websocket for unit {}", unitId, e);
        }

        return response;
    }

    private ResolvedUser resolveUser(String memberId, String userId) {
        if ((memberId == null || memberId.trim().isEmpty()) && (userId == null || userId.trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either memberId or userId must be provided");
        }

        UserAccount account = null;
        Member member = null;

        if (userId != null && !userId.trim().isEmpty()) {
            account = userAccountRepository.findById(userId.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
            member = account.getMember();
        } else {
            account = userAccountRepository.findByMember_MemberId(memberId.trim()).orElse(null);
            if (account == null) {
                member = memberRepository.findById(memberId.trim())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
            } else {
                member = account.getMember();
            }
        }

        String resolvedMemberId = member != null ? member.getMemberId() : (account != null && account.getMember() != null ? account.getMember().getMemberId() : null);
        String resolvedUserId = account != null ? account.getUserId() : null;
        Role resolvedRole = account != null ? account.getRole() : Role.MEMBER;

        return new ResolvedUser(resolvedMemberId, resolvedUserId, resolvedRole);
    }

    private void checkAccess(ResolvedUser user, String unitId) {
        if (user.getRole() == Role.CLUB_LEADER || user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: user has no member ID");
        }

        boolean hasActiveUnit = memberUnitRepository.findByMemberIdAndUnitId(user.getMemberId(), unitId)
                .filter(mu -> mu.getStatus() == Status.ACTIVE)
                .isPresent();

        if (!hasActiveUnit) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: member is not active in this unit");
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class ResolvedUser {
        private final String memberId;
        private final String userId;
        private final Role role;
    }
}
