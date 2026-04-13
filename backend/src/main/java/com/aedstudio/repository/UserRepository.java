package com.aedstudio.repository;

import com.aedstudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.xp = u.xp + :amount WHERE u.id = :userId")
    void addXp(Long userId, int amount);

    @Modifying
    @Query("UPDATE User u SET u.topicsCompleted = u.topicsCompleted + 1 WHERE u.id = :userId")
    void incrementTopicsCompleted(Long userId);
}
