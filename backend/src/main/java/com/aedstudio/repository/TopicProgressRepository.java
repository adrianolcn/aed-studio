package com.aedstudio.repository;

import com.aedstudio.model.TopicProgress;
import com.aedstudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {

    List<TopicProgress> findByUser(User user);

    Optional<TopicProgress> findByUserAndTopicId(User user, String topicId);

    boolean existsByUserAndTopicId(User user, String topicId);

    long countByUser(User user);

    @Query("SELECT COUNT(tp) FROM TopicProgress tp WHERE tp.user = :user AND tp.state = 'COMPLETED'")
    long countCompletedByUser(User user);
}
