package com.preppulse_ai.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_activity", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "activity_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "tests_attempted")
    @Builder.Default
    private Integer testsAttempted = 0;

    @Column(name = "questions_attempted")
    @Builder.Default
    private Integer questionsAttempted = 0;

    @Column(name = "evaluations_completed")
    @Builder.Default
    private Integer evaluationsCompleted = 0;

    @Column(name = "flashcards_reviewed")
    @Builder.Default
    private Integer flashcardsReviewed = 0;

    @Column(name = "time_spent")
    @Builder.Default
    private Integer timeSpent = 0; // in seconds
}
