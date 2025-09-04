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
@Table(name="media_view_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MediaViewLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private Long mediaId;
    @Column(nullable=false)
    private String viewedByIp;
    @Column(nullable=false)
    private Instant timestamp;
}
