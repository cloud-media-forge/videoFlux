package com.mediaforge.videoflux.engine.service;

import java.io.File;
import java.util.List;

/**
 * Video processing service interface for video format conversion
 */
public interface VideoProcessingService {
    
    /**
     * Convert video to m3u8 format with specified resolutions
     * 
     * @param inputVideo Input video file
     * @param outputDir Output directory for m3u8 files
     * @param resolutions List of resolutions (e.g., "720p", "1080p")
     * @return List of paths to generated m3u8 files
     */
    List<String> convertToM3U8(File inputVideo, File outputDir, List<String> resolutions);
    
    /**
     * Convert video to specified resolution
     * 
     * @param inputVideo Input video file
     * @param outputFile Output video file
     * @param width Target width
     * @param height Target height
     */
    void convertResolution(File inputVideo, File outputFile, int width, int height);
}
