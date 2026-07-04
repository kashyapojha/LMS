package com.geeknito.LMS_backend.controller;

import com.geeknito.LMS_backend.response.ApiResponse;
import com.geeknito.LMS_backend.service.CloudinaryService;
import com.geeknito.LMS_backend.cache.RedisService;
import com.geeknito.LMS_backend.entity.media.MediaEntity;
import com.geeknito.LMS_backend.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaUploadController {

    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private RedisService redisService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(new ApiResponse("File is empty", null), HttpStatus.BAD_REQUEST);
        }

        try {
            System.out.println("[MediaUploadController] Received file upload request: " + file.getOriginalFilename() + " (size: " + file.getSize() + ")");
            // Check if Cloudinary is configured
            boolean isCloudinaryConfigured = cloudinaryService != null && cloudinaryService.isConfigured();
            System.out.println("[MediaUploadController] Cloudinary configured status: " + isCloudinaryConfigured);
            
            if (isCloudinaryConfigured) {
                try {
                    Map<String, Object> uploadResult = cloudinaryService.uploadFile(file);
                    System.out.println("[MediaUploadController] Cloudinary raw upload result: " + uploadResult);
                    
                    if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                        Map<String, Object> data = new HashMap<>();
                        String secureUrl = (String) uploadResult.get("secure_url");
                        System.out.println("[MediaUploadController] Cloudinary secure_url: " + secureUrl);

                        long size = file.getSize();
                        Object bytesObj = uploadResult.get("bytes");
                        if (bytesObj instanceof Number) {
                            size = ((Number) bytesObj).longValue();
                        }

                        // Persist media metadata to DB
                        try {
                            MediaEntity media = MediaEntity.builder()
                                .name(file.getOriginalFilename())
                                .url(secureUrl)
                                .size(size)
                                .mimeType(file.getContentType())
                                .storageProvider("cloudinary")
                                .build();
                            MediaEntity saved = mediaRepository.save(media);
                            // Cache in Redis for quick lookup
                            redisService.set("media_" + saved.getId(), saved, 60L);

                            data.put("id", saved.getId());
                        } catch (Exception ex) {
                            System.err.println("[MediaUploadController] Failed to persist media metadata: " + ex.getMessage());
                        }

                        // Keep the full Cloudinary secureUrl so that it can be resolved correctly on frontend
                        data.put("url", secureUrl);
                        data.put("name", file.getOriginalFilename());
                        data.put("size", size);

                        System.out.println("[MediaUploadController] Successfully uploaded to Cloudinary. Path: " + secureUrl);
                        return new ResponseEntity<>(new ApiResponse("Upload successful (Cloudinary)", data), HttpStatus.OK);
                    } else {
                        System.out.println("[MediaUploadController] Cloudinary uploadResult was null or missing secure_url!");
                    }
                } catch (Exception ex) {
                    System.err.println("[MediaUploadController] Cloudinary upload method threw an exception:");
                    ex.printStackTrace();
                }
            }

            // Fallback to local storage if Cloudinary is not configured or fails
            System.out.println("[MediaUploadController] Falling back to local disk storage upload.");
            // Ensure uploads directory exists
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create a unique filename
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);

            // Copy file content
            Files.copy(file.getInputStream(), filePath);

            // Prepare response data
            Map<String, Object> data = new HashMap<>();
            String returnedUrl = "/uploads/" + uniqueFileName;
            data.put("url", returnedUrl);
            data.put("name", originalFileName);
            data.put("size", file.getSize());

            try {
                MediaEntity media = MediaEntity.builder()
                    .name(originalFileName)
                    .url(returnedUrl)
                    .size(file.getSize())
                    .mimeType(file.getContentType())
                    .storageProvider("local")
                    .build();
                MediaEntity saved = mediaRepository.save(media);
                redisService.set("media_" + saved.getId(), saved, 60L);
                data.put("id", saved.getId());
            } catch (Exception ex) {
                System.err.println("[MediaUploadController] Failed to persist local media metadata: " + ex.getMessage());
            }

            return new ResponseEntity<>(new ApiResponse("Upload successful (Local Fallback)", data), HttpStatus.OK);

        } catch (IOException e) {
            return new ResponseEntity<>(new ApiResponse("Failed to save file: " + e.getMessage(), null), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/upload-by-url")
    public ResponseEntity<ApiResponse> uploadByUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.trim().isEmpty()) {
            return new ResponseEntity<>(new ApiResponse("URL is required", null), HttpStatus.BAD_REQUEST);
        }

        try {
            boolean isCloudinaryConfigured = cloudinaryService != null && cloudinaryService.isConfigured();
            if (isCloudinaryConfigured) {
                try {
                    Map<String, Object> uploadResult = cloudinaryService.uploadFromUrl(url);
                    if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                        Map<String, Object> data = new HashMap<>();
                        String secureUrl = (String) uploadResult.get("secure_url");
                        data.put("url", secureUrl);
                        data.put("name", url);
                        Object bytesObj = uploadResult.get("bytes");
                        long size = 0L;
                        if (bytesObj instanceof Number) {
                            size = ((Number) bytesObj).longValue();
                        }
                        data.put("size", size);

                        try {
                            MediaEntity media = MediaEntity.builder()
                                .name(url)
                                .url(secureUrl)
                                .size(size)
                                .mimeType((String) uploadResult.getOrDefault("format", ""))
                                .storageProvider("cloudinary")
                                .build();
                            MediaEntity saved = mediaRepository.save(media);
                            redisService.set("media_" + saved.getId(), saved, 60L);
                            data.put("id", saved.getId());
                        } catch (Exception ex) {
                            System.err.println("[MediaUploadController] uploadByUrl: failed to persist media: " + ex.getMessage());
                        }

                        return new ResponseEntity<>(new ApiResponse("Upload by URL successful (Cloudinary)", data), HttpStatus.OK);
                    } else {
                        System.out.println("[MediaUploadController] uploadByUrl: Cloudinary did not return secure_url");
                    }
                } catch (Exception ex) {
                    System.err.println("[MediaUploadController] uploadByUrl: Cloudinary uploadFromUrl threw exception:");
                    ex.printStackTrace();
                }
            }

            // Fallback: persist the original URL as an external resource and return it
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            data.put("name", url);
            data.put("size", 0);

            try {
                MediaEntity media = MediaEntity.builder()
                    .name(url)
                    .url(url)
                    .size(0L)
                    .mimeType("")
                    .storageProvider("external")
                    .build();
                MediaEntity saved = mediaRepository.save(media);
                redisService.set("media_" + saved.getId(), saved, 60L);
                data.put("id", saved.getId());
            } catch (Exception ex) {
                System.err.println("[MediaUploadController] uploadByUrl fallback: failed to persist external url: " + ex.getMessage());
            }

            return new ResponseEntity<>(new ApiResponse("Upload by URL fallback: returned original URL", data), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse("Failed to upload by URL: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
