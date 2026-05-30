# VideoFlux

[![License: GPL 3.0](https://img.shields.io/badge/License-GPL%203.0-yellow.svg)](https://fsf.org/)
[![Java Version](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

[English](README.md) | [中文](doc/README.zh-CN.md)

**VideoFlux** - High-performance video format conversion service that converts videos to m3u8 format with multiple resolutions using JavaCV/FFmpeg.

## Features

- 🚀 **Video Upload API** - Support multiple video formats (mp4, avi, mov, mkv, flv, wmv)
- 🎬 **M3U8 Conversion** - Convert videos to HLS/m3u8 streaming format
- 📐 **Multi-Resolution Support** - Generate videos in multiple resolutions (720p, 1080p, etc.)
- ⚡ **JavaCV/FFmpeg Processing** - High-performance video processing
- 🗄️ **Multiple Storage Backends** - MinIO, AWS S3, Alibaba Cloud OSS
- 🔧 **Configurable Storage Switching** - Flexible storage strategy
- 🔐 **JWT Authentication** - Secure API access control
- 🐳 **Docker Support** - Containerized deployment
- ⏰ **Scheduled Processing** - Automatic video conversion task processing

## Feature Highlights

| Feature               | This Project          |
|-----------------------|-----------------------|
| Multi-storage backend | ✅ MinIO/S3/OSS        |
| M3U8 conversion       | ✅ JavaCV/FFmpeg       |
| Multi-resolution      | ✅ 720p/1080p/etc      |
| Management UI         | ✅ Built-in Admin UI   |
| Modular design        | ✅ Independent modules |
| Docker support        | ✅ Ready to use        |
| JWT auth              | ✅ Built-in            |
| Scheduled tasks       | ✅ Automatic processing|

## VideoFlux Roadmap

We're planning to add more video processing features. See [Wiki](../../wiki) for details:

| Feature                    | Description                                | Status         |
|----------------------------|--------------------------------------------|----------------|
| M3U8 conversion            | Convert videos to HLS/m3u8 format         | ✅ Support      |
| Multi-resolution           | Generate videos in multiple resolutions    | ✅ Support      |
| Video compression          | Compress videos while maintaining quality  | 📋 Planned     |
| Video thumbnail generation | Generate thumbnails from videos            | 📋 Planned     |
| Video metadata extraction  | Extract video metadata and information     | 📋 Planned     |
| Video watermarking         | Add watermarks to videos                   | 📋 Planned     |
| Video concatenation        | Merge multiple videos                      | 📋 Planned     |
| Video cropping             | Crop videos to specified dimensions        | 📋 Planned     |
| Audio extraction           | Extract audio from videos                  | 📋 Planned     |
| Subtitle support          | Add subtitles to videos                    | 📋 Planned     |

## Use Cases

- **Video streaming platforms** - Convert videos to HLS format for adaptive streaming
- **Content delivery networks** - Generate multiple resolution versions for bandwidth optimization
- **Video hosting services** - Automated video processing and conversion
- **E-commerce** - Product video optimization and conversion
- **Social media** - Video format conversion for different platforms
- **Video management** - Digital asset management for media companies

## Project Structure

```
videoFlux/
├── videoFlux-engine/     # Video processing library
├── videoFlux-upload-api/ # Video upload REST API module
├── videoFlux-admin-ui/   # Admin UI module
```

## Quick Start

### Docker Compose (Recommended)

```bash
git clone
cd videoFlux
docker-compose up -d
# Access: http://localhost:8080
```

### Manual Start

```bash
mvn clean install
mvn spring-boot:run -pl videoFlux-upload-api
mvn spring-boot:run -pl videoFlux-admin-ui
```

## API Usage

### Upload Video for Conversion

```bash
curl -X POST http://localhost:8080/api/v1/upload \
  -F "file=@video.mp4" \
  -F "serviceName=my-service" \
  -F "resolutions=720p,1080p"
```

Response:
```json
{
  "success": true,
  "message": "Video uploaded successfully. Task ID: 1",
  "taskId": 1
}
```

## Tech Stack

- **Spring Boot 3.5** - Application framework
- **Java 17** - Programming language
- **JavaCV/FFmpeg** - Video processing engine
- **MinIO / AWS S3 / Alibaba Cloud OSS** - Object storage
- **JWT** - Authentication & authorization
- **H2 Database** - Embedded database
- **Thymeleaf** - Template engine

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](doc/CONTRIBUTING.md) for details.

## License

This project is licensed under the [GPL License](LICENSE).

## Contact

- Issue reporting: [GitHub Issues](https://github.com/cloud-media-forge/videoFlux/issues)