package com.bharath.media_backend.service;

import com.bharath.media_backend.api.dto.MediaCreateRequest;
import com.bharath.media_backend.api.dto.StreamUrlResponse;
import com.bharath.media_backend.domain.MediaAsset;
import com.bharath.media_backend.domain.MediaViewLog;
import com.bharath.media_backend.exception.MediaNotFoundException;
import com.bharath.media_backend.repo.MediaAssetRepository;
import com.bharath.media_backend.repo.MediaViewLogRepository;
import com.bharath.media_backend.util.HmacSigner;
import com.bharath.media_backend.util.LimitedInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaAssetRepository mediaRepo;
    private final HmacSigner signer;
    private final MediaViewLogRepository mediaViewLogRepo;

    @Value("${app.stream.ttl-minutes}")
    private int streamTtlMinutes;

    public MediaService(MediaAssetRepository mediaRepo,
                        HmacSigner signer,
                        MediaViewLogRepository mediaViewLogRepo) {
        this.mediaRepo = mediaRepo;
        this.signer = signer;
        this.mediaViewLogRepo = mediaViewLogRepo;
    }

    public String createMedia(MediaCreateRequest request) {
        MediaAsset media = MediaAsset.builder()
                .title(request.getTitle())
                .type(request.getType())
                .fileUrl(request.getFileUrl())
                .createdAt(Instant.now())
                .build();
        mediaRepo.save(media);
        return "Media saved successfully with id: " + media.getId();
    }

    public StreamUrlResponse generateStreamUrl(Long id) {
        MediaAsset media = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        long expiry = Instant.now().plusSeconds(streamTtlMinutes * 60).toEpochMilli();
        String data = media.getFileUrl() + "|" + expiry;
        String sig = signer.sign(data);

        String url = "/media/" + id + "/stream?exp=" + expiry + "&sig=" + sig;
        return new StreamUrlResponse(url);
    }

    public ResponseEntity<Resource> streamMedia(
            HttpServletRequest request,
            Long id,
            long exp,
            String sig,
            String rangeHeader
    ) throws IOException {

        MediaAsset media = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        if (Instant.now().toEpochMilli() > exp) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        String data = media.getFileUrl() + "|" + exp;
        String expectedSig = signer.sign(data);
        if (!expectedSig.equals(sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        MediaViewLog log = MediaViewLog.builder()
                .mediaId(media.getId())
                .viewedByIp(request.getRemoteAddr())
                .timestamp(Instant.now())
                .build();
        mediaViewLogRepo.save(log);

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
            if (ranges.length > 1 && !ranges[1].isEmpty()) {
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

    public Map<String, Object> getAnalytics(Long id) {
        MediaAsset media = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        List<MediaViewLog> logs = mediaViewLogRepo.findByMediaId(id);

        long totalViews = logs.size();
        long uniqueIps = logs.stream().map(MediaViewLog::getViewedByIp).distinct().count();

        Map<String, Long> viewsPerDay = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().toString().substring(0, 10),
                        Collectors.counting()
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("total_views", totalViews);
        response.put("unique_ips", uniqueIps);
        response.put("views_per_day", viewsPerDay);

        return response;
    }

    public List<MediaViewLog> getViewLogs(Long id) {
        return mediaViewLogRepo.findByMediaId(id);
    }
}
