package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.UploadedMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedMaterialRepository extends JpaRepository<UploadedMaterial, UUID> {
    Optional<UploadedMaterial> findFirstByContentHash(String contentHash);
    List<UploadedMaterial> findAllByUserId(UUID userId);
}
