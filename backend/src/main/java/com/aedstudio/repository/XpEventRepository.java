package com.aedstudio.repository;

import com.aedstudio.model.User;
import com.aedstudio.model.XpEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XpEventRepository extends JpaRepository<XpEvent, Long> {

    boolean existsByUserAndEventKey(User user, String eventKey);

    List<XpEvent> findByUser(User user);

    List<XpEvent> findByUserOrderByEarnedAtAsc(User user);

    @Query("SELECT xe.eventKey FROM XpEvent xe WHERE xe.user = :user")
    List<String> findEventKeysByUser(User user);
}
