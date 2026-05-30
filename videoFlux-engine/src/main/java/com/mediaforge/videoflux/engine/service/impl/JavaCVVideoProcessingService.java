package com.mediaforge.videoflux.engine.service.impl;

import com.mediaforge.videoflux.engine.service.VideoProcessingService;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Video processing service implementation using JavaCV/FFmpeg
 */
@Service
public class JavaCVVideoProcessingService implements VideoProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaCVVideoProcessingService.class);
    
    @Override
    public List<String> convertToM3U8(File inputVideo, File outputDir, List<String> resolutions) {
        List<String> m3u8Files = new ArrayList<>();
        
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        String baseName = inputVideo.getName().substring(0, inputVideo.getName().lastIndexOf('.'));
        
        for (String resolution : resolutions) {
            try {
                String[] dimensions = resolution.replace("p", "").split("x");
                int width = Integer.parseInt(dimensions[0]);
                int height = dimensions.length > 1 ? Integer.parseInt(dimensions[1]) : (width * 9 / 16);
                
                File outputFile = new File(outputDir, baseName + "_" + resolution + ".mp4");
                convertResolution(inputVideo, outputFile, width, height);
                
                // Convert to m3u8
                File m3u8File = new File(outputDir, baseName + "_" + resolution + ".m3u8");
                convertToHLS(outputFile, m3u8File, resolution);
                
                m3u8Files.add(m3u8File.getAbsolutePath());
                
                // Clean up intermediate mp4 file
                outputFile.delete();
                
            } catch (Exception e) {
                logger.error("Error converting video to resolution: " + resolution, e);
            }
        }
        
        return m3u8Files;
    }
    
    @Override
    public void convertResolution(File inputVideo, File outputFile, int width, int height) {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        
        try {
            grabber = new FFmpegFrameGrabber(inputVideo);
            grabber.start();
            
            recorder = new FFmpegFrameRecorder(outputFile, width, height, grabber.getAudioChannels());
            recorder.setVideoCodecName("libx264");
            recorder.setVideoBitrate(grabber.getVideoBitrate());
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setAudioCodecName("aac");
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setFormat("mp4");
            
            recorder.start();
            
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }
            
            recorder.stop();
            grabber.stop();
            
        } catch (Exception e) {
            logger.error("Error converting video resolution", e);
            throw new RuntimeException("Failed to convert video resolution", e);
        } finally {
            try {
                if (recorder != null) {
                    recorder.close();
                }
                if (grabber != null) {
                    grabber.close();
                }
            } catch (Exception e) {
                logger.error("Error closing resources", e);
            }
        }
    }
    
    private void convertToHLS(File inputFile, File outputFile, String resolution) {
        try {
            String outputDir = outputFile.getParent();
            String segmentFilename = outputFile.getName().replace(".m3u8", "_%03d.ts");
            
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                "-c:v", "libx264",
                "-c:a", "aac",
                "-f", "hls",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", new File(outputDir, segmentFilename).getAbsolutePath(),
                outputFile.getAbsolutePath()
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            logger.error("Error converting to HLS", e);
            throw new RuntimeException("Failed to convert to HLS", e);
        }
    }
}
