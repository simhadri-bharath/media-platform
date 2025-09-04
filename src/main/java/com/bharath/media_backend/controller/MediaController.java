package com.bharath.media_backend.controller;

import com.bharath.media_backend.api.dto.MediaCreateRequest;
import com.bharath.media_backend.api.dto.StreamUrlResponse;
import com.bharath.media_backend.domain.MediaAsset;
import com.bharath.media_backend.domain.MediaViewLog;
import com.bharath.media_backend.exception.MediaNotFoundException;
import com.bharath.media_backend.repo.MediaAssetRepository;
import com.bharath.media_backend.repo.MediaViewLogRepository;
import com.bharath.media_backend.util.HmacSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaAssetRepository mediaRepo;
    private final HmacSigner signer;
    private final MediaViewLogRepository mediaViewLogRepo;

    @Value("${app.stream.ttl-minutes}")
    private int streamTtlMinutes;

    public MediaController(MediaAssetRepository mediaRepo,
                           HmacSigner signer,
                           MediaViewLogRepository mediaViewLogRepo) {
        this.mediaRepo = mediaRepo;
        this.signer = signer;
        this.mediaViewLogRepo = mediaViewLogRepo;
    }

    // Create media metadata
    @PostMapping
    public String createMedia(@RequestBody MediaCreateRequest request) {
        MediaAsset media = MediaAsset.builder()
                .title(request.getTitle())
                .type(request.getType())
                .fileUrl(request.getFileUrl())
                .createdAt(Instant.now())
                .build();
        mediaRepo.save(media);
        return "Media saved successfully with id: " + media.getId();
    }

    // Generate a secure stream URL
    @GetMapping("/{id}/stream-url")
    public StreamUrlResponse getStreamUrl(@PathVariable Long id) {
        MediaAsset media = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        long expiry = Instant.now().plusSeconds(streamTtlMinutes * 60).toEpochMilli();
        String data = media.getFileUrl() + "|" + expiry;
        String sig = signer.sign(data);

        String url = "/media/" + id + "/stream?exp=" + expiry + "&sig=" + sig;
        return new StreamUrlResponse(url);
    }

    // Stream media with range requests and logging
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamMedia(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) throws IOException {

        MediaAsset media = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        // Check expiry
        if (Instant.now().toEpochMilli() > exp) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // Verify signature
        String data = media.getFileUrl() + "|" + exp;
        String expectedSig = signer.sign(data);
        if (!expectedSig.equals(sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // Log the view
        MediaViewLog log = MediaViewLog.builder()
                .mediaId(media.getId())
                .viewedByIp(request.getRemoteAddr())
                .timestamp(Instant.now())
                .build();
        mediaViewLogRepo.save(log);

        // Resolve file
        Path filePath = Paths.get("uploads").resolve(media.getFileUrl().replace("/files/", ""));
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = Files.size(filePath);
        long rangeStart = 0;
        long rangeEnd = fileLength - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            rangeStart = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty()) {  // ✅ fixed part
                rangeEnd = Long.parseLong(ranges[1]);
            }
        }

        long contentLength = rangeEnd - rangeStart + 1;
        InputStream inputStream = Files.newInputStream(filePath);
        inputStream.skip(rangeStart);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", Files.probeContentType(filePath));
        headers.add("Content-Length", String.valueOf(contentLength));
        headers.add("Content-Disposition", "inline; filename=\"" + filePath.getFileName() + "\"");
        headers.add("Accept-Ranges", "bytes");
        headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);

        return ResponseEntity.status(rangeHeader != null ? 206 : 200)
                .headers(headers)
                .body(new InputStreamResource(new LimitedInputStream(inputStream, contentLength)));
    }

    @GetMapping("/{id}/view-log")
    public List<MediaViewLog> getViewLogs(@PathVariable Long id) {
        return mediaViewLogRepo.findByMediaId(id);
    }

    /**
     * Helper class to limit InputStream reading to a specific length
     */
    static class LimitedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        public LimitedInputStream(InputStream delegate, long limit) {
            this.delegate = delegate;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int result = delegate.read();
            if (result != -1) remaining--;
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int count = delegate.read(b, off, toRead);
            if (count != -1) remaining -= count;
            return count;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
