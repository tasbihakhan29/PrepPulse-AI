package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.GeneratedTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratedTestRepository extends JpaRepository<GeneratedTest, UUID> {
    List<GeneratedTest> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT gt FROM GeneratedTest gt JOIN gt.sourceMaterial sm " +
           "WHERE sm.contentHash = :contentHash " +
           "AND gt.examType = :examType " +
           "AND gt.difficulty = :difficulty " +
           "AND gt.questionType = :questionType")
    List<GeneratedTest> findSimilarTests(
        @Param("contentHash") String contentHash,
        @Param("examType") String examType,
        @Param("difficulty") String difficulty,
        @Param("questionType") String questionType
    );
}
