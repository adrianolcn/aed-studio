package com.aedstudio.repository;

import com.aedstudio.model.SimulationEvent;
import com.aedstudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationEventRepository extends JpaRepository<SimulationEvent, Long> {

    List<SimulationEvent> findByUser(User user);

    List<SimulationEvent> findByUserAndTopicId(User user, String topicId);

    long countByUserAndTopicId(User user, String topicId);
}
