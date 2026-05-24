package com.lms.member.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired private S3Client s3Client;
    @Value("${aws.s3.bucket-name}") private String bucketName;
    @Value("${aws.s3.base-url}")    private String baseUrl;

    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);
        String key = folder + "/" + UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build(),
                RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage());
        }
        return baseUrl + "/" + key;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        String key = fileUrl.replace(baseUrl + "/", "");
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucketName).key(key).build());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File must not be empty");
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize)
            throw new IllegalArgumentException("File size must not exceed 5MB");
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf")))
            throw new IllegalArgumentException("Only image files and PDFs are allowed");
    }

    private String sanitizeFilename(String original) {
        if (original == null) return "file";
        return original.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
