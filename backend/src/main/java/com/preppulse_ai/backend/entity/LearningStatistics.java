package com.preppulse_ai.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_statistics", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "topic", "exam_type", "question_type", "difficulty"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String topic;

    @Column(name = "exam_type")
    private String examType;

    @Column(name = "question_type")
    private String questionType;

    @Column
    private String difficulty;

    @Column(name = "total_questions")
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(name = "correct_questions")
    @Builder.Default
    private Integer correctQuestions = 0;

    @Column(name = "wrong_questions")
    @Builder.Default
    private Integer wrongQuestions = 0;

    @Column(name = "skipped_questions")
    @Builder.Default
    private Integer skippedQuestions = 0;

    @Column(name = "average_score")
    private Double averageScore;

    @Column(name = "average_percentage")
    private Double averagePercentage;

    @Column(name = "total_time_spent")
    private Integer totalTimeSpent; // in seconds

    @Column(name = "last_activity_date")
    private OffsetDateTime lastActivityDate;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
