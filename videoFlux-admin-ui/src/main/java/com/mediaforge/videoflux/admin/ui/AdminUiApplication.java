package com.mediaforge.videoflux.admin.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.mediaforge.videoflux.admin.ui", 
    "com.mediaforge.videoflux.upload", 
    "com.mediaforge.videoflux.engine",
})
@EntityScan(basePackages = "com.mediaforge.videoflux.engine.entity")
@EnableJpaRepositories(basePackages = "com.mediaforge.videoflux.engine.repository")
public class AdminUiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AdminUiApplication.class, args);
    }
}