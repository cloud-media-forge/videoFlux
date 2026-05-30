package com.mediaforge.videoflux.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.mediaforge.videoflux.upload",
    "com.mediaforge.videoflux.engine",
})
@EntityScan(basePackages = {
    "com.mediaforge.videoflux.upload.service.entity",
    "com.mediaforge.videoflux.engine.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.mediaforge.videoflux.upload.service.repository",
    "com.mediaforge.videoflux.engine.repository"
})
@EnableScheduling
public class VideoUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoUploadApplication.class, args);
    }
}