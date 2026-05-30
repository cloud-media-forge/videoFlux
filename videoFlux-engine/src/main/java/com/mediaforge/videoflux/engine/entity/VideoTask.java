package com.mediaforge.videoflux.engine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "video_task")
public class VideoTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String originalVideoPath;
    
    @Column(nullable = false)
    private String serviceName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "video_task_output_paths", joinColumns = @JoinColumn(name = "video_task_id"))
    @Column(name = "output_path")
    private List<String> outputPaths;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "video_task_resolutions", joinColumns = @JoinColumn(name = "video_task_id"))
    @Column(name = "resolution")
    private List<String> resolutions;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    
    @Column(length = 1000)
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TaskStatus {
        PENDING,
        PROCESSING,
        FINISHED,
        FAILED
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOriginalVideoPath() {
        return originalVideoPath;
    }
    
    public void setOriginalVideoPath(String originalVideoPath) {
        this.originalVideoPath = originalVideoPath;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public List<String> getOutputPaths() {
        return outputPaths;
    }
    
    public void setOutputPaths(List<String> outputPaths) {
        this.outputPaths = outputPaths;
    }
    
    public List<String> getResolutions() {
        return resolutions;
    }
    
    public void setResolutions(List<String> resolutions) {
        this.resolutions = resolutions;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
    
    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
