package com.mediaforge.videoflux.upload.service.cdn;

import com.mediaforge.videoflux.engine.service.cdn.CdnService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SimpleCdnService implements CdnService {
    
    @Value("${video-flux.cdn.base-url:}")
    private String cdnBaseUrl;
    
    @Value("${video-flux.cdn.api-key:}")
    private String cdnApiKey;
    
    @Override
    public void pushToCdn(String filePath, byte[] data, String contentType) {
        // Simple implementation, just log
        System.out.println("Pushing to CDN: " + filePath + " (Content-Type: " + contentType + ")");
        // In actual application, real CDN push logic would be implemented here
    }
    
    @Override
    public byte[] getFromCdn(String filePath) {
        // Simple implementation, return null to indicate file does not exist in CDN
        System.out.println("Getting from CDN: " + filePath);
        return null;
    }
    
    @Override
    public boolean existsInCdn(String filePath) {
        // Simple implementation, always return false
        System.out.println("Checking existence in CDN: " + filePath);
        return false;
    }
}