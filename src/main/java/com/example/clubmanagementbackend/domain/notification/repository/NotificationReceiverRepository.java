package com.example.clubmanagementbackend.domain.notification.repository;

import com.example.clubmanagementbackend.common.enums.ReadStatus;
import com.example.clubmanagementbackend.domain.notification.entity.NotificationReceiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, Long> {
    
    List<NotificationReceiver> findByMember_MemberId(String memberId);
    
    List<NotificationReceiver> findByMember_MemberIdAndReadStatus(String memberId, ReadStatus readStatus);
    
    Optional<NotificationReceiver> findByIdAndMember_MemberId(Long id, String memberId);
}
