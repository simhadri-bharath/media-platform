package com.bharath.media_backend.api.dto;

import lombok.*;

@Getter @Setter
public class MediaCreateRequest {
    private String title;
    private String type; // video/audio
    private String fileUrl;
}
