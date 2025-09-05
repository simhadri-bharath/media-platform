package com.bharath.media_backend.controller;

import com.bharath.media_backend.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FileUploadControllerTest {

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUploadSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());
        when(fileUploadService.storeFile(file)).thenReturn("/files/test.txt");

        ResponseEntity<String> response = fileUploadController.upload(file);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("/files/test.txt", response.getBody());
        verify(fileUploadService, times(1)).storeFile(file);
    }

    @Test
    void testUploadEmptyFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        when(fileUploadService.storeFile(file)).thenThrow(new IllegalArgumentException("Empty file not allowed"));

        ResponseEntity<String> response = fileUploadController.upload(file);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Empty file not allowed", response.getBody());
        verify(fileUploadService, times(1)).storeFile(file);
    }

    @Test
    void testUploadIOException() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());
        when(fileUploadService.storeFile(file)).thenThrow(new IOException("Disk full"));

        ResponseEntity<String> response = fileUploadController.upload(file);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("File upload failed: Disk full", response.getBody());
        verify(fileUploadService, times(1)).storeFile(file);
    }

    @Test
    void testDownloadSuccess() throws MalformedURLException {
        Resource resource = new ByteArrayResource("Hello".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };
        when(fileUploadService.loadFileAsResource("test.txt")).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile("test.txt");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("test.txt", response.getBody().getFilename());
        verify(fileUploadService, times(1)).loadFileAsResource("test.txt");
    }

    @Test
    void testDownloadMalformedUrl() throws MalformedURLException {
        when(fileUploadService.loadFileAsResource("test.txt")).thenThrow(new MalformedURLException());

        ResponseEntity<Resource> response = fileUploadController.downloadFile("test.txt");

        assertEquals(400, response.getStatusCodeValue());
        verify(fileUploadService, times(1)).loadFileAsResource("test.txt");
    }

    @Test
    void testDownloadFileNotFound() throws MalformedURLException {
        when(fileUploadService.loadFileAsResource("test.txt")).thenThrow(new RuntimeException("File not found"));

        ResponseEntity<Resource> response = fileUploadController.downloadFile("test.txt");

        assertEquals(404, response.getStatusCodeValue());
        verify(fileUploadService, times(1)).loadFileAsResource("test.txt");
    }
}
