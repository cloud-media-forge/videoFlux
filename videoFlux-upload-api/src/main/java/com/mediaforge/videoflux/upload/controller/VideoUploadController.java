package com.mediaforge.videoflux.upload.controller;

import com.mediaforge.videoflux.engine.entity.VideoTask;
import com.mediaforge.videoflux.engine.repository.VideoTaskRepository;
import com.mediaforge.videoflux.engine.service.storage.ObjectStorageService;
import com.mediaforge.videoflux.engine.service.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/upload")
public class VideoUploadController {
    
    @Autowired
    private ObjectStorageService objectStorageService;
    
    @Autowired
    private VideoTaskRepository videoTaskRepository;
    
    @Value("${video-flux.video.max-size:104857600}") // 100MB default
    private long maxFileSize;
    
    @Value("${video-flux.storage.options.minio.bucket}")
    private String bucketName;
    
    @Value("${video-flux.video.supported-formats:mp4,avi,mov,mkv,flv,wmv}")
    private String supportedFormats;
    
    /**
     * Upload video and create processing task
     */
    @PostMapping
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serviceName") String serviceName,
            @RequestParam("resolutions") List<String> resolutions) throws IOException {
        
        try {
            // Validate file size
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.badRequest().body(
                    new VideoUploadResponse(false, "File size exceeds maximum allowed size of " + maxFileSize + " bytes", null));
            }

            // Validate file format
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new VideoUploadResponse(false, "Invalid file name", null));
            }
            
            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            if (!getSupportedFormats().contains(fileExtension)) {
                return ResponseEntity.badRequest().body(
                    new VideoUploadResponse(false, "Unsupported file format: " + fileExtension, null));
            }

            // Read file data
            byte[] videoData = file.getBytes();

            // Calculate hash value
            String hash = HashUtil.calculateHash(videoData);
            String folderName = hash.substring(0, 4);
            String fileName = hash.substring(4) + "." + fileExtension;
            String objectName = folderName + "/" + fileName;

            // Upload to object storage
            String contentType = getContentType(fileExtension);
            objectStorageService.uploadFile(bucketName, objectName, videoData, contentType);

            // Create video task
            VideoTask videoTask = new VideoTask();
            videoTask.setOriginalVideoPath(objectName);
            videoTask.setServiceName(serviceName);
            videoTask.setResolutions(resolutions);
            videoTask.setStatus(VideoTask.TaskStatus.PENDING);
            
            videoTask = videoTaskRepository.save(videoTask);
            
            return ResponseEntity.ok(
                new VideoUploadResponse(true, "Video uploaded successfully. Task ID: " + videoTask.getId(), videoTask.getId()));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new VideoUploadResponse(false, "Failed to upload video: " + e.getMessage(), null));
        }
    }
    
    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    /**
     * Get content type based on format
     */
    private String getContentType(String format) {
        switch (format.toLowerCase()) {
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "mkv":
                return "video/x-matroska";
            case "flv":
                return "video/x-flv";
            case "wmv":
                return "video/x-ms-wmv";
            default:
                return "video/mp4";
        }
    }
    
    private List<String> getSupportedFormats() {
        return List.of(supportedFormats.split(","));
    }
    
    public static class VideoUploadResponse {
        private boolean success;
        private String message;
        private Long taskId;
        
        public VideoUploadResponse(boolean success, String message, Long taskId) {
            this.success = success;
            this.message = message;
            this.taskId = taskId;
        }
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Long getTaskId() {
            return taskId;
        }
        
        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }
    }
}
