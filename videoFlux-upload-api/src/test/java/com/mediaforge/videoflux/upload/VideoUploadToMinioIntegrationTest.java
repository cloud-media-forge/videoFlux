package com.mediaforge.videoflux.upload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediaforge.videoflux.engine.entity.VideoTask;
import com.mediaforge.videoflux.engine.repository.VideoTaskRepository;
import com.mediaforge.videoflux.upload.scheduler.VideoProcessingScheduler;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = VideoUploadApplication.class)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class VideoUploadToMinioIntegrationTest {

    private static final int MINIO_PORT = 9000;
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "videoflux-it-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT);

    @Container
    private static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
            .withCommand("server /data")
            .withExposedPorts(MINIO_PORT)
            .waitingFor(Wait.forHttp("/minio/health/ready")
                    .forPort(MINIO_PORT)
                    .withStartupTimeout(Duration.ofSeconds(90)));

    @TempDir
    Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VideoProcessingScheduler videoProcessingScheduler;

    @Autowired
    private VideoTaskRepository videoTaskRepository;

    @Value("${video-flux.storage.options.minio.bucket}")
    private String bucketName;

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("video-flux.storage.type", () -> "minio");
        registry.add("video-flux.storage.options.minio.endpoint", VideoUploadToMinioIntegrationTest::minioEndpoint);
        registry.add("video-flux.storage.options.minio.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("video-flux.storage.options.minio.secret-key", () -> MINIO_SECRET_KEY);
        registry.add("video-flux.storage.options.minio.bucket", () -> BUCKET_NAME);
        registry.add("video-flux.processing.temp-dir", () -> "target/video-processing-it");
        registry.add("video-flux.video.supported-formats", () -> "mp4");
    }

    @AfterEach
    void cleanMinioBucket() throws Exception {
        MinioClient minioClient = minioClient();
        try {
            if (!bucketExists(minioClient)) {
                return;
            }

            List<DeleteObject> objects = new ArrayList<>();
            for (Result<Item> result : minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder().bucket(bucketName).recursive(true).build())) {
                objects.add(new DeleteObject(result.get().objectName()));
            }

            for (Result<io.minio.messages.DeleteError> ignored : minioClient.removeObjects(
                    RemoveObjectsArgs.builder().bucket(bucketName).objects(objects).build())) {
                ignored.get();
            }
        } catch (Exception ignored) {
            // Cleanup is best-effort because this integration test may be skipped when MinIO is unavailable.
        }
    }

    @Test
    void uploadsMp4ProcessesHlsAndStoresM3u8InMinio() throws Exception {
        assumeFfmpegIsAvailable();
        MinioClient minioClient = minioClient();
        createBucketIfMissing(minioClient);

        File mp4File = createRealMp4(tempDir.resolve("sample.mp4"));
        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "sample.mp4",
                "video/mp4",
                Files.readAllBytes(mp4File.toPath()));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/upload")
                        .file(upload)
                        .param("serviceName", "integration-test")
                        .param("resolutions", "160x90"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        assertTrue(response.get("success").asBoolean());
        long taskId = response.get("taskId").asLong();

        videoProcessingScheduler.processPendingVideoTasks();

        VideoTask task = videoTaskRepository.findById(taskId).orElseThrow();
        assertEquals(VideoTask.TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
        assertNotNull(task.getOutputPaths());
        assertFalse(task.getOutputPaths().isEmpty());

        assertTrue(task.getOutputPaths().stream().anyMatch(path -> path.endsWith(".m3u8")));
        assertTrue(task.getOutputPaths().stream().anyMatch(path -> path.endsWith(".ts")));

        for (String outputPath : task.getOutputPaths()) {
            assertTrue(objectExists(minioClient, outputPath), "Expected object in MinIO: " + outputPath);
        }

        String m3u8Object = task.getOutputPaths().stream()
                .filter(path -> path.endsWith(".m3u8"))
                .findFirst()
                .orElseThrow();
        String playlist = new String(minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(m3u8Object).build()).readAllBytes());

        assertTrue(playlist.startsWith("#EXTM3U"));
        assertTrue(playlist.contains(".ts"));

        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(task.getOriginalVideoPath())
                .build());
    }

    private File createRealMp4(Path outputPath) throws Exception {
        int width = 160;
        int height = 90;
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath.toFile(), width, height)) {
            recorder.setFormat("mp4");
            recorder.setVideoCodecName("libx264");
            recorder.setFrameRate(24);
            recorder.setVideoBitrate(400_000);
            recorder.start();

            for (int i = 0; i < 48; i++) {
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(new Color((i * 5) % 255, 80, 180));
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(Color.WHITE);
                graphics.fillRect(i % width, 20, 24, 24);
                graphics.dispose();

                Frame frame = converter.convert(image);
                recorder.record(frame);
            }
        }

        assertTrue(Files.size(outputPath) > 0);
        return outputPath.toFile();
    }

    private void assumeFfmpegIsAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            Assumptions.assumeTrue(process.waitFor() == 0, "ffmpeg is required for HLS conversion");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "ffmpeg is required for HLS conversion");
        }
    }

    private void createBucketIfMissing(MinioClient minioClient) throws Exception {
        if (!bucketExists(minioClient)) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private boolean bucketExists(MinioClient minioClient) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    private boolean objectExists(MinioClient minioClient, String objectName) {
        try {
            minioClient.statObject(io.minio.StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint())
                .credentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
                .build();
    }

    private static String minioEndpoint() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(MINIO_PORT);
    }
}
