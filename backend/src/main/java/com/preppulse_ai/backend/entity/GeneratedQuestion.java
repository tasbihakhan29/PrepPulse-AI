package com.preppulse_ai.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "generated_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private GeneratedTest test;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "question_type")
    private String questionType;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column
    private String topic;

    @Column
    private String difficulty;
}
