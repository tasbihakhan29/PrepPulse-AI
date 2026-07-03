package com.preppulse_ai.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class SupabaseStorageService {

    private static final String LOCAL_PREFIX = "local://";
    private static final Path LOCAL_STORAGE_DIR = Paths.get(
            System.getProperty("java.io.tmpdir"), "preppulse-answers"
    );

    @Value("${app.supabase.url:https://unrrnznznfxbthveeets.supabase.co}")
    private String supabaseUrl;

    @Value("${app.supabase.service.role.key:}")
    private String supabaseKey;

    @Value("${app.supabase.bucket.name:answer-submissions}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Uploads bytes to Supabase Storage, or to local temp storage when Supabase is not configured.
     */
    public String uploadFile(UUID userId, String originalFilename, byte[] fileBytes, String contentType) {
        if (!isSupabaseConfigured()) {
            log.warn("Supabase is not configured. Storing answer file locally for development.");
            return saveToLocalStorage(userId, originalFilename, fileBytes);
        }

        String uniqueFileName = userId + "/" + UUID.randomUUID() + "_" + originalFilename;
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + uniqueFileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("apikey", supabaseKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("File uploaded successfully to Supabase Storage: {}", uniqueFileName);
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + uniqueFileName;
        }

        log.error("Failed to upload to Supabase, status code: {}", response.getStatusCode());
        throw new RuntimeException("Supabase upload failed: " + response.getBody());
    }

    /**
     * Downloads file bytes for evaluation. Supports local dev storage and Supabase URLs.
     */
    public byte[] downloadFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new RuntimeException("Answer file URL is missing.");
        }

        if (fileUrl.startsWith(LOCAL_PREFIX)) {
            return readLocalFile(fileUrl);
        }

        if (fileUrl.startsWith("mock://")) {
            throw new RuntimeException(
                    "Answer file was not stored. Re-upload after configuring SUPABASE_SERVICE_ROLE_KEY."
            );
        }

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    fileUrl,
                    HttpMethod.GET,
                    null,
                    byte[].class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Public download failed for {}, trying authenticated download: {}", fileUrl, e.getMessage());
        }

        if (isSupabaseConfigured()) {
            return downloadFromSupabase(fileUrl);
        }

        throw new RuntimeException("Failed to download answer file from storage.");
    }

    private boolean isSupabaseConfigured() {
        return supabaseKey != null
                && !supabaseKey.trim().isEmpty()
                && !supabaseKey.contains("your_supabase");
    }

    private String saveToLocalStorage(UUID userId, String originalFilename, byte[] fileBytes) {
        try {
            Path dir = LOCAL_STORAGE_DIR.resolve(userId.toString());
            Files.createDirectories(dir);
            String uniqueName = UUID.randomUUID() + "_" + originalFilename;
            Path filePath = dir.resolve(uniqueName);
            Files.write(filePath, fileBytes);
            log.info("File saved to local storage: {}", filePath);
            return LOCAL_PREFIX + filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save answer file locally", e);
        }
    }

    private byte[] readLocalFile(String fileUrl) {
        try {
            Path path = Paths.get(fileUrl.substring(LOCAL_PREFIX.length()));
            if (!Files.exists(path)) {
                throw new RuntimeException("Local answer file not found: " + path);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read local answer file", e);
        }
    }

    private byte[] downloadFromSupabase(String fileUrl) {
        String publicPrefix = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/";
        String objectPath;

        if (fileUrl.startsWith(publicPrefix)) {
            objectPath = fileUrl.substring(publicPrefix.length());
        } else {
            String privatePrefix = supabaseUrl + "/storage/v1/object/" + bucketName + "/";
            if (fileUrl.startsWith(privatePrefix)) {
                objectPath = fileUrl.substring(privatePrefix.length());
            } else {
                throw new RuntimeException("Cannot parse Supabase file URL: " + fileUrl);
            }
        }

        String downloadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("apikey", supabaseKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        throw new RuntimeException("Failed to download answer file from Supabase.");
    }
}
