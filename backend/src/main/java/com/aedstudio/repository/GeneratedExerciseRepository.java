package com.aedstudio.repository;

import com.aedstudio.model.GeneratedExercise;
import com.aedstudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedExerciseRepository extends JpaRepository<GeneratedExercise, Long> {

    Optional<GeneratedExercise> findByUserAndGeneratedId(User user, String generatedId);

    List<GeneratedExercise> findTop20ByUserOrderByCreatedAtDesc(User user);

    long countByUserAndTopicId(User user, String topicId);
}
