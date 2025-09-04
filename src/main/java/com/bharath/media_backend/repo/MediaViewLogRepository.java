package com.bharath.media_backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bharath.media_backend.domain.MediaViewLog;


public interface MediaViewLogRepository extends JpaRepository<MediaViewLog, Long> {

	List<MediaViewLog> findByMediaId(Long id);}
