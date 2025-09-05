package com.bharath.media_backend.service;

import com.bharath.media_backend.api.dto.MediaCreateRequest;
import com.bharath.media_backend.api.dto.StreamUrlResponse;
import com.bharath.media_backend.domain.MediaAsset;
import com.bharath.media_backend.domain.MediaViewLog;
import com.bharath.media_backend.exception.MediaNotFoundException;
import com.bharath.media_backend.repo.MediaAssetRepository;
import com.bharath.media_backend.repo.MediaViewLogRepository;
import com.bharath.media_backend.util.HmacSigner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaAssetRepository mediaRepo;
    private final MediaViewLogRepository mediaViewLogRepo;
    private final HmacSigner signer;
    private final Cloudinary cloudinary;

    private final int MAX_REQUESTS = 5;
    private final long WINDOW_MILLIS = 60_000; // 1 minute
    private final ConcurrentHashMap<String, RequestInfo> requestMap = new ConcurrentHashMap<>();
    private final int streamTtlMinutes = 10;

    public MediaService(MediaAssetRepository mediaRepo,
                        HmacSigner signer,
                        MediaViewLogRepository mediaViewLogRepo,
                        Cloudinary cloudinary) {
        this.mediaRepo = mediaRepo;
        this.signer = signer;
        this.mediaViewLogRepo = mediaViewLogRepo;
        this.cloudinary = cloudinary;
    }

    // --- Upload file to Cloudinary ---
    public String uploadToCloudinary(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Empty file not allowed");

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder", "media_videos"
                ));
        return uploadResult.get("url").toString();
    }

    // --- Create media record ---
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

    // --- Generate stream URL ---
    public StreamUrlResponse generateStreamUrl(Long id) {
        MediaAsset media = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        long expiry = Instant.now().plusSeconds(streamTtlMinutes * 60).toEpochMilli();
        String data = media.getFileUrl() + "|" + expiry;
        String sig = signer.sign(data);

        String url = media.getFileUrl() + "?exp=" + expiry + "&sig=" + sig;
        return new StreamUrlResponse(url);
    }

    // --- Stream (redirect to Cloudinary with rate limiting) ---
//    public ResponseEntity<Resource> streamMedia(
//            HttpServletRequest request,
//            Long id,
//            long exp,
//            String sig,
//            String rangeHeader
//    ) {
//        MediaAsset media = mediaRepo.findById(id)
//                .orElseThrow(() -> new MediaNotFoundException("Media not found"));
//
//        // validate signed URL
//        if (Instant.now().toEpochMilli() > exp ||
//                !signer.sign(media.getFileUrl() + "|" + exp).equals(sig)) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
//        }
//
//        String clientIp = getClientIp(request);
//
//        // --- Apply Rate Limiting ---
//        if (isRateLimited(media.getId(), clientIp)) {
//            HttpHeaders headers = new HttpHeaders();
//            headers.add("Retry-After", String.valueOf(WINDOW_MILLIS / 1000)); // in seconds
//            return ResponseEntity.status(429).headers(headers).body(null); // Too Many Requests
//        }
//
//        // Save view log (only when allowed)
//        MediaViewLog log = MediaViewLog.builder()
//                .mediaId(media.getId())
//                .viewedByIp(clientIp)
//                .timestamp(Instant.now())
//                .build();
//        mediaViewLogRepo.save(log);
//        evictAnalyticsCache(media.getId());
//
//        // Redirect to Cloudinary URL
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Location", media.getFileUrl());
//        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
//    }

    public ResponseEntity<Resource> streamMedia(
            HttpServletRequest request,
            Long id,
            long exp,
            String sig,
            String rangeHeader
    ) {
        MediaAsset media = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        // --- Validate signed URL ---
        if (Instant.now().toEpochMilli() > exp ||
                !signer.sign(media.getFileUrl() + "|" + exp).equals(sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // --- Log view only once per access ---
        String clientIp = getClientIp(request); // optional: only for logging
        MediaViewLog log = MediaViewLog.builder()
                .mediaId(media.getId())
                .viewedByIp(clientIp)
                .timestamp(Instant.now())
                .build();
        mediaViewLogRepo.save(log);
        evictAnalyticsCache(media.getId());

        // --- Redirect to Cloudinary URL ---
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", media.getFileUrl());
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    private boolean isRateLimited(Long mediaId, String ip) {
        String key = mediaId + ":" + ip;
        long now = Instant.now().toEpochMilli();

        RequestInfo updated = requestMap.compute(key, (k, existing) -> {
            if (existing == null || now - existing.timestamp > WINDOW_MILLIS) {
                // new time window → reset
                return new RequestInfo(1, now);
            } else {
                if (existing.count >= MAX_REQUESTS) {
                    // already exceeded
                    return existing;
                }
                existing.count++;
                return existing;
            }
        });

        return updated.count > MAX_REQUESTS;
    }

    // --- Analytics ---
    @Cacheable(value = "mediaAnalytics", key = "#id")
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
        response.put("cache_status", "MISS");

        return response;
    }

    @CacheEvict(value = "mediaAnalytics", key = "#mediaId")
    public void evictAnalyticsCache(Long mediaId) {}

    public List<MediaViewLog> getViewLogs(Long id) {
        return mediaViewLogRepo.findByMediaId(id);
    }

    public ResponseEntity<String> recordView(Long mediaId, HttpServletRequest request) {
        String ip = getClientIp(request);
        String key = mediaId + ":" + ip;
        long now = Instant.now().toEpochMilli();

        RequestInfo updated = requestMap.compute(key, (k, existing) -> {
            if (existing == null || now - existing.timestamp > WINDOW_MILLIS) {
                // reset counter for new time window
                return new RequestInfo(1, now);
            } else {
                if (existing.count >= MAX_REQUESTS) {
                    // already exceeded → don’t increase count further
                    return existing;
                }
                existing.count++;
                return existing;
            }
        });

        // Block if limit reached
        if (updated.count > MAX_REQUESTS ||
                (now - updated.timestamp <= WINDOW_MILLIS && updated.count == MAX_REQUESTS)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Retry-After", String.valueOf(WINDOW_MILLIS / 1000));
            return ResponseEntity.status(429).headers(headers).body("Too many requests! Try later.");
        }

        // Save log only if request allowed
        MediaViewLog log = MediaViewLog.builder()
                .mediaId(mediaId)
                .viewedByIp(ip)
                .timestamp(Instant.now())
                .build();
        mediaViewLogRepo.save(log);
        evictAnalyticsCache(mediaId);

        return ResponseEntity.ok("View recorded successfully!");
    }

    // --- Extract client IP safely ---
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim(); // first IP is client
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private static class RequestInfo {
        int count;
        long timestamp;
        RequestInfo(int count, long timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }
}
