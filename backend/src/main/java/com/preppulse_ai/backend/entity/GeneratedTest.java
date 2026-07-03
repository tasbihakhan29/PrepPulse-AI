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
@Table(name = "generated_tests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedTest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_material_id")
    private UploadedMaterial sourceMaterial;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @Column(name = "difficulty", nullable = false)
    private String difficulty;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_marks")
    private Double correctMarks;

    @Column(name = "wrong_marks")
    private Double wrongMarks;

    @Column(name = "unattempted_marks")
    private Double unattemptedMarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
