package com.example.clubmanagementbackend.domain.chat.service;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.common.enums.UnitType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ClubUnitRepository clubUnitRepository;
    @Mock private MemberUnitRepository memberUnitRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatService chatService;

    private Member member;
    private UserAccount userAccount;
    private ClubUnit unitGroup;
    private ClubUnit unitDept;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId("M001")
                .fullName("Nguyen Van A")
                .build();
        userAccount = UserAccount.builder()
                .userId("U001")
                .member(member)
                .username("testuser")
                .role(Role.MEMBER)
                .status(Status.ACTIVE)
                .build();
        unitGroup = ClubUnit.builder()
                .unitId("g_k3")
                .unitName("Nhom K3")
                .type(UnitType.GROUP)
                .status(Status.ACTIVE)
                .build();
        unitDept = ClubUnit.builder()
                .unitId("d_tt")
                .unitName("Ban Truyen Thong")
                .type(UnitType.DEPARTMENT)
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    void getRooms_ClubLeader_ReturnsAllActiveUnits() {
        userAccount.setRole(Role.CLUB_LEADER);
        when(userAccountRepository.findByMember_MemberId("M001")).thenReturn(Optional.of(userAccount));
        when(clubUnitRepository.findAll()).thenReturn(List.of(unitGroup, unitDept));
        when(chatMessageRepository.findFirstByUnitIdOrderByCreatedAtDesc("g_k3")).thenReturn(Optional.empty());
        when(chatMessageRepository.findFirstByUnitIdOrderByCreatedAtDesc("d_tt")).thenReturn(Optional.empty());

        List<ChatRoomResponse> rooms = chatService.getRooms("M001", null);

        assertEquals(2, rooms.size());
        assertEquals("Nhom K3", rooms.get(0).getUnitName());
        assertEquals("group", rooms.get(0).getUnitType());
        assertEquals("Ban Truyen Thong", rooms.get(1).getUnitName());
        assertEquals("department", rooms.get(1).getUnitType());
    }

    @Test
    void getRooms_RegularMember_ReturnsOnlyAssignedUnits() {
        when(userAccountRepository.findByMember_MemberId("M001")).thenReturn(Optional.of(userAccount));
        
        MemberUnit mu = MemberUnit.builder()
                .memberId("M001")
                .unitId("g_k3")
                .status(Status.ACTIVE)
                .build();
        when(memberUnitRepository.findByMemberId("M001")).thenReturn(List.of(mu));
        when(clubUnitRepository.findById("g_k3")).thenReturn(Optional.of(unitGroup));
        when(chatMessageRepository.findFirstByUnitIdOrderByCreatedAtDesc("g_k3")).thenReturn(Optional.empty());

        List<ChatRoomResponse> rooms = chatService.getRooms("M001", null);

        assertEquals(1, rooms.size());
        assertEquals("g_k3", rooms.get(0).getUnitId());
        assertEquals("Nhom K3", rooms.get(0).getUnitName());
    }

    @Test
    void getMessages_NoAccess_Throws403() {
        when(userAccountRepository.findByMember_MemberId("M001")).thenReturn(Optional.of(userAccount));
        when(clubUnitRepository.findById("g_k3")).thenReturn(Optional.of(unitGroup));
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "g_k3")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> chatService.getMessages("g_k3", "M001", null));
    }

    @Test
    void getMessages_Success() {
        when(userAccountRepository.findByMember_MemberId("M001")).thenReturn(Optional.of(userAccount));
        when(clubUnitRepository.findById("g_k3")).thenReturn(Optional.of(unitGroup));
        
        MemberUnit mu = MemberUnit.builder().memberId("M001").unitId("g_k3").status(Status.ACTIVE).build();
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "g_k3")).thenReturn(Optional.of(mu));

        ChatMessage message = ChatMessage.builder()
                .id(100L)
                .unitId("g_k3")
                .senderMemberId("M001")
                .senderName("Nguyen Van A")
                .content("Hello World")
                .createdAt(LocalDateTime.now())
                .build();
        when(chatMessageRepository.findByUnitIdOrderByCreatedAtAsc("g_k3")).thenReturn(List.of(message));

        List<ChatMessageResponse> messages = chatService.getMessages("g_k3", "M001", null);

        assertEquals(1, messages.size());
        assertEquals("Hello World", messages.get(0).getContent());
        assertEquals("Nguyen Van A", messages.get(0).getSenderName());
        assertEquals("Nhom K3", messages.get(0).getUnitName());
    }

    @Test
    void sendMessage_EmptyContent_Throws400() {
        SendChatMessageRequest req = SendChatMessageRequest.builder().content("").senderMemberId("M001").build();
        assertThrows(ResponseStatusException.class, () -> chatService.sendMessage("g_k3", req));
    }

    @Test
    void sendMessage_Success() {
        when(clubUnitRepository.findById("g_k3")).thenReturn(Optional.of(unitGroup));
        when(userAccountRepository.findByMember_MemberId("M001")).thenReturn(Optional.of(userAccount));
        
        MemberUnit mu = MemberUnit.builder().memberId("M001").unitId("g_k3").status(Status.ACTIVE).build();
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "g_k3")).thenReturn(Optional.of(mu));
        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));

        ChatMessage savedMsg = ChatMessage.builder()
                .id(1L)
                .unitId("g_k3")
                .senderMemberId("M001")
                .senderName("Nguyen Van A")
                .content("Xin chao")
                .createdAt(LocalDateTime.now())
                .build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        SendChatMessageRequest req = SendChatMessageRequest.builder().senderMemberId("M001").content("Xin chao").build();
        ChatMessageResponse resp = chatService.sendMessage("g_k3", req);

        assertEquals("Xin chao", resp.getContent());
        assertEquals("Nguyen Van A", resp.getSenderName());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/units/g_k3"), any(Object.class));
    }
}
