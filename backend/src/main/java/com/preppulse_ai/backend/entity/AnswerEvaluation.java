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
@Table(name = "answer_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private AnswerSubmission submission;

    @Column(nullable = false)
    private Double score;

    @Column(name = "max_marks", nullable = false)
    private Double maxMarks;

    @Column(name = "evaluation_json", nullable = false, columnDefinition = "TEXT")
    private String evaluationJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
