package com.aedstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_events",
       indexes = {
           @Index(name = "idx_simulation_events_user", columnList = "user_id"),
           @Index(name = "idx_simulation_events_user_topic", columnList = "user_id,topic_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "topic_id", nullable = false, length = 50)
    private String topicId;

    @Column(name = "simulator_type", nullable = false, length = 30)
    private String simulatorType;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 60)
    private String milestone;

    @Column(columnDefinition = "TEXT")
    private String stateSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
