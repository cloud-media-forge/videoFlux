# VideoFlux

[![License: GPL 3.0](https://img.shields.io/badge/License-GPL%203.0-yellow.svg)](https://fsf.org/)
[![Java Version](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

[English](../README.md) | [中文](README.zh-CN.md)

**VideoFlux** - 高性能视频格式转换服务，使用 JavaCV/FFmpeg 将视频转换为 m3u8 格式并支持多种分辨率。

## 功能特性

- 🚀 **视频上传API** - 支持多种视频格式（mp4, avi, mov, mkv, flv, wmv）
- 🎬 **M3U8转换** - 将视频转换为 HLS/m3u8 流媒体格式
- 📐 **多分辨率支持** - 生成多种分辨率的视频（720p, 1080p 等）
- ⚡ **JavaCV/FFmpeg处理** - 高性能视频处理
- 🗄️ **支持多种对象存储服务** - MinIO、AWS S3、阿里云OSS
- 🔧 **可配置的存储服务切换** - 灵活的存储策略
- 🔐 **JWT认证** - 安全的API访问控制
- 🐳 **Docker支持** - 容器化部署
- ⏰ **定时处理** - 自动视频转换任务处理

## 功能亮点

| 功能       | 本项目              |
|----------|------------------|
| 多存储后端支持  | ✅ MinIO/S3/OSS   |
| M3U8转换   | ✅ JavaCV/FFmpeg  |
| 多分辨率     | ✅ 720p/1080p等   |
| 管理界面     | ✅ 内置Admin UI   |
| 模块化设计    | ✅ 独立模块           |
| Docker支持 | ✅ 开箱即用           |
| JWT认证    | ✅ 内置             |
| 定时任务     | ✅ 自动处理           |

## VideoFlux 功能规划

我们计划添加更多视频处理功能。

| 功能       | 描述             | 状态     |
|----------|----------------|--------|
| M3U8转换    | 将视频转换为HLS/m3u8格式 | ✅ 支持   |
| 多分辨率     | 生成多种分辨率的视频    | ✅ 支持   |
| 视频压缩     | 在保持质量的同时压缩视频  | 📋 计划中 |
| 视频缩略图生成  | 从视频生成缩略图       | 📋 计划中 |
| 视频元数据提取  | 提取视频元数据和信息     | 📋 计划中 |
| 视频水印     | 给视频添加水印        | 📋 计划中 |
| 视频拼接     | 合并多个视频         | 📋 计划中 |
| 视频裁剪     | 将视频裁剪到指定尺寸     | 📋 计划中 |
| 音频提取     | 从视频中提取音频       | 📋 计划中 |
| 字幕支持     | 给视频添加字幕        | 📋 计划中 |

## 使用场景

- **视频流媒体平台** - 将视频转换为 HLS 格式以支持自适应流媒体
- **内容分发网络** - 生成多种分辨率版本以优化带宽
- **视频托管服务** - 自动化视频处理和转换
- **电商** - 产品视频优化和转换
- **社交媒体** - 为不同平台转换视频格式
- **视频管理** - 媒体公司的数字资产管理

## 项目结构

```
videoFlux/
├── videoFlux-engine/     # 视频处理公共库
├── videoFlux-upload-api/ # 视频上传REST API模块
├── videoFlux-admin-ui/   # Admin UI模块
```

## 快速开始

### Docker Compose（推荐）

```bash
git clone
cd videoFlux
docker-compose up -d
# 访问: http://localhost:8080
```

### 手动启动

```bash
mvn clean install
mvn spring-boot:run -pl videoFlux-upload-api
mvn spring-boot:run -pl videoFlux-admin-ui
```

## API 使用

### 上传视频进行转换

```bash
curl -X POST http://localhost:8080/api/v1/upload \
  -F "file=@video.mp4" \
  -F "serviceName=my-service" \
  -F "resolutions=720p,1080p"
```

响应：
```json
{
  "success": true,
  "message": "Video uploaded successfully. Task ID: 1",
  "taskId": 1
}
```

## 技术栈

- **Spring Boot 3.5** - 应用框架
- **Java 17** - 编程语言
- **JavaCV/FFmpeg** - 视频处理引擎
- **MinIO / AWS S3 / 阿里云OSS** - 对象存储
- **JWT** - 认证授权
- **H2 Database** - 嵌入式数据库
- **Thymeleaf** - 模板引擎

## 贡献

欢迎贡献！请查看 [CONTRIBUTING.md](../doc/CONTRIBUTING.md) 了解详情。

## 许可证

本项目采用 [GPL 许可证](../LICENSE)。

## 联系方式

- 问题反馈：[GitHub Issues](https://github.com/cloud-media-forge/videoFlux/issues)
