package com.example.clubmanagementbackend.domain.notification.service;

import com.example.clubmanagementbackend.common.enums.ReadStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.notification.dto.CreateNotificationRequest;
import com.example.clubmanagementbackend.domain.notification.entity.Notification;
import com.example.clubmanagementbackend.domain.notification.entity.NotificationReceiver;
import com.example.clubmanagementbackend.domain.notification.repository.NotificationReceiverRepository;
import com.example.clubmanagementbackend.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationReceiverRepository receiverRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private CreateNotificationRequest createRequest;
    private Member member;

    @BeforeEach
    void setUp() {
        createRequest = CreateNotificationRequest.builder()
                .title("Test Title")
                .content("Test Content")
                .memberIds(List.of("M001"))
                .build();
        member = Member.builder().memberId("M001").build();
    }

    @Test
    void createNotification_Success() {
        Notification savedNotif = Notification.builder().id(1L).title("Test Title").build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotif);
        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));

        assertDoesNotThrow(() -> notificationService.createNotification(createRequest));

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(receiverRepository, times(1)).save(any(NotificationReceiver.class));
    }

    @Test
    void createNotification_EmptyTitle_Throws400() {
        createRequest.setTitle("");
        assertThrows(ResponseStatusException.class, () -> notificationService.createNotification(createRequest));
    }

    @Test
    void createNotification_MemberNotFound_Throws404() {
        Notification savedNotif = Notification.builder().id(1L).title("Test Title").build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotif);
        when(memberRepository.findById("M001")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> notificationService.createNotification(createRequest));
    }

    @Test
    void markAsRead_Success() {
        NotificationReceiver receiver = NotificationReceiver.builder().id(1L).readStatus(ReadStatus.UNREAD).build();
        when(receiverRepository.findByIdAndMember_MemberId(1L, "M001")).thenReturn(Optional.of(receiver));

        assertDoesNotThrow(() -> notificationService.markAsRead(1L, "M001"));

        assertEquals(ReadStatus.READ, receiver.getReadStatus());
        verify(receiverRepository, times(1)).save(receiver);
    }

    @Test
    void createNotificationForAllActiveMembers_Success() {
        Member activeMember = Member.builder().memberId("M002").status(Status.ACTIVE).build();
        when(memberRepository.findByStatus(Status.ACTIVE)).thenReturn(List.of(activeMember));
        
        Notification savedNotif = Notification.builder()
                .id(10L)
                .title("New Program")
                .content("Content info")
                .type("ANNUAL_PROGRAM")
                .relatedId("prog-123")
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotif);

        NotificationReceiver savedReceiver = NotificationReceiver.builder()
                .id(50L)
                .notification(savedNotif)
                .member(activeMember)
                .readStatus(ReadStatus.UNREAD)
                .build();
        when(receiverRepository.save(any(NotificationReceiver.class))).thenReturn(savedReceiver);

        assertDoesNotThrow(() -> notificationService.createNotificationForAllActiveMembers(
                "New Program", "Content info", "ANNUAL_PROGRAM", "prog-123"));

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(receiverRepository, times(1)).save(any(NotificationReceiver.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/notifications/M002"), any(Object.class));
    }

    @Test
    void createNotificationForAllActiveMembers_NoActiveMembers_DoesNothing() {
        when(memberRepository.findByStatus(Status.ACTIVE)).thenReturn(List.of());

        assertDoesNotThrow(() -> notificationService.createNotificationForAllActiveMembers(
                "New Program", "Content info", "ANNUAL_PROGRAM", "prog-123"));

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(receiverRepository, never()).save(any(NotificationReceiver.class));
    }
}
