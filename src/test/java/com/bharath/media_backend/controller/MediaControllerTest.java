package com.bharath.media_backend.controller;


import com.bharath.media_backend.api.dto.MediaCreateRequest;
import com.bharath.media_backend.api.dto.StreamUrlResponse;
import com.bharath.media_backend.domain.MediaViewLog;
import com.bharath.media_backend.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateMedia() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "dummy content".getBytes());
        when(mediaService.uploadToCloudinary(file)).thenReturn("http://cloudinary/video.mp4");
        when(mediaService.createMedia(any(MediaCreateRequest.class))).thenReturn("Media saved successfully");

        String response = mediaController.createMedia("Test Title", "video", file);

        assertEquals("Media saved successfully", response);
        verify(mediaService, times(1)).uploadToCloudinary(file);
        verify(mediaService, times(1)).createMedia(any(MediaCreateRequest.class));
    }

    @Test
    void testGetStreamUrl() {
        StreamUrlResponse mockResponse = new StreamUrlResponse("http://cloudinary/video.mp4?exp=123&sig=abc");
        when(mediaService.generateStreamUrl(1L)).thenReturn(mockResponse);

        StreamUrlResponse response = mediaController.getStreamUrl(1L);

        // Use getStreamUrl() instead of getUrl()
        assertEquals(mockResponse.getStreamUrl(), response.getStreamUrl());
        verify(mediaService, times(1)).generateStreamUrl(1L);
    }


    @Test
    void testGetViewLogs() {
        List<MediaViewLog> logs = Collections.emptyList();
        when(mediaService.getViewLogs(1L)).thenReturn(logs);

        List<MediaViewLog> response = mediaController.getViewLogs(1L);

        assertEquals(logs, response);
        verify(mediaService, times(1)).getViewLogs(1L);
    }

    @Test
    void testViewMedia() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(mediaService.recordView(1L, request)).thenReturn(ResponseEntity.ok("View recorded successfully!"));

        ResponseEntity<String> response = mediaController.viewMedia(1L, request);

        assertEquals("View recorded successfully!", response.getBody());
        verify(mediaService, times(1)).recordView(1L, request);
    }

    @Test
    void testGetAnalytics() {
        Map<String, Object> analytics = Map.of("total_views", 10L, "unique_ips", 5L);
        when(mediaService.getAnalytics(1L)).thenReturn(analytics);

        ResponseEntity<Map<String, Object>> response = mediaController.getAnalytics(1L);

        assertEquals(analytics, response.getBody());
        verify(mediaService, times(1)).getAnalytics(1L);
    }
}
