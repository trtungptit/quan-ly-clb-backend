package com.example.clubmanagementbackend.domain.achievement.service;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.achievement.dto.AchievementResponse;
import com.example.clubmanagementbackend.domain.achievement.dto.CreateAchievementRequest;
import com.example.clubmanagementbackend.domain.achievement.entity.Achievement;
import com.example.clubmanagementbackend.domain.achievement.repository.AchievementRepository;
import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import com.example.clubmanagementbackend.domain.annualprogram.repository.AnnualProgramRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.programparticipation.entity.ProgramParticipation;
import com.example.clubmanagementbackend.domain.programparticipation.repository.ProgramParticipationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private ProgramParticipationRepository participationRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AnnualProgramRepository programRepository;

    @InjectMocks
    private AchievementService achievementService;

    private Member member;
    private AnnualProgram program;
    private ProgramParticipation participation;
    private Achievement achievement;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId("M001")
                .fullName("Nguyen Van A")
                .email("a@gmail.com")
                .phone("0987654321")
                .build();

        program = AnnualProgram.builder()
                .programId("P001")
                .programName("Winter Campaign")
                .status(Status.ACTIVE)
                .build();

        participation = ProgramParticipation.builder()
                .participationId(1L)
                .member(member)
                .program(program)
                .status(RegistrationStatus.APPROVED)
                .build();

        achievement = Achievement.builder()
                .achievementId(10L)
                .achievementName("Best Volunteer")
                .description("Top contributor")
                .achievementDate(LocalDate.now())
                .member(member)
                .program(program)
                .participation(participation)
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    void getAchievements_WithProgramId_ReturnsFilteredList() {
        when(achievementRepository.findByProgramId("P001")).thenReturn(List.of(achievement));

        List<AchievementResponse> responses = achievementService.getAchievements("P001");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Best Volunteer", responses.get(0).getTitle());
        assertEquals("P001", responses.get(0).getProgramId());
        verify(achievementRepository, times(1)).findByProgramId("P001");
    }

    @Test
    void createAchievement_Success() {
        CreateAchievementRequest request = new CreateAchievementRequest();
        request.setMemberId("M001");
        request.setProgramId("P001");
        request.setTitle("Top Volunteer");
        request.setDescription("Excellent efforts");
        request.setAchievementDate(LocalDate.now());

        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(programRepository.findById("P001")).thenReturn(Optional.of(program));
        when(participationRepository.findByMember_MemberIdAndProgram_ProgramId("M001", "P001"))
                .thenReturn(Optional.of(participation));
        when(achievementRepository.findByMemberId("M001")).thenReturn(List.of());
        when(achievementRepository.save(any(Achievement.class))).thenReturn(achievement);

        AchievementResponse response = achievementService.createAchievement(request);

        assertNotNull(response);
        assertEquals("Best Volunteer", response.getTitle());
        verify(achievementRepository, times(1)).save(any(Achievement.class));
    }

    @Test
    void createAchievement_MemberNotFound_ThrowsException() {
        CreateAchievementRequest request = new CreateAchievementRequest();
        request.setMemberId("M999");

        when(memberRepository.findById("M999")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> achievementService.createAchievement(request));
    }

    @Test
    void createAchievement_ProgramNotFound_ThrowsException() {
        CreateAchievementRequest request = new CreateAchievementRequest();
        request.setMemberId("M001");
        request.setProgramId("P999");

        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(programRepository.findById("P999")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> achievementService.createAchievement(request));
    }

    @Test
    void createAchievement_NoParticipation_ThrowsException() {
        CreateAchievementRequest request = new CreateAchievementRequest();
        request.setMemberId("M001");
        request.setProgramId("P001");

        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(programRepository.findById("P001")).thenReturn(Optional.of(program));
        when(participationRepository.findByMember_MemberIdAndProgram_ProgramId("M001", "P001"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> achievementService.createAchievement(request));
    }

    @Test
    void createAchievement_ParticipationNotApproved_ThrowsException() {
        CreateAchievementRequest request = new CreateAchievementRequest();
        request.setMemberId("M001");
        request.setProgramId("P001");

        participation.setStatus(RegistrationStatus.REJECTED);

        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(programRepository.findById("P001")).thenReturn(Optional.of(program));
        when(participationRepository.findByMember_MemberIdAndProgram_ProgramId("M001", "P001"))
                .thenReturn(Optional.of(participation));

        assertThrows(ResponseStatusException.class, () -> achievementService.createAchievement(request));
    }

    @Test
    void createAchievement_DuplicateActiveAchievement_ThrowsException() {
        CreateAchievementRequest request = new CreateAchievementRequest();
        request.setMemberId("M001");
        request.setProgramId("P001");

        when(memberRepository.findById("M001")).thenReturn(Optional.of(member));
        when(programRepository.findById("P001")).thenReturn(Optional.of(program));
        when(participationRepository.findByMember_MemberIdAndProgram_ProgramId("M001", "P001"))
                .thenReturn(Optional.of(participation));
        when(achievementRepository.findByMemberId("M001")).thenReturn(List.of(achievement));

        assertThrows(ResponseStatusException.class, () -> achievementService.createAchievement(request));
    }

    @Test
    void updateAchievement_Success() {
        CreateAchievementRequest request = new CreateAchievementRequest();
        request.setTitle("Super Volunteer");
        request.setStatus("INACTIVE");

        when(achievementRepository.findById(10L)).thenReturn(Optional.of(achievement));
        when(achievementRepository.save(any(Achievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AchievementResponse response = achievementService.updateAchievement(10L, request);

        assertNotNull(response);
        assertEquals("Super Volunteer", response.getTitle());
        assertEquals("inactive", response.getStatus());
    }
}
