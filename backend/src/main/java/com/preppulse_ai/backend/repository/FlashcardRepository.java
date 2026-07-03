package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.Flashcard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
    List<Flashcard> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Page<Flashcard> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    @Query("SELECT f FROM Flashcard f WHERE f.user.id = :userId AND LOWER(f.topic) LIKE LOWER(CONCAT('%', :topic, '%')) ORDER BY f.createdAt DESC")
    Page<Flashcard> findByUserIdAndTopicContainingIgnoreCase(@Param("userId") UUID userId, @Param("topic") String topic, Pageable pageable);
    
    @Query("SELECT COUNT(f) FROM Flashcard f WHERE f.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT f FROM Flashcard f WHERE f.user.id = :userId " +
           "AND (:search IS NULL OR LOWER(f.question) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(f.topic) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:topic IS NULL OR LOWER(f.topic) = LOWER(:topic))")
    Page<Flashcard> findFiltered(@Param("userId") UUID userId, 
                                 @Param("search") String search, 
                                 @Param("topic") String topic, 
                                 Pageable pageable);
}
