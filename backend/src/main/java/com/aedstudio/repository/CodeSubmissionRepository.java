package com.aedstudio.repository;

import com.aedstudio.model.CodeSubmission;
import com.aedstudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {

    List<CodeSubmission> findByUser(User user);

    List<CodeSubmission> findByUserAndTopicId(User user, String topicId);

    List<CodeSubmission> findTop20ByUserOrderByCreatedAtDesc(User user);

    List<CodeSubmission> findTop20ByUserAndTopicIdOrderByCreatedAtDesc(User user, String topicId);

    List<CodeSubmission> findTop20ByUserAndExerciseIdOrderByCreatedAtDesc(User user, String exerciseId);

    Optional<CodeSubmission> findTopByUserAndExerciseIdOrderByCreatedAtDesc(User user, String exerciseId);

    Optional<CodeSubmission> findTopByUserAndExerciseIdAndStatusOrderByPassedTestsDescCreatedAtAsc(
            User user,
            String exerciseId,
            String status);

    Optional<CodeSubmission> findTopByUserAndExerciseIdOrderByPassedTestsDescCreatedAtAsc(User user, String exerciseId);
}
