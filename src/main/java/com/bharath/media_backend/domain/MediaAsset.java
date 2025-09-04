package com.bharath.media_backend.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="media_assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MediaAsset {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String title;
    @Column(nullable=false)
    private String type; // video/audio
    @Column(nullable=false)
    private String fileUrl;
    @Column(nullable=false)
    private Instant createdAt;
}
