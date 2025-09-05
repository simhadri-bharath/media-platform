package com.bharath.media_backend.controller;

import com.bharath.media_backend.api.dto.MediaCreateRequest;
import com.bharath.media_backend.api.dto.StreamUrlResponse;
import com.bharath.media_backend.domain.MediaViewLog;
import com.bharath.media_backend.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    // --- Create media (upload to Cloudinary + save record) ---
    @PostMapping
    public String createMedia(
            @RequestParam("title") String title,
            @RequestParam("type") String type,   // video/audio
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        // Create a MediaCreateRequest
        MediaCreateRequest request = new MediaCreateRequest();
        request.setTitle(title);
        request.setType(type);

        // Upload file to Cloudinary
        String cloudUrl = mediaService.uploadToCloudinary(file);
        request.setFileUrl(cloudUrl);

        // Save record
        return mediaService.createMedia(request);
    }

    // --- Generate stream URL ---
    @GetMapping("/{id}/stream-url")
    public StreamUrlResponse getStreamUrl(@PathVariable Long id) {
        return mediaService.generateStreamUrl(id);
    }

    // --- Stream media (redirect to Cloudinary) ---
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamMedia(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig
    ) throws IOException {
        return mediaService.streamMedia(request, id, exp, sig, null);
    }

    // --- Analytics ---
    @GetMapping("/{id}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable Long id) {
        Map<String, Object> analytics = mediaService.getAnalytics(id);
        HttpHeaders headers = new HttpHeaders();
        if (analytics.containsKey("cache_status")) {
            headers.add("X-Cache-Status", analytics.get("cache_status").toString());
            analytics.remove("cache_status");
        }
        return ResponseEntity.ok().headers(headers).body(analytics);
    }

    // --- View logs ---
    @GetMapping("/{id}/view-log")
    public List<MediaViewLog> getViewLogs(@PathVariable Long id) {
        return mediaService.getViewLogs(id);
    }

    // --- Record view ---
    @PostMapping("/{id}/view")
    public ResponseEntity<String> viewMedia(@PathVariable Long id, HttpServletRequest request) {
        return mediaService.recordView(id, request);
    }
}
