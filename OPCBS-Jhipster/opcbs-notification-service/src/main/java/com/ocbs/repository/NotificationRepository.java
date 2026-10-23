package com.ocbs.repository;

import com.ocbs.domain.Notification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserLoginOrderByCreatedDateDesc(String userLogin);

    Page<Notification> findAllByUserLoginOrderByCreatedDateDesc(String userLogin, Pageable pageable);

    long countByUserLoginAndIsReadFalse(String userLogin);

    List<Notification> findAllByUserLoginAndIsReadFalseOrderByCreatedDateDesc(String userLogin);

    @Modifying
    @Query("update Notification n set n.isRead = true, n.readAt = CURRENT_TIMESTAMP where n.userLogin = :userLogin and n.isRead = false")
    int markAllAsReadForUser(@Param("userLogin") String userLogin);

    boolean existsByUserLoginAndRelatedEntityIdAndNotificationType(String userLogin, Long relatedEntityId, String notificationType);
}
