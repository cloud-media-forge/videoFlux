package com.mediaforge.videoflux.engine.repository;

import com.mediaforge.videoflux.engine.entity.VideoTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoTaskRepository extends JpaRepository<VideoTask, Long> {
    
    List<VideoTask> findByStatus(VideoTask.TaskStatus status);
    
    List<VideoTask> findByStatusOrderByCreatedAtAsc(VideoTask.TaskStatus status);
}
