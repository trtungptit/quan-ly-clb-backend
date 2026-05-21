package com.example.clubmanagementbackend.domain.notification.service;

import com.example.clubmanagementbackend.common.enums.ReadStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.notification.dto.CreateNotificationRequest;
import com.example.clubmanagementbackend.domain.notification.dto.NotificationResponse;
import com.example.clubmanagementbackend.domain.notification.entity.Notification;
import com.example.clubmanagementbackend.domain.notification.entity.NotificationReceiver;
import com.example.clubmanagementbackend.domain.notification.repository.NotificationReceiverRepository;
import com.example.clubmanagementbackend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReceiverRepository receiverRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void createNotification(CreateNotificationRequest request) {
        log.info("Creating a new notification with title: {}", request.getTitle());
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            log.warn("Notification creation failed: Title is empty");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be empty");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            log.warn("Notification creation failed: Content is empty");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be empty");
        }

        List<String> memberIds = request.getMemberIds();
        if (memberIds == null || memberIds.isEmpty()) {
            memberIds = request.getReceiverIds();
        }
        if (memberIds == null || memberIds.isEmpty()) {
            if (request.getMemberId() != null && !request.getMemberId().isBlank()) {
                memberIds = List.of(request.getMemberId());
            } else {
                log.warn("Notification creation failed: Member IDs list is empty");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member IDs list cannot be empty");
            }
        }

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Saved notification with ID: {}", savedNotification.getId());

        int count = 0;
        for (String memberId : memberIds) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> {
                        log.warn("Notification creation failed: Member not found: {}", memberId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found: " + memberId);
                    });
            
            NotificationReceiver receiver = NotificationReceiver.builder()
                    .notification(savedNotification)
                    .member(member)
                    .readStatus(ReadStatus.UNREAD)
                    .build();
            receiverRepository.save(receiver);
            count++;
        }
        log.info("Successfully sent notification to {} members", count);
    }

    @Transactional
    public void createNotificationForAllActiveMembers(String title, String content, String type, String relatedId) {
        log.info("Creating notification for all active members. Title: {}", title);
        
        List<Member> activeMembers = memberRepository.findByStatus(Status.ACTIVE);
        if (activeMembers.isEmpty()) {
            log.info("No active members found to send notifications to.");
            return;
        }

        Notification notification = Notification.builder()
                .title(title)
                .content(content)
                .type(type)
                .relatedId(relatedId)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Saved notification template with ID: {}", savedNotification.getId());

        for (Member member : activeMembers) {
            NotificationReceiver receiver = NotificationReceiver.builder()
                    .notification(savedNotification)
                    .member(member)
                    .readStatus(ReadStatus.UNREAD)
                    .build();
            
            NotificationReceiver savedReceiver = receiverRepository.save(receiver);
            NotificationResponse pushPayload = mapToResponse(savedReceiver);
            
            // Push realtime over STOMP topic /topic/notifications/{memberId}
            try {
                messagingTemplate.convertAndSend("/topic/notifications/" + member.getMemberId(), pushPayload);
            } catch (Exception e) {
                log.error("Failed to push websocket notification to member {}", member.getMemberId(), e);
            }
        }
        log.info("Successfully created notifications for {} active members", activeMembers.size());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForMember(String memberId, ReadStatus status) {
        List<NotificationReceiver> receivers;
        if (status != null) {
            receivers = receiverRepository.findByMember_MemberIdAndReadStatus(memberId, status);
        } else {
            receivers = receiverRepository.findByMember_MemberId(memberId);
        }

        return receivers.stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long receiverId, String memberId) {
        log.info("Marking notification receiver {} as read for member {}", receiverId, memberId);
        NotificationReceiver receiver = receiverRepository.findByIdAndMember_MemberId(receiverId, memberId)
                .orElseThrow(() -> {
                    log.warn("Notification receiver {} not found for member {}", receiverId, memberId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification receiver not found");
                });
        
        receiver.setReadStatus(ReadStatus.READ);
        receiverRepository.save(receiver);
        log.info("Successfully marked notification receiver {} as read", receiverId);
    }

    private NotificationResponse mapToResponse(NotificationReceiver receiver) {
        boolean isRead = receiver.getReadStatus() == ReadStatus.READ;
        Notification n = receiver.getNotification();
        return NotificationResponse.builder()
                .id(receiver.getId())
                .notificationId(n.getId())
                .memberId(receiver.getMember() != null ? receiver.getMember().getMemberId() : null)
                .title(n.getTitle())
                .content(n.getContent())
                .createdAt(n.getCreatedAt())
                .readStatus(receiver.getReadStatus() != null ? receiver.getReadStatus().name().toLowerCase() : null)
                .isRead(isRead)
                .read(isRead)
                .type(n.getType())
                .relatedId(n.getRelatedId())
                .build();
    }
}
