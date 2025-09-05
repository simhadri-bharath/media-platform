package com.bharath.media_backend.controller;

import com.bharath.media_backend.api.dto.MediaCreateRequest;
import com.bharath.media_backend.api.dto.StreamUrlResponse;
import com.bharath.media_backend.domain.MediaViewLog;
import com.bharath.media_backend.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public String createMedia(@RequestBody MediaCreateRequest request) {
        return mediaService.createMedia(request);
    }

    @GetMapping("/{id}/stream-url")
    public StreamUrlResponse getStreamUrl(@PathVariable Long id) {
        return mediaService.generateStreamUrl(id);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamMedia(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) throws IOException {
        return mediaService.streamMedia(request, id, exp, sig, rangeHeader);
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getAnalytics(id));
    }

    @GetMapping("/{id}/view-log")
    public List<MediaViewLog> getViewLogs(@PathVariable Long id) {
        return mediaService.getViewLogs(id);
    }
}
