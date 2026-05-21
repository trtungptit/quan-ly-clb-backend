package com.example.clubmanagementbackend.domain.activityregistration.service;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.activity.entity.Activity;
import com.example.clubmanagementbackend.domain.activity.repository.ActivityRepository;
import com.example.clubmanagementbackend.domain.activityregistration.dto.CreateActivityRegistrationRequest;
import com.example.clubmanagementbackend.domain.activityregistration.dto.UpdateRegistrationStatusRequest;
import com.example.clubmanagementbackend.domain.activityregistration.entity.ActivityRegistration;
import com.example.clubmanagementbackend.domain.activityregistration.exception.DuplicateRegistrationException;
import com.example.clubmanagementbackend.domain.activityregistration.repository.ActivityRegistrationRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityRegistrationServiceTest {

    @Mock
    private ActivityRegistrationRepository registrationRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private MemberUnitRepository memberUnitRepository;
    @Mock
    private ClubUnitRepository clubUnitRepository;

    @InjectMocks
    private ActivityRegistrationService activityRegistrationService;

    private Member member;
    private Activity activity;
    private CreateActivityRegistrationRequest createRequest;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId("M001").build();
        activity = Activity.builder().activityId("A001").unitId("g_03").build();
        createRequest = CreateActivityRegistrationRequest.builder()
                .memberId("M001")
                .activityId("A001")
                .note("I want to join")
                .build();
    }

    @Test
    void createRegistration_Success() {
        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(activityRepository.findById("A001")).thenReturn(Optional.of(activity));
        
        MemberUnit mu = MemberUnit.builder()
                .memberId("M001")
                .unitId("g_03")
                .status(com.example.clubmanagementbackend.common.enums.Status.ACTIVE)
                .build();
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "g_03")).thenReturn(Optional.of(mu));
        
        when(registrationRepository.findByMember_MemberIdAndActivity_ActivityId("M001", "A001")).thenReturn(Optional.empty());

        ActivityRegistration savedRegistration = ActivityRegistration.builder()
                .id(1L).member(member).activity(activity).status(RegistrationStatus.PENDING).build();
        when(registrationRepository.save(any(ActivityRegistration.class))).thenReturn(savedRegistration);

        var response = activityRegistrationService.createRegistration(createRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("pending", response.getStatus());
        verify(registrationRepository, times(1)).save(any(ActivityRegistration.class));
    }

    @Test
    void createRegistration_DuplicateRegistration_Throws400() {
        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(activityRepository.findById("A001")).thenReturn(Optional.of(activity));

        MemberUnit mu = MemberUnit.builder()
                .memberId("M001")
                .unitId("g_03")
                .status(com.example.clubmanagementbackend.common.enums.Status.ACTIVE)
                .build();
        when(memberUnitRepository.findByMemberIdAndUnitId("M001", "g_03")).thenReturn(Optional.of(mu));

        ActivityRegistration existing = ActivityRegistration.builder().status(RegistrationStatus.PENDING).build();
        when(registrationRepository.findByMember_MemberIdAndActivity_ActivityId("M001", "A001")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateRegistrationException.class, () -> activityRegistrationService.createRegistration(createRequest));
    }

    @Test
    void updateStatus_Success() {
        ActivityRegistration registration = ActivityRegistration.builder()
                .id(1L).member(member).activity(activity).status(RegistrationStatus.PENDING).build();
        
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(ActivityRegistration.class))).thenReturn(registration);

        UpdateRegistrationStatusRequest req = new UpdateRegistrationStatusRequest("APPROVED");
        var response = activityRegistrationService.updateStatus(1L, req);

        assertEquals("approved", response.getStatus());
        verify(registrationRepository, times(1)).save(any());
    }
}
