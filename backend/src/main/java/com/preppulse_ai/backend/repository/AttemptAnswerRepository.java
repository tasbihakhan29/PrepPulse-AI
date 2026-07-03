package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    
    List<AttemptAnswer> findByAttemptId(UUID attemptId);
    
    List<AttemptAnswer> findByAttemptIdOrderByAnsweredAtAsc(UUID attemptId);
    
    void deleteByAttemptId(UUID attemptId);
}
