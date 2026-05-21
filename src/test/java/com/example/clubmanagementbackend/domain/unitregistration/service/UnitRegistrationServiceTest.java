package com.example.clubmanagementbackend.domain.unitregistration.service;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.unitregistration.dto.CreateUnitRegistrationRequest;
import com.example.clubmanagementbackend.domain.unitregistration.dto.UpdateRegistrationStatusRequest;
import com.example.clubmanagementbackend.domain.unitregistration.entity.UnitRegistration;
import com.example.clubmanagementbackend.domain.unitregistration.exception.DuplicateRegistrationException;
import com.example.clubmanagementbackend.domain.unitregistration.repository.UnitRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UnitRegistrationServiceTest {

    @Mock
    private UnitRegistrationRepository registrationRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ClubUnitRepository clubUnitRepository;
    @Mock
    private MemberUnitRepository memberUnitRepository;

    @InjectMocks
    private UnitRegistrationService unitRegistrationService;

    private Member member;
    private ClubUnit unit;
    private CreateUnitRegistrationRequest createRequest;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId("M001").build();
        unit = ClubUnit.builder().unitId("U001").build();
        createRequest = CreateUnitRegistrationRequest.builder()
                .memberId("M001")
                .unitId("U001")
                .note("Please let me join")
                .build();
    }

    // ─── createRegistration ──────────────────────────────────────────────────

    @Test
    void createRegistration_Success_NoMemberUnit() {
        // Member không có record trong member_units (chưa từng thuộc unit nào)
        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(clubUnitRepository.findById("U001")).thenReturn(Optional.of(unit));
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "U001")).thenReturn(Optional.empty());
        when(registrationRepository.findByMember_MemberIdAndUnit_UnitId("M001", "U001")).thenReturn(Optional.empty());

        UnitRegistration savedRegistration = UnitRegistration.builder()
                .id(1L).member(member).unit(unit).status(RegistrationStatus.PENDING).build();
        when(registrationRepository.save(any(UnitRegistration.class))).thenReturn(savedRegistration);

        var response = unitRegistrationService.createRegistration(createRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("pending", response.getStatus());
        verify(registrationRepository, times(1)).save(any(UnitRegistration.class));
    }

    @Test
    void createRegistration_Success_InactiveMemberUnit() {
        // Member từng thuộc unit nhưng INACTIVE → vẫn được gửi đơn lại
        MemberUnit inactiveMu = MemberUnit.builder()
                .memberId("M001").unitId("U001").status(Status.INACTIVE).build();

        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(clubUnitRepository.findById("U001")).thenReturn(Optional.of(unit));
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "U001")).thenReturn(Optional.of(inactiveMu));
        when(registrationRepository.findByMember_MemberIdAndUnit_UnitId("M001", "U001")).thenReturn(Optional.empty());

        UnitRegistration savedRegistration = UnitRegistration.builder()
                .id(2L).member(member).unit(unit).status(RegistrationStatus.PENDING).build();
        when(registrationRepository.save(any(UnitRegistration.class))).thenReturn(savedRegistration);

        var response = unitRegistrationService.createRegistration(createRequest);

        assertNotNull(response);
        assertEquals("pending", response.getStatus());
        verify(registrationRepository, times(1)).save(any(UnitRegistration.class));
    }

    @Test
    void createRegistration_Blocked_ActiveMemberUnit() {
        // Member đang ACTIVE trong unit → bị chặn
        MemberUnit activeMu = MemberUnit.builder()
                .memberId("M001").unitId("U001").status(Status.ACTIVE).build();

        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(clubUnitRepository.findById("U001")).thenReturn(Optional.of(unit));
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "U001")).thenReturn(Optional.of(activeMu));

        assertThrows(DuplicateRegistrationException.class,
                () -> unitRegistrationService.createRegistration(createRequest));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void createRegistration_MemberNotFound_Throws404() {
        when(memberRepository.findById("M001")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> unitRegistrationService.createRegistration(createRequest));
    }

    @Test
    void createRegistration_PendingExists_Throws() {
        // Không có record member_units, nhưng đã có đơn PENDING → bị chặn
        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(clubUnitRepository.findById("U001")).thenReturn(Optional.of(unit));
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "U001")).thenReturn(Optional.empty());

        UnitRegistration pending = UnitRegistration.builder().status(RegistrationStatus.PENDING).build();
        when(registrationRepository.findByMember_MemberIdAndUnit_UnitId("M001", "U001"))
                .thenReturn(Optional.of(pending));

        assertThrows(DuplicateRegistrationException.class,
                () -> unitRegistrationService.createRegistration(createRequest));
    }

    // ─── updateStatus ────────────────────────────────────────────────────────

    @Test
    void updateStatus_Approved_Success() {
        UnitRegistration registration = UnitRegistration.builder()
                .id(1L).member(member).unit(unit).status(RegistrationStatus.PENDING).build();

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(UnitRegistration.class))).thenReturn(registration);

        UpdateRegistrationStatusRequest req = new UpdateRegistrationStatusRequest("approved");
        var response = unitRegistrationService.updateStatus(1L, req);

        assertEquals("approved", response.getStatus());
        // APPROVED phải tạo/activate MemberUnit
        verify(memberUnitRepository, times(1)).save(any());
    }

    @Test
    void updateStatus_Rejected_Success() {
        UnitRegistration registration = UnitRegistration.builder()
                .id(1L).member(member).unit(unit).status(RegistrationStatus.PENDING).build();

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(UnitRegistration.class))).thenReturn(registration);

        UpdateRegistrationStatusRequest req = new UpdateRegistrationStatusRequest("rejected");
        var response = unitRegistrationService.updateStatus(1L, req);

        assertEquals("rejected", response.getStatus());
        // REJECTED không được tạo MemberUnit
        verify(memberUnitRepository, never()).save(any());
    }

    @Test
    void updateStatus_Pending_Throws400() {
        UnitRegistration registration = UnitRegistration.builder()
                .id(1L).member(member).unit(unit).status(RegistrationStatus.PENDING).build();
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        UpdateRegistrationStatusRequest req = new UpdateRegistrationStatusRequest("pending");
        var ex = assertThrows(ResponseStatusException.class,
                () -> unitRegistrationService.updateStatus(1L, req));

        assertTrue(ex.getMessage().contains("approved or rejected"));
    }

    @Test
    void updateStatus_Joined_Throws400() {
        UnitRegistration registration = UnitRegistration.builder()
                .id(1L).member(member).unit(unit).status(RegistrationStatus.PENDING).build();
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        UpdateRegistrationStatusRequest req = new UpdateRegistrationStatusRequest("joined");
        var ex = assertThrows(ResponseStatusException.class,
                () -> unitRegistrationService.updateStatus(1L, req));

        assertTrue(ex.getMessage().contains("approved or rejected"));
    }

    @Test
    void updateStatus_NullStatus_Throws400() {
        UnitRegistration registration = UnitRegistration.builder()
                .id(1L).member(member).unit(unit).status(RegistrationStatus.PENDING).build();
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        UpdateRegistrationStatusRequest req = new UpdateRegistrationStatusRequest(null);
        var ex = assertThrows(ResponseStatusException.class,
                () -> unitRegistrationService.updateStatus(1L, req));

        assertTrue(ex.getMessage().contains("required"));
    }
}
