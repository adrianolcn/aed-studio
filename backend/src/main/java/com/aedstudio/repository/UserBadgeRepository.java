package com.aedstudio.repository;

import com.aedstudio.model.User;
import com.aedstudio.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUser(User user);

    boolean existsByUserAndBadgeId(User user, String badgeId);
}
