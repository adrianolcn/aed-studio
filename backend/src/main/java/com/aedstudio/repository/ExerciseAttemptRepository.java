package com.aedstudio.repository;

import com.aedstudio.model.ExerciseAttempt;
import com.aedstudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, Long> {

    List<ExerciseAttempt> findByUser(User user);

    List<ExerciseAttempt> findByUserAndTopicId(User user, String topicId);

    List<ExerciseAttempt> findByUserOrderByAttemptedAtAsc(User user);

    boolean existsByUserAndExerciseIdAndCorrectTrue(User user, String exerciseId);
}
