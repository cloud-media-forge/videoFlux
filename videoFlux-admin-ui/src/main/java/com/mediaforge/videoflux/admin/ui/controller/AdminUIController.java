package com.mediaforge.videoflux.admin.ui.controller;

import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.mediaforge.videoflux.admin.ui.dto.VideoInfo;
import com.mediaforge.videoflux.engine.service.storage.MinIOStorageService;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminUIController {
    @Value("${video-flux.storage.bucket.name:origin-video}")
    private String bucketName;
    
    @Autowired
    private MinIOStorageService minIOStorageService;

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(302).header("Location", "/admin/video").build();
    }

    @GetMapping("")
    public ResponseEntity<Void> rootSlash() {
        return ResponseEntity.status(302).header("Location", "/admin/video").build();
    }

    @GetMapping("/user")
    @ResponseBody
    public ResponseEntity<?> getUser(Principal principal) {
        if (principal != null) {
            return ResponseEntity.ok().body(Map.of("username", principal.getName()));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }

    @GetMapping("/dashboard")
    @ResponseBody
    public ResponseEntity<?> dashboard(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        // Add user information to model
        if (principal != null) {
            response.put("username", principal.getName());
        }
        // Add empty video list to model
        response.put("videos", new ArrayList<>());
        response.put("images", new ArrayList<>());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/video")
    @ResponseBody
    public ResponseEntity<?> listVideos(Principal principal, @RequestParam(defaultValue = "") String path) {
        Map<String, Object> response = new HashMap<>();
        // Add user information to model
        if (principal != null) {
            response.put("username", principal.getName());
        }

        // Get video list from MinIO
        List<VideoInfo> videos = new ArrayList<>();
        try {
            MinioClient minioClient = minIOStorageService.getMinioClient();
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(path)
                    .recursive(false)
                    .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();

                // Skip if current path is not empty and object name equals path (this is the current directory itself)
                if (!path.isEmpty() && objectName.equals(path)) {
                    continue;
                }

                // Check if it is a folder (ends with /)
                boolean isFolder = objectName.endsWith("/");

                // Get display name (remove path prefix)
                String displayName = objectName;
                if (!path.isEmpty() && objectName.startsWith(path)) {
                    displayName = objectName.substring(path.length());
                }

                // If it is a folder, only keep the first level directory name
                String finalObjectName = objectName; // Save original object name for subsequent processing
                if (isFolder && !displayName.isEmpty()) {
                    if (displayName.endsWith("/")) {
                        displayName = displayName.substring(0, displayName.length() - 1);
                    }
                    // If there is still /, only take the first level
                    int slashIndex = displayName.indexOf('/');
                    if (slashIndex > 0) {
                        displayName = displayName.substring(0, slashIndex);
                        finalObjectName = path + displayName + "/";
                    }
                }

                VideoInfo videoInfo = new VideoInfo();
                videoInfo.setFileName(displayName);
                // Handle case where lastModified might be null
                try {
                    if (item.lastModified() != null) {
                        videoInfo.setLastModified(item.lastModified().toLocalDateTime());
                    } else {
                        videoInfo.setLastModified(java.time.LocalDateTime.now());
                    }
                } catch (Exception e) {
                    // If conversion fails, use current time
                    videoInfo.setLastModified(java.time.LocalDateTime.now());
                }
                videoInfo.setStorageSpace(bucketName);
                videoInfo.setFileSize(item.size());
                videoInfo.setFolder(isFolder);
                videoInfo.setFullPath(finalObjectName); // Use processed object name
                videos.add(videoInfo);
            }
        } catch (Exception e) {
            // If getting video list fails, log error and use empty list
            e.printStackTrace();
        }

        response.put("images", videos);
        response.put("videos", videos);
        response.put("currentPath", path);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/video/create-folder")
    @ResponseBody
    public ResponseEntity<String> createFolder(@RequestParam String folderName, @RequestParam(defaultValue = "") String path) {
        try {
            MinioClient minioClient = minIOStorageService.getMinioClient();
            String fullPath = path.isEmpty() ? folderName + "/" : path + folderName + "/";

            // Upload an empty object to create folder
            minioClient.putObject(
                io.minio.PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .stream(new java.io.ByteArrayInputStream(new byte[0]), 0, -1)
                    .build()
            );
            
            return ResponseEntity.ok("Folder created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to create folder: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/video/delete")
    @ResponseBody
    public ResponseEntity<String> deleteObject(@RequestParam String objectName) {
        try {
            MinioClient minioClient = minIOStorageService.getMinioClient();
            minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            
            return ResponseEntity.ok("Object deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete object: " + e.getMessage());
        }
    }
    
    @GetMapping("/video/{path}")
    @ResponseBody
    public ResponseEntity<?> viewVideo(@PathVariable String path, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        // Add user information to model
        if (principal != null) {
            response.put("username", principal.getName());
        }
        // Add video object to response (simplified implementation)
        Map<String, Object> video = new HashMap<>();
        video.put("id", path);
        video.put("fileHash", "sample-hash");
        video.put("fileName", "sample.jpg");
        video.put("width", 800);
        video.put("height", 600);
        video.put("fileSize", 102400L);
        video.put("contentType", "video/mp4");
        video.put("createdAt", "2023-01-01 12:00:00");
        video.put("filePath", "/admin/video/" + path);
        response.put("image", video);
        response.put("video", video);
        return ResponseEntity.ok(response);
    }

    // Add endpoint to view video directly
    @GetMapping("/video/**")
    public ResponseEntity<byte[]> viewVideoDirect(HttpServletRequest request) {
        try {
            // Extract video path from request path
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String path = requestUri.substring(contextPath.length() + "/admin/video/".length());

            // Download video from MinIO
            MinioClient minioClient = minIOStorageService.getMinioClient();
            InputStream stream = minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build()
            );

            // Read video data
            byte[] data = stream.readAllBytes();
            stream.close();

            // Determine content type
            String contentType = "video/mp4"; // Default value
            if (path.toLowerCase().endsWith(".m3u8")) {
                contentType = "video/m3u8";
            } else if (path.toLowerCase().endsWith(".mp4")) {
                contentType = "video/mp4";
            }

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(data.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/test")
    @ResponseBody
    public String testPageAccess() {
        return "All pages are accessible without login!";
    }
}
