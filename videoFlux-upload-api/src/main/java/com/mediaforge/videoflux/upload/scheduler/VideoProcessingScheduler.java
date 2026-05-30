package com.mediaforge.videoflux.upload.scheduler;

import com.mediaforge.videoflux.engine.entity.VideoTask;
import com.mediaforge.videoflux.engine.repository.VideoTaskRepository;
import com.mediaforge.videoflux.engine.service.VideoProcessingService;
import com.mediaforge.videoflux.engine.service.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class VideoProcessingScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingScheduler.class);
    
    @Autowired
    private VideoTaskRepository videoTaskRepository;
    
    @Autowired
    private VideoProcessingService videoProcessingService;
    
    @Autowired
    private ObjectStorageService objectStorageService;
    
    @Value("${video-flux.storage.options.minio.bucket}")
    private String bucketName;
    
    @Value("${video-flux.processing.temp-dir:/tmp/videoFlux}")
    private String tempDir;
    
    @Value("${video-flux.storage.options.minio.bucket}")
    private String outputBucketName;
    
    /**
     * Scheduled task to process PENDING video tasks every minute
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void processPendingVideoTasks() {
        try {
            List<VideoTask> pendingTasks = videoTaskRepository.findByStatusOrderByCreatedAtAsc(VideoTask.TaskStatus.PENDING);
            
            if (pendingTasks.isEmpty()) {
                logger.debug("No pending video tasks to process");
                return;
            }
            
            logger.info("Found {} pending video tasks to process", pendingTasks.size());
            
            for (VideoTask task : pendingTasks) {
                processVideoTask(task);
            }
            
        } catch (Exception e) {
            logger.error("Error processing pending video tasks", e);
        }
    }
    
    private void processVideoTask(VideoTask task) {
        try {
            logger.info("Processing video task ID: {}", task.getId());
            
            // Update status to PROCESSING
            task.setStatus(VideoTask.TaskStatus.PROCESSING);
            videoTaskRepository.save(task);
            
            // Create temp directory for this task
            Path taskTempDir = Paths.get(tempDir, "task_" + task.getId());
            Files.createDirectories(taskTempDir);
            
            // Download original video from object storage
            String originalVideoPath = task.getOriginalVideoPath();
            File downloadedVideo = downloadVideoFromStorage(originalVideoPath, taskTempDir);
            
            // Process video to m3u8 with specified resolutions
            File outputDir = taskTempDir.resolve("output").toFile();
            Files.createDirectories(outputDir.toPath());
            
            List<String> m3u8Files = videoProcessingService.convertToM3U8(
                downloadedVideo, outputDir, task.getResolutions());
            
            // Upload converted files to object storage
            List<String> outputPaths = uploadConvertedFiles(outputDir, task.getId());
            
            // Update task with output paths and status
            task.setOutputPaths(outputPaths);
            task.setStatus(VideoTask.TaskStatus.FINISHED);
            task.setFinishedAt(LocalDateTime.now());
            videoTaskRepository.save(task);
            
            logger.info("Successfully processed video task ID: {}", task.getId());
            
            // Clean up temp files
            cleanupTempFiles(taskTempDir);
            
        } catch (Exception e) {
            logger.error("Error processing video task ID: " + task.getId(), e);
            
            // Update task with error status
            task.setStatus(VideoTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            videoTaskRepository.save(task);
        }
    }
    
    private File downloadVideoFromStorage(String objectName, Path tempDir) throws IOException {
        byte[] videoData = objectStorageService.downloadFile(bucketName, objectName);
        
        String fileName = objectName.substring(objectName.lastIndexOf("/") + 1);
        File outputFile = tempDir.resolve(fileName).toFile();
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(videoData);
        }
        
        return outputFile;
    }
    
    private List<String> uploadConvertedFiles(File outputDir, Long taskId) throws IOException {
        List<String> outputPaths = new java.util.ArrayList<>();
        
        File[] files = outputDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String objectName = "converted/" + taskId + "/" + file.getName();
                    byte[] fileData = Files.readAllBytes(file.toPath());
                    String contentType = getContentType(file.getName());
                    
                    objectStorageService.uploadFile(outputBucketName, objectName, fileData, contentType);
                    outputPaths.add(objectName);
                }
            }
        }
        
        return outputPaths;
    }
    
    private void cleanupTempFiles(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                
                logger.debug("Cleaned up temp directory: {}", tempDir);
            }
        } catch (IOException e) {
            logger.warn("Failed to clean up temp directory: " + tempDir, e);
        }
    }
    
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "m3u8":
                return "application/vnd.apple.mpegurl";
            case "ts":
                return "video/mp2t";
            case "mp4":
                return "video/mp4";
            default:
                return "application/octet-stream";
        }
    }
}
