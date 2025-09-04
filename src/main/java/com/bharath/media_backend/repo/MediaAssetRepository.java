package com.bharath.media_backend.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bharath.media_backend.domain.MediaAsset;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {}
