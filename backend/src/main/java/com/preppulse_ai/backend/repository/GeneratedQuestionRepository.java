package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.GeneratedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratedQuestionRepository extends JpaRepository<GeneratedQuestion, UUID> {
    List<GeneratedQuestion> findAllByTestId(UUID testId);
}
