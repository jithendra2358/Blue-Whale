// controller/DownloadController.java
package com.bluewhale.controller;

import com.bluewhale.service.VideoService;
import com.bluewhale.storage.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final VideoService videoService;
    private final FileStorageService storage;

    public DownloadController(VideoService videoService, FileStorageService storage) {
        this.videoService = videoService;
        this.storage = storage;
    }

    /**
     * Stream video for the frontend (also used as poster)
     * GET /api/videos/blue-whale-demo.mp4
     */
    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String filename) {
        Path path = storage.videoPath(filename);
        if (!path.toFile().exists()) return ResponseEntity.notFound().build();
        videoService.incrementDownload(filename);
        Resource resource = new FileSystemResource(path);
        String contentType = filename.endsWith(".mp4") ? "video/mp4" : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Download the immutable, verified APK.
     *
     * The APK must not be copied, mutated, or cleaned up while a response is
     * streaming. Those operations can cause a browser to receive fewer bytes
     * than the advertised Content-Length and report a damaged package.
     * GET /api/download/apk
     */
    @GetMapping("/download/apk")
    public ResponseEntity<Resource> downloadApk() {
        try {
            Path apk = storage.apkPath("GameApp.apk");
            if (!java.nio.file.Files.isRegularFile(apk)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(apk);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                    .contentLength(java.nio.file.Files.size(apk))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"BlueWhale.apk\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300, no-transform")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
